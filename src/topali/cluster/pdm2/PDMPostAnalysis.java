// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm2;

import java.io.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

class PDMPostAnalysis extends MultiThread
{	
	private File jobDir, wrkDir;
	private PDM2Result result;

	public static void main(String[] args)
	{ 
		PDMPostAnalysis post = null;
		
		try
		{
			post = new PDMPostAnalysis(new File(args[0]));
			post.run();
		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(post.jobDir, "error.txt"), e);
		}
	}
	
	PDMPostAnalysis(File jobDir)
		throws Exception
	{
		// Data directory
		this.jobDir = jobDir;

		// Read the PDM2Result
		File resultFile = new File(jobDir, "submit.xml");
		result = (PDM2Result) Castor.unmarshall(resultFile);

		// Read the SequenceSet
//		ss = new SequenceSet(new File(jobDir, "pdm.fasta"));
		
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
			result,	jobDir.getName(), "pdm2_post");
	}
	
	public void run()
	{
		System.out.println("Running PDM2PostAnalysis");
	}
}