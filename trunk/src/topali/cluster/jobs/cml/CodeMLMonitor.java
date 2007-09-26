// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.File;
import java.util.logging.Logger;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class CodeMLMonitor
{
	private static  Logger logger = Logger.getLogger("topali.cluster.info-log");

	private File jobDir;

	CodeMLResult result = null;
	
	public CodeMLMonitor(File jobDir) throws Exception
	{
		this.jobDir = jobDir;
	}

	public JobStatus getPercentageComplete() throws Exception
	{
		
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe(jobDir.getName() + " - error.txt found");
			throw new Exception("CML error.txt");
		}

		if(result==null) {
			result = (CodeMLResult) Castor.unmarshall(new File(jobDir,
			"submit.xml"));
		}
		
		String text = "";

		if(result.type.equals(CodeMLResult.TYPE_SITEMODEL)) {
			int failedJobs = 0;
			for (int i = 0; i < result.models.size(); i++)
			{
				File runDir = new File(jobDir, "run" + (i+1));
				boolean finished = new File(runDir, "model.xml").exists();
				boolean failed = new File(runDir, "error.txt").exists();
				
				if(failed) {
					text += result.models.get(i).model + "=error ";
				}
				else {
					if(finished) 
						text += result.models.get(i).model + "=true ";
					else
						text += result.models.get(i).model + "=false ";
				}
	
				if(failed) {
					failedJobs++;
					if(failedJobs==result.hypos.size()) {
						logger.severe(jobDir.getName() + " - all jobs failed");
						throw new Exception("All CML jobs failed.");
					}
					else
						logger.warning(jobDir.getName() + " - error.txt found for run "+ i);
				}
			}
		}
		else if(result.type.equals(CodeMLResult.TYPE_BRANCHMODEL)) {
			int failedJobs = 0;
			for (int i = 0; i < result.hypos.size(); i++)
			{
				File runDir = new File(jobDir, "run" + (i+1));
				boolean finished = new File(runDir, "hypo.xml").exists();
				boolean failed = new File(runDir, "error.txt").exists();
				
				if(failed) {
					text += "H"+i+"=error ";
				}
				else {
					if(finished)
						text += "H"+i+"=true ";
					else
						text += "H"+i+"=false ";
				}
	
				if(failed) {
					failedJobs++;
					if(failedJobs==result.hypos.size()) {
						logger.severe(jobDir.getName() + " - all jobs failed");
						throw new Exception("All CML jobs failed.");
					}
					else
						logger.warning(jobDir.getName() + " - error.txt found for run "+ i);
				}
			}
		}
		
		if (text.contains("false") || text.equals(""))
			return new JobStatus(0, 0, text);
		else
			return new JobStatus(100f, 0, text);
	}

	public CodeMLResult getResult() throws Exception
	{

		if(result==null) {
			result = (CodeMLResult) Castor.unmarshall(new File(jobDir,
			"submit.xml"));
		}
		
		if(result.type.equals(CodeMLResult.TYPE_SITEMODEL)) {
//			 Populate it with the results from each run
			int max = result.models.size();
			for (int i = 0; i < max; i++)
			{	
				File runDir = new File(jobDir, "run" + (i+1));
				File modelFile = new File(runDir, "model.xml");
				File errorFile = new File(runDir, "error.txt");
			
				result.models.remove(0);
				if(modelFile.exists() && !errorFile.exists()) {
					CMLModel model = (CMLModel) Castor.unmarshall(modelFile);
					result.models.add(model);
				}
			}
//			Throw away repeated models with bad likeklihood
			result.filterModels();
		}
		else if(result.type.equals(CodeMLResult.TYPE_BRANCHMODEL)) {
			int max = result.hypos.size();
			for (int i = 0; i < max; i++)
			{	
				File runDir = new File(jobDir, "run" + (i+1));
				File hypoFile = new File(runDir, "hypo.xml");
				File errorFile = new File(runDir, "error.txt");
				
				result.hypos.remove(0);
				
				if(hypoFile.exists() && !errorFile.exists()) {
					CMLHypothesis hypo = (CMLHypothesis) Castor.unmarshall(hypoFile);
					result.hypos.add(hypo);
				}
			}
		}
		// Return it
		return result;
	}
}