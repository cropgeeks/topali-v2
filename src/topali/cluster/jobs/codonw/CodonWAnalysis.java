// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.codonw;

import java.io.File;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class CodonWAnalysis extends AnalysisThread
{
	CodonWResult result;
	SequenceSet ss;
	
	//If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new CodonWAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	CodonWAnalysis(File runDir)
	{
		super(runDir);
	}
	
	@Override
	public void runAnalysis() throws Exception
	{
//		 Read the SubstitutionModel
		result = (CodonWResult) Castor
				.unmarshall(new File(runDir, "submit.xml"));
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(runDir, "ss.xml"));

		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(result, runDir.getName(),
				"codonw");

//		 Sequences that should be selected/saved for processing
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(wrkDir, "codonw.fasta"), indices, Filters.FAS, false);

		CodonWProcess cp = new CodonWProcess(wrkDir, result);
		cp.run();

		result = CodonWParser.parse(new File(wrkDir, "codonw.out"), new File(wrkDir, "codonw.blk"), result);

		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));

		ClusterUtils.emptyDirectory(wrkDir, true);
		
	}
}
