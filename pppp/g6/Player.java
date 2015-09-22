package pppp.g6;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int currentStrategy = -1;
	private final int NUMBER_OF_MOVES = 6;
	private int n_rats;
	private int n_pipers;
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[][] posStrategySparse = null;
	private Point[][] posStrategyDense = null;
	//private Point[] random_pos = null;
	//private Random gen = new Random();
	private boolean directionFlag =false;
	
	
	HashMap<Integer,int[]> strategyPositionArray = new HashMap<Integer,int[]>();
	
	public class Strategy{
		static final int SPARSE_STRATEGY = 1;
		static final int DENSE_STRATEGY = 2;
	}

	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play)
	{
		double dx = dst.x - src.x;
		double dy = dst.y - src.y;
		double length = Math.sqrt(dx * dx + dy * dy);
		double limit = play ? 0.1 : 0.5;
		if (length > limit) {
			dx = (dx * limit) / length;
			dy = (dy * limit) / length;
		}
		return new Move(dx, dy, play);
	}

	// generate point after negating or swapping coordinates
	private static Point point(double x, double y,
	                           boolean neg_y, boolean swap_xy)
	{
		if (neg_y) y = -y;
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}

	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{

		
		this.id = id;
		this.side = side; 
		n_pipers = pipers[id].length;
		n_rats = rats.length;
		//println("N Pipers" + n_pipers);
		posStrategySparse = new Point[n_pipers][NUMBER_OF_MOVES];
		posStrategyDense = new Point[n_pipers][NUMBER_OF_MOVES];
		
		pos = new Point [n_pipers][NUMBER_OF_MOVES];
		//random_pos = new Point [n_pipers];
		pos_index = new int [n_pipers];

		// Check for sparse or dense environment
		if(n_rats < 20){
		
			// Use sparse strategy
			currentStrategy = Strategy.SPARSE_STRATEGY;
		}
		
		else{
	
			// Use dense stragegy
			currentStrategy = Strategy.DENSE_STRATEGY;
		}
		
		
		setStrategyMoves(currentStrategy,pipers,rats);
		
	}
	
	public void setStrategyMoves(int currentStrategy,Point[][] pipers, Point[] rats){
		
		switch(currentStrategy){
		
		case Strategy.SPARSE_STRATEGY: sparseStrategyPositions(pipers,rats);
			break;
			
			
		case Strategy.DENSE_STRATEGY: denseStrategyPositions();
			break;
		
		}
	}

	public void sparseStrategyPositions(Point[][] pipers, Point[] rats){
		// pick coordinate based on where the player is
		boolean neg_y = id == 2 || id == 3;
		boolean swap  = id == 1 || id == 3;
		
		
		for (int p = 0 ; p != n_pipers ; ++p) {
			
			
			
			double door = 0.0;
			//first point is the door
			pos[p][0]  = point(door, side * 0.5, neg_y, swap);
			
			//second point is the calculated angle for each piper
			if(p == 0){
				pos[p][1] = findNearestRatWithinInfluence(rats, pos[p][0]);
				if(pos[p][1] == null){
					pos[p][1] = pipers[id][p];
				}
			}
			else{
				pos[p][1] = findNearestRatOutsideInfluence(rats, pipers[id][p], posStrategySparse[0][1],pos[p][0]);
				if(pos[p][1] == null){
					pos[p][1] = pipers[id][p];
				}
			}
			
			//third point is the merge point outside the door
			
			pos[p][2] = pos[p][0];			
			pos[p][3] = point(door * -6, side * 0.5 + 3, neg_y, swap);
			pos[p][4] = point(door * +6, side * 0.5 + 3, neg_y, swap);
			// No 6th step
			pos[p][5] = pos[p][4];
			
			
			pos_index[p] = 0;
			
		}
		
	}


	
	public void denseStrategyPositions(){
		
	//	int angle = 180/n_pipers;
		
		double distance = (3 * side)/5;
		//double distance = side;
		
		double slice = (180/(n_pipers +1));
		
		for (int p = 0 ; p != n_pipers ; ++p) {
			
			
			
			//double theta = Math.toRadians(p * 90.0/(n_pipers-1) + 45);
			
			
			double theta = Math.toRadians((p+1)*slice);
			
			// spread out at the door level
			double door = 0.0;
			if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			
			//first point is the door
			pos[p][0]  = point(door, side * 0.5, neg_y, swap);
			
			//second point is the calculated angle for each piper
			pos[p][1] = point(distance * Math.cos(theta),distance * (1- Math.sin(theta)),neg_y,swap);
			
			//third point is the merge point outside the door
			//pos[p][2] = point(door - 5, 0, neg_y, swap);
			
			pos[p][2] = point(0, distance * (1- Math.sin(theta)) ,neg_y, swap);
			System.out.println("$$$$$$ POINT $$$$$$$" + distance * (1- Math.sin(theta)));
			
			pos[p][3]  = point(door * 0.1, side * 0.5, neg_y, swap);
			
			// fourth and fifth positions are outside the rat moving area
			pos[p][4] = point(door * -6, side * 0.5 + 6, neg_y, swap);
			pos[p][5] = point(door * +6, side * 0.5 + 6, neg_y, swap);
			
			
			pos_index[p] = 0;
			
		}
		
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
			n_rats = rats.length;
			
			
			for (int p = 0 ; p != n_pipers ; ++p) {
			Point src = pipers[id][p];
			// Reupdate target position for constantly moving targets
			if(currentStrategy == Strategy.SPARSE_STRATEGY && p == 1){
				if(p == 0){
					pos[p][1] = findNearestRatWithinInfluence(rats, pos[p][0]);
					if(pos[p][1] == null){
						pos[p][1] = pipers[id][p];
					}
				}
				else{
					pos[p][1] = findNearestRatOutsideInfluence(rats, pipers[id][p], posStrategySparse[0][1],pos[p][0]);
					if(pos[p][1] == null){
						pos[p][1] = pipers[id][p];
					}
				}
			}
			Point dst = pos[p][pos_index[p]];
			
			
			// if null then get random position
			//Random commented
			/*if (dst == null) {
				dst = random_pos[p];
			}*/
			
			
			// if position is reached
			if (Math.abs(src.x - dst.x) < 0.000001 &&
			    Math.abs(src.y - dst.y) < 0.000001) {
				
				
				// discard random position
				//Random commented
				//if (dst == random_pos[p]) random_pos[p] = null;
				
				// get next position
				if (++pos_index[p] == pos[p].length) pos_index[p] = 0;{
				dst = pos[p][pos_index[p]];
				}
				
				
				// generate a new position if random
				//Random commented
				/*
				if (dst == null) {
					if(toogle(directionFlag)){
						double x = (gen.nextDouble() - 0.5) * side * 0.9;
						double y = Math.abs((gen.nextDouble() - 0.5) * side * 0.9)*-1;
						random_pos[p] = dst = new Point(x, y);
					}
					else{
						double x = (gen.nextDouble() - 0.5) * side * 0.9;
						double y = Math.abs((gen.nextDouble() - 0.5) * side * 0.9);
						random_pos[p] = dst = new Point(x, y);
					}
					
				}*/
				
			}
			// get move towards position
			moves[p] = move(src, dst, pos_index[p] > 1);
			//moves[p+1] = move(src2, new Point(dst.x+.25,dst.y+.25), pos_index[p] > 1);
		}
	}
	
	public boolean toogle(boolean flag){
		directionFlag = !flag;
		return directionFlag;
	}


	//Sparse Strategy Functions
	
	// Returns location of the nearest rat within the semi-sphere
	// (ie rats guarenteed to be grabbed safely)
	// returns -1 if none
	public void findNearestRatWithinInfluence(){
		
	}
	
	
	double truncate(double number, int precision)
	{
	    double prec = Math.pow(10, precision);
	    int integerPart = (int) number;
	    double fractionalPart = number - integerPart;
	    fractionalPart *= prec;
	    int fractPart = (int) fractionalPart;
	    fractionalPart = (double) (integerPart) + (double) (fractPart)/prec;
	    return fractionalPart;
	}
	
	// Used to finding nearest rat within radius
	public Point findNearestRatWithinInfluence(Point[] rats, Point doorPos){
		float minDistance = -1;
		int ratIndex = -1;
		for(int i = 0; i<rats.length;i++){
			float dist = (float) doorPos.distance(rats[i]);
			if(ratIndex == -1){
				minDistance = dist;
				ratIndex = i;
			}
			else{
				if(minDistance < dist){
					minDistance = dist;
					ratIndex = i;
				}
			}
		}
		// If distance <= half length of wall, then proceed to it
		if(minDistance <= side/2){
			return rats[ratIndex];
		}
		else{
			return null;
		}
	}

	
	// Used for finding rats outside "guarenteed radius"
	public Point findNearestRatOutsideInfluence(Point[] rats, Point curPos, Point takenRatPos,Point doorPos){
		float minDistance = -1;
		int ratIndex = -1;
		for(int i = 0; i<rats.length;i++){
			if(takenRatPos == rats[i]){
				continue;
			}
			float dist = (float) doorPos.distance(rats[i]);
			if(ratIndex == -1){
				minDistance = dist;
				ratIndex = i;
			}
			else{
				if(minDistance < dist){
					minDistance = dist;
					ratIndex = i;
				}
			}
		}
		if(ratIndex != -1){
			return rats[ratIndex];
		}
		else{
			return null;
		}
	}


	
}
