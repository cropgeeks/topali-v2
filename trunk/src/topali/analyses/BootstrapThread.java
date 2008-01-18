// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import org.apache.log4j.Logger;

import pal.alignment.*;
import pal.tree.*;
import pal.util.AlgorithmCallback;
import topali.gui.TOPALi;
import topali.var.threads.*;

public class BootstrapThread extends DesktopThread implements TreeGenerator
{
	Logger log = Logger.getLogger(this.getClass());
	
	private BootstrapThread bsthread = this;
	
	Tree tree;
	Alignment align;
	boolean isDNA;
	int runs;
	
	TreeCreatorThread creator;
	int count = 0;
	Tree bsTree;
	
	double tstv = -1;
	double alpha = -1;
	
	public BootstrapThread(Tree tree, Alignment align, boolean isDNA, int runs) {
		this.tree = tree;
		this.align = align;
		this.isDNA = isDNA;
		this.runs = runs;
	}
	
	public void setParameters(double tstv, double alpha) {
		this.tstv = tstv;
		this.alpha = alpha;
	}
	
	public Tree getTree()
	{
		ProgressBarWaitDialog dlg = new ProgressBarWaitDialog(TOPALi.winMain, "Bootstrapping", "Perform bootstrapping. Please be patient...", this);
		dlg.setLocationRelativeTo(TOPALi.winMain);
		dlg.setVisible(true);
		
		return bsTree;
	}
	
	@Override
	public void kill()
	{
		stop = true;
	}


	@Override
	public void run()
	{
		try
		{
			bsTree = TreeUtils.getReplicateCladeSupport(
					"bootstrap", tree, bsthread, runs,
					pal.util.AlgorithmCallback.Utils.getNullCallback());
		} catch (Exception e)
		{
			log.warn("Bootstrapping failed.", e);
		}
		finally {
			updateObservers(DesktopThread.THREAD_FINISHED);
		}
	}

	public Tree getNextTree(AlgorithmCallback arg1)
	{
		BootstrappedAlignment boot = new BootstrappedAlignment(align);
		creator = new TreeCreatorThread(boot, isDNA, false);
		
		if(tstv!=-1 || alpha!=-1)
			creator.setParameters(tstv, alpha);
		
		updateObservers(count*100/runs);
		
		count++;
		
		if (!stop) {
			return creator.getTree();
		}
		else {
			log.info("Bootstrapping stopped.");
			if(creator!=null)
				creator.kill();
			return null;
		}
	}

	@Override
	public Tree getNextTree(Tree arg0, AlgorithmCallback arg1) {
	    return getNextTree(arg1);
	}
	
	

}
