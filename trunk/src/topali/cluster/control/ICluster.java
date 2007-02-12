// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.control;

import java.io.File;

/* This interface provides operations for interacting with the cluster. */
public interface ICluster
{
	public void submitJob(File jobDir, String scriptName) throws Exception;

	public int getJobStatus(File jobDir);

	public void deleteJob(File jobDir);
}