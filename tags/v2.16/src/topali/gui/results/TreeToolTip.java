// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;

import javax.swing.JToolTip;

import pal.alignment.SimpleAlignment;
import pal.gui.TreePainterNormal;
import pal.tree.Tree;
import topali.analyses.*;
import topali.data.*;
import topali.gui.Prefs;

public class TreeToolTip extends JToolTip
{
	private SequenceSet ss;

	private String[] seqNames;

	private TreePainterNormal painter;

	private Dimension d;

	// Pass in the Alignment object along with the sequences used during
	// whatever
	// analysis run this tooltip will hover over
	public TreeToolTip(AlignmentData data, String[] seqNames)
	{
		ss = data.getSequenceSet();
		this.seqNames = seqNames;
	}

	@Override
	public Dimension getPreferredSize()
	{
		if (painter != null)
		{
			Dimension d = painter.getPreferredSize();

			if (d.width > 200)
				return d;
			else
				return new Dimension(200, d.height);
		} else
			return new Dimension();
	}

	public void createNewTree(int n1, int n2)
	{
		// We have to work out the indices each time, because they could change
		// if the user rearranged the sequence ordering
		int[] indices = ss.getIndicesFromNames(seqNames);
		SimpleAlignment alignment = ss.getAlignment(indices, n1, n2, false);

		TreeCreator tc = new TreeCreator(alignment, ss.isDNA(), true, false);

		Tree tree = tc.getTree();
		if (tree != null)
		{
			painter = new TreePainterNormal(tree, "", false);
			painter.setPenWidth(1);
		} else
		{
			painter = null;
		}
	}

	@Override
	public void paintComponent(Graphics g)
	{
		if (painter != null)
		{
			if (Prefs.gui_tree_unique_cols)
			{
				painter.setColouriser(ss
						.getNameColouriser(Prefs.gui_color_seed));
				painter.setUsingColor(false);
			}

			d = getSize();

			painter.paint(g, d.width, d.height);

			g.setColor(Color.black);
			g.drawRect(0, 0, d.width - 1, d.height - 1);
		}
	}
}
