import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Starter bot implementation.
 */
public class MyBot extends Bot {
	// local variables
	private Map<Tile, Tile> orders = new HashMap<Tile, Tile>();
	private Logger logger;
	private LinkedList<Tile> myAnts = new LinkedList<Tile>();
	private LinkedList<Tile> enemyAnts = new LinkedList<Tile>();
	private Ants ants = getAnts();
	private boolean isTimeOut = false;
	public Tile[][] map;
	private LinkedList<Tile> supplyList = new LinkedList<Tile>();
	private LinkedList<Mission> missions = new LinkedList<Mission>();
	private boolean isMissionPhase = false;

	public static boolean DEBUG = true;

	/**
	 * Main method executed by the game engine for starting the bot.
	 * 
	 * @param args
	 *            command line arguments
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		new MyBot().readSystemInput();
	}

	// create a logger for testing purpose
	public void init() {
		logger = new Logger("EricDummyBotDebug.txt");
	}

	/**
	 * A* Path finding algorithm, this function is going to just find out what
	 * is ant next tile to move and assign ant to do move to next tile
	 */
	public boolean aStar(Tile start, Tile target, String mission) {

		LinkedList<Tile> closedList = new LinkedList<Tile>();
		LinkedList<Tile> openList = new LinkedList<Tile>();

		Tile currentTile = target;
		openList.add(currentTile);
		currentTile.g_score = 0;
		currentTile.h_score = ants.getDistance(target, start);
		currentTile.f_score = currentTile.g_score + currentTile.h_score;

		while (!openList.isEmpty()) {
			// find the smallest F score Tile in the list
			Integer min = Integer.MAX_VALUE;
			Iterator<Tile> a = openList.iterator();
			while (a.hasNext()) {
				Tile tile = a.next();
				if (tile.f_score < min) {
					min = tile.f_score;
					currentTile = tile;
				}
			}

			if (closedList.contains(start)) {
				start.moveTo = start.parent;
				doMoveLocation(start, start.parent, mission);
				return true;
			}

			openList.remove(currentTile);
			closedList.add(currentTile);

			for (Tile n : currentTile.neighbors) {
				if (ants.getIlk(n).isUnoccupied() || n.equals(start)) {
					if (closedList.contains(n)) {
						continue;
					}

					int newG = currentTile.g_score + 1;

					if (!openList.contains(n)) {
						openList.add(n);
						n.parent = currentTile;
						n.g_score = newG;
						n.h_score = ants.getDistance(n, start);
						n.f_score = newG + n.h_score;
					} else if (newG < n.g_score) {
						n.parent = currentTile;
						n.g_score = newG;
						n.h_score = ants.getDistance(n, start);
						n.f_score = newG + n.h_score;
					}
				}
			}
		}
		logger.println("***Error: A* FAIL to find path");
		return false;
	}

	// aStar2 is going to return the next tile
	public Tile aStar2(Tile start, Tile target) {

		LinkedList<Tile> closedList = new LinkedList<Tile>();
		LinkedList<Tile> openList = new LinkedList<Tile>();

		Tile currentTile = target;
		openList.add(currentTile);
		currentTile.g_score = 0;
		currentTile.h_score = ants.getDistance(target, start);
		currentTile.f_score = currentTile.g_score + currentTile.h_score;

		while (!openList.isEmpty()) {
			// find the smallest F score Tile in the list
			Integer min = 99999;
			Iterator<Tile> a = openList.iterator();
			while (a.hasNext()) {
				Tile tile = a.next();
				if (tile.f_score < min) {
					min = tile.f_score;
					currentTile = tile;
				}
			}

			if (closedList.contains(start)) {
				start.moveTo = start.parent;
				return start.parent;
			}

			openList.remove(currentTile);
			closedList.add(currentTile);

			for (Tile n : currentTile.neighbors) {
				if (ants.getIlk(n).isUnoccupied() || n == start) {
					if (closedList.contains(n)) {
						continue;
					}

					int newG = currentTile.g_score + 1;

					if (!openList.contains(n)
							|| (ants.getMyHills().contains(n))) {
						openList.add(n);
						n.parent = currentTile;
						n.g_score = newG;
						n.h_score = ants.getDistance(n, start);
						n.f_score = newG + n.h_score;
					} else if (newG < n.g_score) {
						n.parent = currentTile;
						n.g_score = newG;
						n.h_score = ants.getDistance(n, start);
						n.f_score = newG + n.h_score;
					}
				}
			}
		}
		logger.println("a star 2 doesn't find path!");
		return null;
	}

