# Architecture integration and demonstration guide

## Source and scope

This integration builds on fork main `965b54d` and selectively adapts the team's
[upstream implementation at ce4f973](https://github.com/ShahriarHasan001533/Git-Wreckers-on-a-Zombie-mission/tree/ce4f973a070c0df35bb128fbd69e6a9e185e9cb6/COMP2000/src).
Military states, weapon decorators, ranks and shelter admission policies originate
from that team implementation. The integration adapts their contracts to the fork;
it does not establish individual authorship of the original work.

The fork keeps its root `src/` layout, Java 11 baseline, population settings,
independent military wandering, spawn strategy, world observers, population
history, civilian statistics collector, optional reports and synchronized feeding.
Zombie infection and targeting are unchanged. Medics, traps, zombie waves,
the upstream military report collector and its project-folder reorganisation
are outside this integration.

## State: separate combat modes from movement

Before: `Military.update()` chose idle or hunting movement and attempted a shot
every tick. There was no ammunition or reload lifecycle.

After: `Military.update()` advances cooldowns and delegates to `MilitaryState`.
`PatrolState` runs each soldier's own `WanderBehaviour`, or a supplied
`PatrolBehaviour`. A detected threat enters `EngageState` on the same tick.
`EngageState` pursues and shoots, returns to idle movement when threats disappear,
or enters `ReloadingState` when ammunition is empty or low after combat.
Reloading lasts the weapon's configured number of ticks.

State is justified because combat modes own different actions and transitions.
Movement remains a Strategy because choosing how to move is separate from
choosing when to fight or reload. A single enum and switch would use fewer
classes, but would concentrate every mode's transitions and actions in Military.
Separate states add navigation overhead; they are useful here because reloads
make the lifecycle more than a choice between two movement algorithms.

Example: a recruit starts with six rounds. After six kills its upgraded rifle
has no rounds left. The next threat triggers a 42-tick reload; the soldier then
has 12 rounds and resumes firing. Capacity upgrades do not grant free ammunition.

## Decorator: compose weapon upgrades

Before: range and firing interval were constants in Military.

After: a `Rifle` implements `Weapon`; each upgrade wraps the current weapon.
At two kills, `ScopeDecorator` adds 70 pixels of range and three ticks between
shots. At four kills, `ExtendedMagazineDecorator` adds six rounds of capacity
and 12 ticks of reload time. Rank uses those same kill thresholds.

Decorators allow upgrades to combine without a subclass for every combination.
Flags inside one weapon class would be simpler for a permanently fixed set of
two upgrades, but would accumulate conditional logic as combinations grow.
The chosen design adds small forwarding classes and indirection. The slower
firing and reload times make the benefits carry observable costs.

## Strategy: make shelter admission a building decision

Before: any active survivor could use any free shelter place.

After: existing constructors still create open shelters. A building can instead
receive an `AdmissionPolicy`. The second starting building has capacity three,
with two places reserved for survivors at or below 60 energy. Healthy survivors
can use its first general place or choose the open shelter. Hungry survivors
still leave shelter at the fork's existing energy threshold.

Both autonomous selection and actual entry call the person-aware admission rule.
Anonymous entry cannot bypass reservation. Rejected transfers keep the person's
current shelter and both buildings' occupancy intact.

Strategy keeps the choice local to each building and avoids emergency-policy
branches in Human. Building subclasses could also express the two policies,
but composition lets the same building implementation use either rule.
There is no saved-world migration: simulations live in memory, and startup
and reset create fresh buildings with the configured policies.

## Streams, parallelism and integration boundaries

Military threat selection uses streams to prioritise zombies nearest visible
civilians, then soldier distance. Soldiers defer to a closer active, armed
soldier already engaging that target. Inactive soldiers cannot reserve targets.
This prioritisation scans nearby entities and other soldiers; it is intended
for the coursework's small populations, not a large-scale simulation engine.

`WorldStatistics.collector()` remains the single civilian-health collector.
It feeds immutable snapshots to the display, history and optional text reports.
`ParallelFeeding.feed()` remains a separate operation on otherwise idle entities;
the normal Swing simulation still updates sequentially. Parallel workers must
not run concurrently with world updates or rendering. `Human.eat()` retains the
shared-food lock across checking availability and consuming the item.

## Reproduce the checks

Compile against Java 11, then run every test entry point:

```sh
javac --release 11 -d out src/*.java
javac --release 11 -cp out -d out test/*.java
for test_file in test/*Test.java; do
  test_class=$(basename "$test_file" .java)
  java -ea -Djava.awt.headless=true -cp "out:src" "$test_class" || exit 1
done
```

`MilitaryArchitectureTest` covers upgrade composition, rank thresholds,
ammunition, exact reload timing, threat selection, squad target sharing,
inactive soldiers, blocked patrol points and graphics-state isolation.
`MilitaryMovementTest` retains the fork's movement and cooldown regression checks.
`AdmissionPolicyTest` covers admission, capacity, rejected transfers and autonomous
shelter choice. Existing tests continue covering reset, settings, observers,
reports, statistics collection, infection and the parallel feeding race.

For a presentation, run `java -cp "out:src" Main --report`, select populations,
and start with R. Soldier labels show state, ammunition and rank; building labels
show policy and occupancy. P pauses and +/- changes speed. R resets the world
while retaining selected population counts and the spawn policy.

The final reflection should describe actual contributions, decisions, results
and limitations. These design notes provide evidence to discuss; they do not
replace timely logbook entries or claim a particular mark.
