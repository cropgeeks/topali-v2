// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm2;

import java.io.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

class PDMAnalysis extends MultiThread
{	
	// Gets set to true if this object can't find its data - we assume it's
	// therefore meant to do the PostAnalysis tasks
	// TODO: what if there is an error and data is missing by mistake?
	private boolean doPostAnalysis = false;

	private SequenceSet ss;
	
	// Directory where results will be stored (and temp files worked on)
	// Why two different places? Because while running on the cluster the job
	// directory is usually an NFS share - fine for writing final results to,
	// but during analysis itself it's best to write to a local HD's directory
	private File jobDir, runDir, wrkDir, treesDir;
	// And settings
	private PDM2Result result;
	
	
	// Arrays storing the probability value for each tree found
	private float[] set1;
	private float[] set2;
	
	// This is the maximum robinson-faulds distance that can be expected for
	// this dataset
	private float rfMax = 0;
	
	// This is the (overall) index that the window2 trees begin at. If "20",
	// then we know to get [i-20] as the index for a window2 tree when looking
	// in the arrays
	private int win2Index = 0;
	
	public static void main(String[] args)
	{ 
		PDMAnalysis analysis = null;
		
		try
		{
			analysis = new PDMAnalysis(new File(args[0]));
			analysis.run();
		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(analysis.runDir, "error.txt"), e);
		}
	}
	
	PDMAnalysis(File runDir)
		throws Exception
	{
		// Data (for this run) directory - may not exist (but will not be null)
		// if we're dealing with a PostAnalysis run
		this.runDir = runDir;
				
		// Job directory
		jobDir = runDir.getParentFile().getParentFile();
		System.out.println("jobDir: " + jobDir);
		
		// Read the PDM2Result
		File resultFile = new File(jobDir, "submit.xml");
		result = (PDM2Result) Castor.unmarshall(resultFile);
		
		if (runDir.exists())
		{
			// Read the SequenceSet
			ss = new SequenceSet(new File(runDir, "pdm.fasta"));
		
			// Temporary working directory
			wrkDir = ClusterUtils.getWorkingDirectory(
				result,	jobDir.getName(), runDir.getName());
		}
		else
			doPostAnalysis = true;
	}
	
	// A PDMAnalysis must take a region of DNA and perform a PDM analysis on [n] 
	// windows along that region.
	public void run()
	{
		if (doPostAnalysis)
			new PDMPostAnalysis(jobDir, result);
		else
			doMainAnalysis();
		
		giveToken();
	}
	
	private void doMainAnalysis()
	{		
		File pctDir = new File(runDir, "percent");
		
		// Create the location where we'll save the tree information
		treesDir = new File(runDir, "trees");
		treesDir.mkdirs();
		
		// What is the highest robinson-faulds distance we expect?
		rfMax = (2*ss.getSize()) - 6;
		
		try
		{
			int tW = countWindows();
			float[] scores = new float[tW-1];
			
			// Move along the alignment, a window at a time
			for (int i = 0; i < tW; i++)
			{
				if (LocalJobs.isRunning(result.jobId) == false)
					throw new Exception("cancel");
				
				// Read the window from the runDir
				File winFile = new File(runDir, "win" + (i+1) + ".nex");
				SequenceSet win = new SequenceSet(winFile);
				
				// Save it to the working directory (and add the MrB commands)
				File nexusFile = new File(wrkDir, "pdm.nex");
				win.save(nexusFile, Filters.NEX_B, true);
				addNexusCommands();
				
				System.out.print("Running MrB...");
				RunMrBayes mb = new RunMrBayes(wrkDir, result);
				mb.run();
				System.out.println("done");
				saveWindowResults(i+1);
				
				
				// Once we've done more than one window, we can start to compare
				// them
				if (i > 0)
				{
					new RunTreeDist().runTreeDistTypeA(wrkDir, result, i+1);					
					scores[i-1] = doCalculations();
				}
				
				// Write an update tracking how complete this job is
				int progress = (int) (((i+1) / (float)tW) * 100);
				ClusterUtils.setPercent(pctDir, progress);
			}
			
			writeScores(scores);
			
		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(runDir, "error.txt"), e);
		}
		
