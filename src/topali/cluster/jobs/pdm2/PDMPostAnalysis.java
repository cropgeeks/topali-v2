// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm2;

import java.io.*;
import java.util.*;

import topali.cluster.ClusterUtils;
import topali.data.PDM2Result;
import topali.fileio.Castor;

class PDMPostAnalysis
{
	private File jobDir, wrkDir;

	private Hashtable<String, TreeScore> treeTable = new Hashtable<String, TreeScore>(
			500);

	private TreeScore[] trees;

	private int windowCount = 0;

	private float[] thresholds;

	PDMPostAnalysis(File jobDir, PDM2Result result)
	{
		try
		{
			this.jobDir = jobDir;

			// Temporary working directory
			wrkDir = ClusterUtils.getWorkingDirectory(result, jobDir.getName(),
					"pdm2_post");

			doPostAnalysis();
		} catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}

	private void doPostAnalysis() throws Exception
	{
		System.out.println("Running PDM2PostAnalysis");

		// Nothing can be done until the main alignment run is complete
		float progress = new PDMMonitor(jobDir).getParallelPercentageComplete();
		while (progress < 100f)
		{
			try
			{
				Thread.sleep(5000);
			} catch (InterruptedException e)
			{
			}

			System.out.println("progress=" + progress);
			progress = new PDMMonitor(jobDir).getParallelPercentageComplete();
		}

		long s = System.currentTimeMillis();

		// First we read and compute summation scores for every tree found
		readTreeFiles();
		// Then we move the trees into an array (writing them to disk as well)
		createTreeArray();

		// Next, run TreeDist to remove duplicates from the array
		// runTreeDist();

		// This sorts, and sums the values in the array so it can be searched
		makeArrayCumulative();

		computeThreshold(101);

		collateResults();

		System.out.println("PostRun complete in "
				+ (System.currentTimeMillis() - s) + "ms");
	}

	private void readTreeFiles() throws Exception
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
			lastD = (r == runs.length - 1);

			File[] trees = new File(runs[r], "trees").listFiles();
			sortFileArray(trees);

