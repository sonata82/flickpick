"""Minimal TMDB v3 client: Bearer auth, retries on 429/5xx, gentle rate limiting."""

from __future__ import annotations

import logging
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Callable, Iterable

import requests

API = "https://api.themoviedb.org/3"
DISCOVER_PAGE_CAP = 500  # TMDB refuses page > 500

log = logging.getLogger(__name__)


class TmdbClient:
    def __init__(self, token: str, min_interval: float = 0.03, max_retries: int = 6):
        self.session = requests.Session()
        self.session.headers.update(
            {"Authorization": f"Bearer {token}", "Accept": "application/json"}
        )
        self.min_interval = min_interval
        self.max_retries = max_retries
        self._lock = threading.Lock()
        self._next_slot = 0.0

    def _throttle(self) -> None:
        # Global spacing between requests across threads (~33 req/s max by default).
        with self._lock:
            now = time.monotonic()
            wait = self._next_slot - now
            self._next_slot = max(now, self._next_slot) + self.min_interval
        if wait > 0:
            time.sleep(wait)

    def get(self, path: str, **params: Any) -> dict:
        url = f"{API}{path}"
        for attempt in range(self.max_retries + 1):
            self._throttle()
            try:
                resp = self.session.get(url, params=params, timeout=30)
            except requests.RequestException as e:
                if attempt == self.max_retries:
                    raise
                delay = 2**attempt
                log.warning("GET %s failed (%s), retrying in %ss", path, e, delay)
                time.sleep(delay)
                continue
            if resp.status_code == 429 or resp.status_code >= 500:
                if attempt == self.max_retries:
                    resp.raise_for_status()
                retry_after = resp.headers.get("Retry-After")
                delay = float(retry_after) if retry_after else min(2**attempt, 30)
                log.warning("GET %s -> %s, retrying in %.1fs", path, resp.status_code, delay)
                time.sleep(delay)
                continue
            resp.raise_for_status()
            return resp.json()
        raise RuntimeError("unreachable")

    # ---- endpoints -------------------------------------------------------

    def netflix_provider_id(self, region: str) -> int:
        data = self.get("/watch/providers/movie", watch_region=region)
        matches = [
            p for p in data.get("results", []) if p.get("provider_name", "").strip().lower() == "netflix"
        ]
        if not matches:
            raise RuntimeError(f"Netflix provider not found for region {region}")
        pid = int(matches[0]["provider_id"])
        if pid != 8:
            log.warning("Netflix provider id is %s (expected 8)", pid)
        return pid

    def genres(self, language: str) -> dict[int, str]:
        data = self.get("/genre/movie/list", language=language)
        return {int(g["id"]): g["name"] for g in data.get("genres", [])}

    def _discover_page(self, base: dict, page: int) -> dict:
        return self.get("/discover/movie", **base, page=page)

    def discover_netflix(self, region: str, provider_id: int) -> list[dict]:
        base = {
            "watch_region": region,
            "with_watch_providers": provider_id,
            "with_watch_monetization_types": "flatrate",
            "language": "en-US",  # English titles/overviews; availability is still region DE
            "sort_by": "popularity.desc",
            "include_adult": "false",
        }
        seen: dict[int, dict] = {}
        self._discover_range(base, None, None, seen)
        return list(seen.values())

    def _discover_range(
        self, base: dict, year_from: int | None, year_to: int | None, seen: dict[int, dict]
    ) -> None:
        params = dict(base)
        if year_from is not None:
            params["primary_release_date.gte"] = f"{year_from}-01-01"
        if year_to is not None:
            params["primary_release_date.lte"] = f"{year_to}-12-31"
        first = self._discover_page(params, 1)
        total_pages = int(first.get("total_pages", 0))
        label = f"{year_from or '…'}–{year_to or '…'}"
        if total_pages > DISCOVER_PAGE_CAP:
            lo = year_from if year_from is not None else 1870
            hi = year_to if year_to is not None else time.gmtime().tm_year + 2
            if lo >= hi:
                log.warning("Range %s still exceeds %s pages; truncating", label, DISCOVER_PAGE_CAP)
                total_pages = DISCOVER_PAGE_CAP
            else:
                mid = (lo + hi) // 2
                log.info("Range %s has %s pages, splitting at %s", label, total_pages, mid)
                # Movies without a release date fall outside any bounded range; the unbounded
                # top-level query only adds them if it is not split, which is acceptable.
                self._discover_range(base, lo, mid, seen)
                self._discover_range(base, mid + 1, hi, seen)
                return
        log.info("Discover %s: %s results in %s pages", label, first.get("total_results"), total_pages)
        pages = [first]
        if total_pages > 1:
            with ThreadPoolExecutor(max_workers=6) as pool:
                pages += list(pool.map(lambda p: self._discover_page(params, p), range(2, total_pages + 1)))
        for page in pages:
            for m in page.get("results", []):
                seen.setdefault(int(m["id"]), m)

    def imdb_id(self, tmdb_id: int) -> str | None:
        try:
            data = self.get(f"/movie/{tmdb_id}/external_ids")
        except requests.HTTPError as e:
            if e.response is not None and e.response.status_code == 404:
                return None
            raise
        imdb = data.get("imdb_id")
        return imdb or None

    def map_concurrent(
        self, fn: Callable[[int], Any], items: Iterable[int], workers: int = 8
    ) -> dict[int, Any]:
        items = list(items)
        with ThreadPoolExecutor(max_workers=workers) as pool:
            return dict(zip(items, pool.map(fn, items)))
