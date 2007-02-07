// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

import topali.gui.*;

public class OverviewDialog extends JDialog
{
	private OverviewCanvas canvas = new OverviewCanvas();
	private AlignmentPanel panel;
	
	private int seqCount, nucCount;
	
	// Tracks the MOST RECENT thread that is generating an image
	private BufferedImage image = null;
	private OverviewGenerator imager = null;
	
	public OverviewDialog(WinMain winMain)
	{
		super(winMain, "Alignment Overview", false);
		
		setLayout(new BorderLayout());
		add(canvas);
		
		addListeners();
		
		setSize(Prefs.gui_odialog_w, Prefs.gui_odialog_h);
		if (Prefs.gui_odialog_x == -1)
			setLocationRelativeTo(winMain);
		else
			setLocation(Prefs.gui_odialog_x, Prefs.gui_odialog_y);
	}
	
	private void addListeners()
	{
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				createImage();
			}
		});
		
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				createImage();
			}
		});
	}
	
	public void exit()
	{
		Prefs.gui_odialog_x = getX();
		Prefs.gui_odialog_y = getY();
		Prefs.gui_odialog_w = getWidth();
		Prefs.gui_odialog_h = getHeight();
	}
	
	public void setAlignmentPanel(AlignmentPanel newPanel)
	{
		if (panel == newPanel)
			return;		
		
		panel = newPanel;
		
		if (panel != null)
		{
			seqCount = panel.getSequenceSet().getSize();
			nucCount = panel.getSequenceSet().getLength();
		
			createImage();
		}
		else
			repaint();
	}
	
	// startNuc and width displayed, and startSeq and depth displayed
	public void setPanelPosition(int nStart, int nWid, int sStart, int sHgt)
	{
		if (panel == null || imager == null)
			return;
		
		// Rectange's x and y
		canvas.boxX = (int) (imager.nScale * nStart);
		canvas.boxY = (int) (imager.sScale * sStart);
		
		// And width and height
		if (nWid >= nucCount)
			canvas.boxW = canvas.getWidth()-1;
		else
			canvas.boxW = (int) (imager.nScale * nWid);
		if (sHgt >= seqCount)
			canvas.boxH = canvas.getHeight()-1;
		else
			canvas.boxH = (int) (imager.sScale * sHgt + imager.sHeight);
				
		canvas.repaint();
	}
	
	public void createImage()
	{
		int w = canvas.getSize().width;
		int h = canvas.getSize().height;
		
		if (w == 0 || h == 0 || panel == null || isVisible() == false)
			return;
		
		image = null;
		imager = new OverviewGenerator(this, panel, w, h);
		
		canvas.repaint();
	}
	
	void imageAvailable(OverviewGenerator generator)
	{
		// We need this check because multiple user resizes of the window,
		// *before* the image has finished, will kick off additional threads.
		// There's no way around it, but we just make sure the image shown is
		// from the last thread started, as it will match the window size.		
		if (generator == imager)
		{
			image = imager.getImage();
			panel.updateOverviewDialog();
			repaint();
		}
	}
	
	class OverviewCanvas extends JPanel
	{		
		int boxX, boxY, boxW, boxH;
		
		OverviewCanvas()
		{
			setOpaque(false);
			
			addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) { processMouse(e); }
				public void mousePressed(MouseEvent e) { processMouse(e); }
				public void mouseReleased(MouseEvent e) { processMouse(e); }				
			});
			
			addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent e) { processMouse(e); }
			});
		}
		
		private void processMouse(MouseEvent e)
		{
			if (panel == null)
				return;
			
			int x = e.getX() - (int)(boxW/2f);
			int y = e.getY() - (int)(boxH/2f);
			
			// Compute mouse position (and adjust by wid/hgt of rectangle)
			int nuc = (int) (x / imager.nScale);
			int seq = (int) (y / imager.sScale);
						
			panel.jumpToPosition(nuc, seq, false, false);
		}
	
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			
			if (panel == null)
			{
			}
			
			else if (image != null)
			{
				// Paint the image of the alignment
				g.drawImage(image, 0, 0, null);
			
				// Then draw the tracking rectangle
				((Graphics2D)g).setPaint(new Color(0, 50, 50, 50));
				g.fillRect(boxX, boxY, boxW, boxH);			
				g.setColor(Color.blue);
				g.drawRect(boxX, boxY, boxW, boxH);
			}
			
			else
			{
				String s = "generating overview...please be patient";
				int length = g.getFontMetrics().stringWidth(s);
				
				g.setColor(Color.lightGray);
				g.drawString(s, (int)(getWidth()/2f-length/2f), getHeight()/2);
			}
		}
	}
}