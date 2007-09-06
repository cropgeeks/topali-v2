// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import static topali.gui.WinMainMenuBar.*;
import static topali.mod.Filters.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.apache.log4j.Logger;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;

import topali.data.*;
import topali.gui.*;
import topali.mod.Filters;
import doe.MsgBox;

/**
 * Panel for displaying a graph
 */
public class GraphPanel extends JPanel implements Printable
{
	 Logger log = Logger.getLogger(this.getClass());
	
	public static final int NO = 0;
	public static final int TOP = 1;
	public static final int LEFT = 2;
	public static final int BOTTOM = 3;
	public static final int RIGHT = 4;
	
	AlignmentData aData;

	AlignmentResult aResult;

	double[][] data;

	double fixedYBound = -1;
	double threshold = 0f;
	
	JFreeChart chart;
	ChartPanel chartPanel;
	JToolBar toolbar;
	
	/**
	 * Panel for displaying a graph
	 * @param aData	
	 * @param aResult
	 * @param data
	 * @param fixedYBound Use a fixed max y
	 * @param toolbarPos Position where the toolbar should be placed
	 */
	public GraphPanel(AlignmentData aData, AlignmentResult aResult,
			double[][] data, double fixedYBound, int toolbarPos)
	{
		this.aData = aData;
		this.aResult = aResult;
		this.data = data;
		this.fixedYBound = fixedYBound;
		this.threshold = aResult.threshold;
		
		this.chart = createChart();
		setChartData(data);
		this.chartPanel = new ChartPanel(this.chart);
		this.chartPanel.addMouseListener(new ChartPanelPopupMenuAdapter());
		
		this.setLayout(new BorderLayout());
		this.add(chartPanel, BorderLayout.CENTER);
		
		switch(toolbarPos) {
		case TOP: this.toolbar = createToolbar(toolbarPos); this.add(toolbar, BorderLayout.NORTH); break;
		case RIGHT: this.toolbar = createToolbar(toolbarPos); this.add(toolbar, BorderLayout.EAST); break;
		case BOTTOM: this.toolbar = createToolbar(toolbarPos); this.add(toolbar, BorderLayout.SOUTH); break;
		case LEFT: this.toolbar = createToolbar(toolbarPos); this.add(toolbar, BorderLayout.WEST); break;
		}
	}
	
