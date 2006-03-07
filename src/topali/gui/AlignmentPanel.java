// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;

import topali.data.*;

/* Parent container for the canvas used to draw the sequence data. */
public class AlignmentPanel extends JPanel implements AdjustmentListener
{
	private JScrollPane sp;
	JScrollBar hBar, vBar;
	private JViewport view;
	
	private SequenceListPanel seqList;
	private DisplayCanvas canvas;
	private HeaderCanvas header;
	private AnnotationsPanel annotationsPanel;
	
	// SequenceSet data that is displayed
	private AlignmentData data;
	private SequenceSet ss;
	private PartitionAnnotations pAnnotations;
	
	// Fonts used by the display areas
	private FontMetrics fm;
	private Font font;
	
	////////////////////////////////////////////////////////////////////////////
	// Canvas width and height. Total dimensions for the canvas (which will
	// usually be in the thousands of pixels range for most alignments.
	int canW, canH;
	
	// Top left corner of current view
	int pX, pY;
	
	int start = 0;
	
	// Width and height of each character that is drawn
	int charW, charH;
	
	// Descent of each character (how much from the baseline needs removed
	// to properly draw small case letters like 'g')
	private int charDec;
	// How many characters will fit in the current window size
	private int charCount;
	private int charDepth;
	////////////////////////////////////////////////////////////////////////////
				
	public AlignmentPanel(AlignmentData data)
	{	
		this.data = data;
		ss = data.getSequenceSet();
		pAnnotations = data.getTopaliAnnotations().getPartitionAnnotations();
	
		sp = new JScrollPane();		
		hBar = sp.getHorizontalScrollBar();
		vBar = sp.getVerticalScrollBar();
		hBar.addAdjustmentListener(this);
		vBar.addAdjustmentListener(this);
		view = sp.getViewport();
		
		header = new HeaderCanvas();
		canvas = new DisplayCanvas();
		seqList = new SequenceListPanel(this, ss);
		annotationsPanel = new AnnotationsPanel(this, data);
				
		sp.setViewportView(canvas);
		sp.setRowHeaderView(seqList);
		sp.setColumnHeaderView(header);		
		sp.getViewport().setBackground(Color.white);

		setLayout(new BorderLayout());
		add(sp, BorderLayout.CENTER);
		add(annotationsPanel, BorderLayout.SOUTH);
		
		// Final setup - ensure the header canvas can compute its height
		canvas.setCanvasFont();
		header.computeInitialHeight();
		
		addPageHandlers();
	}	
	
	public SequenceListPanel getListPanel()
		{ return seqList; }
	
	public SequenceSet getSequenceSet()
		{ return ss; }
	
	public AlignmentData getAlignmentData()
		{ return data; }
	
	public void findSequence(int index, boolean select)
	{
		seqList.findSequence(sp, index, select);
	}
	
	public void jumpToPosition(int nuc, int seq)
	{
		// X (nucleotide)
		int x = nuc * charW - (charW);
		sp.getHorizontalScrollBar().setValue(x);
		
		// Y (sequence)
		if (seq >= 0)
		{
			int y = seq * charH - (charH);
			sp.getVerticalScrollBar().setValue(y);
		}
	}
	
	void setPopupMenu(PopupMenuAdapter popup)
	{
		canvas.addMouseListener(popup);
	}
	
	private void addPageHandlers()
	{
		AbstractAction action1 = new AbstractAction() {
			public void actionPerformed(ActionEvent e)
			{
				sp.getVerticalScrollBar().setValue(
					sp.getVerticalScrollBar().getValue() + 50);
			}
		};
		
		AbstractAction action2 = new AbstractAction() {
			public void actionPerformed(ActionEvent e)
			{
				sp.getVerticalScrollBar().setValue(
					sp.getVerticalScrollBar().getValue() - 50);
			}
		};
				
		KeyStroke ks1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
		sp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks1, "down");
		sp.getActionMap().put("down", action1);
				
