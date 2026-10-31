"""Stream-parse the official IMDb non-commercial ratings dataset."""

from __future__ import annotations

import gzip
import io
import logging
from typing import IO, Iterable

import requests

RATINGS_URL = "https://datasets.imdbws.com/title.ratings.tsv.gz"

log = logging.getLogger(__name__)


def parse_ratings(lines: Iterable[str], wanted: set[str] | None = None) -> dict[str, tuple[float, int]]:
    """Parse TSV lines (tconst, averageRating, numVotes) incl. header.

    If `wanted` is given, only those tconsts are kept (keeps memory small).
    """
    out: dict[str, tuple[float, int]] = {}
    it = iter(lines)
    header = next(it, None)
    if header is None:
        return out
    cols = header.rstrip("\n").split("\t")
    i_id, i_rating, i_votes = cols.index("tconst"), cols.index("averageRating"), cols.index("numVotes")
    for line in it:
        parts = line.rstrip("\n").split("\t")
        if len(parts) <= max(i_id, i_rating, i_votes):
            continue
        tconst = parts[i_id]
        if wanted is not None and tconst not in wanted:
            continue
        try:
            out[tconst] = (float(parts[i_rating]), int(parts[i_votes]))
        except ValueError:
            continue
    return out


def parse_gzip_stream(raw: IO[bytes], wanted: set[str] | None = None) -> dict[str, tuple[float, int]]:
    with gzip.GzipFile(fileobj=raw) as gz:
        text = io.TextIOWrapper(gz, encoding="utf-8")
        return parse_ratings(text, wanted)


def download_ratings(wanted: set[str] | None = None, url: str = RATINGS_URL) -> dict[str, tuple[float, int]]:
    log.info("Downloading %s", url)
    with requests.get(url, stream=True, timeout=120) as resp:
        resp.raise_for_status()
        resp.raw.decode_content = False  # we gunzip ourselves
        ratings = parse_gzip_stream(resp.raw, wanted)
    log.info("Parsed %s matching IMDb ratings", len(ratings))
    return ratings
