// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.region;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import pal.alignment.SimpleAlignment;
import pal.gui.*;
import pal.tree.Tree;
import topali.analyses.*;
import topali.data.*;
import topali.gui.*;
import topali.i18n.Text;
import scri.commons.gui.*;

public class TreePreviewPanel extends JPanel implements ActionListener
{
	 Logger log = Logger.getLogger(this.getClass());

	// Current alignment data and partition info
	private SequenceSet ss;

	private boolean drawPreview = true;

	private boolean slowWarningShown = false;

	// The actual tree (PAL object)
	private Tree tree;

	// Object used to paint the tree
	private TreePainter painter;

	private RegionDialog dialog;

	private JCheckBox checkCurrent = null;

	private TreeCanvas canvas = new TreeCanvas();

	private TreeCreatorThread tc = null;

	public TreePreviewPanel()
	{
		checkCurrent = new JCheckBox(Text.get("TreePreviewPanel.gui01"), Prefs.gui_preview_current);
		checkCurrent.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
		checkCurrent.addActionListener(this);
		checkCurrent.setMnemonic(KeyEvent.VK_O);
		checkCurrent.setToolTipText(Text.get("TreePreviewPanel.gui02"));

		setLayout(new BorderLayout(5, 5));
		add(checkCurrent, BorderLayout.NORTH);
		add(canvas, BorderLayout.CENTER);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == checkCurrent)
		{
			Prefs.gui_preview_current = checkCurrent.isSelected();
			dialog.updateTreePreview(false);
		}
	}

	public void clearTree()
	{
		painter = null;
		canvas.repaint();
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

	public void createTree(final int start, final int end)
	{
		if (tc != null && tc.isAlive())
			tc.kill();

		if (drawPreview == false)
		{
			repaint();
			return;
		}

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
				SimpleAlignment alignment = ss.getAlignment(indices, start,
						end, false);
//				TreeCreator tc = new TreeCreator(alignment, ss.isDNA());

				boolean isDna = ss.getProps().isNucleotides();
				tc = new TreeCreatorThread(alignment, isDna, false);
				tree = tc.getTree();
				tc = null;

//				tree = tc.getTree(false);
				long e = System.currentTimeMillis();
				log.info("Tree creation " + (e - s) + "ms");

				if (tree != null)
				{
					painter = new TreePainterNormal(tree, "", false);
					painter.setPenWidth(1);
				} else
					painter = null;

				canvas.repaint();

				// If the time taken to generate the tree is greater than 1.25
				// seconds
				if ((e - s) >= 1250 && slowWarningShown == false)
				{
					String msg = Text.get("TreePreviewPanel.msg01");

					// Offer to disable generating tree previews for this
					// alignment
					if (MsgBox.yesno(msg, 0) == JOptionPane.YES_OPTION)
					{
						drawPreview = false;
						repaint();
					}

					slowWarningShown = true;
				}

			}
		};

		// We could use "new Thread(r).start()" to run this, but it's actually
		// better to hang the GUI - otherwise the user could return to another
		// alignment before the tree is displayed - real PITA if that happens
		SwingUtilities.invokeLater(r);
	}

	class TreeCanvas extends JPanel
	{
		TreeCanvas()
		{
			setBackground(Color.white);
			setBorder(BorderFactory.createLineBorder(Icons.blueBorder));
			setToolTipText(Text.get("TreePreviewPanel.gui03"));

			// Mouse listener to catch double-click events
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						drawPreview = !drawPreview;
						dialog.updateTreePreview(false);
					}
				}
			});
		}

		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			if (painter != null)
			{
				if (Prefs.gui_tree_unique_cols)
				{
					painter.setColouriser(ss
							.getNameColouriser(Prefs.gui_color_seed));
					painter.setUsingColor(false);
				}

				painter.paint(g, getSize().width, getSize().height);
			}
		}
	}
}
