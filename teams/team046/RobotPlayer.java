package team046;

import battlecode.common.*;

import java.util.Arrays;

public class RobotPlayer {

    private static RobotController rc;
    private static int round;
    private static double power;
    private static MapLocation rallyPoint;
    private static int zergRushChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int supplierBuilderChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);

	public static void run(RobotController MyJohn12LongRC) {
        rc = MyJohn12LongRC;

        // Set rally point
        if (rallyPoint == null) {
            MapLocation goodHQ = rc.senseHQLocation();
            MapLocation badHQ = rc.senseEnemyHQLocation();
            MapLocation midPoint = new MapLocation((badHQ.x + goodHQ.x) / 2, (badHQ.y + goodHQ.y) / 2);
            MapLocation qPoint = new MapLocation((midPoint.x + goodHQ.x) / 2, (midPoint.y + goodHQ.y) / 2);
            rallyPoint = new MapLocation((qPoint.x + goodHQ.x)/2, (qPoint.y + goodHQ.y)/2);
        }

		while (true) {
            try {
                round = Clock.getRoundNum();
                power = rc.getTeamPower();

				if (rc.getType() == RobotType.HQ) {
					HQ();
				}
                else if (rc.getType() == RobotType.SOLDIER) {
                    Soldier();
				}

				rc.yield();
			}
            catch (Exception e) {
				e.printStackTrace();
                break;
			}
		}
	}

    private static void HQ() throws GameActionException {
        if (rc.isActive()) {
            if (rc.senseEnemyNukeHalfDone()) {
                rc.broadcast(zergRushChannel, 1);
            }
            else if (round > 250  && !rc.hasUpgrade(Upgrade.FUSION)) {
                rc.researchUpgrade(Upgrade.FUSION);
                return;
            }
            else if (round > 500  && !rc.hasUpgrade(Upgrade.DEFUSION)) {
                rc.researchUpgrade(Upgrade.DEFUSION);
                return;
            }
            /*else if (round > 750 && !rc.hasUpgrade(Upgrade.VISION)) {
                rc.researchUpgrade(Upgrade.VISION);
                return;
            }
            else {
                Robot[] friendlyRobots = rc.senseNearbyGameObjects(Robot.class, rallyPoint, 100, rc.getTeam());
                if  (friendlyRobots.length >= 16) {
                    rc.researchUpgrade(Upgrade.NUKE);
                    return;
                }
            }*/

            // Find an available spawn direction
            MapLocation hqLocation = rc.senseHQLocation();
            MapLocation nextLoc;
            for (Direction dir : Direction.values()) {
                if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir)) {
                    nextLoc = hqLocation.add(dir);
                    if (rc.senseMine(nextLoc) == null) {
                        rc.spawn(dir);
                        break;
                    }
                }
            }
        }
    }

    private static void Soldier() throws GameActionException {
        if (rc.isActive()) {
            int supplierBuilderRobotID;
            MapLocation rLoc = rc.getLocation();

            // Set default rally point
            MapLocation targetLoc = rallyPoint;

            // Get the supplier builder robot ID (or zero)
            if (power > GameConstants.BROADCAST_READ_COST) {
                supplierBuilderRobotID = rc.readBroadcast(supplierBuilderChannel);
                power -= GameConstants.BROADCAST_READ_COST;
            }
            else {
                supplierBuilderRobotID = -1;
            }
            if (supplierBuilderRobotID == 0) {
                rc.broadcast(supplierBuilderChannel, rc.getRobot().getID());
                supplierBuilderRobotID = rc.getRobot().getID();
            }

            // Check for zerg command
            if (power > GameConstants.BROADCAST_READ_COST && rc.readBroadcast(zergRushChannel) == 1) {
                targetLoc = rc.senseEnemyHQLocation();
                power -= GameConstants.BROADCAST_READ_COST;
            }
            // Handle supplier builder robot (including movement)
            else if (supplierBuilderRobotID == rc.getRobot().getID()) {
                BuildSupplier(rLoc);
                return;
            }
            else {
                // Get scared
                Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 3, rc.getTeam().opponent());
                if (nearbyEnemies.length > 0) {
                    return;
                }
            }

            MoveRobot(rLoc, targetLoc);
        }
    }

    private static void MoveRobot(MapLocation rLoc, MapLocation targetLoc) throws GameActionException {
        Direction dir = rLoc.directionTo(targetLoc);
        if (dir == Direction.NONE) {
            return;
        }
        else if (dir == Direction.OMNI) {
            dir = Direction.EAST;
        }
        while (!rc.canMove(dir)) {
            dir = dir.rotateRight();
        }

        MapLocation nextLoc = rLoc.add(dir);

        if (rc.senseMine(nextLoc) != null) {
            rc.defuseMine(nextLoc);
        }
        else {
            rc.move(dir);
        }
    }

    private static void BuildSupplier(MapLocation rLoc) throws GameActionException {
        if (rc.senseEncampmentSquare(rLoc)) {
            rc.captureEncampment(RobotType.SUPPLIER);
        }
        else {
            MapLocation encampmentSquares[] = rc.senseAllEncampmentSquares();
            MapLocation targetLoc = encampmentSquares[0];
            int closest = 1000;

            for (MapLocation loc: encampmentSquares) {
                int dist = rLoc.distanceSquaredTo(loc);
                if (dist < closest) {
                    targetLoc = loc;
                    closest = dist;
                }
            }

            MoveRobot(rLoc, targetLoc);
        }
    }

    private static int randomWithRange(int min, int max) {
        int range = Math.abs(max - min) + 1;
        return (int)(Math.random() * range) + (min <= max ? min : max);
    }
}
