// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm;

import java.io.*;
import java.util.*;

import pal.distance.ReadDistanceMatrix;
import pal.tree.*;
import topali.data.PDMResult;

/*
 * The original PDM method resulted in a histogram of topology scores, where
 * each column represents the proportion of trees of that type. Each row in the
 * histogram array corresponds to one window (as PDM moves along the alignment).
 * The pruning method combines columns which have similar trees in order to
 * reduce noise in the final graphs.
 *
 * Pruning works by
 *  a) estimating pairwise distances (Robinson Foulds) between tree topologies
 *     (using TreeDist)
 *  b) estimating a UPGMA dendrogram from the pairwise RF distances
 *  c) determining at what depth the dendrogram is clustered into [n] groups
 *  d) creating a list of these groups, where each group contains the names of
 *     the nodes within it
 *  e) using these groups to combine those nodes (ie the columns in the original
 *     histogram) into a single (new) column whose score is the sum of the nodes
 */

class PrunePDM
{
	// Entropy class containing the original histogram that will get pruned
	private Entropy oEntropy;

	// Clustered UPGMA tree/dendrogram
	private ClusterTree tree;

	// A list of clustered groups (where each group is an array of node names)
	private Vector<Object> groups = new Vector<Object>();

	// The number of groups we want to try and find
	private int groupCount;

	// Any errors?
	Exception exception;

	private File wrkDir;

	private PDMResult result;

	PrunePDM(File wrkDir, PDMResult result, Entropy e, int gc)
	{
		this.wrkDir = wrkDir;
		this.result = result;

		oEntropy = e;
		groupCount = gc;
	}

	void doPruning() throws Exception
	{
		runTreeDist(); // (a)
		parseTreeDistOutfile();

		// If the number of topologoes in the histogram is already <= to the
		// desired pruning amount, there is not point in pruning
		float[][] oldHistogram = oEntropy.getHistogramArray();
		if (oldHistogram[0].length <= groupCount)
		{
			System.out.println("No pruning required");
			return;
		}

		createUPGMATree(); // (b)

		double bestDistance = getDistanceForGroups(groupCount); // (c)
		createGroups(bestDistance); // (d)

		float[][] newHistogram = computeNewHistogram(oldHistogram); // (e)
		oEntropy.setPrunedHistogramArray(newHistogram);
	}

	private void runTreeDist() throws Exception
	{
		// Step 1
		System.out.println("PRUNE: call setIntMatrix(File, File)");
		oEntropy.setIntMatrix(new File(wrkDir, "resultsAllTopos.out"),
				new File(wrkDir, "resultsAllToposInt.out"));

		System.out.println("PRUNE: " + oEntropy.showNTopos());

		// Step 2
		// Write out the intree file
		System.out.println("PRUNE: call getTopologyStrings()");
		if (oEntropy.getTopologyStrings() == false)
			throw new Exception("Can't create intree file");

		System.out.println("PRUNE: " + oEntropy.showNTopos());

		// Write out the histogram
		oEntropy.getHistogram();
		// TODO: is this step required?
		oEntropy.showHistogram(new File(wrkDir, "results_histo.out"));

		// Step 3
		RunTreeDist td = new RunTreeDist();
		td.runTreeDist(wrkDir, result);
	}

	// Reads the (too human-friendly) output from TreeDist and converts it into
	// a Phylip-formatted distance matrix that PAL will be able to read
	private void parseTreeDistOutfile() throws Exception
	{
		File inFile = new File(wrkDir, "outfile");
		File outFile = new File(wrkDir, "distmatrix.txt");

		Exception exception = null;
		BufferedReader in = null;
		BufferedWriter out = null;

		try
		{
			in = new BufferedReader(new FileReader(inFile));
			out = new BufferedWriter(new FileWriter(outFile));

			out.write("  " + oEntropy.getHistogramArray()[0].length);

			int tree = -1;

			String str = in.readLine();
			while (str != null && str.length() > 0)
			{
				int[] nums = getNumbers(str);

				if (nums[0] != tree)
				{
					out.newLine();
					out.write((tree = nums[0]) + " ");
				}
				out.write(nums[2] + " ");

				str = in.readLine();
			}

		} catch (Exception e)
		{
			exception = e;
		}

		try
		{
			in.close();
			out.close();
		} catch (IOException e)
		{
		}

		if (exception != null)
			throw exception;
	}

	// Helper method for the above
	private int[] getNumbers(String str) throws Exception
	{
		StringTokenizer st = new StringTokenizer(str);
		int[] array = new int[st.countTokens()];

		for (int i = 0; st.hasMoreTokens(); i++)
			array[i] = Integer.parseInt(st.nextToken());

		return array;
	}

	// Uses PAL to perform UPGMA clustering on the TreeDist produced distance
	// matrix in order to create a dendrogram
	private void createUPGMATree() throws Exception
	{
		// Read in the distance matrix created from TreeDist
		File inFile = new File(wrkDir, "distmatrix.txt");
		ReadDistanceMatrix dm = new ReadDistanceMatrix(inFile.getPath());

		tree = new ClusterTree(dm, ClusterTree.UPGMA);
	}

