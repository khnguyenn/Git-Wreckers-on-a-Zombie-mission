import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class SpawnExperimentTest {
    public static void main(String[] args) throws Exception {
        testModesUseTheNormalSceneAndArrivalSchedule();
        testOriginalCohortExcludesSoldiersAndLaterArrivals();
        testCommandProducesCsvAndSeparateSummaries();
        testInvalidRunLengthIsRejected();
        System.out.println("SpawnExperimentTest passed");
    }

    private static void testModesUseTheNormalSceneAndArrivalSchedule() {
        SimulationSettings empty = new SimulationSettings(0, 0, 0);
        SpawnExperiment.TrialResult closed = SpawnExperiment.runTrial(empty, 100, 7, true);
        SpawnExperiment.TrialResult periodic = SpawnExperiment.runTrial(empty, 100, 7, false);
        check(closed.statistics.getTick() == 100 && periodic.statistics.getTick() == 100,
                "Both modes must complete the requested number of ticks");
        check(closed.statistics.getFoodCount() == 3 && closed.statistics.getHumanCount() == 0,
                "Closed mode retains Main's initial food without adding arrivals");
        check(periodic.statistics.getFoodCount() == 7 && periodic.statistics.getHumanCount() == 1,
                "Periodic mode adds four food items and one human by completed tick 100");
        check(periodic.statistics.getAverageCivilianEnergy().orElseThrow() == 100,
                "A terminal-tick arrival is counted before its first entity update");
        check(closed.statistics.getAverageCivilianEnergy().isEmpty(),
                "Energy must be absent when no civilians remain");
        check(SpawnExperiment.runTrial(empty, 300, 7, true).statistics.getFoodCount() == 0,
                "The experiment must execute normal food expiry rather than a simplified model");
    }

    private static void testOriginalCohortExcludesSoldiersAndLaterArrivals() {
        SpawnExperiment.TrialResult initial = SpawnExperiment.runTrial(
                new SimulationSettings(1, 1, 0), 1, 11, true);
        check(initial.originalSurvivors == 1 && initial.statistics.getMilitaryCount() == 1,
                "Military must not be counted as original civilians");
        SpawnExperiment.TrialResult arrivals = SpawnExperiment.runTrial(
                new SimulationSettings(0, 0, 0), 100, 11, false);
        check(arrivals.statistics.getHumanCount() == 1 && arrivals.originalSurvivors == 0,
                "A new arrival must not be reported as an original survivor");
    }

    private static void testCommandProducesCsvAndSeparateSummaries() throws Exception {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream csv = new ByteArrayOutputStream();
        ByteArrayOutputStream summaries = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(csv, true, "UTF-8");
             PrintStream err = new PrintStream(summaries, true, "UTF-8")) {
            System.setOut(out);
            System.setErr(err);
            SpawnExperiment.main(new String[]{"2", "100", "7", "0", "0", "0"});
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
        String[] rows = csv.toString(StandardCharsets.UTF_8.name()).trim().split("\\R");
        check(rows.length == 5 && rows[0].startsWith("trial,mode,layout_seed,ticks,"),
                "CSV must contain a header and one row per mode and trial");
        String[] periodic = rows[1].split(",", -1);
        String[] closed = rows[2].split(",", -1);
        check(periodic[1].equals("periodic") && closed[1].equals("closed")
                        && periodic[2].equals(closed[2]),
                "Each comparison must label both modes with the same initial-layout seed");
        String[] secondPeriodic = rows[3].split(",", -1);
        String[] secondClosed = rows[4].split(",", -1);
        check(secondPeriodic[2].equals(secondClosed[2])
                        && !secondPeriodic[2].equals(periodic[2]),
                "The next comparison must use a new layout seed shared by its two modes");
        check(periodic[7].equals("1") && periodic[8].equals("0") && closed[7].equals("0"),
                "Exported totals must distinguish immigrants from the original cohort");
        check(closed[12].isEmpty() && closed[13].isEmpty(),
                "CSV must leave absent civilian energy blank rather than inventing zero energy");
        String summary = summaries.toString(StandardCharsets.UTF_8.name());
        check(summary.contains("mode=periodic") && summary.contains("mode=closed")
                        && summary.contains("original_survival_pct=n/a"),
                "Summaries must stay on stderr and handle a zero-size initial cohort");
    }

    private static void testInvalidRunLengthIsRejected() {
        try {
            SpawnExperiment.runTrial(new SimulationSettings(0, 0, 0), 0, 1, true);
            throw new AssertionError("Zero-length experiments should be rejected");
        } catch (IllegalArgumentException expected) { }
        try {
            SpawnExperiment.main(new String[]{"0"});
            throw new AssertionError("Zero trials should be rejected before producing CSV");
        } catch (IllegalArgumentException expected) { }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
