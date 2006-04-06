// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm;

import java.io.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

class PDMAnalysis extends MultiThread
{	
	private SequenceSet ss;
	
	// Directory where results will be stored (and temp files worked on)
	private File jobDir, wrkDir;
	// And settings
	private PDMResult result;

	
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
			System.out.println("PDMAnalysis: " + e);
			ClusterUtils.writeError(new File(analysis.jobDir, "error.txt"), e);
		}
	}
	
	PDMAnalysis(File jobDir)
		throws Exception
	{
		// Data directory
		this.jobDir = jobDir;

		// Read the PDMResult
		result = (PDMResult) Castor.unmarshall(new File(jobDir, "submit.xml"));
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(jobDir, "ss.xml"));
		
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
			result, jobDir.getName(), "pdm");
	}
	
	public void run()
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
			jambe.runJambe();
			
			// And the results collated
			getResults(jambe);
		}
		catch (Exception e)
		{
			if (e.getMessage().equals("cancel") == false)
				ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
		
		ClusterUtils.emptyDirectory(wrkDir, true);		
		giveToken();
	}
	
	private void getResults(Jambe2 jambe)
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
}