package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

import pppp.g3.Strategy;

import java.lang.Double;
import java.lang.System;

public class OnePiperStrategy implements pppp.g3.Strategy {

    public static final int PIPER_RADIUS = 10;
    public static final int PIPER_RUN_SPEED = 5; 
    public static final int PIPER_WALK_SPEED = 1;
    public static final double MAX_RAT_DIST_FROM_LINE = 2;
    public static final double DOOR = 0.0;

            // create gate positions
    public static Point gateEntrance;
    public static Point insideGate;
    public static Point justOutsideGate;

    //Because in this class p is always 0.
    public static int p = 0;

    private static int side = 0;
    private int id = -1;
    private long turns = 0;
    private int numberOfPipers = 0;
    private int[] piperState = null;
    private Point[][] piperStateMachine = null;
    private boolean neg_y;
    private boolean swap;

    private int numberOfHunters = 0;
    private int sparseCutoff = 10;

    private int initNumberOfRats;

    public void init(int id, int side, long turns,
                     Point[][] pipers, Point[] rats){
        gateEntrance = Movement.makePoint(DOOR, side * 0.5 - 5, neg_y, swap);
        insideGate = Movement.makePoint(DOOR, side * 0.5 + 2.5, neg_y, swap);
        justOutsideGate = Movement.makePoint(DOOR, side * 0.5 - 10, neg_y, swap);

        // storing variables
        this.id = id;
        this.side = side;
        this.turns = turns;

        // variables to rotate map
        neg_y = id == 2 || id == 3;
        swap  = id == 1 || id == 3;

        // create the state machines for the pipers
        numberOfPipers = pipers[id].length;
        piperStateMachine = new Point [numberOfPipers][];
        piperState = new int[numberOfPipers];
        initNumberOfRats = rats.length;

        //There's just one piper, so every index is 0
        piperStateMachine[0] = createHunterStateMachine();
        piperState[0] = 0;
    }

