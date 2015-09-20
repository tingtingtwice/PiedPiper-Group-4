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
    private final double ratAttractor = 20;
    private final double enemyPiperRepulsor = -10;
    private final double friendlyPiperRepulsor = -1;
    private final double friendlyInDanger = 30;
    private final double D = 0.25;
    private final double playThreshold = 3;
    private final double closeToGate = 8;

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
	//this.threatField = new double[maxMusicStrength][side*stepsPerUnit][side*stepsPerUnit];
	updateBoard(pipers,rats,new boolean[N][N]);
	for (int iter=0; iter<2*N; iter++) {
	    diffuse();
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
	boolean haveGateInfluence = false;
	int ratsRemaining = 0;
	for (int r=0; r<rats.length; r++) {
	    if (rats[r] != null) {
		ratsRemaining++;
	    }
	}
	updateBoard(pipers, rats, pipers_played);
	for (int p = 0 ; p != pipers[id].length ; ++p) {
	    Point src = pipers[id][p];
	    // return back
	    int numCapturedRats = capturedRats(src, rats);
	    boolean playMusic = false;
	    Point target;
	    if (alphaX * pipers[id][p].x + alphaY * pipers[id][p].y > side/2) {
		if (numCapturedRats > 0 && haveGateInfluence == false) {
		    target = new Point(behindGateX, behindGateY);
		    playMusic = true;
		    haveGateInfluence = true;
		} else {
		    target = new Point(gateX, gateY);
		    playMusic = false;
		}
	    } else if(numCapturedRats >= 1+ratsRemaining / (8*pipers[id].length) && ((distance(src, new Point(gateX, gateY)) > closeToGate) || haveGateInfluence == false) ) {
		if (distance(src, new Point(gateX, gateY)) > closeToGate) {
		    target = new Point(behindGateX, behindGateY);
		    playMusic = true;
		}
		else {
		    target = new Point(behindGateX, behindGateY);
		    playMusic = true;
		}
	    } else {
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
		target = new Point(bestX / stepsPerUnit - side/2 - 10 + (perturber.nextFloat() - 0.5), bestY / stepsPerUnit - side/2 - 10 + (perturber.nextFloat() - 0.5));
		if (distance(src, new Point(gateX, gateY)) < closeToGate) {
		    if (haveGateInfluence == true) {
			playMusic = false;
		    }
		    else {
			playMusic = true;
		    }
		}
		else {
		    playMusic = true;
		}
	    }
	    moves[p] = move(src, target, playMusic);
	}
    }
    
    private int capturedRats(Point src, Point[] rats) {
        int ratsCaptured = 0;
        for(Point rat: rats) {
            if(rat != null) {
                if (distance(src, rat) < 4) {
                    ratsCaptured++;
                }
            }
        }
        return ratsCaptured;
    }

    public double distance(Point p1, Point p2)
    {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y);
    }
}
