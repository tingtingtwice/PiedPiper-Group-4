package pppp.g4;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import pppp.sim.Point;
import pppp.sim.WinnerInfo;

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
	
	public static void shuffleArray(int[] ar)
	  {
	    Random rnd = new Random();
	    for (int i = ar.length - 1; i > 0; i--)
	    {
	      int index = rnd.nextInt(i + 1);
	      // Simple swap
	      int a = ar[index];
	      ar[index] = ar[i];
	      ar[i] = a;
	    }
	  }
	
	

	// Yields the number of rats within range
    public static int num_captured_rats(Point piper, Point[] rats) {
        int num = 0;
        for (Point rat : rats)
            num += Utils.distance(piper, rat) <= 10 ? 1 : 0;
        return num;
    }
/** within the Cluster find the most densest point and then start going back from it.. ?
   	 * 
   	 * @param rats
   	 * @param clusters2
   	 * @param clusterIndex
   	 * @return
   	 */
   	public static DenseCell getInitialConfigForMaxRatCluster(
   			Point[] rats, List<Cluster> clusters2, int clusterIndex, Point gate , double maxDistance) {
   		int maxRatsFound=Integer.MIN_VALUE;
   		int index=-1;
   		int i=0;
   		try{
   	
 
   		for(i=0; i< clusters2.get(clusterIndex).getPoints().size();i++){
   			Point currentPoint=clusters2.get(clusterIndex).getPoints().get(i);
   			double d= Utils.distance(gate,currentPoint);
   			if(d> maxDistance) //skip point if outside range
   				continue;
   			int ratCount= num_captured_rats( currentPoint, rats);
   			if(ratCount>maxRatsFound){
   				maxRatsFound=ratCount;
   				index=i;
   			}
   		}
   		if (index==-1){
   			return null;
   		}else{
   			return new DenseCell(clusterIndex, index, Utils.distance(new Point(clusters2.get(clusterIndex).getPoints().get(index).x,clusters2.get(clusterIndex).getPoints().get(index).y), gate));
   		}
   		}catch(Exception ex){
   			ex.printStackTrace();
   		}
		return null;
   	}

	public static boolean reachedDst(Point src, Point dst) {
		return Math.abs(src.x - dst.x) < 0.000001 &&
                Math.abs(src.y - dst.y) < 0.000001;
	}
    public static boolean isSparse(double ratsLength, double side, double density_threshold) {
        double density = ratsLength / (side * side);
        if (density <= density_threshold) {
            return true;
        }
        else {
            return false;
        }
    }
    static int  getRatsCountOnDstForClustered(Point[] rats, Point dst,int nearbyRatScanRadius2){
    	// create move towards specified destination
    	if(dst==null) return -1;
    	HashSet<Point> nearbyRats= new HashSet<Point>();
    	for (Point rat : rats) {
			if ((Utils.distance(dst, rat) < nearbyRatScanRadius2)) {
				nearbyRats.add(rat);
			}
		}
		return nearbyRats.size();
    }
    public static int getRatsCountOnDst(Point[] rats, Point dst, int nearbyRatScanRadius2) {
    /* Returns the number of rats at/near the given destination */
        int nearbyRats = 0;
        for (Point rat : rats) {
            if ((Utils.distance(dst, rat) < nearbyRatScanRadius2)) {
                nearbyRats++;
            }
        }
        return nearbyRats;
    }

     static int ratsOnPath(Point[] rats, List<ClusterPoint> currentPath,
			int nearbyRatScanRadius2, int posIndexInCurrentCluster2) {
		HashSet<Point> nearbyRats= new HashSet<Point>();
		for (int i = posIndexInCurrentCluster2; i < currentPath.size(); i++) {
			for (Point rat : rats) {
				ClusterPoint pathPoint = currentPath.get(i);
				if ((Utils.distance(new Point(pathPoint.x, pathPoint.y), rat) < nearbyRatScanRadius2)) {
					nearbyRats.add(rat);
					break;
				}
			}
		}
		return nearbyRats.size();
	}

    public static Point get_closest_rat(Point[] rats, Point[] box_boundaries, Point piper) {  
    /* Returns rat position which are within the box_boundary near the gate and closest to that piper */
        if (box_boundaries.length != 2)
            return null; 
        double closest_distance = Double.POSITIVE_INFINITY;
        Point closest_rat = null;
        for(Point rat : rats)
        {
            if((Math.min(box_boundaries[0].x, box_boundaries[1].x) <= rat.x) 
                && (rat.x <= (Math.max(box_boundaries[0].x, box_boundaries[1].x))) 
                && (Math.min(box_boundaries[0].y, box_boundaries[1].y) <= rat.y) 
                && (rat.y <= (Math.max(box_boundaries[0].y, box_boundaries[1].y)))) { 

                // if rat in box_boundary
                if ((Utils.distance(piper, rat) < closest_distance) && (closest_distance >= 10.0))
                {
                    closest_rat = rat;
                    closest_distance = Utils.distance(piper, rat);
                }

            }
        }
        return closest_rat;
    }

}
