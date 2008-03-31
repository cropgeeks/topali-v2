// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.fastml;

import java.io.*;

import topali.cluster.AnalysisThread;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class FastMLAnalysis extends AnalysisThread
{

	FastMLResult result;
	SequenceSet ss;
	
	//If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new FastMLAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	FastMLAnalysis(File runDir)
	{
		super(runDir);
	}
	
	
	public void runAnalysis() throws Exception
	{
		result = (FastMLResult) Castor
				.unmarshall(new File(runDir, "submit.xml"));
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(runDir, "ss.xml"));

//		 Sequences that should be selected/saved for processing
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(runDir, "seq.fasta"), indices, Filters.FAS, true); 

		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(runDir,  "tree.txt")));
		writer.write(result.origTree);
		writer.flush();
		writer.close();
		
		FastMLProcess proc = new FastMLProcess(runDir, result);
		proc.run();

		result = FastMLParser.parseTree(new File(runDir, "tree.newick.txt"), result); 
		result = FastMLParser.parseSeq(new File(runDir, "seq.marginal.txt"), result);
		
		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));

		//ClusterUtils.emptyDirectory(runDir, true); //was wrkDir
		
	}
	

}