	// BFS starting at all the foods at once and assign one ant to each food
	private void foodFinding() {
		HashMap<Tile, Boolean> isEnemyNearFood = new HashMap<Tile, Boolean>();
		LinkedList<Tile> foodTiles = new LinkedList<Tile>(ants.getFoodTiles());

		LinkedList<Tile> openList = new LinkedList<Tile>();
		LinkedList<Tile> closedList = new LinkedList<Tile>();

		for (Tile food : foodTiles) {
			openList.add(food);
			food.dist = 0;
			food.source = food;
			food.reached = true;
			isEnemyNearFood.put(food, false);
			closedList.add(food);
		}

		while (!openList.isEmpty()) {
			Tile tile = openList.removeFirst();
			if (tile.dist <= 2 && ants.getIlk(tile).isEnemyAnt())
				isEnemyNearFood.put(tile.source, true);
			if (tile.dist > 2 && isEnemyNearFood.get(tile.source)) {
				Iterator<Tile> it = closedList.iterator();
				while (it.hasNext()) {
					Tile t = it.next();
					if (t.source == tile.source) {
						t.reached = false;
						it.remove();
					}
				}
				it = openList.iterator();
				while (it.hasNext())
					if (!it.next().reached)
						it.remove();
			}
			if (tile.dist >= 10) {
				break;
			}
			if (ants.getIlk(tile).isMyAnt() && !orders.containsValue(tile) && tile.dist == 1) {
				doMoveLocation(tile, tile, "food");
				Iterator<Tile> it = closedList.iterator(); // should be openList
				while (it.hasNext()) {
					Tile t = it.next();
					if (t.source == tile.source) {
						t.reached = false;
						it.remove();
					}
				}
				it = openList.iterator(); // should be closeList
				// maybe don't need close List
				while (it.hasNext())
					if (!it.next().reached)
						it.remove();
			}
			if (tile.ilk.isMyAnt() && !orders.containsValue(tile)
					&& !(tile.isBattleField)) {
				doMoveLocation(tile, tile.parent, "food");
				Iterator<Tile> it = closedList.iterator(); // should be openList
				while (it.hasNext()) {
					Tile t = it.next();
					if (t.source == tile.source) {
						t.reached = false;
						it.remove();
					}
				}
				it = openList.iterator(); // should be closeList
				// maybe don't need close List
				while (it.hasNext())
					if (!it.next().reached)
						it.remove();
			} else if (tile.dist < 10) {
				for (Tile n : tile.neighbors) {
					if (n.reached)
						continue;

					if (ants.getIlk(n).isPassable()
							&& !ants.getMyHills().contains(n)) {
						n.parent = tile;
						n.reached = true;
						n.dist = tile.dist + 1;
						n.source = tile.source;
						closedList.add(n);
						openList.add(n);
					}
				}
			}
		}
		for (Tile tile : closedList)
			tile.reached = false;
	}

	// BFS from enemy hill and assign max to 4 ants to attack
	private void hillsAttacking() {
		LinkedList<Tile> enemyHills = new LinkedList<Tile>(ants.getEnemyHills());

		for (Tile hill : enemyHills) {
			hill.backUp = true;
			LinkedList<Tile> openList = new LinkedList<Tile>();
			LinkedList<Tile> closedList = new LinkedList<Tile>();
			int count = myAnts.size() <= 10 ? 1 : 4;

			openList.add(hill);
			hill.dist = 0;
			hill.reached = true;
			closedList.add(hill);

			while (!openList.isEmpty()) {
				Tile tile = openList.removeFirst();
				if (tile.dist >= 20)
					break;
				for (Tile n : tile.neighbors) {
					if (n.reached)
						continue;
					n.reached = true;
					if (n.ilk.isMyAnt()) {
						if (!orders.containsValue(n) && !tile.ilk.isMyAnt()
								&& !(n.isBattleField || n.isBorder))
							doMoveLocation(n, tile, "attack hill");
						count--;
					}
					n.dist = tile.dist + 1;
					closedList.add(n);
					openList.add(n);
				}
				if (count <= 0)
					break;
			}
			for (Tile tile : closedList)
				tile.reached = false;
		}
	}

	private void explore() {
		initExplore();

		for (Tile antLoc : myAnts) {
			if (!orders.containsValue(antLoc) && !antLoc.hasMission) {
				exploreAnt(antLoc);
			}
		}
	}

