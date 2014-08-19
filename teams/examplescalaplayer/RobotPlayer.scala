package examplescalaplayer

import battlecode.common._

object RobotPlayer {

	def run(myRC : RobotController) {
		while(true) {
			try {
			    // nuke bot!
				while(myRC.isActive()) {
					myRC.researchUpgrade(Upgrade.NUKE)
					myRC.`yield`()
				}
			} catch {
				case e : Exception => {
					println("caught exception:")
					e.printStackTrace()
				}
			}
		}
	}

}