			for (int t = 0; t < trees.length; t++)
			{
				lastF = (t == trees.length - 1);

				// Therefore only run if a) in last dir, or b) not on last file
				if (lastD == true || lastF == false)
					readTreeFile(trees[t]);
			}
		}
	}

	private void readTreeFile(File file) throws Exception
	{
		BufferedReader in = new BufferedReader(new FileReader(file));
		windowCount++;

		String str = in.readLine();
		while (str != null && str.length() > 0)
		{
			String treeStr = str.substring(0, str.indexOf(" "));
			String probStr = str.substring(str.indexOf(" ") + 1);

			float prob = Float.parseFloat(probStr);

			if (treeTable.containsKey(treeStr))
			{
				TreeScore t = treeTable.get(treeStr);
				t.prob += prob;

				treeTable.put(treeStr, t);
			} else
				treeTable.put(treeStr, new TreeScore(treeStr, prob));

			str = in.readLine();
		}

		in.close();
	}

	// Runs through the hashtable pulling out each element and plonking it into
	// an array of TreeScore objects. This allows for easier access when back-
	// mapping the output from TreeDist (ie number '5' to a tree 'array[5]')
	// We also write the trees to disk ready for TreeDist to use
	private void createTreeArray() throws Exception
	{
		trees = new TreeScore[treeTable.size()];
		System.out.println(trees.length + " elements in hashtable");

		Enumeration<String> keys = treeTable.keys();
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(wrkDir,
				"intree")));

		for (int i = 0; keys.hasMoreElements(); i++)
		{
			trees[i] = treeTable.remove(keys.nextElement());
			// Divide the probability score by the number of windows so that
			// they sum to 1 rather than windowCount
			trees[i].prob /= windowCount;

			System.out.println(trees[i].treeStr + "\t" + trees[i].prob);
			out.write(trees[i].treeStr);
			out.newLine();
		}

		out.close();
	}

	/*
	 * private void runTreeDist() throws Exception { long s =
	 * System.currentTimeMillis(); new RunTreeDist().runTreeDistTypeB(wrkDir,
	 * result); System.out.println("TreeDist ran in " +
	 * (System.currentTimeMillis()-s));
	 * 
	 * BufferedReader in = new BufferedReader( new FileReader(new File(wrkDir,
	 * "outfile")));
	 * 
	 * String str = in.readLine(); while (str != null) { StringTokenizer st =
	 * new StringTokenizer(str); int t1 = Integer.parseInt(st.nextToken()); int
	 * t2 = Integer.parseInt(st.nextToken()); int df =
	 * Integer.parseInt(st.nextToken());
	 * 
	 * if (t1 != t2) {
	 *  }
	 * 
	 * 
	 * str = in.readLine(); }
	 * 
	 * in.close(); }
	 */

	private void makeArrayCumulative()
	{
		Arrays.sort(trees);

		System.out.println("Array now");

		float prevValue = 0;
		for (TreeScore ts : trees)
		{
			ts.prob += prevValue;
			prevValue = ts.prob;

			System.out.println(ts.treeStr + "\t" + ts.prob);
		}
	}

	private void computeThreshold(int bootstrapCount)
	{
		long s = System.currentTimeMillis();

		float[] histo1 = null;
		float[] histo2 = null;

		thresholds = new float[bootstrapCount];

		for (int i = 0; i < (bootstrapCount + 1); i++)
		{
			histo1 = createSampledHistogram(200);

			if (i > 0)
			{
				// System.out.println();
				// System.out.println("HISTO_1\tHISTO_2");
				// for (int j = 0; j < histo1.length; j++)
				// System.out.println(" " + histo1[j] + "\t" + histo2[j]);
				// System.out.println();

				thresholds[i - 1] = computePDMFromSamples(histo1, histo2);
			}

			histo2 = histo1;
		}

		// Sort the thresholds data into accending order
		Arrays.sort(thresholds);

		System.out.println("Threshold time: "
				+ (System.currentTimeMillis() - s));
	}

	private float computePDMFromSamples(float[] histo1, float[] histo2)
	{
		float pdm = 0;

		for (int i = 0; i < histo1.length; i++)
		{
			if (histo1[i] == 0 || histo2[i] == 0)
				continue;

			pdm += histo1[i] * Math.log(histo1[i] / histo2[i]);
		}

		return pdm;
	}

	// Creates a simulated histogram by sampling values from the actual global
	// histogram. Note that the simulated histogram will still have the same
	// number of trees as the original. This allows for a quick pdm score to be
	// calculated on sim1[0] against sim2[0] and [1] against [1] etc
	private float[] createSampledHistogram(int sampleCount)
	{
		float[] histogram = new float[trees.length];

		for (int i = 0; i < sampleCount; i++)
		{
			int index = findTreeWithValue(Math.random());

			// Each time a tree is picked, we basically increase its "found"
			// count by 1. However, we want the histogram normalized so its
			// values sum to 1, therefore we actually increase by 1/sampleCount
			histogram[index] += (1.0f / (float) sampleCount);
		}

		return histogram;
	}

	// Does a very inefficient lookup of the global tree histogram to find which
	// tree should be sampled based on the given random value. Basically just
	// searches the histogram array in a linear fashion rather than doing some
	// thing clever with lookup tables.
	// TODO: something clever
	private int findTreeWithValue(double rndValue)
	{
		int index = 0;
		for (TreeScore ts : trees)
		{
			if (ts.prob >= rndValue)
				return index;
			else
				index++;
		}

		return index - 1;
	}

	private void collateResults() throws Exception
	{
		// Read in the result object
		// TODO: Collate results so that result.xml is read rather than
		// submit.xml
		PDM2Result result = (PDM2Result) Castor.unmarshall(new File(jobDir,
				"submit.xml"));

		result.thresholds = thresholds;

		// Create a (temp) vector to hold the 'y' data we'll read from each file
		Vector<Float> v = new Vector<Float>(1000);

		File[] files = new File(jobDir, "nodes").listFiles();
		for (File f : files)
		{
			BufferedReader in = new BufferedReader(new FileReader(new File(f,
					"out.xls")));

			String str = in.readLine();
			while (str != null && str.length() > 0)
			{
				v.add(Float.parseFloat(str));
				str = in.readLine();
			}

			in.close();
		}

		// Now convert this data into a 2D array, complete with x values for
		// each of the y's we've just read
		result.locData = new float[v.size()][2];

		int pos = 1 + (int) ((result.pdm_window / 2f - 0.5) + (result.pdm_step / 2f));

		for (int i = 0; i < result.locData.length; i++, pos += result.pdm_step)
		{
			result.locData[i][0] = pos;
			result.locData[i][1] = v.get(i);
		}

		Castor.saveXML(result, new File(jobDir, "result.xml"));
		new File(jobDir, "ok").createNewFile();
	}

	/*
	 * Sorts a list of files where WE ARE ASSUMING at least some of the files
	 * are named either run[n] or win[n] (where [n] is a number). The method
	 * sorts the files using these numbers, ensuring we get an order: run1,
	 * run2, run10 rather than run1, run10, run2 which is the default return.
	 */
	private void sortFileArray(File[] files)
	{
		Arrays.sort(files, new Comparator<File>()
		{
			public int compare(File f1, File f2)
			{

				try
				{
					int num1 = Integer.parseInt(f1.getName().substring(3));
					int num2 = Integer.parseInt(f2.getName().substring(3));

					if (num1 < num2)
						return -1;
					if (num1 > num2)
						return 1;
				} catch (NumberFormatException e)
				{
				}

				return 0;
			}
		});
	}

	private static class TreeScore implements Comparable
	{
		String treeStr;

		float prob;

		boolean keepMe = true;

		TreeScore(String treeStr, float prob)
		{
			this.treeStr = treeStr;
			this.prob = prob;
		}

		// This sorts an array of these objects so that the tree with the
		// highest probabilty is first
		public int compareTo(Object o)
		{
			TreeScore other = (TreeScore) o;

			if (this.prob < other.prob)
				return 1;
			if (this.prob > other.prob)
				return -1;

			return 0;
		}
	}
}