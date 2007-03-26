// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import topali.data.*;

class AnnotationsPanel extends JPanel
{
	private AlignmentData data;

	private AlignmentPanel panel;

	private JScrollPane cdsSP;

	private CDSCanvas cdsCanvas;

	private TextPanel textPanel;

	// CanvasWidth and characterWidth (from the AlignmentPanel)
	private int canW;

	AnnotationsPanel(AlignmentPanel panel, AlignmentData data)
	{
		this.data = data;
		this.panel = panel;

		cdsCanvas = new CDSCanvas();
		cdsSP = new JScrollPane(cdsCanvas);
		cdsSP
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		cdsSP.getHorizontalScrollBar().setModel(panel.hBar.getModel());

		textPanel = new TextPanel();

		setLayout(new BorderLayout());
		add(textPanel, BorderLayout.WEST);
		add(cdsSP);
	}

	// void setScrollBarValue(int value)
	// {
	// // cdsSP.getHorizontalScrollBar().setValue(value);
	// //
	// System.out.println("anno.max="+cdsSP.getHorizontalScrollBar().getMaximum());
	// }

	void setSizes(int canW, int seqListWidth)
	{
		this.canW = canW;

		cdsCanvas.setSize(canW, 30);

		textPanel.width = seqListWidth;
		textPanel.setSize(textPanel.width, 30);
	}

	private class CDSCanvas extends JPanel
	{

		public Dimension getSize()
		{
			return new Dimension(canW, 30);
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(canW, 30);
		}

		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			int cW = panel.charW;
			// int start = panel.pX / cW;
			int totalWidth = cW * data.getSequenceSet().getLength();

			g.drawLine(0, 9, totalWidth, 9);

			for (RegionAnnotations.Region r : (PartitionAnnotations) data
					.getTopaliAnnotations().getAnnotations(
							PartitionAnnotations.class))
			{
				int nS = r.getS();
				int nE = r.getE();

				int x1 = (nS * cW) - cW;
				int x2 = (nE * cW) - cW - 1;

				// System.out.println("Region from " + x1 + " to " + x2);

				g.setColor(Color.white);
				g.fillRect(x1, 5, x2 - x1 + cW, 7);
				g.setColor(Color.black);
				g.drawRect(x1, 5, x2 - x1 + cW, 7);
			}
		}
	}

	private static class TextPanel extends JPanel
	{
		int width;

		// public Dimension getSize()
		// {
		// return new Dimension(width, 30);
		// }

		public Dimension getPreferredSize()
		{
			return new Dimension(width, 30);
		}

		public Dimension getMaximumSize()
		{
			return new Dimension(width, 30);
		}
	}
}