	// send ant to explore by BFS up to 11 tiles
	private boolean exploreAnt(Tile antTile) {
		boolean enemyArround = false;
		HashMap<Tile, Integer> values;
		if (DEBUG)
			values = new LinkedHashMap<Tile, Integer>();
		else
			values = new HashMap<Tile, Integer>();
		LinkedList<Tile> openList = new LinkedList<Tile>();
		LinkedList<Tile> closedList = new LinkedList<Tile>();
		antTile.reached = true;
		antTile.dist = 0;
		closedList.add(antTile);
		for (Tile n : antTile.neighbors) {
			values.put(n, 0);
			openList.add(n);
			n.dist = 1;
			n.reached = true;
			n.prevTiles.add(n);
			closedList.add(n);
		}
		while (!openList.isEmpty()) {
			Tile tile = openList.removeFirst();
			if (ants.getIlk(tile).isEnemyAnt())
				enemyArround = true;
			if (!ants.getMyHills().isEmpty())
				if (!ants.isVisible(tile) && tile.dist > 10 && !enemyArround)
					tile.backUp = true;
			if (tile.dist > 12) {
				for (Tile prevFirst : tile.prevTiles)
					values.put(prevFirst, values.get(prevFirst)
							+ tile.exploreValue);
				continue;
			}
			for (Tile n : tile.neighbors) {
				if (n.reached) {
					if (n.dist == tile.dist + 1) {
						n.prevTiles.addAll(tile.prevTiles);
					}
					continue;
				}
				n.reached = true;
				n.parent = tile;
				n.dist = tile.dist + 1;
				n.prevTiles.addAll(tile.prevTiles);
				closedList.add(n);
				openList.add(n);
			}
		}
		int bestValue = 0;
		Tile bestDest = null;
		for (Entry<Tile, Integer> entry : values.entrySet()) {
			if (ants.getIlk(entry.getKey()).isPassable()
					&& !ants.getMyHills().contains(entry.getKey())
					&& entry.getValue() > bestValue) {
				bestValue = entry.getValue();
				bestDest = entry.getKey();
			}
		}

		if (bestValue == 0 || bestDest == null) {
			for (Tile tile : closedList) {
				tile.reached = false;
				tile.prevTiles.clear();
			}
			return false;
		}
		for (Tile tile : closedList) {
			if (tile.dist > 10 && tile.prevTiles.contains(bestDest))
				tile.exploreValue = 0;
			tile.reached = false;
			tile.prevTiles.clear();
		}
		doMoveLocation(antTile, bestDest, "explore");
		return true;
	}

	// initatize the explore value
	private void initExplore() {

		LinkedList<Tile> openList = new LinkedList<Tile>();
		LinkedList<Tile> closedList = new LinkedList<Tile>();
		for (Tile ant : ants.getMyAnts())
			openList.add(ant);
		for (Tile tile : openList) {
			tile.dist = 0;
			tile.reached = true;
			tile.source = tile;
			closedList.add(tile);
		}
		while (!openList.isEmpty()) {
			Tile tile = openList.removeFirst();
			if (tile.dist > 10)
				break;
			tile.exploreValue = 0;
			for (Tile n : tile.neighbors) {
				if (n.reached)
					continue;
				n.reached = true;
				n.parent = tile;
				n.dist = tile.dist + 1;
				n.source = tile.source;
				closedList.add(n);
				openList.add(n);
			}
		}
		for (Tile tile : closedList)
			tile.reached = false;
	}

	// default method from starter package
	private boolean doMoveDirection(Tile antLoc, Aim direction) {
		// Track all moves, prevent collisions
		Tile newLoc = ants.getTile(antLoc, direction);
		if (ants.getIlk(newLoc).isPassable() && !orders.containsKey(newLoc)) {
			ants.issueOrder(antLoc, direction);
			orders.put(newLoc, antLoc);
			return true;
		} else {
			return false;
		}
	}

	private boolean doMoveLocation(Tile antLoc, Tile destLoc, String mission) {
		// Track targets to prevent 2 ants to the same location
		if (!isMissionPhase && antLoc.hasMission) {
			antLoc.mission.isRemoved = true;
			antLoc.mission = null;
			antLoc.hasMission = false;
		}
		List<Aim> directions = ants.getDirections(antLoc, destLoc);
		for (Aim direction : directions) {
			if (doMoveDirection(antLoc, direction)) {
				logger.println("move ant (" + antLoc + ") to dest: " + destLoc
						+ " for " + mission);
				return true;
			}
		}
		return false;
	}

