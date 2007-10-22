// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest.analysis;

import java.io.File;

import topali.cluster.*;
import topali.data.ModelTestResult;
import topali.fileio.Castor;

public class ModelTestAnalysis extends AnalysisThread
{
	String CR = System.getProperty("line.separator");
	ModelTestResult result = null;
	
	public ModelTestAnalysis(File runDir)
	{
		super(runDir);
	}

	public static void main(String[] args)
	{
		new ModelTestAnalysis(new File(args[0])).run();
	}
	
	@Override
	public void runAnalysis() throws Exception
	{
		File resultFile = new File(runDir.getParentFile(), "submit.xml");
		this.result = (ModelTestResult) Castor.unmarshall(resultFile);
	
		ProcessBuilder pb = null;
		if (ClusterUtils.isWindows){
			File batchFile = new File(runDir, "runphyml.bat");
			pb = new ProcessBuilder(batchFile.toString());
		}
		else {
			File batchFile = new File(wrkDir, "runphyml.sh");
			pb = new ProcessBuilder("sh", batchFile.toString());
		}
		
		pb.directory(runDir);
		pb.redirectErrorStream(true);

		Process proc = pb.start();
		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.start();
		
		try
		{
			proc.waitFor();
			proc.destroy();
		} catch (Exception e)
		{
			System.out.println(e);
		}
		
		(new File(runDir, "finished")).createNewFile();
	}

	
}
