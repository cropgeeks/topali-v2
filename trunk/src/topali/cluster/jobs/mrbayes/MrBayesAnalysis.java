// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.*;

import topali.cluster.AnalysisThread;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class MrBayesAnalysis extends AnalysisThread
{

	public static final String VERSION = "3.1";

	private SequenceSet ss;

	private MBTreeResult result;

	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new MrBayesAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	MrBayesAnalysis(File runDir)
	{
		super(runDir);
	}

	
	public void runAnalysis() throws Exception
	{
		// Read the MBTreeResult
		result = (MBTreeResult) Castor
				.unmarshall(new File(runDir, "submit.xml"));
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(runDir, "ss.xml"));

		// Temporary working directory
		// wrkDir = ClusterUtils.getWorkingDirectory(result, runDir.getName(),
		// "mrbayes");

		// We need to save out the SequenceSet for MrBayes to read, ensuring
		// that only the sequences meant to be processed are saved
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(runDir, "mb.nex"), indices,
				result.getPartitionStart(), result.getPartitionEnd(),
				Filters.NEX_B, true);

		// Add nexus commands to tell MrBayes what to do
		addNexusCommands();

		MrBayesProcess mb = new MrBayesProcess(runDir, result);
		mb.run();

		this.result = MBParser.parse(runDir, this.result);

		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));

		// ClusterUtils.emptyDirectory(wrkDir, true);
	}

	private void addNexusCommands() throws Exception
	{
		MBCmdBuilder cmd = new MBCmdBuilder();
		cmd.burnin = result.burnin;
		cmd.dna = ss.getProps().isNucleotides();
		cmd.ngen = result.nGen;
		cmd.nruns = result.nRuns;
		cmd.sampleFreq = result.sampleFreq;

		String cmds = cmd.getCmds(result);
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
		"mb.nex"), true));
		out.write("\n\n"+cmds);
		out.flush();
		out.close();
		 
		StringBuffer sb = new StringBuffer();
		sb.append("Algorithm: MrBayes\n");
		sb.append("Runs: "+result.nRuns+"\n");
		sb.append("Generations: " + result.nGen + "\n");
		sb.append("Sample Freq.: " + result.sampleFreq + "\n");
		sb.append("Burnin: " + ((int) (result.burnin * 100)) + "%\n\n");
		sb.append("MrBayes Commands:\n\n");
		sb.append(cmds);
		
		sb.append("\n MrBayes output:\n");
		sb.append("\n[SUMMARY]\n");
		
		sb.append("\n\nApplication: MrBayes (Version 3.1.1)\n");
		sb
				.append("F Ronquist, JP Huelsenbeck, 2003, MrBayes 3: Bayesian phylogenetic\n"
						+ "inference under mixed models, Bioinformatics, 19(12), pp 1572-1574");

		result.info = sb.toString();
	}
}
