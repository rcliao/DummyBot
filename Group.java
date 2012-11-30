import java.util.LinkedList;

// defining combat group as group object
	public class Group implements Comparable<Group> {
		LinkedList<Tile> myAntsInCombat = new LinkedList<Tile>();
		LinkedList<Tile> enemyAntsInCombat = new LinkedList<Tile>();
		int size = 0;
		int maxNumCloseOwnAnts = 0;
		boolean isAggressive = false;
		@Override
		public int compareTo(Group o) {
			// TODO Auto-generated method stub
			if (this.size == o.size)
				return 0;
			else if (this.size > o.size)
				return 1;
			else
				return -1;
		}
	}