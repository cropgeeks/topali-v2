// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

public class JobException extends Exception
{
	// The Java (or other) Exception (possibly) caught as part of this job error
	private Exception exception;

	public JobException()
	{
	}

	public JobException(String message)
	{
		super(message);
	}

	public JobException(String message, Exception exception)
	{
		super(message);

		this.exception = exception;
	}

	public Exception getException()
	{
		return exception;
	}
}