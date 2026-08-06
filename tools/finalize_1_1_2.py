from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "app/build.gradle.kts"

content = GRADLE.read_text(encoding="utf-8")
content = content.replace('versionName = "1.1.2-test"', 'versionName = "1.1.2"')
GRADLE.write_text(content, encoding="utf-8")

print("Galerie 1.1.2 final version applied.")
