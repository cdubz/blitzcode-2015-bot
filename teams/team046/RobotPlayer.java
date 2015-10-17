package team046;

import battlecode.common.*;

public class RobotPlayer {

    private static RobotController rc;
    private static int round;
    private static double power;

    // LOL
    private static int buildChannelX = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int buildChannelY = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int buildChannelZ = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);
    private static int zergRushChannel = randomWithRange(0, GameConstants.BROADCAST_MAX_CHANNELS);

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
				// End turn
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
            MapLocation hqLoc = rc.getLocation();
            MapLocation targetLoc;
            targetLoc = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);

            // Check for a build objective
            int bCZ = rc.readBroadcast(buildChannelZ);

            if (round == 0) {
                targetLoc = setBuildTarget(hqLoc);
            }
            else if (power < 150) {
                rc.researchUpgrade(Upgrade.NUKE);
                return;
            }
            else {
                if (bCZ == GameConstants.ROUND_MAX_LIMIT || rc.senseEnemyNukeHalfDone()) {
                    targetLoc = rc.senseEnemyHQLocation();
                    rc.broadcast(zergRushChannel, 1);
                }
                else if (round - bCZ >= GameConstants.CAPTURE_ROUND_DELAY) {
                    rc.broadcast(buildChannelZ, 0);
                    targetLoc = setBuildTarget(hqLoc);
                }
                else if (round > 50 && !rc.hasUpgrade(Upgrade.DEFUSION)) {
                    rc.researchUpgrade(Upgrade.DEFUSION);
                    return;
                }
                else if (round > 100 && !rc.hasUpgrade(Upgrade.FUSION)) {
                    rc.researchUpgrade(Upgrade.FUSION);
                    return;
                }
            }

            // Find an available spawn direction
            Direction dir = hqLoc.directionTo(targetLoc);
            while (!rc.canMove(dir)) {
                dir = dir.rotateRight();
            }
            rc.spawn(dir);
        }
    }

    private static void Soldier() throws GameActionException {
        if (rc.isActive()) {
            MapLocation rLoc = rc.getLocation();

            // Set default to halfway between bases
            MapLocation goodHQ = rc.senseHQLocation();
            MapLocation badHQ = rc.senseEnemyHQLocation();
            MapLocation targetLoc = new MapLocation((badHQ.x + goodHQ.x)/2, (badHQ.y + goodHQ.y)/2);

            // Check for build objective
            if (power > GameConstants.BROADCAST_READ_COST * 2) {
                int bCZ = rc.readBroadcast(buildChannelZ);
                if (bCZ == 0 & rc.senseCaptureCost() < power) {
                    targetLoc = new MapLocation(rc.readBroadcast(buildChannelX), rc.readBroadcast(buildChannelY));
                    if (rLoc.equals(targetLoc)) {
                        rc.captureEncampment(RobotType.SUPPLIER);
                        rc.broadcast(buildChannelZ, round);
                        return;
                    }

                }
                else if (rc.readBroadcast(zergRushChannel) == 1) {
                    targetLoc = rc.senseEnemyHQLocation();
                }
                else {
                    // Get scared
                    Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 3, rc.getTeam().opponent());
                    if (nearbyEnemies.length > 0) {
                        return;
                    }
                }
            }

            // Find an available movement direction
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
    }

    private static MapLocation setBuildTarget(MapLocation hqLoc) throws GameActionException {
        MapLocation targetLocs[] = rc.senseEncampmentSquares(hqLoc, 500, Team.NEUTRAL);
        MapLocation targetLoc;

        if (targetLocs.length > 0) {
            int shortest = 1000;
            targetLoc = targetLocs[0];
            for (MapLocation l: targetLocs) {
                int distTo = hqLoc.distanceSquaredTo(l);
                if (distTo < shortest) {
                    targetLoc = l;
                    shortest = distTo;
                }
            }
            rc.broadcast(buildChannelX, targetLoc.x);
            rc.broadcast(buildChannelY, targetLoc.y);
        }
        else {
            targetLoc = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
            rc.broadcast(buildChannelX, 0);
            rc.broadcast(buildChannelY, 0);
            rc.broadcast(buildChannelZ, GameConstants.ROUND_MAX_LIMIT);
        }
        return targetLoc;
    }

    private static int randomWithRange(int min, int max) {
        int range = Math.abs(max - min) + 1;
        return (int)(Math.random() * range) + (min <= max ? min : max);
    }
}