//		ClusterUtils.emptyDirectory(wrkDir, true);
	}
	
	private float doCalculations()
		throws Exception
	{
		// Summation of all PDM values calculated
		float pdmSum = 0;
		
		BufferedReader in = new BufferedReader(
			new FileReader(new File(wrkDir, "outfile")));
		
		StringTokenizer st = null;
		String line = in.readLine();
		boolean onFirstLine = true;
		
		
		while (line != null)
		{
			st = new StringTokenizer(line);
			// Integer representations of the two probabilies
			int p1 = Integer.parseInt(st.nextToken());
			int p2 = Integer.parseInt(st.nextToken());
			// And robinson-faulds distance
			int rf = Integer.parseInt(st.nextToken());
			
			if (onFirstLine)
			{
				win2Index = p2;
				onFirstLine = false;
			}
						
			// Are we half-way through the data yet?
			if (p2 == 1)
				break;
			
			if (rf == 0)
				pdmSum += (float) calculateWeightedPDM(p1, p2, rf);
			
			// Read the next line from the datafile
			line = in.readLine();
		}
		
		System.out.println("pdmSum: " + pdmSum);
		
		in.close();
		
		return pdmSum;
	}
	
	private double calculateWeightedPDM(int p1, int p2, float rf)
	{
//		System.out.println("Looking for " + p1 + " in " + set1.length + " and " + p2 + " in " + set2.length);
		
		double pK = set1[p1-1];
		double qK = set2[p2-win2Index];
		
		if (pK < 0.000001 || qK < 0.000001)
			return 0;
	
		double p = pK * Math.log(pK/qK);
		double q = qK * Math.log(qK/pK);
		
		return (1-(rf/rfMax)) * (q);//(p+q);
	}

	
	// Reads a tree datafile from MrBayes and rewrites its results into a
	// simpler file format, containing first the tree and then its probability.
	// This data is written twice - to the local hard drive for more processing
	// by TreeDist, but also back to the head node where it's needed again later
	// for the threshold simulations.
	//
	// This method also updates this class's float[] set2 array with the values
	// read. The previous data in set2 is copied to set1
	private void saveWindowResults(int num)
		throws Exception
	{
		// Before we write out the data, create a list to store what we read
		LinkedList<Float> data = new LinkedList<Float>();
		
		BufferedReader in = new BufferedReader(
			new FileReader(new File(wrkDir, "pdm.nex.trprobs")));
		
		System.out.println("Writing " + new File(wrkDir, "win" + num));
			
		// File written to the local node's hard disk
		BufferedWriter outL = new BufferedWriter(
			new FileWriter(new File(wrkDir, "win" + num)));
		// File written to the head node's hard disk	
		BufferedWriter outH = new BufferedWriter(
			new FileWriter(new File(treesDir, "win" + num)));
		
		String str = in.readLine();
		while (str != null)
		{
			str = str.trim();
			if (str.startsWith("tree"))
			{
				str = str.substring(str.indexOf("&W")+3);
				String p = str.substring(0, str.indexOf("]"));
				String t = str.substring(str.indexOf("("));
				
				data.add(Float.parseFloat(p));
				
				outL.write(t + " " + p);
				outH.write(t + " " + p);
				outL.newLine();
				outH.newLine();
			}
			
			str = in.readLine();
		}
		
		in.close();
		outL.close();
		outH.close();
		
		// Convert the list back into an array
		set1 = set2;
		set2 = convertListToArray(data);
	}
	
	// TODO: This should be possible with LinkedList.toArray(T[] a)
	private float[] convertListToArray(LinkedList<Float> data)
	{
		float[] array = new float[data.size()];
		
		int i = 0;
		for (float value: data)
			array[i++] = value;
		
		return array;
	}
	
	private void addNexusCommands()
		throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(
			new File(wrkDir, "pdm.nex"), true));
		
		out.newLine();
		out.newLine();
		out.write("begin mrbayes;");
		out.newLine();
		
		out.write("  set autoclose=yes;");
		out.newLine();
		out.write("  set nowarnings=yes;");
		out.newLine();
		out.write("  lset nst=6 rates=invgamma Ngammacat=4;");
		out.newLine();
		out.write("  mcmcp ngen=10000 printfreq=100 samplefreq=50 nchain=4 savebrlens=yes;");
		out.newLine();
		out.write("  mcmc;");
		out.newLine();
		out.write("  sumt burnin=10;");
		out.newLine();
		out.write("quit;");
		
		out.close();
	}
	
	private void writeScores(float[] scores)
		throws IOException
	{
		BufferedWriter out = new BufferedWriter(
			new FileWriter(new File(runDir, "out.xls")));
		
		for (float pdm: scores)
		{
			out.write(Float.toString(pdm));
			out.newLine();
		}
		
		out.close();
	}
	
	// Counts the number of (windowed) nexus files available for this analysis
	private int countWindows()
	{
		int count = 0;
		
		File[] files = runDir.listFiles();
		for (File f: files)
			if (f.getName().startsWith("win") && f.getName().endsWith(".nex"))
				count++;
			
		return count;
	}
}