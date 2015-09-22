package pppp.g0;

import pppp.sim.Point;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by naman on 9/19/15.
 */
public class Piper {

    public int id;
    public Set<Integer> capturedRats;
    public Point prevLocation;
    public Point curLocation;
    public boolean playedMusic;
    public double absMovement;
    public Strategy strategy;

    public Piper(int id, Point curLocation) {
        this.id = id;
        this.capturedRats = new HashSet<Integer>();
        this.prevLocation = null;
        this.curLocation = curLocation;
        this.playedMusic = false;
	    this.absMovement = 0;
        this.strategy = new Strategy();
    }

    public Piper(int id, Point curLocation, Strategy strategy) {
        this.id = id;
        this.capturedRats = new HashSet<Integer>();
        this.prevLocation = null;
        this.curLocation = curLocation;
        this.playedMusic = false;
        this.absMovement = 0;
        this.strategy = strategy;
    }

    public Piper(int id, Point curLocation, boolean playedMusic) {
        this.id = id;
        this.capturedRats = new HashSet<Integer>();
        this.prevLocation = null;
        this.curLocation = curLocation;
        this.playedMusic = playedMusic;
        this.absMovement = 0;
        this.strategy = new Strategy();
    }

    public void updateMusic(boolean playMusic) {
	this.playedMusic = playMusic;
    }

    public void updateLocation(Point point) {
	if (this.prevLocation != null) {
	    double memory = 4;
	    absMovement = absMovement * (memory - 1) / memory;
	    absMovement += Math.hypot(point.x - this.prevLocation.x, point.y - this.prevLocation.y) / memory;
	}
        this.prevLocation = this.curLocation;
        this.curLocation = point;
    }

    public void resetRats() {
        this.capturedRats = new HashSet<Integer>();
    }

    public void addRat(Integer ratId) {
        this.capturedRats.add(ratId);
    }

    public int getNumCapturedRats() {
	return capturedRats.size();
    }

    public double getAbsMovement() {
	return absMovement;
    }
}
