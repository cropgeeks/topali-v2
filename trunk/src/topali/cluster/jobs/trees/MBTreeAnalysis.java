// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.trees;

import java.io.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

public class MBTreeAnalysis extends AnalysisThread
{
	private SequenceSet ss;
	private MBTreeResult result;

	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
		{ new MBTreeAnalysis(new File(args[0])).run(); }
	
	// If running locally, the job will be started via a normal constructor call
	MBTreeAnalysis(File runDir)
		{ super(runDir); }
	
	
	public void runAnalysis()
		throws Exception
	{
		// Read the MBTreeResult
		result = (MBTreeResult) Castor.unmarshall(new File(runDir, "submit.xml"));
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(runDir, "ss.xml"));
		
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
			result, runDir.getName(), "mrbayes");

		// We need to save out the SequenceSet for MrBayes to read, ensuring
		// that only the sequences meant to be processed are saved
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(wrkDir, "mb.nex"), indices, Filters.NEX_B, true);
			
		// Add nexus commands to tell MrBayes what to do
		addNexusCommands();
		
		RunMrBayes mb = new RunMrBayes(wrkDir, result);
		mb.run();
		
		readTree();
		
		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));

	
		ClusterUtils.emptyDirectory(wrkDir, true);
	}
	
	private void addNexusCommands()
		throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(
			new File(wrkDir, "mb.nex"), true));
		
		out.newLine();
		out.newLine();
		out.write("begin mrbayes;");
		out.newLine();
		
		out.write("  lset nst=6 rates=gamma;");
		out.newLine();
		out.write("  mcmc nruns=1 ngen=10000 samplefreq=10;");
		out.newLine();
		out.write("  sump burnin=1000;");
		out.newLine();
		out.write("  sumt burnin=1000;");
		out.newLine();
		out.write("end;");
		
		out.close();
	}
	
	private void readTree()
		throws Exception
	{
		
		File mbFile = new File(wrkDir, "mb.nex.con");
		if (mbFile.exists() == false)
			throw new Exception("MrBayes did not create a tree file");
		
		String treeStr = null;
		
		BufferedReader in = new BufferedReader(new FileReader(mbFile));
		String str = in.readLine();
		while (str != null)
		{
			if (str.startsWith("   tree"))
			{
				treeStr = str.substring(str.indexOf("=")+2);
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
		BufferedWriter out = new BufferedWriter(
			new FileWriter(new File(runDir, "tree.txt")));
		out.write(treeStr);
		out.close();
	}
}