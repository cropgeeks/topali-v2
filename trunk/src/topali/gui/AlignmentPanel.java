// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.*;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.*;
import topali.gui.SequenceListPanel.MyPopupMenuAdapter;

/* Parent container for the canvas used to draw the sequence data. */
public class AlignmentPanel extends JPanel implements AdjustmentListener, PropertyChangeListener
{
	Logger log = Logger.getLogger(this.getClass());

	final int imgBufferType = BufferedImage.TYPE_INT_RGB;
	
	JScrollPane sp;
	JScrollBar hBar, vBar;
	JViewport view;
	
	public SequenceListPanel seqlistPanel;
	public DisplayCanvas displayCanvas;
	HeaderCanvas headerCanvas;
	MyPopupMenuAdapter popupAdapt;
	Scroller scroller;
	
	Font font;
	FontMetrics fm;
	int charW, charH, charDec;
	
	BufferedImage imgBuffer = null;
	boolean useBuffer = true;
	
	//alignment canvas size
	int canW = 0;
	int canH = 0;
	
	//current view (alignment positions)
	int nucStart = 0;
	int seqStart = 0;
	int nucEnd = 0;
	int seqEnd = 0;
	
	//current view (pixels)
	int viewX = 0;
	int viewY = 0;
	int viewW = 0;
	int viewH = 0;
	
	AlignmentData data;
	SequenceSet ss;
	
	public AlignmentPanel(AlignmentData data)
	{
		this.data = data;
		this.data.addChangeListener(this);	
		this.ss = this.data.getSequenceSet();
		
		sp = new JScrollPane();
		hBar = sp.getHorizontalScrollBar();
		vBar = sp.getVerticalScrollBar();
		hBar.addAdjustmentListener(this);
		vBar.addAdjustmentListener(this);
		view = sp.getViewport();
		
		headerCanvas = new HeaderCanvas();
		headerCanvas.setBackground(Color.WHITE);
		displayCanvas = new DisplayCanvas();
		seqlistPanel = new SequenceListPanel(this, ss);
		
		sp.setViewportView(displayCanvas);
		sp.setRowHeaderView(seqlistPanel);
		sp.setColumnHeaderView(headerCanvas);
		sp.getViewport().setBackground(Color.white);
		
		scroller = new Scroller();
		scroller.start();
		
		setLayout(new BorderLayout());
		add(sp, BorderLayout.CENTER);
		
		setBackground(Color.WHITE);
		
		refreshAndRepaint();
	}
	
	/**
	 * Recalculates all relevant display values (e.g. after font size changed)
	 */
	public void refreshAndRepaint() {
		int bold = Prefs.gui_seq_font_bold ? Font.BOLD : Font.PLAIN;
		font = new Font("Monospaced", bold, Prefs.gui_seq_font_size);
		fm = new java.awt.image.BufferedImage(1, 1,
				java.awt.image.BufferedImage.TYPE_INT_RGB).getGraphics()
				.getFontMetrics(font);
		charW = fm.charWidth('G');
		charH = fm.getHeight();
		charDec = fm.getMaxDescent();
		
		hBar.setUnitIncrement(charW);
		vBar.setUnitIncrement(charH);
		vBar.setBlockIncrement(charH);

		canW = ss.getSequence(0).getLength() * charW + (charW - 1);
		canH = ss.getSize() * charH;
		
		if(useBuffer) {
			displayCanvas.createBuffer();
		}
		else
			imgBuffer = null;
		
		headerCanvas.recalculate();
		sp.setColumnHeaderView(headerCanvas);
		
		seqlistPanel.refreshAndRepaint();
		sp.setRowHeaderView(seqlistPanel);
		
		displayCanvas.refreshAndRepaint();
		sp.setViewportView(displayCanvas);
	}
	
	public SequenceSet getSequenceSet()
	{
		return ss;
	}

	public AlignmentData getAlignmentData()
	{
		return data;
	}
	
	public void findSequence(int index, boolean select)
	{
		seqlistPanel.findSequence(sp, index, select);
	}
	
