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

		if (currentStrategy != null) {
            if(rats.length <= 25 && !(currentStrategy instanceof pppp.g3.HunterSweep)){
                currentStrategy = new HunterSweep();
                currentStrategy.init(id, side, turns, pipers, rats);
            }
			return currentStrategy;
		}
		switch (pipers[id].length) {
			case 1: 
				currentStrategy = new OnePiperStrategy();
				break;
			/*case 2:
				currentStrategy = new TwoPiperStrategy();
				break;
			case 3: 
				currentStrategy = new ThreePiperStrategy();
				break;*/
			case 4:
				currentStrategy = new FourPiperStrategy(); 
				break;
			/*case 5:
				currentStrategy = new FivePiperStrategy();
				break;
			case 6: 
				currentStrategy = new SixPiperStrategy();
				break;
			case 7: 
				currentStrategy = new SevenPiperStrategy();
				break;
			case 8: 
				currentStrategy = new EightPiperStrategy();
				break;
			case 9:
				currentStrategy = new NinePiperStrategy();
				break;*/
			case 10: 
				currentStrategy = new TenPiperStrategy();
				break;
			default:
                if(rats.length >= 100)
				    currentStrategy = new AngularSweep();
                else
                    currentStrategy = new HunterSweep();
				break;
		}

        currentStrategy.init(id, side, turns, pipers, rats);
        return currentStrategy;

		/*
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
        }*/
	}
}