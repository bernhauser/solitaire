#!/usr/bin/env python3
"""Generate the 52 playing-card face SVGs in app/src/main/assets/cards/.

Design ("banner" style, chosen 2026-07-05 for readability on small screens
without reading glasses):
  * Top banner (upright only, no rotated bottom copy): huge rank glyph
    (Arial Black) followed by the suit symbol, spanning the card's top strip
    so buried tableau cards stay readable.
  * A / 2..10  -> one large suit symbol in the center.
  * J / Q / K  -> large flat-style head in the center:
                    K = gold crown worn low + full grey beard
                    Q = tiara on long brown hair, red lips, earrings
                    J = narrow face, red slouch cap with purple feather,
                        thin golden mustache + pointed goatee
  * Suit symbols are custom paths with sharper points and deeper indrawn
    "bays" than the standard glyphs.

Each card is drawn as a pure-vector SVG, rasterized here to a 720x1008 PNG
(so the committed asset doesn't depend on fonts being present at build time),
quantized, and wrapped in a tiny self-contained SVG (one base64 <image>), so
the existing Gradle `generateCardPngAssets` task keeps working unchanged
under both rsvg-convert and magick.

Run from the repo root:  python3 tools/generate_cards.py [KEYS...]
Requires `rsvg-convert` and `magick` (ImageMagick) on PATH.
"""
import base64
import os
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CARDS = os.path.join(REPO, "app", "src", "main", "assets", "cards")

W, H = 720, 1008  # rasterization target, 3px per SVG user unit

