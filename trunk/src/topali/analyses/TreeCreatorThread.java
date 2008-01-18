// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import org.apache.log4j.Logger;

import pal.alignment.*;
import pal.distance.AlignmentDistanceMatrix;
import pal.misc.Identifier;
import pal.substmodel.*;
import pal.tree.*;
import topali.gui.*;
import topali.var.threads.*;
import scri.commons.gui.MsgBox;

public class TreeCreatorThread extends DesktopThread
{
	Logger log = Logger.getLogger(this.getClass());
	
	Alignment alignment;
	boolean isDNA;
	boolean showDialog;
	
	private double tsTv = 2;
	private double alpha = 4;
	private long start;
	private long end;
	private Tree tree;
	
	public TreeCreatorThread(Alignment alignment, boolean isDNA, boolean showDialog) {
		this.alignment = alignment;
		this.isDNA = isDNA;
		this.showDialog = showDialog;
	}
	
	public void setParameters(double tsTv, double alpha) {
		this.tsTv = tsTv;
		this.alpha = alpha;
	}
	
	public long getStartTime() {
		return start;
	}
	
	public long getEndTime() {
		return end;
	}
	
	public Tree getTree() {
		if(showDialog) {
			DefaultWaitDialog dlg = new DefaultWaitDialog(TOPALi.winMain, "Creating Tree", "Creating Tree. Please be patient...", this);
			dlg.setLocationRelativeTo(TOPALi.winMain);
			dlg.setVisible(true);
		}
		else {
			run();
		}
		
		return tree;
	}

	@Override
	public void run()
	{
		try
		{
			start = System.currentTimeMillis();
			double[] freqs = AlignmentUtils.estimateFrequencies(alignment);
			
			AbstractRateMatrix ratematrix = null;
			RateDistribution rate = null;
			
			if(isDNA) {
				ratematrix = new F84(tsTv, freqs);
				rate = new GammaRates(4, alpha);
			}
			else {
				ratematrix = new WAG(freqs);
				rate = new GammaRates(4, alpha);
			}
			
			SubstitutionModel sModel = SubstitutionModel.Utils.createSubstitutionModel(ratematrix, rate);
			AlignmentDistanceMatrix distmatrix = new AlignmentDistanceMatrix(new SitePattern(alignment), sModel);
			tree = new NeighborJoiningTree(distmatrix);
			
			tree.getRoot().setIdentifier(new Identifier(""));
			
			end = System.currentTimeMillis();
			
		} catch (Exception e)
		{
			log.warn(e);
			MsgBox.msg(Text.format(
					Text.Analyses.getString("TreeCreator.err01"), e),
					MsgBox.ERR);

			// Ensure the tree cannot be returned in a readable format
			tree = null;
		}
		
		updateObservers(DesktopThread.THREAD_FINISHED);
	}

	
}
