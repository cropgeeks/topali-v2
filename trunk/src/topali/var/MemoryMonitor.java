// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.lang.management.*;

public class MemoryMonitor extends Thread
{
	public static final int UNIT_BYTES = 1;
	public static final int UNIT_KILOBYTES = 1024;
	public static final int UNIT_MEGABYTES = 1024*1024;
	
	boolean running = true;
	
	int divisor = 1;
	String unit = "b";
	
	MemoryMXBean membean;
	
	int time = 10;
	
	public MemoryMonitor() {
		membean = ManagementFactory.getMemoryMXBean();
	}
	
	public MemoryMonitor(int unit, int time) {
		this();
		this.divisor = unit;
		this.time = time;
		
		switch(unit) {
		case UNIT_BYTES: this.unit = "b"; break;
		case UNIT_KILOBYTES: this.unit = "kb"; break;
		case UNIT_MEGABYTES: this.unit = "mb"; break;
		}
	}
	
	@Override
	public void run()
	{
		while(running) {
			int heap = (int)(membean.getHeapMemoryUsage().getUsed()/divisor);
			int maxheap = (int)(membean.getHeapMemoryUsage().getMax()/divisor);
			int freeheap = maxheap - heap;
			int nonheap = (int)(membean.getNonHeapMemoryUsage().getUsed()/divisor);
			int maxnonheap = (int)(membean.getNonHeapMemoryUsage().getMax()/divisor);
			int freenonheap = maxnonheap-nonheap;
			
			int total = heap+nonheap;
			int free = freeheap+freenonheap;
			
			String result = "--\n" +
			"Memory Used: "+total+" "+unit+"\n" +
			"Memory Free: "+free+" "+unit+"\n" +
			"Used Heap: "+heap+" "+unit+"\n" +
			"Free Heap: "+freeheap+" "+unit+"\n";
			System.out.println(result);
			
			try
			{
				Thread.sleep(time*1000);
			} catch (InterruptedException e)
			{
			}
		}
	}

	
	public void stopMonitor() {
		this.running = false;
	}
	
	
}
