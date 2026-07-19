#!/usr/bin/env python3
"""Generate the Chat Wingman brand derivatives from the approved hat artwork.

The opaque 1254px file remains the source-of-truth Master.  This script only
creates deterministic derivatives: it cleans the existing alpha matte without
redrawing the mark, then lays the result out for UI, launcher, and review use.

Run with the Codex bundled Python (Pillow + NumPy):

    /path/to/bundled/python3 design/icon/generate_android_assets.py
"""

from __future__ import annotations

from collections import deque
from pathlib import Path
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[2]
ICON_DIR = ROOT / "design" / "icon"
RES_DIR = ROOT / "android" / "app" / "src" / "main" / "res"

MASTER = ICON_DIR / "chat-wingman-icon-master.png"
SOURCE_ALPHA = ICON_DIR / "chat-wingman-icon-foreground.png"

# The approved 1254px Master is the user's original image.  The legacy alpha
# source was made from a crop of its earlier, pixel-identical 1024px resize.
# Keeping the crop in that reference coordinate system lets us transfer only
# the alpha shape back to the original-resolution RGB artwork.
ALPHA_REFERENCE_SIZE = (1024, 1024)
ALPHA_REFERENCE_CROP = (231, 200, 797, 817)

TRANSPARENT_MASTER = ICON_DIR / "chat-wingman-hat-transparent-master.png"
PLAY_STORE = ICON_DIR / "chat-wingman-icon-play-store.png"
VALIDATION_BOARD = ICON_DIR / "chat-wingman-brand-validation.png"
UI_ASSET = RES_DIR / "drawable-nodpi" / "brand_kongming_hat.png"

BRAND_CREAM = (249, 233, 211)  # #F9E9D3
WARM_WHITE = (255, 249, 243)   # #FFF9F3
SURFACE = (255, 255, 255)
INK = (29, 23, 18)             # #1D1712

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def nearest_solid_rgb(rgb: np.ndarray, solid: np.ndarray) -> np.ndarray:
    """Bleed the nearest opaque subject color into antialiased edge pixels.

    The prior cutout stored cream matte RGB under low alpha.  A multi-source
    flood fill replaces that matte with neighboring subject color while alpha
    continues to describe coverage.  This prevents pale halos on dark UI.
    """

    height, width = solid.shape
    filled = solid.copy()
    output = rgb.copy()
    queue: deque[tuple[int, int]] = deque(
        (int(y), int(x)) for y, x in np.argwhere(solid)
    )
    neighbors = ((-1, 0), (1, 0), (0, -1), (0, 1))

    while queue:
        y, x = queue.popleft()
        color = output[y, x]
        for dy, dx in neighbors:
            ny, nx = y + dy, x + dx
            if 0 <= ny < height and 0 <= nx < width and not filled[ny, nx]:
                output[ny, nx] = color
                filled[ny, nx] = True
                queue.append((ny, nx))
    return output


def clean_alpha(source: Path) -> Image.Image:
    image = Image.open(source).convert("RGBA")
    original_alpha = np.asarray(image.getchannel("A"))

    # Close isolated one-pixel extraction holes while preserving the wide,
    # intentional gaps between the five hat panels.
    opaque = Image.fromarray(
        np.where(original_alpha >= 224, 255, 0).astype(np.uint8), mode="L"
    )
    closed = opaque.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
    closed_mask = np.asarray(closed) == 255
    alpha = original_alpha.astype(np.float32)
    alpha[closed_mask & (original_alpha < 224)] = 255

    # Contract the old soft matte very slightly.  True antialiasing remains,
    # but the long alpha tail caused by the cream glow is removed.
    alpha = np.clip((alpha - 12.0) * (255.0 / 243.0), 0.0, 255.0).astype(np.uint8)

    result = Image.fromarray(alpha, mode="L")
    bbox = result.getbbox()
    if bbox is None:
        raise RuntimeError("The cleaned brand mark is unexpectedly empty")
    return result


def native_crop_box(master_size: tuple[int, int]) -> tuple[int, int, int, int]:
    scale_x = master_size[0] / ALPHA_REFERENCE_SIZE[0]
    scale_y = master_size[1] / ALPHA_REFERENCE_SIZE[1]
    left, top, right, bottom = ALPHA_REFERENCE_CROP
    return (
        round(left * scale_x),
        round(top * scale_y),
        round(right * scale_x),
        round(bottom * scale_y),
    )


