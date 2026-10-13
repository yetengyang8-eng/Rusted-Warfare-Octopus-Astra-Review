"""Read-only baseline identity check. Never resets, builds, installs or launches games."""
import argparse
import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def content_digest(path):
    with zipfile.ZipFile(path) as archive:
        entries = {name: hashlib.sha256(archive.read(name)).hexdigest()
                   for name in archive.namelist() if name != 'META-INF/MANIFEST.MF'}
    payload = '\n'.join('%s %s' % (name, entries[name]) for name in sorted(entries))
    return hashlib.sha256(payload.encode('utf-8')).hexdigest()


def git(root, *args):
    result = subprocess.run(['git', '-C', str(root), *args], capture_output=True,
                            encoding='utf-8', errors='replace')
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip())
    return result.stdout.strip()


def check(root, manifest, workspace=None):
    failures = []
    checked = 0
    source = manifest['source']
    try:
        git(root, 'cat-file', '-e', source['commit'] + '^{commit}')
        differences = git(root, 'diff', '--name-only', source['commit'], '--', *source['paths'])
        untracked = git(root, 'ls-files', '--others', '--exclude-standard', '--', *source['paths'])
        if differences or untracked:
            failures.append({'kind': 'source_drift', 'changed': differences.splitlines(),
                             'untracked': untracked.splitlines()})
    except RuntimeError as error:
        failures.append({'kind': 'source_unverified', 'detail': str(error)})

    groups = [(root, manifest['artifacts'], 'repository')]
    if workspace is not None:
        groups.append((workspace, manifest['localEnvironmentArtifacts'], 'workspace'))
    for base, records, scope in groups:
        for record in records:
            path = base / record['path']
            checked += 1
            if not path.is_file():
                failures.append({'kind': 'missing_file', 'scope': scope, 'path': record['path']})
                continue
            actual = sha256(path)
            if actual != record['sha256']:
                failures.append({'kind': 'sha_mismatch', 'scope': scope, 'path': record['path'],
                                 'expected': record['sha256'], 'actual': actual})
            if 'contentDigest' in record:
                actual_content = content_digest(path)
                if actual_content != record['contentDigest']:
                    failures.append({'kind': 'content_mismatch', 'scope': scope,
                                     'path': record['path'], 'actual': actual_content})
    return {'baselineId': manifest['baselineId'], 'passed': not failures,
            'sourceCommit': source['commit'], 'filesChecked': checked,
            'localEnvironmentsChecked': workspace is not None, 'failures': failures,
            'action': 'Read-only; preserve differences. No reset, build, install or game launch performed.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--workspace', type=Path, help='Optional local workspace root to check installed/legacy artifacts')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    manifest = json.loads((root / 'project-state/baseline-manifest.json').read_text(encoding='utf-8-sig'))
    result = check(root, manifest, args.workspace)
    print(json.dumps(result, ensure_ascii=True, indent=2))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