	// initialize the variables
	private void initTurn() {
		ants = getAnts();

		isTimeOut = false;

		groups = new LinkedList<Group>();
		myAnts = new LinkedList<Tile>(ants.getMyAnts());
		enemyAnts = new LinkedList<Tile>(ants.getEnemyAnts());
		supplyList = new LinkedList<Tile>();
		for (Tile[] row : map)
			for (Tile tile : row) {
				tile.exploreValue++;
				tile.backUp = false;
				tile.moves = new LinkedList<Tile>();
				tile.aggressiveMoves = new LinkedList<Tile>();
				tile.passiveMove = new LinkedList<Tile>();
				tile.isBattleField = false;
				tile.enemyInRange = 0;
				tile.isBorder = false;
				tile.numCloseOwnAnts = 0;
			}

		Iterator<Tile> a = myAnts.iterator();
		while (a.hasNext()) {
			Tile ant1 = a.next();

			Iterator<Tile> b = myAnts.descendingIterator();
			Tile ant2 = b.next();
			while (ant1 != ant2) {
				int d1 = Math.abs(ant1.getRow() - ant2.getRow());
				if (d1 <= 5) {
					int d2 = Math.abs(ant1.getCol() - ant2.getCol());
					if (d2 <= 5) {
						ant1.numCloseOwnAnts++;
						ant2.numCloseOwnAnts++;
					}
				}
				ant2 = b.next();
			}
		}

		for (Tile enemy : enemyAnts) {
			int currStayValue = 0;
			for (int i = 0; i < enemy.neighbors.length; i++)
				currStayValue |= (ants.getIlk(enemy.neighbors[i]).isEnemyAnt() ? 1
						: 0) << i;

			if (enemy.stayValue == currStayValue) {
				enemy.stayTurnCount++;
				if (enemy.stayTurnCount >= 3) {
					enemy.willStay = true;
					logger.println("enemy ant: " + enemy
							+ " will stay this turn");
				}
			} else {
				enemy.stayValue = currStayValue;
				enemy.stayTurnCount = 0;
				enemy.willStay = false;
			}
		}
	}

	// the whole combat method
	private void combat() {
		defineGroups();
		Collections.sort(groups);
		defineMoves();
		for (Group group : groups) {
			alphaBeta(group);
			for (Tile myAnt : group.myAntsInCombat) {
				if (!myAnt.bestTo.equals(myAnt))
					doMoveLocation(myAnt, myAnt.bestTo, "combat");
				else {
					logger.println("my ant (" + myAnt
							+ ") is not moving due to combat this turn");
					orders.put(myAnt, myAnt);
				}
			}
		}
	}

	// define a list to contains all the groups
	private List<Group> groups = new LinkedList<Group>();

	// define ants who is in combat to be included in a group and add this group
	// to list
	private void defineGroups() {
		for (Tile myAnt : myAnts) {
			boolean inGroup = false;
			for (Group group : groups) {
				if (group.myAntsInCombat.contains(myAnt))
					inGroup = true;
			}
			if (inGroup)
				continue;

			Group group = new Group();
			LinkedList<Tile> openSet = new LinkedList<Tile>();

			if (myAnt.isBorder || myAnt.isBattleField) {
				group.myAntsInCombat.add(myAnt);
				group.maxNumCloseOwnAnts = Math.max(group.maxNumCloseOwnAnts,
						myAnt.numCloseOwnAnts);
				openSet.add(myAnt);
				while (!openSet.isEmpty()) {
					Tile currAnt = openSet.remove();
					LinkedList<Tile> enemies = findEnemies(currAnt, group);
					openSet.addAll(enemies);
					if (ants.getIlk(currAnt).isMyAnt()) {
						group.enemyAntsInCombat.addAll(enemies);
					} else {
						group.myAntsInCombat.addAll(enemies);
					}
				}
			}

			if (!group.myAntsInCombat.isEmpty()
					&& !group.enemyAntsInCombat.isEmpty()) {
				// testing purpose print out all the ants in group
				logger.println("my ants in combat group: "
						+ group.myAntsInCombat);
				logger.println("enemy ants in combat gorup: "
						+ group.enemyAntsInCombat);
				logger.println("group max number of close ant: "
						+ group.maxNumCloseOwnAnts);

				group.size = group.myAntsInCombat.size()
						+ group.enemyAntsInCombat.size();

				groups.add(group);
			}
		}
	}

	// find all nearby enemies for this ant (can be enemy, or mine)
	private LinkedList<Tile> findEnemies(Tile ant, Group group) {
		LinkedList<Tile> result = new LinkedList<Tile>();
		if (ants.getIlk(ant).isMyAnt()) {
			for (Tile enemyAnt : enemyAnts) {
				if (!group.enemyAntsInCombat.contains(enemyAnt)) {
					int dx = Math.abs(ant.getRow() - enemyAnt.getRow());
					int dy = Math.abs(ant.getCol() - enemyAnt.getCol());
					if (dx >= ants.getRows() - 5)
						dx = ants.getRows() - dx;
					if (dy >= ants.getCols() - 5)
						dy = ants.getCols() - dy;
					if (dx + dy <= 5
							&& !((dx == 0 && dy == 5) || (dy == 0 && dx == 5))) {
						result.add(enemyAnt);
					}
				}
			}
			return result;
		} else {
			for (Tile myAnt : myAnts) {
				if (!group.myAntsInCombat.contains(myAnt)) {
					int dx = Math.abs(ant.getRow() - myAnt.getRow());
					int dy = Math.abs(ant.getCol() - myAnt.getCol());
					if (dx >= ants.getRows() - 5)
						dx = ants.getRows() - dx;
					if (dy >= ants.getCols() - 5)
						dy = ants.getCols() - dy;
					if (dx + dy <= 5
							&& !((dx == 0 && dy == 5) || (dy == 0 && dx == 5))) {
						group.maxNumCloseOwnAnts = Math
								.max(group.maxNumCloseOwnAnts,
										myAnt.numCloseOwnAnts);
						result.add(myAnt);
					}
				}
			}
			return result;
		}
	}

