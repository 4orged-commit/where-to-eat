# Where to Eat 0.6.1

Recommends where to eat in **Alabang** and **BGC**, ranked by the best credit-card dining promo for the cards you own.

**The app to install:** `WhereToEat-0.6.1.apk` (signed). Every build lands by itself in Google Drive under **My Drive > Projects > Where to Eat**; open it from the phone's Drive app, allow "Install unknown apps" once, and tap Install.

## Using it
1. First launch: tick the credit cards you own, or the cards of whoever you're eating with (**My cards**, also under the gear icon).
2. Pick a **Location** from the chip at the top (Alabang, BGC; new areas appear by themselves when the feed has them).
3. Each restaurant is one compact row: logo, name, the bank's logo with your card, the day rule only when it's limited (e.g. "Fri & Sat only"), minimum spend, and "Ends in N days" in amber within a week. The discount is on the right, dimmed if the deal doesn't work today. "+1 more deal" means another bank or card also has a promo there.
4. **Search** (magnifier in the top bar) by restaurant name; tap the **star** on a restaurant's page to save it as a favorite and the **Favorites** chip to show only them. The **bank chips** (with logos) under search show only those banks' deals. **NEW** marks deals that appeared since you last opened the app.
5. Tap a restaurant for every deal's terms, branches, **Directions** for each branch, **Full terms**, and **Share** (sends the deal, card, days, address and a map link to Messenger, Viber, etc.).
6. **Settings** (gear): **Appearance** switches System / Light / Dark (the new look spreads out in a circle from your tap) and the colours between **Classic** (warm ivory/charcoal with wine and gold) and **Material You**; below it, **My cards**.
7. The header shrinks as you scroll; taps on favorites, filters and theme give a light vibration.
8. Pull the list down (or tap refresh) to get the latest promos. A saved copy keeps the app working offline.

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