	public void updateOverviewDialog()
	{
		WinMain.ovDialog.setPanelPosition(nucStart, nucEnd-nucStart,
				seqStart, seqEnd-seqStart);
	}
	
	/**
	 * Move view to a certain position
	 * 
	 * @param nuc
	 *            Nucleotide to place in view
	 * @param seq
	 *            Sequence to place in view
	 * @param center
	 *            Center view
	 * @param justWhenOutOfVisArea
	 *            Move only if position is really out of view.
	 */
	public void jumpToPosition(int nuc, int seq, boolean center,
			boolean justWhenOutOfVisArea)
	{
		Rectangle vSize = sp.getViewport().getViewRect();
		Point vPos = sp.getViewport().getViewPosition();

		int visSeqMin = (int) (vPos.getY() / charH);
		int visSeqMax = (int) ((vPos.getY() + vSize.getHeight()) / charH);
		int visNucMin = (int) (vPos.getX() / charW);
		int visNucMax = (int) ((vPos.getX() + vSize.getWidth()) / charW);

		boolean outOfVisArea = (seq < visSeqMin || seq > visSeqMax
				|| nuc < visNucMin || nuc > visNucMax);

		int x = nuc * charW - (charW);
		int y = seq * charH - (charH);
		if (center)
		{
			x -= vSize.getWidth() / 2;
			y -= vSize.getHeight() / 2;
			if (x > sp.getHorizontalScrollBar().getMaximum())
				x = sp.getHorizontalScrollBar().getMaximum();
			if (y > sp.getVerticalScrollBar().getMaximum())
				y = sp.getVerticalScrollBar().getMaximum();
			if (x < 0)
				x = 0;
			if (y < 0)
				y = 0;
		}

		if (outOfVisArea || !justWhenOutOfVisArea)
		{
			sp.getHorizontalScrollBar().setValue(x);
			sp.getVerticalScrollBar().setValue(y);
		}
	}
	
	public void highlight(int seq, int nuc, boolean localCall)
	{
		highlight(seq, 0, nuc, 0, localCall);
	}

	/**
	 * Highlight a certain (area of) nucleotide(s)
	 * 
	 * @param seq
	 *            Sequence to start highlighting
	 * @param seqRange
	 *            No of sequences to highlight
	 * @param nuc
	 *            Nucleotid to start highlighting
	 * @param nucRange
	 *            No of nucleotides to highlight
	 * @param localCall
	 *            Flag if this method was called locally or as response to an
	 *            external message
	 */
	public void highlight(int seq, int seqRange, int nuc, int nucRange,
			boolean localCall)
	{
		if (localCall)
		{
			if (WinMain.vEvents != null && seq >= 0)
			{
				WinMain.vEvents.sendAlignmentPanelMouseOverEvent(ss.getSequence(seq), nuc);
			}
		}

		if (!localCall && nuc > -1 && seq > -1)
		{
			jumpToPosition(nuc, seq, true, true);
		}

		displayCanvas.mouse.nucPosS = nuc;
		displayCanvas.mouse.nucPosE = nuc + nucRange;
		displayCanvas.mouse.seqPosS = seq;
		displayCanvas.mouse.seqPosE = seq + seqRange;
		
		updateStatusBar(seq, seqRange, nuc+1, nucRange);

		this.repaint();
	}
	
