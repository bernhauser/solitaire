#!/usr/bin/env python3
"""Generate the 52 playing-card face SVGs in app/src/main/assets/cards/ from the
purchased card-art kit (tools/card-art/).

Design (matches the app's readable, big-center style):
  * Corners (top-left + rotated bottom-right): new rank glyph above new suit symbol.
  * Ace        -> big central suit symbol.
  * 2..10      -> big central rank number (new typeface).
  * J / Q / K  -> existing vector court portrait, with the old numeric corner
                  ("11"/"12"/"13") + old corner suit stripped and replaced by the
                  new-style corner.

Each generated card is flattened to a 720x1008 PNG (the build's rasterization
target) and wrapped in a tiny self-contained SVG (one base64 <image>), so the
existing Gradle `generateCardPngAssets` task keeps working unchanged under both
rsvg-convert and magick.

Backs (1B/2B) are intentionally left untouched.

Run from the repo root:  python3 tools/generate_cards.py
Requires `magick` (ImageMagick) and `rsvg-convert` on PATH.

NOTE: court cards (J/Q/K) are edited from their *committed* vector portrait
source, so run this on a clean tree (`git restore app/src/main/assets/cards`)
before regenerating, otherwise the new corner would be injected twice.
"""
import base64
import os
import re
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(REPO, "tools", "card-art")
CARDS = os.path.join(REPO, "app", "src", "main", "assets", "cards")

W, H = 720, 1008
RADIUS = 44

# rank symbol used in asset keys -> glyph filename in the kit
RANK_GLYPH = {
    "A": "A", "2": "2", "3": "3", "4": "4", "5": "5", "6": "6", "7": "7",
    "8": "8", "9": "9", "T": "10", "J": "J", "Q": "Q", "K": "K",
}
SUITS = {  # symbol -> (suit png stem, color)
    "C": ("clubs", "black"),
    "D": ("diamonds", "red"),
    "H": ("hearts", "red"),
    "S": ("spades", "black"),
}
COURT = {"J", "Q", "K"}
NUMBERS = ["2", "3", "4", "5", "6", "7", "8", "9", "T"]


def run(*args):
    subprocess.run(args, check=True)


def suit_png(stem):
    return os.path.join(ART, "suits", f"{stem}.png")


def rank_png(rank_glyph, color):
    return os.path.join(ART, f"numbers-{color}", f"{rank_glyph}.png")


def build_corner_stamp(rank_glyph, suit_stem, color, out):
    """A transparent block holding the rank glyph above the suit symbol."""
    cw, ch = 160, 220
    r = rank_png(rank_glyph, color)
    s = suit_png(suit_stem)
    with tempfile.TemporaryDirectory() as tmp:
        rp = os.path.join(tmp, "r.png")
        sp = os.path.join(tmp, "s.png")
        run("magick", r, "-resize", "x118", rp)
        run("magick", s, "-resize", "x82", sp)
        run(
            "magick", "-size", f"{cw}x{ch}", "xc:none",
            rp, "-gravity", "North", "-geometry", "+0+2", "-composite",
            sp, "-gravity", "North", "-geometry", "+0+128", "-composite",
            # force RGBA storage so red glyphs aren't optimized to grayscale
            f"PNG32:{out}",
        )


def base_white_card(out):
    run(
        "magick", "-size", f"{W}x{H}", "xc:none",
        "-fill", "white", "-stroke", "black", "-strokewidth", "4",
        "-draw", f"roundrectangle 4,4 {W-4},{H-4} {RADIUS},{RADIUS}",
        # force RGBA storage so later red composites keep their color
        f"PNG32:{out}",
    )


def strip_court_corners(svg_text):
    """Remove the old numeric corner index and the old corner suit symbol."""
    # numeric value glyph in the top-left corner (e.g. x="-122" y="-156");
    # these <use> elements are written as <use ...></use> pairs, so consume both.
    svg_text = re.sub(r'<use\b[^>]*\bx="-122"[^>]*\by="-156"[^>]*>(\s*</use>)?', "", svg_text)
    # corner suit symbol (e.g. x="-115.473" ... y="-81")
    svg_text = re.sub(r'<use\b[^>]*\bx="-115\.473"[^>]*>(\s*</use>)?', "", svg_text)
    return svg_text


