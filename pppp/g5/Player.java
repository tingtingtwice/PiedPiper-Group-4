/*
* Our feature is applicable to only a 1-piper configuration.
*
* This feature greedily looks for the nearest rat, and then searches
* for more rats "nearby". A rat is "nearby", if it can be reached within 5 seconds.
* We estimate the time needed to encounter all rats, and if there is any rat that can be encountered/reached 
* in 5 seconds, the piper moves towards it.
* If it finds such a rat, it hovers around for 5 more seconds.
* This goes on till it does not find anymore rats nearby, in which case, it heads back to the gate.
*
* The idea was partly adopted from the Analytic solution suggested by Group 1 last year.
*/

package pppp.g5;

import java.util.Random;

import pppp.sim.Move;
import pppp.sim.Point;


public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[] last_rat_pos = null;
	private boolean playing = false;
	private Random gen = new Random();

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

	private boolean in_square(double x, double y){
		if (x>-side*0.5 && x<side*0.5 && y>-side*0.5 && y<side*0.5)
			return true;
		else 
			return false;
	}

	// find the nearest rat
	private Point find_next_rat(Point piper, Point[] rats,boolean playing, double max_time)
	{
		Point ans = null;
		for(int i=0;i< rats.length;i++){
			double dist_x = rats[i].x-piper.x;
			double dist_y = rats[i].y-piper.y;
			double speed_x = rats[i].x-last_rat_pos[i].x;
			double speed_y = rats[i].y-last_rat_pos[i].y;
			double dist = Math.sqrt(dist_x*dist_x+dist_y*dist_y);
			if (dist<10 && playing){
				continue;
			}
			double speed = playing? 0.1:0.5;
			double speed_vertical = speed_y*dist_y/dist+speed_x*dist_x/dist;
			double speed_parallel = speed_x*dist_y/dist-speed_y*dist_x/dist;
			double speed_piper_vertical = Math.sqrt(speed*speed-speed_parallel*speed_parallel);
			double time_est = (dist-8)/(speed_piper_vertical-speed_vertical);
			if (0< time_est && time_est < max_time){
				double dst_x =piper.x + time_est*(speed_piper_vertical*dist_x/dist+speed_parallel*dist_y/dist);//rats[i].x + time_est*speed_x;//speed_parallel*time_est* dist_y / dist;
				double dst_y =piper.y + time_est*(speed_piper_vertical*dist_y/dist-speed_parallel*dist_x/dist);//rats[i].y + time_est*speed_y;//speed_parallel*time_est* dist_x / dist;
				if (in_square(dst_x,dst_y)){
					max_time = time_est;
					ans = new Point(dst_x,dst_y);
				}
			}
		}
		return ans;
	}
	
	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{
		this.id = id;
		this.side = side;
		int n_pipers = pipers[id].length;
		pos_index = new int [n_pipers];
		pos = new Point [n_pipers][3];
		for (int p = 0 ; p != n_pipers ; ++p) {
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			// first position is at the door
			pos[p][0] = point(0, side * 0.5, neg_y, swap);
			// start with first position
			pos_index[p] = 0;
		}
		last_rat_pos = new Point[rats.length];
		for (int i = 0 ; i != rats.length ; ++i) {
			last_rat_pos[i] = new Point(rats[i].x,rats[i].y);
		}
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = pos[p][pos_index[p]];
			// if position is reached
			if (Math.abs(src.x - dst.x) < 0.000001 &&
			    Math.abs(src.y - dst.y) < 0.000001) {
			    if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
				// go towards gate
				if (pos_index[p] == 0){
					playing = false;
				}
				// find the nearest rat
				else if (pos_index[p] == 1){					
					pos[p][pos_index[p]] = find_next_rat(pipers[id][p],rats,playing,Double.MAX_VALUE);
					if (pos[p][pos_index[p]]==null){
						double x = (gen.nextDouble() - 0.5) * side * 0.9;
						double y = (gen.nextDouble() - 0.5) * side * 0.9;
						pos[p][pos_index[p]] = new Point(x,y);
					}
				}
				// find more rats in the limited time
				else if (pos_index[p] == 2){
					playing =true;
					double max_time = 50;  
					pos[p][pos_index[p]] = find_next_rat(pipers[id][p],rats,playing,max_time);
					// cannot find any more rats
					if (pos[p][pos_index[p]] ==null){
						double dist_x = (pos[p][0].x-pos[p][1].x);
						double dist_y = (pos[p][0].y-pos[p][1].y);
						double dist = Math.sqrt(dist_x*dist_x+dist_y*dist_y);
						pos[p][pos_index[p]] = new Point(pos[p][0].x+8*dist_x/dist,pos[p][0].y+8*dist_y/dist);
					}
					// found more rats nearby
					else{
						pos_index[p]--;
						pos[p][pos_index[p]] = new Point(pos[p][pos_index[p]+1].x,pos[p][pos_index[p]+1].y);
					}
					if (pos_index[p] == pos[p].length) {
						pos_index[p] = 0;
					}
				}
				dst = pos[p][pos_index[p]];
			}

			// move towards position
			moves[p] = move(src, dst, playing);
		}

		last_rat_pos = new Point[rats.length];
		for (int i = 0 ; i != rats.length ; ++i) {
			last_rat_pos[i] = new Point(rats[i].x,rats[i].y);
		}
	}
}
