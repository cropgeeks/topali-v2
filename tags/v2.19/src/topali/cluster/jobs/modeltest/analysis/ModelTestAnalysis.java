// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest.analysis;

import java.io.File;

import topali.cluster.AnalysisThread;
import topali.data.ModelTestResult;
import topali.fileio.Castor;

public class ModelTestAnalysis extends AnalysisThread
{
	
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
		ModelTestResult result = (ModelTestResult) Castor.unmarshall(resultFile);
		
		System.out.println("ModelTestAnalysis runAnalysis()");
		ModelTestProcess proc = new ModelTestProcess(runDir, result);
		proc.run();
	}

	
}
