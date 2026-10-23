public class SimulationSettingsTest {

    public static void main(String[] args) {
        testValidPopulationIsAccepted();
        testTooManyZombiesAreRejected();
        testNegativePopulationIsRejected();
        testLargeValidPopulationsDoNotOverflow();
        testLargeRatioBoundaryIsEnforced();

        System.out.println("Simulation settings tests passed");
    }

    private static void testValidPopulationIsAccepted() {
        SimulationSettings settings = new SimulationSettings(3, 2, 10);

        check(settings.getHumanCount() == 3,
                "Human count should be retained");
        check(settings.getMilitaryCount() == 2,
                "Military count should be retained");
        check(settings.getZombieCount() == 10,
                "Zombie count should be retained");
    }

    private static void testTooManyZombiesAreRejected() {
        expectException(() -> new SimulationSettings(3, 2, 11),
                "Zombie count should not exceed twice the human and military count");
    }

    private static void testNegativePopulationIsRejected() {
        expectException(() -> new SimulationSettings(-1, 2, 1),
                "Negative population counts should be rejected");
    }

    private static void testLargeValidPopulationsDoNotOverflow() {
        SimulationSettings multiplied = new SimulationSettings(1 << 30, 0, Integer.MAX_VALUE);
        check(multiplied.getZombieCount() == Integer.MAX_VALUE,
                "Doubling a valid population must not overflow the ratio limit");
        SimulationSettings added = new SimulationSettings(
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        check(added.getHumanCount() == Integer.MAX_VALUE
                        && added.getMilitaryCount() == Integer.MAX_VALUE,
                "Adding large civilian and military counts must retain their valid values");
    }

    private static void testLargeRatioBoundaryIsEnforced() {
        SimulationSettings exact = new SimulationSettings(536_870_911, 536_870_912, Integer.MAX_VALUE - 1);
        check(exact.getZombieCount() == Integer.MAX_VALUE - 1,
                "A population exactly at the large ratio boundary should be accepted");
        expectException(() -> new SimulationSettings(536_870_911, 536_870_912, Integer.MAX_VALUE),
                "One zombie over the large ratio boundary should still be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectException(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected result.
        }
    }
}
