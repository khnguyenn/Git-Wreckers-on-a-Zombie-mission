public final class HumanFoodTest {

    private HumanFoodTest() {
    }

    public static void main(String[] args) {
        testEnergyDecreasesAfterUpdate();
        testInvalidFoodEnergyIsRejected();
        testEatingIncreasesEnergyAndConsumesFood();
        testDistantFoodCannotBeEaten();
        testFoodExpiresAfter300Updates();
        testHumanEntersAndLeavesBuilding();
        testStarvationDeactivatesHuman();
        testSimulationAssetsLoadWithoutSourceClasspath();
        System.out.println("HumanFoodTest passed");
    }

    private static void testEnergyDecreasesAfterUpdate() {
        Human human = new Human(500, 500);
        double initialEnergy = human.getEnergy();

        human.update(new World(1000, 1000));

        assert human.getEnergy() < initialEnergy;
    }

    private static void testInvalidFoodEnergyIsRejected() {
        boolean rejected = false;
        try {
            new Food(0, 0, 0);
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }

        assert rejected;
    }

    private static void testEatingIncreasesEnergyAndConsumesFood() {
        Human human = new Human(100, 100);
        human.update(new World(1000, 1000));
        double energyBeforeEating = human.getEnergy();
        Food food = new Food(100, 100, 20);

        assert human.eat(food);
        assert human.getEnergy() > energyBeforeEating;
        assert !food.isActive();
    }

    private static void testDistantFoodCannotBeEaten() {
        Human human = new Human(100, 100);
        Food food = new Food(200, 100, 20);

        assert !human.eat(food);
        assert food.isActive();
    }

    private static void testFoodExpiresAfter300Updates() {
        Food food = new Food(0, 0, 20);
        World world = new World(1000, 1000);

        for (int tick = 0; tick < 299; tick++) {
            food.update(world);
        }
        assert food.isActive();

        food.update(world);
        assert !food.isActive();
    }

    private static void testHumanEntersAndLeavesBuilding() {
        Human human = new Human(120, 140);
        Building building = new Building(100, 100, 40, 40, 1);

        assert human.enterBuilding(building);
        assert human.isHidden();
        assert human.getShelter() == building;
        assert building.getOccupantCount() == 1;

        human.leaveBuilding();
        assert !human.isHidden();
        assert human.getShelter() == null;
        assert building.getOccupantCount() == 0;
        assert human.getX() == building.getEntranceX();
        assert human.getY() == building.getEntranceY();
    }

    private static void testStarvationDeactivatesHuman() {
        Human human = new Human(500, 500);
        World world = new World(1000, 1000);

        for (int tick = 0; tick < 600 && human.isActive(); tick++) {
            human.update(world);
        }

        assert !human.isActive();
    }

    private static void testSimulationAssetsLoadWithoutSourceClasspath() {
        World world = new World(1000, 750);

        new Building(155, 125, 150, 100);
        new SimPanel(world);
    }
}
