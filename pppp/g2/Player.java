package pppp.g2;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    private double stepsPerUnit = 0.5;
    private int N;
    private int[] pos_index = null;
    private Point[][] pos = null;
    private Point[] random_pos = null;
    private Random gen = new Random();
    boolean neg_y;
    boolean swap;

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
    private final double baseRatAttractor = 40;
    private int totalRats;
    private double ratAttractor = baseRatAttractor;
    private final double enemyPiperRepulsor = 0;
    private final double friendlyPiperRepulsor = -1;
    private final double friendlyInDanger = 30;
    private final double D = 0.2;
    private final double playThreshold = 3;
    private final double closeToGate = 25;

    // modified sweep strategy variables
    private int sweepNumPipersSide1;
    private int sweepNumPipersSide2;
    private int sweepNumPipersSide3;
    private int sweepNumPipersSide4;
    private int sweepPoint1;
    private int sweepPoint2;

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

    private int getMusicStrength(Point loc, Point[] pipers, double threshold) {
	int strength = 0;
	for (int p=0; p<pipers.length; p++) {
	    if (Math.sqrt((pipers[p].x - loc.x)*(pipers[p].x-loc.x) + (pipers[p].y - loc.y)*(pipers[p].y-loc.y)) < threshold) {
		strength += 1;
	    }
	}
	return strength-1;
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
        this.neg_y = id == 2 || id == 3;
        this.swap  = id == 1 || id == 3;
	this.id = id;
	this.side = side;
	this.maxMusicStrength = (int)Math.log(4*pipers[id].length);
	this.totalRats = rats.length;
	N = (int) ((side+20) * stepsPerUnit + 1);
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

        this.sweepNumPipersSide1 = this.sweepNumPipersSide2 = this.sweepNumPipersSide3 = this.sweepNumPipersSide4 =
                pipers[0].length/4;
        if(pipers[0].length%4 > 2) {
            this.sweepNumPipersSide1++; this.sweepNumPipersSide2++; this.sweepNumPipersSide3++;
        } else if(pipers[0].length%4 > 1) {
            this.sweepNumPipersSide2++; this.sweepNumPipersSide3++;
        } else if(pipers[0].length%4 > 0) {
            this.sweepNumPipersSide2++;
        }
        int p1 = (side/2) - 7;
        int p2 = (side/2)/5 + 7;
        this.sweepPoint1 = Math.max(p1 ,p2);
        this.sweepPoint2 = Math.min(p1 ,p2);

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
            if(rats.length > 25 && pipers[this.id].length >3) {
                strategy = new Strategy(StrategyType.sweep);
            } else if (rats.length > 4){
                strategy = new Strategy(StrategyType.diffusion);
            }
	    else {
		strategy = new Strategy(StrategyType.greedy);
	    }
            this.pipers.put(i, new Piper(i, pipers[this.id][i], strategy));
        }
    }

    private void updateStrategy(Point[][] pipers, Point[] rats) {
	double ratClusters = rats.length;
	for (int r=0; r<rats.length; r++) {
	    double nearbyRats = 0;	    
	    for (int s=0; s<rats.length; s++) {
		if (r != s) {
		    if (distance(rats[r], rats[s]) < 10) {
			nearbyRats += 1;
		    }
		}
	    }
	    ratClusters -= nearbyRats / (nearbyRats + 1);	    
	}
	//System.out.println(ratClusters);
        for(int i = 0; i < pipers[this.id].length; i++) {
	    if (ratClusters > 4) {
                this.pipers.get(i).strategy = new Strategy(StrategyType.diffusion);
            }
            else {
                this.pipers.get(i).strategy = new Strategy(StrategyType.greedy);
            }
        }	
    }

    private void updatePipersAndRats(Point[] rats, Point[][] pipers, boolean[][] pipers_played) {
	for (int p=0; p<pipers[id].length; p++) {
            this.pipers.get(p).resetRats();
	    this.pipers.get(p).updateLocation(pipers[id][p]);
        }
        /*for(int i =0; i < rats.length; i++) {
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
	    }*/
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
		    if (!isCaptured(rats[r], pipers[id], pipers_played[id]) && distance(rats[r], new Point(gateX, gateY)) > 20) {
			rewardField[d][(int) Math.round((rats[r].x+side/2+10)*stepsPerUnit)][ (int) Math.round((rats[r].y+side/2+10)*stepsPerUnit)] = ratAttractor;
		    }
		}
	    }
	}
	for (int d=0; d<maxMusicStrength; d++) {
	    rewardField[d][(int) ((behindGateX + side/2 + 10) * stepsPerUnit)][(int) ((behindGateY * stepsPerUnit + side/2 + 10) * stepsPerUnit)] = -100;
	}
	for (int t=0; t<4; t++) {
	    for (int p=0; p<pipers[t].length; p++) {
		if (pipers[t][p].x > -side/2 && pipers[t][p].x < side/2 && pipers[t][p].y > -side/2 && pipers[t][p].y < side/2) {
		    int strength = Math.min(getMusicStrength(pipers[t][p], pipers[t],10),maxMusicStrength);
		    if (t != id) {
			for (int d=0; d<Math.min(strength, maxMusicStrength); d++) {			
			    rewardField[d][(int) Math.round((pipers[t][p].x+side/2+10)*stepsPerUnit)][ (int) Math.round((pipers[t][p].y+side/2+10)*stepsPerUnit)] = enemyPiperRepulsor;
			}
		    }
		    else {
			for (int d=0; d<maxMusicStrength; d++) {
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
	if (this.pipers.get(0).strategy.type != StrategyType.sweep) {
	    updateStrategy(pipers,rats);
	}
	//System.out.println(this.pipers.get(0).strategy.type);
	int numEnemiesNearGate = 0;
	int numFriendliesNearGate = 0;
	int goalie = -1;
	double goalieD = 0;
	for (int p=0; p<pipers[id].length; p++) {
	    double d = distance(new Point(gateX, gateY), pipers[id][p]);
	    if (d < goalieD || goalie == -1) {
		goalieD = d;
		goalie = p;
	    }
	}	
	for (int t=0; t<4; t++) {
	    if (t != id) {
		for (int p=0; p<pipers[t].length; p++) {
		    if (distance(pipers[t][p], new Point(gateX, gateY)) < closeToGate) {
			numEnemiesNearGate++;
		    }
		}
	    }
	}	
        updatePipersAndRats(rats, pipers, pipers_played);
	boolean haveGateInfluence = false;
	ratAttractor = baseRatAttractor * Math.pow((double) totalRats / (double) rats.length,3);
	updateBoard(pipers, rats, pipers_played);
        Boolean allPipersWithinDistance = null;
	for (int p = 0 ; p != pipers[id].length ; ++p) {
        Piper piper = this.pipers.get(p);
        if(piper.strategy.type == StrategyType.sweep) {
            moves[p] = modifiedSweep(piper, rats, allPipersWithinDistance);
            continue;
        }
	    Point src = pipers[id][p];
	    // return back
	    int numCapturedRats = nearbyRats(src, rats, null);
	    //int numCapturedRats = this.pipers.get(p).getNumCapturedRats();

	    boolean playMusic = false;
	    Point target;

	    //piper is behind gate
	    if (alphaX * pipers[id][p].x + alphaY * pipers[id][p].y > side/2) {
		if (numCapturedRats > 0 && haveGateInfluence == false && distance(pipers[id][p], new Point(behindGateX, behindGateY)) < 2.2) {
		    target = new Point(behindGateX, behindGateY);
		    playMusic = true;
		    numFriendliesNearGate++;
		    if (numFriendliesNearGate > numEnemiesNearGate) {
			haveGateInfluence = true;
		    }
		} else {
		    target = new Point(gateX, gateY);
		    playMusic = false;
		}
	    }

	    //piper has captured enough rats
	    else if(numCapturedRats >= 1 + rats.length / (8*pipers[id].length) && ((distance(src, new Point(gateX, gateY)) > closeToGate) || haveGateInfluence == false) ) {
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

		if (piper.strategy.type == StrategyType.diffusion) {
		    int strength = Math.min(getMusicStrength(src, pipers[id],25),maxMusicStrength-1);
		    int x = (int)Math.round((src.x + side/2 + 10)*stepsPerUnit);
		    int y = (int)Math.round((src.y + side/2 + 10)*stepsPerUnit);
		    double bestX = -1;
		    double bestY = -1;
		    double steepestPotential = -1000;
		    for (int i=Math.max(x-3,0); i<=Math.min(x+3,N-1); i++) {
			for (int j=Math.max(y-3,0); j<=Math.min(y+3,N-1); j++){
			    if (rewardField[strength][i][j] > steepestPotential) {
				bestX = i;
				bestY = j;
				steepestPotential = rewardField[strength][i][j];
			    }
			}
		    }
		    target = new Point(bestX / stepsPerUnit - side/2 - 10 + (perturber.nextFloat() - 0.5) / 10, bestY / stepsPerUnit - side/2 - 10 + (perturber.nextFloat() - 0.5) / 10);
		}
		else if (piper.strategy.type == StrategyType.greedy) {
		    int closestRat = -1;
		    double closestDist = 0;
		    for (int r=0; r<rats.length; r++) {
			double d = distance(new Point(gateX, gateY), rats[r]);
			if (d < closestDist || closestRat == -1) {
			    closestDist = d;
			    closestRat = r;
			}
		    }
		    target = rats[closestRat];
		}
		else {
		    target = new Point(0,0);
		}
		//don't play music near gate if a piper is behind the gate trying to pull rats in
		if (distance(src, pipers[id][goalie]) < 15) {
		    if (haveGateInfluence == true) {
			playMusic = false;
		    }
		}
		else {
		    // if already playing music, keep playing unless lost all rats
		    if (this.pipers.get(p).playedMusic == true) {
			if (numCapturedRats > 0) {			    
			    playMusic = true;
			}
			else {
			    playMusic = false;
			}
		    }
		    else {
			// if not already playing, play music when approaching local optima			
			if (this.pipers.get(p).getAbsMovement() < 0.45 && nearbyRats(src, rats, null) > 0) {
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

    private Move modifiedSweep(Piper piper, Point[] rats, Boolean allPipersWithinDistance) {
        boolean playMusic = false;
        Point target = null;
        if (allPipersWithinDistance == null) {
            allPipersWithinDistance = allPipersWithinDistance(10);
        }
        if(piper.strategy.type != StrategyType.sweep || !piper.strategy.isPropertySet("step")) {
            piper.strategy = new Strategy(StrategyType.sweep);
            piper.strategy.setProperty("step", 1);
            target = new Point(gateX, gateY);
            playMusic = false;
        } else if(4 == (Integer) piper.strategy.getProperty("step") && allPipersWithinDistance) {
            piper.strategy.setProperty("step", 5);
            playMusic = true;
            target = new Point(behindGateX, behindGateY);
        }
        if(target == null) {
            if (distance(piper.curLocation, (Point) piper.strategy.getProperty("location")) != 0) {
                target = (Point) piper.strategy.getProperty("location");
                playMusic = piper.playedMusic;
            } else {
                Integer step = (Integer) piper.strategy.getProperty("step");
                switch (step) {
                    case 1:
                        int delta;
                        if (piper.id < this.sweepNumPipersSide1) {
                            // side 1
                            delta = (piper.id) * (this.sweepPoint1 - this.sweepPoint2) / this.sweepNumPipersSide1;
                            target = point(this.sweepPoint1, this.sweepPoint1 - delta, this.neg_y, this.swap);
                        } else if (piper.id < this.sweepNumPipersSide1 + this.sweepNumPipersSide2) {
                            // side 2
                            delta = (piper.id - this.sweepNumPipersSide1) * (this.sweepPoint1) / this.sweepNumPipersSide2;
                            target = point(this.sweepPoint1 - delta, this.sweepPoint2, this.neg_y, this.swap);
                        } else if (piper.id < this.sweepNumPipersSide1 + this.sweepNumPipersSide2 + this.sweepNumPipersSide3) {
                            // side 3
                            delta = (this.sweepNumPipersSide3 + this.sweepNumPipersSide2 + this.sweepNumPipersSide1 - piper.id - 1) * (this.sweepPoint1) / this.sweepNumPipersSide2;
                            target = point(-this.sweepPoint1 + delta, this.sweepPoint2, this.neg_y, this.swap);
                        } else {
                            // side 4
                            delta = (this.sweepNumPipersSide4 + this.sweepNumPipersSide3 + this.sweepNumPipersSide2 + this.sweepNumPipersSide1 - piper.id - 1) * (this.sweepPoint1 - this.sweepPoint2) / this.sweepNumPipersSide4;
                            target = point(-this.sweepPoint1, this.sweepPoint1 - delta, this.neg_y, this.swap);
                        }
                        piper.strategy.setProperty("step", 2);
                        break;
                    case 2:
                        // in middle, should make a check that only if all pipers are in a certain distance with each other, move to step 4
                        playMusic = true;
                        piper.strategy.setProperty("step", 4);
                        target = new Point(alphaX * 3 * side / 10, alphaY * 3 * side / 10);
                        break;
                    case 3:
                        // in front of gate
                        playMusic = true;
                        piper.strategy.setProperty("step", 4);
                        target = new Point(alphaX * (side / 2 - 5), alphaY * (side / 2 - 5));
                        break;
                    case 4:
                        playMusic = true;
                        if(allPipersWithinDistance) {
                            piper.strategy.setProperty("step", 5);
                            target = new Point(gateX + 2*Math.random(), gateY + 2*Math.random());
                        } else {
                            target = (Point) piper.strategy.getProperty("location");
                        }
                        break;
                    case 5:
			//                        if (nearbyRats(piper.curLocation, rats, 10) == 0) {
			piper.strategy = new Strategy(StrategyType.diffusion);
			    //}
                        playMusic = true;
                        target = new Point(behindGateX, behindGateY);
                }
            }
        }
        piper.strategy.setProperty("location", target);
        return move(piper.curLocation, target, playMusic);
    }

    private boolean allPipersWithinDistance(int distance) {
        for(Piper piper1: this.pipers.values()) {
            for(Piper piper2: this.pipers.values()) {
                if(distance(piper1.curLocation, piper2.curLocation) > distance) {
                    return false;
                }
            }
        }
        return true;
    }

    // pass distanceThreshold as null to use a default threshold value
    private int nearbyRats(Point src, Point[] rats, Integer distanceThreshold) {
        int ratsNearby = 0;
        double threshold = 9.5;
        if(distanceThreshold != null) {
            threshold = distanceThreshold;
        }
        for(Point rat: rats) {
            if(rat != null) {
                if (distance(src, rat) < threshold) {
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
