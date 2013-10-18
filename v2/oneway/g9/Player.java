package oneway.g9;

import oneway.sim.MovingCar;
import oneway.sim.Parking;

import java.util.*;



public class Player extends oneway.sim.Player
{
	// if the parking lot is almost full
	// it asks the opposite direction to yield
	private static double AlmostFull = 1.0;
	public static int emergencyModeTics = -1;
	public static int emergencyModeDir = 0;
	public static int emergencyPL = -1;

	private int nsegments;
	private int[] nblocks;
	private int[] capacity;
	private boolean[] indanger;
	private Parking[] left, right;
	private int[] leftTraffic, rightTraffic;

	// expected penalty calculation
	private int minTime;
	private int timeNow;
	// to deal with the zero size parking lot: 
	// Ignore its usage, treat it like a 0-length road segment.
	private int[] parkingLotIndexMapping;
	private int[] reverseLotIndexMapping;
	private int nsegments_sim;
	private int[] nblocks_sim;

	public Player() {}

	public void init(int nsegments, int[] nblocks, int[] capacity)
	{
		this.nsegments = 0;
		for(int i = 1 ; i <= nsegments ; i++) {
			if( 0 != capacity[i]) {
				this.nsegments++;
			}
		}
		nsegments_sim = nsegments;
		nblocks_sim = nblocks;
		this.nblocks = new int[this.nsegments];
		this.capacity = new int [this.nsegments+1];
		parkingLotIndexMapping = new int[nsegments+1];
		reverseLotIndexMapping = new int[this.nsegments+1];
		for(int i = 0, j = 0; i <= nsegments ; i++) {
			parkingLotIndexMapping[i] = -1;
			if( 0 != capacity[i]) {
				this.capacity[j] = capacity[i];
				parkingLotIndexMapping[i] = j;
				reverseLotIndexMapping[j] = i;
				j++;
			}
			if( i != nsegments) {
				this.nblocks[j-1] += nblocks[i];
			}
		}

		left = new Parking[this.nsegments+1];
		right = new Parking[this.nsegments+1];
		this.indanger = new boolean[this.nsegments+1];
		this.leftTraffic = new int[this.nsegments];
		this.rightTraffic = new int[this.nsegments];
		minTime = 0;
		for(int n: this.nblocks) {
			minTime += n;
		}
		timeNow = 0;
	}

	// the mapping / reverse mapping modules for ignoring the 0-size parking lot
	private MovingCar mapPosition(MovingCar movingCar_sim){
		int seg = movingCar_sim.segment;
		int blk = movingCar_sim.block;
		int dir = movingCar_sim.dir;
		int sTime = movingCar_sim.startTime;
		while(parkingLotIndexMapping[seg] < 0) {
			seg--;
			blk += nblocks_sim[seg];
		}
		seg = parkingLotIndexMapping[seg];
		MovingCar c = new MovingCar(seg,blk,dir,sTime);
		return c;
	}
	private void mapParkingLot(Parking[] p_sim, Parking[] p){
		for(int i = 0; i <= nsegments_sim ; i++) {
			int j = parkingLotIndexMapping[i];
			if(j >= 0) {
				if(p_sim[i] != null) {
					p[j] = new Parking(p_sim[i]);
				}
				else {
					p[j] = new Parking();
				}
			}
		}
	}
	private void reverseMapLights(boolean[] lights, boolean[] lights_sim, int dir) {
		int offset = (dir > 0) ? 0 : 1;
		for(int i = 0; i < nsegments_sim; i++) {
			int j = parkingLotIndexMapping[i+offset];
			if( j >= 0 ) {
				j -= offset;
				lights_sim[i] = lights[j];
			}
			else {
				lights_sim[i] = true;
			}
		}
	}


