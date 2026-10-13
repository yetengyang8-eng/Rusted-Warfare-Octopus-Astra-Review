# Rusted Warfare Agent

A research and engineering project for building a controllable AI agent around **Rusted Warfare 1.15 (PC Build #28 / Game Code 176)**.

This public mirror contains the Agent source, tests, tooling, project-state notes, selected engineering handoffs, and the current knowledge packet used for capability / terrain / recon work.

## Current candidate

- KnowledgeBacked World Model / Capability v0
- Agent JAR SHA-256: `5741e241ce8ec1b921f36ed3274087609d0430f9281c44f6f62c096de84c5f54`
- Required vanilla `game-lib.jar` SHA-256: `8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9`
- The vanilla game and its binaries/assets are **not distributed here**.

## Repository layout

- `agent/` — Java agent source, test harnesses and build scripts
- `tools/` — report analysis, headless runner, A/B and tactical-viewer tooling
- `docs/` — architecture, API, validation and roadmap notes
- `project-state/` — current project state and Codex handoff
- `handoff/` — selected engineering contracts / handoffs (chat transcripts excluded)
- `knowledge/` — distilled terrain, movement, vision and targeting knowledge packet
- `evidence/` — selected Agent-POV tactical viewer output

## Build

You must supply your own compatible Rusted Warfare 1.15 `game-lib.jar`.

See `agent/README_CN.md` for the full build and regression workflow.

Windows example:

```powershell
agent\build.bat "<path-to-game-lib.jar>"
powershell -NoProfile -ExecutionPolicy Bypass -File agent\test-win.ps1 <game-lib.jar> <libs-directory>
```
