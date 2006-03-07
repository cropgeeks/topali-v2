package topali.cluster;

// Utility class that stores an allocation of Thread "tokens" that can be used
// for running [n] multiple threads at once (but never more than [n]). Each
// Thread must ask this class for a token, waiting until one is free if the
// request cannot be met immediately.
public class ThreadManager
{
	private int tokens, maxTokens;
	
	public ThreadManager()
	{
		this(Runtime.getRuntime().availableProcessors());
	}
	
	public ThreadManager(int numOfTokens)
	{
		tokens = maxTokens = numOfTokens;
	}
	
	public synchronized void getToken()
	{
		// Wait until a token is available
		while (tokens == 0)
		{
			try { wait(); }
            catch (InterruptedException e) {}
		}
		
		// Release a token for use by the Thread
		tokens = tokens - 1;
	}
	
	public synchronized void giveToken()
	{
		// Store the token again
		tokens = tokens + 1;
				
		notifyAll();
	}
	
	public boolean threadsRunning()
		{ return tokens < maxTokens ? true : false; }
}