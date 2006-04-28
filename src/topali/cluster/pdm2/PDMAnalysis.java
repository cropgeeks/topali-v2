// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm2;

import java.io.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

class PDMAnalysis extends MultiThread
{	
	private SequenceSet ss;
	
	// Directory where results will be stored (and temp files worked on)
	// Why two different places? Because while running on the cluster the job
	// directory is usually an NFS share - fine for writing final results to,
	// but during analysis itself it's best to write to a local HD's directory
	private File runDir, wrkDir;
	// And settings
	private PDM2Result result;
	
	// PDM object that does the (Frank) calculations
	private PDM pdm = null;
	
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
		// Data directory
		this.runDir = runDir;

		// Read the PDM2Result
		File resultFile = new File(runDir.getParentFile(), "submit.xml");
		result = (PDM2Result) Castor.unmarshall(resultFile);
		// Read the SequenceSet
		ss = new SequenceSet(new File(runDir, "pdm.fasta"));
		
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
			result,	runDir.getParentFile().getName(), runDir.getName());
	}
	
	// A PDMAnalysis must take a region of DNA and perform a PDM analysis on [n] 
	// windows along that region.
	public void run()
	{
		File pctDir = new File(runDir, "percent");
		
		System.out.println(wrkDir);
		pdm = new PDM(result, runDir, wrkDir, ss.getSize());
		
		try
		{
			int w = result.pdm_window;
			int s = result.pdm_step;			
			int tW = (int) ((ss.getLength() - w) / s) + 1;

			float[] scores = new float[tW-1];
			
			// Move along the alignment, a step (s) at a time
			for (int i = 0, p = 1; i < tW; i++, p += s)
			{
				if (LocalJobs.isRunning(result.jobId) == false)
					throw new Exception("cancel");
				
				// Strip out the window at this position "(p) to (p+w-1)"
				int winS = p;
				int winE = p+w-1;

				System.out.println("Window: " + winS + "-" + winE);

				// Save it in Nexus format (ready for MrB to use)
				File nexusFile = new File(wrkDir, "pdm.nex");
				ss.save(nexusFile, ss.getSelectedSequences(), winS, winE, Filters.NEX_B, true);
				addNexusCommands();
				
				System.out.print("Running MrB...");
				RunMrBayes mb = new RunMrBayes(wrkDir, result);
				mb.run();
				System.out.println("done");
				pdm.saveWindowResults(i+1);
				
				
				// Once we've done more than one window, we can start to compare
				// them
				if (i > 0)
				{
					new RunTreeDist().runTreeDist(wrkDir, result, i+1);
					
					long st = System.currentTimeMillis();
					scores[i-1] = pdm.doCalculations();
					long ed = System.currentTimeMillis();
					
					System.out.println("PDM ran in " + (ed-st) + "ms");
					
//					System.exit(0);
// TODO: Cancel local job for PDM2
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
		giveToken();
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

	
/*	public void run()
	{
		try
		{
			// Save the input file used by Bambe
			new BambeInfile().saveInfile(wrkDir, ss, result);
			
			// We need to save out the SequenceSet for Bambe to read, ensuring
			// that only the sequences meant to be processed are saved
			int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
			ss.save(new File(wrkDir, "dna.dat"), indices, Filters.BAM, true);
			
			
			Jambe2 jambe = new Jambe2(jobDir, wrkDir, result, ss.getLength());
			if (jambe.run())
				System.out.println("jambe.run() ok");
			else
				System.out.println("jambe.run() FAILED");
			
			// And the results collated
			getResults(jambe);

		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(runDir, "error.txt"), e);
		}
		
		ClusterUtils.emptyDirectory(wrkDir, true);		
		giveToken();
	}
*/
	
/*	private void getResults(Jambe2 jambe)
		throws Exception
	{
		// Retrieve the data arrays from Jambe
		float[] gblData = jambe.getKullbackMean();
		float[] locData = jambe.getLocalData();
		
		// Turn them into x/y arrays that can be plotted
		result.glbData = new float[gblData.length][2];
		result.locData = new float[locData.length][2];
		
		populateXaxis(gblData, result.glbData, true);
		populateXaxis(locData, result.locData, false);
		
		// Other values
		result.histograms = jambe.getHistogramArray();
		result.df = jambe.getHistogramDF();
		result.N = jambe.getN();
		
		Castor.saveXML(result, new File(jobDir, "result.xml"));
		jambe.setPercent(100);
	}

	private void populateXaxis(float[] d1d, float[][] d2d, boolean global)
	{
		int pos = 0;
		
		if (global)
			pos = 1+ (int)(result.pdm_window/2f - 0.5);
		else
			pos = 1+ (int)((result.pdm_window/2f - 0.5) + (result.pdm_step/2f));
		
		for (int i = 0; i < d1d.length; i++, pos += result.pdm_step)
		{
			d2d[i][0] = pos;
			d2d[i][1] = d1d[i];
		}
	}
*/
}