	public void setLights(MovingCar[] movingCars_sim,
			Parking[] left_sim,
			Parking[] right_sim,
			boolean[] llights_sim,
			boolean[] rlights_sim)
	{
		timeNow++;
		boolean setNewStrategy = false;
		boolean zerocapacity = false;
		this.left = left;
		this.right = right;

		for (int i = 1; i < nsegments; i++) {
			if (((nblocks[i-1])/2 + (nblocks[i])/2) >= (2*capacity[i])) {
				setNewStrategy = true;
				break;
			}
		}

		System.out.println("Parking lots: " + parkingLotIndexMapping.length);
		for (int i = 1; i < nsegments; i++) {
			if (parkingLotIndexMapping[i] == -1) {
				zerocapacity = true;
				break;
			}
		}

		if (setNewStrategy && !zerocapacity) {
			strategy1(timeNow, movingCars_sim, left_sim, right_sim, llights_sim, rlights_sim);
		}
		else{

		// Strategy:
		// 0. Ignoring any parking lots with 0 capacity.
		//    Treat those parking lots as 0-length road segments,
		//	  and always set the adjacent outbound lights green.
		// 1. initially turn all traffic lights off
		// 2. sort the parking lot by the expected penalty of all cars it related to
		// 3. in the sorted order, check each parking lot 
		//	  to see if we can turn any outbound light to green without
		//	  making the following accidents:
		//	  a) the cars crash
		//	  b) the parking lot will inevitably full some tics after
		// 4. if no cars are moving but there are still cars in the system,
		//	  then a "deadlock" happens. A deadlock dealing strategy is then applied.

		// Map a 0-size parking lot configuration to 
		// an equivalent configuration without any 0-size parking lots
		MovingCar[] movingCars = new MovingCar[movingCars_sim.length];
		for(int i = 0; i < movingCars_sim.length; i++) {
			movingCars[i] = mapPosition(movingCars_sim[i]);
		}
		System.out.println("= setLights =");
		mapParkingLot(left_sim, left);
		mapParkingLot(right_sim, right);
		boolean[] llights = new boolean[nsegments];
		boolean[] rlights = new boolean[nsegments];
		// End of mapping. 
		// The reverse configuration mapping will be implemented 
		// at the end of this setLights() function.

		if (emergencyModeTics > 0) {
			System.out.println("\n\n*************In emergency mode");
			rlights [0] = false;
			llights[nsegments-1] = false;
			for (int i = emergencyPL; i < nsegments ; i++) {
				rlights[i] = true;
				//rlights[parking] = true;
			}
			for (int i = emergencyPL -1; i >= 0 ; i--) {
				llights[i] = true;
			}			
			emergencyModeTics--;
			if( 0 == emergencyModeTics) {
				if(!lightsAvailable(movingCars, llights, rlights)) {
					emergencyModeTics = 1;
				}	
			}

			// if (emergencyModeDir == 1) {
			// 	rlights [0] = false;
			// 	for (int i = emergencyPL; i < nsegments ; i++) {
			// 		rlights[i] = true;
			// 		//rlights[parking] = true;
			// 	}
			// }

			// else {
			// 	for (int i = emergencyPL -1; i >= 0 ; i--) {
			// 		llights[i] = true;
			// 	}
			// 	//llights[parking-1] = true;
			// 	llights[nsegments-1] = false;
			// }
		}
		else {
			int deadlock = isDeadlock(movingCars);
			if (isinDeadlock(movingCars)) { 
				System.out.println("\n\n*************In deadlock");
				int[] details = releaseCar(deadlock);
				int parking = details[0];
				int direction = details[1];
				System.out.println("parking lot:" + details[0] + " direction: " + details[1]);

				rlights [0] = false;
				llights[nsegments-1] = false;
				for (int i = parking; i < nsegments ; i++) {
					rlights[i] = true;
					//rlights[parking] = true;
				}
				for (int i = parking - 1; i >=0 ; i--) {
					llights[i] = true;
				}

				if (direction == 1) {
					// rlights [0] = false;
					// for (int i = parking; i < nsegments ; i++) {
					// 	rlights[i] = true;
					// 	//rlights[parking] = true;
					// }
					int tics = 0;
					for (int i = parking; i < nsegments; i++) {
						tics = tics + nblocks[i];
					}
					emergencyModeTics = tics;
					emergencyModeDir = 1;
				}
				else {
					// for (int i = parking - 1; i >=0 ; i--) {
					// 	llights[i] = true;
					// }
					// //llights[parking-1] = true;
					// llights[nsegments-1] = false;
					int tics = 0;
					for (int i = parking - 1; i >= 0; i--) {
						tics = tics + nblocks[i];
					}
					emergencyModeTics = tics;
					emergencyModeDir = -1;
					
				} 
				emergencyPL = parking;
			}

			else {
				for (int i = 0; i != nsegments; ++i) {
					llights[i] = false;
					rlights[i] = false;
				}
				// Sort the index of parking lot 
				// by the sum of expected penalty of cars related to each parking lot
				Parked[] parkingLotIndex = orderTheParkingLots(movingCars);

				System.out.print("parkingLot ordered by occupation:");
				for(Parked p : parkingLotIndex) {
					if( p!=null) {System.out.print(p.index+" ");}
				}
				System.out.println();
				System.out.print("penalty contribution in this order:");
				for(Parked p : parkingLotIndex) {
					if( p!=null) {System.out.print(p.penalty+" ");}
				}
				System.out.println();
				// Go through all parking lots by the sorted order
				// Turn the outbound lights green and see if there will be anything wrong
				// If something is wrong, turn them back to red again
				for(int i=0; i<=nsegments; i++) {
					int index = parkingLotIndex[i].index;
					if( index != nsegments && index != 0) {
						if(left[index].size() >= right[index].size()) {
							llights[index-1] = true;
							if(!lightsAvailable(movingCars, llights, rlights)) {
								llights[index-1] = false;
							}
							rlights[index] = true;
							if(!lightsAvailable(movingCars, llights, rlights)) {
								rlights[index] = false;
							}
						}
						else {
							rlights[index] = true;
							if(!lightsAvailable(movingCars, llights, rlights)) {
								rlights[index] = false;
							}
							llights[index-1] = true;
							if(!lightsAvailable(movingCars, llights, rlights)) {
								llights[index-1] = false;
							}	
						}
					}
					if(index == 0 ) {
						rlights[index] = true;
						if(!lightsAvailable(movingCars, llights, rlights)) {
							rlights[index] = false;
						}
					}
					if(index == nsegments) {
						llights[index-1] = true;
						if(!lightsAvailable(movingCars, llights, rlights)) {
							llights[index-1] = false;
						}	
					}
				}
				// recalculate the next tic parkinglot occupation status for current light settings
				willExceedLimit(movingCars, llights, rlights);
				System.out.println("traffic:");
				for(int i = 0; i<nsegments; i++) {
					System.out.print("r"+i+":"+rightTraffic[i]+", ");
					System.out.print("L"+i+":"+leftTraffic[i]+", ");
				}
				System.out.println("");
			}
		}

		//do the inverse mapping back to a possibly containing zero-size parking lot config
		reverseMapLights(rlights, rlights_sim, 1);
		reverseMapLights(llights, llights_sim, -1);
		}
	}