	/**
	 * Update status bar to match current mouse highlight/selection
	 * 
	 * @param seq
	 * @param seqRange
	 * @param nuc
	 * @param nucRange
	 */
	private void updateStatusBar(int seq, int seqRange, int nuc, int nucRange)
	{
		if (seq == -1 || nuc == -1)
		{
			WinMainStatusBar.setText("");
			setToolTipText(null);
		} else
		{
			String text = "";
			if (seqRange > 0 && Prefs.gui_show_horizontal_highlight)
			{
				if (nucRange > 0 && Prefs.gui_show_vertical_highlight)
					text = "Seq " + (seq + 1) + "-" + (seq + 1 + seqRange)
							+ ": " + ss.getSequence(seq).getName() + "-"
							+ ss.getSequence(seq + seqRange).getName() + " (" + nuc
							+ "-" + (nuc + nucRange) + ")";
				else
					text = "Seq " + (seq + 1) + "-" + (seq + 1 + seqRange)
							+ ": " + ss.getSequence(seq).getName() + "-"
							+ ss.getSequence(seq + seqRange).getName();
			} else
			{
				if (nucRange > 0 && Prefs.gui_show_vertical_highlight)
					text = "(" + nuc + "-" + (nuc + nucRange) + ")";
				else
					text = "Seq " + (seq + 1) + ": " + ss.getSequence(seq).getName()
							+ " (" + nuc + ")";
			}

			WinMainStatusBar.setText(text);
			if (Prefs.gui_seq_tooltip)
				setToolTipText(text);
		}
	}
	
	/**
	 * Start the scroller (direction and speed depends on mouse coordinates)
	 * 
	 * @param mouse
	 */
	private void scroll(Point mouse)
	{
		Rectangle vSize = sp.getViewport().getViewRect();
		Point vPos = sp.getViewport().getViewPosition();
		int x1 = (int) vPos.getX();
		int y1 = (int) vPos.getY();
		int x2 = (int) (x1 + vSize.getWidth());
		int y2 = (int) (y1 + vSize.getHeight());

		boolean scroll = false;

		// if there's a need for scrolling, notify the scroller
		if (mouse.getX() < x1)
		{
			scroller.left = (int) (x1 - mouse.getX());
			scroll = true;
		}
		if (mouse.getX() > x2)
		{
			scroller.right = (int) (mouse.getX() - x2);
			scroll = true;
		}
		if (mouse.getY() < y1)
		{
			scroller.up = (int) (y1 - mouse.getY());
			scroll = true;
		}
		if (mouse.getY() > y2)
		{
			scroller.down = (int) (mouse.getY() - y2);
			scroll = true;
		}

		scroller.setRunning(scroll);
		synchronized (scroller)
		{
			scroller.notify();
		}
	}
	
	public Color getColor(char c)
	{
		if (ss.isDNA())
		{
			// DNA COLOUR CODING
			switch (c)
			{
			case 'A':
				return Prefs.gui_seq_color_a;
			case 'C':
				return Prefs.gui_seq_color_c;
			case 'G':
				return Prefs.gui_seq_color_g;
			case 'T':
				return Prefs.gui_seq_color_t;
			case 'U':
				return Prefs.gui_seq_color_t;

			default:
				return Prefs.gui_seq_color_gaps;
			}
		} else
		{
			// PROTEIN COLOUR CODING
			switch (c)
			{
			case 'G':
				return Prefs.gui_seq_color_gpst;
			case 'P':
				return Prefs.gui_seq_color_gpst;
			case 'S':
				return Prefs.gui_seq_color_gpst;
			case 'T':
				return Prefs.gui_seq_color_gpst;

			case 'H':
				return Prefs.gui_seq_color_hkr;
			case 'K':
				return Prefs.gui_seq_color_hkr;
			case 'R':
				return Prefs.gui_seq_color_hkr;

			case 'F':
				return Prefs.gui_seq_color_fwy;
			case 'W':
				return Prefs.gui_seq_color_fwy;
			case 'Y':
				return Prefs.gui_seq_color_fwy;

			case 'I':
				return Prefs.gui_seq_color_ilmv;
			case 'L':
				return Prefs.gui_seq_color_ilmv;
			case 'M':
				return Prefs.gui_seq_color_ilmv;
			case 'V':
				return Prefs.gui_seq_color_ilmv;

			default:
				return Prefs.gui_seq_color_gaps;
		}
		}
	}
		
