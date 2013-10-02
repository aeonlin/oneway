import java.io.*;
import java.util.*;
import java.lang.*;

public class RandomTiming {
	public static void main(String[] args) throws Exception {
		if(args.length != 3) {
			showFormat();
		}
		else {
			try{
				int timeLength = Integer.parseInt(args[0]);
				double expectedAppearIntervalRight = Double.parseDouble(args[1]);
				double expectedAppearIntervalLeft = Double.parseDouble(args[2]);
				//create a random number generator
				Random rand = new Random();
				int x = 0;
				double leftChance = 1.0/expectedAppearIntervalLeft;
				double rightChance = 1.0/expectedAppearIntervalRight;
				//generating the timing tick by tick
				for(int i = 1; i<= timeLength; i++) {
					//for each tick, there can be 1/expectedAppearInterval chance
					//that there is a car appearing at the specific end.
					if( rand.nextDouble() <= leftChance ) {
						System.out.print("-");
						System.out.println(i);
					}
					if( rand.nextDouble() <= rightChance ) {
						System.out.println(i);
					}	
				}
			}
			catch (Exception e){
				showFormat();
				throw new RuntimeException(e);
			}
		}
	}

	public static void showFormat(){
		System.out.println("===============================");
		System.out.println("Run:");
		System.out.println("java RandomTiming <Time Length> <Expected Car Appearance Interval (Left End)> <Expected Car Appearance Interval (Right End)> ");
		System.out.println("===============================");
	}
}