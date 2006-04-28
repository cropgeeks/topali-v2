// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm2;

import java.io.*;
import java.util.*;

import topali.data.*;

class PDM
{
	private PDM2Result result;
	private File runDir, wrkDir;
	
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
		
	PDM(PDM2Result result, File runDir, File wrkDir, int seqCount)
	{
		this.result = result;
		this.runDir = runDir;
		this.wrkDir = wrkDir;
		
		// What is the highest robinson-faulds distance we expect?
		rfMax = (2*seqCount) - 6;	
	}
	
	float doCalculations()
		throws IOException
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
				System.out.println("win2Index = " + win2Index);
				onFirstLine = false;
			}
			
			// Are we half-way through the data yet?
			if (p2 == 1)
				break;
			
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
		
		return (1-(rf/rfMax)) * (p+q);
	}

	
	// Reads a tree datafile from MrBayes and rewrites its results into a
	// simpler file format, containing first the tree and then its probability.
	// This data is written twice - to the local hard drive for more processing
	// by TreeDist, but also back to the head node where it's needed again later
	// for the threshold simulations.
	//
	// This method also updates this class's float[] set2 array with the values
	// read. The previous data in set2 is copied to set1
	void saveWindowResults(int num)
		throws Exception
	{
		// Before we write out the data, create a list to store what we read
		LinkedList<Float> data = new LinkedList<Float>();
		
		BufferedReader in = new BufferedReader(
			new FileReader(new File(wrkDir, "pdm.nex.trprobs")));
		
		System.out.println("Writing " + new File(wrkDir, "win" + num + ".txt"));
			
		// File written to the local node's hard disk
		BufferedWriter outL = new BufferedWriter(
			new FileWriter(new File(wrkDir, "win" + num + ".txt")));
		// File written to the head node's hard disk
		BufferedWriter outH = new BufferedWriter(
			new FileWriter(new File(runDir, "win" + num + ".txt")));
		
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
}