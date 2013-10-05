package oneway.g9;

import oneway.sim.MovingCar;
import oneway.sim.Parking;

import java.util.*;

public class Player extends oneway.sim.Player
{
	// if the parking lot is almost full
	// it asks the opposite direction to yield
	private static double AlmostFull = 0.8;

	public Player() {}

	public void init(int nsegments, int[] nblocks, int[] capacity)
	{
		this.nsegments = nsegments;
		this.nblocks = nblocks;
		this.capacity = capacity.clone();
		this.indanger = new boolean[nsegments+1];
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
		// 1. initially turn all traffic lights off
		// 2. check each parking lot
		//    if it has pending cars, try to turn the light green
		//    a) if there is no opposite traffic, go ahead and turn right
		//    b) if there is opposite traffic, but the parking lot is piled up
		//       turn red the opposite traffic light.
		//       resume turning the traffic light after the traffic is clear
		// This strategy avoids car crash, but it cannot guarantee all cars
		// will be delivered in time and the parking lot is never full

		for (int i = 0; i != nsegments; ++i) {
			llights[i] = false;
			rlights[i] = false;
		}

		// find out almost full parking lot
		findFullParkingLot(movingCars);
		/*
		   boolean[] indanger = new boolean[nsegments+1];
		   for (int i = 1; i != nsegments; ++i) {
		   if (left[i].size() + right[i].size() 
		   > capacity[i] * AlmostFull) {
		   indanger[i] = true;
		   }            
		   }
		 */

		int deadlock = -1;
		deadlock = isDeadlock(movingCars);
		System.out.println("Deadlock: " +  deadlock);

		if (deadlock != -1) {
			int[] details = releaseCar(deadlock);
			int parking = details[0];
			int direction = details[1];
			System.out.println("parking lot:" + details[0] + " direction: " + details[1]);
			if (direction == 1) {
				rlights[parking] = true;
			}
			else {
				llights[parking-1] = true;
			} 
		}

		else {
			for (int i = 0; i != nsegments; ++i) {
				// if right bound has car
				// and the next parking lot is not in danger
				if (right[i].size() > 0 &&
						!indanger[i+1] &&
						!hasTraffic(movingCars, i, -1)) {
                                        System.out.println("Right: " + i);
					rlights[i] = true;
				}

				if (left[i+1].size() > 0 &&
						!indanger[i] &&
						!hasTraffic(movingCars, i, 1)) {
                                        System.out.println("Left: " + i);
					llights[i] = true;
				}

				// if both left and right is on
				// find which dir is in more danger
				if (rlights[i] && llights[i]) {
					double lratio = 1.0 * (left[i+1].size() + right[i+1].size()) / capacity[i+1];
					double rratio = 1.0 * (left[i].size() + right[i].size()) / capacity[i];
					if (lratio > rratio)
						rlights[i] = false;
					else if (lratio < rratio)
						llights[i] = false;
					else {
						if (left[i+1].get(0) < right[i].get(0)) {
							rlights[i] = false;
							llights[i] = true;
						}
						else {
							llights[i] = false;
							rlights[i] = true;
						}
                                        }
				}
			}
		}
	}


	// check if the segment has traffic
	private boolean hasTraffic(MovingCar[] cars, int seg, int dir) {
		for (MovingCar car : cars) {
			if (car.segment == seg && car.dir == dir)
				return true;
		}
		return false;
	}

	private void findFullParkingLot(MovingCar[] cars) {
		//reset the indanger flag
		for(int i=0; i<indanger.length;i++){ indanger[i]=false;}
		//determine which parking lot is almost fuull
		int carsOnSegmentLeft[] = new int[nsegments];
		int carsOnSegmentRight[] = new int[nsegments];
		for(MovingCar car : cars) {
			if( 1 == car.dir ){ carsOnSegmentRight[car.segment] += 1; }
			if( -1 == car.dir ){ carsOnSegmentLeft[car.segment] += 1; }
		}

		for (int i = 1; i != nsegments; ++i) {
			if (left[i].size() + right[i].size()
					+ carsOnSegmentRight[i-1] + carsOnSegmentLeft[i]
					+ isEmptyParkingLot(i-1,1)+isEmptyParkingLot(i+1,-1)
					> capacity[i] * AlmostFull) {
				indanger[i] = true;
			}            
		}
	}

	private int isEmptyParkingLot(int parkingLotId, int dir) {
		//dir = 1:  if there are any cars going right
		//dir = -1: if there are any cars going left
		//dir = 0:  if there are any cars in the parking lot
		if(dir > 0 ) {
			//for going right
			if(right[parkingLotId].size() > 0) { return 1;}
			else { return 0; }
		}
		if(dir < 0 ) {
			//for going left
			if(left[parkingLotId].size() > 0) { return 1;}
			else { return 0; }
		}
		//for both directions
		if(left[parkingLotId].size()+right[parkingLotId].size() > 0) { return 1;}
		else { return 0; }
	}


	private int isDeadlock(MovingCar[] cars) {
		for (int i = 1; i < nsegments; i++) {
			if (indanger[i-1] && indanger[i+1] && indanger[i]) {
				System.out.println("Indanger: " + i + " = " + indanger[i]);
				System.out.println("Indanger: " + (i+1) + " = " + indanger[i+1]);
				for (MovingCar c : cars) {
					if (c.segment == i-1 || c.segment == i) {
						return -1;
					}
				} 
				return i;
			}
		}
		return -1;	
	}

	private int[] releaseCar(int deadlockedParking) {
		int maxWaited = 100000;
		int parking = -1;
		int dir = 0;
		int[] details = new int[2];
		for (int l = 1; l < nsegments; l++) {
			if (left[l].size() > 0) {
				//for (int i = 0; i < left[l].length; i++) {
				if (maxWaited > left[l].get(0)) {
					maxWaited = left[l].get(0);
					parking = l;
					dir = -1;
				}
			}
			}
			for (int r = 1; r < nsegments; r++) {
				if (right[r].size() > 0) {
					//for (int i = 0; i < right[r].length; i++) {
					if (maxWaited > right[r].get(0)) {
						maxWaited = right[r].get(0);
						parking = r;
						dir = 1;
					}
				}
				}
				details[0] = parking;
				details[1] = dir;
				return details;
			}

			private int nsegments;
			private int[] nblocks;
			private int[] capacity;
			private boolean[] indanger;
			private Parking[] left, right;
		}
