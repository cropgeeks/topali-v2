// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.File;
import java.util.logging.Logger;

import topali.cluster.JobStatus;
import topali.data.*;
import topali.fileio.Castor;

public class CodeMLMonitor
{
	private static Logger logger = Logger.getLogger("topali.cluster.info-log");

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

		if(result.type==CodeMLResult.TYPE_SITEMODEL) {
			for (int i = 0; i < result.models.size(); i++)
			{
				File runDir = new File(jobDir, "run" + (i+1));
	
				boolean ok = new File(runDir, "model.xml").exists();
				text += result.models.get(i).abbr + "=" + ok + " ";
	
				// But also check if an error file for this run exists
				if (new File(runDir, "error.txt").exists())
				{
					logger.severe(jobDir.getName() + " - error.txt found for run "
							+ i);
					throw new Exception("CML error.txt (run " + (i+1) + ")");
				}
			}
		}
		else if(result.type==CodeMLResult.TYPE_BRANCHMODEL) {
			for (int i = 0; i < result.hypos.size(); i++)
			{
				File runDir = new File(jobDir, "run" + (i+1));
	
				boolean ok = new File(runDir, "hypo.xml").exists();
				text += "H"+i+"=" + ok + " ";
	
				// But also check if an error file for this run exists
				if (new File(runDir, "error.txt").exists())
				{
					logger.severe(jobDir.getName() + " - error.txt found for run "
							+ i);
					throw new Exception("CML error.txt (run " + (i+1) + ")");
				}
			}
		}
		
		if (text.contains("false"))
			return new JobStatus(0, 0, text);
		else
			return new JobStatus(100f, 0, text);
	}

	public CodeMLResult getResult() throws Exception
	{
//		// Load in the original submission xml
//		CodeMLResult result = (CodeMLResult) Castor.unmarshall(new File(jobDir,
//				"submit.xml"));

		if(result==null) {
			result = (CodeMLResult) Castor.unmarshall(new File(jobDir,
			"submit.xml"));
		}
		
		if(result.type==CodeMLResult.TYPE_SITEMODEL) {
//			 Populate it with the results from each run
			for (int i = 0; i < result.models.size(); i++)
			{
				File runDir = new File(jobDir, "run" + (i+1));
	
				File modelFile = new File(runDir, "model.xml");
				CMLModel model = (CMLModel) Castor.unmarshall(modelFile);
	
				result.models.remove(i);
				result.models.add(i, model);
			}
//			Throw away repeated models with bad likeklihood
			result.filterModels();
		}
		else if(result.type==CodeMLResult.TYPE_BRANCHMODEL) {
			for (int i = 0; i < result.hypos.size(); i++)
			{
				File runDir = new File(jobDir, "run" + (i+1));
	
				File hypoFile = new File(runDir, "hypo.xml");
				CMLHypothesis hypo = (CMLHypothesis) Castor.unmarshall(hypoFile);
	
				result.hypos.remove(i);
				result.hypos.add(i, hypo);
			}
		}
		// Return it
		return result;
	}
}