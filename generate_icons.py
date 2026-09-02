import math
from PIL import Image, ImageDraw

def render_timer_icon(size=512, is_round=False):
    # Render at 4x for super sampling anti-aliasing
    scale = 4
    canvas_size = size * scale
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    center_x = canvas_size / 2
    center_y = canvas_size / 2 + (canvas_size * 0.03)

    # 1. Background
    bg_radius = canvas_size * 0.46
    bg_box = [
        center_x - bg_radius,
        canvas_size / 2 - bg_radius,
        center_x + bg_radius,
        canvas_size / 2 + bg_radius
    ]

    if is_round:
        draw.ellipse(bg_box, fill=(13, 17, 23, 255), outline=(36, 48, 68, 255), width=int(3 * scale))
    else:
        # Squircle / rounded rect
        corner_r = int(canvas_size * 0.22)
        draw.rounded_rectangle(
            [canvas_size * 0.04, canvas_size * 0.04, canvas_size * 0.96, canvas_size * 0.96],
            radius=corner_r,
            fill=(13, 17, 23, 255),
            outline=(36, 48, 68, 255),
            width=int(3 * scale)
        )

    # Decorative subtle concentric circle
    draw.ellipse(
        [center_x - bg_radius * 0.85, center_y - bg_radius * 0.85,
         center_x + bg_radius * 0.85, center_y + bg_radius * 0.85],
        fill=(22, 30, 46, 255)
    )

    # 2. Stopwatch Crown & Loop at Top
    loop_r = canvas_size * 0.08
    loop_y = center_y - canvas_size * 0.38
    draw.ellipse(
        [center_x - loop_r, loop_y - loop_r, center_x + loop_r, loop_y + loop_r],
        outline=(255, 215, 0, 255),
        width=int(4 * scale)
    )

    crown_w = canvas_size * 0.07
    crown_h = canvas_size * 0.05
    crown_y = center_y - canvas_size * 0.32
    draw.rectangle(
        [center_x - crown_w / 2, crown_y, center_x + crown_w / 2, crown_y + crown_h],
        fill=(255, 215, 0, 255)
    )

    # Lap buttons
    lap_offset_x = canvas_size * 0.22
    lap_offset_y = canvas_size * 0.22
    btn_len = canvas_size * 0.06
    draw.line(
        [center_x - lap_offset_x, center_y - lap_offset_y,
         center_x - lap_offset_x + btn_len * 0.7, center_y - lap_offset_y + btn_len * 0.7],
        fill=(255, 193, 7, 255),
        width=int(4.5 * scale)
    )
    draw.line(
        [center_x + lap_offset_x, center_y - lap_offset_y,
         center_x + lap_offset_x - btn_len * 0.7, center_y - lap_offset_y + btn_len * 0.7],
        fill=(255, 193, 7, 255),
        width=int(4.5 * scale)
    )

    # 3. Outer Stopwatch Bezel
    watch_r = canvas_size * 0.28
    watch_box = [center_x - watch_r, center_y - watch_r, center_x + watch_r, center_y + watch_r]
    draw.ellipse(watch_box, fill=(17, 22, 34, 255), outline=(255, 193, 7, 255), width=int(5 * scale))

    # Inner subtle rim
    inner_rim_r = watch_r * 0.90
    draw.ellipse(
        [center_x - inner_rim_r, center_y - inner_rim_r, center_x + inner_rim_r, center_y + inner_rim_r],
        outline=(42, 54, 79, 255),
        width=int(2 * scale)
    )

    # 4. Active Green Combat Sector (Pie Slice 12 to 2 o'clock)
    pie_r = inner_rim_r * 0.95
    draw.pieslice(
        [center_x - pie_r, center_y - pie_r, center_x + pie_r, center_y + pie_r],
        start=-90,
        end=-20,
        fill=(0, 230, 118, 230)
    )

    # 5. Dial Tick Marks
    for i in range(12):
        angle = math.radians(i * 30 - 90)
        is_major = (i % 3 == 0)
        t_len = watch_r * 0.16 if is_major else watch_r * 0.09
        w = int(3.5 * scale) if is_major else int(2 * scale)
        color = (255, 255, 255, 255) if is_major else (120, 144, 156, 255)

        start_r = watch_r * 0.85
        end_r = start_r - t_len
        x1 = center_x + start_r * math.cos(angle)
        y1 = center_y + start_r * math.sin(angle)
        x2 = center_x + end_r * math.cos(angle)
        y2 = center_y + end_r * math.sin(angle)
        draw.line([x1, y1, x2, y2], fill=color, width=w)

    # 6. Stopwatch Hand (Pointing to ~2 o'clock, 50 degrees from 12)
    needle_angle = math.radians(-30)
    needle_len = watch_r * 0.65
    nx = center_x + needle_len * math.cos(needle_angle)
    ny = center_y + needle_len * math.sin(needle_angle)
    draw.line([center_x, center_y, nx, ny], fill=(255, 255, 255, 255), width=int(4 * scale))

    # Needle tip in Fight Green
    tip_len = needle_len * 0.25
    tx1 = nx - tip_len * math.cos(needle_angle)
    ty1 = ny - tip_len * math.sin(needle_angle)
    draw.line([tx1, ty1, nx, ny], fill=(0, 230, 118, 255), width=int(4.5 * scale))

    # 7. Center Hub / Pivot
    hub_r = canvas_size * 0.04
    draw.ellipse([center_x - hub_r, center_y - hub_r, center_x + hub_r, center_y + hub_r], fill=(255, 215, 0, 255))
    pin_r = hub_r * 0.45
    draw.ellipse([center_x - pin_r, center_y - pin_r, center_x + pin_r, center_y + pin_r], fill=(255, 255, 255, 255))

    # Downscale with Lanczos for anti-aliasing
    return img.resize((size, size), Image.Resampling.LANCZOS)

# Generate icon set
sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

base_res_dir = "app/src/main/res"

for density, sz in sizes.items():
    square_img = render_timer_icon(sz, is_round=False)
    round_img = render_timer_icon(sz, is_round=True)

    square_path = f"{base_res_dir}/mipmap-{density}/ic_launcher.webp"
    round_path = f"{base_res_dir}/mipmap-{density}/ic_launcher_round.webp"

    square_img.save(square_path, "WEBP")
    round_img.save(round_path, "WEBP")
    print(f"Generated {density} ({sz}x{sz})")

# Generate 512x512 preview for user review
preview = render_timer_icon(512, is_round=False)
preview.save("app_icon_preview.png", "PNG")
preview.save("C:/Users/inovace/.gemini/antigravity/brain/5788e3cb-7127-4b47-a5e4-973e08b54406/app_icon_preview.png", "PNG")
print("Saved 512x512 app_icon_preview.png")
