# Astra GitHub Writeback Protocol

The remote Astra environment is expected to work through GitHub. Do not depend on the user's local filesystem.

## Preferred workflow

1. Read `AGENTS.md`, `ASTRA_START_HERE.md`, and current project-state files.
2. Create a branch from current `main`, preferably `astra/<task>-YYYYMMDD`.
3. Make reviewable commits. Avoid unrelated cleanup inside an implementation commit.
4. Push the branch. Open a PR if the environment supports it; otherwise provide the branch and commit SHAs.
5. Never force-push `main`.

## Handoff requirements

At the end of a work round report:

- branch name and commit SHA(s);
- exact production files changed;
- tests/fixtures/regression executed and their actual results;
- candidate/JAR identity if one was built;
- evidence paths produced;
- known failures, PARTIAL results, skipped tests and limitations;
- one recommended next engineering breakpoint.

If a parser/auditor was changed after a raw run, preserve the original raw and explain the correction rather than rewriting history.

## Credentials

Use the platform's GitHub connection/OAuth when available. Never commit tokens, PATs, credentials, secrets, or user-local paths containing private data. If write access is unavailable, produce a clean patch/branch-ready commit description and say so explicitly.