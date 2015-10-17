package bortrand44;

import battlecode.common.*;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously. 
 */
public class RobotPlayer {
    public static boolean smartMove(RobotController rc, Direction dir) throws GameActionException {
        MapLocation current = rc.getLocation();
        Direction dirLeft = dir.rotateLeft();
        Direction dirRight = dir.rotateRight();
        MapLocation ahead = current.add(dir);
        MapLocation aheadLeft = current.add(dirLeft);
        MapLocation aheadRight = current.add(dirRight);
        boolean aheadMines = rc.senseMine(ahead) != null;
        boolean leftMines = rc.senseMine(aheadLeft) != null;
        boolean rightMines = rc.senseMine(aheadRight) != null;
        if (rc.canMove(dir) && !aheadMines) {
            rc.move(dir);
            return true;
        }
        if (rc.canMove(dirLeft) && !leftMines) {
            rc.move(dirLeft);
            return true;
        }

        if (rc.canMove(dirRight) && !rightMines) {
            rc.move(dirRight);
            return true;
        }

        if (rc.canMove(dir) && aheadMines) {
            rc.defuseMine(ahead);
            return true;
        }

        if (rc.canMove(dirLeft) && leftMines) {
            rc.defuseMine(aheadLeft);
            return true;
        }

        if (rc.canMove(dirRight) && rightMines) {
            rc.defuseMine(aheadRight);
            return true;
        }

        return false;
    }

	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
                        Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
                        if ((rc.senseEnemyNukeHalfDone() ||
                                    rc.senseMineLocations(rc.senseEnemyHQLocation(), 160, rc.getTeam().opponent()).length > 0)
                                && rc.checkResearchProgress(Upgrade.DEFUSION) < Upgrade.DEFUSION.numRounds) {
                            // research de-fusion if needed
                            rc.researchUpgrade(Upgrade.DEFUSION);
                        }
						else if (rc.canMove(dir) && rc.senseMine(rc.getLocation().add(dir)) == null) {
                            // Spawn a soldier
							rc.spawn(dir);
						} else {
                            int i = 0;
                            while(!(rc.canMove(dir) && rc.senseMine(rc.getLocation().add(dir)) == null) && i < 8) {
                                dir = dir.rotateLeft();
                            }
                            if (i < 8) {
                                rc.spawn(dir);
                            }
                        }

					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
                        Robot[] adjacentAllies = rc.senseNearbyGameObjects(Robot.class, 3, rc.getTeam());
                        Robot[] adjacentEnemies = rc.senseNearbyGameObjects(Robot.class, 3, rc.getTeam().opponent());
                        Robot[] enemies = rc.senseNearbyGameObjects(Robot.class, 25, rc.getTeam().opponent());
                        Robot[] allies = rc.senseNearbyGameObjects(Robot.class, 25, rc.getTeam());
                        if (enemies.length == 0) {
                            // if we don't see anyone, advance!
                            Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
                            smartMove(rc, dir);
                        } else if (adjacentEnemies.length > 0) {
                            // if we are adjancent to an enemy, sit there!
                        } else if (allies.length > enemies.length) {
                            // if we outnumber the enemies, attack!
                            RobotInfo info = rc.senseRobotInfo(enemies[0]);
                            Direction dir = rc.getLocation().directionTo(info.location);
                            smartMove(rc, dir);
                        } else if (allies.length > 0 && adjacentAllies.length == 0) {
                            // if they outnumber us and we aren't grouped up, group up!
                            RobotInfo info = rc.senseRobotInfo(allies[0]);
                            Direction dir = rc.getLocation().directionTo(info.location);
                            smartMove(rc, dir);
                        } else {
                            // run away!
                            RobotInfo info = rc.senseRobotInfo(enemies[0]);
                            Direction dir = rc.getLocation().directionTo(info.location).opposite();
                            smartMove(rc, dir);
                        }
					}
					
					if (Math.random()<0.01 && rc.getTeamPower()>5) {
						// Write the number 5 to a position on the message board corresponding to the robot's ID
						rc.broadcast(rc.getRobot().getID()%GameConstants.BROADCAST_MAX_CHANNELS, 5);
					}
				}

				// End turn
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