	void setPopupMenu(MyPopupMenuAdapter popup)
	{
		this.popupAdapt = popup;
		displayCanvas.addMouseListener(this.popupAdapt);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		log.info("PropertyChangeEvent: "+evt);
		imgBuffer = null;
		refreshAndRepaint();
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		Dimension viewSize = view.getExtentSize();
		Point viewPoint = view.getViewPosition();
		
		viewX = (int)viewPoint.getX();
		viewY = (int)viewPoint.getY();
		viewW = (int)viewSize.getWidth();
		viewH = (int)viewSize.getHeight();
		
		nucStart = (viewX / charW);
		seqStart = (viewY / charH);
		nucEnd = nucStart + (viewW/charW);
		if(nucEnd>=ss.getLength())
			nucEnd = ss.getLength()-1;
		seqEnd = seqStart + (viewH/charH);
		if(seqEnd>=ss.getSize())
			seqEnd = ss.getSize()-1;
		
		//System.out.println(nucStart+","+nucEnd+" "+seqStart+","+seqEnd);
		
		sp.setViewportView(displayCanvas);
		
		updateOverviewDialog();
		repaint();
	}

	// Secondary canvas (row header) that appears along the top of the alignment
	class HeaderCanvas extends JPanel
	{
		int y1 = charH / 2 + 2;
		int y2 = 2 * y1 + 2;

		/**
		 * Recalculates values (e.g. after font size changed)
		 */
		private void recalculate() {
			y1 = charH / 2 + 2;
			y2 = 2 * y1 + 2;
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(canW, y2 + 1);
		}

		// Paints Clustal *** information and column numbers
		@Override
		public void paintComponent(Graphics g)
		{	
			super.paintComponent(g);

			g.setFont(font);
			g.setColor(Prefs.gui_seq_color_text);

			// *** * ** data
			char str[] = ss.getOverview().substring(nucStart, nucEnd).toCharArray();

			for (int i = 0, x = viewX; i < str.length; i++, x += charW) {
				g.drawString("" + str[i], x, y2);
			}

			// Column numbers
			int highlightHeight = charH - charDec;
			for (int i = nucStart + 1, x = viewX; i <= nucEnd; i++, x += charW)
			{
				if (i % 25 == 0)
				{
					int sub = (("" + i).length() - 1) * charW;
					g.setColor(new Color(224, 223, 227));
					g.fillRect(x, 0, charW, highlightHeight);
					g.setColor(Prefs.gui_seq_color_text);
					g.drawString("" + i, x - sub, y1);
				}
			}
		}
	}

	
	class DisplayCanvas extends JPanel
	{
		DiplayCanvasMouseListener mouse;
		
		Color mouseHLColor;
		
		DisplayCanvas()
		{
			setBackground(Color.white);
			setOpaque(false);
			
			mouse = new DiplayCanvasMouseListener(this);
			this.addMouseListener(mouse);
			this.addMouseMotionListener(mouse);
			
			mouseHLColor = new Color(Prefs.gui_seq_highlight
					.getRed(), Prefs.gui_seq_highlight.getGreen(),
					Prefs.gui_seq_highlight.getBlue(), 75);
		}

		@Override
		public Dimension getSize()
		{
			return new Dimension(canW, canH);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return getSize();
		}

		private void refreshAndRepaint() {
			setSize(canW, canH);
			repaint();
		}
		
