import java.awt.geom.Point2D;
import java.util.List;

/** Integration checks for upstream combat patterns with the fork's movement contract. */
public final class MilitaryArchitectureTest {
    public static void main(String[] args) throws Exception {
        testWeaponUpgradesAndReload();
        testLowMagazineReloadsWhenSafe();
        testCivilianProtectionAndSquadTargets();
        testInactiveSoldiersDoNotReserveTargets();
        testExplicitPatrolSkipsBlockedWaypoint();
        testDrawingPreservesGraphicsState();
        System.out.println("MilitaryArchitectureTest passed");
    }

    private static void testWeaponUpgradesAndReload() throws Exception {
        Weapon rifle = new Rifle();
        Weapon scoped = new ScopeDecorator(rifle);
        Weapon upgraded = new ExtendedMagazineDecorator(scoped);
        check(rifle.range() == 140 && rifle.magazineSize() == 6, "Original rifle stays unchanged");
        check(upgraded.range() == 210 && upgraded.magazineSize() == 12
                        && upgraded.fireIntervalTicks() == 15 && upgraded.reloadTicks() == 42,
                "Stacked upgrades retain each other's effects and trade-offs");
        World world = closedWorld();
        Military soldier = new Military(300, 300);
        world.addEntity(soldier);
        check(soldier.getStateName().equals("Wandering"), "Default movement remains independent");
        for (int kills = 1; kills <= 6; kills++) {
            Zombie target = new Zombie(soldier.getX() + 50, soldier.getY());
            world.addEntity(target);
            for (int tick = 0; tick < 100 && target.isActive(); tick++) soldier.update(world);
            check(!target.isActive() && soldier.getKills() == kills, "Every target is shot once");
            if (kills == 2) check(soldier.getRank() == Rank.VETERAN
                    && soldier.getWeapon().range() == 210, "Second kill earns scope");
            if (kills == 4) check(soldier.getRank() == Rank.ELITE
                    && soldier.getWeapon().magazineSize() == 12, "Fourth kill earns magazine");
            world.removeInactiveEntities();
        }
        check(soldier.getAmmo() == 0, "Upgrading capacity does not conjure extra rounds");
        Zombie waiting = new Zombie(soldier.getX() + 50, soldier.getY());
        world.addEntity(waiting);
        soldier.update(world);
        check(soldier.getStateName().equals("Reloading") && waiting.isActive(),
                "Empty rifle enters reload without firing");
        for (int tick = 1; tick < soldier.getWeapon().reloadTicks(); tick++) soldier.update(world);
        check(soldier.getAmmo() == 0 && waiting.isActive(), "Reload cannot finish early");
        soldier.update(world);
        check(soldier.getAmmo() == 12 && soldier.getStateName().equals("Engaging"),
                "Reload refills at the weapon's exact duration");
        soldier.update(world);
        check(!waiting.isActive() && soldier.getAmmo() == 11, "Combat resumes after reload");
        try {
            soldier.shoot();
            throw new AssertionError("Inactive target must be rejected");
        } catch (NoTargetException expected) { }
    }

    private static void testCivilianProtectionAndSquadTargets() {
        World world = closedWorld();
        Military soldier = new Military(300, 300);
        Zombie nearest = new Zombie(300, 350);
        Zombie dangerous = new Zombie(450, 300);
        world.addEntity(soldier);
        world.addEntity(nearest);
        world.addEntity(dangerous);
        world.addEntity(new Human(470, 300));
        check(soldier.findThreat(world).orElseThrow() == dangerous,
                "Civilian danger takes priority over soldier distance");

        world = closedWorld();
        Military closer = new Military(200, 300);
        Military farther = new Military(260, 300);
        Zombie first = new Zombie(200, 500);
        Zombie second = new Zombie(330, 500);
        world.addEntity(closer);
        world.addEntity(farther);
        world.addEntity(first);
        world.addEntity(second);
        closer.update(world);
        farther.update(world);
        check(closer.getTarget() == first && farther.getTarget() == second,
                "Soldiers spread across live threats");
    }

    private static void testLowMagazineReloadsWhenSafe() {
        World world = closedWorld();
        Military soldier = new Military(300, 300);
        world.addEntity(soldier);
        for (int kills = 0; kills < 4; kills++) {
            Zombie zombie = new Zombie(soldier.getX() + 50, soldier.getY());
            world.addEntity(zombie);
            for (int tick = 0; tick < 100 && zombie.isActive(); tick++) soldier.update(world);
            check(!zombie.isActive(), "Soldier clears each threat before the next arrives");
            world.removeInactiveEntities();
        }
        check(soldier.getAmmo() == 2, "A promoted soldier retains its two remaining rounds");
        soldier.update(world);
        check(soldier.getStateName().equals("Reloading"), "A safe soldier tops up a low magazine");
        for (int tick = 0; tick <= soldier.getWeapon().reloadTicks(); tick++) soldier.update(world);
        check(soldier.getAmmo() == 12 && soldier.getStateName().equals("Wandering")
                        && soldier.getTarget() == null,
                "A safe reload returns to independent movement with no stale target");
    }

    private static void testInactiveSoldiersDoNotReserveTargets() {
        World world = closedWorld();
        Military closer = new Military(200, 300);
        Military farther = new Military(260, 300);
        Zombie target = new Zombie(200, 500);
        world.addEntity(closer);
        world.addEntity(farther);
        world.addEntity(target);
        closer.update(world);
        closer.deactivate();
        check(farther.findThreat(world).orElseThrow() == target,
                "An inactive soldier cannot keep another soldier from defending civilians");
    }

    private static void testExplicitPatrolSkipsBlockedWaypoint() {
        World world = closedWorld();
        Military soldier = new Military(300, 300, List.of(
                new Point2D.Double(-500, 300), new Point2D.Double(300, 400)));
        boolean returned = false;
        for (int tick = 0; tick < 500; tick++) {
            soldier.update(world);
            if (soldier.getY() > 390) returned = true;
        }
        check(returned, "Unreachable route points cannot pin a soldier against the boundary");
    }

    private static World closedWorld() {
        return new World(1000, 750, (world, tick) -> { });
    }

    private static void testDrawingPreservesGraphicsState() {
        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(
                400, 400, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setColor(java.awt.Color.BLUE);
            java.awt.Font font = graphics.getFont();
            java.awt.Stroke stroke = graphics.getStroke();
            Military soldier = new Military(200, 200);
            World world = closedWorld();
            world.addEntity(soldier);
            world.addEntity(new Zombie(200, 250));
            soldier.update(world);
            soldier.draw(graphics);
            check(graphics.getColor().equals(java.awt.Color.BLUE)
                            && graphics.getFont().equals(font) && graphics.getStroke().equals(stroke),
                    "Combat and status overlays cannot change how later entities render");
        } finally {
            graphics.dispose();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