def clean_cutout(master_path: Path, alpha_source: Path) -> tuple[Image.Image, Image.Image]:
    """Return a tight cutout plus a native-resolution transparent Master."""

    master = Image.open(master_path).convert("RGB")
    crop_box = native_crop_box(master.size)
    rgb_crop = master.crop(crop_box)
    alpha = clean_alpha(alpha_source).resize(rgb_crop.size, Image.Resampling.LANCZOS)

    rgb = np.asarray(rgb_crop).copy()
    alpha_array = np.asarray(alpha)
    bleed = nearest_solid_rgb(rgb, alpha_array >= 250)
    partial = (alpha_array > 0) & (alpha_array < 250)
    rgb[partial] = bleed[partial]
    rgb[alpha_array == 0] = 0

    crop_rgba = Image.fromarray(
        np.dstack((rgb, alpha_array)).astype(np.uint8), mode="RGBA"
    )
    bbox = crop_rgba.getchannel("A").getbbox()
    if bbox is None:
        raise RuntimeError("The cleaned brand mark is unexpectedly empty")

    native = Image.new("RGBA", master.size, (0, 0, 0, 0))
    native.alpha_composite(crop_rgba, (crop_box[0], crop_box[1]))
    return crop_rgba.crop(bbox), native


def square_mark(
    cutout: Image.Image,
    size: int,
    *,
    content_height_ratio: float,
    vertical_offset_ratio: float = 0.0,
) -> Image.Image:
    """Center a cutout on a transparent square with a fixed optical scale."""

    target_height = max(1, round(size * content_height_ratio))
    target_width = max(1, round(cutout.width * target_height / cutout.height))
    resized = cutout.resize((target_width, target_height), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    x = (size - target_width) // 2
    y = (size - target_height) // 2 + round(size * vertical_offset_ratio)
    canvas.alpha_composite(resized, (x, y))
    return canvas


def flat_launcher(mark: Image.Image, size: int, *, round_icon: bool) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    if round_icon:
        draw.ellipse((0, 0, size - 1, size - 1), fill=BRAND_CREAM + (255,))
    else:
        draw.rectangle((0, 0, size, size), fill=BRAND_CREAM + (255,))

    placed = square_mark(mark, size, content_height_ratio=0.64, vertical_offset_ratio=-0.01)
    canvas.alpha_composite(placed)
    return canvas


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)


def adaptive_mask(size: int, kind: str) -> Image.Image:
    """Approximate common OEM masks for the visual validation board."""

    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    inset = round(size / 6)  # 72dp visible mask inside a 108dp layer.
    box = (inset, inset, size - inset - 1, size - inset - 1)
    if kind == "circle":
        draw.ellipse(box, fill=255)
    elif kind == "rounded":
        draw.rounded_rectangle(box, radius=round(size * 0.16), fill=255)
    elif kind == "squircle":
        cx = cy = size / 2
        radius = (size - 2 * inset) / 2
        points: list[tuple[float, float]] = []
        exponent = 5.0
        for step in range(720):
            angle = 2 * math.pi * step / 720
            cos_a, sin_a = math.cos(angle), math.sin(angle)
            x = math.copysign(abs(cos_a) ** (2 / exponent), cos_a)
            y = math.copysign(abs(sin_a) ** (2 / exponent), sin_a)
            points.append((cx + radius * x, cy + radius * y))
        draw.polygon(points, fill=255)
    else:
        raise ValueError(f"Unknown mask kind: {kind}")
    return mask