def b64_file(path):
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("ascii")


# corner stamp placement in the card's user-space (viewBox -120 -168 240 336),
# matching the raster cards' NorthWest +28+30 of a 160x220 stamp at 3px/unit.
CORNER_X, CORNER_Y, CORNER_W, CORNER_H = -110.667, -158.0, 53.333, 73.333


def emit_court_svg(rank, suit):
    """Keep the existing vector court portrait; swap its old numeric corner for
    the new-style corner (rank glyph + suit) embedded as a small base64 image."""
    suit_stem, color = SUITS[suit]
    rank_glyph = RANK_GLYPH[rank]
    key = f"{rank}{suit}"
    src = os.path.join(CARDS, f"{key}.svg")
    with open(src, encoding="utf-8") as f:
        svg = f.read()
    svg = strip_court_corners(svg)
    with tempfile.TemporaryDirectory() as tmp:
        stamp = os.path.join(tmp, "stamp.png")
        small = os.path.join(tmp, "stamp_small.png")
        build_corner_stamp(rank_glyph, suit_stem, color, stamp)
        run("magick", stamp, "-strip", "-colors", "16",
            "-define", "png:compression-level=9", small)
        b64 = b64_file(small)
    img = (f'<image x="{CORNER_X}" y="{CORNER_Y}" width="{CORNER_W}" height="{CORNER_H}" '
           f'xlink:href="data:image/png;base64,{b64}"></image>')
    inject = img + f'<g transform="rotate(180)">{img}</g>'
    svg = svg.replace("</svg>", inject + "</svg>")
    with open(src, "w", encoding="utf-8") as f:
        f.write(svg)


def compose_number_card(rank, suit, out):
    """Flatten an ace/number card (kit raster art) to a compact PNG."""
    suit_stem, color = SUITS[suit]
    rank_glyph = RANK_GLYPH[rank]
    with tempfile.TemporaryDirectory() as tmp:
        base = os.path.join(tmp, "base.png")
        stamp = os.path.join(tmp, "stamp.png")
        stamp_r = os.path.join(tmp, "stamp_r.png")

        base_white_card(base)
        build_corner_stamp(rank_glyph, suit_stem, color, stamp)
        run("magick", stamp, "-rotate", "180", stamp_r)

        args = ["magick", base,
                stamp, "-gravity", "NorthWest", "-geometry", "+28+30", "-composite",
                stamp_r, "-gravity", "SouthEast", "-geometry", "+28+30", "-composite"]

        big = os.path.join(tmp, "big.png")
        if rank == "A":  # big central suit
            run("magick", suit_png(suit_stem), "-resize", "x470", big)
        else:            # big central number
            h = "360" if rank == "T" else "400"
            run("magick", rank_png(rank_glyph, color), "-resize", f"x{h}", big)
        args += [big, "-gravity", "Center", "-geometry", "+0+0", "-composite"]

        # These cards are white + one flat color, so quantize to a small palette
        # and max-compress to keep the embedded asset tiny. -strip drops the kit's
        # leftover ICC/XMP profiles.
        args += ["-strip", "-colors", "32",
                 "-define", "png:compression-level=9", out]
        run(*args)


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
    ranks = ["A", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K"]
    with tempfile.TemporaryDirectory() as tmp:
        for suit in SUITS:
            for rank in ranks:
                key = f"{rank}{suit}"
                if only and key not in only:
                    continue
                if rank in COURT:
                    # edits the existing vector court SVG in place
                    emit_court_svg(rank, suit)
                else:
                    png = os.path.join(tmp, f"{key}.png")
                    compose_number_card(rank, suit, png)
                    wrap_svg(png, key, os.path.join(CARDS, f"{key}.svg"))
                print(f"generated {key}")


if __name__ == "__main__":
    main()
