Build a personal Android app plus a small data pipeline that shows the movies currently on Netflix in Germany, sorted by IMDb rating, with genre filtering. This is for personal, non-commercial use only.

## Overview

Two parts in one Git repository (monorepo):

1. `pipeline/`: a Python script that runs once a day as a GitHub Action, builds `movies.json` and publishes it to GitHub Pages.
2. `app/`: an Android app (Kotlin, Jetpack Compose) that downloads `movies.json`, caches it, and lets me browse, filter and sort.

The app gets the Netflix catalog only from `movies.json`. Its one direct TMDB call is the movie search for the watchlist (see "Watchlist & notifications"). The TMDB token for that comes from `local.properties` (git-ignored) through a `BuildConfig` field and is never committed.

## Part 1: Data pipeline (`pipeline/`)

Python 3.12, keep dependencies minimal (`requests` is fine; use the standard library where possible). Include a `requirements.txt`.

Steps:

1. **Netflix provider ID**: call `GET https://api.themoviedb.org/3/watch/providers/movie?watch_region=DE` and look up the Netflix provider ID (expected to be 8). Don't hardcode it without checking. Include only flat-rate (subscription) availability.
2. **Netflix DE movie list**: page through `GET /3/discover/movie` with `watch_region=DE`, `with_watch_providers=<netflix id>`, `with_watch_monetization_types=flatrate`, `language=de-DE`, `sort_by=popularity.desc`. TMDB caps discover at 500 pages; if the total ever exceeds that, split the query by release year ranges. Deduplicate by TMDB ID.
3. **Genres**: fetch `GET /3/genre/movie/list` in both `de-DE` and `en-US` and map each movie's `genre_ids` to names. Store genre IDs on movies and keep a separate genre lookup table with both languages.
4. **IMDb IDs**: for each movie, get the IMDb ID via `GET /3/movie/{id}/external_ids`. Cache these in a local file (committed or stored as an Actions cache) so later runs only look up new movies. Respect TMDB rate limits (add a small delay or concurrency limit and retry on 429).
5. **IMDb ratings**: download `https://datasets.imdbws.com/title.ratings.tsv.gz` (the official IMDb non-commercial dataset, refreshed daily), stream-parse it, and join on IMDb ID to get `averageRating` and `numVotes`. Movies without an IMDb ID or rating stay in the output with `null` rating.
6. **Output**: write `movies.json` in this shape:

```json
{
  "generatedAt": "2026-10-31T04:00:00Z",
  "region": "DE",
  "genres": [{ "id": 28, "de": "Action", "en": "Action" }],
  "movies": [
    {
      "tmdbId": 27205,
      "imdbId": "tt1375666",
      "title": "Inception",
      "originalTitle": "Inception",
      "year": 2010,
      "genreIds": [28, 878, 12],
      "imdbRating": 8.8,
      "imdbVotes": 2600000,
      "posterPath": "/abc.jpg",
      "overview": "..."
    }
  ]
}
```

   Sort `movies` by `imdbRating` descending (nulls last). Keep the file compact (no pretty-printing in the published version).

7. **Auth**: read the TMDB API Read Access Token from the env var `TMDB_TOKEN` and send it as a Bearer token. Never commit it.
8. **Local run**: `python pipeline/build.py --out dist/movies.json` must work locally for testing. Log counts (movies found, with IMDb ID, with rating).

### GitHub Action

`.github/workflows/build-data.yml`:
- Runs daily (e.g. 04:00 UTC) and on manual dispatch.
- Uses the `TMDB_TOKEN` repository secret.
- Publishes `movies.json` to GitHub Pages so it's reachable at a stable URL like `https://<user>.github.io/<repo>/movies.json`.

## Part 2: Android app (`app/`)

- Kotlin, Jetpack Compose, Material 3, min SDK 26, target the latest stable SDK.
- Networking: OkHttp or Ktor, plus kotlinx.serialization. Images: Coil, with TMDB poster URLs (`https://image.tmdb.org/t/p/w342{posterPath}`).
- Keep the data URL in one config constant (`BuildConfig` field) so I can change it.
- Architecture: single-activity, a ViewModel, a repository that caches the last downloaded JSON on disk, so the app works offline and opens instantly. Refresh on launch if the cache is older than 12 hours, plus pull-to-refresh.

### Features

- **Visual style** (dark only, uncluttered):
  - Background pure black `#000000`. At the top of the screen, a soft glow behind the header: `linear-gradient(160deg, #7A1C3E 0%, #3B1C78 38%, transparent 78%)`, about 340 dp tall, fading into black.
  - Brand gradient (red → purple → blue): `#E8475F` → `#8A4FD8` → `#3E63F2`. Use it sparingly: the active tab underline and the ≥ 8.0 rating ring only.
  - Cards: `#141417`, 1 dp border `rgba(255,255,255,0.06)`, corner radius 20 dp. Primary text `#F5F5F7`, secondary text `#9C9CA6`.
  - Font: Plus Jakarta Sans (Google Fonts, bundled via downloadable fonts or res/font), bold/extra-bold for headings.
  - Top of screen: "Flickpick" title (30 sp, extra bold), a pill-shaped search field (`rgba(255,255,255,0.08)` fill), then two tabs "Top rated" | "Watchlist" with a 3 dp gradient underline under the active tab (no bottom navigation bar), then the genre chips. Selected chip: `rgba(138,79,216,0.28)` fill, `rgba(160,110,240,0.6)` border, `#E2D6FF` text; unselected: `rgba(255,255,255,0.06)` fill, `#C9C9D1` text.
