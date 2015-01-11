package team158.buildings;

import battlecode.common.*;

import java.util.*;

import team158.utils.Broadcast;
import team158.utils.DirectionHelper;
import team158.utils.Hashing;

public class Headquarters extends Building {
	int[] groupID = new int[7919];
	int[] groupA = new int[200];
	int[] groupB = new int[200];

	private int attackGroup = 1;
	private int defendGroup = 0;
	
	// 0 - undecided, 1 - ground, 2 - air
	private int strategy = 1;
	
	
	protected void actions() throws GameActionException {
		if (strategy == 1) {
			groundGame();
		}
		else if (strategy == 2) {
			aerialGame();
		}
		else {
			openingGame();
		}
	}

	protected void openingGame() throws GameActionException {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		MapLocation myLocation = rc.getLocation();
		double myOre = rc.getTeamOre();

		int numBeavers = 0;
		for (RobotInfo r : myRobots) {
			if (r.type == RobotType.BEAVER) {
				numBeavers++;
			}
		}
		
		if (rc.isCoreReady()) {
			if (numBeavers == 0) {
				int offsetIndex = 0;
				int[] offsets = {0,1,-1,2,-2,3,-3,4};
				int dirint = DirectionHelper.directionToInt(myLocation.directionTo(rc.senseEnemyHQLocation()));
				while (offsetIndex < 8 && !rc.canSpawn(DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8], RobotType.BEAVER)) {
					offsetIndex++;
				}
				Direction buildDirection = null;
				if (offsetIndex < 8) {
					buildDirection = DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8];
				}
				if (buildDirection != null && myOre >= 100) {
					rc.spawn(buildDirection, RobotType.BEAVER);
				}
			}
		}
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(999999, rc.getTeam().opponent());
		
	}
	
	protected void aerialGame() throws GameActionException {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		MapLocation myLocation = rc.getLocation();
		int numBeavers = 0;
		int numMiners = 0;
		int numMinerFactories = 0;
		int numSupplyDepots = 0;
		int numHelipads = 0;
		
		int minBeaverDistance = 25; // Make sure that the closest beaver is actually close
		int closestBeaver = 0;
		
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.MINER) {
				numMiners++;
			} else if (type == RobotType.BEAVER) {
				numBeavers++;
				int distanceSquared = r.location.distanceSquaredTo(myLocation);
				if (distanceSquared < minBeaverDistance) {
					closestBeaver = r.ID;
					minBeaverDistance = r.location.distanceSquaredTo(myLocation);
				}
			} else if (type == RobotType.MINERFACTORY) {
				numMinerFactories++;
			} else if (type == RobotType.SUPPLYDEPOT) {
				numSupplyDepots++;
			} else if (type == RobotType.HELIPAD) {
				numHelipads++;
			}
		}
		
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
			double ore = rc.getTeamOre();
			if (numBeavers < 2) {
				int offsetIndex = 0;
				int[] offsets = {0,1,-1,2,-2,3,-3,4};
				int dirint = DirectionHelper.directionToInt(myLocation.directionTo(rc.senseEnemyHQLocation()));
				while (offsetIndex < 8 && !rc.canSpawn(DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8], RobotType.BEAVER)) {
					offsetIndex++;
				}
				Direction buildDirection = null;
				if (offsetIndex < 8) {
					buildDirection = DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8];
				}
				if (buildDirection != null && ore >= 100) {
					rc.spawn(buildDirection, RobotType.BEAVER);
				}
			}
			else if (numHelipads == 0) {
				if (ore >= 300) {
					rc.broadcast(Broadcast.buildHelipadsCh, closestBeaver);
				}
			} else if (numMinerFactories == 0) {
				if (ore >= 500) {
					rc.broadcast(Broadcast.buildMinerFactoriesCh, closestBeaver);
				}
			} else if (numSupplyDepots == 0 && ore >= 100) {
				rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
			}
			else if (ore >= 300 + numHelipads * 200) {
				rc.broadcast(Broadcast.buildHelipadsCh, closestBeaver);
				// tell closest beaver to build barracks
			}
			else if (numSupplyDepots < 3 && ore >= 500) {
				rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
			} 
			
