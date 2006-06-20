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
	
	private Hashtable<String, TreeScore> treeTable = new Hashtable<String, TreeScore>(500);
	private TreeScore[] trees;

	PDMPostAnalysis(File jobDir, PDM2Result result)
	{
		try
		{		
			this.jobDir = jobDir;
			this.result = result;
			
			// Temporary working directory
			wrkDir = ClusterUtils.getWorkingDirectory(result, jobDir.getName(), "pdm2_post");
			
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
		
		// First we read and compute summation scores for every tree found
		readTreeFiles();
		// Then we move the trees into an array (writing them to disk as well)
		createTreeArray();
		// Next, run TreeDist to remove duplicates from the array
		runTreeDist();
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
	}
	
	private void readTreeFile(File file)
		throws Exception
	{
		BufferedReader in = new BufferedReader(new FileReader(file));
		
		String str = in.readLine();
		while (str != null && str.length() > 0)
		{
			String treeStr = str.substring(0, str.indexOf(" "));
			String probStr = str.substring(str.indexOf(" ")+1);
			
			float prob = Float.parseFloat(probStr);
			
			if (treeTable.containsKey(treeStr))
			{
				TreeScore t = treeTable.get(treeStr);
				t.prob += prob;
				
				treeTable.put(treeStr, t);
			}
			else
				treeTable.put(treeStr, new TreeScore(treeStr, prob));
			
			str = in.readLine();
		}
		
		in.close();
	}
	
	// Runs through the hashtable pulling out each element and plonking it into
	// an array of TreeScore objects. This allows for easier access when back-
	// mapping the output from TreeDist (ie number '5' to a tree 'array[5]')
	// We also write the trees to disk ready for TreeDist to use
	private void createTreeArray()
		throws Exception
	{
		trees = new TreeScore[treeTable.size()];
		System.out.println(trees.length + " elements in hashtable");
		
		Enumeration<String> keys = treeTable.keys();
		BufferedWriter out = new BufferedWriter(
			new FileWriter(new File(wrkDir, "intree")));
		
		for (int i = 0; keys.hasMoreElements(); i++)
		{
			trees[i] = treeTable.remove(keys.nextElement());
			System.out.println(trees[i].treeStr + "\t" + trees[i].prob);
			
			out.write(trees[i].treeStr);
			out.newLine();			
		}
		
		out.close();
	}
	
	private void runTreeDist()
		throws Exception
	{
		long s = System.currentTimeMillis();
		new RunTreeDist().runTreeDistTypeB(wrkDir, result);
		System.out.println("TreeDist ran in " + (System.currentTimeMillis()-s));
		
		BufferedReader in = new BufferedReader(
			new FileReader(new File(wrkDir, "outfile")));
			
		String str = in.readLine();
		while (str != null)
		{
			StringTokenizer st = new StringTokenizer(str);
			int t1 = Integer.parseInt(st.nextToken());
			int t2 = Integer.parseInt(st.nextToken());
			int df = Integer.parseInt(st.nextToken());
			
			if (t1 != t2)
			{
				
			}
			
			
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
	
	private static class TreeScore
	{
		String treeStr;
		float prob;
		
		boolean keepMe = true;
		
		TreeScore(String treeStr, float prob)
		{
			this.treeStr = treeStr;
			this.prob = prob;
		}
	}
}