// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

// Base class for TOPALi Threads that are to be run in a multi cpu/core system.
public abstract class MultiThread extends Thread
{
	protected ThreadManager manager;
	
	protected long s, e;
	
	public MultiThread()
	{
	}
	
	public void startThread(ThreadManager manager)
	{
		this.manager = manager;
		
		// The manager will block until a token is available
		manager.getToken();
		
		// Then the Thread can start
		start();
	}
	
	protected void giveToken()
	{
		e = System.currentTimeMillis();
		System.out.println("Thread complete in " + (e-s) + "ms");
		
		if (manager != null)
			manager.giveToken();
	}
}