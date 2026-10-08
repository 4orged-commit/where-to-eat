"""Metrobank dining promos for Alabang and BGC.

Metrobank's public promos page (https://www.metrobank.com.ph/promos) ships every promo as
structured JSON inside the page itself (Next.js __NEXT_DATA__), so a normal page download is
enough: no browser, no login, no private API.

About half the running dining promos come without a branch list on that page (chains, hotels,
"selected restaurants"). For those, the promo's own public page is read once and its terms are
searched for Alabang/BGC addresses.
"""
import html as htmllib
import json
import re
import time
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
    # Not plain "high street": a "High Street Grill" exists in GenSan.
    "BGC": ["bgc", "bonifacio", "global city", "uptown", "serendra", "burgos circle", "forbes town", "venice grand canal",
            "mckinley west", "mckinley hill", "high street central", "high street south", "market! market", "stopover"],
    # "spectrum midway", not "spectrum": Fairmont Makati has a restaurant called Spectrum.
    "Alabang": ["alabang", "muntinlupa", "filinvest", "madrigal", "westgate", "festival mall", "molito", "ayala alabang",
                "southvale", "spectrum midway"],
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


def _compact(s):
    return re.sub(r"[^a-z0-9]", "", (s or "").lower())


def _tokens(s):
    return {t for t in re.findall(r"[a-z0-9]+", (s or "").lower()) if len(t) >= 4}


def _matches(label, title):
    """True when a merchant/branch label plausibly names the same place as the promo title."""
    c = _compact(label)
    return bool(c) and (c in _compact(title) or bool(_tokens(label) & _tokens(title)))


def name_from_title(title):
    """'50% OFF at Domino's Pizza' -> "Domino's Pizza". Used when Metrobank's merchant label is wrong."""
    return re.split(r"\b(?:at|OFF|on)\b\s*", title, flags=re.I)[-1].strip(" -–") or title


def _next_data(url):
    html = requests.get(url, headers={"User-Agent": UA}, timeout=90).text
    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, re.S)
    if not m:
        raise RuntimeError(f"{url} no longer contains __NEXT_DATA__")
    return json.loads(m.group(1))["props"]["pageProps"]


def fetch():
    return _next_data(PAGE)["data"]


def _detail_cells(slug):
    """The promo page's terms as plain text pieces (table cells and paragraphs)."""
    raw = json.dumps(_next_data(PAGE + slug), ensure_ascii=False)
    raw = raw.replace("\\n", "  ").replace("\\t", "  ").replace("\\r", " ").replace('\\"', '"')
    text = htmllib.unescape(re.sub(r"<[^>]+>", "  ", raw)).replace("\xa0", " ")
    return [c.strip(' ",') for c in re.split(r"\s{2,}|\",\"", text) if c.strip(' ",')]


_phone = re.compile(r"^[\d\s()+\-/]{7,}$")
# Roundup pages ("November Metro Deal Guide", "Dine, Shop, and Celebrate Cebu") list other promos; skip them.
_roundup = re.compile(r"deal guide|dining deals|dine,? (?:and )?shop|hotels (?:and dining )?in |wanderlist|deals in ", re.I)
_page_chrome = ("got questions", "seometatags", "__typename", "breadcrumb", "backgroundcolor")


def _area_in(text):
    low = text.lower().replace("bonifacio day", "")  # the November 30 holiday, listed in blackout dates
    return next((a for a, words in WORDS.items() if any(w in low for w in words)), None)


def _good_name(c):
    c = c.strip()
    return (0 < len(c) < 60 and not _phone.match(c) and "," not in c and not c.endswith("?")
            and not re.match(r"^(\W|\w?\d*\.|what|where|how|got|branch|address|contact)", c, re.I))


def detail_branches(p):
    """Alabang/BGC locations named in a promo's own page, for promos without a branch list."""
    if _roundup.search(p["title"]):
        return []
    merchant = name_from_title(p["title"].replace("​", ""))
    cells = _detail_cells(p["slug"])
    found, seen = [], set()
    for i, cell in enumerate(cells):
        low = cell.lower()
        area = _area_in(cell)
        if not area or len(cell) < 12 or any(c in low for c in _page_chrome):
            continue
        nxt = cells[i + 1] if i + 1 < len(cells) else ""
        if "," not in cell and "," in nxt and _area_in(nxt) == area:
            continue  # a branch name ("BGC High Street Central"); its address is the next cell
        if len(cell) > 160:  # a paragraph: keep the part right after "located at", else just name the area
            start = low.find("located at")
            address = cell[start + 11:start + 140].split(".")[0] if start >= 0 else f"{merchant}, {area}"
        else:
            address = re.sub(r"^(the )?promo is available at ", "", cell, flags=re.I)
        address = address.strip(" .,")
        # In branch tables the cell before the address (skipping phone numbers) is the branch name.
        name = next((c for c in reversed(cells[max(0, i - 3):i]) if _good_name(c)), "")
        key = (area, address.lower())
        if key in seen:
            continue
        seen.add(key)
        # Group promos ("50% OFF at Bistro Group restaurants") name the actual restaurant per branch.
        place = name if name and re.search(r"restaurants|group", merchant, re.I) else merchant
        found.append({"merchant": place, "name": name, "address": address, "area": area, "lat": None, "lon": None})
    return found


def _thumb(p):
    url = (p.get("cardLogoImage") or p.get("cardBannerImage") or {}).get("url")
    if not url:
        return None
    # Most dining thumbnails are 1144x644 panels: Metrobank's "Metro Dining Deals" badge on the left, the restaurant's
    # logo on the right. The image server (imgix) can cut out just the logo square.
    if re.search(r"mdd|1144-x-644|1144x644", url):
        return f"{url}?rect=552,26,592,592&auto=format&fm=png&w=256"
    return f"{url}?auto=format&fm=png&w=320"


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
        if not p.get("participatingBranches"):
            try:
                branches = detail_branches(p)
            except Exception as e:  # one unreadable promo page shouldn't sink the rest
                print("detail page failed:", p["slug"], e)
            time.sleep(0.5)  # be gentle: these are extra page loads
        for br in p.get("participatingBranches") or []:
            g = geo.get(br["id"])
            area = area_of(br, g)
            if area:
                merchant = (br.get("merchant") or {}).get("name")
                if not (_matches(merchant, p["title"]) or _matches(br.get("name"), p["title"])):
                    # Metrobank's data sometimes pairs a promo with another business's branch. A generic
                    # branch label ("BGC") is most likely this promo's own branch; a full different name is not.
                    if len(br.get("name") or "") > 12:
                        continue
                    merchant = name_from_title(p["title"])
                elif not _matches(merchant, p["title"]):
                    merchant = name_from_title(p["title"])
                branches.append({
                    "merchant": merchant,
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
            "title": p["title"].replace("​", "").strip(),
            "description": (p.get("description") or "").replace("​", "").strip(),
            "cards": sorted({c["title"] for c in p.get("cards", []) if "Credit Card" in
                             [t["name"] for t in c.get("cardTypes", [])]}),
            "start": p.get("startDate"),
            "end": end,
            "url": "https://www.metrobank.com.ph/promos" + p["slug"],
            # The promo's thumbnail: the restaurant's logo on a white panel (shown as the place's logo in the app).
            "image": _thumb(p),
            "branches": branches,
        })
    return out


if __name__ == "__main__":
    promos = run()
    print(len(promos), "Metrobank dining promos in Alabang/BGC")
    print(json.dumps(promos[:2], indent=1, ensure_ascii=False))
