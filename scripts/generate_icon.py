#!/usr/bin/env python3
# 生成像素风治愈系拼豆图标
from PIL import Image, ImageDraw
import os

SIZE = 512  # 主尺寸 (xxxhdpi)


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
                # 主体
                px[x, y] = mint
                # 顶部高光区
                if dy < -radius * 0.35 and abs(dx) < radius * 0.55:
                    px[x, y] = mint_light
                # 左上小高光点 (像素风亮点)
                if -radius * 0.55 < dy < -radius * 0.25 and -radius * 0.4 < dx < -radius * 0.05:
                    px[x, y] = highlight
                # 下半部阴影
                if dy > radius * 0.3 and abs(dx) < radius * 0.85:
                    px[x, y] = mint_dark
                # 中心孔
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
    # 各密度 (Android launcher icon 标准)
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
        # 像素风: 用 NEAREST 缩放保留锯齿感
        base.resize((sz, sz), Image.NEAREST).save(path)
        # round 版本相同
        round_path = os.path.join(res_dir, folder, 'ic_launcher_round.png')
        base.resize((sz, sz), Image.NEAREST).save(round_path)
        print(f'  -> {path} ({sz}x{sz})')

    # foreground for adaptive icon (前景透明背景)
    fg = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    # 复制主体到前景 (adaptive icon 用)
    fg.paste(base, (0, 0), base)
    fg_path = os.path.join(res_dir, 'drawable', 'ic_launcher_foreground.png')
    os.makedirs(os.path.dirname(fg_path), exist_ok=True)
    fg.resize((108, 108), Image.NEAREST).save(fg_path)
    print(f'  -> {fg_path}')


if __name__ == '__main__':
    main()
