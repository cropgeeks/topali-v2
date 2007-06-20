// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import pal.alignment.Alignment;
import pal.distance.JukesCantorDistanceMatrix;
import pal.misc.Identifier;
import pal.tree.*;
import topali.gui.Text;
import doe.MsgBox;

public class TreeCreator extends JDialog
{
	Logger log = Logger.getLogger(this.getClass());
	
	private Alignment alignment;

	private Tree tree;

	// Constructor for TreeCreator - assumes the dialog will always be shown
	// although we don't make it visible here
	public TreeCreator(Alignment alignment)
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
			
			createLocJCNJTree();

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

	// Creates a PAL tree using a JC distance matrix and NJ tree
	private void createLocJCNJTree() throws Exception
	{
		// Compute the distance matrix for this alignment file
		JukesCantorDistanceMatrix distance = getJCDistanceMatrix();

		// Finally, construct the tree using the matrix
		tree = new NeighborJoiningTree(distance);
	}

	public JukesCantorDistanceMatrix getJCDistanceMatrix()
	{
		return new JukesCantorDistanceMatrix(alignment);
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