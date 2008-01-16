// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.lrt;

import java.util.ArrayList;

import pal.alignment.*;
import pal.distance.DistanceMatrix;
import pal.eval.LikelihoodValue;
import pal.misc.Identifier;
import pal.substmodel.SubstitutionModel;
import pal.tree.*;
import topali.analyses.TreeUtilities;
import topali.data.LRTResult;

public class LRT
{
	public static final int METHOD_JC = 1;

	public static final int METHOD_F84 = 2;

	private SimpleAlignment[] windows;

	private LRTResult result;

	private double alpha, ratio;

	private double gapThreshold;
	
	public LRT(LRTResult result, SimpleAlignment[] windows, double alpha,
			double ratio, double gapThreshold)
	{
		this.result = result;
		this.windows = windows;

		this.alpha = alpha;
		this.ratio = ratio;
		this.gapThreshold = gapThreshold;
	}

	double calculate() throws Exception
	{
		if(gapThreshold<1);
			removeGapSequences();
		
		// Calculate distances for the two windows
		DistanceMatrix dm1 = getDistance(windows[0], result.method);
		Tree t1 = new NeighborJoiningTree(dm1);
		double LnL1 = getLikelihood(windows[0], t1);

		DistanceMatrix dm2 = getDistance(windows[1], result.method);
		Tree t2 = new NeighborJoiningTree(dm2);
		double LnL2 = getLikelihood(windows[1], t2);

		// And the distance for the two windows combined
		DistanceMatrix dm3 = getDistance(windows[2], result.method);
		Tree t3a = new NeighborJoiningTree(dm3);
		Tree t3b = new NeighborJoiningTree(dm3);

		// ChiSquareValue cs = new ChiSquareValue(dm1, false);
		// cs.setTree(new UnconstrainedTree(t3a));
		// cs.optimiseParameters();
		// Tree t3_1 = cs.getTree();
		double LnL3_1 = getLikelihood(windows[0], t3a);

		// cs = new ChiSquareValue(dm2, false);
		// cs.setTree(new UnconstrainedTree(t3b));
		// cs.optimiseParameters();
		// Tree t3_2 = cs.getTree();
		double LnL3_2 = getLikelihood(windows[1], t3b);

		// Scale (and multiply) each distance matrix
		// scaleMulDistanceMatrix(dm1, result.avgDist /
		// getAverageDistance(dm1));
		// scaleMulDistanceMatrix(dm2, result.avgDist /
		// getAverageDistance(dm2));

		return -2 * ((LnL3_1 + LnL3_2) - (LnL1 + LnL2));

		/*
		 * RunFitch.saveData(wrkDir, 1, dm1, tree1); RunFitch.saveData(wrkDir,
		 * 2, dm2, tree1);
		 *  // Backward if (result.passCount == TWO_PASS) { Tree tree2 = new
		 * NeighborJoiningTree(dm2); RunFitch.saveData(wrkDir, 3, dm2, tree2);
		 * RunFitch.saveData(wrkDir, 4, dm1, tree2); }
		 */

	}

	private void removeGapSequences () {
		//Determine sequences which exceed gap treshold
		ArrayList<Integer> removeSeqPos = new ArrayList<Integer>(windows[0].getSequenceCount());
		for(int i=0; i<windows[0].getSequenceCount(); i++) {
			char[] seq = windows[0].getAlignedSequenceString(i).toCharArray();
			int count = 0;
			for(char c : seq) {
				if(c==Alignment.GAP)
					count++;
			}
			double gaps = ((double)count/(double)(seq.length));
			if(gaps>gapThreshold)
				removeSeqPos.add(new Integer(i));
		}
		
		for(int i=0; i<windows[1].getSequenceCount(); i++) {
			char[] seq = windows[1].getAlignedSequenceString(i).toCharArray();
			int count = 0;
			for(char c : seq) {
				if(c==Alignment.GAP)
					count++;
			}
			double gaps = ((double)count/(double)(seq.length));
			if(gaps>gapThreshold && !removeSeqPos.contains(new Integer(i)))
				removeSeqPos.add(new Integer(i));
		}
		
		//create new alignments without the bad sequences
		int newSize = windows[0].getSequenceCount()-removeSeqPos.size();
		Identifier[] ids1 = new Identifier[newSize];
		Identifier[] ids2 = new Identifier[newSize];
		Identifier[] ids3 = new Identifier[newSize];
		String[] seqs1 = new String[newSize];
		String[] seqs2 = new String[newSize];
		String[] seqs3 = new String[newSize];
		for(int i=0, j=0; i<windows[0].getSequenceCount(); i++) {
			if(!removeSeqPos.contains(new Integer(i))) {
				ids1[j] = windows[0].getIdentifier(i);
				ids2[j] = windows[1].getIdentifier(i);
				ids3[j] = windows[2].getIdentifier(i);
				seqs1[j] = windows[0].getAlignedSequenceString(i);
				seqs2[j] = windows[1].getAlignedSequenceString(i);
				seqs3[j] = windows[2].getAlignedSequenceString(i);
				j++;
			}
			else {
				System.out.println("Leaving out seq "+i);
			}
		}
		this.windows[0] = new SimpleAlignment(ids1, seqs1, windows[0].getDataType());
		this.windows[1] = new SimpleAlignment(ids2, seqs2, windows[1].getDataType());
		this.windows[2] = new SimpleAlignment(ids3, seqs3, windows[2].getDataType());
	}
	
	private double getLikelihood(Alignment alignment, Tree tree)
	{
		SubstitutionModel sm = null;

		if (result.method == METHOD_JC)
			sm = TreeUtilities.getJCSubstitutionModel(alignment);
		else if (result.method == METHOD_F84)
			sm = TreeUtilities.getF84SubstitutionModel(alignment, ratio, alpha);

		SitePattern sp = new SitePattern(alignment);

		LikelihoodValue likelihood = new LikelihoodValue(sp);
		likelihood.setModel(sm);
		// likelihood.setTree(new NeighborJoiningTree(new
		// AlignmentDistanceMatrix(sp, sm)));
		likelihood.setTree(new pal.tree.UnconstrainedTree(tree));

		// return likelihood.compute();
		return likelihood.optimiseParameters();
	}

	private DistanceMatrix getDistance(SimpleAlignment window, int method)
	{
		switch (method)
		{
		case METHOD_JC:
			return TreeUtilities.getJukesCantorDistanceMatrix(window);

		case METHOD_F84:
		{
			return TreeUtilities.getMaximumLikelihoodDistanceMatrix(window,
					result.tRatio, result.alpha);
		}

		default:
			return null;
		}
	}
}