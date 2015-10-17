package nukebot;

import battlecode.common.*;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously. 
 */
public class RobotPlayer {
	public static void run(RobotController myRC) {
		while (true) {
			try {
				while(myRC.isActive()) {
					myRC.researchUpgrade(Upgrade.NUKE);
					myRC.yield();
				}
				// End turn
				myRC.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
