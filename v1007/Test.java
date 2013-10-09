import java.io.*;
import java.util.*;

public class Test {
	public static void main(String[] args) throws Exception {

		Parked p = new Parked();
		System.out.println("i:"+p.index + ",a:"+p.amount);
		
		Parked[] pks = new Parked[5];
		for(int i=0; i < pks.length; i++) {
			pks[i] = new Parked(i,i);
		}
		pks[0].amount = 3;
		pks[1].amount = 5;
		pks[2].amount = 1;
		pks[3].amount = 4;
		pks[4].amount = 2;

		Arrays.sort(pks);
		for(int i=0; i < pks.length; i++) {
			System.out.println("i:"+pks[i].index + ",a:"+pks[i].amount);
		}
		
	}
}

class Parked implements Comparable<Parked>{
	//for sorting the parking lot by the numbers of cars parked inside.
	public int index;
	public int amount;

	public Parked() {
		index = -1;
	}	
	public Parked(int i, int a) {
		index = i;
		amount = a;
	}
	public Parked(Parked p) {
		this.index = p.index;
		this.amount = p.amount;
	}
	public int compareTo(Parked p) {
		if(this.amount < p.amount) { return -1;}
		if(this.amount > p.amount) { return 1;}
		return 0;
	}
}