package pppp.g4;

import pppp.sim.Point;

public class Cell implements Comparable<Cell> {

	public static int counter = 0;
	public int id;
	public Point center = null;
	public float side = 20;
	public int weight = 0;

	public Cell(Point center) {
		this.id = counter++;
		this.center = center;
	}

	public void display_cell() {
		System.out.println("ID: " + id);
		System.out.println("Center: " + center.x + ", " + center.y);
		System.out.println("Weight: " + weight);
	}

	public int compareTo(Cell other) {
		return new Integer(this.weight).compareTo(new Integer(other.weight));
      }
}