    public void play(Point[][] pipers, boolean[][] pipers_played,
                     Point[] rats, Move[] moves) {
        Point dst, src;
        boolean catchingPotentialRat = false;

        try {
            int state = piperState[p];
            //Stay inside gate till all rats are captured
            if (state == 4 && !noRatsAreWithinRange(pipers[id][p], rats, PIPER_RADIUS/2)) {
                dst = piperStateMachine[p][piperState[p]];
                src = pipers[id][p];
                moves[p] = Movement.makeMove(src, dst, play(state));
                return;
            }

            //Chase down any lost rats if you're heading for gate entrance or inside gate
            if (state == 4 && noRatsAreWithinRange(pipers[id][p], rats, 10)) {
                piperState[p] = 0;
            }
            if (state == 2) {
                piperStateMachine[p][piperState[p]] = densestPoint(pipers, pipers_played, rats);
            }

            src = pipers[id][p];
            dst = piperStateMachine[p][piperState[p]];

            if (state == 2 && isWithinDistance(src, dst, 3)) {
                ++piperState[p];
                piperState[p] = piperState[p] % piperStateMachine[p].length;
                dst = piperStateMachine[p][piperState[p]];
            }
            if (catchingPotentialRat && state == 3 && isWithinDistance(src, dst, 10)) {
                catchingPotentialRat = false;
                piperStateMachine[p][piperState[p]] = gateEntrance;
            }
            else if (isWithinDistance(src, dst, 0.05)) {
                ++piperState[p];
                piperState[p] = piperState[p] % piperStateMachine[p].length;
                dst = piperStateMachine[p][piperState[p]];
            }
            state = piperState[p];

            if(state == 0){
                piperStateMachine[0] = createHunterStateMachine();
            }

            //Try tracking down rats on the way home
            state = piperState[p];
            if (state == 3) {
                if (noRatsAreWithinRange(pipers[id][p], rats, PIPER_RADIUS)) {
                    piperState[p] = 2;
                    dst = densestPoint(pipers, pipers_played, rats);
                }
                else {
                    Point lineStart = pipers[id][p];
                    Point lineEnd = gateEntrance;
                    Point potentialCatch = findRatOnPathToGate(lineStart, lineStart, lineEnd, rats, MAX_RAT_DIST_FROM_LINE);
                    if (!potentialCatch.equals(gateEntrance)) {
                        piperStateMachine[p][piperState[p]] = potentialCatch;
                        catchingPotentialRat = true;
                    }
                }
            }
            moves[p] = Movement.makeMove(src, dst, play(state));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean play(int state) {
        return (state  >= 3);
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

    private boolean isInArena(Point pos){
        return (Math.abs(pos.x) < side/2 && Math.abs(pos.y) < side/2);
    }

    public boolean isNearGate(Point p, double distance){
        return (Movement.distance(p, gateEntrance) < distance);
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

    /*
    * Also needs to consider how far away this point is from the gate. Basically a cost function.
    */
    private Point densestPoint(Point[][] pipers, boolean[][] pipers_played,
                     Point[] rats) {

        Point thisPiper = pipers[id][p];
        Point densest = Movement.makePoint(0, 0, neg_y, swap);
        double bestReward = 0;

        //Go through candidate points and find point with 
        for (int i = - side/2; i <= side/2; i = i+side/25) {
            for (int j = -side/2; j <= side/2; j = j+side/25) {
                Point p = Movement.makePoint(i, j, neg_y, swap);

                double distanceFromPiperToPoint = PIPER_WALK_SPEED * Movement.distance(p, thisPiper);
                double distanceToGate = PIPER_RUN_SPEED * Movement.distance(p, gateEntrance);
                int numberOfRatsNearPoint = (int) Math.pow(numberOfRatsWithinXMetersOfPoint(p, 
                    PIPER_RADIUS, rats), 2);

                double reward = numberOfRatsNearPoint / (distanceFromPiperToPoint + distanceToGate);

                if (reward > bestReward) {
                    bestReward = reward;
                    densest = p;
                }
            }
        }
        return densest;
    }

    /*
     * Here's some documentation to explain a function even though it explains itself
     */
    private int numberOfRatsWithinXMetersOfPoint(Point p, double x, Point[] rats) {
        int result = 0;
        for (Point rat : rats) {
            double distanceFromPointToRat = Movement.distance(p, rat);
            if (distanceFromPointToRat < x) {
                result++;
            }
        }
        return result;
    }

    /*
     * Here's some documentation to explain a function even though it explains itself
     */
    private int numberOfPipersWithinXMetersOfPoint(Point p, Point[][] pipers, double dist) {
        int result = 0;
        for(int i = 0; i < pipers.length; i++){
            if(i == id)
                continue;
            for(int j = 0; j < pipers[i].length; j++){
                if(Movement.distance(p, pipers[i][j]) < dist){
                    ++result;
                }
            }
        }
        return result;
    }

    /*
    * Methods for finding rats along the path back to gate
    */
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

    // finds closest rat in direction away from the gate
    public Point findClosest(Point start, Point[] ends) {
        double c_dist;
        double dist_from_gate;
        double closest_distance = Double.MAX_VALUE;
        int closest = 0;

        for (int i = 0; i < ends.length; i++) {
            dist_from_gate = Movement.distance(gateEntrance, ends[i]);
            c_dist = Movement.distance(start, ends[i]);

            if (c_dist < closest_distance && dist_from_gate > 10) {
                closest = i;
                closest_distance = c_dist;
            }
        }
        return ends[closest];
    }

    private Point[] createHunterStateMachine() {
        // Hunters have a 5 state machine
        Point[] pos = new Point [5];

        // go to gate entrance
        pos[0] = gateEntrance;
        // go just outside gate
        pos[1] = justOutsideGate;
        //Will be replaced by eventual location
        pos[2] = justOutsideGate;
        // move to gate entrance
        pos[3] = gateEntrance;
        // Finally go inside the gate
        pos[4] = insideGate;

        return pos;
    }
}