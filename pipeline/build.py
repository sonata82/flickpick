#!/usr/bin/env python3
"""Build movies.json: Netflix Germany movies (TMDB/JustWatch) joined with IMDb ratings.

Usage: TMDB_TOKEN=... python pipeline/build.py --out dist/movies.json
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import assemble  # noqa: E402
import imdb  # noqa: E402
from tmdb import TmdbClient  # noqa: E402

REGION = "DE"
DEFAULT_CACHE = Path(__file__).resolve().parent / "cache" / "imdb_ids.json"
MISSING_RETRY_SECONDS = 30 * 24 * 3600  # re-check movies without IMDb ID monthly

log = logging.getLogger("build")


def load_cache(path: Path) -> dict[str, dict]:
    if path.exists():
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            log.warning("Cache %s is corrupt, starting fresh", path)
    return {}


def save_cache(path: Path, cache: dict[str, dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    ordered = {k: cache[k] for k in sorted(cache, key=int)}
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(ordered, indent=0, separators=(",", ":")) + "\n", encoding="utf-8")
    tmp.replace(path)


def resolve_imdb_ids(client: TmdbClient, tmdb_ids: list[int], cache: dict[str, dict]) -> dict[int, str | None]:
    """cache entry: {"i": "tt..."} or {"i": null, "t": <unix ts of last check>}"""
    now = int(time.time())
    todo = []
    for tid in tmdb_ids:
        entry = cache.get(str(tid))
        if entry is None or (entry.get("i") is None and now - entry.get("t", 0) > MISSING_RETRY_SECONDS):
            todo.append(tid)
    log.info("IMDb IDs: %s cached, %s to look up", len(tmdb_ids) - len(todo), len(todo))
    if todo:
        results = client.map_concurrent(client.imdb_id, todo, workers=8)
        for tid, imdb_id in results.items():
            cache[str(tid)] = {"i": imdb_id} if imdb_id else {"i": None, "t": now}
    return {tid: cache.get(str(tid), {}).get("i") for tid in tmdb_ids}


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out", default="dist/movies.json", type=Path)
    ap.add_argument("--cache", default=DEFAULT_CACHE, type=Path)
    ap.add_argument("--pretty", action="store_true", help="indent output (for debugging)")
    args = ap.parse_args(argv)

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    token = os.environ.get("TMDB_TOKEN", "").strip()
    if not token:
        log.error("TMDB_TOKEN env var is not set (TMDB API Read Access Token)")
        return 2

    client = TmdbClient(token)
    provider_id = client.netflix_provider_id(REGION)
    log.info("Netflix provider id for %s: %s", REGION, provider_id)

    genres = assemble.genre_table(client.genres("de-DE"), client.genres("en-US"))
    discovered = assemble.dedupe(client.discover_netflix(REGION, provider_id))
    log.info("Movies found on Netflix %s: %s", REGION, len(discovered))

    cache = load_cache(args.cache)
    imdb_ids = resolve_imdb_ids(client, [int(m["id"]) for m in discovered], cache)
    save_cache(args.cache, cache)

    wanted = {i for i in imdb_ids.values() if i}
    ratings = imdb.download_ratings(wanted)

    movies = assemble.build_movies(discovered, imdb_ids, ratings)
    generated_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    doc = assemble.document(generated_at, REGION, genres, movies)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(doc, ensure_ascii=False, indent=2) if args.pretty else assemble.dumps_compact(doc)
    args.out.write_text(text, encoding="utf-8")

    with_imdb = sum(1 for m in movies if m["imdbId"])
    with_rating = sum(1 for m in movies if m["imdbRating"] is not None)
    log.info(
        "Done: %s movies, %s with IMDb ID, %s with rating, %s genres -> %s (%.0f KB)",
        len(movies), with_imdb, with_rating, len(genres), args.out, args.out.stat().st_size / 1024,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
