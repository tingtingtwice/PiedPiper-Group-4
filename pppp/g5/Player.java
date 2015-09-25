package pppp.g5;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int[] count;
	private int id = -1;
	private int side = 0;
	private int grid_size = 15;
	private Point gate;
	private int[] pos_index = null;
	// private Point[] last_destination;
	private Point[][] pos = null;
	private ArrayList<Grid> gridlist;
	private ArrayList<Piper> piperlist;
	private Point[] random_pos = null;
	private Random gen = new Random();
	private boolean isPiperAtGate = false;

	// create move towards specified destination
	private double distance (Point a, Point b){
		
		return Math.sqrt((a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y));
	}
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
	private double calculate_weight(Point p){
		double weight = 1 / Math.pow((100+Math.abs(distance(gate,p)-side* .5)),0.1);
		return weight;
	}
	// Update information
	// Calculate the density of different cells
	private void update_circumstance(Point[][] pipers, boolean[][] pipers_played, Point[] rats){
		
		for (int i=0;i<pipers[id].length;i++){
			piperlist.add(new Piper(pipers[id][i],pipers_played[id][i],i));
		}
		double density =(double) rats.length / (side*side);
		double max_weight = 0;
		if (density * Math.PI * 10*10 < 1) {
			for (int i=0;i<rats.length;i++){
				int catched_times = 0;
				ArrayList<Integer> index = new ArrayList<Integer>();
				for(int p=0;p<pipers[id].length;p++){
					if (pos_index[p] == 2 || pos_index[p] == 3){
						if (distance(pipers[id][p],rats[i]) < 10){
							catched_times++;
							index.add(p);
						}
					}
				}
				for(Integer j:index){
					piperlist.get(j).rats+=Math.pow(2, -catched_times)*calculate_weight(piperlist.get(j).pos);
				}

				if (catched_times ==0) {
					Grid cell = new Grid(new Point(rats[i].x,rats[i].y),0);
					cell.rats++;
					cell.rats = cell.rats*calculate_weight(cell.center);
					if (max_weight < cell.rats)
						max_weight = cell.rats;
					gridlist.add(cell);
				}
			}
		}
		else {
			int grid_num = (side-1)/grid_size+1;
			double true_size = (double)side/grid_num;
			for(int i=0;i<grid_num;i++){
				for(int j=0;j<grid_num;j++){
					Grid cell = new Grid(new Point((j+.5)*true_size-side*.5,(i+.5)*true_size-side*.5),0);
					gridlist.add(cell);
				}
			}
			// calculate how many rats are in one cell, if the rat is already influenced by the piper, decrease the weight of this rat
			for (int i=0;i<rats.length;i++){
				int catched_times = 0;
				ArrayList<Integer> index = new ArrayList<Integer>();
				for(int p=0;p<pipers[id].length;p++){
					if (pos_index[p] == 2 || pos_index[p] == 3){
						if (distance(pipers[id][p],rats[i]) < 10){
							catched_times++;
							index.add(p);
						}
					}
				}
				for(Integer j:index){
					piperlist.get(j).rats+=Math.pow(2, -catched_times+1)*calculate_weight(piperlist.get(j).pos);
				}
				if (catched_times ==0) {
					int col = new Double((rats[i].x+side*.5) / true_size).intValue();
					int row = new Double((rats[i].y+side*.5) / true_size).intValue();
					//gridlist.get(row*grid_num+col).rats+=Math.pow(2, -catched_times);
					 gridlist.get(row*grid_num+col).rats++;
				}
				
			}
			//give the grid different weight according to distance between it and the gate
			//we may need choose a better weight formula
			for(Grid cell: gridlist){
				cell.rats = cell.rats*calculate_weight(cell.center);
				if (max_weight < cell.rats)
					max_weight = cell.rats;
			}
//			for(int i=grid_num-1;i>=0;i--){
//				for(int j=0;j<grid_num;j++){
//					System.out.print(String.format("%1$.2f",gridlist.get(i*5+j).rats)+"\t");
//				}
//				System.out.println();
//			}
//			System.out.println();
		}

		//if the piper lose all its rats when come back to the gate, stop going back
		for(int p=0;p<pipers[id].length;p++){
			if (pos_index[p] == 2){
				Piper current_one = piperlist.get(p);
				if (current_one.rats < max_weight / 4 || current_one.rats ==0)
					pos_index[p]=1;
				else{
					gridlist.add(new Grid(current_one.pos,current_one.rats/2));
				}
			}
		}
		
		//sort the grid
		gridlist.sort(null);
		return;
	}
	//allocate jobs to different pipers
	private void allocate_destination(ArrayList<Grid> gridlist, ArrayList<Piper> free_pipers){
		int piper_num = free_pipers.size();
		// double[][] weight_matrix = new double[piper_num][piper_num];
		TreeSet<Grid> sorted_grid = new TreeSet<Grid>();
		int num = (piper_num < gridlist.size()) ? piper_num:gridlist.size();
		for (int i=0;i<num;i++){
			sorted_grid.add(gridlist.get(i));
		}
		boolean[] if_free= new boolean [piper_num];
		for(int i=0;i<piper_num;i++){
			if_free[i] = true;
		}
//		for(int i=0;i<piper_num;i++){
//			for(int j=0;j<piper_num;j++){
//				weight_matrix[i][j] = 1 ; /// this.distance(free_pipers.get(j).pos,gridlist.get(i).center);
//				weight_matrix[i][j] = weight_matrix[i][j] * gridlist.get(i).rats;
//			}
//		}
		for(int i=0;i<piper_num;i++){
			double max_weight = 0;
			int piper_id = -1;
			Grid cell = sorted_grid.pollFirst();
			for(int j=0;j<piper_num;j++){
				if (if_free[j]){
					double weight = cell.rats / Math.pow(100+this.distance(free_pipers.get(j).pos,cell.center),0.1);
					if (max_weight < weight){
						piper_id = j;
						max_weight = weight;
					}
				}
			}
			pos[free_pipers.get(piper_id).index][1] = cell.center;
//			System.out.println("piper("+free_pipers.get(piper_id).index+"):"+ max_weight * Math.pow(100+this.distance(free_pipers.get(piper_id).pos,cell.center),0.1));
			cell.rats /=1.5;
			sorted_grid.add(cell);
			if_free[piper_id] = false;
		}
		return;
	}
	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{
		this.id = id;
		this.side = side;
		this.gridlist = new ArrayList<Grid>();
		this.piperlist = new ArrayList<Piper>();
		int n_pipers = pipers[id].length;
		pos = new Point [n_pipers][4];
		random_pos = new Point [n_pipers];
		pos_index = new int [n_pipers];
		count = new int [n_pipers];
		for(int i=0;i<n_pipers;i++) {count[i] = 0;}
		for (int p = 0 ; p != n_pipers ; ++p) {
			// spread out at the door level
			double door = 0.0;
			if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			// first and third position is at the door
			
			gate = pos[p][0] = pos[p][2] = point(0, side * 0.5, neg_y, swap);
			// second position is calculated
			// double r = 30;
			// double theta = Math.PI / (n_pipers + 1);
			// double x = r * Math.cos((p + 1) * theta);
			// double y = r * Math.sin((p + 1) * theta);
			// pos[p][1] = point(x, y, neg_y, swap);
			// fourth position behind door to get rat
			pos[p][3] = point(0, side * 0.5+2.1, neg_y, swap);
			// start with first position
			pos_index[p] = 0;
		}
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
		gridlist.clear();
		piperlist.clear();
		update_circumstance(pipers,pipers_played,rats);
		ArrayList<Piper> free_piper = new ArrayList<Piper> ();
		for(int i=0;i< pipers[id].length;i++){
			if (pos_index[i] ==1 /*|| pos_index[i] == 2*/) {
				free_piper.add(new Piper(pipers[id][i],pipers_played[id][i],i));
			}
		}
		if (!gridlist.isEmpty())
			allocate_destination(gridlist,free_piper);
		else {
			for(Piper piper:free_piper){
				pos_index[piper.index] = 3;
			}
		}
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = pos[p][pos_index[p]];
			if (dst == null ) dst = random_pos[p];
			// if position is reached
			if (Math.abs(src.x - dst.x) < 0.000001 &&
			    Math.abs(src.y - dst.y) < 0.000001 ||
			    (this.distance(src,dst) < 3 && pos_index[p] == 1)) {
				// get next position
				if (pos_index[p] == 3) {
					if (isNear(pos[p][pos_index[p]], rats)){
						moves[p] = move(src, src, true);
						continue;
					} else {
						isPiperAtGate = false;
					}
				}
				if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
				if (pos_index[p] == 3) {
					if (isPiperAtGate)
						pos_index[p] = 0;
					else
						isPiperAtGate =true;
				}
				dst = pos[p][pos_index[p]];
				// generate a new position if random
				if (dst == null) {
					double x = (gen.nextDouble() - 0.5) * side * 0.9;
					double y = (gen.nextDouble() - 0.5) * side * 0.9;
					random_pos[p] = dst = new Point(x, y);
				}
			}
			// set music on or off
			boolean music = false;
			if (pos_index[p] == 2 || pos_index[p] == 3) {
				music = true;
			}
			// get move towards position
			moves[p] = move(src, dst, music);
		}
	}

	/* Calculate if any of points is 10m away from center
	 */
	private boolean isNear(Point center, Point[] points) {
		for (Point point : points) {
			if (distance(center, point) < 10) {
				return true;
			}
		}
		return false;
	}
}


class Grid implements Comparable<Grid>{
	Point center;
	Double rats;

	public Grid(Point center, double rats) {
		this.center = center;
		this.rats = rats;
	}

	public int compareTo(Grid g1) {
		return g1.rats.compareTo(this.rats);
	}
}

class Piper {
	Point pos;
	Boolean playing;
	double rats;
	int index;
	public Piper(Point pos, boolean playing, int index) {
		this.pos = pos;
		this.playing = playing;
		this.index = index;
	}
}