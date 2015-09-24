package pppp.g8;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[] random_pos = null;
	private Random gen = new Random();
	
	private boolean[] blowPiper; 
	
	
	private int ratsCountInit = 0;
	private int ratsCountCurrent = 0;
	
	private int playCount = 0;

	private Point[] ratsPrevious = null;
	private Point[] ratsCurrent = null;
	private Point[] ratsFuture = null;
	
	
	private int numberPasses = 0;
	
	private int count = 0;
	private int granularity = 4;
	private int[] largest_ind;
	
	
	
	
	private static double distance(Point a, Point b)		
	{		
		double x = a.x-b.x;		
		double y = a.y-b.y;		
		return Math.sqrt(x * x + y * y);		
	}		
			
	private Point[] nearest_neighbor(Point[][] pipers)		
	{		
		double radius = 5.0; //radius at which pipers considered part of the same cluser		
							//EXPERIMENT with value		
		//keeps track of which pipers still need a nearest neighbor assignment		
		Point[] neighbors = new Point[pipers[id].length];		
		//keeps track of pipers which still need to be assigned a neighbor		
		HashSet<Integer> pipers_remaining = new HashSet<Integer>();		
		//add each piper to the hashset		
		for(int i=0; i<pipers[id].length; ++i)		
	{		
			pipers_remaining.add(i);		
		}		
		
		for(int i=0; i<pipers[id].length; ++i)		
		{		
			//if pipers remaining doesn't contain the piper, then it has already been assigned a neighbor		
			if(!pipers_remaining.contains(i))		
			{		
				continue;		
			}		
			//keeps track of other pipers who are part of the same cluster		
			ArrayList<Integer> companions = new ArrayList<Integer>();		
		
			double min_dist = Double.MAX_VALUE;		
			int neighbor = -1;		
					
			for(int j=0; j<pipers[id].length; j++)		
			{		
				if(!pipers_remaining.contains(i))		
				{		
					continue;		
				}		
				//if another piper is in the viciinity of this piper, consider them 		
				//as part of the same cluster and send them to the same neighbor		
				double dist = distance(pipers[id][i], pipers[id][j]);		
				if (dist < radius)		
				{		
					companions.add(j);		
					pipers_remaining.remove(j);		
					continue;		
				}		
				else if(dist < min_dist)		
				{		
					min_dist = dist;		
					neighbor = j;		
				}		
		
			}		
			//if odd number of pipers, one left without a piar, just sent it to closest other piper		
			if(neighbor == -1)		
			{		
				for(int j=0; j<pipers[id].length; j++)		
				{		
					double dist = distance(pipers[id][i], pipers[id][j]);		
					if(dist<min_dist)		
					{		
						min_dist = dist;		
						neighbor = j;		
					}		
		
				}		
			}		
			neighbors[i] = pipers[id][neighbor];		
			neighbors[neighbor] = pipers[id][i];		
			for(Integer k : companions)		
		{		
				neighbors[k] = pipers[id][neighbor];		
			}		
			pipers_remaining.remove(i);		
			pipers_remaining.remove(neighbor);		
		}		
		return neighbors;		
	}		
		
	//return true if all pipers within a certain radius of eachother		
	//shoudl check before checking for nearest neighbors		
	private boolean pipers_together(double radius, Point[][] pipers)		
	{		
		for (int i=0; i<pipers[id].length; ++i)		
		{		
			for(int j=i; j<pipers[id].length; ++j)		
			{		
				if(distance(pipers[id][i], pipers[id][j])>radius)		
				{		
					return false;		
				}		
			}		
		}		
		return true;		
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
	
	private static Move moveSlowly(Point src, Point dst, boolean play)
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
	
	
	private static double getDistance(Point a, Point b)
	{
		double x = a.x-b.x;
		double y = a.y-b.y;
		return Math.sqrt(x * x + y * y);
	}

	// generate point after negating or swapping coordinates
	private static Point point(double x, double y,
	                           boolean neg_y, boolean swap_xy)
	{
		if (neg_y) y = -y;
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}
	
		

	// specify location that the player will alternate between
	// Init for semi circle sweeping .. 
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{
		//Initialize total Rats
		this.ratsCountInit = rats.length;
		this.ratsCountCurrent = rats.length;
		
		this.id = id;
		this.side = side;
		int n_pipers = pipers[id].length;
		largest_ind = new int[pipers[id].length];
		for (int i=0; i<pipers[id].length; i++)
			largest_ind[i] = 0;
		
		
		blowPiper = new boolean[n_pipers];
		pos = new Point [n_pipers][5];
		random_pos = new Point [n_pipers];
		pos_index = new int [n_pipers];
		for (int p = 0 ; p != n_pipers ; ++p) {
			// spread out at the door level
			double door = 0.0;
			//if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			if (n_pipers != 1) door = -0.95;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			// first and third position is at the door
			pos[p][0] = point(door, side * 0.5, neg_y, swap);
			pos[p][3] = point(door-3, side * 0.50, neg_y, swap);
			
			// Set the second position 
			double xCoordinate = 0.0;
			double yCoordinate = 0.0;
			
			/*if (p == 3)
			{
				xCoordinate = 0.45*side;
				yCoordinate = 0.35*side;
			}
			else
			{
				xCoordinate = (p * 0.4 / (n_pipers - 1) - 0.2) * side;
				yCoordinate = -0.1*side;
			}	*/			
			xCoordinate = (p * 0.4 / (n_pipers - 1) - 0.2) * side;
			yCoordinate = 0.05*side;
			
			pos[p][1] = point(xCoordinate, yCoordinate, neg_y, swap);
			pos[p][2] = point(0, 0.40*side, neg_y, swap);
			
			// second position is chosen randomly in the rat moving area
//			pos[p][1] = null;
			// fourth and fifth positions are outside the rat moving area
			pos[p][4] = point(door * -6, side * 0.5 + 3, neg_y, swap);
			//pos[p][5] = point(door * +6, side * 0.5 + 3, neg_y, swap);
			
			
			// start with first position
			pos_index[p] = 0;
		}
	}
	
	
	private void decideGate(Point[] rats, int p)
	{
		boolean neg_y = id == 2 || id == 3;
		boolean swap  = id == 1 || id == 3;
		double xCoordinate = 0.0;
		double yCoordinate = 0.0;
		// Define point 1 coordinates
		xCoordinate = -0.25*side;
		yCoordinate = 0.25*side;
		Point p1 = point(xCoordinate, yCoordinate, neg_y, swap);
		// Define point 2 coordinates
		xCoordinate = -0.48*side;
		yCoordinate = -0.25*side;
		Point p2 = point(xCoordinate, yCoordinate, neg_y, swap);
		// Define point 1 coordinates
		xCoordinate = 0.25*side;
		yCoordinate = 0.25*side;
		Point p3 = point(xCoordinate, yCoordinate, neg_y, swap);
		// Define point 2 coordinates
		xCoordinate = 0.48*side;
		yCoordinate = -0.25*side;
		Point p4 = point(xCoordinate, yCoordinate, neg_y, swap);
		
		// Define point 1 coordinates
		xCoordinate = -0.25*side;
		yCoordinate = -0.25*side;
		Point p5 = point(xCoordinate, yCoordinate, neg_y, swap);
		// Define point 2 coordinates
		xCoordinate = 0.25*side;
		yCoordinate = -0.48*side;
		Point p6 = point(xCoordinate, yCoordinate, neg_y, swap);
		
		
		int count1 = 0;
		int count2 = 0;
		int count3 = 0;
	
		for (int i=0; i<rats.length; i++)
		{
			double smallX = (p1.x < p2.x) ? p1.x : p2.x;
			double largeX = (p1.x > p2.x) ? p1.x : p2.x;
			double smallY = (p1.y < p2.y) ? p1.y : p2.y;
			double largeY = (p1.y > p2.y) ? p1.y : p2.y;			
			if (rats[i].x < largeX && rats[i].x > smallX && rats[i].y < largeY && rats[i].y > smallY )
			{
				count1++;
			}
			smallX = (p3.x < p4.x) ? p3.x : p4.x;
			largeX = (p3.x > p4.x) ? p3.x : p4.x;
			smallY = (p3.y < p4.y) ? p3.y : p4.y;
			largeY = (p3.y > p4.y) ? p3.y : p4.y;			
			if (rats[i].x < largeX && rats[i].x > smallX && rats[i].y < largeY && rats[i].y > smallY )
			{
				count2++;
			}
			smallX = (p5.x < p6.x) ? p5.x : p6.x;
			largeX = (p5.x > p6.x) ? p5.x : p6.x;
			smallY = (p5.y < p6.y) ? p5.y : p6.y;
			largeY = (p5.y > p6.y) ? p5.y : p6.y;			
			if (rats[i].x < largeX && rats[i].x > smallX && rats[i].y < largeY && rats[i].y > smallY )
			{
				count3++;
			}				
			
			
		}		
		int max = Math.max(count3, Math.max(count1, count2));		
		if (max == count1)
		{
			pos[p][pos_index[p]] = point(-0.45*side, 0, neg_y, swap);
		}
		else if (max == count2)
		{
			pos[p][pos_index[p]] = point(0.45*side, 0, neg_y, swap);
		}
		else
		{
			pos[p][pos_index[p]] = point(0, -0.45*side, neg_y, swap);
		}
	}
	
	
	private void waitAtGate(Point[] rats, int p)
	{
		int ratsInRange = 0;
		for (int i=0; i<rats.length; i++)
		{
			if (getDistance(pos[p][pos_index[p]], rats[i]) < 10)
			{
				ratsInRange++;
			}
		}
		if (ratsInRange < 4 )
		{
			if (rats.length > 2)
			{
				pos_index[p] = 0;
			}
			if (ratsInRange > 0)
			{
				// Wait there only
				blowPiper[p] = true;
			}
		}
	}
	
	
	private void initSweepPosition(Point[] rats, Point[][] pipers)
	{
		// Where would all the rats be after 5 seconds
		// No rats would be captured in the first 0.1 seconds
		// Initialize the starting position for sweeping
		if (playCount == 0)
		{
			ratsPrevious = new Point[rats.length];
			for (int i=0; i<rats.length; i++ )
			{
				ratsPrevious[i] = rats[i];
			}
			
		}
		else if (playCount == 1)
		{
			ratsCurrent = new Point[rats.length];
			for (int i=0; i<rats.length; i++ )
			{
				ratsCurrent[i] = rats[i];
			}
		}
		
		if (playCount == 2 )
		{
			ratsFuture = new Point[rats.length];
			for (int i=0; i<rats.length; i++ )
			{
				double xCoordinate = ratsCurrent[i].x  + (ratsCurrent[i].x - ratsPrevious[i].x)*40 ;
				double yCoordinate = ratsCurrent[i].y  + (ratsCurrent[i].y - ratsPrevious[i].y)*40 ;
				
				if (xCoordinate < -0.5*side)
				{
					xCoordinate = -0.49*side;
				}
				if (xCoordinate > 0.5*side)
				{
					xCoordinate = 0.49*side;
				}
				
				if (yCoordinate < -0.5*side)
				{
					yCoordinate = -0.49*side;
				}
				if (yCoordinate > 0.5*side)
				{
					yCoordinate = 0.49*side;
				}
							
				Point p = new Point(xCoordinate, yCoordinate);
				ratsFuture[i] = p;
			}
			
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			
			double xCoordinate = 0.0;
			double yCoordinate = 0.0;
			xCoordinate = 0.5*side;
			yCoordinate = 0.5*side;
			Point p1 = point(xCoordinate, yCoordinate, neg_y, swap);
			
			double smallXP1 = Math.min(p1.x, 0.0);
			double largeXP1 = Math.max(p1.x, 0.0);
			double smallYP1 = Math.min(p1.y, 0.0);
			double largeYP1 = Math.max(p1.y, 0.0);	

			xCoordinate = -0.5*side;
			yCoordinate = 0.5*side;
			Point p2 = point(xCoordinate, yCoordinate, neg_y, swap);
			
			double smallXP2 = Math.min(p2.x, 0.0);
			double largeXP2 = Math.max(p2.x, 0.0);
			double smallYP2 = Math.min(p2.y, 0.0);
			double largeYP2 = Math.max(p2.y, 0.0);	
			
			int countLeft = 0 ;
			int countRight = 0 ;
			for (int i=0; i<rats.length; i++ )
			{
				if (ratsFuture[i].x > smallXP2 && ratsFuture[i].x<largeXP2 && ratsFuture[i].y > smallYP2 && ratsFuture[i].y < largeYP2)
				{
					countLeft++;
				}
				
				if (ratsFuture[i].x > smallXP1 && ratsFuture[i].x<largeXP1 && ratsFuture[i].y > smallYP1 && ratsFuture[i].y < largeYP1)
				{
					countRight++;
				}
				
			}
			
			xCoordinate = 0.0;
			yCoordinate = 0.0;
			int pipersLength = pipers[id].length;
			
			double offSet = (countLeft > countRight ? 0.4 : -0.1);
			double offSetY = 0.06;
			
			for (int p = 0 ; p != pipers[id].length ; ++p) {
				xCoordinate = (p * 0.3 / (pipersLength - 1) - offSet) * side;
				if (countRight > countLeft)
				{
					yCoordinate = (0.05 + p*offSetY)*side;
				}
				else
				{
					yCoordinate = (0.05 + (pipersLength - p - 1)*offSetY)*side;
				}
				
				pos[p][1] = point(xCoordinate, yCoordinate, neg_y, swap);
			}		
		}
	}
	
	
	private int[] findNofRats(Point[] rats, int granularity)
	{
		int rats_per[] = new int[granularity*granularity];
		for (int i=0; i<granularity; i++)
		{
		rats_per[i] = 0;
		}
		for (int i=0; i<rats.length; i++)
		{
			for (int j=0; j<granularity; j++)
			{
				for (int k=0; k<granularity; k++)
				{
					if ((rats[i].x >= k*side/granularity - side/2 && rats[i].x <= (k+1)*side/granularity - side/2) &&
							(rats[i].y >= -(j+1)*side/granularity + side/2 && rats[i].y <= -j*side/granularity + side/2))
						rats_per[j*granularity+k]++;
				}
			}
		}
		return rats_per;
	}
	
	private double[] calculatePiperDist(Point[] rats, Point[][] pipers, int p, int granularity)
	{
		double dist[] = new double[granularity*granularity];
		for (int i=0; i<granularity; i++)
		{
			for (int j=0; j<granularity; j++)
			{
				Point centerofCell = new Point(granularity*j*side - 3*side/8, -granularity*i*side + 3*side/8);
				dist[i*granularity+j] = getDistance(pipers[id][p], centerofCell);
			}
		}
		return dist;
	}
	
		
	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{	
		if (rats.length > 5)
		{
			initSweepPosition(rats, pipers);
			playCount++;
			
			for (int p = 0 ; p != pipers[id].length ; ++p) {
				blowPiper[p] = false;
			}
			if (numberPasses >= 4)
			{
				this.ratsCountCurrent = rats.length;
				for (int p = 0 ; p != pipers[id].length ; ++p) {
					if (pos_index[p] == 1)
					{
						// Set position at gate, Decide the gate 
						// pick coordinate based on where the player is
						decideGate(rats, p);
					}
				}
			}
			for (int p = 0 ; p != pipers[id].length ; ++p) {
				Point src = pipers[id][p];
				Point dst = pos[p][pos_index[p]];
				boolean flag = false;
				boolean flag_rats = false;
				for (int i=0; i<rats.length; i++)
					if (getDistance(pipers[id][p], rats[i]) <= 10) flag_rats = true;
				if (!flag_rats && pos_index[p] == 2)
				{
					pos_index[p] = 1;
				}
				
	
				// if position is reached
				if (Math.abs(src.x - dst.x) < 0.000001 &&
				    Math.abs(src.y - dst.y) < 0.000001) {
					// Piper has reached position 1 , Wait until he has got a certain number of rats
					if (pos_index[p] == 1 && numberPasses >= 4)
					{
						boolean flag_pipers_2gether = true;
						for (int i=0; i<pipers[id].length; i++)
							if (getDistance(pipers[id][i], pipers[id][p]) >= 10) flag_pipers_2gether = false;
						if (count < 500 )
						{
							waitAtGate(rats, p);
							count ++;
						}
						else
						{
							count = 0;
							flag = true;
							int rats_per[] = findNofRats(rats, granularity);
							double dist[] = calculatePiperDist(rats, pipers, p, granularity);
							double ratio[] = new double[granularity*granularity];
							for(int i=0; i<granularity*granularity; i++)
									ratio[i] = rats_per[i] / dist[i];
							int largest_ind_temp = 0;
							double largest_ratio = ratio[0];
							for(int i=1; i<granularity*granularity; i++)
								if (largest_ratio < ratio[i])
									{
									largest_ratio = ratio[i];
									largest_ind_temp = i;
									}
							if (largest_ind[p] != largest_ind_temp)
							{
								Random random = new Random();
								int row, col;
								row = largest_ind_temp / granularity;
								col = largest_ind_temp - row * granularity;
								//set pos[p][1] to be a random place within the largest rats/distance ratio area
								pos[p][1] = new Point(col*side/granularity-side/2 + (side/granularity)*random.nextDouble(), 
										-(row+1)*side/granularity + (side/granularity)*random.nextDouble());
								largest_ind[p] = largest_ind_temp;
							}
							
						}
					}
					if (pos_index[p] == 3)
					{
						numberPasses++;
					}
					// get next position
					if (!flag_rats && pos_index[p] == 1 && flag)
					{
						int a=0;
					}
					else{
						if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
					}
						
					dst = pos[p][pos_index[p]];
				}
				// get move towards position
				if (pos_index[p] == 3 || pos_index[p] == 2)
				{
					moves[p] = moveSlowly(src, dst, pos_index[p] > 1);
				}
				else
				{
					moves[p] = move(src, dst, pos_index[p] > 1);
				}
			}
		}
		
		else
		{
			boolean pipers_clustered = pipers_together(5,pipers);		
			Point[] next;	
			if(!pipers_clustered)		
			{		
			 	next = nearest_neighbor(pipers);		
			}		
			 else		
			 {		
				 next = null;		
			 }
			
			
			for (int p = 0 ; p != pipers[id].length ; ++p) {
				Point src = pipers[id][p];
				Point dst = pos[p][pos_index[p]];
				boolean flag = false;
				boolean flag_rats = false;
				for (int i=0; i<rats.length; i++)
					if (getDistance(pipers[id][p], rats[i]) <= 10) flag_rats = true;
				if (!flag_rats && pos_index[p] == 2)
				{
					pos_index[p] = 1;
				}
				int rats_per[] = findNofRats(rats, granularity);
				double dist[] = calculatePiperDist(rats, pipers, p, granularity);
				double ratio[] = new double[granularity*granularity];
				for(int i=0; i<granularity*granularity; i++)
					ratio[i] = rats_per[i] / dist[i];
				int largest_ind_temp = 0;
				double largest_ratio = ratio[0];
				for(int i=1; i<granularity*granularity; i++)
					if (largest_ratio < ratio[i])
						{
						largest_ratio = ratio[i];
						largest_ind_temp = i;
						}
				if (largest_ind_temp != largest_ind[p])
				{
				Random random = new Random();
				int row, col;
				row = largest_ind_temp / granularity;
				col = largest_ind_temp - row * granularity;
				//set pos[p][1] to be a random place within the largest rats/distance ratio area
				pos[p][1] = new Point(col*side/granularity-side/2 + (side/granularity)*random.nextDouble(), 
						-(row+1)*side/granularity + (side/granularity)*random.nextDouble());
				largest_ind[p] = largest_ind_temp;
				}
				
				if(!pipers_clustered)		
				{		
					pos[p][2] = next[p];		
				}
				
				
				// if null then get random position
				if (dst == null) dst = random_pos[p];
				// if position is reached
				if (Math.abs(src.x - dst.x) < 0.000001 &&
				    Math.abs(src.y - dst.y) < 0.000001) {
					flag = true;
					// discard random position
					if (dst == random_pos[p]) random_pos[p] = null;
					// get next position
					if (!flag_rats && pos_index[p] == 1)
					{
						int a=0;
					}
					else
					{
						if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
					}
						
					dst = pos[p][pos_index[p]];
					// generate a new position if random
					if (dst == null) {
						double x = (gen.nextDouble() - 0.5) * side * 0.9;
						double y = (gen.nextDouble() - 0.5) * side * 0.9;
						random_pos[p] = dst = new Point(x, y);
					}
				}
				// get move towards position
				moves[p] = move(src, dst, pos_index[p] > 1);
			}
		
		}
		
	}

	// This method is follows greedy approach : 
	// The pipers go to the nearest rat , start playing the pipe and come back 
	/*public void play2(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{	
		
		boolean teamPlaying = false;
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			boolean playPiper = false;
			boolean piperStationary = false;
			double minDistance = Double.MAX_VALUE;
			Point src = pipers[id][p];	
			double distance = 0.0;
			//Point dst = null;
			Point dst = pos[p][pos_index[p]];
			// if null then get random position
			//if (dst == null) dst = random_pos[p];
			
			//Calculate if piper should be played or not
			double minX = 0.0;
			double minY = 0.0;
			Point minRat = null;
			for (int i =0; i<rats.length; i++)
			{
				distance = getDistance(src, rats[i]);
				
				if (distance < minDistance)
				{
					minRat = rats[i];
					minX = minRat.x;
					minY = minRat.y;
					minDistance = distance;
				}
			}
			if (minDistance < 3)
			{
				playPiper = true;
				teamPlaying = true;
			}
			
			//If distance between piper and gate is less than 2m, stop the piper
			distance = getDistance(src, pos[p][0]);
			if (distance < 5)
			{
				for (int temp = 0 ; temp < p ; ++temp)
				{
					Point mySrc = pipers[id][temp];
					//If distance between piper and gate is less than 2m, stop the piper
					distance = getDistance(mySrc, pos[p][0]); 
					if (distance < 6  && teamPlaying == true)
					{
						//System.out.println("Reached here at least ");
						//if (pipers_played[id][temp] == true)
						//{
							//System.out.println("Reached here man ");
							playPiper = false;
							piperStationary = true;
							break;
						//}
					}
				}
			} 
			if (pos_index[p] == 1)
			{	
				// If the minimum distance is less than 10, it means that rat is within the piper's range,
				// Now it will get automatically attracted towards the piper
				if (minDistance < 3)
				{
					//Increment pos index
					pos_index[p] = pos_index[p] + 1;
					dst = pos[p][pos_index[p]];
				}
				else
				{
					// Continue looking for the rat unless one rat is found
					// Sweeping
					// Update the destination
					dst = new Point(minX, minY);
				}
			}
			else
			{
				if (Math.abs(src.x - dst.x) < 0.000001 && Math.abs(src.y - dst.y) < 0.000001) {
					// get next position
					if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
					dst = pos[p][pos_index[p]];
					
					if (pos_index[p] == 1)
					{
						dst = new Point(minX, minY);
					}
				}
				if (piperStationary == true)
				{
					dst = new Point(src.x, src.y);
				}
				
			}
			// If the position is reached,  get move towards position
			//moves[p] = move(src, dst, pos_index[p] > 1);
			if (src == null)
			{
				System.out.println("Source is null ");
			}
			if (dst == null)
			{
				System.out.println("Destination is null ");
			}
			
			moves[p] = move(src, dst, playPiper);
			//moves[p] = move(src, dst, true);
	
	*/
}
