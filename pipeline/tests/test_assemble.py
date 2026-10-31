import gzip
import io
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import assemble  # noqa: E402
import imdb  # noqa: E402


def tm(id_, title, genre_ids=(), release="2010-07-16", original=None, poster="/p.jpg"):
    return {
        "id": id_,
        "title": title,
        "original_title": original or title,
        "genre_ids": list(genre_ids),
        "release_date": release,
        "poster_path": poster,
        "overview": f"About {title}",
    }


class JoinTest(unittest.TestCase):
    def setUp(self):
        self.discovered = [
            tm(1, "Inception", [28, 878]),
            tm(2, "Kein IMDb", release=""),
            tm(3, "Ohne Rating", original="No Rating"),
            tm(4, "Mittel"),
            tm(1, "Inception duplicate"),
        ]
        self.imdb_ids = {1: "tt1375666", 2: None, 3: "tt0000003", 4: "tt0000004"}
        self.ratings = {"tt1375666": (8.8, 2600000), "tt0000004": (6.5, 1200)}

    def test_join_and_nulls(self):
        movies = assemble.build_movies(self.discovered, self.imdb_ids, self.ratings)
        by_id = {m["tmdbId"]: m for m in movies}
        self.assertEqual(len(movies), 4, "deduped by TMDB id")
        self.assertEqual(by_id[1]["title"], "Inception", "first occurrence wins")
        self.assertEqual(by_id[1]["imdbRating"], 8.8)
        self.assertEqual(by_id[1]["imdbVotes"], 2600000)
        self.assertEqual(by_id[1]["genreIds"], [28, 878])
        self.assertEqual(by_id[1]["year"], 2010)
        self.assertIsNone(by_id[2]["imdbId"])
        self.assertIsNone(by_id[2]["imdbRating"])
        self.assertIsNone(by_id[2]["year"])
        self.assertEqual(by_id[3]["imdbId"], "tt0000003")
        self.assertIsNone(by_id[3]["imdbRating"])
        self.assertEqual(by_id[3]["originalTitle"], "No Rating")

    def test_sorted_desc_nulls_last(self):
        movies = assemble.build_movies(self.discovered, self.imdb_ids, self.ratings)
        self.assertEqual([m["tmdbId"] for m in movies[:2]], [1, 4])
        self.assertTrue(all(m["imdbRating"] is None for m in movies[2:]))

    def test_tiebreak_by_votes(self):
        movies = assemble.build_movies(
            [tm(1, "A"), tm(2, "B")], {1: "tt1", 2: "tt2"}, {"tt1": (7.0, 10), "tt2": (7.0, 500)}
        )
        self.assertEqual([m["tmdbId"] for m in movies], [2, 1])

    def test_genre_table(self):
        table = assemble.genre_table({28: "Action", 35: "Komödie"}, {28: "Action", 35: "Comedy", 99: "Doc"})
        self.assertEqual(table[1], {"id": 35, "de": "Komödie", "en": "Comedy"})
        self.assertEqual(table[2], {"id": 99, "de": "Doc", "en": "Doc"})

    def test_compact_output(self):
        doc = assemble.document("2026-10-31T04:00:00Z", "DE", [], [])
        text = assemble.dumps_compact(doc)
        self.assertNotIn(" ", text)
        self.assertEqual(json.loads(text)["region"], "DE")


class ImdbParseTest(unittest.TestCase):
    TSV = "tconst\taverageRating\tnumVotes\ntt0000001\t5.7\t2100\ntt1375666\t8.8\t2600000\nbad\tx\ty\n"

    def test_parse_filtered(self):
        r = imdb.parse_ratings(io.StringIO(self.TSV), wanted={"tt1375666"})
        self.assertEqual(r, {"tt1375666": (8.8, 2600000)})

    def test_parse_gzip(self):
        raw = io.BytesIO(gzip.compress(self.TSV.encode()))
        r = imdb.parse_gzip_stream(raw)
        self.assertEqual(r["tt0000001"], (5.7, 2100))
        self.assertNotIn("bad", r)


if __name__ == "__main__":
    unittest.main()
