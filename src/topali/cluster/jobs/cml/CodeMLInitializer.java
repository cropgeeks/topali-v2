// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.File;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

/**
 * Initializer class for a codeml postitive selection run. An instance of this
 * class must save a copy of the alignment to disk where it can be read by the
 * subjobs, then start a selection of subjobs, each job running a different
 * codeml model on the data.
 */
public class CodeMLInitializer extends Thread
{
	private SequenceSet ss;

	private CodeMLResult result;

	// Directory where the job will store its final results
	private File jobDir;

	public CodeMLInitializer(File jobDir, SequenceSet ss, CodeMLResult result)
	{
		this.jobDir = jobDir;
		this.ss = ss;
		this.result = result;
	}

	public void run()
	{
		try
		{
			startThreads();
		} catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}

	private void startThreads() throws Exception
	{
		// Ensure the directory for this job exists
		jobDir.mkdirs();

		// Store the CodeMLResult object where it can be read by the sub-job
		Castor.saveXML(result, new File(jobDir, "submit.xml"));

		// Sequences that should be selected/saved for processing
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		// Store the sequence data in phylip sequential
		ss.save(new File(jobDir, "seq.phy"), indices, Filters.PHY_S, true);

		if(result.type==CodeMLResult.TYPE_SITEMODEL) {
	//		 We want to run each of the models
			for(int i=0; i<result.models.size(); i++) {
				if (LocalJobs.isRunning(result.jobId) == false)
					return;
	
				File runDir = new File(jobDir, "run" + (i+1));
				runDir.mkdirs();
	
				if (result.isRemote == false)
					new CodeMLSiteAnalysis(runDir).start(LocalJobs.manager);
			}
		}
		else if(result.type==CodeMLResult.TYPE_BRANCHMODEL) {
			//		 We want to run each of the hypothesis
			for(int i=0; i<result.hypos.size(); i++) {
				if (LocalJobs.isRunning(result.jobId) == false)
					return;
	
				File runDir = new File(jobDir, "run" + (i+1));
				runDir.mkdirs();
	
				if (result.isRemote == false)
					new CodeMLBranchAnalysis(runDir).start(LocalJobs.manager);
			}
		}
		
		if (result.isRemote)
			CodeMLWebService.runScript(jobDir);
	}
}