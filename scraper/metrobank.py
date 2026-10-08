"""Metrobank dining promos for Alabang and BGC.

Metrobank's public promos page (https://www.metrobank.com.ph/promos) ships every promo as
structured JSON inside the page itself (Next.js __NEXT_DATA__), so a normal page download is
enough: no browser, no login, no private API.
"""
import json
import re
from datetime import datetime, timezone

import requests

PAGE = "https://www.metrobank.com.ph/promos"
UA = "WhereToEat/1.0 (personal app; reads the public promos page once a day)"

# (min_lat, max_lat, min_lon, max_lon)
BOXES = {
    "BGC": (14.510, 14.570, 121.030, 121.070),
    "Alabang": (14.390, 14.450, 121.010, 121.060),
}
WORDS = {
    "BGC": ["bgc", "bonifacio", "global city", "uptown", "serendra", "burgos circle", "forbes town", "venice grand canal", "mckinley west"],
    "Alabang": ["alabang", "muntinlupa", "filinvest", "madrigal", "westgate", "festival mall", "molito", "ayala alabang", "southvale", "spectrum"],
}


def area_of(branch, geo):
    if geo:
        for name, (a, b, c, d) in BOXES.items():
            if a <= geo["latitude"] <= b and c <= geo["longitude"] <= d:
                return name
    text = f"{branch.get('name', '')} {branch.get('address', '')}".lower()
    for name, words in WORDS.items():
        if any(w in text for w in words):
            return name
    return None


def fetch():
    html = requests.get(PAGE, headers={"User-Agent": UA}, timeout=90).text
    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, re.S)
    if not m:
        raise RuntimeError("Metrobank page no longer contains __NEXT_DATA__")
    return json.loads(m.group(1))["props"]["pageProps"]["data"]


def run():
    data = fetch()
    geo = {}
    for b in data.get("merchants", []):
        if b.get("geolocation"):
            geo[b["id"]] = b["geolocation"]
    now = datetime.now(timezone.utc)
    out = []
    for p in data["promoDetails"]:
        if not any(c["category"] == "Dining" for c in p.get("categories", [])):
            continue
        end = p.get("expirationDate")
        if end and datetime.fromisoformat(end) < now:
            continue
        branches = []
        for br in p.get("participatingBranches") or []:
            g = geo.get(br["id"])
            area = area_of(br, g)
            if area:
                branches.append({
                    "merchant": (br.get("merchant") or {}).get("name"),
                    "name": br.get("name"),
                    "address": br.get("address"),
                    "area": area,
                    "lat": g["latitude"] if g else None,
                    "lon": g["longitude"] if g else None,
                })
        if not branches:
            continue
        out.append({
            "id": "metrobank:" + p["id"],
            "bank": "Metrobank",
            "title": p["title"],
            "description": p.get("description"),
            "cards": sorted({c["title"] for c in p.get("cards", []) if "Credit Card" in
                             [t["name"] for t in c.get("cardTypes", [])]}),
            "start": p.get("startDate"),
            "end": end,
            "url": "https://www.metrobank.com.ph/promos" + p["slug"],
            "branches": branches,
        })
    return out


if __name__ == "__main__":
    promos = run()
    print(len(promos), "Metrobank dining promos in Alabang/BGC")
    print(json.dumps(promos[:2], indent=1, ensure_ascii=False))
