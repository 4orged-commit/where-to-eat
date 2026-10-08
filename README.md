# Where to Eat 0.1.1

Recommends where to eat in **Alabang** and **BGC**, ranked by the best credit-card dining promo for the cards you own.

**The app to install:** `WhereToEat-0.1.1.apk` (signed). Every build lands by itself in Google Drive under **My Drive > Projects > Where to Eat**; open it from the phone's Drive app, allow "Install unknown apps" once, and tap Install.

## Using it
1. First launch: tick the credit cards you own (**My cards**, also under the gear icon).
2. Pick **Alabang** or **BGC**. Deals are ranked best-first and only show promos your cards can use and that are running today.
3. **Surprise me** picks one of the top ten. Tap a deal for the terms, branches, and **Open in Maps**.
4. The app downloads the latest promos when you open it (and with the refresh button) and keeps a saved copy, so it also works offline.

## Where the promos come from
| Bank | How | Fresh |
|---|---|---|
| Metrobank | Read automatically from the data in Metrobank's public promos page, daily at 6:00 Manila by GitHub Actions | Daily |
| BDO | Checked by hand from BDO's public promo pages and kept in `data/manual.json` | When refreshed |
| UnionBank | Same. UnionBank's site blocks automated reading | When refreshed |

The app's footer shows when each bank was last updated or checked.

**Refreshing BDO / UnionBank:** ask Claude to "refresh BDO and UnionBank". It re-checks the public pages, edits `data/manual.json`, and pushes. **Refreshing Metrobank on demand:** GitHub repo > Actions > Refresh promos > Run workflow.

## Layout
- `android/`: the app (Kotlin, Jetpack Compose, no database; the feed is a small JSON file).
- `scraper/`: `metrobank.py` (the Metrobank reader) and `build.py` (builds the feed, adds the hand-checked file).
- `docs/promos.json`: the feed, served free by GitHub Pages at https://4orged-commit.github.io/where-to-eat/promos.json
- `data/manual.json`: hand-checked BDO and UnionBank snapshots.

Signing uses the same key folder as Ledger (`C:\Users\gelor\BudgetTrackerKeys`), kept outside the project.
