// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.*;

import topali.cluster.AnalysisThread;
import topali.data.*;
import topali.data.models.*;
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

	@Override
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

		readTree();

		readSummary();

		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));

		// ClusterUtils.emptyDirectory(wrkDir, true);
	}

	private void addNexusCommands() throws Exception
	{
		MBCmdBuilder cmd = new MBCmdBuilder();
		cmd.burnin = result.burnin;
		cmd.dna = ss.isDNA();
		cmd.ngen = result.nGen;
		cmd.nruns = result.nRuns;
		cmd.sampleFreq = result.sampleFreq;

		String cmds = cmd.getCmds(result.partitions);
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
		sb.append("MrBayes Commands:\n");
		sb.append(cmds);
		
		sb.append("\n[SUMMARY]\n");
		
		sb.append("\n\nApplication: MrBayes (Version 3.1.1)\n");
		sb
				.append("F Ronquist, JP Huelsenbeck, 2003, MrBayes 3: Bayesian phylogenetic\n"
						+ "inference under mixed models, Bioinformatics, 19(12), pp 1572-1574");

		result.info = sb.toString();
	}

	private void readTree() throws Exception
	{

		File mbFile = new File(runDir, "mb.nex.con");
		if (mbFile.exists() == false)
			throw new Exception("MrBayes did not create a tree file");

		String treeStr = null;

		BufferedReader in = new BufferedReader(new FileReader(mbFile));
		String str = in.readLine();
		while (str != null)
		{
			if (str.startsWith("   tree"))
			{
				treeStr = str.substring(str.indexOf("=") + 2);
				break;
			}

			str = in.readLine();
		}

		in.close();

		if (treeStr == null)
			throw new Exception("No tree found in mb.nex.con");

		// Update the result
		result.setTreeStr(treeStr);

		// TODO: Is this worth it (other than a percentage tracker)?
		// And write the data to tree.txt too
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
				"tree.txt")));
		out.write(treeStr);
		out.close();
	}

	private void readSummary() throws Exception
	{
		File f = new File(runDir, "summary.txt");
		BufferedReader in = new BufferedReader(new FileReader(f));

		StringBuffer sb = new StringBuffer();
		boolean warn = false;
		String line = null;
		while ((line = in.readLine()) != null) {
			sb.append(line+"\n");
			
			try
			{
				String[] split = line.split("\\s+");
				double d = Double.parseDouble(split[8]);
				if(d>1.2) {
					warn = true;
				}
			} catch (RuntimeException e)
			{
				//just ignore NullPointer and NumberFormatExceptions here 
			}
		}

		
		result.summary = sb.toString();
		
		String tmp = "\nSummary statistics for taxon bipartitions:\n";
		tmp += result.summary;
		tmp += "\n";
		result.info = result.info.replaceFirst("\\[SUMMARY\\]", tmp);
		
		if(warn) {
			result.warning = "One or more PSRF values are greater than 1.2!\n" +
					"This means the MCMC chains may have not converged.\n" +
					"Please rerun the analysis with a higher number of\n" +
					"generations and/or longer burnin period.\n" +
					"(For further details view tree and click on information icon)";
		}
	}
}
