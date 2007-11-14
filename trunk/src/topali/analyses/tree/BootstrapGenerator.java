// (C) 2003-2004 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses.tree;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import pal.alignment.*;
import pal.tree.*;
import pal.util.AlgorithmCallback;
import topali.analyses.TreeCreator;
import topali.gui.*;
import doe.MsgBox;

public class BootstrapGenerator extends JDialog implements TreeGenerator
{
	private BootstrapGenerator bs = this;
	// The original tree
	private Tree tree = null;
	// The bootstrapped tree
	private Tree bsTree = null;
	private Alignment alignment = null;
	
	private boolean isRunning = true;
	private JProgressBar pBar;
	private int count = 0;
	private int bootstraps = 0;
	private boolean isDna = true;
	private boolean mproot = true;
	
	TreeCreator creator;
	
	public BootstrapGenerator(Tree t, Alignment a, boolean dna, boolean mp, int bs)
	{
		super(TOPALi.winMain, "Bootstrapping Tree", true);
		
		tree = t;
		alignment = a;
		bootstraps = bs;
		isDna = dna;
		mproot = mp;
		
		pBar = new JProgressBar();	
		JLabel label = new JLabel("Performing bootstrapping - please be patient.     ");
		
		JPanel p1 = new JPanel(new BorderLayout(5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p1.add(label, BorderLayout.NORTH);
		p1.add(pBar);
		
		getContentPane().add(p1);
		
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e)
			{
				computeTree();
			}
			
			public void windowClosing(WindowEvent e)
			{
				isRunning = false;
			}
		});
		
		pack();
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setResizable(false);
		setLocationRelativeTo(TOPALi.winMain);
		setVisible(true);
	}
	
	public Tree getTree()
	{
		return bsTree;
	}
	
	private void computeTree()
	{			
		pBar.setMaximum(bootstraps);
		
		Runnable r = new Runnable() {
			public void run()
			{
				try
				{
					bsTree = TreeUtils.getReplicateCladeSupport(
						"bootstrap", tree, bs, bootstraps,
						pal.util.AlgorithmCallback.Utils.getNullCallback());
				}
				catch (Exception e)
				{
					System.out.println(e);
				}
				
				setVisible(false);
			}
		};
		
		new Thread(r).start();
	}
	
	public Tree getNextTree(Tree baseTree, AlgorithmCallback callback)
	{
		BootstrappedAlignment boot = new BootstrappedAlignment(alignment);
		creator = new TreeCreator(boot, isDna, mproot, false);
		
		pBar.setValue(++count);
				
		if (isRunning) {
			return creator.getTree();
		}
		else {
			if(creator!=null)
				creator.kill();
			return null;
		}
	}
}