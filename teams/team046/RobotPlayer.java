package team046;

import battlecode.common.*;

public class RobotPlayer {

    private static RobotController rc;
    private static int round;
    private static double power;
    private static int zergRushChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int zergRushCode = randomWithRange(2, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int EncampmentBuilderChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int EncampmentSearchStartedChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int FirstSupplierBuilt = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);

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
            else if (power > 500 || round > 2000) {
                Robot[] myBuddies = rc.senseNearbyGameObjects(Robot.class, 33, rc.getTeam());
                if (myBuddies.length > 30) {
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
            int EncampmentBuilderRobotID;
            int EncampmentSearchStartedRound;
            MapLocation rLoc = rc.getLocation();
            MapLocation targetLoc = null;

            // Get the Encampment builder robot ID (or zero)
            if (power > GameConstants.BROADCAST_READ_COST * 2) {
                EncampmentBuilderRobotID = rc.readBroadcast(EncampmentBuilderChannel);
                EncampmentSearchStartedRound = rc.readBroadcast(EncampmentSearchStartedChannel);
                power -= GameConstants.BROADCAST_READ_COST * 2;
            }
            else {
                EncampmentBuilderRobotID = -1;
                EncampmentSearchStartedRound = 0;
            }
            if (EncampmentBuilderRobotID == 0
                    || EncampmentSearchStartedRound + GameConstants.CAPTURE_ROUND_DELAY * 2 < round) {
                rc.broadcast(EncampmentBuilderChannel, rc.getRobot().getID());
                if (power > GameConstants.BROADCAST_SEND_COST) {
                    rc.broadcast(EncampmentSearchStartedChannel, round);
                    power -= GameConstants.BROADCAST_SEND_COST * 2;
                }
                EncampmentBuilderRobotID = rc.getRobot().getID();
            }

            // Check for zerg command
            if (power > GameConstants.BROADCAST_READ_COST && rc.readBroadcast(zergRushChannel) == zergRushCode) {
                targetLoc = rc.senseEnemyHQLocation();
                power -= GameConstants.BROADCAST_READ_COST;
            }
            // Handle Encampment builder robot (including movement)
            else if (EncampmentBuilderRobotID == rc.getRobot().getID()) {
                BuildEncampment(rLoc);
                return;
            }
            else {
                // Get scared
                Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 3, rc.getTeam().opponent());
                if (nearbyEnemies.length > 0) {
                    return;
                }
            }

            // Set rally point
            if (targetLoc == null) {
                MapLocation goodHQ = rc.senseHQLocation();
                if (goodHQ.x <= 2 || goodHQ.x >= rc.getMapWidth() - 2) {
                    MapLocation rudeHQ = rc.senseEnemyHQLocation();
                    Direction dir = goodHQ.directionTo(rudeHQ);
                    int xm = 1, ym = 1;

                    switch (dir) {
                        case NORTH: xm = 0; ym = -3; break;
                        case NORTH_EAST: xm = 3; ym = -3; break;
                        case EAST: xm = 3; ym = 0; break;
                        case SOUTH_EAST: xm = 3; ym = 3; break;
                        case SOUTH: xm = 0; ym = 3; break;
                        case SOUTH_WEST: xm = -3; ym = 3; break;
                        case WEST: xm = -3; ym = 0; break;
                        case NORTH_WEST: xm = -3; ym = 3; break;
                    }

                    targetLoc = new MapLocation(goodHQ.x + xm, goodHQ.y + ym);
                }
                else {
                    MapLocation rallyPoints[] = {
                        new MapLocation(goodHQ.x + randomWithRange(1,2), goodHQ.y + randomWithRange(1,2)),
                        new MapLocation(goodHQ.x - randomWithRange(1,2), goodHQ.y - randomWithRange(1,2))
                    };
                    targetLoc = rallyPoints[randomWithRange(0, rallyPoints.length - 1)];
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

    private static void BuildEncampment(MapLocation rLoc) throws GameActionException {
        if (rc.senseEncampmentSquare(rLoc)) {
            if (power > GameConstants.BROADCAST_READ_COST + GameConstants.BROADCAST_SEND_COST
                    && rc.readBroadcast(FirstSupplierBuilt) == 0) {
                rc.captureEncampment(RobotType.SUPPLIER);
                rc.broadcast(FirstSupplierBuilt, 1);
                power -= GameConstants.BROADCAST_READ_COST + GameConstants.BROADCAST_SEND_COST;
            }
            else {
                rc.captureEncampment(RobotType.GENERATOR);
            }
        }
        else {
            MapLocation encampmentSquares[] = rc.senseAllEncampmentSquares();
            MapLocation goodEncampments[] = rc.senseAlliedEncampmentSquares();
            MapLocation targetLoc = encampmentSquares[0];
            int closest = 1000;

            checkLocations:
            for (MapLocation loc: encampmentSquares) {
                int dist = rLoc.distanceSquaredTo(loc);
                if (dist < closest) {
                    for (MapLocation goodLoc: goodEncampments) {
                        if (goodLoc.equals(loc)) {
                            continue checkLocations;
                        }
                    }
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
