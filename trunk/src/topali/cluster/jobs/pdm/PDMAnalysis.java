// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm;

import java.io.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

class PDMAnalysis extends AnalysisThread
{	
	private SequenceSet ss;
	private PDMResult result;

	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
		{ new PDMAnalysis(new File(args[0])).run(); }
	
	// If running locally, the job will be started via a normal constructor call
	PDMAnalysis(File runDir)
		{ super(runDir); }


	public void runAnalysis()
		throws Exception
	{
		// Read the PDMResult
		File resultFile = new File(runDir.getParentFile(), "submit.xml");
		result = (PDMResult) Castor.unmarshall(resultFile);
		// Read the SequenceSet
		ss = new SequenceSet(new File(runDir, "pdm.fasta"));
		
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
		result,	runDir.getParentFile().getName(), runDir.getName());

		// Save the input file used by Bambe
//			new BambeInfile().saveInfile(wrkDir, result);
		
		// We need to save out the SequenceSet for Bambe to read, ensuring
		// that only the sequences meant to be processed are saved
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(wrkDir, "dna.dat"), indices, Filters.BAM, true);
		
		
		Jambe2 jambe = new Jambe2(runDir, wrkDir, result, ss.getLength());
		jambe.runJambe();
		
		// And the results collated
		getResults(jambe);

		ClusterUtils.emptyDirectory(wrkDir, true);		
	}
	
	private void getResults(Jambe2 jambe)
		throws Exception
	{
		// Retrieve the data arrays from Jambe
		float[] gblData = jambe.getKullbackMean();
		float[] locData = jambe.getLocalData();
		
		// Highest value found?
		float max = locData[0];
		for (float value: locData)
			if (value > max)
				max = value;
		
		// Turn them into x/y arrays that can be plotted
		result.glbData = new float[gblData.length][2];
		result.locData = new float[locData.length][2];
		
		populateXaxis(gblData, result.glbData, true);
		populateXaxis(locData, result.locData, false);
		
		// Other values
		result.histograms = jambe.getHistogramArray();
		result.df = jambe.getHistogramDF();
		result.N = jambe.getN();
		
		Castor.saveXML(result, new File(runDir, "result.xml"));
		ClusterUtils.setPercent(new File(runDir, "percent"), 105);
		
		// Save the value of the highest local point found to a file
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir, "max.txt")));
		out.write("" + max);
		out.close();
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