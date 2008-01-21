// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.threads;

import java.util.*;

/**
 * A killable Thread class for small jobs which can be run on the desktop
 * (Subclasses: updateObservers(DesktopThread.THREAD_FINISHED) should be called
 * at the end of the overriden run() method)
 */
public abstract class DesktopThread extends Thread
{
	public static final String THREAD_FINISHED = "STOP";
	
	private List<DesktopThreadObserver> observers = new LinkedList<DesktopThreadObserver>();
	protected boolean stop = false;
	
	public DesktopThread() {
		
	}
	
	/**
	 * Immediately stop the thread
	 * (if possible override in sub classes, and use 'stop' flag!)
	 */
	@SuppressWarnings("deprecation")
	public void kill() {
		if(this.isAlive()) {
			this.stop();
			updateObservers(THREAD_FINISHED);
		}
	}
	
	public void addObserver(DesktopThreadObserver obs) {
		this.observers.add(obs);
	}
	
	public void removeObserver(DesktopThreadObserver obs) {
		this.observers.remove(obs);
	}
		
	protected void updateObservers(Object obj) {
		for(DesktopThreadObserver obs : observers) 
			obs.update(obj);
	}
}