		private void createBuffer() {
			long start = System.currentTimeMillis();
			
			if(imgBuffer!=null) {
				imgBuffer = null;
				System.runFinalization();
				System.gc();
			}
			
			//Use a maximum of 50% of the free heap space for buffering:
			MemoryMXBean membean = ManagementFactory.getMemoryMXBean();
			long freeMem = membean.getHeapMemoryUsage().getMax()-membean.getHeapMemoryUsage().getUsed();
			long maxBufferSize = (long)(freeMem/2);
			long imgSize = canH*canW;
			String logMsg = "Using max. "+(maxBufferSize/1024/1024)+" mb for alignment display buffer. " +
					"("+(imgSize/1024/1024)+" mb needed)";
			log.info(logMsg);
			if(imgSize<maxBufferSize) {
				try {
					imgBuffer = new BufferedImage(canW, canH, imgBufferType);
				}
				catch (Throwable e) {
					log.warn("Image Buffer still too big, switched back to direct painting.");
					imgBuffer = null;
					return;
				}
			}
			else {
				log.info("Image buffer size to high, will use direct painting");
				imgBuffer = null;
				return;
			}
			
			Graphics g = imgBuffer.createGraphics();
			
			g.setFont(font);

			for (int seq = 0, y = charH; seq <= ss.getLength(); seq++, y += charH)
			{
				if (seq >= ss.getSize())
					break;

				boolean drawDim = !seqlistPanel.getList().isSelectedIndex(seq)
				&& Prefs.gui_seq_dim;
				
				// Extract the text to display in this section
				char str[] = ss.getSequence(seq).getBuffer().toString().toCharArray();

				int y1 = y - charH;
				int y2 = y - charDec;

				{
					for (int i = 0, x = 0; i < str.length; i++, x += charW)
					{
						
						if (Prefs.gui_seq_show_colors && drawDim == false)
						{
							g.setColor(getColor(str[i]));

							// Subset selection highlighting
							int cS = data.getActiveRegionS();
							int cE = data.getActiveRegionE();
							if ((i + 1) < cS || (i + 1) > cE) {
								g.setColor(g.getColor().darker().darker());
							}

						} else {
							g.setColor(Color.WHITE);
						}

						g.fillRect(x, y1, charW, charH);
					}
				}

				if (Prefs.gui_seq_show_text) {
					if (drawDim == false)
						g.setColor(Prefs.gui_seq_color_text);
					else
						g.setColor(new Color(235, 235, 235));
					g.drawString(new String(str), viewX, y2);
				}
			}
			
			g.dispose();
			
			log.info("Alignment display buffer created ("+(System.currentTimeMillis()-start)+" ms)");
		}
		
		@Override
		public void paintComponent(Graphics g)
		{	
			super.paintComponent(g);
			if(imgBuffer!=null)
				bufferPaint(g);
			else
				directPaint(g);
			
			mouseOverHighlight(g);
		}
		
		private void bufferPaint(Graphics g) {	
			int x = charW * nucStart;
			int y = charH * seqStart;
			int w = (nucEnd-nucStart+1)*charW;
			int h = (seqEnd-seqStart+1)*charH;
			
			//for some reason we need an intermediate buffer here,
			//otherwise image could get smeared.
			BufferedImage tmp = new BufferedImage(w, h, imgBufferType);
			Graphics gtmp = tmp.createGraphics();
			gtmp.drawImage(imgBuffer, 0, 0, w, h, x, y, x+w, y+h, null);
			gtmp.dispose();
			
			g.drawImage(tmp, x, y, x+w, y+h, 0, 0, w, h, null);
			
			//g.drawImage(imgBuffer, x, y, x+w, y+h,  x, y, x+w, y+h, null);
		}
		
		private void directPaint(Graphics g)
		{	
			g.setFont(font);

			for (int seq = seqStart, y = charH + (charH * seqStart); seq <= seqEnd; seq++, y += charH)
			{
				if (seq >= ss.getSize())
					break;

				boolean drawDim = !seqlistPanel.getList().isSelectedIndex(seq)
				&& Prefs.gui_seq_dim;
				
				// Extract the text to display in this section
				char str[] = ss.getSequence(seq).getBuffer().substring(nucStart, nucEnd).toCharArray();

				int y1 = y - charH;
				int y2 = y - charDec;

				for (int i = 0, x = viewX; i < str.length; i++, x += charW)
				{
					if (Prefs.gui_seq_show_colors && drawDim == false)
					{
						g.setColor(getColor(str[i]));

						// Subset selection highlighting
						int cS = data.getActiveRegionS();
						int cE = data.getActiveRegionE();
						if ((nucStart + i + 1) < cS || (nucStart + i + 1) > cE)
							g.setColor(g.getColor().darker().darker());

					} else
						g.setColor(Color.WHITE);

					g.fillRect(x, y1, charW, charH);
				}

				if (Prefs.gui_seq_show_text) {
					if (drawDim == false)
						g.setColor(Prefs.gui_seq_color_text);
					else
						g.setColor(new Color(235, 235, 235));
					g.drawString(new String(str), viewX, y2);
				}
			}
		}
		
