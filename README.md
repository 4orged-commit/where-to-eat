# Where to Eat

Recommends where to eat in **Alabang** and **BGC** based on the credit-card dining promos you can use.

- `scraper/`: reads each bank's public promos page and builds the feed. `metrobank.py` reads the structured data Metrobank publishes in its own promos page.
- `docs/promos.json`: the feed, refreshed daily at 6:00 (Manila) by GitHub Actions (`.github/workflows/scrape.yml`) and served free by GitHub Pages.
- `android/`: the phone app (to come). It downloads the feed and keeps a copy for offline use.

## Banks
| Bank | Status |
|---|---|
| Metrobank | Working |
| BDO | Not yet: its deals site is a separate platform that needs a decision on how to read it |
| UnionBank | Not yet: its site blocks automated reading |
