// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.print.*;
import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.data.xy.*;
import org.jfree.chart.plot.*;

import topali.data.*;
import topali.gui.*;

public class AlignmentGraph extends JPanel implements Printable
{
	// Reference back to the AlignmentData object
	private AlignmentData data;
	private AlignmentResult aResult;
	private int alignmentLength;
	
	// The data being plotted
	private float[][] graphData;
	private float[] thresholds;
	
	private JFreeChart chart;
	private GraphPanel graph;
	
	public AlignmentGraph(AlignmentData data, AlignmentResult aResult, float[][] graphData, float[] thresholds)
	{
		this.data = data;
		this.aResult = aResult;
		this.graphData = graphData;
		this.thresholds = thresholds;
		
		alignmentLength = data.getSequenceSet().getLength();
		
		initializeChart();

		setLayout(new BorderLayout());
		add(graph);
	}
	
	// Sets the threshold to be displayed at the given percentage level
	public void setThresholdValue(float value)
	{
		XYPlot plot = chart.getXYPlot();
		plot.clearRangeMarkers();
		
		float[] dashPattern = { 5, 5 };
		BasicStroke s1 = new BasicStroke(1, BasicStroke.CAP_BUTT,
    		BasicStroke.JOIN_MITER, 10, dashPattern, 0);
		ValueMarker marker = new ValueMarker(
			value, new Color(0, 0, 255, 64), s1, null, null, 0.1f);
        
        plot.addRangeMarker(marker);
	}
	
	void doSaveAs()
	{
		try
		{
			graph.doSaveAs();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}
	
	private void initializeChart()
	{
		chart = ChartFactory.createXYLineChart(
			null,
			null, // xaxis title
			null, // yaxis title
			null,
			PlotOrientation.VERTICAL,
			true,
			true,
			false
		);
		
		setChartData();
				
		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_OFF);
		chart.setRenderingHints(rh);
		chart.removeLegend();

		XYPlot plot = chart.getXYPlot();
		
//		plot.setDomainGridlinesVisible(false);
//		plot.setRangeGridlinesVisible(false);
		
        		
		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
//		xAxis.setLowerBound(1);
//		xAxis.setUpperBound(data[data.length-1]);
		xAxis.setTickLabelFont(new JLabel().getFont());
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setLowerBound(0);
		
		yAxis.setTickLabelFont(new JLabel().getFont());
		
		// Set the height of the graph to show 5% above the maximum value
		float maxY = findMax();
		yAxis.setUpperBound(maxY * 1.05);
		
		// And set the width of the graph to fit the data exactly
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(alignmentLength);
		
		graph = new GraphPanel();
	}
	
	public void setHMMUpperLowerLimits()
	{
		XYPlot plot = chart.getXYPlot();
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		
		yAxis.setLowerBound(-0.01);
		yAxis.setUpperBound(+1.01);
	}
	
	private void setChartData()
	{
		XYSeries series = new XYSeries("");
		
		for (int i = 0; i < graphData.length; i++)
			series.add(graphData[i][0], graphData[i][1]);
		
		XYSeriesCollection coll = new XYSeriesCollection(series);		
		chart.getXYPlot().setDataset(coll);
	}
	
	// Searches both the data and the thresholds to find the maximum Y value
	private float findMax()
	{
		float max = 0;
		
		for (int i = 0; i < graphData.length; i++)
			if (graphData[i][1] > max)
				max = graphData[i][1];
		
		if (thresholds[thresholds.length-1] > max)
			max = thresholds[thresholds.length-1];
		
		System.out.println("max is " + max);
		return max;
	}
	
	GraphPanel getGraphPanel()
		{ return graph; }
	
	public int print(Graphics graphics, PageFormat pf, int pageIndex)
	{
		return graph.print(graphics, pf, pageIndex);
	}
	
