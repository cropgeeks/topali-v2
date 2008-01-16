// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest;

import java.io.File;
import java.util.logging.Logger;

import topali.cluster.JobStatus;
import topali.cluster.jobs.modeltest.analysis.*;
import topali.data.*;
import topali.data.models.*;
import topali.fileio.Castor;
import topali.var.*;
import topali.var.utils.*;

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
			
			ModelTestParser mtp = new ModelTestParser(runDir);
			
			double lnl = mtp.getLnl();
			model.setLnl(lnl);
			int df = model.getFreeParameters()+(2 * result.selectedSeqs.length - 3);
			if(model.isGamma())
				df++;
			if(model.isInv())
				df++;
			model.setAic1(MathUtils.calcAIC1(lnl, df));
			model.setAic2(MathUtils.calcAIC2(lnl, df, result.sampleSize));
			model.setBic(MathUtils.calcBIC(lnl, df, result.sampleSize));
			
			model.setTree(mtp.getTree());
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
		
		addRFDistances();
		
		Castor.saveXML(result, new File(jobDir, "result.xml"));
		
		return result;
	}
	
	private void addRFDistances() throws Exception {
		StringBuffer trees = new StringBuffer();
		
		Model bestLnl = ModelUtils.getBestModel(result.models, ModelUtils.CRIT_LNL);
		Model bestAic1 = ModelUtils.getBestModel(result.models, ModelUtils.CRIT_AIC1);
		Model bestAic2 = ModelUtils.getBestModel(result.models, ModelUtils.CRIT_AIC2);
		Model bestBic = ModelUtils.getBestModel(result.models, ModelUtils.CRIT_BIC);
		trees.append(bestLnl.getTree());
		trees.append(bestAic1.getTree());
		trees.append(bestAic2.getTree());
		trees.append(bestBic.getTree());
		
		for(int i=0; i<result.models.size(); i++) {
			trees.append(result.models.get(i).getTree());
		}
		
		TreeDistProcess proc = new TreeDistProcess();
		proc.run(jobDir, trees.toString(), result.treeDistPath);
		
		TreeDistParser pars = new TreeDistParser();
		int[][] distances = pars.parse(new File(jobDir, "outfile"));
		
		int[] distance = distances[0];
		for(int i=0; i<distance.length; i++) {
			Distance<String> dist = new Distance<String>(bestLnl.getIGName(), result.models.get(i).getIGName(), distance[i]);
			result.rfDistances.add(dist);
		}
		
		distance = distances[1];
		for(int i=0; i<distance.length; i++) {
			Distance<String> dist = new Distance<String>(bestAic1.getIGName(), result.models.get(i).getIGName(), distance[i]);
			result.rfDistances.add(dist);
		}
		
		distance = distances[2];
		for(int i=0; i<distance.length; i++) {
			Distance<String> dist = new Distance<String>(bestAic2.getIGName(), result.models.get(i).getIGName(), distance[i]);
			result.rfDistances.add(dist);
		}
		
		distance = distances[3];
		for(int i=0; i<distance.length; i++) {
			Distance<String> dist = new Distance<String>(bestBic.getIGName(), result.models.get(i).getIGName(), distance[i]);
			result.rfDistances.add(dist);
		}
	}
}
