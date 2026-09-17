#!/usr/bin/env python3
"""Generate Play Console high-res icon and feature graphic as PNGs."""

from __future__ import annotations

import struct
import zlib
from pathlib import Path

NAVY = (0x1B, 0x36, 0x5D)
CREAM = (0xF6, 0xF3, 0xEE)
ALARM = (0xC4, 0x5C, 0x26)


def png(width: int, height: int, pixels: list[tuple[int, int, int]]) -> bytes:
    raw = bytearray()
    i = 0
    for _y in range(height):
        raw.append(0)
        for _x in range(width):
            r, g, b = pixels[i]
            raw.extend((r, g, b))
            i += 1

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def fill(w: int, h: int, color: tuple[int, int, int]) -> list[tuple[int, int, int]]:
    return [color] * (w * h)


def rect(
    pixels: list[tuple[int, int, int]],
    w: int,
    h: int,
    x0: int,
    y0: int,
    x1: int,
    y1: int,
    color: tuple[int, int, int],
) -> None:
    for y in range(max(0, y0), min(h, y1)):
        row = y * w
        for x in range(max(0, x0), min(w, x1)):
            pixels[row + x] = color


def circle(
    pixels: list[tuple[int, int, int]],
    w: int,
    h: int,
    cx: int,
    cy: int,
    r: int,
    color: tuple[int, int, int],
    inner: int | None = None,
) -> None:
    r2 = r * r
    inner2 = inner * inner if inner is not None else None
    for y in range(max(0, cy - r), min(h, cy + r + 1)):
        dy = y - cy
        row = y * w
        for x in range(max(0, cx - r), min(w, cx + r + 1)):
            dx = x - cx
            d = dx * dx + dy * dy
            if d <= r2 and (inner2 is None or d >= inner2):
                pixels[row + x] = color


def lock_icon(size: int) -> list[tuple[int, int, int]]:
    pixels = fill(size, size, NAVY)
    s = size / 108.0

    def sc(v: float) -> int:
        return int(round(v * s))

    circle(pixels, size, size, sc(54), sc(40), sc(16), CREAM, inner=sc(10))
    rect(pixels, size, size, sc(38), sc(40), sc(48), sc(50), NAVY)
    rect(pixels, size, size, sc(60), sc(40), sc(70), sc(50), NAVY)
    rect(pixels, size, size, sc(30), sc(48), sc(78), sc(86), CREAM)
    circle(pixels, size, size, sc(54), sc(66), sc(6), NAVY)
    rect(pixels, size, size, sc(51), sc(66), sc(57), sc(78), NAVY)
    return pixels


FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    " ": ["00000", "00000", "00000", "00000", "00000", "00000", "00000"],
}


def draw_text(
    pixels: list[tuple[int, int, int]],
    w: int,
    h: int,
    text: str,
    x: int,
    y: int,
    scale: int,
    color: tuple[int, int, int],
) -> None:
    cx = x
    for ch in text:
        glyph = FONT[ch]
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    rect(
                        pixels,
                        w,
                        h,
                        cx + gx * scale,
                        y + gy * scale,
                        cx + (gx + 1) * scale,
                        y + (gy + 1) * scale,
                        color,
                    )
        cx += 6 * scale


def feature_graphic() -> list[tuple[int, int, int]]:
    w, h = 1024, 500
    pixels = fill(w, h, NAVY)
    rect(pixels, w, h, 0, 0, 18, h, ALARM)
    lock = lock_icon(240)
    ox, oy = 72, 130
    for y in range(240):
        for x in range(240):
            px = lock[y * 240 + x]
            if px != NAVY:
                pixels[(oy + y) * w + (ox + x)] = px
    draw_text(pixels, w, h, "PIN ALARM LOCK", 360, 214, 6, CREAM)
    return pixels


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    listing = root / "play" / "listing" / "en-US" / "images"
    (listing / "icon").mkdir(parents=True, exist_ok=True)
    (listing / "featureGraphic").mkdir(parents=True, exist_ok=True)
    icon = listing / "icon" / "512.png"
    feature = listing / "featureGraphic" / "1024x500.png"
    icon.write_bytes(png(512, 512, lock_icon(512)))
    feature.write_bytes(png(1024, 500, feature_graphic()))
    print(f"wrote {icon}")
    print(f"wrote {feature}")


if __name__ == "__main__":
    main()
