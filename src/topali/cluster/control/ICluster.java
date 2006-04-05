// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

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