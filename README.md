# Where to Eat 0.2.0

Recommends where to eat in **Alabang** and **BGC**, ranked by the best credit-card dining promo for the cards you own.

**The app to install:** `WhereToEat-0.2.0.apk` (signed). Every build lands by itself in Google Drive under **My Drive > Projects > Where to Eat**; open it from the phone's Drive app, allow "Install unknown apps" once, and tap Install.

## Using it
1. First launch: tick the credit cards you own, or the cards of whoever you're eating with (**My cards**, also under the gear icon).
2. Pick a **Location** from the dropdown (Alabang, BGC; new areas appear by themselves when the feed has them).
3. Each restaurant appears once, with its logo and its best deal for your cards: the discount, **Works today** (or e.g. "Fri & Sat only"), minimum spend, when it ends (amber within a week), and the bank's logo with your card. "+1 more deal" means another bank or card also has a promo there.
4. **Search** by restaurant name; tap the **star** to save favorites and the **Favorites** chip to show only them.
5. **Surprise me** picks one of the top ten deals that work today. Tap a restaurant for every deal's terms, branches, and **Open in Maps**.
6. Pull the list down (or tap refresh) to get the latest promos. A saved copy keeps the app working offline.

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
