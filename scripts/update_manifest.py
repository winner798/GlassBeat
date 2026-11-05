"""Generate the public fallback feed from the latest published signed release."""
import json
import re
import subprocess
from pathlib import Path

release = json.loads(subprocess.check_output(["gh", "api", "repos/winner798/GlassBeat/releases/latest"], text=True))
version = release["tag_name"]
if release["draft"] or release["prerelease"] or not re.fullmatch(r"v\d+\.\d+\.\d+", version):
    raise SystemExit("A stable three-part release is required")
name = f"GlassBeat-{version}.apk"
asset = next(item for item in release["assets"] if item["name"] == name)
digest = asset.get("digest", "")
if not re.fullmatch(r"sha256:[0-9a-fA-F]{64}", digest):
    raise SystemExit("The published APK must have a SHA-256 digest")
manifest = {
    "packageName": "io.github.glassbeat",
    "version": version.removeprefix("v"),
    "size": asset["size"],
    "sha256": digest.removeprefix("sha256:"),
    "notes": release.get("body", ""),
    "downloadUrls": [
        f"https://raw.githubusercontent.com/winner798/GlassBeat/release-artifacts/{version}/dist/{name}",
        asset["url"],
        asset["browser_download_url"],
    ],
}
Path("updates").mkdir(exist_ok=True)
Path("updates/stable.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
print(f"Fallback manifest prepared for {version}")
