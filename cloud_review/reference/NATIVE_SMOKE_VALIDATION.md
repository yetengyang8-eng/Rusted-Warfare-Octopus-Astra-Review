# Cloud adapter validation, 2026-10-13

This is a **local Windows smoke test of the portable cloud_review/run_spain.py adapter**, performed with the original, legitimately available game resources. It does NOT mean the cloud environment already has the engine.

## Input fingerprints

- Exact frozen R12 JAR: `61d8a341b3fff0445e67978d7deb2a6d4e349589e683a1f35afc35c9317b98cf`.
- Exact frozen R16 JAR: `3a1eeb33725c93279818e90a9812e21bfb0bbf04f079407143835054d8ee8ad5`.
- game-lib.jar: `8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9`.
- MP97 Spain .tmx: `342db6d8a8b8320b6a271b9e3c8a4c29c96a203e206c4be690c44bbef5882746`.

Both `--check-only` runs returned `INPUTS_OK`.

## Important runner compatibility discovered and corrected

R12 `HeadlessRunner.java` does **not** accept `--player`, and starts native player0 by default; R16 added the `--player 0..7` option and also defaults to player0. The first R12 attempted smoke included `--player 5` and correctly failed with the R12 runner usage message. This was **a harness option incompatibility, not an Agent ground-behavior failure**.

The portable adapter was corrected to omit `--player` for both frozen runners and to require controlled `player=0`. It no longer assumes R12 has `native-bootstrap.json` (R16 has one).

## Successful isolated 120-game-second starts

| Frozen Agent | GameTime reached | Final frame | player | outcome at budget | BattleClient exit | Meaning |
|---|---:|---:|---:|---|---:|---|
| R12 | 122832 ms | 7677 | 0 | ONGOING | 2 | Native headless start/bridge/client + budget exit passed |
| R16 | 123488 ms | 7718 | 0 | ONGOING | 2 | Native headless start/bridge/client + budget exit passed |

Native raw, input.json, start/end /state, monitor.jsonl and subprocess logs are archived under `cloud_review/reference/native_120s_smoke/r12-p0-smoke2/` and `r16-p0-smoke2/`.

**Do not interpret budget exit2 or ONGOING as a victory, or treat these 2-minute smokes as a valid R12/R16 quality comparison.** Full-length native matches and the requested R12/R16 behavioral review remain future work for Astra with a compatible runtime.
