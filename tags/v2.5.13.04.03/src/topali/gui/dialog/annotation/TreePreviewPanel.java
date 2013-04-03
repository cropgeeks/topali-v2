// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.annotation;

import java.awt.Graphics;
import javax.swing.JPanel;
import org.apache.log4j.Logger;
import pal.alignment.SimpleAlignment;
import pal.gui.*;
import pal.tree.Tree;
import topali.analyses.TreeCreatorThread;
import topali.data.*;
import topali.data.annotations.Annotation;
import topali.i18n.Text;

public class TreePreviewPanel extends JPanel
{
	 Logger log = Logger.getLogger(this.getClass());
	
	// Current alignment data and partition info
	private SequenceSet ss;

	// The actual tree (PAL object)
	private Tree tree;

	// Object used to paint the tree
	private TreePainter painter;

	private boolean enabled = true;
	
	TreeCreatorThread tc = null;
	
	public TreePreviewPanel()
	{
	}

	public void clearTree()
	{
		painter = null;
		repaint();
	}

	public void setEnabled(boolean b) {
		this.enabled = b;
		repaint();
	}
	
	public void setAlignmentData(AlignmentData data)
	{
		if (data == null)
			clearTree();
		else
		{
			ss = data.getSequenceSet();
		}
	}

	public void createTree(final Annotation anno)
	{	
		if(tc!=null && tc.isAlive())
			tc.kill();
		
		painter = null;
		
		repaint();
		
		if (!enabled)
			return;
		
		Runnable r = new Runnable()
		{
			public void run()
			{

				// Work out which sequences to use
				int[] indices = null;
				if (Prefs.gui_preview_current)
					indices = ss.getSelectedSequences();
				else
					indices = ss.getAllSequences();

				// Can only draw trees with at least 3 sequences
				if (indices.length < 3)
				{
					clearTree();
					return;
				}

				// Create a PAL alignment that can be used to create this tree
				long s = System.currentTimeMillis();
				SimpleAlignment alignment = ss.getAlignment(indices, anno, false);
				
				boolean dnarna = anno.getSeqType()==SequenceSetProperties.TYPE_DNA || anno.getSeqType()==SequenceSetProperties.TYPE_RNA;
				if(anno.getSeqType()==SequenceSetProperties.TYPE_UNKNOWN)
					return;
				
				tc = new TreeCreatorThread(alignment, dnarna, false);
				tree = tc.getTree();
				tc = null;
				
				long e = System.currentTimeMillis();
				log.info("Tree creation " + (e - s) + "ms");

				if (tree != null)
				{
					painter = new TreePainterNormal(tree, "", false);
					painter.setPenWidth(1);
				} else
					painter = null;

				repaint();
			}
		};

		Thread th = new Thread(r);
		th.setPriority(Thread.MIN_PRIORITY);
		th.start();
	}

	
	public void paint(Graphics g) {
		super.paint(g);
		
		if(enabled) {
			if (painter != null)
			{
				if (Prefs.gui_tree_unique_cols)
				{
					painter.setColouriser(ss
							.getNameColouriser(Prefs.gui_color_seed));
					painter.setUsingColor(false);
				}

				int w = getSize().width-2;
				int h = getSize().height-2;
				Graphics g2 = g.create(1, 1, w, h);
				painter.paint(g2, w, h);
			}
			
			else {
				if(tc!=null)
					g.drawString(Text.get("TreePreviewPanel.0"), 10, 20);
			}
		}
		
	}
}
