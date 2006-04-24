// (C) 2003-2006 Dirk Husmeier & Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm;

import java.io.*;

import topali.cluster.*;
import topali.data.*;

public class Jambe2
{
	private boolean writeToFile = true;
	private boolean analysisOK = true;
	
	private int windowSize = 0;
	private int stepSize = 0;
	private int nFirst = 1;
	private int nLast = 0;
	private int N = 0;
	private int nDiscard = 0;
	private int alignmentLength = 0;
	
	private PhyloData oPhyloData = null;
	private AutoCorrelationTime oAutoCorrelationTime = null;
	private Kullback oKullback = null;
	private Entropy oEntropy;
		
	// Stores a set of bootstrap results created by running Jambe multiple times
	private float[] bootstrapValues = null;
	
	private File jobDir, wrkDir;
	private PDMResult result;
	
	// Tracks the job percentage-complete value
	private int percent;

	public Jambe2(File jobDir, File wrkDir, PDMResult result, int length)
	{
		this.jobDir = jobDir;
		this.wrkDir = wrkDir;
		this.result = result;
		
		windowSize = result.pdm_window;
		stepSize = result.pdm_step;
		nDiscard = result.pdm_burn / 200;
		alignmentLength = length;
		
		oEntropy = new Entropy(wrkDir, windowSize, stepSize, result.pdm_prune);
	}
	
	public void setBootstrapValues(float[] values)
	{
		bootstrapValues = values;
	}
	
	public float[] getBootstrapValues()
	{
		return bootstrapValues;
	}
	
	public int getTotal()
	{
		return ((alignmentLength - windowSize) / stepSize) + 1;
	}
	
	void setPercent(int value)
		throws IOException
	{
		if (value > percent)
		{
			// Create a file for each difference
			for (int i = value; i > percent; i--)
				new File(new File(jobDir, "percent"), "p" + i).createNewFile();
			
			percent = value;
		}
	}

	public void runJambe()
		throws Exception
	{
		// Number of Bambe runs required
		int total = getTotal();
	
		int nLast;
		ExtractTopos oExtractTopos= new ExtractTopos(wrkDir, nDiscard);
		
		// Read in the number of base pairs from the alignment
		oPhyloData = new PhyloData(wrkDir, 10,10); //dummy instantiation
		oPhyloData.select();
		nLast = oPhyloData.showLengthDNA()-windowSize-1;			
		
		// Error checking...
		if (nLast < nFirst)
			throw new JambeException("J01: The current settings for window "
				+ "size and step size are not suitable for running an "
				+ "analysis. Try lowering the window size.");
				
		if (total < 2)
			throw new JambeException("J02: The current settings for window "
				+ "size and step size do not allow for more than one "
				+ "window location.");
		
		
		// Initialize an array to hold each window pair's local statistic value
		oEntropy.createLocalArray(total-1);
//			oEntropy.setHistogramPanel(dialog.getHistogramPanel());
		
		// Instantiate an object to compute the autocorrelation time
		oAutoCorrelationTime = new AutoCorrelationTime(wrkDir, result);

		RunBambe oRunBambe = new RunBambe(wrkDir);	

		// Now start with the program ...
		for (int i = 0; i < total; i++)
		{				
			if (LocalJobs.isRunning(result.jobId) == false)
				throw new Exception("cancel");
		
			// Read window from the alignment
			oPhyloData = new PhyloData(wrkDir, (i*stepSize)+1, windowSize);
			oPhyloData.select();
			
			// Let the dialog know the current window position
			int n1 = (i * stepSize) + 1;
			int n2 = n1 + windowSize - 1;
//				if (updateGUI)
//					dialog.setValue(i, n1, n2);
//				else
//					dialog.setValue(count * total + i);
			
			// Run Bambe;
			oRunBambe.runBambe();
			
			// Extract topologies
			if (!oExtractTopos.addTopo(writeToFile))
				throw new JambeException("oExtractTopos.addTopo failed");
			
			
			/////
/*				if (updateGUI)
			{
				oEntropy = computeEntropy(oExtractTopos, i);
				if (oEntropy == null)
					return false;
			
				dialog.getLocalGraph().setData(
					oEntropy.getLocalData(), n1 + stepSize);
			}
*/				/////
						
			// Compute autocorrelation time
			oAutoCorrelationTime.addACT();
			
			
			// We set the percentage as i/total. i starts as 0 so will
			// always be one less than the actual value - this allows us to
			// make the "pruning" step the final 1% of any job
			setPercent((int)(i/(float)total*100));
		}
		
		if (result.pdm_prune)
		{
			System.out.println("Starting pruning...");
			long s = System.currentTimeMillis();
			PrunePDM prune = new PrunePDM(wrkDir, result, oEntropy, 6);
			prune.doPruning();
			if (prune.exception != null)
				throw prune.exception;
			System.out.println("Pruned in " + (System.currentTimeMillis()-s));
		}

		
		// If we're not updating the GUI (ie, if this run formed part of a
		// threshold run, then the Entropy calculations can just be run
		// once)
//			if (!updateGUI)
		{
			for (int i = 0; i < total; i++)
			{
				oEntropy = computeEntropy(oExtractTopos, i);
				if (oEntropy == null)
					throw new JambeException("oEntropy is null");
			}
		}
		
		// Kullback-Leibler divergence
		oKullback= new Kullback(wrkDir, oEntropy);
		oKullback.doItAll();
		
		// Compute average autocorrelation time
		oAutoCorrelationTime.printOutAll(); // all the individual ACTs
		oAutoCorrelationTime.printOutResults();
					
		N = oAutoCorrelationTime.effectiveSampleSizeApprox();
	}
		
	public float[] getKullbackMean()
	{
		return oKullback.getKullbackMean();
	}
	
//	public Entropy getEntropy()
//	{
//		return oEntropy;
//	}
	
	public int getHistogramDF() { return oEntropy.getHistogramDF(); }
	public float[] getLocalData() { return oEntropy.getLocalData(); }
	public float[][] getHistogramArray() { return oEntropy.getHistogramArray(); }
	
	public int getN()
	{
		return N;
	}
	
	private Entropy computeEntropy(ExtractTopos oExtractTopos, int window)
		throws Exception
	{
		// TODO: Error checking within the Entropy functions
		
		if (writeToFile)
			// Reads in topology strings from file
			analysisOK = oEntropy.getTopologyStrings();
		else
			// Reads in topology strings from array
			analysisOK = oEntropy.getTopologyStrings(oExtractTopos.showTopos());
			
		oEntropy.doItAll(window);
		
		
		if (!analysisOK)
			return null;
		
		return oEntropy;
	}
}
