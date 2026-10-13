# Cloud-only Astra review package

**Start with [ASTRA_R12_R16_CLOUD_PROMPT.md](ASTRA_R12_R16_CLOUD_PROMPT.md).** This directory is for an Astra session that can access **this GitHub repo**, but **not** the user's Windows PC or local Desktop Commander.

## Contents

- `ASTRA_R12_R16_CLOUD_PROMPT.md`: direct, complete instructions about what to analyze, R12/R16 comparison, constraints and cloud run strategy.
- `run_spain.py`: the cloud-path adaptation of the previous Windows-absolute `spain_natural.py` used in R15/R16 validation. Same HeadlessRunner/Bridge/BattleClient natural game design, fixed Spain MP97, player0, difficulty0, speed5. R12 cannot select other slots; R16 supports --player but defaults to 0. The native quick-match is 8 slots and is not the human full 1v9 game. **Requires separately supplied licensed compatible engine files**. Inputs are SHA checked; output goes to `cloud_review/runs/`.
- `summarize_ground.py`: optional descriptive metrics comparing heavy-tank spatial dispersion from two new monitor.jsonl outputs. Does **not** determine General membership or attack legality.
- `reference/spain_natural.py`: verbatim archived historical Windows original, **not the portable entrypoint**.
- `reference/NATIVE_SMOKE_VALIDATION.md` and `reference/native_120s_smoke/`: locally completed 120-game-second native adapter checks for each frozen R12/R16 jar (both ONGOING at time budget; not quality tests).
- `reference/spain-r15-normal-p5-v2-summary.json`, `spain-r16-normal-p5-summary.json`, `air-isolation.json`, `human-audit.json`: key native historical outputs exposed as plain repository files for direct cloud review.

## Already in the repository

- `versions/R12/` and `versions/R16/`: complete frozen source, docs, JAR, hash, kit tools and manifest.
- `evidence/validation-archives/R12_native_validation_text_and_trace.zip` and `R16_native_validation_text_and_trace.zip`: archived validation scripts, native logs and failures, stored with Git LFS.
- `evidence/native-raw/R12/` and `R16/`: native run outputs and compressed battle files, also with LFS.
- `evidence/human-1v9-original/[10p] 10p 西班牙混战_by_MP97.tmx`: original MP97 Spain map, exact validated hash.
- `review/FULL_EVIDENCE_INDEX.md`: inventory of raw files.

## External game engine dependency

The public repository **does not include the proprietary game engine or resources**. This is a hard boundary, not a missing attachment that can be recovered by assuming access to G: paths.

To run the new native match, Astra's own cloud computer must have a legally available game resource directory containing:
```text
game-lib.jar
assets/
  tilesets/
libs/
res/
```
The exact expected game-lib SHA256 is in `run_spain.py`; get the source legitimately. Do not replace it with an arbitrary jar. Java and Python are required; some platform-native dependencies may also be necessary. An engine-less cloud session can still do comprehensive source/trace comparisons, but cannot produce **fresh** verified Spain battle results.

## Cloud commands

```bash
git lfs pull
python cloud_review/run_spain.py --version R12 --resources-dir "$ENGINE_DIR" --check-only
python cloud_review/run_spain.py --version R16 --resources-dir "$ENGINE_DIR" --check-only

python cloud_review/run_spain.py --version R12 --resources-dir "$ENGINE_DIR" --label r12-normal-p0-01 --game-seconds 3600
python cloud_review/run_spain.py --version R16 --resources-dir "$ENGINE_DIR" --label r16-normal-p0-01 --game-seconds 3600

python cloud_review/summarize_ground.py --r12 cloud_review/runs/r12-normal-p0-01/monitor.jsonl --r16 cloud_review/runs/r16-normal-p0-01/monitor.jsonl
```

Do not interpret `ONGOING` after budget as a native win or native defeat; do not compare games with different map/engine/player/difficulty hashes.

**Verification performed on preparation day:** `run_spain.py` passes Python syntax checks, and its `--check-only` fingerprint validation passed for both original R12 and R16 jars with the originally validated local engine and map. Live end-to-end native checks, where present, should be recorded separately; do not infer cloud platform compatibility from those checks.
