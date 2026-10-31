"""Pure functions that turn raw TMDB/IMDb data into movies.json."""

from __future__ import annotations

import json
from typing import Any


def genre_table(de: dict[int, str], en: dict[int, str]) -> list[dict]:
    ids = sorted(set(de) | set(en))
    return [{"id": i, "de": de.get(i) or en.get(i), "en": en.get(i) or de.get(i)} for i in ids]


def _year(release_date: str | None) -> int | None:
    if release_date and len(release_date) >= 4 and release_date[:4].isdigit():
        return int(release_date[:4])
    return None


def dedupe(movies: list[dict]) -> list[dict]:
    seen: dict[int, dict] = {}
    for m in movies:
        seen.setdefault(int(m["id"]), m)
    return list(seen.values())


def build_movies(
    discovered: list[dict],
    imdb_ids: dict[int, str | None],
    ratings: dict[str, tuple[float, int]],
) -> list[dict]:
    out = []
    for m in dedupe(discovered):
        tmdb_id = int(m["id"])
        imdb_id = imdb_ids.get(tmdb_id)
        rating = ratings.get(imdb_id) if imdb_id else None
        out.append(
            {
                "tmdbId": tmdb_id,
                "imdbId": imdb_id,
                "title": m.get("title") or m.get("original_title") or "",
                "originalTitle": m.get("original_title") or m.get("title") or "",
                "year": _year(m.get("release_date")),
                "genreIds": [int(g) for g in m.get("genre_ids", [])],
                "imdbRating": rating[0] if rating else None,
                "imdbVotes": rating[1] if rating else None,
                "posterPath": m.get("poster_path"),
                "overview": m.get("overview") or "",
            }
        )
    return sort_movies(out)


def sort_movies(movies: list[dict]) -> list[dict]:
    """Rating descending, nulls last; ties broken by votes desc, then title."""
    return sorted(
        movies,
        key=lambda m: (
            m["imdbRating"] is None,
            -(m["imdbRating"] or 0.0),
            -(m["imdbVotes"] or 0),
            m["title"].lower(),
        ),
    )


def document(generated_at: str, region: str, genres: list[dict], movies: list[dict]) -> dict[str, Any]:
    return {"generatedAt": generated_at, "region": region, "genres": genres, "movies": movies}


def dumps_compact(doc: dict) -> str:
    return json.dumps(doc, ensure_ascii=False, separators=(",", ":"))
