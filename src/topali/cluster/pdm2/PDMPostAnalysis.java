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

class PDMPostAnalysis
{	
	private File jobDir, wrkDir;
	private PDM2Result result;
	
	private Hashtable<String, Float> treeTable = new Hashtable<String, Float>(500);

	PDMPostAnalysis(File jobDir, PDM2Result result)
	{
		try
		{		
			// Data directory
			this.jobDir = jobDir;
			this.result = result;

			// Temporary working directory
			wrkDir = ClusterUtils.getWorkingDirectory(
				result,	jobDir.getName(), "pdm2_post");
			
			doPostAnalysis();
		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}
	
	private void doPostAnalysis()
		throws Exception
	{
		System.out.println("Running PDM2PostAnalysis");
		
		// Nothing can be done until the main alignment run is complete
		while (new PDMMonitor(jobDir).getParallelPercentageComplete() < 100f)
		{
			try { Thread.sleep(5000); }
			catch (InterruptedException e) {}
		}
		
		readTreeFiles();
	}
	
	private void readTreeFiles()
		throws Exception
	{
		File[] runs = new File(jobDir, "nodes").listFiles();
		sortFileArray(runs);
		
		// Last directory or last file?
		// This is used because the last window in each directory (APART from
		// the last window in the last directory) is a clone of the first
		// window in the next directory - so we don't want to process them
		boolean lastD, lastF;
		
		
		for (int r = 0; r < runs.length; r++)
		{
			lastD = (r == runs.length-1);
			System.out.println(runs[r] + " - " + lastD);
						
			File[] trees = new File(runs[r], "trees").listFiles();
			sortFileArray(trees);
			
			for (int t = 0; t < trees.length; t++)
			{
				lastF = (t == trees.length-1);
				
				// Therefore only run if a) in last dir, or b) not on last file
				if (lastD == true || lastF == false)
					readTreeFile(trees[t]);
			}
		}
		
		///////////////
		
		System.out.println();
		System.out.println(treeTable.size() + " elements in hashtable");
		System.out.println();
		Enumeration<String> keys = treeTable.keys();
		
		while (keys.hasMoreElements())
		{
			String key = keys.nextElement();
			
			System.out.println(key + "\t" + treeTable.get(key));
		}
	}
	
	private void readTreeFile(File file)
		throws Exception
	{
		System.out.println("  " + file);
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		
		String str = in.readLine();
		while (str != null && str.length() > 0)
		{
			String treeStr = str.substring(0, str.indexOf(" "));
			String probStr = str.substring(str.indexOf(" ")+1);
			
			float prob = Float.parseFloat(probStr);
			
			if (treeTable.containsKey(treeStr))
			{
				float sum = treeTable.get(treeStr);
				treeTable.put(treeStr, sum + prob);
			}
			else
				treeTable.put(treeStr, prob);
			
			str = in.readLine();
		}
		
		in.close();
	}
	
	/*
	 * Sorts a list of files where WE ARE ASSUMING at least some of the files
	 * are named either run[n] or win[n] (where [n] is a number). The method
	 * sorts the files using these numbers, ensuring we get an order:
	 *   run1, run2, run10
	 * rather than
	 *   run1, run10, run2
	 * which is the default return.
	 */
	private void sortFileArray(File[] files)
	{
		Arrays.sort(files, new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				File f1 = (File) o1; 
				File f2 = (File) o2;
				
				try
				{
					int num1 = Integer.parseInt(f1.getName().substring(3));
					int num2 = Integer.parseInt(f2.getName().substring(3));
				
					if (num1 < num2) return -1;
					if (num1 > num2) return 1;
				}
				catch (NumberFormatException e) {}
				
				return 0;
			}
		});
	}
}