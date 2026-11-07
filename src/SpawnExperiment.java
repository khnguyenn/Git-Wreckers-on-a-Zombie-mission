import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.stream.Collectors;

/** Compares the normal periodic and closed spawning modes using Main's initial scene. */
public final class SpawnExperiment {
    private static final String USAGE =
            "Usage: SpawnExperiment [trials] [ticks] [layoutSeed] [civilians] [military] [zombies]";

    private SpawnExperiment() { }

    /** CSV goes to stdout; experiment settings, limitations and summaries go to stderr. */
    public static void main(String[] args) {
        if (args.length > 6) throw new IllegalArgumentException(USAGE);
        int trials = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : 600;
        long layoutSeed = args.length > 2 ? Long.parseLong(args[2]) : 20261107L;
        SimulationSettings settings = new SimulationSettings(
                args.length > 3 ? Integer.parseInt(args[3]) : 3,
                args.length > 4 ? Integer.parseInt(args[4]) : 2,
                args.length > 5 ? Integer.parseInt(args[5]) : 1);
        if (trials <= 0 || ticks <= 0) {
            throw new IllegalArgumentException("Trials and ticks must be positive. " + USAGE);
        }
        System.setProperty("java.awt.headless", "true");
        System.err.printf(Locale.ROOT,
                "trials_per_mode=%d ticks=%d first_layout_seed=%d initial_civilians=%d "
                        + "initial_military=%d initial_zombies=%d%n",
                trials, ticks, layoutSeed, settings.getHumanCount(),
                settings.getMilitaryCount(), settings.getZombieCount());
        System.err.println("Initial positions are matched by layout seed. Entity wandering uses "
                + "independent unseeded generators, so full runs are not exactly reproducible.");
        System.err.println("Periodic mode changes both food supply and human arrivals. "
                + "Final counts include arrivals on the terminal tick before their first update.");
        System.out.println("trial,mode,layout_seed,ticks,initial_civilians,initial_military,"
                + "initial_zombies,final_civilians,original_survivors,final_military,final_zombies,"
                + "food,mean_civilian_energy,lowest_civilian_energy,sheltered_civilians");
        List<TrialResult> periodicResults = new ArrayList<>();
        List<TrialResult> closedResults = new ArrayList<>();
        for (int trial = 0; trial < trials; trial++) {
            long seed = layoutSeed + trial;
            TrialResult periodic = runTrial(settings, ticks, seed, false);
            TrialResult closed = runTrial(settings, ticks, seed, true);
            periodicResults.add(periodic);
            closedResults.add(closed);
            printRow(trial + 1, "periodic", seed, settings, periodic);
            printRow(trial + 1, "closed", seed, settings, closed);
        }
        printSummary("periodic", settings.getHumanCount(), periodicResults);
        printSummary("closed", settings.getHumanCount(), closedResults);
    }

    /** Runs sequentially with independent entities and a fresh policy for every trial. */
    static TrialResult runTrial(SimulationSettings settings, int ticks, long layoutSeed,
                                boolean closedPopulation) {
        Objects.requireNonNull(settings, "Settings cannot be null");
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        SpawnPolicy policy = closedPopulation ? (world, tick) -> { }
                : new PeriodicSpawnPolicy(new Random(layoutSeed ^ 0x9E3779B97F4A7C15L));
        World world = new World(1000, 750, policy);
        Main.populateWorld(world, settings, new Random(layoutSeed));
        List<Human> originalCivilians = world.getEntities().stream()
                .filter(entity -> entity.getClass() == Human.class)
                .map(Human.class::cast)
                .collect(Collectors.toList());
        for (int tick = 0; tick < ticks; tick++) world.update();
        long survivors = originalCivilians.stream().filter(Entity::isActive).count();
        return new TrialResult(world.getStatistics(), survivors);
    }

    private static void printRow(int trial, String mode, long seed, SimulationSettings settings,
                                 TrialResult result) {
        WorldStatistics statistics = result.statistics;
        System.out.printf(Locale.ROOT, "%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%d%n",
                trial, mode, seed, statistics.getTick(), settings.getHumanCount(),
                settings.getMilitaryCount(), settings.getZombieCount(), statistics.getHumanCount(),
                result.originalSurvivors, statistics.getMilitaryCount(), statistics.getZombieCount(),
                statistics.getFoodCount(), csvEnergy(statistics.getAverageCivilianEnergy()),
                csvEnergy(statistics.getLowestCivilianEnergy()), statistics.getShelteredCivilianCount());
    }

    private static String csvEnergy(OptionalDouble energy) {
        return energy.isPresent() ? String.format(Locale.ROOT, "%.6f", energy.getAsDouble()) : "";
    }

    private static void printSummary(String mode, int initialCivilians, List<TrialResult> results) {
        long survivors = results.stream().mapToLong(result -> result.originalSurvivors).sum();
        long civilians = results.stream().mapToLong(result -> result.statistics.getHumanCount()).sum();
        double energy = results.stream().mapToDouble(result ->
                result.statistics.getAverageCivilianEnergy().orElse(0)
                        * result.statistics.getHumanCount()).sum();
        String survivalRate = initialCivilians == 0 ? "n/a" : String.format(Locale.ROOT, "%.2f",
                100.0 * survivors / ((long) initialCivilians * results.size()));
        String pooledEnergy = civilians == 0 ? "n/a"
                : String.format(Locale.ROOT, "%.2f", energy / civilians);
        double meanZombies = results.stream().mapToLong(result ->
                result.statistics.getZombieCount()).average().orElse(0);
        System.err.printf(Locale.ROOT,
                "mode=%s trials=%d mean_final_civilians=%.2f mean_original_survivors=%.2f "
                        + "original_survival_pct=%s mean_final_zombies=%.2f "
                        + "pooled_final_civilian_energy=%s%n",
                mode, results.size(), (double) civilians / results.size(),
                (double) survivors / results.size(), survivalRate, meanZombies, pooledEnergy);
    }

    static final class TrialResult {
        final WorldStatistics statistics;
        final long originalSurvivors;

        private TrialResult(WorldStatistics statistics, long originalSurvivors) {
            this.statistics = statistics;
            this.originalSurvivors = originalSurvivors;
        }
    }
}