BLACK = "#1a1a1a"
RED = "#C8102E"
SUITS = {"C": BLACK, "D": RED, "H": RED, "S": BLACK}
RANKS = ["A", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K"]
COURT = {"J", "Q", "K"}
RANK_TEXT = {"T": "10"}

FONT = 'font-family="Arial Black" font-weight="900"'

# Suit glyphs, drawn in a local space vertically centered on (0,0).
SUIT_GLYPHS = {
    "S": (106, (
        '<path d="M0 -53 C2 -42 4 -30 6 -20 C8 -8 12 0 19 -2 C29 -5 40 -2 44 8 '
        'C48 20 41 34 28 37.5 C17 40 7 34 2 24 L0 20 L-2 24 C-7 34 -17 40 -28 37.5 '
        'C-41 34 -48 20 -44 8 C-40 -2 -29 -5 -19 -2 C-12 0 -8 -8 -6 -20 '
        'C-4 -30 -2 -42 0 -53 Z '
        'M0 20 C3 34 7 45 15 53 L-15 53 C-7 45 -3 34 0 20 Z"/>'
    )),
    "H": (92, (
        '<path d="M0 -18 C-3 -30 -13 -46 -28 -46 C-44 -46 -50 -32 -50 -18 '
        'C-50 0 -30 18 0 46 C30 18 50 0 50 -18 C50 -32 44 -46 28 -46 '
        'C13 -46 3 -30 0 -18 Z"/>'
    )),
    "D": (100, (
        '<path d="M0 -50 C3 -30 10 -8 32 0 C10 8 3 30 0 50 C-3 30 -10 8 -32 0 '
        'C-10 -8 -3 -30 0 -50 Z"/>'
    )),
    "C": (95, (
        '<circle cx="0" cy="-26" r="19.5"/>'
        '<circle cx="-20.5" cy="6" r="19.5"/>'
        '<circle cx="20.5" cy="6" r="19.5"/>'
        '<path d="M0 4 C3 20 7 34 15 44 L-15 44 C-7 34 -3 20 0 4 Z"/>'
    )),
}

# Court head palette
SKIN, SHADE = "#F3C9A2", "#DDA877"
GOLD, GOLD2 = "#E5B32B", "#D4A017"
CRIMSON, HAIRBROWN, GREY, PURPLE = "#C0392B", "#7A4A21", "#8f8f8f", "#7D4FA8"

KING_HEAD = (
    f'<ellipse cx="0" cy="42" rx="58" ry="52" fill="{GREY}"/>'
    f'<ellipse cx="0" cy="-8" rx="47" ry="50" fill="{SKIN}"/>'
    f'<path d="M -50 -46 L -50 -88 L -25 -66 L 0 -98 L 25 -66 L 50 -88 L 50 -46 Z" fill="{GOLD}"/>'
    f'<circle cx="-50" cy="-90" r="6" fill="{GOLD}"/><circle cx="0" cy="-100" r="6" fill="{GOLD}"/>'
    f'<circle cx="50" cy="-90" r="6" fill="{GOLD}"/>'
    f'<circle cx="-28" cy="-56" r="5" fill="{CRIMSON}"/><circle cx="0" cy="-56" r="5" fill="#2E6DB4"/>'
    f'<circle cx="28" cy="-56" r="5" fill="{CRIMSON}"/>'
    '<circle cx="-17" cy="-14" r="4.5" fill="#333"/><circle cx="17" cy="-14" r="4.5" fill="#333"/>'
    '<path d="M -28 -27 L -8 -24" stroke="#5c5c5c" stroke-width="5" stroke-linecap="round" fill="none"/>'
    '<path d="M 28 -27 L 8 -24" stroke="#5c5c5c" stroke-width="5" stroke-linecap="round" fill="none"/>'
    f'<path d="M 0 -12 C -2 -2 -4 4 -6 8" stroke="{SHADE}" stroke-width="5" fill="none" stroke-linecap="round"/>'
    '<path d="M -2 18 C -10 10 -24 12 -28 22 C -18 26 -8 24 -2 18 Z" fill="#7d7d7d"/>'
    '<path d="M 2 18 C 10 10 24 12 28 22 C 18 26 8 24 2 18 Z" fill="#7d7d7d"/>'
)

QUEEN_HEAD = (
    f'<ellipse cx="0" cy="-14" rx="60" ry="58" fill="{HAIRBROWN}"/>'
    f'<path d="M -60 -10 C -64 40 -54 68 -38 82 C -30 60 -32 30 -30 12 Z" fill="{HAIRBROWN}"/>'
    f'<path d="M 60 -10 C 64 40 54 68 38 82 C 30 60 32 30 30 12 Z" fill="{HAIRBROWN}"/>'
    f'<ellipse cx="0" cy="-2" rx="40" ry="46" fill="{SKIN}"/>'
    f'<path d="M -36 -62 L -36 -78 L -18 -68 L 0 -86 L 18 -68 L 36 -78 L 36 -62 Z" fill="{GOLD}"/>'
    f'<circle cx="0" cy="-70" r="4.5" fill="{CRIMSON}"/>'
    '<circle cx="-15" cy="-8" r="4" fill="#333"/><circle cx="15" cy="-8" r="4" fill="#333"/>'
    '<path d="M -23 -19 C -18 -23 -10 -23 -7 -20" stroke="#5b3a1a" stroke-width="3.5" fill="none" stroke-linecap="round"/>'
    '<path d="M 23 -19 C 18 -23 10 -23 7 -20" stroke="#5b3a1a" stroke-width="3.5" fill="none" stroke-linecap="round"/>'
    f'<path d="M 0 -6 C -1 0 -2 4 -4 7" stroke="{SHADE}" stroke-width="4" fill="none" stroke-linecap="round"/>'
    f'<path d="M -10 22 C -5 18 5 18 10 22 C 5 28 -5 28 -10 22 Z" fill="{CRIMSON}"/>'
    f'<circle cx="-43" cy="18" r="4" fill="{GOLD}"/><circle cx="43" cy="18" r="4" fill="{GOLD}"/>'
)

JACK_HEAD = (
    f'<path d="M -38 -36 C -42 -18 -40 -6 -36 2 L -28 -2 C -32 -14 -32 -26 -32 -34 Z" fill="{GOLD2}"/>'
    f'<path d="M 38 -36 C 42 -18 40 -6 36 2 L 28 -2 C 32 -14 32 -26 32 -34 Z" fill="{GOLD2}"/>'
    f'<ellipse cx="0" cy="2" rx="33" ry="48" fill="{SKIN}"/>'
    f'<path d="M 28 -60 C 42 -82 56 -88 64 -86 C 60 -72 48 -60 34 -54 Z" fill="{PURPLE}"/>'
    f'<path d="M -38 -38 C -50 -78 50 -78 38 -38 Z" fill="{CRIMSON}"/>'
    '<rect x="-40" y="-44" width="80" height="11" rx="5.5" fill="#8E2B21"/>'
    '<circle cx="-13" cy="-6" r="4" fill="#333"/><circle cx="13" cy="-6" r="4" fill="#333"/>'
    '<path d="M -20 -16 L -7 -17" stroke="#8a6510" stroke-width="3.5" stroke-linecap="round" fill="none"/>'
    '<path d="M 20 -16 L 7 -17" stroke="#8a6510" stroke-width="3.5" stroke-linecap="round" fill="none"/>'
    f'<path d="M 0 -4 C -1 2 -2 6 -3 9" stroke="{SHADE}" stroke-width="4" fill="none" stroke-linecap="round"/>'
    f'<path d="M -3 22 C -8 19 -14 19 -18 23" stroke="{GOLD2}" stroke-width="3" fill="none" stroke-linecap="round"/>'
    f'<path d="M 3 22 C 8 19 14 19 18 23" stroke="{GOLD2}" stroke-width="3" fill="none" stroke-linecap="round"/>'
    '<path d="M -8 28 C -3 31 3 31 8 28" stroke="#B06A4A" stroke-width="3.5" fill="none" stroke-linecap="round"/>'
    f'<path d="M -8 42 L 0 64 L 8 42 Z" fill="{GOLD2}"/>'
)

HEADS = {"K": (KING_HEAD, 0.9), "Q": (QUEEN_HEAD, 1.0), "J": (JACK_HEAD, 1.05)}


# Optical-size corrections: the heart fills its bounding box more than the
# other suits and reads too large at equal height, so render it slightly
# smaller; the diamond is narrow, so bow its sides 5% further out.
SUIT_SCALE = {"H": 0.80}
SUIT_XSCALE = {"D": 1.05}


def suit_glyph(suit, cx, cy, height):
    glyph_h, inner = SUIT_GLYPHS[suit]
    k = height * SUIT_SCALE.get(suit, 1.0) / glyph_h
    kx = k * SUIT_XSCALE.get(suit, 1.0)
    return (f'<g transform="translate({cx} {cy}) scale({kx:.4f} {k:.4f})" '
            f'fill="{SUITS[suit]}">{inner}</g>')


# All banner symbols share one center axis so they line up in a tableau
# column. The widest glyph (heart) still keeps a ~16-unit right margin.
BANNER_SUIT_CX = 183


def banner_suit_glyph(suit, cy, height):
    return suit_glyph(suit, BANNER_SUIT_CX, cy, height)


def card_svg(rank, suit):
    color = SUITS[suit]
    text = RANK_TEXT.get(rank, rank)
    if len(text) == 1:
        rank_el = f'<text x="18" y="94" font-size="98" {FONT} fill="{color}">{text}</text>'
    else:  # "10" needs to squeeze in before the banner suit symbol
        rank_el = (f'<text x="14" y="94" font-size="86" letter-spacing="-5" '
                   f'{FONT} fill="{color}">{text}</text>')
    if rank in COURT:
        head, scale = HEADS[rank]
        center = f'<g transform="translate(120 222) scale({scale})">{head}</g>'
    else:
        center = suit_glyph(suit, 120, 212, 190)
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 336" '
        f'width="{W}" height="{H}">'
        '<rect x="3" y="3" width="234" height="330" rx="18" '
        'fill="#ffffff" stroke="#555555" stroke-width="3"/>'
        f'{rank_el}{banner_suit_glyph(suit, 58, 86)}{center}'
        '</svg>'
    )