		KeyStroke ks2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
		sp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks2, "up");
		sp.getActionMap().put("up", action2);
		
		JList list = seqList.getList();
		list.getInputMap(JComponent.WHEN_FOCUSED).put(ks1, "down");
		list.getActionMap().put("down", action1);	
		list.getInputMap(JComponent.WHEN_FOCUSED).put(ks2, "up");
		list.getActionMap().put("up", action2);		
	}
	
	void refreshAndRepaint()
	{
//		ss.setRemoveRestoreState();
//		winMain.setGraphEnabledStatus();

		seqList.refreshAndRepaint();
		sp.setRowHeaderView(seqList);

		canvas.setCanvasFont();
		header.computeInitialHeight();
	}

	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		// Each time the scollbars are moved, the canvas must be redrawn, with
		// the new dimensions of the canvas being passed to it (window size
		// changes will cause scrollbar movement events)
		canvas.redraw(view.getExtentSize(), view.getViewPosition());
	}
	
	public void updateOverviewDialog()
	{
		WinMain.ovDialog.setPanelPosition(
				(pX/charW), charCount, (pY/charH), charDepth);
	}
	
	// Secondary canvas (row header) that appears along the top of the alignment
	class HeaderCanvas extends JPanel
	{
		int width = 0;
		int y1 = 0, y2 = 0;
		
		public Dimension getPreferredSize()
		{
			return new Dimension(width, y2 + 1);
		}
				
		void computeInitialHeight()
		{
			setBackground(Color.white);
			
			// Get a FontMetrics object so font dimensions can be computed
			fm = new java.awt.image.BufferedImage(1,1,
				java.awt.image.BufferedImage.TYPE_INT_RGB).getGraphics()
					.getFontMetrics(font);
			
			y1 = fm.getHeight() / 2 + 2;
			y2 = 2 * y1 + 2;
		}
		
		// Paints Clustal *** information and column numbers
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (ss == null)
				return;

			y1 = charH / 2 + 2;
			y2 = 2 * y1 + 2;
			g.setFont(font);
			g.setColor(Prefs.gui_seq_color_text);
						
			// *** * ** data
			char str[];
			if (start + charCount > ss.getLength())
				str = ss.getOverview().toCharArray();
			else
				str = ss.getOverview().substring(start,
					start + charCount).toCharArray();
			
			for (int i = 0, x = pX; i < str.length; i++, x += charW)
				g.drawString("" + str[i], x, y2);
			
			// Column numbers
			int highlightHeight = charH - charDec;
			for (int i = start+1, x = pX; i <= start+charCount; i++, x+= charW)
			{
				if (i % 25 == 0)
				{
					int sub = (("" + i).length()-1) * charW;
					g.setColor(new Color(224, 223, 227));
					g.fillRect(x, 0, charW, highlightHeight);
					g.setColor(Prefs.gui_seq_color_text);
					g.drawString("" + i, x-sub, y1);					
				}
			}
		}
	}
	
	// The actual canvas that handles the drawing. It receives the sequence
	// data from its parent (DisplayPanel), and uses that along with the current
	// size of the viewable area to determine how much text should be displayed.
	// Starting and ending positions within the sequence are then found, and
	// this data is written to the canvas. If color coding is on, a highlighted
	// background for each letter is first drawn.
	
	class DisplayCanvas extends JPanel
	{
		private Dimension dimension;		
		private StringBuffer buffer;
	
		DisplayCanvas()
		{		
			canW = canH = 1;
			dimension = new Dimension(canW, canH);	
			setBackground(Color.white);
			setOpaque(false);
			setCanvasFont();
			
			addMouseListener(new CanvasMouseListener());
			addMouseMotionListener(new CanvasMouseMotionListener());
		}
				
		void setCanvasFont()
		{
			int bold = Prefs.gui_seq_font_bold ? Font.BOLD : Font.PLAIN;
			font = new Font("Monospaced", bold, Prefs.gui_seq_font_size);
				
			if (ss != null)
				setData();
		}
		
		class CanvasMouseListener extends MouseAdapter
		{
			public void mouseExited(MouseEvent e)
			{
				setToolTipText(null);
				WinMainStatusBar.setText("");
			}
		}
		
		class CanvasMouseMotionListener extends MouseMotionAdapter
		{
			String seqName = null;
			String str = null;
		
			public void mouseMoved(MouseEvent e)
			{
				System.out.println("main.canvas: " + e.getPoint().x);
				
				if (ss == null)
					return;
					
				Point p = e.getPoint();
				
				try
				{				
					// (offset/charW) gives char number of current display
					// then + to starting char to get actual char
					int nuc = (((p.x-pX) / charW) + (pX / charW)) + 1;
					int seq = p.y / charH;
				
					seqName = ss.getSequence(seq).name;
					str = "Seq " + (seq+1) + ": " + seqName + " (" +  nuc + ")";
					
					System.out.println("hBar.v="+hBar.getValue());
					System.out.println(getWidth());
					annotationsPanel.test();
				
					WinMainStatusBar.setText(str);
					if (Prefs.gui_seq_tooltip)
						setToolTipText(str);
				}
				catch (Exception ne)
				{
					setToolTipText(null);
					WinMainStatusBar.setText("");
				}
			}
		}
	
		// Updated each time new data has been loaded (or changes have been made
		// to the sizes of fonts by the user)
		void setData()
		{
			buffer = ss.getSequence(0).getBuffer();
			
			// Get a FontMetrics object so font dimensions can be computed
			fm = new java.awt.image.BufferedImage(1,1,
				java.awt.image.BufferedImage.TYPE_INT_RGB).getGraphics()
					.getFontMetrics(font);

			charW = fm.charWidth('G');
			charH = fm.getHeight();
			charDec = fm.getMaxDescent();
			
//			System.out.println("charW: " + charW);
//			System.out.println("charH: " + charH);
			
			
			hBar.setUnitIncrement(charW);
			vBar.setUnitIncrement(charH);
			vBar.setBlockIncrement(charH);
			
		
			// Why do I need that extra char-1 at the end?
			canW = buffer.length() * charW + (charW - 1);
			canH = ss.getSize() * charH;// + (charH * 2 ) + 5;
			
			// Resize the canvas so the scrollbars will adjust to size too
			dimension = new Dimension(canW, canH);
			header.width = canW;
			header.setBackground(new Color(234, 234, 234));
			setSize(canW, canH);
			
			repaint();
		}
				
		void redraw(Dimension d, Point p)
		{
			if (buffer == null)
				return;
			
			charCount = (int) ((float) d.getWidth() / (float) charW);
			charDepth = (int) ((float) d.getHeight() / (float) charH);
//			System.out.println("CharCount: " + charCount);
//			System.out.println("Dim width: " + d.getWidth());
			
			pX = p.x;
			pY = p.y;
						
//			System.out.println(pX + ", " + pY);

			if (annotationsPanel != null)
			{
				annotationsPanel.setSizes(canW, charW, seqList.getWidth());
				annotationsPanel.setScrollBarValue(hBar.getValue());
				
				System.out.println("hBar.max="+hBar.getMaximum());
			}
			
			updateOverviewDialog();
			repaint();
		}
			
		public Dimension getSize()
		{
			return dimension;
		}
		
		public Dimension getPreferredSize()
		{
			return dimension;
		}
		
		public void paintComponent(Graphics g)
		{		
			super.paintComponent(g);
						
//			g.setColor(Color.white);
//			g.fillRect(0, 0, getSize().width, getSize().height);
						
			if (buffer == null)
				return;

			start = (pX / charW);
			int seqStart = (int) (pY / charH);
//			int seqStart = 0;

			g.setFont(font);
			
			int seqEnd = seqStart + charDepth + 2;

			// For each sequence...
//			for (int seq = seqStart, y = charH; seq < ss.getSize();
//				seq++, y += charH)
			for (int seq = seqStart, y = charH + (charH * seqStart); seq < seqEnd;
				seq++, y += charH)
			{
				if (seq >= ss.getSize())
					break;
			
				boolean drawDim = !seqList.getList().isSelectedIndex(seq)
					&& Prefs.gui_seq_dim;
			
				// Extract the text to display in this section
				char str[];
				if (start + charCount > ss.getLength())
					str = ("" + ss.getSequence(seq).getBuffer()).toCharArray();
				else
					str = ss.getSequence(seq).getBuffer().substring(start,
						start + charCount).toCharArray();
				
				int y1 = y - charH;
				int y2 = y - charDec;
												
				// Highlight the colours
				if (Prefs.gui_seq_show_colors && drawDim == false)
				{
					for (int i = 0, x = pX; i < str.length; i++, x += charW)
					{
						g.setColor(getColor(str[i]));
/*						if (ss.isDNA())
						{
							// DNA COLOUR CODING
							switch (str[i])
							{
								case 'A' : g.setColor(Prefs.gui_seq_color_a); break;
								case 'C' : g.setColor(Prefs.gui_seq_color_c); break;
								case 'G' : g.setColor(Prefs.gui_seq_color_g); break;
								case 'T' : g.setColor(Prefs.gui_seq_color_t); break;
								case 'U' : g.setColor(Prefs.gui_seq_color_t); break;
								default  : g.setColor(Prefs.gui_seq_color_gaps);
									break;
							}
						}
						else
						{
							// PROTEIN COLOUR CODING
							switch (str[i])
							{
								case 'G' : g.setColor(Prefs.gui_seq_color_gpst); break;
								case 'P' : g.setColor(Prefs.gui_seq_color_gpst); break;
								case 'S' : g.setColor(Prefs.gui_seq_color_gpst); break;
								case 'T' : g.setColor(Prefs.gui_seq_color_gpst); break;
								
								case 'H' : g.setColor(Prefs.gui_seq_color_hkr); break;
								case 'K' : g.setColor(Prefs.gui_seq_color_hkr); break;
								case 'R' : g.setColor(Prefs.gui_seq_color_hkr); break;
								
								case 'F' : g.setColor(Prefs.gui_seq_color_fwy); break;
								case 'W' : g.setColor(Prefs.gui_seq_color_fwy); break;
								case 'Y' : g.setColor(Prefs.gui_seq_color_fwy); break;
								
								case 'I' : g.setColor(Prefs.gui_seq_color_ilmv); break;
								case 'L' : g.setColor(Prefs.gui_seq_color_ilmv); break;
								case 'M' : g.setColor(Prefs.gui_seq_color_ilmv); break;
								case 'V' : g.setColor(Prefs.gui_seq_color_ilmv); break;
							
								default: g.setColor(Prefs.gui_seq_color_gaps); break;
							}
						}
*/						
						
						// Subset selection highlighting
						int cS = pAnnotations.getCurrentStart();
						int cE = pAnnotations.getCurrentEnd();
						
						if ((start+i+1) < cS || (start+i+1) > cE)
							g.setColor(g.getColor().darker().darker());

//						if ((start+i+1) < SequenceSet.nStart ||
//							(start+i+1) > SequenceSet.nEnd)
//							g.setColor(g.getColor().darker().darker());
						
						
						g.fillRect(x, y1, charW, charH);
					}
				}
				
				// Draw the actual text
				if (drawDim == false)
					g.setColor(Prefs.gui_seq_color_text);
				else
					g.setColor(new Color(235, 235, 235));
				
				if (Prefs.gui_seq_show_text)
					g.drawString(new String(str), pX, y2);
			}
						
			header.repaint();
		}
		
/*		private Color getGreyScale(Color c)
		{
			int r = c.getRed();
			int g = c.getGreen();
			int b = c.getBlue();
			
			int out = (int)(((double)r*0.3) + ((double)g * 0.59) + ((double)b * 0.11));
						
			return new Color(out, out, out);
		}
*/
	}
	
	public Color getColor(char c)
	{
		if (ss.isDNA())
		{
			// DNA COLOUR CODING
			switch (c)
			{
				case 'A' : return Prefs.gui_seq_color_a;
				case 'C' : return Prefs.gui_seq_color_c;
				case 'G' : return Prefs.gui_seq_color_g;
				case 'T' : return Prefs.gui_seq_color_t;
				case 'U' : return Prefs.gui_seq_color_t;
				
				default  : return Prefs.gui_seq_color_gaps;
			}
		}
		else
		{
			// PROTEIN COLOUR CODING
			switch (c)
			{
				case 'G' : return Prefs.gui_seq_color_gpst;
				case 'P' : return Prefs.gui_seq_color_gpst;
				case 'S' : return Prefs.gui_seq_color_gpst;
				case 'T' : return Prefs.gui_seq_color_gpst;
				
				case 'H' : return Prefs.gui_seq_color_hkr;
				case 'K' : return Prefs.gui_seq_color_hkr;
				case 'R' : return Prefs.gui_seq_color_hkr;
				
				case 'F' : return Prefs.gui_seq_color_fwy;
				case 'W' : return Prefs.gui_seq_color_fwy;
				case 'Y' : return Prefs.gui_seq_color_fwy;
				
				case 'I' : return Prefs.gui_seq_color_ilmv;
				case 'L' : return Prefs.gui_seq_color_ilmv;
				case 'M' : return Prefs.gui_seq_color_ilmv;
				case 'V' : return Prefs.gui_seq_color_ilmv;
			
				default: return Prefs.gui_seq_color_gaps;
			}
		}
	}
}