	// (RECURSIVE) Works out the total distance (tree-depth) across the tree
	private double getNodeDistance(Node node)
	{
		if (node.getChildCount() == 0)
			return node.getBranchLength();
		else
			return node.getBranchLength() + getNodeDistance(node.getChild(0));
	}

	// Returns at what distance the tree must be clustered to get the given
	// number of groups
	private double getDistanceForGroups(int numGroups)
	{
		double treeDepth = getNodeDistance(tree.getRoot());
		double increment = treeDepth / 50d;

		double bestDistance = 0;
		for (double depth = 0; depth < treeDepth; depth += increment)
		{
			int groups = countGroups(tree.getRoot(), depth, 0);
			System.out.println("At " + depth + ", count is " + groups);

			if (groups <= numGroups)
				bestDistance = depth;

			if (groups >= numGroups)
				break;
		}

		System.out.println();
		System.out.println("Best distance for " + numGroups + " is "
				+ bestDistance);

		return bestDistance;
	}

	// Determines if this node is beyond the cutoff depth or not. If it is, then
	// it is assumed that all nodes below this one form a single group. If not,
	// then we need to return the number of other groups further down the tree
	private int countGroups(Node node, double cutoffDepth, double currentDepth)
	{
		currentDepth = currentDepth + node.getBranchLength();

		// If the current depth is greater than the current cutoff, then assume
		// a group has been found, and return
		// Same for leaf nodes - if they've been reached, then obviously each
		// node is a group...
		if (currentDepth >= cutoffDepth || node.isLeaf())
			return 1;

		// Otherwise, we need to count the number of groups below this node
		int count = 0;
		for (int i = 0; i < node.getChildCount(); i++)
			count += countGroups(node.getChild(i), cutoffDepth, currentDepth);

		return count;
	}

	// Uses the optimum distance value to create the array of clustered groups
	private void createGroups(double cutoffDepth)
	{
		// Work out the groups...
		getGroups(tree.getRoot(), null, cutoffDepth, 0);

		// ...then rewrite the array to have integer numbers (corresponding to
		// the histogram columns) rather than strings
		for (int i = 0; i < groups.size(); i++)
		{
			Vector<?> nodes = (Vector<?>) groups.get(i);
			int[] array = new int[nodes.size()];

			System.out.println("Group:");

			for (int j = 0; j < nodes.size(); j++)
			{
				// NOTE: the -1 so that the tree index becomes a proper array
				// index (ie tree 5 is location 4 in the original histogram)
				array[j] = Integer.parseInt((String) nodes.get(j)) - 1;
				System.out.print("  " + array[j] + " ");
			}

			groups.set(i, array);

			System.out.println();
		}
	}

	private void getGroups(Node node, Vector<Object> nodes, double cutoffDepth,
			double currentDepth)
	{
		currentDepth = currentDepth + node.getBranchLength();

		if (currentDepth >= cutoffDepth && nodes == null)
		{
			nodes = new Vector<Object>();
			groups.add(nodes);
		}

		if (node.isLeaf())
			nodes.add(node.getIdentifier().getName());

		// And continue traversing down the tree
		for (int i = 0; i < node.getChildCount(); i++)
		{
			getGroups(node.getChild(i), nodes, cutoffDepth, currentDepth);

			// Resetting the nodes list if necassary
			if (currentDepth < cutoffDepth)
				nodes = null;
		}
	}

	// Uses the group information retrieved from the clustered UPGMA tree to
	// recreate the Entropy object's histogram array, so that (eg) columns 1,2
	// and 3 and 4,5, and 6 become two new columns where {1+2+3} is column 1 and
	// {4+5+6} becomes column two. THIS IS THE PRUNING
	private float[][] computeNewHistogram(float[][] oH)
	{
		float[][] nH = new float[oH.length][groups.size()];

		// For every row (window)
		for (int row = 0; row < oH.length; row++)
		{
			// For (each group)
			for (int g = 0; g < groups.size(); g++)
			{
				int[] nodes = (int[]) groups.get(g);
				float newValue = 0;

				// For each node
				for (int i = 0; i < nodes.length; i++)
					newValue += oH[row][nodes[i]];

				nH[row][g] = newValue;
			}
		}

		System.out.println("NEW HISTOGRAM: " + nH.length + " by "
				+ groups.size());

		/*
		 * try { BufferedWriter out = new BufferedWriter(new FileWriter( new
		 * File(Prefs.mcmc_scratch, "new_histo.txt")));
		 * 
		 * for (int row = 0; row < nH.length; row++) { for (int col = 0; col <
		 * nH[row].length; col++) out.write(d.format(nH[row][col]) + " ");
		 * out.newLine(); }
		 * 
		 * out.close(); } catch (Exception e) {}
		 */
		return nH;
	}
}