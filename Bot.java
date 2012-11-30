/**
 * Provides basic game state handling.
 */
public abstract class Bot extends AbstractSystemInputParser {
    private Ants ants;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
            int attackRadius2, int spawnRadius2, Tile[][] map) {
        setAnts(new Ants(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2,
            spawnRadius2, map));
    }
    
    /**
     * Returns game state information.
     * 
     * @return game state information
     */
    public Ants getAnts() {
        return ants;
    }
    
    /**
     * Sets game state information.
     * 
     * @param ants game state information to be set
     */
    protected void setAnts(Ants ants) {
        this.ants = ants;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeUpdate() {
        ants.setTurnStartTime(System.currentTimeMillis());
        ants.clearMyAnts();
        ants.clearEnemyAnts();
        ants.clearMyHills();
        ants.clearEnemyHills();
        ants.clearFood();
        ants.clearDeadAnts();
        ants.getOrders().clear();
        ants.clearVision();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addWater(Tile tile) {
		for (Tile n : tile.neighbors) n.removeNeighbor(tile);
					tile.neighbors = null;
        ants.update(Ilk.WATER, tile);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnt(Tile tile, int owner) {
        ants.update(owner > 0 ? Ilk.ENEMY_ANT : Ilk.MY_ANT, tile);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addFood(Tile tile) {
        ants.update(Ilk.FOOD, tile);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAnt(Tile tile, int owner) {
        ants.update(Ilk.DEAD, tile);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addHill(Tile tile, int owner) {
        ants.updateHills(owner, tile);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void afterUpdate() {
        ants.setVision();
    }
}
