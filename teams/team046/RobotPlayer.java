package team046;

import battlecode.common.*;

public class RobotPlayer {

    private static RobotController rc;
    private static int round;
    private static double power;
    private static int hoardNukeResearchMin = 50;
    private static int zergRushChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int zergRushCode = randomWithRange(2, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int EncampmentBuilderChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int EncampmentSearchStartedChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int SupplierBuilt = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int researchNukeChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);

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
                else if (rc.getType() == RobotType.ARTILLERY) {
                    Artillery();
                }
                /*else if (rc.getType() == RobotType.SUPPLIER) {
                    Supplier();
                }
                else if (rc.getType() == RobotType.GENERATOR) {
                    Generator();
                }*/

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
                if (rc.checkResearchProgress(Upgrade.NUKE) < 175
                        && power > GameConstants.BROADCAST_READ_COST + GameConstants.BROADCAST_SEND_COST * 2
                        && rc.readBroadcast(zergRushChannel) != zergRushCode) {
                    rc.broadcast(zergRushChannel, zergRushCode);
                    rc.broadcast(researchNukeChannel, round);
                }
            }
            /*else if (round > 50 && !rc.hasUpgrade(Upgrade.PICKAXE)) {
                rc.researchUpgrade(Upgrade.PICKAXE);
                return;
            }
            else if (round > 200 && !rc.hasUpgrade(Upgrade.FUSION)) {
                rc.researchUpgrade(Upgrade.FUSION);
                return;
            }
            else if (round > 250 && !rc.hasUpgrade(Upgrade.VISION)) {
                rc.researchUpgrade(Upgrade.VISION);
                return;
            }*/
            // Check the HQ's own surroundings
            else if ((round > 100 && power < 100)
                    || rc.senseNearbyGameObjects(Robot.class, 33, rc.getTeam()).length > hoardNukeResearchMin) {
                rc.researchUpgrade(Upgrade.NUKE);
                return;
            }

            // Last check for a nuke research cue
            if (power > GameConstants.BROADCAST_READ_COST) {
                int researchNuke = rc.readBroadcast(researchNukeChannel);
                power -= GameConstants.BROADCAST_READ_COST;
                if (researchNuke != 0 && round - researchNuke < 11) {
                    rc.researchUpgrade(Upgrade.NUKE);
                    return;
                }
                else if (power > GameConstants.BROADCAST_SEND_COST && researchNuke != 0) {
                    rc.broadcast(researchNukeChannel, 0);
                    power -= GameConstants.BROADCAST_SEND_COST;
                }
            }

            // Find an available spawn direction
            MapLocation hqLocation = rc.senseHQLocation();
            MapLocation nextLoc;
            for (Direction dir : Direction.values()) {
                if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir)) {
                    nextLoc = hqLocation.add(dir);
                    Team mine = rc.senseMine(nextLoc);
                    if (mine == null || mine == rc.getTeam()) {
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
            if (power > GameConstants.BROADCAST_SEND_COST * 2 && (EncampmentBuilderRobotID == 0
                    || EncampmentSearchStartedRound + GameConstants.CAPTURE_ROUND_DELAY < round)) {
                rc.broadcast(EncampmentBuilderChannel, rc.getRobot().getID());
                rc.broadcast(EncampmentSearchStartedChannel, round);
                power -= GameConstants.BROADCAST_SEND_COST * 2;
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
                RobotInfo roboData;
                Robot[] assholes = rc.senseNearbyGameObjects(Robot.class, 3, rc.getTeam().opponent());
                Robot[] baddies = rc.senseNearbyGameObjects(Robot.class, 33, rc.getTeam().opponent());
                Robot[] mypals = rc.senseNearbyGameObjects(Robot.class, 33, rc.getTeam());
                if (assholes.length > 0) {
                    // ATTACK!
                    if (assholes.length == 1) {
                        roboData = rc.senseRobotInfo(assholes[0]);
                        targetLoc = roboData.location;
                    }
                    // Ahhhhh!!~!~
                    else {
                        return;
                    }
                }
                // If we have greater numbers, be more aggressive
                else if (baddies.length > 0 && mypals.length > baddies.length) {
                    roboData = rc.senseRobotInfo(baddies[0]);
                    targetLoc = roboData.location;
                }
                // If we are outnumbered, stop circling around like dumbasses
                else if (mypals.length > 0 && mypals.length <= baddies.length) {
                    roboData = rc.senseRobotInfo(mypals[0]);
                    targetLoc = roboData.location;
                }
                // Check for and plant mines
                else if (baddies.length == 0 && rc.senseMine(rLoc) == null) {
                    rc.layMine();
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
                        case NORTH_WEST: xm = -3; ym = -3; break;
                    }

                    targetLoc = new MapLocation(goodHQ.x + xm, goodHQ.y + ym);
                }
                else {
                    MapLocation rallyPoints[] = {
                        new MapLocation(goodHQ.x + randomWithRange(1,3), goodHQ.y + randomWithRange(1,3)),
                        new MapLocation(goodHQ.x - randomWithRange(1,3), goodHQ.y - randomWithRange(1,3))
                    };
                    targetLoc = rallyPoints[randomWithRange(0, rallyPoints.length - 1)];
                }
            }

            // If already on targetLoc, sense buddies and send nuke research signal
            if (power > GameConstants.BROADCAST_READ_COST + GameConstants.BROADCAST_SEND_COST) {
                int currentStatus = rc.readBroadcast(researchNukeChannel);
                if (rLoc.equals(targetLoc)) {
                    Robot[] myFriends = rc.senseNearbyGameObjects(Robot.class, 33, rc.getTeam());
                    if (myFriends.length > hoardNukeResearchMin && currentStatus == 0) {
                        rc.broadcast(researchNukeChannel, round);
                    }
                    else if (myFriends.length < hoardNukeResearchMin && currentStatus == 1) {
                        rc.broadcast(researchNukeChannel, 0);
                    }

                    return;
                }
            }

            MoveRobot(rLoc, targetLoc);
        }
    }

    private static void Artillery() throws GameActionException {
        if (rc.isActive()) {
            MapLocation target = null;
            Robot[] baddies = rc.senseNearbyGameObjects(Robot.class, 33, rc.getTeam().opponent());
            if (baddies.length > 0) {
                RobotInfo baddieInfo = rc.senseRobotInfo(baddies[0]);
                target = baddieInfo.location;
            }

            if (target != null && rc.canAttackSquare(target)) {
                rc.attackSquare(target);
            }
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

        // If the bot can't move in the default direction, test all other directions
        if (!rc.canMove(dir)) {
            int shortest = 1000, testDist;
            Direction bestDir = dir;
            MapLocation testLoc;

            for (Direction testDir : Direction.values()) {
                if (testDir != dir && testDir != Direction.NONE && testDir != Direction.OMNI
                        && rc.canMove(testDir)) {
                    testLoc = rLoc.add(testDir);
                    testDist = testLoc.distanceSquaredTo(targetLoc);
                    if (testDist < shortest) {
                        shortest = testDist;
                        bestDir = testDir;
                    }
                }
            }

            // If the direction hasn't changed, there is just no where to go.
            if (bestDir == dir) {
                return;
            }

            dir = bestDir;
        }

        MapLocation nextLoc = rLoc.add(dir);
        Team mine = rc.senseMine(nextLoc);

        if (mine == Team.NEUTRAL || mine == rc.getTeam().opponent()) {
            rc.defuseMine(nextLoc);
        }
        else {
            rc.move(dir);
        }
    }

    private static void BuildEncampment(MapLocation rLoc) throws GameActionException {
        if (power > rc.senseCaptureCost()) {
            if (rc.senseEncampmentSquare(rLoc)) {
                // Check how close the encampment is to HQ, if close build an artillery
                MapLocation goodHQ = rc.senseHQLocation();
                if (goodHQ.distanceSquaredTo(rLoc) < 60) {
                    rc.captureEncampment(RobotType.ARTILLERY);
                }
                // Otherwise, check status of supplier build and do another one or generator
                else {
                    rc.captureEncampment(RobotType.SUPPLIER);
                }
            }
            else {
                MapLocation encampmentSquares[] = rc.senseAllEncampmentSquares();
                MapLocation goodEncampments[] = rc.senseAlliedEncampmentSquares();
                MapLocation targetLoc = encampmentSquares[0];
                int closest = 1000;

                checkLocations:
                for (MapLocation loc : encampmentSquares) {
                    int dist = rLoc.distanceSquaredTo(loc);
                    if (dist < closest) {
                        for (MapLocation goodLoc : goodEncampments) {
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
    }

    private static int randomWithRange(int min, int max) {
        int range = Math.abs(max - min) + 1;
        return (int)(Math.random() * range) + (min <= max ? min : max);
    }
}
