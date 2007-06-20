// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import pal.alignment.*;
import pal.distance.AlignmentDistanceMatrix;
import pal.misc.Identifier;
import pal.substmodel.*;
import pal.tree.*;
import topali.gui.Text;
import doe.MsgBox;

public class F84TreeCreator extends JDialog
{

	private final double tsTv = 2;
	private final double alpha = 4;
	
Logger log = Logger.getLogger(this.getClass());
	
	private Alignment alignment;

	private Tree tree;

	// Constructor for TreeCreator - assumes the dialog will always be shown
	// although we don't make it visible here
	public F84TreeCreator(Alignment alignment)
	{
		super(MsgBox.frm, Text.Analyses.getString("TreeCreator.gui01"), true);

		this.alignment = alignment;
		createDialog();
	}

	public Tree getTree(boolean showDialog)
	{
		// Both of these calls should block - either by popping open the dialog
		// and not returning from it until createTree() is finished, or by
		// calling createTree directly.
		if (showDialog)
			setVisible(true);
		else
			createTree();

		dispose();

		return tree;
	}

	private void createTree()
	{
		try
		{
			long start = System.currentTimeMillis();

			double[] freqs = AlignmentUtils.estimateFrequencies(alignment);
			F84 f84 = new F84(tsTv, freqs);
			GammaRates rates = new GammaRates(4, alpha);
			SubstitutionModel sModel = SubstitutionModel.Utils.createSubstitutionModel(f84, rates);
			AlignmentDistanceMatrix matrix = new AlignmentDistanceMatrix(new SitePattern(alignment), sModel);
			tree = new NeighborJoiningTree(matrix);
			
			tree.getRoot().setIdentifier(new Identifier(""));
			// Midpoint route...
			tree = TreeRooter.getMidpointRooted(tree);
			
			log.info("Tree creation took: "+(System.currentTimeMillis()-start)+"ms");
			
		} catch (Exception e)
		{
			log.warn(e);
			MsgBox.msg(Text.format(
					Text.Analyses.getString("TreeCreator.err01"), e),
					MsgBox.ERR);

			// Ensure the tree cannot be returned in a readable format
			tree = null;
		}

		// Does nothing if the dialog wasn't visible, but unblocks the thread if
		// it was, allowing control to return to the caller
		setVisible(false);
	}

	private void createDialog()
	{
		addWindowListener(new WindowAdapter()
		{
			public void windowOpened(WindowEvent e)
			{
				Runnable r = new Runnable()
				{
					public void run()
					{
						createTree();
					}
				};
				new Thread(r).start();
			}
		});

		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(new JLabel(Text.Analyses.getString("TreeCreator.gui02")),
				BorderLayout.NORTH);
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(p1);
		pack();

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setResizable(false);
		setLocationRelativeTo(MsgBox.frm);
	}
	
}