	private Parked[] orderTheParkingLots(MovingCar[] movingCars) {
		Parked[] parkingLotIndex = new Parked[nsegments+1];
		
		int[] blocksToRightEnd = new int[nsegments];
		int[] blocksToLeftEnd = new int[nsegments+1];
		int[] queueTimeInLotLeft = new int [nsegments+1];
		int[] queueTimeInLotRight = new int [nsegments+1];
		blocksToLeftEnd[0] = 0;
		blocksToRightEnd[nsegments-1] = nblocks[nsegments-1];
		for(int i = 1 ; i <= nsegments ; i++ ){
			blocksToLeftEnd[i] = blocksToLeftEnd[i-1] + nblocks[i-1];
		}
		for(int i = nsegments-2 ; i >= 0 ; i-- ){
			blocksToRightEnd[i] = blocksToRightEnd[i+1] + nblocks[i];
		}
		double L = 0.0;
		for(int i = 0 ; i <= nsegments ; i++) {
			parkingLotIndex[i] = new Parked(i,i,0);
			int optimumTimeToLeft = timeNow;
			int optimumTimeToRight = timeNow;
			queueTimeInLotRight[i] = 0;
			queueTimeInLotLeft[i] = 0;
			parkingLotIndex[i].amount = 0;
			parkingLotIndex[i].penalty = 0;
			if(i!=nsegments) {
				parkingLotIndex[i].amount += right[i].size();
				optimumTimeToRight += blocksToRightEnd[i];
				for(int start : right[i]) {
					L = (optimumTimeToRight + queueTimeInLotRight[i]) - start;
					parkingLotIndex[i].penalty += 
						( L * Math.log(L) - minTime* Math.log(minTime) );
					queueTimeInLotRight[i] += 2;
				}
			}
			if(i!=0){
				parkingLotIndex[i].amount = left[i].size();
				optimumTimeToLeft += blocksToLeftEnd[i];
				for(int start : left[i]) {
					L = (optimumTimeToLeft + queueTimeInLotLeft[i]) - start;
					parkingLotIndex[i].penalty += 
						( L * Math.log(L) - minTime* Math.log(minTime) );
					queueTimeInLotLeft[i] += 2;
				}
			}
		}
		for(MovingCar c : movingCars) {
			int optimumTime = timeNow;
			int i = c.segment;
			if(c.dir == 1) {
				//moving right
				optimumTime += blocksToRightEnd[i] - c.block;
				if( i != nsegments-1) {
					int extraWait = (c.block + 1 + queueTimeInLotRight[i+1]) - nblocks[i];
					if( extraWait > 0) {
						optimumTime += extraWait;
					}
				}
			}
			else {
				//moving left
				optimumTime += blocksToLeftEnd[c.segment] + c.block + 1;
				if( i != 0) {
					int extraWait = queueTimeInLotLeft[i] - (c.block + 1);
					if( extraWait > 0) {
						optimumTime += extraWait;
					}
				}
			}
			L = optimumTime - c.startTime;
			parkingLotIndex[i].penalty += 
						( L * Math.log(L) - minTime* Math.log(minTime) );
		}
		Arrays.sort(parkingLotIndex);
		return parkingLotIndex;
	}

