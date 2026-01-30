import re
import os

PROPERTIES_FILE = 'gradle.properties'

def bump_version():
    with open(PROPERTIES_FILE, 'r') as f:
        content = f.read()

    version_pattern = r'mod_version=(\d+)\.(\d+)\.(\d+)'
    match = re.search(version_pattern, content)

    if not match:
        print("Error: Could not find mod_version in gradle.properties")
        exit(1)

    major = int(match.group(1))
    minor = int(match.group(2))
    patch = int(match.group(3))

    new_version = f"{major}.{minor}.{patch + 1}"
    
    new_content = re.sub(version_pattern, f"mod_version={new_version}", content)

    with open(PROPERTIES_FILE, 'w') as f:
        f.write(new_content)
    
    print(f"Bumped version from {major}.{minor}.{patch} to {new_version}")
    print(f"::set-output name=new_version::{new_version}")

if __name__ == "__main__":
    if os.path.exists(PROPERTIES_FILE):
        bump_version()
    else:
        print(f"Error: {PROPERTIES_FILE} not found")
        exit(1)
