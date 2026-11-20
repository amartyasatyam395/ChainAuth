#!/usr/bin/env python3
from pathlib import Path
import zipfile
root = Path(__file__).resolve().parents[1]
out = root.parent / "ChainAuth_Backup_06_Phase6.zip"
excluded = {"node_modules", "target", "dist", "artifacts", "cache", ".git", "__pycache__"}
def include(path):
    if not path.is_file() or any(part in excluded for part in path.parts):
        return False
    if path.name == ".env" or (path.name.startswith(".env.") and path.name != ".env.example"):
        return False
    return True
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as archive:
    for path in root.rglob("*"):
        if include(path):
            archive.write(path, Path("ChainAuth") / path.relative_to(root))
print(out)
