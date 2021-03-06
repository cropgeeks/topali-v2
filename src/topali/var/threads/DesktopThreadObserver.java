// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.threads;

/**
 * Interface for classes which want to receive updates from a DesktopThread
 */
public interface DesktopThreadObserver
{
	public void update(Object obj);
	
}
