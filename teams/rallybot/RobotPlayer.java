package rallybot;

import battlecode.common.*;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously. 
 */
public class RobotPlayer {
	static boolean movingToRally = true;
	static MapLocation rallyLocation;
	static MapLocation enemyHqLocation;
	static MapLocation myHqLocation;
	static Team ally;
	static Team opponent;
	static RobotController rc;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	
	public static void run(RobotController _rc) {
		rc = _rc;
		enemyHqLocation = rc.senseEnemyHQLocation();
		myHqLocation = rc.senseHQLocation();
		rallyLocation = new MapLocation((enemyHqLocation.x + myHqLocation.x) / 2, (enemyHqLocation.y + myHqLocation.y) / 2);
		ally = rc.getTeam();
		opponent = ally.opponent();
		
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.canMove(dir))
							rc.spawn(dir);
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
						if (rc.senseEncampmentSquare(rc.getLocation())) {
							rc.captureEncampment(RobotType.GENERATOR);
						} else {
							if (movingToRally) {
								if(rc.senseNearbyGameObjects(Robot.class, 999999, ally).length > 10) {
									movingToRally = false;
									moveOrDefuse(enemyHqLocation);
								} else {
									moveOrDefuse(rallyLocation);
								}
							} else {
								moveOrDefuse(enemyHqLocation);
							}
						}
					}
				}

				// End turn
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void moveOrDefuse(MapLocation goal) throws GameActionException{
		MapLocation myLocation = rc.getLocation();
		Direction toGoal = myLocation.directionTo(goal);
		Direction toMove = tryMove(toGoal);
		if (toMove != null) {
			Team mineInDirTeam = rc.senseMine(myLocation.add(toMove));
			if (mineInDirTeam == null || mineInDirTeam == ally) {
				rc.move(toMove);
			} else {
				rc.defuseMine(myLocation.add(toMove));
			}
		}
	}
	
	public static Direction tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = d.ordinal();

		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8]) ) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			return directions[(dirint+offsets[offsetIndex]+8)%8];
		}
		
		return null;
	}
	
}
