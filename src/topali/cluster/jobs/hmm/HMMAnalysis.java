// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.hmm;

import java.io.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

class HMMAnalysis extends AnalysisThread
{	
	private SequenceSet ss;
	private HMMResult result;

	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
		{ new HMMAnalysis(new File(args[0])).run(); }
	
	// If running locally, the job will be started via a normal constructor call
	HMMAnalysis(File runDir)
		{ super(runDir); }

	
	public void runAnalysis()
		throws Exception
	{
		// Read the HMMResult
		result = (HMMResult) Castor.unmarshall(new File(runDir, "submit.xml"));
		// Read the SequenceSet
		ss = new SequenceSet(new File(runDir, "hmm.fasta"));
				
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
			result, runDir.getName(), "barce");


		// Firstly, check the alignment is Barce-compatible
		verifyForBarce();
		
		// We need to save out the SequenceSet for Barce to read, ensuring
		// that only the sequences meant to be processed are saved
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(wrkDir, "seq.phy"), indices, Filters.PHY_S, true);
		
		// Store the mosaic.in file
		saveMosaicFile(new File(wrkDir, "mosaic.in"));
		
		// Then Barce can be run
		RunBarce barce = new RunBarce(result, wrkDir, runDir);
		barce.runBarce();
		
		// And the results collated
		getResults();
		
		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));
		// And write the final percentage as 105%!
		ClusterUtils.setPercent(new File(runDir, "percent"), 105);

		ClusterUtils.emptyDirectory(wrkDir, true);		
	}
	
	private void getResults()
		throws Exception
	{
		File file = new File(wrkDir, "topol_prob.out");
		BufferedReader in = null;
		
		// Temporary objects to hold the data (its length is currently unknown)
		Vector<Float> v1 = new Vector<Float>();
		Vector<Float> v2 = new Vector<Float>();
		Vector<Float> v3 = new Vector<Float>();
		
		try
		{
			in = new BufferedReader(new FileReader(file));
			
			String str = in.readLine();
			while (str != null)
			{
				if (str.length() > 0)
				{
					StringTokenizer st = new StringTokenizer(str);
				
					v1.addElement(Float.parseFloat(st.nextToken()));
					v2.addElement(Float.parseFloat(st.nextToken()));
					v3.addElement(Float.parseFloat(st.nextToken()));
				}
				
				str = in.readLine();
			}
		}
		catch (Exception e)
		{
			throw new Exception("Results read error: " + e);
		}
		
		try { in.close(); }
		catch (IOException e) {}
		
		int S = 20;
		
		// If we get this far, then it's ok to convert the vector data into
		// normal float arrays
		result.data1 = new float[v1.size()/S][2];
		result.data2 = new float[v2.size()/S][2];
		result.data3 = new float[v3.size()/S][2];
		
		System.out.println("Storing " + (int)(v1.size()/S) + " points");
		
		// We also need to work out corresponding X values for the Ys returned
		// by Barce
		int increment = ss.getLength() / v1.size();
		int x = increment / 2;
		
		for (int i = 0, j = 0; i < result.data1.length; i++, j+=S, x+=(increment*S))
		{
			result.data1[i][0] = x;
			result.data1[i][1] = v1.elementAt(j);
			
			result.data2[i][0] = x;
			result.data2[i][1] = v2.elementAt(j);
			
			result.data3[i][0] = x;
			result.data3[i][1] = v3.elementAt(j);
		}
	}
	
	/* Writes out the mosaic.in file that BARCE requires to prior info. */
	private void saveMosaicFile(File file)
		throws Exception
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			
			// Number of segments
			out.write("" + result.bpArray.length);
			out.newLine();
			
			// Breakpoints
			for (int i = 0; i < result.bpArray.length-1; i++)
			{
				out.write("" + result.bpArray[i][0]);
				if (i < result.bpArray.length-2)
					out.write(" ");
			}
			out.newLine();
			
			// Topologies
			for (int i = 0; i < result.bpArray.length; i++)
			{
				out.write("" + result.bpArray[i][1]);
				if (i < result.bpArray.length-1)
					out.write(" ");
			}
		
			out.close();
		}
		catch (IOException e)
		{
			throw new IOException("Error while writing mosaic.in file: " + e);
		}
	}
	
	// Ensures that any alignments passed to Barce only contain A, G, C, T,
	// - or N
	private void verifyForBarce()
	{
		int count = 0;
		
		for (Sequence seq: ss.getSequences())
		{
			StringBuffer buffer = seq.getBuffer();
			
			// For each character in the sequence...
			for (int c = 0; c < buffer.length(); c++)
			{
				switch (buffer.charAt(c))
				{
					case 'A' : break; case 'C' : break;
					case 'G' : break; case 'T' : break;
					case '-' : break; case 'N' : break;
					default: 
					{
						count++;
						buffer.setCharAt(c, 'N');
					}
				}
			}
		}
	
		if (count > 0)
			System.out.println(count + " illegal character(s) replaced");
	}
}