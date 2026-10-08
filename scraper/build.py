"""Builds docs/promos.json (the feed the Android app downloads) from every bank.

- Metrobank is read automatically by metrobank.py.
- BDO and UnionBank block automated readers, so their promos live in data/manual.json,
  checked by hand from the banks' public promo pages and merged in here.
A bank that fails keeps its previous promos in the feed and is marked stale, so one broken
scraper never empties the app.
"""
import json
import pathlib
import traceback
from datetime import datetime, timezone

import metrobank

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "promos.json"
MANUAL = ROOT / "data" / "manual.json"
SCRAPERS = {"Metrobank": metrobank.run}

# The cards the app lets you tick. Promos with an empty "cards" list apply to all of a bank's cards.
CARDS = {
    "Metrobank": ["Cashback Visa", "Femme Signature Visa", "M Free Mastercard", "PSBank Credit Mastercard",
                  "Platinum Mastercard", "Rewards Plus Visa", "Titanium Mastercard", "Toyota Mastercard",
                  "Travel Platinum Visa", "Travel Signature Visa", "World Mastercard", "Dollar Mastercard"],
    "BDO": ["BDO Visa", "BDO Mastercard", "BDO JCB", "BDO UnionPay", "BDO Elite"],
    "UnionBank": ["Rewards Visa Platinum", "Miles Visa Platinum", "Cashback Mastercard",
                  "Lazada Mastercard", "Gold Visa", "Other UnionBank credit card"],
}


def main():
    old = json.loads(OUT.read_text(encoding="utf8")) if OUT.exists() else {"promos": [], "banks": {}}
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    promos, banks = [], {}
    for bank, run in SCRAPERS.items():
        try:
            found = run()
            promos += found
            banks[bank] = {"status": "ok", "updated": now, "count": len(found)}
        except Exception:
            traceback.print_exc()
            kept = [p for p in old["promos"] if p["bank"] == bank]
            promos += kept
            prev = old["banks"].get(bank, {})
            banks[bank] = {"status": "stale", "updated": prev.get("updated"), "count": len(kept)}

    manual = json.loads(MANUAL.read_text(encoding="utf8")) if MANUAL.exists() else {"banks": {}, "promos": []}
    promos += manual["promos"]
    for bank, info in manual["banks"].items():
        n = sum(1 for p in manual["promos"] if p["bank"] == bank)
        banks[bank] = {"status": "manual", "updated": info.get("checked"), "count": n}

    OUT.parent.mkdir(exist_ok=True)
    OUT.write_text(json.dumps({"updated": now, "banks": banks, "cards": CARDS, "promos": promos},
                              indent=1, ensure_ascii=False), encoding="utf8")
    print(f"{len(promos)} promos written; banks: { {k: (v['status'], v['count']) for k, v in banks.items()} }")


if __name__ == "__main__":
    main()
