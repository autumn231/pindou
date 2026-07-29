#!/usr/bin/env python3
# 生成像素风治愈系拼豆图标 (PNG, Android 7 及以下兼底)
# Android 8+ 使用 mipmap-anydpi-v26 里的 adaptive-icon xml
from PIL import Image
import os

SIZE = 512  # 主尺寸


def draw_bead(size):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    cx = cy = size // 2
    radius = int(size * 0.42)
    hole_r = int(size * 0.13)

    # 治愈系配色: 薄荷绿主体 + 奶白中心孔 + 暖粉高光
    mint = (180, 230, 210, 255)
    mint_dark = (140, 200, 180, 255)
    mint_light = (215, 245, 230, 255)
    cream = (255, 245, 220, 255)
    cream_dark = (240, 230, 200, 255)
    outline = (90, 110, 100, 255)
    highlight = (255, 220, 225, 255)

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            d = (dx * dx + dy * dy) ** 0.5
            if d <= radius - 1:
                px[x, y] = mint
                if dy < -radius * 0.35 and abs(dx) < radius * 0.55:
                    px[x, y] = mint_light
                if -radius * 0.55 < dy < -radius * 0.25 and -radius * 0.4 < dx < -radius * 0.05:
                    px[x, y] = highlight
                if dy > radius * 0.3 and abs(dx) < radius * 0.85:
                    px[x, y] = mint_dark
                if d <= hole_r:
                    px[x, y] = cream
                    if d <= hole_r - 2:
                        px[x, y] = cream_dark
                    if d <= hole_r - 5:
                        px[x, y] = (220, 200, 170, 255)
            elif d <= radius + 1:
                px[x, y] = outline
    return img


def main():
    base = draw_bead(SIZE)
    targets = [
        ('mipmap-mdpi', 48),
        ('mipmap-hdpi', 72),
        ('mipmap-xhdpi', 96),
        ('mipmap-xxhdpi', 144),
        ('mipmap-xxxhdpi', 192),
    ]
    res_dir = 'app/src/main/res'
    for folder, sz in targets:
        path = os.path.join(res_dir, folder, 'ic_launcher.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        base.resize((sz, sz), Image.NEAREST).save(path)
        round_path = os.path.join(res_dir, folder, 'ic_launcher_round.png')
        base.resize((sz, sz), Image.NEAREST).save(round_path)
        print(f'  -> {path} ({sz}x{sz})')


if __name__ == '__main__':
    main()
