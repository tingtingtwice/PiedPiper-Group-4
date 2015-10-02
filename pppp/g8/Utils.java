package pppp.g8;

import java.util.Random;

import pppp.sim.Point;

public class Utils {

	public Utils() {
		// TODO Auto-generated constructor stub
	}

	
	
	/*
	 * Count the rats within the piper range
	 */
	public static int countRatsWithinPiper(Point[] rats, Point piper)
	{
		int count = 0;
		for (int i=0; i<rats.length; i++)
		{
			if (getDistance(rats[i], piper) < 10)
			{
				count++;
			}
		}
		return count;
	}
	
	private static double getDistance(Point a, Point b)
	{
		double x = a.x-b.x;
		double y = a.y-b.y;
		return Math.sqrt(x * x + y * y);
	}
	
	

}