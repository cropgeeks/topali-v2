// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

public class JobStatus
{
	public static final int UNKNOWN = 0;

	public static final int STARTING = 1;
	public static final int QUEUING = 2;
	public static final int RUNNING = 3;
	public static final int HOLDING = 4;
	public static final int COMPLETING = 5;
	public static final int CANCELLING = 6;
	
	public static final int COMPLETED = 10;
	public static final int CANCELLED = 11;
	
	public static final int COMMS_ERROR = 20;
	public static final int FATAL_ERROR = 21;
	
	public float progress;
	public int status;
	
	
	public JobStatus()
	{
	}
	
	public JobStatus(float progress, int status)
	{
		this.progress = progress;
		this.status = status;
	}
}