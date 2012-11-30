import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Handles system input stream parsing.
 */
public abstract class AbstractSystemInputParser extends AbstractSystemInputReader {
    private static final String READY = "ready";
    
    private static final String GO = "go";
    
    private static final char COMMENT_CHAR = '#';
    
    private final List<String> input = new ArrayList<String>();
	
	public int turn = 0;
	public long startTime;
	
	public Tile[][] map;
    
    private enum SetupToken {
        LOADTIME, TURNTIME, ROWS, COLS, TURNS, VIEWRADIUS2, ATTACKRADIUS2, SPAWNRADIUS2;
        
        private static final Pattern PATTERN = compilePattern(SetupToken.class);
    }
    
    private enum UpdateToken {
        W, A, F, D, H;
        
        private static final Pattern PATTERN = compilePattern(UpdateToken.class);
    }
    
    private static Pattern compilePattern(Class<? extends Enum> clazz) {
        StringBuilder builder = new StringBuilder("(");
        for (Enum enumConstant : clazz.getEnumConstants()) {
            if (enumConstant.ordinal() > 0) {
                builder.append("|");
            }
            builder.append(enumConstant.name());
        }
        builder.append(")");
        return Pattern.compile(builder.toString());
    }
    
    /**
     * Collects lines read from system input stream until a keyword appears and then parses them.
     */
    @Override
    public void processLine(String line) {
        if (line.equals(READY)) {
            parseSetup(input);
			init();
            doTurn();
            finishTurn();
            input.clear();
        } else if (line.equals(GO)) {
            parseUpdate(input);
            doTurn();
            finishTurn();
            input.clear();
        } else if (!line.isEmpty()) {
            input.add(line);
        }
    }
    
    /**
     * Parses the setup information from system input stream.
     * 
     * @param input setup information
     */
    public void parseSetup(List<String> input) {
        int loadTime = 0;
        int turnTime = 0;
        int rows = 0;
        int cols = 0;
        int turns = 0;
        int viewRadius2 = 0;
        int attackRadius2 = 0;
        int spawnRadius2 = 0;
        for (String line : input) {
            line = removeComment(line);
            if (line.isEmpty()) {
                continue;
            }
            Scanner scanner = new Scanner(line);
            if (!scanner.hasNext()) {
                continue;
            }
            String token = scanner.next().toUpperCase();
            if (!SetupToken.PATTERN.matcher(token).matches()) {
                continue;
            }
            SetupToken setupToken = SetupToken.valueOf(token);
            switch (setupToken) {
                case LOADTIME:
                    loadTime = scanner.nextInt();
                break;
                case TURNTIME:
                    turnTime = scanner.nextInt();
                break;
                case ROWS:
                    rows = scanner.nextInt();
                break;
                case COLS:
                    cols = scanner.nextInt();
                break;
                case TURNS:
                    turns = scanner.nextInt();
                break;
                case VIEWRADIUS2:
                    viewRadius2 = scanner.nextInt();
                break;
                case ATTACKRADIUS2:
                    attackRadius2 = scanner.nextInt();
                break;
                case SPAWNRADIUS2:
                    spawnRadius2 = scanner.nextInt();
                break;
            }
        }
		map = new Tile[rows][cols];
		for (int row = 0; row < rows; row++) {
			map[row] = new Tile[cols];
			for (int col = 0; col < cols; col++) {
				map[row][col] = new Tile(row, col);
			}
		}
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				Tile tile = map[row][col];
				Tile n = map[(row+rows-1)%rows][col];
				Tile e = map[row][(col+1)%cols];
				Tile s = map[(row+1)%rows][col];
				Tile w = map[row][(col+cols-1)%cols];
				if (((col + row) & 1) == 1) {
					tile.neighbors[0] = n;
					tile.neighbors[1] = s;
					tile.neighbors[2] = e;
					tile.neighbors[3] = w;
				} else {
					tile.neighbors[0] = e;
					tile.neighbors[1] = w;
					tile.neighbors[2] = n;
					tile.neighbors[3] = s;
				}
			}
		}
        setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2, map);
    }
    
    /**
     * Parses the update information from system input stream.
     * 
     * @param input update information
     */
    public void parseUpdate(List<String> input) {
        beforeUpdate();
        for (String line : input) {
            line = removeComment(line);
            if (line.isEmpty()) {
                continue;
            }
            Scanner scanner = new Scanner(line);
            if (!scanner.hasNext()) {
                continue;
            }
            String token = scanner.next().toUpperCase();
            if (!UpdateToken.PATTERN.matcher(token).matches()) {
                continue;
            }
            UpdateToken updateToken = UpdateToken.valueOf(token);
            int row = scanner.nextInt();
            int col = scanner.nextInt();
			Tile tile = map[row][col];
            switch (updateToken) {
                case W:
                    addWater(tile);
                break;
                case A:
                    if (scanner.hasNextInt()) {
                        addAnt(tile, scanner.nextInt());
                    }
                break;
                case F:
                    addFood(tile);
                break;
                case D:
                    if (scanner.hasNextInt()) {
                        removeAnt(tile, scanner.nextInt());
                    }
                break;
                case H:
                    if (scanner.hasNextInt()) {
                        addHill(tile, scanner.nextInt());
                    }
                break;
            }
        }
        afterUpdate();
    }
    
    /**
     * Sets up the game state.
     * 
     * @param loadTime timeout for initializing and setting up the bot on turn 0
     * @param turnTime timeout for a single game turn, starting with turn 1
     * @param rows game map height
     * @param cols game map width
     * @param turns maximum number of turns the game will be played
     * @param viewRadius2 squared view radius of each ant
     * @param attackRadius2 squared attack radius of each ant
     * @param spawnRadius2 squared spawn radius of each ant
     */
    public abstract void setup(int loadTime, int turnTime, int rows, int cols, int turns,
            int viewRadius2, int attackRadius2, int spawnRadius2, Tile[][] map);
    
    /**
     * Enables performing actions which should take place prior to updating the game state, like
     * clearing old game data.
     */
    public abstract void beforeUpdate();
    
    /**
     * Adds new water tile.
     * 
     * @param row row index
     * @param col column index
     */
    public abstract void addWater(Tile tile);
    
    /**
     * Adds new ant tile.
     * 
     * @param row row index
     * @param col column index
     * @param owner player id
     */
    public abstract void addAnt(Tile tile, int owner);
    
    /**
     * Adds new food tile.
     * 
     * @param row row index
     * @param col column index
     */
    public abstract void addFood(Tile tile);
    
    /**
     * Removes dead ant tile.
     * 
     * @param row row index
     * @param col column index
     * @param owner player id
     */
    public abstract void removeAnt(Tile tile, int owner);
    
    /**
     * Adds new hill tile.
     *
     * @param row row index
     * @param col column index
     * @param owner player id
     */
    public abstract void addHill(Tile tile, int owner);
    
    /**
     * Enables performing actions which should take place just after the game state has been
     * updated.
     */
    public abstract void afterUpdate();
    
    /**
     * Subclasses are supposed to use this method to process the game state and send orders.
     */
    public abstract void doTurn();
	
	public abstract void init();
    
    /**
     * Finishes turn.
     */
    public void finishTurn() {
        System.out.println("go");
        System.out.flush();
		this.turn ++;
    }
    
    private String removeComment(String line) {
        int commentCharIndex = line.indexOf(COMMENT_CHAR);
        String lineWithoutComment;
        if (commentCharIndex >= 0) {
            lineWithoutComment = line.substring(0, commentCharIndex).trim();
        } else {
            lineWithoutComment = line;
        }
        return lineWithoutComment;
    }
}
