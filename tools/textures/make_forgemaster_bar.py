"""La barre de boss du maître de forge (plan, section 14 ; aperçu validé par Anger) : une texture 256 x 64.
  (0, 0)  la barre vide (200 x 15) ;  (0, 16) la barre pleine ;  (0, 32) les fêlures et la vapeur de la phase 2 ;
  (0, 48), (16, 48), (32, 48), (48, 48) : le rouage, en quatre positions (15 x 15).

Usage : python3 tools/textures/make_forgemaster_bar.py fireforge/src/main/resources
"""
import os
import sys

import math, random
from PIL import Image, ImageDraw
BRASS, BRASS_D, BRASS_L, IRON, IRON_D = (150, 112, 60), (78, 56, 30), (196, 160, 96), (58, 56, 60), (30, 29, 32)

def gear(img, cx, cy, r, turn):
    p = img.load()
    for x in range(cx - r - 2, cx + r + 3):
        for y in range(cy - r - 2, cy + r + 3):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            a = (math.atan2(dy, dx) + turn) % (2 * math.pi / 8)
            tooth = a < math.pi / 8
            if d <= r - 0.5 or (d <= r + 1.6 and tooth):
                c = BRASS_D if d > r - 1.2 and not tooth else BRASS
                if d < 2.2: c = IRON_D
                elif d < 3.2: c = BRASS_L
                p[x, y] = c + (255,)

def bar(frac, phase2=False, seed=1, turn=0.0):
    rnd = random.Random(seed)
    W, H = 200, 15
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x0, x1, y0, y1 = 10, 194, 4, 11            # le tube
    d.rectangle([x0, y0, x1, y1], fill=BRASS_D + (255,))
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=BRASS + (255,))
    for x in range(x0 + 1, x1):                  # reflet sur le laiton
        img.putpixel((x, y0 + 1), BRASS_L + (255,))
    ix0, ix1, iy0, iy1 = x0 + 2, x1 - 2, y0 + 2, y1 - 2
    d.rectangle([ix0, iy0, ix1, iy1], fill=IRON_D + (255,))
    fill = ix0 + int((ix1 - ix0 + 1) * frac) - 1
    for x in range(ix0, ix1 + 1):
        for y in range(iy0, iy1 + 1):
            t = (y - iy0) / max(1, iy1 - iy0)
            if x <= fill:   # le métal en fusion : jaune en haut, rouge en bas, qui ondule
                wave = 0.5 + 0.5 * math.sin(x * 0.45 + y)
                c = (255, int(220 - 120 * t - 25 * wave), int(70 - 60 * t))
                if x >= fill - 1: c = (255, 245, 180)
            else:           # refroidi : du fer noirci
                c = (40 + rnd.randint(-4, 4),) * 3
            img.putpixel((x, y), c + (255,))
    for x in range(x0 + 14, x1 - 6, 16):         # rivets
        for y in (y0, y1):
            img.putpixel((x, y), BRASS_L + (255,))
    d.rectangle([x1 + 1, y0 + 1, x1 + 4, y1 - 1], fill=IRON + (255,))      # la valve au bout
    d.rectangle([x1 + 2, y0 - 1, x1 + 3, y0], fill=IRON_D + (255,))
    if phase2:
        for x, y in ((48, y0), (49, y0 + 1), (50, y0 + 1), (121, y1), (122, y1 - 1), (160, y0), (161, y0 + 1)):
            img.putpixel((x, y), IRON_D + (255,))  # fêlures
        for cx, cy in ((50, 1), (122, 13), (162, 1)):
            for k in range(6):
                img.putpixel((cx + rnd.randint(-2, 2), max(0, min(H - 1, cy + rnd.randint(-1, 1)))), (210, 205, 200, 200))
    return img


if __name__ == "__main__":
    atlas = Image.new("RGBA", (256, 64), (0, 0, 0, 0))
    atlas.alpha_composite(bar(0.0, seed=1), (0, 0))
    atlas.alpha_composite(bar(1.0, seed=1), (0, 16))
    # la phase 2 : seulement ce qui change (fêlures, vapeur)
    empty, cracked = bar(0.0, seed=1), bar(0.0, True, seed=1)
    over = Image.new("RGBA", empty.size, (0, 0, 0, 0))
    for xy in ((x, y) for x in range(empty.width) for y in range(empty.height)):
        if cracked.getpixel(xy) != empty.getpixel(xy):
            over.putpixel(xy, cracked.getpixel(xy))
    atlas.alpha_composite(over, (0, 32))
    for i in range(4):
        g = Image.new("RGBA", (15, 15), (0, 0, 0, 0))
        gear(g, 7, 7, 6, i * math.pi / 16)
        atlas.alpha_composite(g, (i * 16, 48))
    out = os.path.join(sys.argv[1], "assets", "echoes_fireforge", "textures", "gui")
    os.makedirs(out, exist_ok=True)
    atlas.save(os.path.join(out, "forge_master_bar.png"))
