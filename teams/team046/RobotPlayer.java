package team046;

import battlecode.common.*;

public class RobotPlayer {

    private static RobotController rc;
    private static int round;
    private static double power;
    private static int zergRushChannel = 10104;
    private static int zergRushCode = 31337;

	public static void run(RobotController MyJohn12LongRC) {
        rc = MyJohn12LongRC;

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
            if (rc.senseEnemyNukeHalfDone() && rc.readBroadcast(zergRushChannel) != zergRushCode) {
                rc.broadcast(zergRushChannel, zergRushCode);
            }
            else if (round > 250  && !rc.hasUpgrade(Upgrade.FUSION)) {
                rc.researchUpgrade(Upgrade.FUSION);
                return;
            }
            else if (round > 500  && !rc.hasUpgrade(Upgrade.VISION)) {
                rc.researchUpgrade(Upgrade.VISION);
                return;
            }
            else {
                Robot[] myBuddies = rc.senseNearbyGameObjects(Robot.class, 33, rc.getTeam());
                if (round > 500 && myBuddies.length > 20) {
                    rc.researchUpgrade(Upgrade.NUKE);
                    return;
                }
            }

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
            MapLocation rLoc = rc.getLocation();
            MapLocation targetLoc;
            int supplierBuilderChannel = 10111;
            int SupplierBuilderID = -1;

            if (power > GameConstants.BROADCAST_READ_COST) {
                SupplierBuilderID = rc.readBroadcast(supplierBuilderChannel);
                power -= GameConstants.BROADCAST_READ_COST;
            }

            // Handle supplier builder robot (including movement)
            if (power > GameConstants.BROADCAST_SEND_COST
                    && (SupplierBuilderID == 0 || SupplierBuilderID == rc.getRobot().getID())) {
                if (SupplierBuilderID == 0) {
                    rc.broadcast(supplierBuilderChannel, rc.getRobot().getID());
                    power -= GameConstants.BROADCAST_SEND_COST;
                }
                BuildSupplier(rLoc);
                return;
            }

            // Check for zerg command
            if (power > GameConstants.BROADCAST_READ_COST && rc.readBroadcast(zergRushChannel) == zergRushCode) {
                targetLoc = rc.senseEnemyHQLocation();
            }
            else {
                Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 3, rc.getTeam().opponent());
                if (nearbyEnemies.length > 0) {
                    return;
                }

                MapLocation rudeHQ = rc.senseEnemyHQLocation();
                MapLocation goodHQ = rc.senseHQLocation();
                Direction dir = goodHQ.directionTo(rudeHQ);
                int xm = 1, ym = 1;

                switch (dir) {
                    case NORTH: xm = 0; ym = -2; break;
                    case NORTH_EAST: xm = 2; ym = -2; break;
                    case EAST: xm = 2; ym = 0; break;
                    case SOUTH_EAST: xm = 2; ym = 2; break;
                    case SOUTH: xm = 0; ym = 2; break;
                    case SOUTH_WEST: xm = -2; ym = 2; break;
                    case WEST: xm = -2; ym = 0; break;
                    case NORTH_WEST: xm = -2; ym = -2; break;
                }

                targetLoc = new MapLocation(goodHQ.x + xm, goodHQ.y + ym);
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
            rc.setIndicatorString(2, String.valueOf(targetLoc));
            MoveRobot(rLoc, targetLoc);
        }
    }
}
