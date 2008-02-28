// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.*;
import java.lang.management.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.*;
import topali.gui.AlignmentPanel.DisplayCanvas.BufferThread;
import topali.gui.SequenceListPanel.MyPopupMenuAdapter;
import topali.var.threads.*;

/* Parent container for the canvas used to draw the sequence data. */
public class AlignmentPanel extends JPanel implements AdjustmentListener, PropertyChangeListener
{
	Logger log = Logger.getLogger(this.getClass());

	final int imgBufferType = BufferedImage.TYPE_USHORT_555_RGB;

	JScrollPane sp;
	JScrollBar hBar, vBar;
	JViewport view;

	public SequenceListPanel seqlistPanel;
	public DisplayCanvas displayCanvas;
	HeaderCanvas headerCanvas;
	MyPopupMenuAdapter popupAdapt;

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

		setLayout(new BorderLayout());
		add(sp, BorderLayout.CENTER);

		setBackground(Color.WHITE);

		refreshAndRepaint();
	}

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#refreshAndRepaint()
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

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#getSequenceSet()
	 */
	public SequenceSet getSequenceSet()
	{
		return ss;
	}

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#getAlignmentData()
	 */
	public AlignmentData getAlignmentData()
	{
		return data;
	}

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#findSequence(int, boolean)
	 */
	public void findSequence(int index, boolean select)
	{
		seqlistPanel.findSequence(sp, index, select);
	}

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#updateOverviewDialog()
	 */
	public void updateOverviewDialog()
	{
		WinMain.ovDialog.setPanelPosition(nucStart, nucEnd-nucStart,
				seqStart, seqEnd-seqStart);
	}

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#jumpToPosition(int, int, boolean, boolean)
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

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#highlight(int, int, boolean)
	 */
	public void highlight(int seq, int nuc, boolean localCall)
	{
		highlight(seq, 0, nuc, 0, localCall);
	}

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#highlight(int, int, int, int, boolean)
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

		if (!localCall && nuc > 0 && seq > -1)
		{
			jumpToPosition(nuc, seq, true, true);
			displayCanvas.mouse.nucPosS = nuc-1;
			displayCanvas.mouse.nucPosE = nuc-1 + nucRange;
			displayCanvas.mouse.seqPosS = seq;
			displayCanvas.mouse.seqPosE = seq + seqRange;
			this.repaint();
		}
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
	    if(seq<0 || (seq+seqRange)>ss.getSize() || nuc<0 || (nuc+nucRange)>ss.getLength())
		return;

		try
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
		} catch (RuntimeException e)
		{
			// TODO: Check what's actually going wrong here.
			log.info("TODO: Check what's actually going wrong here.", e);
		}
	}

	private void scroll(Point mouse) {
		Rectangle vSize = sp.getViewport().getViewRect();
		Point vPos = sp.getViewport().getViewPosition();
		int x1 = (int) vPos.getX();
		int y1 = (int) vPos.getY();
		int x2 = (int) (x1 + vSize.getWidth());
		int y2 = (int) (y1 + vSize.getHeight());

		if(mouse.getX()<x1 || mouse.getX()>x2 || mouse.getY()<y1 || mouse.getY()>y2) {
		    Rectangle scrollTo = new Rectangle((int)mouse.getX()-5, (int)mouse.getY()-5, 10, 10);
		    displayCanvas.scrollRectToVisible(scrollTo);
		}
	}

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#getColor(char)
	 */
	public Color getColor(char c)
	{
		if (ss.getParams().isDNA())
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

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		repaint();
	}

	/* (non-Javadoc)
	 * @see topali.gui.IAlignmentPanel#adjustmentValueChanged(java.awt.event.AdjustmentEvent)
	 */
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

		BufferedImage[] nucs;
		BufferedImage[] aas;
		BufferedImage gap;
		BufferedImage unknown;

		BufferThread bufferThread = null;

		DisplayCanvas()
		{
			setBackground(Color.white);
			setOpaque(false);

			mouse = new DiplayCanvasMouseListener(this);
			this.addMouseListener(mouse);
			this.addMouseMotionListener(mouse);

			mouseHLColor = new Color(Prefs.gui_seq_highlight
					.getRed(), Prefs.gui_seq_highlight.getGreen(),
					Prefs.gui_seq_highlight.getBlue(), 130);
		}

		private void init() {
			nucs = new BufferedImage[4];
			aas = new BufferedImage[20];

			gap = createMiniImg('-');

			unknown = createMiniImg('?');

			nucs[0] = createMiniImg('A');
			nucs[1] = createMiniImg('C');
			nucs[2] = createMiniImg('G');
			nucs[3] = createMiniImg('T');

			aas[0] = createMiniImg('A');
			aas[1] = createMiniImg('R');
			aas[2] = createMiniImg('N');
			aas[3] = createMiniImg('D');
			aas[4] = createMiniImg('C');
			aas[5] = createMiniImg('E');
			aas[6] = createMiniImg('Q');
			aas[7] = createMiniImg('G');
			aas[8] = createMiniImg('H');
			aas[9] = createMiniImg('I');
			aas[10] = createMiniImg('L');
			aas[11] = createMiniImg('K');
			aas[12] = createMiniImg('M');
			aas[13] = createMiniImg('F');
			aas[14] = createMiniImg('P');
			aas[15] = createMiniImg('S');
			aas[16] = createMiniImg('T');
			aas[17] = createMiniImg('W');
			aas[18] = createMiniImg('Y');
			aas[19] = createMiniImg('V');
		}

		private BufferedImage createMiniImg(char c) {
			BufferedImage img = new BufferedImage(charW, charH, imgBufferType);
			Graphics2D g = img.createGraphics();
			if(Prefs.gui_seq_antialias)
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			else
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setFont(font);
			Color txtCol = Color.BLACK;
			Color col1 = getColor(c);
			Color col2 = col1.darker();

			int x = 0;
			int y = 0;

			if(Prefs.gui_seq_show_colors)
				g.setPaint(new GradientPaint(x, (y+charH), col2, (x+charW), y, col1));
			else
				g.setColor(Color.WHITE);

			g.fillRect(x, y, charW, charH);

			if(Prefs.gui_seq_show_text) {
				g.setColor(txtCol);
				g.drawString(""+c, x, y+charH-charDec);
			}

			g.dispose();

			return img;
		}

		private BufferedImage getMiniImg(char c)
		{
			if(c=='-')
				return gap;

			if (ss.getParams().isDNA())
			{
				int index = -1;

				switch (c)
				{
				case 'A':
					index = 0; break;
				case 'C':
					index = 1; break;
				case 'G':
					index = 2; break;
				case 'T':
					index = 3; break;

				default:
					return unknown;
				}

				return nucs[index];

			} else
			{
				int index = -1;

				switch (c)
				{
				case 'A':
					index = 0; break;
				case 'R':
					index = 1; break;
				case 'N':
					index = 2; break;
				case 'D':
					index = 3; break;
				case 'C':
					index = 4; break;
				case 'E':
					index = 5; break;
				case 'Q':
					index = 6; break;
				case 'G':
					index = 7; break;
				case 'H':
					index = 8; break;
				case 'I':
					index = 9; break;
				case 'L':
					index = 10; break;
				case 'K':
					index = 11; break;
				case 'M':
					index = 12; break;
				case 'F':
					index = 13; break;
				case 'P':
					index = 14; break;
				case 'S':
					index = 15; break;
				case 'T':
					index = 16; break;
				case 'W':
					index = 17; break;
				case 'Y':
					index = 18; break;
				case 'V':
					index = 19; break;

				default:
					return unknown;
				}

				return aas[index];
			}
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
			init();
			repaint();
		}

		private void createBuffer() {
			if(bufferThread!=null) {
				bufferThread.kill = true;
				bufferThread.interrupt();
			}
			bufferThread = new BufferThread(this);
			bufferThread.start();
		}

		/**
		 * This will shade the sequences, which are not selected,
		 * and if a partition is selected, the area outside the selected partition.
		 * @param g
		 */
		private void paintMask(Graphics g) {

			//Partition highlighting
			int cS = data.getActiveRegionS();
			int cE = data.getActiveRegionE();
			int height = (seqEnd*charH - seqStart*charH + charH);
			if(height>viewH)
				height = viewH;

			for(int nuc = nucStart, x = (nucStart*charW); nuc <= nucEnd; nuc++, x += charW) {
				if((nuc+1)<cS || (nuc+1)>cE) {
					g.setColor(new Color(0,0,0,150));
					g.fillRect(x, viewY, charW, height);
				}
			}

			//Sequence highlighting
			JList seqList = seqlistPanel.getList();
			if(Prefs.gui_seq_dim && seqList.getSelectedIndices().length<seqList.getModel().getSize()) {
				int width = (nucEnd*charW - nucStart*charW + charW);
				if(width>viewW)
					width = viewW;
				for (int seq = seqStart, y = (seqStart*charH); seq <= seqEnd; seq++, y += charH)
				{
					boolean drawDim = !seqlistPanel.getList().isSelectedIndex(seq)
					&& Prefs.gui_seq_dim;

					if(drawDim) {
						g.setColor(new Color(0,0,0,150));
						g.fillRect(viewX, y, width, charH);
					}
				}
			}
		}

		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if(imgBuffer!=null)
				bufferPaint(g);
			else
				directPaint(g);

			paintMask(g);
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
		}

		private void directPaint(Graphics g)
		{
			g.setFont(font);

			for (int seq = seqStart, y = (seqStart*charH); seq <= seqEnd; seq++, y += charH)
			{
				// Extract the text to display in this section
				char str[] = ss.getSequence(seq).getBuffer().substring(nucStart, nucEnd).toCharArray();

				for (int i = 0, x = (nucStart*charW); i < str.length; i++, x += charW)
				{
					BufferedImage img = getMiniImg(str[i]);
					g.drawImage(img, x, y, charW, charH, null);
				}
			}
		}

		/**
		 * Draws the mouseover highlight into the graphics
		 * @param g
		 */
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
				highlight(seq, nuc+1, true);
			}
			else {
				updateStatusBar(mouse.curSeqPos, -1, mouse.curNucPos+1, -1);
				highlight(mouse.curSeqPos, mouse.curNucPos+1, true);
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

		class BufferThread extends Thread {

			public boolean kill;

			private DisplayCanvas canvas;

			public BufferThread(DisplayCanvas canvas) {
				this.canvas = canvas;
			}

			@Override
			public void run()
			{

				try {
						Thread.sleep(2000);
						if(kill)
							return;
				}
				catch (InterruptedException e1) {
					if(kill)
						return;
				}

				long start = System.currentTimeMillis();

				init();

				if(imgBuffer!=null) {
					imgBuffer = null;
					System.runFinalization();
					System.gc();
				}

				//Use a maximum of 50% of the free heap space for buffering:
				MemoryMXBean membean = ManagementFactory.getMemoryMXBean();
				long freeMem = membean.getHeapMemoryUsage().getMax()-membean.getHeapMemoryUsage().getUsed();
				long maxBufferSize = (freeMem/2);
				long factor = 1;
				switch(imgBufferType) {
					case BufferedImage.TYPE_INT_RGB: factor = 3; break;
					case BufferedImage.TYPE_USHORT_555_RGB: factor = 2; break;
					case BufferedImage.TYPE_BYTE_INDEXED: factor = 1; break;
					default: factor = 1;
				}
				long imgSize = canH*canW*factor;
				String logMsg = "Using max. "+(maxBufferSize/1024/1024)+" mb for alignment display buffer. " +
						"("+(imgSize/1024/1024)+" mb needed)";
				log.info(logMsg);

				BufferedImage tmpBuffer = null;
				if(imgSize<maxBufferSize) {
					try {
						tmpBuffer = new BufferedImage(canW, canH, imgBufferType);
					}
					catch (Throwable e) {
						log.warn("Image Buffer still too big, switched back to direct painting.");
						tmpBuffer = null;
						return;
					}
				}
				else {
					log.info("Image buffer size to high, will use direct painting");
					imgBuffer = null;
					return;
				}

				Graphics g = tmpBuffer.createGraphics();

				g.setFont(font);

				//Draw alignment
				int size = ss.getSize();
				for (int seq = 0, y = 0; seq < size; seq++, y += charH)
				{
					// Extract the text to display in this section
					char str[] = ss.getSequence(seq).getBuffer().toString().toCharArray();

					for (int i = 0, x = 0; i < str.length; i++, x += charW)
					{
						BufferedImage img = getMiniImg(str[i]);
						g.drawImage(img, x, y, img.getWidth(), img.getHeight(), null);
					}
				}

				g.dispose();
				imgBuffer = tmpBuffer;

				canvas.bufferThread = null;

				log.info("Alignment display buffer created ("+(System.currentTimeMillis()-start)+" ms)");
			}
		}
	}

	class DiplayCanvasMouseListener implements MouseListener, MouseMotionListener {

		//current mouse pos
		int curNucPos = -1;
		int curSeqPos = -1;

		//mouse selection (-1 if there is no area selected)
		int nucPosS = -1;
		int nucPosE = -1;
		int seqPosS = -1;
		int seqPosE = -1;

		//Positions of the last click events
		Point rightClick = null;
		Point leftClick = null;

		//if mouse is dragged this value always holds the previous mouse position
		//(needed to "stop" the mouse, if an edge of the alignment display is reached)
		Point oldDragValue;

		//Used to "stop" mouse dragging, if an edge of the alignment display is reached
		//(the robot actually justs sets the mouse cursor back to the previous value)
		Robot robot;

		DisplayCanvas canvas;

		public DiplayCanvasMouseListener(DisplayCanvas canvas) {
			this.canvas = canvas;

			try
			{
				robot = new Robot();
			} catch (Exception e)
			{
				log.warn("Robot creation failed", e);
			}
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{

			if(rightClick!=null) {

				int diffX = oldDragValue.x - e.getPoint().x;
				int diffY = oldDragValue.y - e.getPoint().y;

				hBar.setValue(hBar.getValue() + diffX);
				vBar.setValue(vBar.getValue() + diffY);

/*				int dx = rightClick.x-e.getPoint().x;
				int dy = rightClick.y-e.getPoint().y;
				int newX = hBar.getValue()+dx;
				int newY = vBar.getValue()+dy;

				Point loc = e.getLocationOnScreen();

				if(newX<0 || newX>hBar.getMaximum()-hBar.getVisibleAmount())  {
					robot.mouseMove(oldDragValue.x, e.getLocationOnScreen().y);
					loc.setLocation(oldDragValue.x, loc.y);
				}
				else
					hBar.setValue(newX);

				if(newY<0 || newY>vBar.getMaximum()-vBar.getVisibleAmount()) {
					robot.mouseMove(e.getLocationOnScreen().x, oldDragValue.y);
					loc.setLocation(loc.x, oldDragValue.y);
				}
				else
					vBar.setValue(newY);
*/
				popupAdapt.setEnabled(false);

//				oldDragValue = loc;
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

				scroll(e.getPoint());
			}

			mouseMoved(e);
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
			if(Prefs.gui_show_horizontal_highlight || Prefs.gui_show_vertical_highlight) {
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			}
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
			curNucPos = -1;
			curSeqPos = -1;
			canvas.repaint();

			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			if(e.getButton()==MouseEvent.BUTTON3) {
				rightClick = e.getPoint();
				oldDragValue = e.getPoint(); //e.getLocationOnScreen();
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

				if(nucPosS>ss.getLength() || seqPosS>ss.getSize()) {
					nucPosS = -1;
					seqPosS = -1;
					leftClick = null;
				}

			}
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			if(e.getButton()==MouseEvent.BUTTON3) {
				rightClick = null;
				canvas.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				oldDragValue = null;
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
}