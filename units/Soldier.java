package battlecode2015.units;

import battlecode.common.*;
import battlecode2015.Robot;
import battlecode2015.utils.DirectionHelper;
import battlecode2015.utils.Broadcast;

public class Soldier extends Robot {
	protected void actions() throws GameActionException {
        if (rc.isWeaponReady()) {
        	RobotInfo[] enemies = rc.senseNearbyRobots(
				rc.getType().attackRadiusSquared,
				rc.getTeam().opponent()
			);
			if (enemies.length > 0) {
				rc.attackLocation(enemies[0].location);
			}
        }

		if (rc.isCoreReady()) {
			// told by headquarters to attack
			final int soldierClusterMin = 5;
			boolean toldToAttack = rc.readBroadcast(Broadcast.soldierMarchCh) == 1 ? true : false;
			boolean enoughFriendlyUnits = countNearbyFriendlyUnits() > soldierClusterMin ? true: false;
			MapLocation target;
			if (toldToAttack && enoughFriendlyUnits) {
				target = rc.senseEnemyHQLocation();
			}
			else {
				int loc = rc.readBroadcast(Broadcast.soldierRallyCh);
				target = new MapLocation(loc / 65536, loc % 65536);
			}
			int dirint = DirectionHelper.directionToInt(rc.getLocation().directionTo(target));
			int offsetIndex = 0;
			int[] offsets = {0,1,-1,2,-2};
			while (offsetIndex < 5 && !rc.canMove(DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8])) {
				offsetIndex++;
			}
			Direction moveDirection = null;
			if (offsetIndex < 5) {
				moveDirection = DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8];
			}
			if (moveDirection != null) {
				rc.move(moveDirection);
			}
		}	
	}
	///////////////////////////////
	// Navigation methods
	
	// move from point a to point b
	public void avoidObstacle(RobotController RC) {
		
	}
	
	//////////////////////////////
	// Detection methods
	
	// count number of friend units next to you
	public int countNearbyFriendlyUnits() {
		RobotInfo[] allies = rc.senseNearbyRobots(
				rc.getType().attackRadiusSquared,
				rc.getTeam()
			);
		return allies.length;
	}
}