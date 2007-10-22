// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest;

import java.io.*;

import topali.cluster.*;
import topali.cluster.jobs.modeltest.analysis.ModelTestAnalysis;
import topali.data.*;
import topali.data.models.Model;
import topali.fileio.Castor;
import topali.mod.Filters;

public class ModelTestInitializer extends Thread
{
	String CR = System.getProperty("line.separator");
	
	private SequenceSet ss;

	private ModelTestResult result;

	private File jobDir;

	public ModelTestInitializer(File jobDir, SequenceSet ss,
			ModelTestResult result)
	{
		this.jobDir = jobDir;
		this.ss = ss;
		this.result = result;
	}

	@Override
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

			for (int i = 1; i <= result.models.size(); i++)
			{
				Model model = result.models.get(i-1);
				
				if (LocalJobs.isRunning(result.jobId) == false)
					return;

				File runDir = new File(jobDir, "run" + i);
				runDir.mkdirs();

				//Store model name (just for debugging purposes)
				String modName = model.getName();
				if (model.isInv())
					modName += "+I";
				if (model.isGamma())
					modName += "+G";
				File tmp = new File(runDir, modName);
				tmp.createNewFile();

				//Store alignment
				ss.save(new File(runDir, "seq"), indices, Filters.PHY_I, false);

				//Store run scripts
				if (ClusterUtils.isWindows){
					writeDosScripts(model, runDir);
				}
				else {
					writeUnixScript(model, runDir);
				}
				
				if (result.isRemote == false)
					new ModelTestAnalysis(runDir).start(LocalJobs.manager);
			}

			if (result.isRemote)
				ModelTestWebService.runScript(jobDir, result);

		} catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}

	private void writeUnixScript(Model m, File runDir) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
		"runphyml.sh")));
		
		out.write(result.phymlPath + " << END1" + CR);
		out.write(PhyMLCmdGenerator.getModelCmd("seq", m, true, true, null));
		out.write("END1" + CR);
		
		out.flush();
		out.close();
	}
	
	private void writeDosScripts(Model m, File runDir) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
		"runphyml.bat")));
		
		out.write(result.phymlPath+" < ");
		out.write("phymlinput" + CR);
		
		out.flush();
		out.close();
		
		out = new BufferedWriter(new FileWriter(new File(runDir, "phymlinput")));
		out.write(PhyMLCmdGenerator.getModelCmd("seq", m, true, true, null));
		out.flush();
		out.close();
	}
}