	private boolean lightsAvailable(MovingCar[] cars, boolean[] llights, boolean[] rlights){
		return !(willCrash(cars, llights, rlights) || willExceedLimit(cars, llights, rlights));
	}
	private boolean willCrash(MovingCar[] cars, boolean[] llights, boolean[] rlights) {
		boolean crash = false;
		findNextTicTraffic(cars, llights, rlights);
		for(int i = 0; i < nsegments; i++) {
			if( leftTraffic[i] > 0 && rightTraffic[i] > 0) {
				crash = true;
			}
		}
		return crash;
	}

	private void findNextTicTraffic(MovingCar[] cars, boolean[] llights, boolean[] rlights) {
		//Usage:
		//	for given lights setting,
		//	calculate the number of cars moving on each segments at the next tic

		//reset the leftTraffic and rightTraffic to be all 0's
		Arrays.fill(leftTraffic, 0);
		Arrays.fill(rightTraffic, 0);
		//First we consider the moving cars
		for (MovingCar car : cars) {
			if (car.dir == 1){
				// going right
				if(car.block == nblocks[car.segment] -1 ) {
					//The right-most end of the segment
					if(car.segment != nsegments-1) {
						//if it is not the right-most segment (otherwise it arrives)
						if(rlights[car.segment+1] && (right[car.segment+1].size() == 0)) {
							//only if the right light is green
							//then there will be right traffic on the next segment
							//but if there are right-bound cars waiting in the parking lot
							//this car needs to wait inside the parking lot
							rightTraffic[car.segment+1] += 1;
						}
					}
				}
				else {
					//The car will remain on the same segment
					rightTraffic[car.segment] += 1;
				}
			}
			else {
				// going left 
				if(car.block == 0) {
					if(car.segment != 0) {
						//if it is not the left-most segment (otherwise it arrives)
						if(llights[car.segment-1] && (left[car.segment].size() == 0)) {
							//only if the left light is green
							//then there will be left traffic on the previous segment
							//but if there are left-bound cars waiting in the parking lot
							//this car needs to wait inside the parking lot
							leftTraffic[car.segment-1] += 1;
						}
					}
				}
				else {
					//The car will remain on the same segment
					leftTraffic[car.segment] += 1;
				}
			}
		}
		//Now calculate the cars to be released from each parking lot
		//case 1: the parking lot in the middle
		for(int i=1; i<nsegments; i++) {
			if(llights[i-1] && (left[i].size() > 0)) {
				leftTraffic[i-1] += 1;
			}
			if(rlights[i] && (right[i].size() > 0)) {
				rightTraffic[i] += 1;
			}
		}
		//case 2: the parking lot at the both ends
		if(rlights[0]) {
			rightTraffic[0] += 1;
		}
		if(llights[nsegments-1]) {
			leftTraffic[nsegments-1] += 1;
		}
	}

