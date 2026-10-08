"""Builds docs/promos.json (the feed the Android app downloads) from every bank scraper.

A bank that fails keeps its previous promos in the feed and is marked stale, so one broken
scraper never empties the app.
"""
import json
import pathlib
import traceback
from datetime import datetime, timezone

import metrobank

OUT = pathlib.Path(__file__).resolve().parent.parent / "docs" / "promos.json"
SCRAPERS = {"Metrobank": metrobank.run}
# Banks wanted but without a scraper yet; shown in the app as "not available".
PENDING = ["BDO", "UnionBank"]


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
    for bank in PENDING:
        banks[bank] = {"status": "unavailable", "updated": None, "count": 0}
    OUT.parent.mkdir(exist_ok=True)
    OUT.write_text(json.dumps({"updated": now, "banks": banks, "promos": promos},
                              indent=1, ensure_ascii=False), encoding="utf8")
    print(f"{len(promos)} promos written; banks: { {k: v['status'] for k, v in banks.items()} }")


if __name__ == "__main__":
    main()
