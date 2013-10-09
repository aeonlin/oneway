package oneway.g2;

public class MovingCar
{
    public  int segment;
    public  int block;

    // Right bound: 1
    // Left bound: -1
    public  int dir;
    public  int startTime;

    public MovingCar(int seg, int blk,
                     int d, int time)
    {
        this.segment = seg;
        this.block = blk;
        this.dir = d;
        this.startTime = time;
    }
}
