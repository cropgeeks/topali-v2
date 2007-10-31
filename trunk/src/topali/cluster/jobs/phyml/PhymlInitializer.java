// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml;

import java.io.*;

import topali.cluster.*;
import topali.cluster.jobs.PhyMLCmdGenerator;
import topali.cluster.jobs.modeltest.ModelTestWebService;
import topali.cluster.jobs.modeltest.analysis.ModelTestAnalysis;
import topali.cluster.jobs.phyml.analysis.PhymlAnalysis;
import topali.data.*;
import topali.data.models.Model;
import topali.fileio.Castor;
import topali.mod.Filters;

public class PhymlInitializer extends Thread
{
	String CR = System.getProperty("line.separator");
	
	private SequenceSet ss;

	private PhymlResult result;

	// Directory where the job will run
	private File jobDir;

	public PhymlInitializer(File jobDir, SequenceSet ss, PhymlResult result)
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
			Castor.saveXML(result, new File(jobDir, "submit.xml"));
			Castor.saveXML(ss, new File(jobDir, "ss.xml"));
			// Sequences that should be selected/saved for processing
			int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
			//Store alignment
			ss.save(new File(jobDir, "seq"), indices, Filters.PHY_I, false);
			
			Model model = result.model;
			
			//Store run scripts
			if (ClusterUtils.isWindows){
				writeDosScripts(model, result.bootstrap, jobDir);
			}
			else {
				writeUnixScript(model, result.bootstrap, jobDir);
			}
			
			if (result.isRemote)
				PhymlWebService.runScript(jobDir);
			else
				new PhymlAnalysis(jobDir).start(LocalJobs.manager);
			
		} catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}
	
	private void writeUnixScript(Model m, int bs, File runDir) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
		"runphyml.sh")));
		
		out.write(result.phymlPath + " << END1" + CR);
		out.write(PhyMLCmdGenerator.getModelCmd("seq", m, true, true, bs, null));
		out.write("END1" + CR);
		
		out.flush();
		out.close();
	}
	
	private void writeDosScripts(Model m, int bs, File runDir) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
		"runphyml.bat")));
		
		out.write(result.phymlPath+" < ");
		out.write("phymlinput" + CR);
		
		out.flush();
		out.close();
		
		out = new BufferedWriter(new FileWriter(new File(runDir, "phymlinput")));
		out.write(PhyMLCmdGenerator.getModelCmd("seq", m, true, true, bs, null));
		out.flush();
		out.close();
	}
	
}
