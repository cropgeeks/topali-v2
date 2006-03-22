package topali.cluster.control;

import java.io.*;

/* This interface provides operations for interacting with the cluster. */
public interface ICluster
{
	public void submitJob(File jobDir, String scriptName)
		throws Exception;
	
	public int getJobStatus(File jobDir);
	
	public void deleteJob(File jobDir);
}