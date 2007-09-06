// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import topali.gui.Prefs;
import topali.var.Utils;

class HistogramPanel extends JPanel
{
	public float[] array = null;
	
	private TitledBorder border = null;
	private JPanel borderPanel = null;

	public HistogramPanel()
	{
		setToolTipText("Histogram data for the current window position");
	}
	
	public void setData(float[] data)
	{
		array = data;
		if (borderPanel != null)
		{
			border.setTitle("Probability distribution (" + array.length + "):");
			borderPanel.repaint();
		}
		else
			repaint();
	}
	
	@Override
	public Dimension getPreferredSize()
		{ return new Dimension(50, 30); }
	
	public void setTitledBorder(TitledBorder t)
	{
		border = t;
		border.setTitle("Probability distribution:");
	}
	
	public void setBorderPanel(JPanel panel)
	{
		borderPanel = panel;
	}
	
	public TitledBorder getTitledBorder()
	{
		return border;
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		setBackground(Prefs.gui_histo_background);
		
		if (array == null)
			return;
		
		float wF = 0;
		
		// Scale based on showing 3 bars, or any number...
		if (array.length <= 3)
			wF = getSize().width / 3f;
		else
			wF = getSize().width / (float) array.length;
					
		
		int wI = (int) wF;		
		int h = getSize().height;
		
		for (int k = 0; k < array.length; k++)
		{
			int scale = (int) (h * array[k]);
		
			g.setColor(Utils.getColor(k));
			if (wI < 1)
				g.fillRect((int)(k*wF), h-scale, 1, scale);
			else
				g.fillRect(k*wI, h-scale, wI, scale);
		}
	}
}