		private void mouseOverHighlight(Graphics g) {
			if((mouse.nucPosS==-1 || mouse.seqPosS==-1) && (mouse.curNucPos==-1 || mouse.curSeqPos==-1)) {
				//mouse is outside alignment and there is no area highlighted,
				//so do nothing
				return;
			}
			
			g.setColor(mouseHLColor);
			
			if(Prefs.gui_show_horizontal_highlight) {	
				
				int x = viewX;
				int y;
				int w = (nucEnd - nucStart+1)*charW;
				int h;
				
				if(mouse.nucPosS>-1 && mouse.seqPosS>-1) {
					int seqPos;
					int dist;
					if(mouse.seqPosS<mouse.seqPosE) {
						seqPos = mouse.seqPosS;
						dist = mouse.seqPosE - mouse.seqPosS;
					}
					else {
						seqPos = mouse.seqPosE;
						dist = mouse.seqPosS - mouse.seqPosE;
					}
					y = seqPos * charH;
					h = dist * charH + charH;
				}
				else {
					y = mouse.curSeqPos*charH;
					h = charH;
				}
				g.fillRect(x, y, w, h);
			}
			
			if(Prefs.gui_show_vertical_highlight) {
				int x;
				int y = viewY;
				int w;
				int h = (seqEnd-seqStart+1)*charH;
				
				if(mouse.nucPosS>-1 && mouse.seqPosS>-1) {
					int nucPos;
					int dist;
					if(mouse.nucPosS<mouse.nucPosE) {
						nucPos = mouse.nucPosS;
						dist = mouse.nucPosE - mouse.nucPosS;
					}
					else {
						nucPos = mouse.nucPosE;
						dist = mouse.nucPosS - mouse.nucPosE;
					}
					x = nucPos * charW;
					w = dist * charW + charW;
				}
				
				else {
					x = mouse.curNucPos * charW;
					w = charW;
				}
				
				g.fillRect(x, y, w, h);
			}

			if(mouse.nucPosS>-1 && mouse.seqPosS>-1) {
				int seq = (mouse.seqPosS<mouse.seqPosE) ? mouse.seqPosS : mouse.seqPosE;
				int nuc = (mouse.nucPosS<mouse.nucPosE) ? mouse.nucPosS : mouse.nucPosE;
				int seqDist = mouse.seqPosS-mouse.seqPosE;
				if(seqDist<0)
					seqDist *= -1;
				int nucDist = mouse.nucPosS-mouse.nucPosE;
				if(nucDist<0)
					nucDist *= -1;
				
				updateStatusBar(seq, seqDist, nuc+1, nucDist);
			}
			else {
				updateStatusBar(mouse.curSeqPos, -1, mouse.curNucPos+1, -1);
			}
			
			if(mouse.nucPosS!=mouse.nucPosE)
				popupAdapt.enableAnnotate(true);
			else
				popupAdapt.enableAnnotate(false);
			
			if(mouse.seqPosS!=mouse.seqPosE)
				popupAdapt.enableSelectHighlighted(true);
			else
				popupAdapt.enableSelectHighlighted(false);
		}
	}
	
	class DiplayCanvasMouseListener implements MouseListener, MouseMotionListener {

		//current mouse pos
		int curNucPos = -1;
		int curSeqPos = -1;
		
		//mouse selection
		int nucPosS = -1;
		int nucPosE = -1;
		int seqPosS = -1;
		int seqPosE = -1;
		
		Point rightClick = null;
		Point leftClick = null;
		
		DisplayCanvas canvas;
		
		public DiplayCanvasMouseListener(DisplayCanvas canvas) {
			this.canvas = canvas;
		}
		
