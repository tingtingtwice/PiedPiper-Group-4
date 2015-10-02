package pppp.g4;

/* 
 * KMeans.java ; Cluster.java ; Point.java
 *
 * Solution implemented by DataOnFocus
 * www.dataonfocus.com
 * 2015
 *
*/
 

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pppp.sim.Point;

 

public class KMeans {

	//Number of Clusters. This metric should be related to the number of points
    private int NUM_CLUSTERS = 3;    
    //Number of Points
    private int NUM_POINTS = 15;
    //Min and Max X and Y
    private static int MIN_COORDINATE = 0;
    private static int MAX_COORDINATE = 10;
    
    private List<ClusterPoint> points;
    private List<Cluster> clusters;
    public static ClusterPoint gatePoint;
    public KMeans(int ClusterCount, int MIN_COORDINATE, int MAX_COORDINATE, ClusterPoint gate) {
        this.NUM_CLUSTERS = ClusterCount;    
       //Min and Max X and Y
        KMeans.MIN_COORDINATE = MIN_COORDINATE;
        KMeans.MAX_COORDINATE = MAX_COORDINATE;
    	this.points = new ArrayList<ClusterPoint>();
    	this.clusters = new ArrayList<Cluster>(); 
    	KMeans.gatePoint=gate; 
    }
    
    public KMeans() {
    	this.points = new ArrayList<ClusterPoint>();
    	this.clusters = new ArrayList<Cluster>();    	
    }
    
    public static void main(String[] args) {
    	
    	//KMeans kmeans = new KMeans();
    	int clusterCount = 3;
    	int minX=-50;
    	int maxX=50;
    	KMeans kmeans = new KMeans(clusterCount, minX, maxX,new ClusterPoint(0,50));
    	List<Point> rats=new ArrayList<Point>();
    	rats.add(new Point(5,6));
    	rats.add(new Point(15,27));
    	rats.add(new Point(-10,-50));
    	rats.add(new Point(50,-27));
    	rats.add(new Point(22,33));
    	rats.add(new Point(-33,44));
    	
    	//Set Random Centroids
    	List<ClusterPoint> centerPoints=new ArrayList<ClusterPoint>();
    	centerPoints.add(new ClusterPoint(0,-35));
    	centerPoints.add(new ClusterPoint(35,0));
    	centerPoints.add(new ClusterPoint(-35,0));
    	
    	kmeans.init(rats.toArray(new Point[rats.size()]), centerPoints);
    	kmeans.calculate();
    	kmeans.printClusterSizes(); 
    	kmeans.sortClustersAndUpdateShortestPathsInCluster();
    	kmeans.plotClusters();
    	//kmeans.calculatePiperSplits();
    }
    
    
    List<Cluster> sortClustersAndUpdateShortestPathsInCluster() {
    	//Collections.sort(clusters, Collections.reverseOrder());
    	for (int i = 0; i < NUM_CLUSTERS; i++) {
    		Cluster c = clusters.get(i);
    		Collections.sort(c.getPoints(), Collections.reverseOrder());
    	}
    	return clusters;
	}

	void printClusterSizes(){
    	for (int i = 0; i < NUM_CLUSTERS; i++) {
    		Cluster c = clusters.get(i);
    		System.out.println("Cluster : " + i + ", Size: "+ c.getPoints().size() );
    	}
    }
    //Initializes the process
    public void init() {
    	//Create Points
    	points = ClusterPoint.createRandomPoints(MIN_COORDINATE,MAX_COORDINATE,NUM_POINTS);
    	
    	//Create Clusters
    	//Set Random Centroids
    	for (int i = 0; i < NUM_CLUSTERS; i++) {
    		Cluster cluster = new Cluster(i);
    		ClusterPoint centroid = ClusterPoint.createRandomPoint(MIN_COORDINATE,MAX_COORDINATE);
    		cluster.setCentroid(centroid);
    		clusters.add(cluster);
    	}
    	
    	//Print Initial state
    	plotClusters();
    }
    
    //Initializes the process
    public void init(Point[] rats, List<ClusterPoint> centerPoints) {
    	//convert Point to ClusterPoint 
    	//TODO: 
    	points = ClusterPoint.pointToClusterPoint(rats);
    	//Create Points
    	//points = ClusterPoint.createRandomPoints(MIN_COORDINATE,MAX_COORDINATE,NUM_POINTS);
    	
    	//Create Clusters

    	for (int i = 0; i < NUM_CLUSTERS; i++) {
    		Cluster cluster = new Cluster(i);
    		ClusterPoint centroid = centerPoints.get(i);//ClusterPoint.createRandomPoint(MIN_COORDINATE,MAX_COORDINATE);
    		cluster.setCentroid(centroid);
    		clusters.add(cluster);
    	}
    	
    	//Print Initial state
    	plotClusters();
    }
    
