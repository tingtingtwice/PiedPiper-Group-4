package pppp.g4;

import pppp.sim.Point;

public class Utils {

	public Utils() {
		// TODO Auto-generated constructor stub
	}
	// Euclidean distance between two points
	public static double distance(Point p1, Point p2)
	{
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
	public static boolean allowMove(int playerId, int currentPipersId , Point[][] pipers,Point dst, int total_regions){
		Point currentPiper=pipers[playerId][currentPipersId];
		int cur_pipers_region=currentPipersId % total_regions;
		double d0=distance(currentPiper, dst);
		for (int p = 0 ; p != pipers[playerId].length ; ++p){
			if(p%total_regions == cur_pipers_region && p!=currentPipersId){
				Point nearbyPiperPoint = pipers[playerId][p];
				double d1=distance(nearbyPiperPoint, dst);
				if((d1-d0)>4) return false;
			}

		}
		return true;
	}
}
