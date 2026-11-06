# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Flickpick shows movies currently on Netflix Germany, sorted by IMDb rating. It is a personal, non-commercial project and a monorepo with two parts:

- `pipeline/`: Python 3.12+ script that builds `movies.json`. GitHub Actions runs it daily and publishes it to GitHub Pages (`https://sonata82.github.io/flickpick/movies.json`).
- `app/`: Android app (Kotlin, Jetpack Compose, package `de.remk0.flickpick`) that downloads and browses `movies.json`.

`netflix-imdb-prompt.md` is the original spec. Where it differs from the code or README, the code and README are newer.

## Commands

Pipeline (from the repo root; needs `TMDB_TOKEN`, the TMDB API Read Access Token `eyJ…`, not the short API key):

```sh
pip install -r pipeline/requirements.txt
TMDB_TOKEN=eyJ... python pipeline/build.py --out dist/movies.json [--pretty]
python -m unittest discover -s pipeline/tests                       # all tests
python -m unittest discover -s pipeline/tests -k test_name          # single test
```

App (from `app/`; needs `app/local.properties` with `sdk.dir`, `TMDB_TOKEN` and optionally `DATA_URL`, e.g. `http://10.0.2.2:8000/movies.json` for a local server):

```sh
./gradlew assembleDebug testDebugUnitTest
./gradlew testDebugUnitTest --tests 'de.remk0.flickpick.domain.MovieFilterTest'   # single test class
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

JDK 17, compileSdk/targetSdk 36, minSdk 26. Release builds are signed with the debug key on purpose, so they can be installed.

## Architecture

### Data contract

`movies.json` is the only interface between the two halves: `{generatedAt, region, genres[{id,de,en}], movies[{tmdbId, imdbId, title, originalTitle, year, genreIds, imdbRating, imdbVotes, posterPath, overview}]}`. The pipeline produces it in `pipeline/assemble.py` (`build_movies`, `document`), and the app parses it in `app/.../data/Models.kt` (kotlinx.serialization with `ignoreUnknownKeys` and `coerceInputValues`). **Change both sides together.** Movies are pre-sorted by rating descending (nulls last), then votes, then title. Movies without an IMDb ID or rating stay in the file with nulls.

### Pipeline

`build.py` coordinates the run:

1. `tmdb.py` (TmdbClient) looks up the Netflix provider ID, gets genres in de and en, and pages through discover for flat-rate Netflix DE. The client handles throttling across threads, retries on 429/5xx, and the 500-page discover limit.
2. It resolves IMDb IDs through `external_ids`.
3. `imdb.py` stream-parses the official `title.ratings.tsv.gz`, keeping only the IMDb IDs it needs.
4. `assemble.py` (pure functions, which is what the unit tests cover) builds the output.

IMDb IDs are cached in `pipeline/cache/imdb_ids.json`, which is committed. An entry is `{"i": "tt…"}`, or `{"i": null, "t": <ts>}` when there was no ID; those are retried after 30 days. The workflow (`.github/workflows/build-data.yml`) runs the tests, builds the file, commits the changed cache with `[skip ci]`, and deploys `dist/` to Pages.

### App

There is no DI framework. `FlickpickApp.kt` defines `AppContainer`, which wires everything by hand.

- `data/MovieRepository` downloads `movies.json` and writes it atomically to `filesDir`. On a cold start it falls back to the cached copy, or in debug builds to the bundled seed `src/debug/assets/movies_seed.json` (git-ignored; copy it from `dist/`). The catalog is exposed as a `StateFlow`. After every successful `refresh()` it calls an `onRefreshed` hook, and that hook is how the watchlist check runs.
- `work/WatchlistCheckWorker` is a WorkManager job that runs every 12 h and just calls `repository.refresh()`. The hook then calls `domain/WatchlistChecker`, which applies `diffWatchlist` (pure, in `WatchlistDiff.kt`), saves status changes (WAITING → AVAILABLE) and sends notifications through `notify/Notifier`. Debug builds have **⋮ → Run watchlist check now**.
- `data/watchlist/Watchlist.kt` holds the Room DB `watchlist.db` with the `watchlist` and `seen` tables. It has hand-written migrations and `exportSchema = false`. When you change the schema, bump `version` and add a `Migration`.
- `data/FilterPrefs` stores the filter and sort state (`domain/FilterState`) in DataStore Preferences.
- `data/TmdbSearchApi` is the app's only direct TMDB call: the movie search for the watchlist. It uses `BuildConfig.TMDB_TOKEN`.
- `domain/` holds pure Kotlin logic (`MovieFilter`, `WatchlistDiff`, `Format`). It is what the JVM unit tests in `src/test` cover.
- `ui/`: `MainViewModel` combines the catalog, filters, watchlist and seen IDs. The Compose screens are in `MainScreen`, `WatchlistTab`, `Sheets` and `AboutScreen`; shared components such as `SwipeAction` and `RatingRing` are in `ui/components`.

`DATA_URL` and `TMDB_TOKEN` are read from `local.properties` or environment variables and baked into `BuildConfig` by `app/app/build.gradle.kts`.

## Constraints

- Availability means Netflix **DE**, flat-rate (subscription) only. Ratings come only from the IMDb non-commercial dataset.
- Keep pipeline dependencies minimal (`requests` plus the standard library).
- Keep the TMDB/JustWatch/IMDb attribution (README and About screen).