- **List item layout** (one card per movie, in a single-column list), deliberately minimal:
  - Left: poster thumbnail (64×96 dp, radius 12 dp, placeholder if missing) with a small rank number in a dark circle in its top-left corner.
  - Right of it: title (17 sp bold, max 2 lines), below it year · genres (1 line, muted).
  - Bottom-right corner of the card: the rating ring (below). Vote count is not on the card; show it in the detail sheet.
- **Rating ring**: a 46 dp circle with a 3 dp ring and the rating (one decimal, 14 sp extra bold) in the middle. The ring's strength shows the band, so 8, 7 and 6 are told apart at a glance:
  - ≥ 8.0: brand gradient ring, white number
  - 7.0–7.9: solid purple ring `#8A4FD8`, white number
  - 6.0–6.9: grey ring `#4A4A55`, light number `#E4E4EA`
  - < 6.0 and no rating ("–"): faint ring `#26262C`, muted number `#8E8E98`
  Define the bands in one place (e.g. `RatingBand` enum) and use the same ring in the detail sheet and watchlist.
- **Detail sheet**: larger poster, title, year, genres, rating ring, vote count (e.g. "1.2M votes"), overview.
- **Default sort**: IMDb rating descending. Also sort options for vote count, year and title.
- **Genre filter**: multi-select filter chips (horizontal scroll row or bottom sheet). Toggle between "match any" and "match all" selected genres. Genre names in German if the device language is German, else English.
- **Minimum votes filter**: slider or presets (0, 1k, 10k, 50k, 100k), default 10k, so obscure titles with few votes don't dominate the top.
- **Search**: by title (both German and original title).
- **Remember filters** between launches (DataStore).
- **Detail**: tapping a movie shows a bottom sheet with overview and buttons: "Open in Netflix" (open `https://www.netflix.com/search?q=<title>`, which the Netflix app handles), "IMDb" (`https://www.imdb.com/title/<imdbId>/`).
- **Header**: show when the data was last generated.
- **About screen** with the required attribution: "This product uses the TMDB API but is not endorsed or certified by TMDB", streaming data by JustWatch, and ratings from IMDb (non-commercial dataset).
- Handle loading, empty-filter results and network errors gracefully.

### Watchlist & notifications

I want to add movies that are **not** on Netflix Germany yet, and get a notification once they are.

- **Add a movie**: a "Watchlist" tab with a search field. It queries `GET https://api.themoviedb.org/3/search/movie?query=...&language=de-DE` (debounced, Bearer token from `BuildConfig`). Results show poster, title, original title and year. Tap to add.
- If the movie is already in the current `movies.json`, say so right away ("Already on Netflix") and offer to open it instead of adding it.
- **Storage**: Room database with watchlist entries: TMDB ID, title, year, poster path, date added, status (`waiting` / `available`), date it became available.
- **Watchlist screen**: two sections, "Waiting" and "Now on Netflix". Swipe to remove, plus a button to clear the available ones.
- **Background check**: a WorkManager periodic job (every 12 hours, requires network). It downloads `movies.json` through the same repository (refreshing the cache), compares it with the watchlist, and for each `waiting` movie that now appears:
  - marks it `available` (so it only notifies once),
  - posts a notification, "<Title> is now on Netflix", that opens the movie's detail sheet when tapped. If several arrive at once, group them into one summary notification.
- Run the same check whenever the app refreshes data in the foreground.
- If a movie marked `available` later disappears from `movies.json`, set it back to `waiting` so I get notified again if it returns.
- **Notifications setup**: create a notification channel ("Watchlist") and request the `POST_NOTIFICATIONS` permission on Android 13+ the first time I add a movie to the watchlist, not at app start. If it's denied, show a short hint on the Watchlist screen.
- Add a debug-only menu option "Run watchlist check now" so I can test it without waiting.
- Unit tests for the comparison logic (new arrival, already notified, left and came back).

## Working approach

1. Start with the pipeline. Run it locally once I've exported `TMDB_TOKEN`, and show me the counts and a few sample entries before moving on.
2. Then build the Android app against a local copy of `movies.json`, then switch to the GitHub Pages URL.
3. Make sure `./gradlew assembleDebug` succeeds. Add a few unit tests for the filter/sort logic and for the pipeline's join step.
4. Write a short `README.md`: how to get a TMDB token, put it in `local.properties` as `TMDB_TOKEN=...` for the app, set the GitHub secret, enable Pages, build and install the APK (`adb install` or via Android Studio).
5. Ask me before making choices not covered here that would be hard to change later.
