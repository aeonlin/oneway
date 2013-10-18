package oneway.batch;

import oneway.sim.MovingCar;
import oneway.sim.Parking;

import java.util.*;

public class Player extends oneway.sim.Player
{
    // if the parking lot is almost full
    // it asks the opposite direction to yield
    private static double AlmostFull = 0.8;
    private int phase;

    private Parking[] left, right;

    public Player() {}

    public void init(int nsegments, int[] nblocks, int[] capacity)
    {
        this.nsegments = nsegments;
        this.nblocks = nblocks;
        this.capacity = capacity.clone();
        phase = 0;
    }


    public void setLights(MovingCar[] movingCars,
                          Parking[] left,
                          Parking[] right,
                          boolean[] llights,
                          boolean[] rlights)
    {
        this.left = left;
        this.right = right;
        // Strategy:
        // using a state diagram allowing batching cars from one side then the other
        for (int i = 0; i != nsegments; ++i) {
            llights[i] = true;
            rlights[i] = true;
        }
        System.out.println("current phase:" + phase);
        phase = phaseStrategy(movingCars, llights, rlights, phase);
        System.out.println("New phase:"+phase);
    }

    private int checkNextPhase(MovingCar[] movingCars, int phase) {
        if(movingCars.length == 0) {
            //no moving cars, check if there will be any moving cars next tic
            //make sure it's consistent with what the phaseStrategy will do
            if(right[0].size()==0 && left[nsegments].size()==0) {
                return 0;
            }
            else {
                if(right[0].size()>=left[nsegments].size()){
                    return 1;
                }
                else {
                    return -1;
                }
            }
        }
        else if(movingCars.length >=2 ){
            switch(phase) {
                //for phase 1 and -1:
                //  if the first car is not arriving, stays in the same phase
                //  otherwise, move on to the next phase (2 or -2)
                case 1:
                    if(hasTraffic(movingCars, nsegments-1, nblocks[nsegments-1]-1, 1)) {
                        return 2;
                    }
                    else {
                        return 1;
                    }
                    //break;
                case -1:
                    if(hasTraffic(movingCars, 0, 0, -1)) {
                        return -2;
                    }
                    else {
                        return -1;
                    }
                    //break;
                //for phase 2 and -2
                //  obviously the last car is not arriving,
                //  so stay in the same phase
                case 2:
                case -2:
                    return phase;
                    //break;
                default:
                    return 0;
            }
        }
        else {
            //only 1 car is moving
            //  arriving:
            //      no cars in lot: go to phase 0
            //      cars ready to move on: go to phase 1 / -1
            //  not arriving: 
            //      just begin: 1 / -1
            //      in the middle: stays the same
            if(hasTraffic(movingCars, nsegments-1, nblocks[nsegments-1]-1, 1)) {
                // arriving right
                if(right[0].size()==0 && left[nsegments].size()==0) {
                    //no cars are waiting
                    return 0;
                }
                else if(left[nsegments].size() > 0) {
                    return -1;
                }
                else {
                    return 1;
                }
            }
            else if(hasTraffic(movingCars, 0, 0, -1)) {
                //arriving left
                if(right[0].size()==0 && left[nsegments].size()==0) {
                    //no cars are waiting
                    return 0;
                }
                else if(right[0].size() > 0) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
            else {
                if(hasTraffic(movingCars, 0, 0, 1)) {
                    return 1;
                }
                else if(hasTraffic(movingCars, nsegments-1, nblocks[nsegments-1]-1, -1)) {
                    return -1;
                }
                else {
                    return phase;
                }
            }
        }
        //should not goes here
    }

    private int phaseStrategy(MovingCar[] movingCars, 
                                boolean[] llights, 
                                boolean[] rlights, 
                                int phase) {

        int nextPhase = checkNextPhase(movingCars, phase);
        // then we set lights based on the current phase and the nextPhase we are facing
        switch(nextPhase) {
            case 0:
                switch (phase){
                    case 0:
                        if(right[0].size()>=left[nsegments].size()){
                            setDirection(llights,rlights,1);
                        }
                        else {
                            setDirection(llights,rlights,-1);
                        }
                        break;
                    //give chance for the other direction
                    case 1:
                    case 2:
                        setDirection(llights,rlights,-1);
                        break;
                    case -1:
                    case -2:
                        setDirection(llights,rlights,1);
                        break;
                    default:
                        setDirection(llights,rlights,0);
                        break;
                }
                break;
            case 1:
                setDirection(llights,rlights,1);
                break;
            case 2:
                setDirection(llights,rlights,0);
                break;
            case -1:
                setDirection(llights,rlights,-1);
                break;
            case -2:
                setDirection(llights,rlights,0);
                break;
            default:
                break;
        }
        //then we return the nextPhase, ready to move on.
        return nextPhase;
    }

    private void setDirection(boolean[] llights, boolean[] rlights, int dir) {
        if(dir == 0){
            llights[nsegments-1] = false;
            rlights[0] = false;
        }
        if(dir > 0){
            llights[nsegments-1] = false;
            rlights[0] = true;
        }
        if(dir < 0){
            llights[nsegments-1] = true;
            rlights[0] = false;
        }
    }

    // check if the segment has traffic
    private boolean hasTraffic(MovingCar[] cars, int seg, int blk, int dir) {
        for (MovingCar car : cars) {
            if (car.segment == seg && car.block == blk && car.dir == dir)
                return true;
        }
        return false;
    }


    private int nsegments;
    private int[] nblocks;
    private int[] capacity;
}
