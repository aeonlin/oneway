package oneway.g2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class GenTiming {

	public static void main(String arg[]) throws IOException
	{
		genTime(Integer.parseInt(arg[0]),Integer.parseInt(arg[1]));
	}
	
	static void genTime(int cars, int time) throws IOException
	{
//		float start=(float) 0.8;
		int timecount=0;
		int carscount=0;
		File timing=new File("timing.txt");
		FileWriter fw=new FileWriter(timing);
		
		while(timecount<=time/3 && carscount<=cars)
		{
			fw.write(++timecount+"\n");
			fw.write((timecount*-1)+"\n");
			carscount+=2;
		}
		while(timecount>time/3 && timecount<=2*time/3 && carscount<=cars)
		{
			timecount++;
			int random = (int) (Math.random()*100);
//			fw.write("Random: " +random+"\n");
			if(random<=40)
			{
				int random2=(int) (Math.random()*100);
				if(random2<=30)
				{
					fw.write(timecount+"\n");
					carscount++;
				}
				else if(random2>30 && random2<=60)
				{
					fw.write((timecount*-1)+"\n");
					carscount++;
				}
				else {
					fw.write(timecount+"\n");
					fw.write((timecount*-1)+"\n");
					carscount+=2;
				}
			}
		}
		while(timecount<=time && carscount<=cars)
		{
			timecount++;
			int random = (int) (Math.random()*100);
//			fw.write("Random: " +random+"\n");
			if(random<=80)
			{
				fw.write(timecount+"\n");
				fw.write((timecount*-1)+"\n");
				carscount+=2;
			}
		}
		fw.close();
	}
}

	