	// defined danger zone and border
	private void defineBattleField() {
		for (Tile enemyAnt : enemyAnts) {
			LinkedList<Tile> openList = new LinkedList<Tile>();
			LinkedList<Tile> closedList = new LinkedList<Tile>();

			openList.add(enemyAnt);
			enemyAnt.dist = 0;
			enemyAnt.reached = true;
			closedList.add(enemyAnt);

			while (!openList.isEmpty()) {
				Tile tile = openList.removeFirst();
				if (tile.dist >= 6) {
					break;
				}
				if (ants.getIlk(tile).isPassable()) {
					int dx = Math.abs(tile.getRow() - enemyAnt.getRow());
					int dy = Math.abs(tile.getCol() - enemyAnt.getCol());
					if (dx >= ants.getRows() - 5) {
						dx = ants.getRows() - dx;
					}

					if (dy >= ants.getCols() - 5) {
						dy = ants.getCols() - dy;
					}
					if (dx + dy <= 4
							&& !((dx == 0 && dy == 4) || (dy == 0 && dx == 4))) {
						tile.isBattleField = true;
					}
					if ((dx + dy == 5 || (dx == 4 && dy == 0) || (dy == 4 && dx == 0))
							&& !((dx == 0 && dy == 5) || (dy == 0 && dx == 5))
							&& !(tile.isBattleField)
							&& tile.neighbors.length > 1) {
						tile.isBorder = true;
						if (ants.getIlk(tile).isUnoccupied())
							tile.backUp = true;
					}
				}
				if (tile.dist < 6) {
					for (Tile n : tile.neighbors) {
						if (n.reached)
							continue;

						if (ants.getIlk(n).isPassable()) {
							n.reached = true;
							n.dist = tile.dist + 1;
							closedList.add(n);
							openList.add(n);
						}
					}
				}
			}
			for (Tile tile : closedList)
				tile.reached = false;
		}

	}

	// trying to make a better escape move by move toward my ant
	private void betterEscape() {
		for (Tile myAnt : myAnts) {
			if (!(myAnt.isBattleField || myAnt.isBorder)) {
				LinkedList<Tile> openList = new LinkedList<Tile>();
				LinkedList<Tile> closedList = new LinkedList<Tile>();

				openList.add(myAnt);
				myAnt.dist = 0;
				myAnt.reached = true;
				closedList.add(myAnt);

				while (!openList.isEmpty()) {
					Tile tile = openList.removeFirst();
					if (tile.dist >= 12) {
						break;
					}
					if (ants.getIlk(tile).isPassable() && !tile.equals(myAnt)) {
						tile.escapeForce += 49 / Math.pow(tile.dist, 2);
					}
					if (tile.dist < 12) {
						for (Tile n : tile.neighbors) {
							if (n.reached)
								continue;

							if (ants.getIlk(n).isPassable()) {
								n.reached = true;
								n.dist = tile.dist + 1;
								closedList.add(n);
								openList.add(n);
							}
						}
					}
				}
				for (Tile tile : closedList)
					tile.reached = false;
			}
		}
	}