def run(*args):
    subprocess.run(args, check=True)


def wrap_svg(png_path, key, out_path):
    with open(png_path, "rb") as f:
        b64 = base64.b64encode(f.read()).decode("ascii")
    svg = (
        '<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n'
        '<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'class="card" face="{key}" width="2.5in" height="3.5in" '
        'preserveAspectRatio="none" viewBox="-120 -168 240 336">'
        f'<image x="-120" y="-168" width="240" height="336" '
        f'xlink:href="data:image/png;base64,{b64}"/>'
        '</svg>\n'
    )
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(svg)


def main():
    only = set(sys.argv[1:])  # optional: generate just these keys (e.g. AH 5H KH)
    with tempfile.TemporaryDirectory() as tmp:
        for suit in SUITS:
            for rank in RANKS:
                key = f"{rank}{suit}"
                if only and key not in only:
                    continue
                src = os.path.join(tmp, f"{key}.svg")
                raw = os.path.join(tmp, f"{key}_raw.png")
                png = os.path.join(tmp, f"{key}.png")
                with open(src, "w", encoding="utf-8") as f:
                    f.write(card_svg(rank, suit))
                run("rsvg-convert", "-w", str(W), "-h", str(H), src, "-o", raw)
                # Flat-color art: quantize + max-compress to keep assets small.
                colors = "64" if rank in COURT else "32"
                run("magick", raw, "-strip", "-colors", colors,
                    "-define", "png:compression-level=9", f"PNG32:{png}")
                wrap_svg(png, key, os.path.join(CARDS, f"{key}.svg"))
                print(f"generated {key}")


if __name__ == "__main__":
    main()
