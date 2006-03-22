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