	// combat optimization by reducing calculation on moves
	private void defineMoves() {
		betterEscape();
		for (Group group : groups) {
			for (Tile myAnt : group.myAntsInCombat) {
				if (myAnt.isBattleField)
					myAnt.aggressiveMoves.add(myAnt);
				if (myAnt.passiveMove.isEmpty()) {
					if (!myAnt.isBattleField)
						myAnt.passiveMove.add(myAnt);
				}
				double escapeForce = 0;
				Tile goodEscapeTile = myAnt;
				for (Tile target : myAnt.neighbors) {
					if (target.isBattleField) {
						myAnt.aggressiveMoves.add(target);
					}
					if (myAnt.passiveMove.isEmpty() && !target.isBattleField
							&& target.neighbors.length > 2) {
						if (target.escapeForce > escapeForce)
							goodEscapeTile = target;
					}
				}
				if (myAnt.passiveMove.isEmpty())
					myAnt.passiveMove.add(goodEscapeTile);

				myAnt.moves.addAll(myAnt.aggressiveMoves);
				myAnt.moves.addAll(myAnt.passiveMove);
			}
			for (Tile enemyAnt : group.enemyAntsInCombat) {
				if (!enemyAnt.willStay
						|| group.enemyAntsInCombat.size() >= group.myAntsInCombat
								.size()) {
					for (Tile myAnt : group.myAntsInCombat) {
						int dx = Math.abs(myAnt.getRow() - enemyAnt.getRow());
						int dy = Math.abs(myAnt.getCol() - enemyAnt.getCol());
						if (dx >= ants.getRows() - 5) {
							dx = ants.getRows() - dx;
						}

						if (dy >= ants.getCols() - 5) {
							dy = ants.getCols() - dy;
						}
						if (dx + dy <= 4
								&& !((dx == 0 && dy == 4) || (dy == 0 && dx == 4))) {
							enemyAnt.aggressiveMoves.add(enemyAnt);
						}
						if ((dx + dy == 5 || (dx == 4 && dy == 0) || (dy == 4 && dx == 0))
								&& !((dx == 0 && dy == 5) || (dy == 0 && dx == 5))) {
							enemyAnt.passiveMove.add(enemyAnt);
						}
					}

					for (Tile target : enemyAnt.neighbors) {
						for (Tile myAnt : group.myAntsInCombat) {
							int dx = Math.abs(myAnt.getRow() - target.getRow());
							int dy = Math.abs(myAnt.getCol() - target.getCol());
							if (dx >= ants.getRows() - 5) {
								dx = ants.getRows() - dx;
							}

							if (dy >= ants.getCols() - 5) {
								dy = ants.getCols() - dy;
							}
							if (dx + dy <= 4
									&& !((dx == 0 && dy == 4) || (dy == 0 && dx == 4))) {
								enemyAnt.aggressiveMoves.add(target);
							}
							if ((dx + dy == 5 || (dx == 4 && dy == 0) || (dy == 4 && dx == 0))
									&& !((dx == 0 && dy == 5) || (dy == 0 && dx == 5))
									&& enemyAnt.passiveMove.isEmpty()) {
								enemyAnt.passiveMove.add(target);
							}
						}
					}

					enemyAnt.moves.addAll(enemyAnt.aggressiveMoves);
					enemyAnt.moves.addAll(enemyAnt.passiveMove);
				} else {
					enemyAnt.moves.add(enemyAnt);
				}
			}
		}
	}

	// combat calculation from alphabeta to evaluate
	private void alphaBeta(Group group) {
		bestPrecValue = Integer.MIN_VALUE;
		max(0, group);
	}

	int bestPrecValue = Integer.MIN_VALUE;

	private void max(int antIndex, Group group) {
		if (ants.getTimeRemaining() < 160)
			isTimeOut = true;
		if (isTimeOut) {
			for (Tile myAnt : group.myAntsInCombat) {
				myAnt.target = myAnt;
			}
			logger.println("left time: " + ants.getTimeRemaining());
			logger.println("!time out! preventing from combat calculation!");
			return;
		}
		if (antIndex < group.myAntsInCombat.size()) {
			Tile myAnt = group.myAntsInCombat.get(antIndex);
			Tile from = myAnt;

			for (Tile n : myAnt.moves) {
				if (n.hasVirtAnt)
					continue;
				n.hasVirtAnt = true;
				from = myAnt.target;
				myAnt.target = n;
				max(antIndex + 1, group);
				if (from != myAnt.target)
					myAnt.target = from;
				n.hasVirtAnt = false;
			}
		} else {
			doCut = false;
			int value = min(0, group);
			if (value > bestPrecValue) {
				bestPrecValue = value;
				// save all the moves for myAnts maybe just doMove of these
				// moves too

				// testing purpose
				logger.println("calculated value: " + bestPrecValue);
				for (Tile ant : group.myAntsInCombat) {
					ant.bestTo = ant.target;
					logger.println("ant " + ant + " move to " + ant.bestTo);
				}
			}
		}
	}

	private boolean doCut = false;

	private int min(int antIndex, Group group) {
		if (ants.getTimeRemaining() < 160)
			isTimeOut = true;
		if (isTimeOut)
			return bestPrecValue;
		if (antIndex < group.enemyAntsInCombat.size()) {
			Tile enemyAnt = group.enemyAntsInCombat.get(antIndex);
			Tile from = enemyAnt;
			int bestValue = Integer.MAX_VALUE;

			for (Tile n : enemyAnt.moves) {
				if (n.hasVirtAnt)
					continue;
				from = enemyAnt.target;
				n.hasVirtAnt = true;
				enemyAnt.target = n;
				int value = min(antIndex + 1, group);
				n.hasVirtAnt = false;
				if (enemyAnt.target != from)
					enemyAnt.target = from;
				if (doCut) {
					if (enemyAnt.target != from)
						enemyAnt.target = from;
					return bestPrecValue;
				}
				if (value < bestValue) {
					bestValue = value;
				}
			}
			return bestValue;
		} else {
			return evaluate(group);
		}
	}

