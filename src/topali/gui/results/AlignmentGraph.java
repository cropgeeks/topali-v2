// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import topali.data.AlignmentData;
import topali.data.AlignmentResult;
import topali.gui.*;

public class AlignmentGraph extends JPanel implements Printable
{
	public static int TYPE_LINECHART = 0;

	public static int TYPE_HISTOGRAMM = 1;

	private int type;

	// Reference back to the AlignmentData object
	private AlignmentData data;

	private AlignmentResult aResult;

	private int alignmentLength;

	// The data being plotted
	private float[][] graphData;

	private float threshold;
	
	private JFreeChart chart;

	private GraphPanel graph;

	public AlignmentGraph(AlignmentData data, AlignmentResult aResult,
			float[][] graphData, float threshold, int type)
	{
		this.type = type;
		this.data = data;
		this.threshold = threshold;
		this.aResult = aResult;
		this.graphData = graphData;

		alignmentLength = data.getSequenceSet().getLength();

		initializeChart();

		setLayout(new BorderLayout());
		add(graph);
	}

	// Sets the threshold to be displayed at the given percentage level
	public void setThresholdValue(float value)
	{
		this.threshold = value;
		XYPlot plot = chart.getXYPlot();
		plot.clearRangeMarkers();

		float[] dashPattern =
		{ 5, 5 };
		BasicStroke s1 = new BasicStroke(1, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 10, dashPattern, 0);
		ValueMarker marker = new ValueMarker(value, new Color(0, 0, 255, 64),
				s1, null, null, 0.1f);

		plot.addRangeMarker(marker);
	}

	void doSaveAs()
	{
		try
		{
			graph.doSaveAs();
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	private void initializeChart()
	{
		if (type == TYPE_LINECHART)
			chart = ChartFactory.createXYLineChart(null, null, // xaxis title
					null, // yaxis title
					null, PlotOrientation.VERTICAL, true, true, false);
		else if (type == TYPE_HISTOGRAMM)
			chart = ChartFactory.createHistogram(null, null, null, null,
					PlotOrientation.VERTICAL, false, false, false);

		setChartData(this.graphData);

		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
		chart.setRenderingHints(rh);
		chart.removeLegend();

		XYPlot plot = chart.getXYPlot();

		ValueAxis xAxis = (ValueAxis) plot.getDomainAxis();
		xAxis.setTickLabelFont(new JLabel().getFont());
		ValueAxis yAxis = (ValueAxis) plot.getRangeAxis();
		yAxis.setLowerBound(0);

		yAxis.setTickLabelFont(new JLabel().getFont());

//		 Set the height of the graph to show 5% above the maximum value
		adjustUpperYBound();
		
		// And set the width of the graph to fit the data exactly
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(alignmentLength);

		graph = new GraphPanel();
	}

	public void setChartData(float[][] data)
	{
		this.graphData = data;
		
		XYSeries series = new XYSeries("");

		for (int i = 0; i < data.length; i++)
			series.add(data[i][0], data[i][1]);

		XYSeriesCollection coll = new XYSeriesCollection(series);
		chart.getXYPlot().setDataset(coll);
		
		adjustUpperYBound();
	}

	// Searches both the data and the thresholds to find the maximum Y value
	private void adjustUpperYBound()
	{
		float max = 0;

		for (int i = 0; i < graphData.length; i++)
			if (graphData[i][1] > max)
				max = graphData[i][1];

		if(threshold>max)
			max = threshold;
		
		XYPlot plot = chart.getXYPlot();
		ValueAxis yAxis = (ValueAxis) plot.getRangeAxis();
		yAxis.setUpperBound(max * 1.05);
	}

	public GraphPanel getGraphPanel()
	{
		return graph;
	}

	public int print(Graphics graphics, PageFormat pf, int pageIndex)
	{
		return graph.print(graphics, pf, pageIndex);
	}

	// Creates a BufferedImage and draws the map onto it
	BufferedImage getSavableImage()
	{
		BufferedImage image = new BufferedImage(800, 300,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();

		g.setColor(Color.white);
		g.fillRect(0, 0, 800, 300);
		graph.paintComponent(g);
		g.dispose();

		return image;
	}

	private static Cursor CROSS = new Cursor(Cursor.CROSSHAIR_CURSOR);

	private static Cursor DEFAULT = new Cursor(Cursor.DEFAULT_CURSOR);

	class GraphPanel extends ChartPanel implements MouseListener,
			MouseMotionListener
	{
		// Tracks graph parts needed for calculations
		private XYPlot plot;

		private ValueAxis xAxis, yAxis;

		// Mouse position (when clicked, and when dragging)
		private int clickX, dragX;

		TreeToolTip tooltip;

		GraphPanel()
		{
			super(chart, true);

			tooltip = new TreeToolTip(data, aResult.selectedSeqs);

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

		public JToolTip createToolTip()
		{
			return tooltip;
		}

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
				if (winE > alignmentLength)
					winE = alignmentLength;

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
		}

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

		public void mouseClicked(MouseEvent e)
		{
			if (e.isMetaDown() == false && !e.isPopupTrigger())
			{
				data.setActiveRegion(-1, -1);
				WinMain.repaintDisplay();
			}
		}

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
			if (nE > alignmentLength)
				nE = alignmentLength;

			data.setActiveRegion(nS, nE);

			WinMain.repaintDisplay();
		}

		private Color highlight = new Color(50, 50, 50, 50); // last value is

		// alpha

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

				int nS = data.getActiveRegionS();
				int nE = data.getActiveRegionE();
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
}