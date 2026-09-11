public class SimulationSettingsTest {

    public static void main(String[] args) {
        testValidPopulationIsAccepted();
        testTooManyZombiesAreRejected();
        testNegativePopulationIsRejected();

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
