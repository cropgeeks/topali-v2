// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.AlignmentData;
import topali.data.SequenceSet;
import topali.gui.SequenceListPanel.MyPopupMenuAdapter;

/* Parent container for the canvas used to draw the sequence data. */
public class AlignmentPanel extends JPanel implements AdjustmentListener
{
	Logger log = Logger.getLogger(this.getClass());
	
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

	// Fonts used by the display areas
	private FontMetrics fm;

	private Font font;

	final int nSeq, nNuc;

	// //////////////////////////////////////////////////////////////////////////
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

	// //////////////////////////////////////////////////////////////////////////

	// Area to highlight (unit: nuc. and seq. numbers, NOT px!)
	public Rectangle mouseHighlight = new Rectangle(-1, -1, 0, 0);

	// Flag to hold highlight (false = mouse over highlight, true = hold current
	// highlight) (toggled by mouse click)
	public boolean holdMouseHighlight = false;

	// Stores the position, when user clicks the rigth mouse button
	private Point rightMouseCoord = null;

	MyPopupMenuAdapter popup;

	// Scrolls if the user wants to select an area outside the current view.
	Scroller scroller;

	public AlignmentPanel(AlignmentData data)
	{
		this.data = data;
		ss = data.getSequenceSet();

		nSeq = ss.getSize();
		nNuc = ss.getLength();

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

		// Final setup - ensure the header canvas can compute its height
		canvas.setCanvasFont();
		header.computeInitialHeight();

		addPageHandlers();

		scroller = new Scroller();
		scroller.start();

	}

	public SequenceListPanel getListPanel()
	{
		return seqList;
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
		seqList.findSequence(sp, index, select);
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

	void setPopupMenu(MyPopupMenuAdapter popup)
	{
		this.popup = popup;
		canvas.addMouseListener(this.popup);
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
				String seqName = ss.getSequence(seq).name;
				
				WinMain.vEvents.sendAlignmentPanelMouseOverEvent(seqName, nuc);
			}
		}

		if (!localCall && nuc > -1 && seq > -1)
		{
			jumpToPosition(nuc, seq, true, true);
		}

		this.mouseHighlight.setBounds(nuc, seq, nucRange, seqRange);

