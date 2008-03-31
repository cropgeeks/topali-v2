// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.raxml.analysis;

import java.io.File;

import topali.cluster.AnalysisThread;
import topali.data.RaxmlResult;
import topali.fileio.Castor;

public class RaxmlAnalysis extends AnalysisThread
{
	
	public RaxmlAnalysis(File runDir)
	{
		super(runDir);
	}

	public static void main(String[] args)
	{
		new RaxmlAnalysis(new File(args[0])).run();
	}
	
	
	public void runAnalysis() throws Exception
	{
		File resultFile = new File(runDir.getParentFile(), "submit.xml");
		RaxmlResult result = (RaxmlResult) Castor.unmarshall(resultFile);
		RaxmlProcess proc = new RaxmlProcess(runDir, result);
		proc.run();
	}

}