//			int[] groupSize = {numSoldiersG1, numSoldiersG2};
//			int[] groupCh = {Broadcast.soldierGroup1Ch, Broadcast.soldierGroup2Ch};
//			if (numSoldiersG1 > 0 || numSoldiersG2 > 0) {
//				stopGroup(RobotType.SOLDIER);
//			}
//			rc.setIndicatorString(1, Integer.toString(groupSize[attackGroup]));
//			rc.setIndicatorString(2, Integer.toString(groupSize[defendGroup]));
//			if (numSoldiers - groupSize[defendGroup] > 30 && groupSize[attackGroup] == 0) {
//				groupUnits(groupCh[attackGroup], RobotType.SOLDIER);
//				rc.broadcast(groupCh[attackGroup], 1);
//			}
//			else if (rc.readBroadcast(groupCh[attackGroup]) == 1 && groupSize[attackGroup] < 15) {
//				rc.broadcast(groupCh[attackGroup], 0);
//				attackGroup = 1 - attackGroup;
//				defendGroup = 1 - defendGroup;
//			}
//			else if (rc.readBroadcast(groupCh[defendGroup]) == -1) {
//				unGroup(groupCh[defendGroup]);
//			}			
		}
		
	}

	protected void groundGame() throws GameActionException {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		MapLocation myLocation = rc.getLocation();
		int numSoldiers = 0;
		int numSoldiersG1 = 0;
		int numSoldiersG2 = 0;
		int numBeavers = 0;
		int numBarracks = 0;
		int numMiners = 0;
		int numMinerFactories = 0;
		int numSupplyDepots = 0;
		
		int closestBeaver = 0;
		
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.SOLDIER) {
				numSoldiers++;
				if (Hashing.find(groupID, r.ID) == Broadcast.soldierGroup1Ch) {
					numSoldiersG1++;
				}							
				else if (Hashing.find(groupID, r.ID)  == Broadcast.soldierGroup2Ch) {
					numSoldiersG2++;
				}			

			} else if (type == RobotType.MINER) {
				numMiners++;
			} else if (type == RobotType.BEAVER) {
				numBeavers++;
				closestBeaver = r.ID;
			} else if (type == RobotType.BARRACKS) {
				numBarracks++;
			} else if (type == RobotType.MINERFACTORY) {
				numMinerFactories++;
			} else if (type == RobotType.SUPPLYDEPOT) {
				numSupplyDepots++;
			}
		}
		
		rc.broadcast(Broadcast.numBeaversCh, numBeavers);
		rc.broadcast(Broadcast.numSoldiersCh, numSoldiers);
		rc.broadcast(Broadcast.numMinersCh, numMiners);
		rc.broadcast(Broadcast.numBarracksCh, numBarracks);
		rc.broadcast(Broadcast.numMinerFactoriesCh, numMinerFactories);
		rc.broadcast(Broadcast.numSupplyDepotsCh, numSupplyDepots);
		
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
			double ore = rc.getTeamOre();
			// Spawn beavers
			if (numBeavers == 0) {
				int offsetIndex = 0;
				int[] offsets = {0,1,-1,2,-2,3,-3,4};
				int dirint = DirectionHelper.directionToInt(myLocation.directionTo(rc.senseEnemyHQLocation()));
				while (offsetIndex < 8 && !rc.canSpawn(DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8], RobotType.BEAVER)) {
					offsetIndex++;
				}
				Direction buildDirection = null;
				if (offsetIndex < 8) {
					buildDirection = DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8];
				}
				if (buildDirection != null && ore >= 100) {
					rc.spawn(buildDirection, RobotType.BEAVER);
				}
			}
			// Broadcast to build structures
			else if (numMinerFactories == 0) {
				if (ore >= 500) {
					rc.broadcast(Broadcast.buildMinerFactoriesCh, closestBeaver);
				}
			}
			else if (numSupplyDepots == 0 && ore >= 100) {
				rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
			}
			else if (ore >= 300 + numBarracks * 200) {
				rc.broadcast(Broadcast.buildBarracksCh, closestBeaver);
				// tell closest beaver to build barracks
			}
			else if (numSupplyDepots < 3 && ore >= 500) {
				rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
			}

			int[] groupSize = {numSoldiersG1, numSoldiersG2};
			int[] groupCh = {Broadcast.soldierGroup1Ch, Broadcast.soldierGroup2Ch};
			if (numSoldiersG1 > 0 || numSoldiersG2 > 0) {
				stopGroup(RobotType.SOLDIER);
			}
			rc.setIndicatorString(1, Integer.toString(groupSize[attackGroup]));
			rc.setIndicatorString(2, Integer.toString(groupSize[defendGroup]));
			if (numSoldiers - groupSize[defendGroup] > 30 && groupSize[attackGroup] == 0) {
				groupUnits(groupCh[attackGroup], RobotType.SOLDIER);
				rc.broadcast(groupCh[attackGroup], 1);
			}
			else if (rc.readBroadcast(groupCh[attackGroup]) == 1 && groupSize[attackGroup] < 15) {
				rc.broadcast(groupCh[attackGroup], 0);
				attackGroup = 1 - attackGroup;
				defendGroup = 1 - defendGroup;
			}
			else if (rc.readBroadcast(groupCh[defendGroup]) == -1) {
				unGroup(groupCh[defendGroup]);
			}
		}
	}
	
	public void groupUnits(int ID_Broadcast, RobotType rt) {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		int i = 0;
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.SOLDIER) {
				//update hashmap with (id, group id) pair;
				// if soldier is in the hashmap but not in a group
				if (Hashing.find(groupID, ID_Broadcast) == 0) {
					Hashing.put(groupID, r.ID, ID_Broadcast);
					//update the corresponding broadcasted group
					if (ID_Broadcast == Broadcast.soldierGroup1Ch) {
						groupA[i] = r.ID;
						i++;
					}
					else if (ID_Broadcast == Broadcast.soldierGroup2Ch) {
						groupB[i] = r.ID;
						i++;
					}
				}
			}
		}
		int broadcastCh;
		if (rt == RobotType.SOLDIER) {
			broadcastCh = Broadcast.groupingSoldiersCh;
		}
		else if (rt == RobotType.DRONE) {
			broadcastCh = Broadcast.groupingDronesCh;
		}
		else {
			broadcastCh = 9999;
		}
		try {
			rc.broadcast(broadcastCh, ID_Broadcast);
		}
		catch (GameActionException e) {
			return;
		}
	}
	
	public void stopGroup(RobotType rt) {
		int broadcastCh;
		if (rt == RobotType.SOLDIER) {
			broadcastCh = Broadcast.groupingSoldiersCh;
		}
		else if (rt == RobotType.DRONE) {
			broadcastCh = Broadcast.groupingDronesCh;
		}
		else {
			broadcastCh = 9999;
		}
		try {
			rc.broadcast(broadcastCh, 0);
		}
		catch (GameActionException e) {
			return;
		}
	}
	
	public void unGroup(int ID_Broadcast) {
		try {
			rc.broadcast(ID_Broadcast, -1);

			if (ID_Broadcast == Broadcast.soldierGroup1Ch) {
				int i = 0;
				while (groupA[i] != 0) {
					Hashing.put(groupID, groupA[i], 0);
					groupA[i] = 0;
					i++;
				}
			}
			else if (ID_Broadcast == Broadcast.soldierGroup2Ch) {
				int i = 0;
				while (groupB[i] != 0) {
					Hashing.put(groupID, groupB[i], 0);
					groupB[i] = 0;
					i++;
				}
			}
		}
		catch (GameActionException e) {
			return;
		}
	}
}