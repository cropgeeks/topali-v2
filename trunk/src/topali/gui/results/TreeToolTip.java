package topali.gui.results;

import java.awt.*;
import javax.swing.*;

import pal.alignment.*;
import pal.gui.*;
import pal.tree.*;

import topali.analyses.*;
import topali.data.*;
import topali.gui.*;

class TreeToolTip extends JToolTip
{
	private SequenceSet ss;
	private String[] seqNames;
	
	private TreePainterNormal painter;
	private Dimension d;
	
	// Pass in the Alignment object along with the sequences used during whatever
	// analysis run this tooltip will hover over
	TreeToolTip(AlignmentData data, String[] seqNames)
	{
		ss = data.getSequenceSet();
		this.seqNames = seqNames;
	}
	
	public Dimension getPreferredSize()
	{
		if (painter != null)
		{
			Dimension d = painter.getPreferredSize();
			
			if (d.width > 200)
				return d;
			else
				return new Dimension(200, d.height);			
		}
		else
			return new Dimension();
	}
	
	void createNewTree(int n1, int n2)
	{
		// We have to work out the indices each time, because they could change
		// if the user rearranged the sequence ordering
		int[] indices = ss.getIndicesFromNames(seqNames);
		SimpleAlignment alignment = ss.getAlignment(indices, n1, n2, false);
		
		TreeCreator tc = new TreeCreator(alignment);
		
		Tree tree = tc.getTree(false);		
		if (tree != null)
		{
			painter = new TreePainterNormal(tree, "", false);
			painter.setPenWidth(1);
		}
		else
		{
			painter = null;
		}
	}
	
	public void paintComponent(Graphics g)
	{
		if (painter != null)
		{
			if (Prefs.gui_tree_unique_cols)
			{
				painter.setColouriser(
					ss.getNameColouriser(Prefs.gui_color_seed));
				painter.setUsingColor(false);
			}
			
			d = getSize();
			
			painter.paint(g, d.width, d.height);
			
			g.setColor(Color.black);
			g.drawRect(0, 0, d.width-1, d.height-1);
		}
	}
}