// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest;

import java.io.File;
import java.util.logging.Logger;

import topali.cluster.JobStatus;
import topali.cluster.jobs.modeltest.analysis.ModelTestParser;
import topali.data.*;
import topali.data.models.*;
import topali.fileio.Castor;

public class ModelTestMonitor
{
	private static  Logger logger = Logger.getLogger("topali.cluster.info-log");
	
	File jobDir;
	ModelTestResult result;
	
	public ModelTestMonitor(File jobDir) throws Exception {
		this.jobDir = jobDir;
		result = (ModelTestResult) Castor.unmarshall(new File(jobDir, "submit.xml"));
	}
	
	public JobStatus getPercentageComplete() throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe(jobDir.getName() + " - error.txt found");
			throw new Exception("ModelTest error.txt");
		}
		
		int fin = 0;
		for(int i=1; i<=result.models.size(); i++) {
			File runDir = new File(jobDir, "run"+i);
			File tmp = new File(runDir, "finished");
			if(tmp.exists())
				fin++;
			
			if (new File(runDir, "error.txt").exists())
			{
				logger.severe(jobDir.getName() + " - error.txt found for run "
						+ i);
				throw new Exception("ModelTest error.txt (run " + i + ")");
			}
		}
		float progress = (float)fin/(float)(result.models.size())*100f;
		return new JobStatus(progress, 0, "_status");
	}
	
	public ModelTestResult getResult() throws Exception
	{
		for(int i=1; i<=result.models.size(); i++) {
			Model model = result.models.get(i-1);
			File runDir = new File(jobDir, "run"+i);
			File file = new File(runDir, "seq_phyml_stat.txt");
			
			ModelTestParser mtp = new ModelTestParser(file);
			model.setLnl(mtp.getLnl());
			if(model.isGamma()) {
				model.setGammaCat(mtp.getGammaCat());
				model.setAlpha(mtp.getGamma());
			}
			if(model.isInv())
				model.setInvProp(mtp.getInv());
			
			if(model instanceof DNAModel) {
				DNAModel dnaModel = (DNAModel)model;
				dnaModel.setBaseFreqs(mtp.getBaseFreq());
				dnaModel.setSubRates(mtp.getSubRates());
			}
		}
		
		Castor.saveXML(result, new File(jobDir, "result.xml"));
		
		return result;
	}
	
}
