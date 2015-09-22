package pppp.g0;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    private int stepsPerUnit = 1;
    private int N;
    private int[] pos_index = null;
    private Point[][] pos = null;
    private Point[] random_pos = null;
    private Random gen = new Random();

    private int maxMusicStrength;
    private double[][][] rewardField;
    private double[][][] threatField;  // case num pipers together, board x, board y
    private double gateX;
    private double gateY;
    private double behindGateX;
    private double behindGateY;
    private double alphaX;
    private double alphaY;
    private Random perturber;

    // bunch of values to be learned later
    private final double ratAttractor = 40;
    private final double enemyPiperRepulsor = -100;
    private final double friendlyPiperRepulsor = -1;
    private final double friendlyInDanger = 30;
    private final double D = 0.25;
    private final double playThreshold = 3;
    private final double closeToGate = 9;

    private Map<Integer, Rat> rats;
    private Map<Integer, Piper> pipers;

    // create move towards specified destination
    private static Move move(Point src, Point dst, boolean play)
    {
	double dx = dst.x - src.x;
	double dy = dst.y - src.y;
	double length = Math.sqrt(dx * dx + dy * dy);
	double limit = play ? 0.1 : 0.5;
	if (length > limit) {
	    dx = (dx * limit) / length;
	    dy = (dy * limit) / length;
	}	
	return new Move(dx, dy, play);
    }

    // generate point after negating or swapping coordinates
    private static Point point(double x, double y,
			       boolean neg_y, boolean swap_xy)
    {
	if (neg_y) y = -y;
	return swap_xy ? new Point(y, x) : new Point(x, y);
    }

    // specify location that the player will alternate between

    private int getMusicStrength(Point loc, Point[] pipers) {
	double threshold = 10;
	int strength = 0;
	for (int p=0; p<pipers.length; p++) {
	    if (Math.sqrt((pipers[p].x - loc.x)*(pipers[p].x-loc.x) + (pipers[p].y - loc.y)*(pipers[p].y-loc.y)) < threshold) {
		strength += 1;
	    }
	}
	return strength;
    }

    private void refreshBoard() {
	this.rewardField = new double[maxMusicStrength][N][N];
    }

    private void diffuse() { 
	double[][][] newRewardField = new double[maxMusicStrength][N][N];
	//double[][][] newThreatField = new double[maxMusicStrength][side*stepsPerUnit][side*stepsPerUnit];
	for (int x=1; x<side*stepsPerUnit-1; x++) {
	    for (int y=1; y<side*stepsPerUnit-1; y++) {
		for (int d=0; d<maxMusicStrength; d++) {
		    newRewardField[d][x][y] = newRewardField[d][x][y] + D * (rewardField[d][x-1][y] + rewardField[d][x][y-1] + rewardField[d][x+1][y] + rewardField[d][x][y+1]);
		    //newThreatField[d][x][y] += D * (threatField[d][x-1][y] + threatField[d][x][y-1] + threatField[d][x+1][y] + threatField[d][x][y+1]);
		}
	    }
	}
	rewardField = newRewardField;
	//threatField = newThreatField;
    }
    
    public void init(int id, int side, long turns,
		     Point[][] pipers, Point[] rats)
    {
	this.id = id;
	this.side = side;
	this.maxMusicStrength = (int)Math.log(4*pipers[id].length);
	N = (side+21) * stepsPerUnit;
	perturber = new Random();
	double delta = 2.1;
	switch(id) {	   
	case 0:
	    gateX = 0;
	    gateY = side/2;
	    behindGateX = 0;
	    behindGateY = side/2 + delta;
	    alphaX = 0;
	    alphaY = 1;
	    break;
	case 1:
	    gateX = side/2;
	    gateY = 0;
	    behindGateX = side/2 + delta;
	    behindGateY = 0;
	    alphaY = 0;
	    alphaX = 1;
	    break;
	case 2:
	    gateX = 0;
	    gateY = -side/2;
	    behindGateX = 0;
	    behindGateY = -(side/2 + delta);
	    alphaX = 0;
	    alphaY = -1;
	    break;
	case 3:
	    gateX = -side/2;
	    gateY = 0;
	    behindGateX = -(side/2 + delta);
	    behindGateY = 0;
	    alphaX = -1;
	    alphaY = 0;
	    break;
	}
	
	this.rewardField = new double[maxMusicStrength][N][N];
        this.rats = new HashMap<Integer, Rat>();
        this.pipers = new HashMap<Integer, Piper>();
	//this.threatField = new double[maxMusicStrength][side*stepsPerUnit][side*stepsPerUnit];
	updateBoard(pipers,rats,new boolean[N][N]);
	createPipers(pipers, rats);
	updatePipersAndRats(rats, pipers, new boolean[4][pipers[0].length]);
	for (int iter=0; iter<N; iter++) {
	    diffuse();
	}
    }

    private void createPipers(Point[][] pipers, Point[] rats) {
        for(int i = 0; i < pipers[this.id].length; i++) {
            Strategy strategy;
            if(rats.length > 15) {
                strategy = new Strategy(StrategyType.sweep);
            } else {
                strategy = new Strategy(StrategyType.none);
            }
            this.pipers.put(i, new Piper(i, pipers[this.id][i], strategy));
        }
    }

    private void updatePipersAndRats(Point[] rats, Point[][] pipers, boolean[][] pipers_played) {
	for (int p=0; p<pipers[id].length; p++) {
            this.pipers.get(p).resetRats();
	    this.pipers.get(p).updateLocation(pipers[id][p]);
        }
        for(int i =0; i < rats.length; i++) {
            if(rats[i] == null) {
                if(this.rats.containsKey(i)) {
                    this.rats.remove(i);
                }
            } else {
                if(!this.rats.containsKey(i)) {
                    Rat rat = new Rat(i);
                    this.rats.put(i, rat);
                }
                Rat rat = this.rats.get(i);
                updateRat(rat, rats[i], pipers, pipers_played);
            }
        }
    }

    private void updateRat(Rat rat, Point location, Point[][] pipers, boolean[][] pipers_played) {
        rat.updateLocation(location);
        int maxPipersNearbySingleTeam = 0;
        int ratCapturedTeamId = -1;
        int piperId = -1;
        boolean conflict = false;
        int pipersNearby[] = new int[4];
        for(int i =0; i < pipers.length; i++) {
            pipersNearby[i] = 0;
            for(int j = 0; j < pipers[i].length; j++) {
                if(distance(pipers[i][j], location) < 10) {
                    if(i == this.id) {
                        if(this.pipers.get(j).playedMusic) {
                            piperId = j;
                            pipersNearby[i]++;
                        }
                    } else {
                        if(pipers_played[i][j]) {
                            pipersNearby[i]++;
                        }
                    }
                }
            }
            if(pipersNearby[i] > maxPipersNearbySingleTeam) {
                maxPipersNearbySingleTeam = pipersNearby[i];
                ratCapturedTeamId = i;
            }
        }
        Arrays.sort(pipersNearby);
        if(pipersNearby[0] == pipersNearby[1]) {
            conflict = true;
        }
        if((ratCapturedTeamId == -1) || conflict) {
            rat.captured = false;
            rat.hasEnemyCaptured = false;
            rat.piperId = -1;
        } else if(ratCapturedTeamId != this.id){
            rat.captured = true;
            rat.hasEnemyCaptured = true;
        } else {
            if(rat.captured && !rat.hasEnemyCaptured) {
                Piper prevPiper = this.pipers.get(rat.piperId);
                if(prevPiper.playedMusic &&
                        (distance(prevPiper.curLocation, location) < 10)
                ) {
                    prevPiper.addRat(rat.id);
                } else {
                    rat.piperId = piperId;
                    this.pipers.get(piperId).addRat(rat.id);
                }
            } else {
                rat.piperId = piperId;
                this.pipers.get(piperId).addRat(rat.id);
            }
            rat.captured = true;
            rat.hasEnemyCaptured = false;
        }
    }

    private boolean isCaptured(Point loc, Point[] pipers, boolean[] playing) {
	for (int p=0; p<pipers.length; p++) {
	    Double val = Math.hypot(loc.x - pipers[p].x, loc.y - pipers[p].y);
	    if (val < 10) {
		return true;
	    }
	}
	return false;
    }

    public void updateBoard(Point[][] pipers, Point[] rats, boolean[][] pipers_played) {
	refreshBoard();
	for (int r=0; r<rats.length; r++) {
	    if (rats[r] != null){
		for (int d=0; d<maxMusicStrength; d++) {
		    if (!isCaptured(rats[r], pipers[id], pipers_played[id])) {
			rewardField[d][(int) Math.round((rats[r].x+side/2+10)*stepsPerUnit)][ (int) Math.round((rats[r].y+side/2+10)*stepsPerUnit)] = ratAttractor;
		    }
		}
	    }
	}
	for (int d=0; d<maxMusicStrength; d++) {
	    rewardField[d][(int) (behindGateX + side/2 + 10) * stepsPerUnit][(int) (behindGateY * stepsPerUnit + side/2 + 10) * stepsPerUnit] = -100;
	}
	for (int t=0; t<4; t++) {
	    for (int p=0; p<pipers[t].length; p++) {
		if (pipers[t][p].x > -side/2 && pipers[t][p].x < side/2 && pipers[t][p].y > -side/2 && pipers[t][p].y < side/2) {
		    int strength = Math.min(getMusicStrength(pipers[t][p], pipers[t]),maxMusicStrength);
		    for (int d=0; d<strength; d++) {
			if (t != id) {
			    rewardField[d][(int) Math.round((pipers[t][p].x+side/2+10)*stepsPerUnit)][ (int) Math.round((pipers[t][p].y+side/2+10)*stepsPerUnit)] = enemyPiperRepulsor;
			}
			else {
			    rewardField[d][(int) Math.round((pipers[t][p].x+side/2+10)*stepsPerUnit)][ (int) Math.round((pipers[t][p].y+side/2+10)*stepsPerUnit)] = friendlyPiperRepulsor;
			}
		    }
		}
	    }
	}
	for (int iter=0; iter<N/2; iter++) {
	    diffuse();
	}
    }

    // return next locations on last argument
    public void play(Point[][] pipers, boolean[][] pipers_played,
		     Point[] rats, Move[] moves)
    {
        updatePipersAndRats(rats, pipers, pipers_played);
	boolean haveGateInfluence = false;
	int ratsRemaining = 0;
	for (int r=0; r<rats.length; r++) {
	    if (rats[r] != null) {
		ratsRemaining++;
	    }
	}
	updateBoard(pipers, rats, pipers_played);
	for (int p = 0 ; p != pipers[id].length ; ++p) {
        Piper piper = this.pipers.get(p);
        if(piper.strategy.type == StrategyType.sweep) {
            moves[p] = modifiedSweep(piper);
            continue;
        }
	    Point src = pipers[id][p];
	    // return back
	    int numCapturedRats = nearbyRats(src, rats);
	    //int numCapturedRats = this.pipers.get(p).getNumCapturedRats();
	    boolean playMusic = false;
	    Point target;

	    //piper is behind gate
	    if (alphaX * pipers[id][p].x + alphaY * pipers[id][p].y > side/2) {
		if (numCapturedRats > 0 && haveGateInfluence == false) {
		    target = new Point(behindGateX, behindGateY);
		    playMusic = true;
		    haveGateInfluence = true;
		} else {
		    target = new Point(gateX, gateY);
		    playMusic = false;
		}
	    }

	    //piper has captured enough rats
	    else if(numCapturedRats >= 1 + ratsRemaining / (8*pipers[id].length) && ((distance(src, new Point(gateX, gateY)) > closeToGate) || haveGateInfluence == false) ) {
		if (distance(src, new Point(gateX, gateY)) > closeToGate) {
		    target = new Point(behindGateX, behindGateY);
		    playMusic = true;
		}
		else {
		    target = new Point(behindGateX, behindGateY);
		    playMusic = true;
		}
	    }

	    //piper should capture more rats
	    else {
		int strength = Math.min(getMusicStrength(src, pipers[id]),maxMusicStrength-1);
		int x = (int)Math.round((src.x + side/2 + 10)*stepsPerUnit);
		int y = (int)Math.round((src.y + side/2 + 10)*stepsPerUnit);
		int bestX = -1;
		int bestY = -1;
		double steepestPotential = -1000;
		for (int i=Math.max(x-1,0); i<=Math.min(x+1,N-1); i++) {
		    for (int j=Math.max(y-1,0); j<=Math.min(y+1,N-1); j++){
			if (rewardField[strength][i][j] > steepestPotential) {
			    bestX = i;
			    bestY = j;
			    steepestPotential = rewardField[strength][i][j];
			}
		    }
		}
		target = new Point(bestX / stepsPerUnit - side/2 - 10 + (perturber.nextFloat() - 0.5) / 10, bestY / stepsPerUnit - side/2 - 10 + (perturber.nextFloat() - 0.5) / 10);
		//don't play music near gate if a piper is behind the gate trying to pull rats in
		if (distance(src, new Point(gateX, gateY)) < closeToGate) {
		    if (haveGateInfluence == true) {
			playMusic = false;
		    }
		}
		else {
		    // if already playing music, keep playing unless lost all rats
		    if (this.pipers.get(p).playedMusic == true) {
			/*			if (numCapturedRats > 0) {			    
			    playMusic = true;
			}
			else {
			    System.out.println("lost rats");
			    playMusic = false;
			    }*/
			playMusic = true;
		    }
		    else {
			// if not already playing, play music when approaching local optima
			if (this.pipers.get(p).getAbsMovement() < 0.65 && nearbyRats(src, rats) > 0) {
			    playMusic = true;
			}
			else {
			    playMusic = false;
			}
		    }
		}
	    }
	    //	    System.out.println(this.pipers.get(p).getAbsMovement());
	    this.pipers.get(p).updateMusic(playMusic);
	    moves[p] = move(src, target, playMusic);
	}
        for(int i = 0; i < moves.length; i++) {
            this.pipers.get(i).playedMusic = moves[i].play;
        }
    }

    private Move modifiedSweep(Piper piper) {
        boolean playMusic = false;
        Point target = null;
        if(piper.strategy.type != StrategyType.sweep || !piper.strategy.isPropertySet("step")) {
            piper.strategy = new Strategy(StrategyType.sweep);
            piper.strategy.setProperty("step", 1);
            target = new Point(gateX, gateY);
            playMusic = false;
        } else if(distance(piper.curLocation, (Point) piper.strategy.getProperty("location")) != 0) {
            target = (Point) piper.strategy.getProperty("location");
            playMusic = piper.playedMusic;
        } else {
            Integer step = (Integer) piper.strategy.getProperty("step");
            switch (step) {
                case 1:
                    int p1 = (side/2) - 7; //43
                    int p2 = (side/2)/5 + 7;  //17
                    switch (this.id) {
                        case 0:
                            switch (piper.id) {
                                case 0:
                                    target = new Point(-p1, p1);
                                    break;
                                case 1:
                                    target = new Point(-p1, p2);
                                    break;
                                case 2:
                                    target = new Point(p1, p2);
                                    break;
                                case 3:
                                    target = new Point(p1, p1);
                                    break;
                            }
                            break;
                        case 1:
                            switch (piper.id) {
                                case 0:
                                    target = new Point(p1, -p1);
                                    break;
                                case 1:
                                    target = new Point(p2, -p1);
                                    break;
                                case 2:
                                    target = new Point(p2, p1);
                                    break;
                                case 3:
                                    target = new Point(p1, p1);
                                    break;
                            }
                            break;
                        case 2:
                            switch (piper.id) {
                                case 0:
                                    target = new Point(-p1, -p1);
                                    break;
                                case 1:
                                    target = new Point(-p1, -p2);
                                    break;
                                case 2:
                                    target = new Point(p1, -p2);
                                    break;
                                case 3:
                                    target = new Point(p1, -p1);
                                    break;
                            }
                            break;
                        case 3:
                            switch (piper.id) {
                                case 0:
                                    target = new Point(-p1, p1);
                                    break;
                                case 1:
                                    target = new Point(-p2, p1);
                                    break;
                                case 2:
                                    target = new Point(-p2, -p1);
                                    break;
                                case 3:
                                    target = new Point(-p1, -p1);
                                    break;
                            }
                            break;
                    }
                    piper.strategy.setProperty("step", 2);
                    break;
                case 2:
                    playMusic = true;
                    piper.strategy.setProperty("step", 3);
                    target = new Point(alphaX * side/4, alphaY * side/4);
                    break;
                case 3:
                    playMusic = true;
                    piper.strategy.setProperty("step", 4);
                    target = new Point(alphaX * (side/2 - 5), alphaY * (side/2 - 5));
                    break;
                case 4:
                    piper.strategy = new Strategy(StrategyType.none);
                    target = new Point(gateX, gateY);
            }
        }
        piper.strategy.setProperty("location", target);
        return move(piper.curLocation, target, playMusic);
    }
    
    private int nearbyRats(Point src, Point[] rats) {
        int ratsNearby = 0;
        for(Point rat: rats) {
            if(rat != null) {
                if (distance(src, rat) < 4) {
                    ratsNearby++;
                }
            }
        }
        return ratsNearby;
    }

    public double distance(Point p1, Point p2)
    {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y);
    }
}