	// Creates a BufferedImage and draws the map onto it
	BufferedImage getSavableImage()
	{
		BufferedImage image = new BufferedImage(800, 300, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		
		g.setColor(Color.white);
		g.fillRect(0, 0, 800, 300);
		graph.paintComponent(g);
		g.dispose();
		
		return image;
	}
	
	private static Cursor CROSS = new Cursor(Cursor.CROSSHAIR_CURSOR);
	private static Cursor DEFAULT = new Cursor(Cursor.DEFAULT_CURSOR);

	class GraphPanel extends ChartPanel
		implements MouseListener, MouseMotionListener
	{
		// Tracks graph parts needed for calculations
		private XYPlot plot;
		private ValueAxis xAxis, yAxis;
		
		private PartitionAnnotations pAnnotations;
		// Mouse position (when clicked, and when dragging)
		private int clickX, dragX;
		
		TreeToolTip tooltip;
		
		GraphPanel()
		{
			super(chart);
			
			tooltip = new TreeToolTip(data, aResult.selectedSeqs);
			
			pAnnotations = data.getTopaliAnnotations().getPartitionAnnotations();
			
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
			
//			this.setDisplayToolTips(true);
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
				
				int winS = (int)d[0] - aResult.treeToolTipWindow/2;
				if (winS <= 0)
					winS = 1;
				int winE = winS + aResult.treeToolTipWindow;
				if (winE > alignmentLength)
					winE = alignmentLength;
				
				tooltip.createNewTree(winS, winE);
				
				return "" + x ;
			}
			
			return null;			
		}
		
		private void setStatusBarText(int x, int y)
		{
			double[] d = getValuesFromPoint(x, y);
				
			String msg = "Nucleotide: " + ((int)d[0]) + " (value: " + Prefs.d5.format(d[1]) + ")";
			WinMainStatusBar.setText(msg);
		}
		
		public void mouseMoved(MouseEvent e)
		{
			int x = e.getX(), y = e.getY();
			
			if (getCanvasArea().contains(x, y))
			{
				setCursor(CROSS);
				setStatusBarText(x, y);
			}
			else
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
				pAnnotations.resetCurrentPartition();		
				WinMain.repaintDisplay();
			}
		}
		
		public void mouseExited(MouseEvent e)
			{ WinMainStatusBar.setText(""); }
		
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
			}
			else
			{
				nS = getNucleotideFromPoint(dragX);
				nE = getNucleotideFromPoint(clickX);
			}
			
			// Verify that the partition is not out of bounds
			if (nS < 1)
				nS = 1;
			if (nE > alignmentLength)
				nE = alignmentLength;
				
			pAnnotations.setCurrentPartition(nS, nE);			
			WinMain.repaintDisplay();
		}
		
		private Color highlight = new Color(50, 50, 50, 50); // last value is alpha
		
		public void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			
			Graphics2D g = (Graphics2D) graphics;		
			
			
//			g.setColor(new Color(50, 50, 50));	
			g.setPaint(highlight);
//			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,50f/255f));
			
	//		if (isPartioned)
			{
				Rectangle r = getCanvasArea();

				int nS = pAnnotations.getCurrentStart();
				int nE = pAnnotations.getCurrentEnd();

				int pStart = getPointFromNucleotide(nS-1) - r.x;
				int pEnd   = getPointFromNucleotide(nE+1);
				
				// Left of partition
				g.fillRect(r.x, r.y, pStart, r.height);
				
				// Right of partition
				int width = r.width + r.x - pEnd;
				g.fillRect(pEnd, r.y, width, r.height);
			}
			
	/*		if (xpos != -1)
			{
				Shape oldClip = g.getClip();
				g.clipRect(xpos-20, (int)rec.getY(), 40, (int)rec.getHeight());
				g.fillRect(xpos-20, (int)rec.getY(), 40, (int)rec.getHeight());
				g.setClip(oldClip);
			}
	*/
		}
	
		private Rectangle2D getDataArea()
			{ return getChartRenderingInfo().getPlotInfo().getDataArea(); }
		
		// Converts the 2D double-precision data area into an integer canvas area
		private Rectangle getCanvasArea()
		{
			Rectangle2D r = getDataArea();
			
			return new Rectangle(
				(int)r.getX(),
				(int)r.getY(),
				(int)r.getWidth(),
				(int)r.getHeight()
			);
		}
		
		// Converts a 2D (xAxis) position into a nucleotide position
		int getNucleotideFromPoint(int xPos)
			{ return (int) getValuesFromPoint(xPos, 0)[0]; }
		
		private double[] getValuesFromPoint(int xPos, int yPos)
		{
			// The following translation takes account of the fact that the chart
			// image may have been scaled up or down to fit the panel
			Point2D p = translateScreenToJava2D(new Point(xPos, yPos));		
			Rectangle2D dArea = getDataArea();
			
			// Now convert the Java2D coordinate to axis coordinates
			double x =
				xAxis.java2DToValue(p.getX(), dArea, plot.getDomainAxisEdge());		
			double y =
				yAxis.java2DToValue(p.getY(), dArea, plot.getRangeAxisEdge());

			double[] result = { x, y };
			return result;
//			return (int) x;
		}
		
		// Converts a nucleotide position into a 2D (xAxis) screen position
		private int getPointFromNucleotide(int nucleotide)
		{
			Rectangle2D dArea = getDataArea();
			
			double x = xAxis.valueToJava2D(nucleotide, dArea, plot.getDomainAxisEdge());
//			double y = yAxis.valueToJava2D(yy, dArea, plot.getRangeAxisEdge());
	
			return (int) translateJava2DToScreen(new Point2D.Double(x, 0)).getX();
		}
	}
}