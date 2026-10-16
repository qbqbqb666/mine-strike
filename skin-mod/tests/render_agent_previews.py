"""Render the packaged agent textures with verified proportions, including UI portraits."""
from pathlib import Path
import importlib.util
import json
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("agent_renderer", root / "art/agents-v1/render_preview.py")
renderer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(renderer)
manifest = json.loads((root / "art/agents-v1/manifest.json").read_text())
textures = root / "src/main/resources/assets/minestrike_skins/textures"
portraits = textures / "gui/agents"
portraits.mkdir(parents=True, exist_ok=True)
overview = Image.new("RGB", (1300, 1260), (17, 24, 34))
draw = ImageDraw.Draw(overview)
font = ImageFont.load_default(size=22)
draw.text((20, 18), "AGENTS / torso width 85%, depth 75% / limbs 75% x 100% / head 65%", font=font, fill="white")
draw.text((20, 50), "Actual packaged textures on verified geometry | offline preview", font=font, fill=(170, 185, 200))
for index, agent in enumerate(manifest["agents"]):
    renderer.skin = np.array(Image.open(textures / "entity/agents" / agent["texture"]).convert("RGBA"))
    portrait = renderer.render(renderer.figure(True), 0, 15, 130)
    portrait.save(portraits / agent["texture"])
    x, y = index % 5 * 260, 90 + index // 5 * 580
    overview.paste(portrait, (x, y + 28))
    draw.text((x + 10, y), agent["label"], font=font, fill=(120, 200, 255) if index < 5 else (240, 180, 100))
destination = root / "build/agents-current-preview.png"
overview.save(destination)
print(destination)