	public JToolBar createToolbar(int pos) {
		int p = (pos==LEFT||pos==RIGHT) ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL;
		JToolBar tb = new JToolBar(p);
		
		tb.setFloatable(false);
		tb.setBorderPainted(false);
		tb.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		
		JButton bExport;
		AbstractAction aExport;
		
		aExport = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				actionSaveGraph();
			}
		};

		bExport = (JButton) WinMainToolBar.getButton(false, null, "dss01",
				Icons.EXPORT, aExport);


		tb.add(bExport);
		
		return tb;
	}
	
	public void setChartData(double[][] data)
	{
		this.data = data;

		XYSeries series = new XYSeries("");

		for (int i = 0; i < data.length; i++)
			series.add(data[i][0], data[i][1]);

		XYSeriesCollection coll = new XYSeriesCollection(series);
		chart.getXYPlot().setDataset(coll);

		adjustUpperYBound();
		
		repaint();
	}

	public void setThreshold(double thres) {
		this.threshold = thres;
		
		XYPlot plot = chart.getXYPlot();
		plot.clearRangeMarkers();

		float[] dashPattern =
		{ 5, 5 };
		BasicStroke s1 = new BasicStroke(1, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 10, dashPattern, 0);
		ValueMarker marker = new ValueMarker(thres, new Color(0, 0, 255, 64),
				s1, null, null, 0.1f);
		plot.addRangeMarker(marker);
		
		adjustUpperYBound();
		
		repaint();
	}

	public int getNucleotideFromPoint(int xPos)
	{
		if(this.chartPanel==null)
			return -1;
		else
			return this.chartPanel.getNucleotideFromPoint(xPos);
	}
	
	@Override
	public Dimension getPreferredSize()
	{
		return chartPanel.getPreferredSize();
	}

	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if(!enabled)
			this.remove(chartPanel);
		else
			this.add(chartPanel, BorderLayout.CENTER);
		
		validate();
		//chartPanel.setEnabled(enabled);
	}

	private JFreeChart createChart()
	{
		JFreeChart chart = ChartFactory.createXYLineChart(null, null, // xaxis title
				null, // yaxis title
				null, PlotOrientation.VERTICAL, true, true, false);

		//setChartData(this.data);

		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
		chart.setRenderingHints(rh);
		chart.removeLegend();

		XYPlot plot = chart.getXYPlot();

		// plot.setDomainGridlinesVisible(false);
		// plot.setRangeGridlinesVisible(false);

		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		// xAxis.setLowerBound(1);
		// xAxis.setUpperBound(data[data.length-1]);
		xAxis.setTickLabelFont(new JLabel().getFont());
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setLowerBound(0);

		yAxis.setTickLabelFont(new JLabel().getFont());

		// Set the height of the graph to show 5% above the maximum value
		//adjustUpperYBound();

		// And set the width of the graph to fit the data exactly
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(aData.getSequenceSet().getLength());
		
		return chart;
	}

	/**
	 * Searches both the data and the thresholds to find the maximum Y value
	 */
	private void adjustUpperYBound()
	{
		double max = (float) fixedYBound;

		// if there's no fixed bound, look for the highest value
		if (max < 0)
		{
			for (int i = 0; i < data.length; i++)
				if (data[i][1] > max)
					max = data[i][1];

//			if (aResult.threshold > max)
//				max = aResult.threshold;
			
			if(threshold > max)
				max = threshold;
			
			max *= 1.05;
		}
		XYPlot plot = chart.getXYPlot();
		ValueAxis yAxis = plot.getRangeAxis();
		yAxis.setUpperBound(max);
	}

	private void saveCSV(File filename) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));

		out.write("Nucleotide, Score");
		out.newLine();

		for (int i = 0; i < data.length; i++)
		{
			out.write(data[i][0] + "," + data[i][1]);
			out.newLine();
		}

		out.close();
	}

	private void savePNG(File filename) throws IOException
	{
		BufferedImage bi = new BufferedImage(chartPanel.getSize().width, chartPanel.getSize().height, BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g2d = bi.createGraphics();
		chartPanel.paint(g2d);
		updateUI();
		
		ImageIO.write(bi, "png", filename);
	}
	
	// ----------------
	// Actions
	
	protected void actionReselectSequences()
	{
		String msg = "This will reselect the sequences used at the time of this "
				+ "analysis in the main alignment view window. Continue?";

		if (MsgBox.yesno(msg, 0) == JOptionPane.YES_OPTION)
			TOPALi.winMain.menuAnlsReselectSequences(aResult.selectedSeqs);
	}
	
	protected void actionAddSelectedRegion(Class type)
	{
		WinMain.rDialog.addCurrentRegion(type);
		WinMainMenuBar.aFileSave.setEnabled(true);
		WinMainMenuBar.aVamCommit.setEnabled(true);
	}
	
	protected void actionShowToolTipDialog()
	{
		TreeToolTipDialog dialog = new TreeToolTipDialog(
				aResult.useTreeToolTips, aResult.treeToolTipWindow, aData.getSequenceSet().getLength());

		aResult.useTreeToolTips = dialog.isOptionChecked();
		aResult.treeToolTipWindow = dialog.getWindowSize();

		WinMainMenuBar.aFileSave.setEnabled(true);
		WinMainMenuBar.aVamCommit.setEnabled(true);
	}
	
	protected void actionSaveGraph()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Export Graphs");
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		fc.setSelectedFile(new File(aResult.guiName));

		Filters.setFilters(fc, Prefs.gui_filter_graph, CSV, PNG);
		fc.setAcceptAllFileFilterUsed(false);

		while (fc.showSaveDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
		{
			File file = Filters.getSelectedFileForSaving(fc);

			// Confirm overwrite
			if (file.exists())
			{
				String msg = file
						+ " already exists.\nDo you want to replace it?";
				int response = MsgBox.yesnocan(msg, 1);

				if (response == JOptionPane.NO_OPTION)
					continue;
				else if (response == JOptionPane.CANCEL_OPTION
						|| response == JOptionPane.CLOSED_OPTION)
					return;
			}

			// Otherwise it's ok to save...
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			Prefs.gui_filter_graph = ((Filters) fc.getFileFilter()).getExtInt();

			try
			{
				if (Prefs.gui_filter_graph == CSV)
					saveCSV(file);
				else if (Prefs.gui_filter_graph == PNG)
					savePNG(file);
				
				MsgBox.msg("Graph data successfully saved to " + file,
						MsgBox.INF);
			} catch (Exception e)
			{
				MsgBox.msg(
						"There was an unexpected error while saving graph data:\n "
								+ e, MsgBox.ERR);
				log.warn(e);
			}

			return;
		}
	}
	
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException
	{
		return chartPanel.print(graphics, pageFormat, pageIndex);
	}

	private static Cursor CROSS = new Cursor(Cursor.CROSSHAIR_CURSOR);
	private static Cursor DEFAULT = new Cursor(Cursor.DEFAULT_CURSOR);
	
	class ChartPanel extends org.jfree.chart.ChartPanel implements
			MouseListener, MouseMotionListener
	{
		
		// Tracks graph parts needed for calculations
		private XYPlot plot;

		private ValueAxis xAxis, yAxis;

		// Mouse position (when clicked, and when dragging)
		private int clickX, dragX;

		TreeToolTip tooltip;

		ChartPanel(JFreeChart chart)
		{
			super(chart, true);

			tooltip = new TreeToolTip(aData, aResult.selectedSeqs);

			addMouseListener(this);
			addMouseMotionListener(this);

			plot = (XYPlot) chart.getPlot();
			xAxis = plot.getDomainAxis();
			yAxis = plot.getRangeAxis();

			// Disables font scaling on the axis labels.
			// Why? Ours is not to question why...
			setMinimumDrawWidth(100);
			setMaximumDrawWidth(10000);
			setMinimumDrawHeight(100);
			setMaximumDrawHeight(10000);

			// this.setDisplayToolTips(true);
		}

		@Override
		public JToolTip createToolTip()
		{
			return tooltip;
		}

		@Override
		public String getToolTipText(MouseEvent e)
		{
			int x = e.getX(), y = e.getY();

			if (aResult.useTreeToolTips && getCanvasArea().contains(x, y))
			{
				double[] d = getValuesFromPoint(x, y);

				int winS = (int) d[0] - aResult.treeToolTipWindow / 2;
				if (winS <= 0)
					winS = 1;
				int winE = winS + aResult.treeToolTipWindow;
				if (winE > aData.getSequenceSet().getLength())
					winE = aData.getSequenceSet().getLength();

				tooltip.createNewTree(winS, winE);

				return "" + x;
			}

			return null;
		}

		private void setStatusBarText(int x, int y)
		{
			double[] d = getValuesFromPoint(x, y);

			String msg = "Nucleotide: " + ((int) d[0]) + " (value: "
					+ Prefs.d5.format(d[1]) + ")";
			WinMainStatusBar.setText(msg);
		}

		@Override
		public void mouseMoved(MouseEvent e)
		{
			int x = e.getX(), y = e.getY();

			if (getCanvasArea().contains(x, y))
			{
				setCursor(CROSS);
				setStatusBarText(x, y);
			} else
			{
				WinMainStatusBar.setText("");
				setCursor(DEFAULT);
			}
			
			//forward event to parent component
			MouseEvent e2 = SwingUtilities.convertMouseEvent(this, e, this.getParent());
			this.getParent().dispatchEvent(e2);
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			if (e.isMetaDown() == false && !e.isPopupTrigger())
			{
				int x = e.getX(), y = e.getY();
				dragX = x;
				calculatePartition();
				setStatusBarText(x, y);
			}
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			// Do nothing if the click is outside of the canvas area
			if (getCanvasArea().contains(e.getX(), e.getY()) == false)
				return;

			if (e.isMetaDown() == false && !e.isPopupTrigger())
			{
				clickX = dragX = e.getX();
				calculatePartition();
			}
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (e.isMetaDown() == false && !e.isPopupTrigger())
			{
				aData.setActiveRegion(-1, -1);
				WinMain.repaintDisplay();
			}
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
			WinMainStatusBar.setText("");
		}

		// Determines where the partition should be based on the current X
		// position of the mouse. Takes into account the fact that the user can
		// drag to the left or the right of the starting point (mouseX)
		private void calculatePartition()
		{
			int nS = 0, nE = 0;

			if (clickX <= dragX)
			{
				nS = getNucleotideFromPoint(clickX);
				nE = getNucleotideFromPoint(dragX);
			} else
			{
				nS = getNucleotideFromPoint(dragX);
				nE = getNucleotideFromPoint(clickX);
			}

			// Verify that the partition is not out of bounds
			if (nS < 1)
				nS = 1;
			if (nE > aData.getSequenceSet().getLength())
				nE = aData.getSequenceSet().getLength();

			aData.setActiveRegion(nS, nE);

			WinMain.repaintDisplay();
		}

		private Color highlight = new Color(50, 50, 50, 50); // last value is

		// alpha

		@Override
		public void paintComponent(Graphics graphics)
		{
			/*
			 * int nS = pAnnotations.getCurrentStart(); int nE =
			 * pAnnotations.getCurrentEnd();
			 * 
			 * IntervalMarker iMarker = new IntervalMarker(nS, nE);
			 * iMarker.setPaint(new Color(200, 200, 200));
			 * 
			 * plot.clearDomainMarkers(); plot.addDomainMarker(iMarker,
			 * Layer.BACKGROUND);
			 */
			super.paintComponent(graphics);

			Graphics2D g = (Graphics2D) graphics;

			// g.setColor(new Color(50, 50, 50));
			g.setPaint(highlight);
			// g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,50f/255f));

			// if (isPartioned)
			{
				Rectangle r = getCanvasArea();

				int nS = aData.getActiveRegionS();
				int nE = aData.getActiveRegionE();
				int pStart = getPointFromNucleotide(nS - 1) - r.x;
				int pEnd = getPointFromNucleotide(nE + 1);

				// Left of partition
				g.fillRect(r.x, r.y, pStart, r.height);

				// Right of partition
				int width = r.width + r.x - pEnd;
				g.fillRect(pEnd, r.y, width, r.height);
			}
		}

		private Rectangle2D getDataArea()
		{
			return getChartRenderingInfo().getPlotInfo().getDataArea();
		}

		// Converts the 2D double-precision data area into an integer canvas
		// area
		private Rectangle getCanvasArea()
		{
			Rectangle2D r = getDataArea();

			return new Rectangle((int) r.getX(), (int) r.getY(), (int) r
					.getWidth(), (int) r.getHeight());
		}

		// Converts a 2D (xAxis) position into a nucleotide position
		int getNucleotideFromPoint(int xPos)
		{
			return (int) getValuesFromPoint(xPos, 0)[0];
		}

		private double[] getValuesFromPoint(int xPos, int yPos)
		{
			// The following translation takes account of the fact that the
			// chart
			// image may have been scaled up or down to fit the panel
			Point2D p = translateScreenToJava2D(new Point(xPos, yPos));
			Rectangle2D dArea = getDataArea();

			// Now convert the Java2D coordinate to axis coordinates
			double x = xAxis.java2DToValue(p.getX(), dArea, plot
					.getDomainAxisEdge());
			double y = yAxis.java2DToValue(p.getY(), dArea, plot
					.getRangeAxisEdge());

			double[] result =
			{ x, y };
			return result;
			// return (int) x;
		}

		// Converts a nucleotide position into a 2D (xAxis) screen position
		private int getPointFromNucleotide(int nucleotide)
		{
			Rectangle2D dArea = getDataArea();

			double x = xAxis.valueToJava2D(nucleotide, dArea, plot
					.getDomainAxisEdge());
			// double y = yAxis.valueToJava2D(yy, dArea,
			// plot.getRangeAxisEdge());

			return (int) translateJava2DToScreen(new Point2D.Double(x, 0))
					.getX();
		}
		
	}
	
	class ChartPanelPopupMenuAdapter extends PopupMenuAdapter
	{

		JMenu annotate;

		ChartPanelPopupMenuAdapter()
		{
			JMenuItem addPart = new JMenuItem();
			addPart.setAction(new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					WinMain.rDialog.addRegion(aData.getActiveRegionS(), aData
							.getActiveRegionE(), PartitionAnnotations.class);
				}
			});
			addPart.setText(Text.Gui.getString("aAlgnAddPartition"));
			addPart.setMnemonic(KeyEvent.VK_P);

			JMenuItem addCodReg = new JMenuItem();
			addCodReg.setAction(new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					WinMain.rDialog.addRegion(aData.getActiveRegionS(), aData
							.getActiveRegionE(), CDSAnnotations.class);
				}
			});
			addCodReg.setText(Text.Gui.getString("aAlgnAddCDS"));
			addCodReg.setMnemonic(KeyEvent.VK_C);

			annotate = new JMenu(Text.Gui.getString("menuAlgnAnnotate"));
			annotate.setIcon(Icons.ADD_PARTITION);
			annotate.add(addPart);
			annotate.add(addCodReg);
			p.add(annotate);

			add(aAnlsPartition, Icons.AUTO_PARTITION, KeyEvent.VK_P, 0, 0, 0,
					false);
			add(aAnlsCreateTree, Icons.CREATE_TREE, KeyEvent.VK_T,
					KeyEvent.VK_T, InputEvent.CTRL_MASK, 9, true);
		}

		@Override
		protected void handlePopup(int x, int y)
		{
			
		}
	}
}