		@Override
		public void mouseDragged(MouseEvent e)
		{	
			if(rightClick!=null) {
				int dx = rightClick.x-e.getPoint().x;
				int dy = rightClick.y-e.getPoint().y;
				int newX = hBar.getValue()+dx;
				int newY = vBar.getValue()+dy;
				//if(newX>0 && newX<hBar.getMaximum())
				hBar.setValue(newX);	
				vBar.setValue(newY);
				popupAdapt.setEnabled(false);
			}
			
			if(leftClick!=null) {
				int x = e.getPoint().x;
				int y = e.getPoint().y;
				nucPosE = (x/charW);
				seqPosE = (y/charH);
				
				if(nucPosE>nucEnd)
					nucPosE = nucEnd;
				if(seqPosE>seqEnd)
					seqPosE = seqEnd;
			}
			
			mouseMoved(e);
			
			scroll(e.getPoint());
		}

		@Override
		public void mouseMoved(MouseEvent e)
		{	
			int x = e.getPoint().x;
			int y = e.getPoint().y;
			
			curNucPos = (x/charW);
			curSeqPos = (y/charH);
			
			if(curNucPos>nucEnd)
				curNucPos = -1;
			if(curSeqPos>seqEnd)
				curSeqPos = -1;
			
			canvas.repaint();
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{	
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{	
		}

		@Override
		public void mouseExited(MouseEvent e)
		{	
		}

		@Override
		public void mousePressed(MouseEvent e)
		{	
			if(e.getButton()==MouseEvent.BUTTON3) {
				rightClick = e.getPoint();
				canvas.setCursor(new Cursor(Cursor.HAND_CURSOR));
				popupAdapt.setEnabled(true);
			}
			
			if(e.getButton()==MouseEvent.BUTTON1) {
				leftClick = e.getPoint();
				int x = e.getPoint().x;
				int y = e.getPoint().y;
				nucPosS = (x/charW);
				seqPosS = (y/charH);
				nucPosE = -1;
				seqPosE = -1;
			}
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			if(e.getButton()==MouseEvent.BUTTON3) {
				rightClick = null;
				canvas.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			
			if(e.getButton()==MouseEvent.BUTTON1) {
				leftClick = null;
				
				if(nucPosE==-1 && seqPosE==-1) {
					//user made just left mouse click without dragging,
					//so remove highlight area
					nucPosS = -1;
					seqPosS = -1;
				}
			}
		}
		
	}
	
	/**
	 * Scrolls the canvas. Set up, right, down and/or left (the value determines
	 * the scrolling speed) and set running=true.
	 */
	class Scroller extends Thread
	{
		public int up = 0;

		public int right = 0;

		public int down = 0;

		public int left = 0;

		boolean running = false;

		public Scroller()
		{
		}

		@Override
		public void run()
		{
			while (true)
			{
				while (running)
				{
					int value;
					if (up > 0)
					{
						value = vBar.getValue();
						if (value <= up)
							this.running = false;
						vBar.setValue(value - up);
					}
					if (right > 0)
					{
						value = hBar.getValue();
						if (value > (canW - right))
							this.running = false;
						hBar.setValue(value + right);
					}
					if (down > 0)
					{
						value = vBar.getValue();
						if (value > (canH - down))
							this.running = false;
						vBar.setValue(value + down);
					}
					if (left > 0)
					{
						value = hBar.getValue();
						if (value <= left)
							this.running = false;
						hBar.setValue(value - left);
					}

					try
					{
						Thread.sleep(10);
					} catch (InterruptedException e)
					{
						log.warn(e);
					}
				}
				this.up = 0;
				this.right = 0;
				this.down = 0;
				this.left = 0;

				synchronized (this)
				{
					try
					{
						wait();
					} catch (InterruptedException e)
					{
					}
				}
			}

		}

		public void setRunning(boolean b)
		{
			this.running = b;
		}

		public boolean isRunning()
		{
			return this.running;
		}
	}
}