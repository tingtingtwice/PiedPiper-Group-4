package pppp.g4;

import java.util.ArrayList;
import java.util.List;

public class Cluster implements Comparable<Cluster>{
	
	public List<ClusterPoint> points;
	public ClusterPoint centroid;
	public int id;
	
	//Creates a new Cluster
	public Cluster(int id) {
		this.id = id;
		this.points = new ArrayList<ClusterPoint>();
		this.centroid = null;
	}

	public List<ClusterPoint> getPoints() {
		return points;
	}
	
	public void addPoint(ClusterPoint point) {
		points.add(point);
	}

	public void setPoints(List<ClusterPoint> points) {
		this.points = points;
	}

	public ClusterPoint getCentroid() {
		return centroid;
	}

	public void setCentroid(ClusterPoint centroid) {
		this.centroid = centroid;
	}

	public int getId() {
		return id;
	}
	
	public void clear() {
		points.clear();
	}
	
	public void plotCluster() {
		System.out.println("[Cluster: " + id+"]");
		System.out.println("[Centroid: " + centroid + "]");
		System.out.println("[Points: \n");
		for(ClusterPoint p : points) {
			System.out.println(p);
		}
		System.out.println("]");
	}

	@Override
	public int compareTo(Cluster o) {
		return Integer.compare(this.points.size(), o.getPoints().size());
	}

}
