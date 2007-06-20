// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modelgenerator;

import java.io.File;

import topali.cluster.AnalysisThread;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class MGAnalysis extends AnalysisThread
{
	MGResult result;
	SequenceSet ss;
	
	//If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new MGAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	MGAnalysis(File runDir)
	{
		super(runDir);
	}
	
	@Override
	public void runAnalysis() throws Exception
	{
//		 Read the SubstitutionModel
		result = (MGResult) Castor
				.unmarshall(new File(runDir, "submit.xml"));
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(runDir, "ss.xml"));

		// Temporary working directory
//		wrkDir = ClusterUtils.getWorkingDirectory(result, runDir.getName(),
//				"modelgenerator");
		
//		 Sequences that should be selected/saved for processing
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(runDir, "mg.fasta"), indices, Filters.FAS, true); //was wrkDir

		ModelGeneratorProcess mg = new ModelGeneratorProcess(runDir, result); //was wrkDir
		mg.run();

		result = MGParser.parse(new File(runDir, "modelgenerator0.out"), result); //was wrkDir

		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));

		//ClusterUtils.emptyDirectory(runDir, true); //was wrkDir
		
	}
	
}
