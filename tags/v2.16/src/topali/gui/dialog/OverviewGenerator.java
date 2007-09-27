// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import topali.data.SequenceSet;
import topali.gui.AlignmentPanel;

class OverviewGenerator extends Thread
{
	private OverviewDialog dialog;

	private AlignmentPanel panel;

	// Width and height of the image to be created
	private int w, h;

	private BufferedImage image;

	float sScale, nScale;

	int sHeight, nWidth;

	OverviewGenerator(OverviewDialog dialog, AlignmentPanel panel, int w, int h)
	{
		this.dialog = dialog;
		this.panel = panel;
		this.w = w;
		this.h = h;

		setPriority(Thread.MIN_PRIORITY);
		start();
	}

	BufferedImage getImage()
	{
		return image;
	}

	@Override
	public void run()
	{
		image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g = image.createGraphics();
		SequenceSet ss = panel.getSequenceSet();

		// Number of sequences
		int s = ss.getSize();
		// Number of nucleotides
		int n = ss.getLength();

		// Scaling factors
		sScale = h / (float) s;
		nScale = w / (float) n;

		// Height of each sequence (SEE NOTE BELOW)
		sHeight = 1 + (int) ((sScale >= 1) ? sScale : 1);
		// Width of each nucleotide
		nWidth = 1 + (int) ((nScale >= 1) ? nScale : 1);

		// For each sequence
		float y = 0;
		for (int seq = 0; seq < s; seq++)
		{
			char[] c = ("" + ss.getSequence(seq).getBuffer()).toCharArray();

			// For each nucleotide
			float x = 0;
			for (int nuc = 0; nuc < c.length; nuc++)
			{
				g.setColor(panel.getColor(c[nuc]));
				g.fillRect((int) x, (int) y, nWidth, sHeight);

				x += nScale;
			}

			y += sScale;
		}

		// Once complete, let the dialog know its image is ready
		dialog.imageAvailable(this);
	}

	// We use (1 +) do deal with integer roundoff that results in columns
	// being skipped due to overlaps: eg with width of 1.2:
	// 1.2 (1) 2.4 (2) 3.6 (3) 4.8 (4) 6.0 (6)
	// position 5 was skipped
}