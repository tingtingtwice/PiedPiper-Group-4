package pppp.g3;

import pppp.g3.Strategy;
import pppp.g3.HH;
import pppp.g3.HunterSweep;
import pppp.g3.AngularSweep;

import pppp.sim.Point;
import pppp.sim.Move;

public class StrategyFactory{

	final public static double piperPivot = 1;
	final public static double ratPivot = 1;

	private Strategy currentStrategy = null;

	public Strategy getStrategy(int id, int side, long turns, Point[][] pipers, Point[] rats){
		if(currentStrategy == null){
            if(rats.length >= 100)
                currentStrategy = new AngularSweep();
            else
                currentStrategy = new HunterSweep();
			currentStrategy.init(id, side, turns, pipers, rats);
		}
        if(rats.length <= 25 && !(currentStrategy instanceof pppp.g3.HunterSweep)){
            currentStrategy = new HunterSweep();
            currentStrategy.init(id, side, turns, pipers, rats);
        }
		return currentStrategy;
	}

	private double getRatDensity(Point[] rats){
		return 0;
	}

	private double getPiperDensity(Point[][] pipers, Point[] rats){
		return 0;
	}

}