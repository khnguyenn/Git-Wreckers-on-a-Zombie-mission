# Team changes retained in the fork

Base: fork `main` at `5b5c970`.
Upstream source: team `main` at `7e6abeb`.

This branch selectively ports team changes into the fork's existing layout.
It does not merge upstream history or replace the fork with the upstream tree.

## Imported and adapted

| Team change | Source | Adaptation |
| --- | --- | --- |
| Generic food search and checked missing-target handling | `ca142d8`, `d42112c` | Keep `/assets/` paths and no-food wandering |
| Human/Food regression checks | `d42112c`, `7e6abeb` | Keep the team test in root `test/` |
| Building/background filesystem asset loading | `7e6abeb` | Use root `src/assets/`; retain Building image validation |
| Zombie sprite-sheet rendering | `7a142fd`, current upstream `Zombie.java` | Retain fork targeting/public methods and individual-image fallback |
| CI test execution | Current upstream workflow | Run all root test entry points and both resource-loading modes |

The fork's Human, Food, Military and Zombie filesystem loaders now resolve
`assets/...` relative to `src/`, rather than repeating the `assets` directory.
The local World test is retained in root `test/` without changing its checks.

## Preserved fork behaviour

- `Main`, `SimulationSettings`, `World` and movement strategies retain their code.
- Initial human, military and zombie counts remain configurable.
- Starting positions remain random within the existing margin.
- Reset reuses selected populations and restores tick zero/default speed.
- Start, pause/resume, keyboard speed controls and occupancy display remain.
- World updates and repaint requests retain their separate Swing timer rates.
- Zombie targeting retains exact-radius inclusion and ignores inactive,
  sheltered and military entities. Infection removes a victim without creating
  another zombie.
- All original image assets remain available. The optional team sprite sheet
  supplies four front and side walking frames; missing/unreadable sheets use
  the original individual images.

## Scope

No Observer, spawning Strategy, State, parallel entity updates, balance changes,
patrol-route fix or population-validation rule change is included. Those remain
separate work. No open upstream PR is treated as accepted code.
