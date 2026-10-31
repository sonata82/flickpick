# Flickpick

Movies currently on **Netflix Germany**, sorted by **IMDb rating**, with genre filters and a watchlist that notifies you when a movie arrives. Personal, non-commercial use only.

```
pipeline/   Python script, run daily by GitHub Actions → movies.json on GitHub Pages
app/        Android app (Kotlin, Jetpack Compose) that downloads and browses movies.json
```

Data URL: `https://sonata82.github.io/flickpick/movies.json`

## 1. Get a TMDB token

1. Create a free account at <https://www.themoviedb.org> and request an API key under
   **Settings → API** (choose "Developer", personal use).
2. Copy the **API Read Access Token** (the long `eyJ…` string, *not* the short "API Key").

## 2. Run the pipeline locally

Requires Python 3.12+.

```sh
pip install -r pipeline/requirements.txt
export TMDB_TOKEN=eyJ...
python pipeline/build.py --out dist/movies.json      # add --pretty for readable output
python -m unittest discover -s pipeline/tests
```

It logs how many movies it found, how many have an IMDb ID, and how many have a rating. TMDB → IMDb ID lookups are cached in
`pipeline/cache/imdb_ids.json` (committed), so only new movies are looked up. The first run takes ~4 minutes; later runs
take seconds.

Titles and overviews are English (`en-US`). Availability is Netflix **DE**, subscription (flat-rate) only. Ratings come from
the official IMDb dataset `title.ratings.tsv.gz`.

## 3. GitHub Actions + Pages

1. Repository **Settings → Secrets and variables → Actions → New repository secret**: `TMDB_TOKEN` = your token.
   (CLI: `gh secret set TMDB_TOKEN`)
2. **Settings → Pages → Build and deployment → Source: GitHub Actions**.
3. **Actions → Build movies.json → Run workflow** (it also runs daily at 04:00 UTC).
4. Check: `curl -sI https://sonata82.github.io/flickpick/movies.json`

The workflow runs the unit tests, builds `movies.json`, commits the updated IMDb-ID cache (`[skip ci]`) and deploys `dist/`
to Pages.

## 4. Build and install the app

Requires Android Studio (or JDK 17+ and the Android SDK, platform 36).

Create `app/local.properties` (git-ignored):

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
TMDB_TOKEN=eyJ...
# optional: point the app somewhere else, e.g. a local server during development
# DATA_URL=http://10.0.2.2:8000/movies.json
```

`TMDB_TOKEN` is only used for the watchlist's movie search. The catalog comes from `movies.json` only.

```sh
cd app
./gradlew assembleDebug testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the `app/` folder in Android Studio and press Run.

Debug builds include a bundled seed copy of the catalog if you put one at `app/app/src/debug/assets/movies_seed.json`
(e.g. `cp dist/movies.json app/app/src/debug/assets/movies_seed.json`). It is used on first launch before the first
download, and it is git-ignored.

### Testing the watchlist notification

Debug builds have **⋮ → Run watchlist check now**, which runs the background job immediately. To see a notification:
add a movie that isn't on Netflix yet, then serve a modified `movies.json` that contains its TMDB ID (via `DATA_URL`), or
wait for it to arrive in the real catalog.

## Attribution

This product uses the TMDB API but is not endorsed or certified by TMDB. Streaming availability data by JustWatch (via
TMDB). Ratings: IMDb non-commercial datasets, courtesy of IMDb (<https://www.imdb.com>), for personal, non-commercial
use.
