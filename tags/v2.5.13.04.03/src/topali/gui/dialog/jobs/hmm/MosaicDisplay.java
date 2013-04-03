// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.hmm;

import java.awt.*;
import java.util.Vector;

import javax.swing.JPanel;

/* This is a subpanel of the MosaicPanel (which is a subpanel of the
 * HMMSettingsDialog), designed to display a graphic of the currently selected
 * set of breakpoints for Barce.
 */
class MosaicDisplay extends JPanel
{
	private Vector<BreakPoint> breakpoints = null;

	// x scale minimum and maximum
	private float x0, xm;

	// start and end of canvas
	private int p0, pm;

	MosaicDisplay(int length)
	{
		x0 = 1;
		xm = length;

		setPreferredSize(new Dimension(200, 20));
	}

	int getPointFromNucleotide(int nucleotide)
	{
		return (int) ((pm - p0) * ((nucleotide - x0) / (xm - x0)) + p0);
	}

	void setBreakpoints(Vector<BreakPoint> points)
	{
		breakpoints = points;
		repaint();
	}

	@Override
	public void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);

		if (breakpoints == null)
			return;

		Graphics2D g = (Graphics2D) graphics;

		// Start of canvas (x axis)
		p0 = 0;
		// End of canvas (x axis)
		pm = getSize().width;

		for (int i = breakpoints.size() - 1; i >= 0; i--)
		{
			BreakPoint point = breakpoints.elementAt(i);

			int bp = point.breakpoint;
			int tp = point.topology;

			switch (tp)
			{
			case 0:
				g.setColor(Color.green);
				break;
			case 1:
				g.setColor(Color.blue);
				break;
			case 2:
				g.setColor(Color.yellow);
				break;
			}

			g.fillRect(0, 0, getPointFromNucleotide(bp), getSize().height);
		}
	}
}
