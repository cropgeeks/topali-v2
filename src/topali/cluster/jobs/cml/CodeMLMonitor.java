// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.File;
import java.util.logging.Logger;

import topali.cluster.JobStatus;
import topali.data.CodeMLModel;
import topali.data.CodeMLResult;
import topali.fileio.Castor;

public class CodeMLMonitor
{
	private static Logger logger = Logger.getLogger("topali.cluster.info-log");

	private File jobDir;

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

		String text = "";

		for (int i = 1; i <= Models.MAX; i++)
		{
			File runDir = new File(jobDir, "run" + i);

			boolean ok = new File(runDir, "model.xml").exists();
			text += Models.getModelName(i) + "=" + ok + " ";

			// But also check if an error file for this run exists
			if (new File(runDir, "error.txt").exists())
			{
				logger.severe(jobDir.getName() + " - error.txt found for run "
						+ i);
				throw new Exception("CML error.txt (run " + i + ")");
			}
		}

		if (text.contains("false"))
			return new JobStatus(0, 0, text);
		else
			return new JobStatus(100f, 0, text);
	}

	public CodeMLResult getResult() throws Exception
	{
		// Load in the original submission xml
		CodeMLResult result = (CodeMLResult) Castor.unmarshall(new File(jobDir,
				"submit.xml"));

		// Populate it with the results from each run
		for (int i = 1; i <= Models.MAX; i++)
		{
			File runDir = new File(jobDir, "run" + i);

			File modelFile = new File(runDir, "model.xml");
			CodeMLModel model = (CodeMLModel) Castor.unmarshall(modelFile);

			result.models.add(model);
		}

		// Return it
		return result;
	}
}