	void plotClusters() {
    	for (int i = 0; i < NUM_CLUSTERS; i++) {
    		Cluster c = clusters.get(i);
    		c.plotCluster();
    	}
    }
    
	//The process to calculate the K Means, with iterating method.
    public void calculate() {
        boolean finish = false;
        int iteration = 0;
        
        // Add in new data, one at a time, recalculating centroids with each new one. 
        while(!finish) {
        	//Clear cluster state
        	clearClusters();
        	
        	List<ClusterPoint> lastCentroids = getCentroids();
        	
        	//Assign points to the closer cluster
        	assignCluster();
            
            //Calculate new centroids.
        	calculateCentroids();
        	
        	iteration++;
        	
        	List<ClusterPoint> currentCentroids = getCentroids();
        	
        	//Calculates total distance between new and old Centroids
        	double distance = 0;
        	for(int i = 0; i < lastCentroids.size(); i++) {
        		distance += ClusterPoint.distance(lastCentroids.get(i),currentCentroids.get(i));
        	}
        	System.out.println("#################");
           	System.out.println("#################");
        	System.out.println("Iteration: " + iteration);
        	System.out.println("Centroid distances: " + distance);
           	System.out.println("#################");
        	plotClusters();
        	        	
        	if(distance == 0) {
        		finish = true;
        	}
        }
    }
    
    private void clearClusters() {
    	for(Cluster cluster : clusters) {
    		cluster.clear();
    	}
    }
    
    private List<ClusterPoint> getCentroids() {
    	List<ClusterPoint> centroids = new ArrayList<ClusterPoint>(NUM_CLUSTERS);
    	for(Cluster cluster : clusters) {
    		ClusterPoint aux = cluster.getCentroid();
    		ClusterPoint point = new ClusterPoint(aux.getX(),aux.getY());
    		centroids.add(point);
    	}
    	return centroids;
    }
    
    private void assignCluster() {
        double max = Double.MAX_VALUE;
        double min = max; 
        int cluster = 0;                 
        double distance = 0.0; 
        
        for(ClusterPoint point : points) {
        	min = max;
            for(int i = 0; i < NUM_CLUSTERS; i++) {
            	Cluster c = clusters.get(i);
                distance = ClusterPoint.distance(point, c.getCentroid());
                if(distance < min){
                    min = distance;
                    cluster = i;
                }
            }
            point.setCluster(cluster);
            clusters.get(cluster).addPoint(point);
        }
    }
    
    private void calculateCentroids() {
        for(Cluster cluster : clusters) {
            double sumX = 0;
            double sumY = 0;
            List<ClusterPoint> list = cluster.getPoints();
            int n_points = list.size();
            
            for(ClusterPoint point : list) {
            	sumX += point.getX();
                sumY += point.getY();
            }
            
            ClusterPoint centroid = cluster.getCentroid();
            if(n_points < 0) {
            	double newX = sumX / n_points;
            	double newY = sumY / n_points;
            	cluster.setCentroid(new ClusterPoint(newX, newY,   centroid.getCluster()));
            }
        }
    }

	public static int findMaxClusterIndex(List<Cluster> clusters2) {
		int maxSizeOfClusterFound=-1;
		int indexOfMaxCluster=-1;
		for(int i=0;i<clusters2.size();i++){
			Cluster c= clusters2.get(i);
			if(maxSizeOfClusterFound < c.getPoints().size()){
				maxSizeOfClusterFound=c.getPoints().size();
				indexOfMaxCluster=i;
			}
		}
		return indexOfMaxCluster;
	}

	public static int findMaxClusterIndexOrig(List<Cluster> clusters2) {
		int maxSizeOfClusterFound=-1;
		int indexOfMaxCluster=-1;
		for(int i=0;i<clusters2.size();i++){
			Cluster c= clusters2.get(i);
			if(maxSizeOfClusterFound < c.getPoints().size()){
				maxSizeOfClusterFound=c.getPoints().size();
				indexOfMaxCluster=i;
			}
		}
		return indexOfMaxCluster;
	}
	
	public static int resetIndex(List<Cluster> clusters2, int whichCurrentCluster,double maxDistanceInCurrentCluster) {
		int i=0; 
		try{
			while(i < clusters2.get(whichCurrentCluster).getPoints().size() && 
					ClusterPoint.distance(KMeans.gatePoint, clusters2.get(whichCurrentCluster).getPoints().get(i)) > maxDistanceInCurrentCluster
					){
				i=i+1;
			}
		   }catch(Exception e ){
            	e.printStackTrace();
            }
		return i;
	}

    
  
}