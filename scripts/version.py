#!/usr/bin/env python3
"""
SOH3IL Semantic Versioning & Build Counting Utility.
Manages version.properties, calculates versionCode, and handles automated releases.
"""

import sys
import os
import subprocess

VERSION_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "version.properties")

def load_version():
    props = {}
    if not os.path.exists(VERSION_FILE):
        return {"VERSION_MAJOR": 1, "VERSION_MINOR": 0, "VERSION_PATCH": 0, "VERSION_BUILD": 1}
    with open(VERSION_FILE, "r") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                props[k.strip()] = int(v.strip())
    return props

def save_version(props):
    with open(VERSION_FILE, "w") as f:
        f.write("# SOH3IL Version Properties\n")
        f.write(f"VERSION_MAJOR={props.get('VERSION_MAJOR', 1)}\n")
        f.write(f"VERSION_MINOR={props.get('VERSION_MINOR', 0)}\n")
        f.write(f"VERSION_PATCH={props.get('VERSION_PATCH', 0)}\n")
        f.write(f"VERSION_BUILD={props.get('VERSION_BUILD', 1)}\n")

def get_version_code(props):
    major = props.get("VERSION_MAJOR", 1)
    minor = props.get("VERSION_MINOR", 0)
    patch = props.get("VERSION_PATCH", 0)
    build = props.get("VERSION_BUILD", 1)
    # Monotonic formula: Major * 10000 + Minor * 100 + Patch + Build
    return major * 10000 + minor * 100 + patch + build

def get_version_name(props):
    return f"{props.get('VERSION_MAJOR', 1)}.{props.get('VERSION_MINOR', 0)}.{props.get('VERSION_PATCH', 0)}"

def cmd_current(props):
    name = get_version_name(props)
    code = get_version_code(props)
    print(f"Current Version: v{name} (versionCode: {code}, build: {props.get('VERSION_BUILD', 1)})")

def cmd_bump(props, part="patch"):
    props["VERSION_BUILD"] = props.get("VERSION_BUILD", 1) + 1
    if part == "major":
        props["VERSION_MAJOR"] = props.get("VERSION_MAJOR", 1) + 1
        props["VERSION_MINOR"] = 0
        props["VERSION_PATCH"] = 0
    elif part == "minor":
        props["VERSION_MINOR"] = props.get("VERSION_MINOR", 0) + 1
        props["VERSION_PATCH"] = 0
    elif part == "patch":
        props["VERSION_PATCH"] = props.get("VERSION_PATCH", 0) + 1
    elif part == "build":
        pass
    else:
        print(f"Unknown version part: {part}. Use major, minor, patch, or build.")
        sys.exit(1)

    save_version(props)
    name = get_version_name(props)
    code = get_version_code(props)
    print(f"Bumped to v{name} (versionCode: {code})")
    return name

def main():
    props = load_version()
    if len(sys.argv) < 2 or sys.argv[1] == "current":
        cmd_current(props)
    elif sys.argv[1] == "code":
        print(get_version_code(props))
    elif sys.argv[1] == "name":
        print(get_version_name(props))
    elif sys.argv[1] == "bump":
        part = sys.argv[2] if len(sys.argv) > 2 else "patch"
        new_tag = cmd_bump(props, part)
        if "--tag" in sys.argv:
            subprocess.run(["git", "add", VERSION_FILE], check=True)
            subprocess.run(["git", "commit", "-m", f"chore(release): bump version to v{new_tag}"], check=True)
            subprocess.run(["git", "tag", f"v{new_tag}"], check=True)
            print(f"Created git commit and tag v{new_tag}")
    else:
        print("Usage: python3 scripts/version.py [current|name|code|bump (patch|minor|major)]")

if __name__ == "__main__":
    main()
