package pppp.g4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pppp.sim.Point;

public class ClusterPoint extends Point implements Comparable<ClusterPoint>{

	public ClusterPoint(double x, double y) {
		super(x, y);
	}
	
	public ClusterPoint(double x, double y,  int cluster_number) {
		super(x, y);
		this.cluster_number= cluster_number;
	}
	
	 private int cluster_number = 0;
 
	    public double getX()  {
	        return this.x;
	    }
	    
 
	    
	    public double getY() {
	        return this.y;
	    }
	    
	    public void setCluster(int n) {
	        this.cluster_number = n;
	    }
	    
	    public int getCluster() {
	        return this.cluster_number;
	    }
	    
	    //Calculates the distance between two points.
	    protected static double distance(ClusterPoint p, ClusterPoint centroid) {
	        return Math.sqrt(Math.pow((centroid.getY() - p.getY()), 2) + Math.pow((centroid.getX() - p.getX()), 2));
	    }
	    
	    //Creates random point
	    protected static ClusterPoint createRandomPoint(int min, int max) {
	    	Random r = new Random();
	    	double x = min + (max - min) * r.nextDouble();
	    	double y = min + (max - min) * r.nextDouble();
	    	return new ClusterPoint(x,y);
	    }
	    
	    //Creates random point
	    protected static ClusterPoint getMidPoint(int min, int max , int points) {
	    	Random r = new Random();
	    	double x = min + (max - min) * r.nextDouble();
	    	double y = min + (max - min) * r.nextDouble();
	    	return new ClusterPoint(x,y);
	    }
	    
	    protected static List<ClusterPoint> createRandomPoints(int min, int max, int number) {
	    	List<ClusterPoint> points = new ArrayList<ClusterPoint>(number);
	    	for(int i = 0; i < number; i++) {
	    		points.add(createRandomPoint(min,max));
	    	}
	    	return points;
	    }

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "ClusterPoint [cluster_number=" + cluster_number + ", ("
					+ x + "," + y + ")";
		}

		public static List<ClusterPoint> pointToClusterPoint(Point[] rats) {
			List<ClusterPoint> cpList=new ArrayList<ClusterPoint>(); 
			for(Point r :rats){
				cpList.add(new ClusterPoint(r.x,r.y));
			}
			return cpList;
		}

		@Override
		public int compareTo(ClusterPoint p2) {
			return Double.compare(ClusterPoint.distance(KMeans.gatePoint,this),
					ClusterPoint.distance(KMeans.gatePoint,p2));
		}
 
}