	private boolean willExceedLimit(MovingCar[] cars, boolean[] llights, boolean[] rlights) {
		//reset the indanger flag
		for(int i=0; i<indanger.length;i++){ indanger[i]=false;}
		//determine which parking lot is almost fuull
		int nextTicLeft[] = new int[nsegments+1];
		int nextTicRight[] = new int[nsegments+1];
		boolean overLimit = false;
		for(MovingCar car : cars) {
			if( 1 == car.dir && car.block == nblocks[car.segment] -1) { 
				nextTicRight[car.segment+1] += 1; }
			if( -1 == car.dir && car.block == 0) { 
				nextTicLeft[car.segment] += 1; }
		}
		for (int i = 1; i != nsegments; ++i) {
			nextTicLeft[i] += left[i].size();
			nextTicRight[i] += right[i].size();
			//there is car going out and the light is green
			if(rlights[i] && nextTicRight[i] > 0) {
				nextTicRight[i] -= 1;
			}
			if(llights[i-1] && nextTicLeft[i] > 0) {
				nextTicLeft[i] -= 1;
			}
			//So far we've counted all the cars which will stay in parking lots
			//Now add the rest moving cars
			nextTicRight[i] += rightTraffic[i-1];
			nextTicLeft[i] += leftTraffic[i];
			if (nextTicLeft[i] + nextTicRight[i] > capacity[i] * AlmostFull) {
				indanger[i] = true;
				overLimit = true;
			}            
		}
		return overLimit;
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

	// check if the segment has traffic
	private boolean hasTraffic(MovingCar[] cars, int seg, int dir) {
		for (MovingCar car : cars) {
			if (car.segment == seg && car.dir == dir)
				return true;
		}
		return false;
	}

	private boolean isinDeadlock(MovingCar[] cars) {
		if (cars.length == 0) {
			for (int i = 0; i < nsegments; i++) {
				if (left[i+1].size() > 0 || right[i].size() > 0)
					return true;
			}
		}
		return false;
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
		for (int l = 1; l <= nsegments; l++) {
			if (left[l].size() > 0) {
				//for (int i = 0; i < left[l].length; i++) {
				if (maxWaited > left[l].get(0)) {
					maxWaited = left[l].get(0);
					parking = l;
					dir = -1;
				}
			}
		}
		for (int r = 0; r < nsegments; r++) {
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
	
	private void strategy1(int timeNow,
			MovingCar[] movingCars,
			Parking[] left,
			Parking[] right,
			boolean[] llights,
			boolean[] rlights)
	{
		if (timeNow == 1 || right[0].size() > 0) {
			for (int i = 0; i < nsegments; i++) {
				rlights[i] = true;
				llights[i] = false;
			}
		}
		else {
			if (movingCars.length == 0) {
				for (int i = 0; i < nsegments; i++) {
					llights[i] = true;
					rlights[i] = false;
				}
			}
		}
	}

}

class Parked implements Comparable<Parked>{
	//for sorting the parking lot by the numbers of cars parked inside.
	public int index;
	public int amount;
	public double penalty;

	public Parked() {
		index = -1;
	}	
	public Parked(int i, int a, double p) {
		index = i;
		amount = a;
		penalty = p;
	}
	public Parked(Parked p) {
		this.index = p.index;
		this.amount = p.amount;
		this.penalty = p.penalty;
	}
	public int compareTo(Parked p) {
		if(this.penalty < p.penalty) { return 1;}
		if(this.penalty > p.penalty) { return -1;}
		return 0;
	}
		
}