	private int evaluate(Group group) {
		if (ants.getTimeRemaining() < 160)
			isTimeOut = true;
		if (isTimeOut)
			return 0;
		int myAntDead = 0;
		int enemyAntDead = 0;

		// calculating the number of enemy for my Ant
		for (Tile myAnt : group.myAntsInCombat) {
			for (Tile enemyAnt : group.enemyAntsInCombat) {
				int dx = Math.abs(myAnt.target.getRow()
						- enemyAnt.target.getRow());
				int dy = Math.abs(myAnt.target.getCol()
						- enemyAnt.target.getCol());
				if (dx >= 15)
					dx = ants.getRows() - dx;
				if (dy >= 15)
					dy = ants.getCols() - dy;
				if (dx + dy <= 3
						&& !((dx == 0 && dy == 3) || (dy == 0 && dx == 3))) {
					myAnt.target.enemyInRange++;
				}
			}
		}

		// calculating the number of enemy for enemy ant
		for (Tile enemyAnt : group.enemyAntsInCombat) {
			for (Tile myAnt : group.myAntsInCombat) {
				int dx = Math.abs(myAnt.target.getRow()
						- enemyAnt.target.getRow());
				int dy = Math.abs(myAnt.target.getCol()
						- enemyAnt.target.getCol());
				if (dx >= 15)
					dx = ants.getRows() - dx;
				if (dy >= 15)
					dy = ants.getCols() - dy;
				if (dx + dy <= 3
						&& !((dx == 0 && dy == 3) || (dy == 0 && dx == 3))) {
					enemyAnt.target.enemyInRange++;
				}
			}
		}

		// evaluation by implementing the pseudocode code from aichallenge
		for (Tile myAnt : group.myAntsInCombat) {
			for (Tile enemyAnt : group.enemyAntsInCombat) {
				if (myAnt.target.enemyInRange != 0
						&& enemyAnt.target.enemyInRange != 0) {
					if (myAnt.target.enemyInRange >= enemyAnt.target.enemyInRange) {
						myAntDead++;
						break;
					}
				}
			}
		}
		for (Tile enemyAnt : group.enemyAntsInCombat) {
			for (Tile myAnt : group.myAntsInCombat) {
				if (enemyAnt.target.enemyInRange != 0
						&& myAnt.target.enemyInRange != 0) {
					if (enemyAnt.target.enemyInRange >= myAnt.target.enemyInRange) {
						enemyAntDead++;
						break;
					}
				}
			}
		}

		for (Tile myAnt : group.myAntsInCombat) {
			myAnt.target.enemyInRange = 0;
		}
		for (Tile enemyAnt : group.enemyAntsInCombat) {
			enemyAnt.target.enemyInRange = 0;
		}
		// aggression check
		group.isAggressive = group.maxNumCloseOwnAnts >= 10
				|| group.myAntsInCombat.size() > group.enemyAntsInCombat.size() + 3;

		if (group.isAggressive)
			return enemyAntDead * 300 - myAntDead * 200;
		else
			return enemyAntDead * 200 - myAntDead * 350;
	}

	// just a method to prevent my ant killing each other
	private void preventTeamKill() {
		for (Tile myAnt : myAnts) {
			if (orders.containsKey(myAnt) && !orders.containsValue(myAnt)) {
				if (!orders.containsKey(orders.get(myAnt)))
					doMoveLocation(myAnt, orders.get(myAnt),
							"prevent team kill by swap");
				else {
					for (Tile n : myAnt.neighbors) {
						if (!orders.containsKey(n)
								&& ants.getIlk(n).isUnoccupied()) {
							doMoveLocation(myAnt, n,
									"prevent team kill by move to other tile");
							break;
						}
					}
				}
			}
		}
	}

	// this method is going to add all the place that is calling backup to the
	// list
	private void supplyList() {
		for (Tile[] row : map)
			for (Tile tile : row) {
				if (tile.backUp)
					supplyList.add(tile);
			}
	}

	// creating mission for ant to move to the specific target between turns
	class Mission {
		Tile target;
		Tile antLoc;
		int lastUpdated;
		boolean isRemoved;
	}

	// remove the mission that should be removed
	private void initMissions() {
		ListIterator<Mission> it = missions.listIterator();
		while (it.hasNext()) {
			Mission m = it.next();
			Tile ant = m.antLoc;
			if (m.isRemoved) {
				it.remove();
				continue;
			}
			if (ants.getIlk(ant).isUnoccupied()) {
				it.remove();
				logger.println("remove mission because no ant at " + m.antLoc);
				continue;
			}
			ant.hasMission = true;
			ant.mission = m;
		}
	}