		if (seq >= 0 && Prefs.gui_show_horizontal_highlight)
			popup.enableSelectHighlighted(true);
		else
			popup.enableSelectHighlighted(false);
		if (nuc >= 0 && Prefs.gui_show_vertical_highlight)
			popup.enableAnnotate(true);
		else
			popup.enableAnnotate(false);
		updateStatusBar(seq, seqRange, nuc, nucRange);

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
							+ ": " + ss.getSequence(seq).name + "-"
							+ ss.getSequence(seq + seqRange).name + " (" + nuc
							+ "-" + (nuc + nucRange) + ")";
				else
					text = "Seq " + (seq + 1) + "-" + (seq + 1 + seqRange)
							+ ": " + ss.getSequence(seq).name + "-"
							+ ss.getSequence(seq + seqRange).name;
			} else
			{
				if (nucRange > 0 && Prefs.gui_show_vertical_highlight)
					text = "(" + nuc + "-" + (nuc + nucRange) + ")";
				else
					text = "Seq " + (seq + 1) + ": " + ss.getSequence(seq).name
							+ " (" + nuc + ")";
			}

			WinMainStatusBar.setText(text);
			if (Prefs.gui_seq_tooltip)
				setToolTipText(text);
		}
	}

	private void addPageHandlers()
	{
		AbstractAction action1 = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				sp.getVerticalScrollBar().setValue(
						sp.getVerticalScrollBar().getValue() + 50);
			}
		};

		AbstractAction action2 = new AbstractAction()
		{
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
		WinMain.ovDialog.setPanelPosition((pX / charW), charCount,
				(pY / charH), charDepth);
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
			fm = new java.awt.image.BufferedImage(1, 1,
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
				str = ss.getOverview().substring(start, start + charCount)
						.toCharArray();

			for (int i = 0, x = pX; i < str.length; i++, x += charW)
				g.drawString("" + str[i], x, y2);

			// Column numbers
			int highlightHeight = charH - charDec;
			for (int i = start + 1, x = pX; i <= start + charCount; i++, x += charW)
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

		int mouseStartSeq = -1;

		int mouseStartNuc = -1;

		class CanvasMouseListener extends MouseAdapter
		{
			int x = -1;

			int y = -1;

			public void mouseExited(MouseEvent e)
			{
				if (!holdMouseHighlight)
				{
					highlight(-1, -1, true);
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				super.mousePressed(e);

				if (e.getButton() == MouseEvent.BUTTON3)
				{
					rightMouseCoord = e.getPoint();
					popup.setEnabled(true);
					return;
				}

				if (e.getButton() == MouseEvent.BUTTON1)
				{
					holdMouseHighlight = !holdMouseHighlight;
					mouseStartNuc = (((e.getX() - pX) / charW) + (pX / charW)) + 1;
					mouseStartSeq = e.getY() / charH;
					if (!holdMouseHighlight)
					{
						highlight(-1, -1, true);
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				super.mouseReleased(e);

				if (e.getButton() == MouseEvent.BUTTON3)
				{
					rightMouseCoord = null;
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					return;
				}

				if (e.getButton() == MouseEvent.BUTTON1)
				{
					mouseStartSeq = -1;
					mouseStartNuc = -1;
					scroller.setRunning(false);
				}
			}

		}

		class CanvasMouseMotionListener extends MouseMotionAdapter
		{

			public void mouseMoved(MouseEvent e)
			{
				if (ss == null
						|| (!Prefs.gui_show_horizontal_highlight && !Prefs.gui_show_vertical_highlight)
						|| holdMouseHighlight)
					return;

				Point p = e.getPoint();

				// (offset/charW) gives char number of current display
				// then + to starting char to get actual char
				int nuc = (((p.x - pX) / charW) + (pX / charW)) + 1;
				int seq = p.y / charH;

				if (!holdMouseHighlight)
				{
					if (seq >= 0 && seq < nSeq && nuc > 0 && nuc <= nNuc)
						highlight(seq, nuc, true);
					else
						highlight(-1, -1, true);
				}

			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				super.mouseDragged(e);

				if (rightMouseCoord != null)
				{
					Point p = e.getPoint();
					setCursor(new Cursor(Cursor.HAND_CURSOR));
					int dX = (int) (rightMouseCoord.getX() - p.getX());
					int dY = (int) (rightMouseCoord.getY() - p.getY());
					int newX = sp.getHorizontalScrollBar().getValue() + dX;
					int newY = sp.getVerticalScrollBar().getValue() + dY;

					sp.getHorizontalScrollBar().setValue(newX);
					sp.getVerticalScrollBar().setValue(newY);

					popup.setEnabled(false);
					return;
				}

				if (mouseStartSeq >= 0 && mouseStartSeq < nSeq && mouseStartNuc >= 0 && mouseStartNuc <= nNuc)
				{
					int nuc = (((e.getX() - pX) / charW) + (pX / charW)) + 1;
					int seq = e.getY() / charH;
					nuc = (nuc < 1) ? 1 : nuc;
					seq = (seq < 0) ? 0 : seq;

					int nucRange = (nuc - mouseStartNuc);
					int nucStart = (nucRange < 0) ? nuc : mouseStartNuc;
					nucRange = (nucRange < 0) ? -nucRange : nucRange;

					int seqRange = (seq - mouseStartSeq);
					int seqStart = (seqRange < 0) ? seq : mouseStartSeq;
					seqRange = (seqRange < 0) ? -seqRange : seqRange;

					if ((seqStart + seqRange >= ss.getSize()))
						seqRange = ss.getSize() - seqStart - 1;
					if ((nucStart + nucRange) > ss.getLength())
						nucRange = ss.getLength() - nucStart;

					highlight(seqStart, seqRange, nucStart, nucRange, true);
					scroll(e.getPoint());
				}

				holdMouseHighlight = true;
			}

		}

		// Updated each time new data has been loaded (or changes have been made
		// to the sizes of fonts by the user)
		void setData()
		{
			buffer = ss.getSequence(0).getBuffer();

			// Get a FontMetrics object so font dimensions can be computed
			fm = new java.awt.image.BufferedImage(1, 1,
					java.awt.image.BufferedImage.TYPE_INT_RGB).getGraphics()
					.getFontMetrics(font);

			charW = fm.charWidth('G');
			charH = fm.getHeight();
			charDec = fm.getMaxDescent();

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
			pX = p.x;
			pY = p.y;

			if (annotationsPanel != null)
			{
				// annotationsPanel.setSizes(canW, charW, seqList.getWidth());
				annotationsPanel.setSizes(canW, seqList.getWidth());
				// not used (empty method)
				// annotationsPanel.setScrollBarValue(hBar.getValue());
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

			if (buffer == null)
				return;

			start = (pX / charW);
			int seqStart = (int) (pY / charH);

			g.setFont(font);

			int seqEnd = seqStart + charDepth + 2;

			for (int seq = seqStart, y = charH + (charH * seqStart); seq < seqEnd; seq++, y += charH)
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
				{
					for (int i = 0, x = pX; i < str.length; i++, x += charW)
					{
						if (Prefs.gui_seq_show_colors && drawDim == false)
						{
							g.setColor(getColor(str[i]));

							// Subset selection highlighting
							int cS = data.getActiveRegionS();
							int cE = data.getActiveRegionE();
							if ((start + i + 1) < cS || (start + i + 1) > cE)
								g.setColor(g.getColor().darker().darker());

						} else
							g.setColor(Color.WHITE);

						g.fillRect(x, y1, charW, charH);

						// Highlight
						boolean horz = false;
						boolean vert = false;
						Color highlight = new Color(Prefs.gui_seq_highlight
								.getRed(), Prefs.gui_seq_highlight.getGreen(),
								Prefs.gui_seq_highlight.getBlue(), 75);

						if (seq >= mouseHighlight.y
								&& seq <= mouseHighlight.y
										+ mouseHighlight.height
								&& Prefs.gui_show_horizontal_highlight)
						{
							g.setColor(highlight);
							horz = true;
						}
						if (i + start + 1 >= mouseHighlight.x
								&& i + start + 1 <= mouseHighlight.x
										+ mouseHighlight.width
								&& Prefs.gui_show_vertical_highlight)
						{
							g.setColor(highlight);
							vert = true;
						}
						if (horz && vert)
							g.setColor(new Color(highlight.getRed(), highlight
									.getGreen(), highlight.getBlue(), 150));

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

		public void run()
		{
			while (true)
			{
				while (running)
				{
					int value;
					if (up > 0)
					{
						value = sp.getVerticalScrollBar().getValue();
						if (value <= up)
							this.running = false;
						sp.getVerticalScrollBar().setValue(value - up);
					}
					if (right > 0)
					{
						value = sp.getHorizontalScrollBar().getValue();
						if (value > (canvas.getWidth() - right))
							this.running = false;
						sp.getHorizontalScrollBar().setValue(value + right);
					}
					if (down > 0)
					{
						value = sp.getVerticalScrollBar().getValue();
						if (value > (canvas.getHeight() - down))
							this.running = false;
						sp.getVerticalScrollBar().setValue(value + down);
					}
					if (left > 0)
					{
						value = sp.getHorizontalScrollBar().getValue();
						if (value <= left)
							this.running = false;
						sp.getHorizontalScrollBar().setValue(value - left);
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