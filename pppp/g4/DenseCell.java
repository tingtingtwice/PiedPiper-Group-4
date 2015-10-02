package pppp.g4;

public class DenseCell {
	int clusterIndex;
	int pointIndexInCluster;
	double distanceFromGate;
	
	public DenseCell(int clusterIndex, int pointIndexInCluster,
			double d) {
		super();
		this.clusterIndex = clusterIndex;
		this.pointIndexInCluster = pointIndexInCluster;
		this.distanceFromGate = d;
	}
 

}
