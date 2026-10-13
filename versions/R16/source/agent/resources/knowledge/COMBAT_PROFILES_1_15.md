# Combat profile calibration, 2026-10-04

Engine identity: Rusted Warfare PC 1.15 Build28, game-lib.jar SHA256
`8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9`.
Runtime uses profiles only when the existing catalog/game identity witness matches.
These mechanics are static evidence; no live hidden enemy data is read.

| Profile | max HP | direct damage | reload (60 ticks/game second) | range | max speed/tick | sources |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| tank / c_tank | 210 | 25 | 75 | 130 | 1.1 | assets/units/tanks/tank.ini core, attack, projectile_1, movement |
| scout | 350 | 17 | 50 | 110 | 1.0 | assets/units/scout/scout.ini core, attack, projectile_1, movement |
| heavyTank | 600 | 50 | 70 | 160 | 0.8 | javap -c -p com.corrodinggames.rts.game.units.e.f constructor cv/cu, q(int), a(am,int) projectile U, b(int), m(), z() |
| commandCenter | 4000 | 70 | 70 | 280 | 0 | javap -c -p com.corrodinggames.rts.game.units.d.e constructor cv/cu, q(int), a(am,int) projectile U, b(int), m() |

Original config SHA256: tank `11e259de2ddf7329458585e645811faef4a1c56b29a265b7c044001fd93b5359`,
scout `b9af06cde3b3d0b00b23ada976c50ec5b8a5024b5760f55d7edf3f253d37741b`.
This round's read-only javap evidence is retained locally at
`G:/deepseek 工作台/_baseline_audit/fight-static-20261004/`.

Community candidate file 星星版铁锈机制库_清洗转写.txt was a calibration lead only.
Its base HP3000 is rejected in favor of verified native4000. The model always uses
current lawful observed HP, never static maxHP in place of current HP.

Surface/air compatibility for tank/scout comes from the verified configuration.
HeavyTank a(am,int) branches on target.i() into surface/air projectile branches;
commandCenter l() is true and a(am,int) has an ungated target-directed projectile.
Their profiles are fixed surface units, so current own type identifies their surface
domain. No amphibious or submerged profile is supplied; those remain UNKNOWN.

Nominal DPS is directDamage*60/reloadTicks. TTK is observed targetHP/current eligible
own nominalDPS. Expected exchange loss includes a bounded approach exposure of the
defender's range advantage divided by nominal movement speed, then nominal enemyDPS
times (TTK + approach exposure) / observed ownHP. The 0.55 expected-loss admission
budget and 60-unit remembered-zone margin are policy choices, not native constants.

This is a transparent coarse model, not a simulator. Formation, blocked movement,
turning, fire dispersion, focus fire, projectile travel, AoE, damage multipliers,
repair and ally support are not modeled. No model result claims execution, causal
benefit, kills, survival, desktop acceptance or guaranteed victory. Unknown profiles
keep the older conservative HP fallback explicitly labeled UNKNOWN. A legally
observed unarmed building contributes zero incoming DPS regardless of its HP.