	// for each my ant, keep up with their assigned mission
	private void doMissions() {
		isTimeOut = false;
		isMissionPhase = true;
		for (Mission m : missions)
			doMission(m);
		isMissionPhase = false;
	}

	// do mission on single ant
	private void doMission(Mission m) {
		if (m.isRemoved)
			return;
		Tile ant = m.antLoc;
		if (ants.getTimeRemaining() < 60)
			isTimeOut = true;
		if (isTimeOut || orders.containsValue(ant)) {
			return;
		}
		if (turn - m.lastUpdated >= 10 || ants.getTimeRemaining() > 200)
			updateMission(m);
		Tile next = aStar2(ant, m.target);
		if (next == null) {
			m.isRemoved = true;
			ant.hasMission = false;
			logger.println("remove mission because cannot savely reach "
					+ m.target + " from " + m.antLoc);
			return;
		}
		doMoveLocation(ant, next, "mission to " + m.target);
		if (next == m.target)
			m.isRemoved = true;
		else {
			m.antLoc.hasMission = false;
			m.antLoc.mission = null;
			m.antLoc = next;
			m.antLoc.mission = m;
			m.antLoc.hasMission = true;
		}

	}

	// update the mission target to the correct tile who need back up
	private void updateMission(Mission m) {
		logger.println("update mission from " + m.antLoc + " to " + m.target);
		Tile searchStartTile = !ants.getIlk(m.target).isPassable() ? m.antLoc
				: m.target;
		Tile borderTile = findBackUp(searchStartTile);
		m.lastUpdated = turn;
		if (borderTile != null) {
			m.target = borderTile;
		} else
			logger.println("borderTile = null !");
	}

	// create mission for those ant are surrounded by my own ants
	private void createMissions() {
		if (isTimeOut)
			return;
		for (Tile ant : myAnts) {
			if (ant.hasMission || orders.containsValue(ant))
				continue;
			Tile target;
			if (ants.getMyHills().contains(ant) && !supplyList.isEmpty())
				target = supplyList
						.get(new Random().nextInt(supplyList.size()));
			else
				target = findBackUp(ant);
			if (target == null)
				continue;
			Tile dest = aStar2(ant, target);
			if (dest == null)
				continue;
			doMoveLocation(ant, dest, "new created mission");

			Mission m = new Mission();
			m.antLoc = dest;
			m.antLoc.mission = m;
			m.antLoc.hasMission = true;
			m.target = target;
			m.lastUpdated = turn;
			missions.add(m);
			logger.println("create mission from " + m.antLoc + " to "
					+ m.target);

		}
	}

	// BFS to find closest tile which is calling for back up
	private Tile findBackUp(Tile startTile) {
		if (ants.getTimeRemaining() < 60)
			return startTile;
		if (startTile.backUp)
			return startTile;
		final int maxDist = 400;
		Tile backUp = null;
		LinkedList<Tile> openList = new LinkedList<Tile>();
		LinkedList<Tile> changedTiles = new LinkedList<Tile>();
		openList.add(startTile);
		startTile.dist = 0;
		startTile.reached = true;
		changedTiles.add(startTile);
		while (!openList.isEmpty()) {
			Tile tile = openList.removeFirst();
			if (tile.dist >= maxDist)
				break;
			for (Tile n : tile.neighbors) {
				if (n.backUp) {
					backUp = n;
					break;
				}
				if (n.reached)
					continue;
				n.reached = true;
				n.dist = tile.dist + 1;
				changedTiles.add(n);
				openList.add(n);
			}
			if (backUp != null)
				break;
		}
		for (Tile tile : changedTiles)
			tile.reached = false;
		return backUp;
	}

	// defence hil lmethod
	public void defence() {

	}

	// what ant is going to do for each turn
	@Override
	public void doTurn() {
		Ants ants = getAnts();
		map = ants.mapTiles;
		orders.clear();

		initTurn();

		initMissions();

		logger.println("============== turn " + turn);

		defineBattleField();

		hillsAttacking();

		foodFinding();

		combat();

		isTimeOut = false;

		explore();

		supplyList();
		doMissions();
		createMissions();

		preventTeamKill();

		logger.println("my ants at this case: " + myAnts);
		for (Tile freeAnt : myAnts) {
			if (!orders.containsValue(freeAnt))
				logger.println("there is free ant this turn: " + freeAnt);
		}
		logger.println("rest of Time from this turn=" + ants.getTimeRemaining()
				+ "ms");
	}
}
