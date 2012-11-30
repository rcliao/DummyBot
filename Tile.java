import java.util.*;

/**
 * Represents a tile of the game map.
 */
public class Tile implements Comparable<Tile> {
    private final int row;
    private final int col;
	
	// explore
	public int exploreValue = 10;
	
	// BFS
	public boolean reached = false;
	public int dist;
	public Tile parent;
	public Tile source; 
	public HashSet<Tile> prevTiles = new HashSet<Tile>();
	public Tile[] neighbors = new Tile[4];
	
	// A*
	public int f_score;
	public int g_score;
	public int h_score;
	
	// type
	public Ilk ilk = Ilk.LAND;
	
	// combat and battlefield stuff
	public LinkedList<Tile> aggressiveMoves = new LinkedList<Tile>();
	public LinkedList<Tile> passiveMove = new LinkedList<Tile>();
	public LinkedList<Tile> moves = new LinkedList<Tile>();
	public boolean isBattleField = false;
	public boolean isBorder = false;
	public boolean backUp = false;
	public Tile target = this;
	public int enemyInRange = 0;
	public Tile bestTo = this;
	public boolean hasVirtAnt = false;
	public int numCloseOwnAnts = 0;
	public boolean willStay = false;
	public int stayTurnCount = 0;
	public int stayValue = -1;
	public double escapeForce = 0;
	
	// mission
	public MyBot.Mission mission;
	public boolean hasMission = false;
	public Tile moveTo;

    /**
     * Creates new {@link Tile} object.
     * 
     * @param row row index
     * @param col column index
     */
    public Tile(int row, int col) {
        this.row = row;
        this.col = col;
    }
	
	public void removeNeighbor(Tile n) {
		Tile[] newNeighbors = new Tile[neighbors.length-1];
		int i = 0;
		for (Tile m : neighbors) if (m != n) newNeighbors[i++] = m;
		neighbors = newNeighbors;
	}
    
    /**
     * Returns row index.
     * 
     * @return row index
     */
    public int getRow() {
        return row;
    }
    
    /**
     * Returns column index.
     * 
     * @return column index
     */
    public int getCol() {
        return col;
    }
    
    /** 
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Tile o) {
        return hashCode() - o.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return row * Ants.MAX_MAP_SIZE + col;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof Tile) {
            Tile tile = (Tile)o;
            result = row == tile.row && col == tile.col;
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return row + " " + col;
    }
}