def validation_board(mark: Image.Image, adaptive: Image.Image) -> Image.Image:
    board = Image.new("RGB", (1500, 980), (241, 235, 228))
    draw = ImageDraw.Draw(board)

    panels = [
        (40, 40, 460, 460, SURFACE, "WHITE"),
        (540, 40, 960, 460, BRAND_CREAM, "BRAND CREAM"),
        (1040, 40, 1460, 460, INK, "DARK"),
    ]
    preview = square_mark(mark, 330, content_height_ratio=0.9)
    for left, top, right, bottom, color, label in panels:
        draw.rounded_rectangle((left, top, right, bottom), radius=36, fill=color)
        board.paste(preview, (left + 45, top + 36), preview)
        label_color = WARM_WHITE if color == INK else INK
        draw.text((left + 18, bottom - 34), label, fill=label_color)

    # Pixel-size recognition: render at target sizes, then scale with nearest
    # neighbor so reviewers can inspect actual small-icon silhouettes.
    x = 40
    small_scale = 3
    draw.text((x, 500), "SMALL-SIZE MARKS (24 / 32 / 48 / 64 PX)", fill=INK)
    for px in (24, 32, 48, 64):
        small = square_mark(mark, px, content_height_ratio=0.96)
        enlarged = small.resize(
            (px * small_scale, px * small_scale), Image.Resampling.NEAREST
        )
        board.paste(enlarged, (x, 540), enlarged)
        draw.text((x, 750), f"{px}px", fill=INK)
        x += px * small_scale + 44

    draw.text((750, 500), "ADAPTIVE MASK PREVIEW", fill=INK)
    background = Image.new("RGBA", adaptive.size, BRAND_CREAM + (255,))
    background.alpha_composite(adaptive)
    x = 750
    for kind in ("circle", "rounded", "squircle"):
        mask = adaptive_mask(adaptive.width, kind)
        composed = Image.new("RGBA", adaptive.size, (0, 0, 0, 0))
        composed.paste(background, (0, 0), mask)
        thumb = composed.resize((220, 220), Image.Resampling.LANCZOS)
        board.paste(thumb, (x, 540), thumb)
        draw.text((x + 70, 780), kind.upper(), fill=INK)
        x += 235

    draw.text(
        (40, 930),
        "Source geometry and orange/caramel gradient preserved; alpha matte cleaned deterministically.",
        fill=(91, 80, 70),
    )
    return board


def main() -> None:
    if not MASTER.exists() or not SOURCE_ALPHA.exists():
        raise FileNotFoundError("Approved Master and alpha source are both required")

    cutout, transparent_master = clean_cutout(MASTER, SOURCE_ALPHA)

    # Preserve original pixels and placement in the transparent Master.  The
    # UI derivative is separately optically scaled for small-screen clarity.
    save_png(transparent_master, TRANSPARENT_MASTER)
    save_png(square_mark(cutout, 768, content_height_ratio=0.94), UI_ASSET)

    # Play Store art remains a faithful half-size copy of the approved Master.
    master = Image.open(MASTER).convert("RGB")
    save_png(master.resize((512, 512), Image.Resampling.LANCZOS), PLAY_STORE)

    # Android Adaptive layers: the farthest opaque point stays inside the
    # guaranteed 33dp-radius safe circle.  51/108 content height preserves
    # readability while leaving mask/animation headroom.
    adaptive_xxxhdpi = square_mark(cutout, 432, content_height_ratio=51 / 108)
    for density, scale in DENSITIES.items():
        foreground_size = round(108 * scale)
        foreground = square_mark(cutout, foreground_size, content_height_ratio=51 / 108)
        save_png(
            foreground,
            RES_DIR / f"drawable-{density}" / "ic_launcher_foreground.png",
        )

        launcher_size = round(48 * scale)
        save_png(
            flat_launcher(cutout, launcher_size, round_icon=False).convert("RGB"),
            RES_DIR / f"mipmap-{density}" / "ic_launcher.png",
        )
        save_png(
            flat_launcher(cutout, launcher_size, round_icon=True),
            RES_DIR / f"mipmap-{density}" / "ic_launcher_round.png",
        )

    save_png(validation_board(cutout, adaptive_xxxhdpi), VALIDATION_BOARD)

    print(f"Preserved Master: {MASTER.relative_to(ROOT)}")
    print(f"Transparent Master: {TRANSPARENT_MASTER.relative_to(ROOT)}")
    print(f"UI mark: {UI_ASSET.relative_to(ROOT)}")
    print(f"Play Store: {PLAY_STORE.relative_to(ROOT)}")
    print(f"Validation: {VALIDATION_BOARD.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
