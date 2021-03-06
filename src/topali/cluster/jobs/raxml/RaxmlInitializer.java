// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.raxml;

import java.io.File;

import topali.cluster.*;
import topali.cluster.jobs.raxml.analysis.RaxmlAnalysis;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;
import topali.var.utils.SequenceSetUtils;

public class RaxmlInitializer extends Thread
{

	private SequenceSet ss;

	private RaxmlResult result;

	private File jobDir;
	
	public RaxmlInitializer(File jobDir, SequenceSet ss, RaxmlResult result)
	{
		this.jobDir = jobDir;
		this.ss = ss;
		this.result = result;
	}
	
	
	public void run()
	{
		try
		{
			// Ensure the directory for this job exists
			jobDir.mkdirs();
			// Store the Result object where the individual runs can get it
			Castor.saveXML(result, new File(jobDir, "submit.xml"));
			// Sequences that should be selected/saved for processing
			int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
			//Store alignment
			ss.save(new File(jobDir, "seq"), indices, result.getPartitionStart(), result.getPartitionEnd(), Filters.PHY_I, true);
			
			StringBuffer sb = new StringBuffer();
			for(RaxPartition p : result.partitions) {
				if(!p.dna)
					sb.append(p.model+", "+ p.name+" = "+p.indeces+"\n");
				else
					sb.append(p.name+" = "+p.indeces+"\n");
			}
			
			ClusterUtils.writeFile(new File(jobDir, "partitions"), sb.toString());
			
			for (int i = 1; i <= result.bootstrap+1; i++)
			{	
				if (LocalJobs.isRunning(result.jobId) == false)
					return;

				File runDir = new File(jobDir, "run" + i);
				runDir.mkdirs();

				//Store alignment
				if(i==1)
					ss.save(new File(runDir, "seq"), indices, result.getPartitionStart(), result.getPartitionEnd(), Filters.PHY_I, true);
				else {
					SequenceSet boot = ss.subSet(result.getPartitionStart(), result.getPartitionEnd(), indices);
					if(result.partitions.size()>1)
						boot = SequenceSetUtils.getBootstrappedSequenceSet(boot, 3, true);
					else
						boot = SequenceSetUtils.getBootstrappedSequenceSet(boot, 1, true);
					boot.save(new File(runDir, "seq"), Filters.PHY_I, true);
				}
				
				ClusterUtils.writeFile(new File(runDir, "partitions"), sb.toString());
				
				if (result.isRemote == false)
					new RaxmlAnalysis(runDir).start(LocalJobs.manager);
			}

			if (result.isRemote)
				RaxmlWebService.runScript(jobDir, result);

		} catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}
}
