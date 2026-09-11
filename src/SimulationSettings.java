/** Immutable population settings for a simulation run. */
public final class SimulationSettings {

    private final int humanCount;
    private final int militaryCount;
    private final int zombieCount;

    public SimulationSettings(
            int humanCount,
            int militaryCount,
            int zombieCount
    ) {
        if (humanCount < 0 || militaryCount < 0 || zombieCount < 0) {
            throw new IllegalArgumentException(
                    "Population counts cannot be negative"
            );
        }

        if (zombieCount > 2 * (humanCount + militaryCount)) {
            throw new IllegalArgumentException(
                    "Zombie count cannot exceed twice the human and military count"
            );
        }

        this.humanCount = humanCount;
        this.militaryCount = militaryCount;
        this.zombieCount = zombieCount;
    }

    public int getHumanCount() {
        return humanCount;
    }

    public int getMilitaryCount() {
        return militaryCount;
    }

    public int getZombieCount() {
        return zombieCount;
    }
}
