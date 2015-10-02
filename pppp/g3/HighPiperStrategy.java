package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

import pppp.g3.Strategy;


import java.lang.Double;
import java.lang.System;

public class HighPiperStrategy implements pppp.g3.Strategy {

    public static final int PIPER_RADIUS = 10;
    public static final int PIPER_RUN_SPEED = 5;
    public static final int PIPER_WALK_SPEED = 1;

    public static final int POINT_DENSITY = 20;

    private int id = -1;
	private int side = 0;
    private long turns = 0;
    private int numberOfPipers = 0;
	private int[] piperState = null;
	private Point[][] piperStateMachine = null;
	private double door = 0.0;
	private boolean neg_y;
	private boolean swap;

    private int[][] ratNumbers;

    private Point gateEntrance = null;
    private Point insideGate = null;
    private Point outsideGate = null;

	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats){
		// storing variables
        this.id = id;
		this.side = side;
        this.turns = turns;

        ratNumbers = new int[POINT_DENSITY + 1][POINT_DENSITY + 1];

        // variables to rotate map
        neg_y = id == 2 || id == 3;
		swap  = id == 1 || id == 3;

        // create gate positions
        gateEntrance = Movement.makePoint(door, side * 0.5, neg_y, swap);
        insideGate = Movement.makePoint(door, side * 0.5 + 2.5, neg_y, swap);
        outsideGate = Movement.makePoint(door, side * 0.5 - 5, neg_y, swap);

        // create the state machines for the pipers
		numberOfPipers = pipers[id].length;
		piperStateMachine = new Point [numberOfPipers][];
		piperState = new int [numberOfPipers];

		for (int p = 0 ; p != numberOfPipers; ++p) {
			piperStateMachine[p] = generateStateMachine(p);
            piperState[p] = 0;
            if(rats.length / (double) (side * side) < StrategyFactory.RAT_DENSITY_THRESHOLD) {
                piperState[p] = 7;
            }
        }
	}

	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves) {
        Point src, dst;
        int state;

        Point ratOnWayBack = null;

        fillRatDensities(rats);

        for(int p = 0; p < numberOfPipers; p++){
            state = piperState[p];
            boolean play = false;
            src = pipers[id][p];
            dst = piperStateMachine[p][state];

            //System.err.println(state);

            if(state == 0){
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 1;
                    dst = piperStateMachine[p][state];
                }
            }

            else if (state == 1) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 3;
                    dst = src;
                    play = true;
                }
            }

            else if (state == 2) {
                if(allPipersAreAtLeastState(2)){
                    piperState[p] = state = 3;
                    dst = piperStateMachine[p][state];
                } else {
                    dst = src;
                }
                play = true;

            }

            else if (state == 3) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 4;
                    dst = piperStateMachine[p][state];
                    play = true;
                }
                play = true;
            }

            else if (state == 4) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 5;
                    dst = piperStateMachine[p][state];
                    play = true;
                }
                play = true;
            }

            else if (state == 5) {
                if(isWithinDistance(src, dst, 0.00001) && noRatsAreWithinRange(pipers[id][p], rats, 10)){
                    piperState[p] = state = 6;
                    dst = src;
                }
                play = true;
            }

            else if (state == 6) {
                if(allPipersAreAtLeastState(6)){
                    piperState[p] = state = 7;
                    dst = piperStateMachine[p][state];
                } else {
                    dst = src;
                }
                play = true;
            }

            else if (state == 7) {
                dst = gateEntrance;
                if (isWithinDistance(src, dst, 0.00001)) {
                    piperState[p] = state = 8;
                    dst = densestPoint(pipers, pipers_played, rats, p);
                    piperStateMachine[p][8] = dst;
                }

            }

            else if (state == 8) {
                if(isWithinDistance(src, dst, 0.001)){
                    piperState[p] = state = 9;
                    dst = piperStateMachine[p][state];
                    play = true;
                } else {
                    dst = densestPoint(pipers, pipers_played, rats, p);
                    piperStateMachine[p][8] = dst;
                }
            }

            else if (state == 9) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 10;
                    dst = piperStateMachine[p][state];
                }
                else if(noRatsAreWithinRange(pipers[id][p], rats, 9)){
                    piperState[p] = state = 8;
                    dst = densestPoint(pipers, pipers_played, rats, p);
                    piperStateMachine[p][8] = dst;
                } else {
                    if(ratOnWayBack == null)
                        ratOnWayBack = dst = findRatOnPathToGate(pipers[id][p], pipers[id][p], gateEntrance, rats, 2);
                    else
                        dst = ratOnWayBack;
                }
                play = true;
            } else if (state == 10) {
                if(isWithinDistance(src, dst, 0.00001) && noRatsAreWithinRange(pipers[id][p], rats, 4)){
                    piperState[p] = state = 7;
                    dst = piperStateMachine[p][state];
                }
                play = true;
            } else {
                System.out.println("Piper " + p + " is in state " + state);
            }

            moves[p] = Movement.makeMove(src, dst, play);
        }
    }

    private void fillRatDensities(Point[] rats){

        //Go through candidate points and find point with
        for (int i = 0; i <= POINT_DENSITY; i++) {
            for (int j = 0; j <= POINT_DENSITY; j++) {

                Point point = Movement.makePoint(-side/2 + i*side/POINT_DENSITY, -side/2 + j*side/POINT_DENSITY, neg_y, swap);

                int numberOfRatsNearPoint = (int) Math.pow(numberOfRatsWithinXMetersOfPoint(point,
                        PIPER_RADIUS, rats, 0), 2);

                ratNumbers[i][j] = numberOfRatsNearPoint;
            }
        }
    }

    private boolean allPipersAreAtLeastState(int state){
        for(int i = 0; i < numberOfPipers; i++){
            if(piperState[i] < state)
                return false;
        }
        return true;
    }

    private boolean isWithinDistance(Point src, Point dst, double error){
        if(src != null && dst != null &&
                // checking if we are within a minimum distance of the destination
                Math.abs(src.x - dst.x) < error &&
                Math.abs(src.y - dst.y) < error){
            return true;
        }

        return false;
    }

    private boolean noRatsAreWithinRange(Point piper, Point[] rats, double distance){
        for(Point rat:rats){
            if(rat == null){
                continue;
            }
            if(Movement.distance(piper, rat) < distance){
                return false;
            }
        }
        return true;
    }

    private Point findClosestPiper(Point[][] pipers, Point closestRat, int p){

        for(int i = 0; i < numberOfPipers; i++){
            if(i == p)
                continue;

            if(Movement.distance(pipers[id][p], closestRat) <= 5){
                return pipers[id][p];
            }
        }

        return null;
    }

    /*
    * Also needs to consider how far away this point is from the gate. Basically a cost function.
    */
    public Point densestPoint(Point[][] pipers, boolean[][] pipers_played,
                                     Point[] rats, int p) {

        Point thisPiper = pipers[id][p];
        Point densest = Movement.makePoint(0, 0, neg_y, swap);
        double bestReward = 0;

        //Go through candidate points and find point with
        for (int i = 0; i <= POINT_DENSITY; i++) {
            for (int j = 0; j <= POINT_DENSITY; j++) {

                Point point = Movement.makePoint(-side/2 + i*side/POINT_DENSITY, -side/2 + j*side/POINT_DENSITY, neg_y, swap);

                double distanceFromPiperToPoint = PIPER_WALK_SPEED * Movement.distance(point, thisPiper);
                double distanceToGate = PIPER_RUN_SPEED * Movement.distance(point, gateEntrance);
                int numberOfRatsNearPoint = ratNumbers[i][j];/*(int) Math.pow(numberOfRatsWithinXMetersOfPoint(point,
                        PIPER_RADIUS, rats, p), 2);*/

                double reward = numberOfRatsNearPoint / (distanceFromPiperToPoint + distanceToGate);

                if (reward > bestReward) {
                    bestReward = reward;
                    densest = point;
                }
            }
        }

        Point closestPiper = findClosestPiper(pipers, densest, p);

        return closestPiper == null ? densest : closestPiper;
    }

    /*
     * Here's some documentation to explain a function even though it explains itself
     */
    private int numberOfRatsWithinXMetersOfPoint(Point p, double x, Point[] rats, int piper) {
        int result = 0;
        for (Point rat : rats) {
            double distanceFromPointToRat = Movement.distance(p, rat);
            if (distanceFromPointToRat < x) {
                result++;
            }
        }
        return result;
    }

    private Point findNearestRat(Point[][] pipers, Point[] rats, int p){

        Point piper = pipers[id][p];
        double minDist = Double.MAX_VALUE;
        Point closestRat = null;
        int index = -1;
        for(int i = 0; i < rats.length; ++i){
            if(i%(numberOfPipers - (p)) != 0){
                continue;
            }

            Point rat = rats[i];
            if(rat == null){
                continue;
            }
            double dist = Movement.distance(piper, rat);
            if(dist < minDist){
                minDist = dist;
                closestRat = rat;
            }
        }


        if(minDist > side/5){
             closestRat = rats[p % rats.length];
        }


        Point closestPiper = findClosestPiper(pipers, closestRat, p);

        if(closestPiper != null){
            closestRat = closestPiper;
        }

        return closestRat;

    }

    private double distanceFromPointToLine(Point p, Point lineStart, Point lineEnd) {
        return Math.abs((lineEnd.y-lineStart.y)*p.x - (lineEnd.x-lineStart.x)*p.y + lineEnd.x*lineStart.y - lineEnd.y*lineStart.x) / Math.sqrt(Math.pow(lineEnd.y-lineStart.y, 2) + Math.pow(lineEnd.x-lineStart.x, 2));
    }

    private Point findRatOnPathToGate(Point piper, Point lineStart, Point lineEnd, Point[] rats, double maxDist) {
        for (Point rat : rats) {
            if (Movement.distance(gateEntrance, rat) < side/5 
                && Movement.distance(piper, rat) > PIPER_RADIUS-1 && distanceFromPointToLine(rat, lineStart, lineEnd) <= maxDist) {
                System.out.println(rat);
                return rat;
            }
        }
        return gateEntrance;
    }

	private Point[] generateStateMachine(int p){

        Point[] states = new Point[11];

        states[0] = gateEntrance;

        if(p%10 != p && p % 2 == 0){
            p = 0;
        } else if (p%10 != p && p % 2 != 0){
            p = 9;
        }

        double spreadAngle = Math.min(90, numberOfPipers * 10);

        double theta = Math.toRadians(p * spreadAngle/(Math.min(numberOfPipers, 10) - 1) + 90 - spreadAngle/2);

        states[1] = Movement.makePoint(side/2 * Math.cos(theta), side/2 - (side * 0.5 * Math.sin(theta)), neg_y, swap);

        states[2] = null;

        states[3] = outsideGate;

        states[4] = gateEntrance;

        states[5] = insideGate;

        states[6] = null;

        states[7] = gateEntrance;

        states[8] = null;

        states[9] = gateEntrance;

        states[10] = insideGate;

        return states;

    }
}