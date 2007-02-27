// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToolTip;

import org.jfree.chart.*;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import topali.data.*;
import topali.gui.*;

public class CodeMLGraphPanel extends JPanel
{

	JFreeChart chart;

	public ChartPanel chartPanel;

	AlignmentData data;

	AlignmentResult result;

	int alignmentLength;
	
	CMLModel model;
	
	public CodeMLGraphPanel(AlignmentData data, AlignmentResult aResult)
	{
		this.data = data;
		this.result = aResult;
		alignmentLength = data.getSequenceSet().getLength();

		chart = ChartFactory.createHistogram(null, null, null, null,
				PlotOrientation.VERTICAL, false, false, false);
		initChart();
		chartPanel = new GraphPanel();
		this.add(chartPanel);
	}

	private void initChart()
	{
		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
		chart.setRenderingHints(rh);
		chart.removeLegend();

		XYPlot plot = chart.getXYPlot();
		plot.setRenderer(new XYStepAreaRenderer());
		plot.getRenderer().setSeriesPaint(0, Color.BLUE);
		
		ValueAxis x = plot.getDomainAxis();
		x.setTickLabelsVisible(false);
		x.setLowerBound(0);
		x.setUpperBound(alignmentLength);
		
		ValueAxis y = plot.getRangeAxis();
		y.setUpperBound(1.03d);
		y.setLowerBound(0.0d);
	}

	public void setModel(CMLModel model)
	{
		// set data
		this.model = model;
		XYSeriesCollection dataset = getDataSet(model);
		XYPlot plot = chart.getXYPlot();
		plot.setDataset(dataset);	
		repaint();
	}
	
	private void annotate(float threshold) {
		if(!model.isSupportsPSS())
			return;
		
		//draw thresholds line
		XYPlot plot = chart.getXYPlot();
		plot.clearRangeMarkers();
		float[] dashPattern =
		{ 5, 5 };
		BasicStroke s1 = new BasicStroke(1, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 10, dashPattern, 0);
		ValueMarker marker = new ValueMarker(threshold, new Color(0, 0, 255, 64),
				s1, null, null, 0.1f);
		plot.addRangeMarker(marker);
		
		// draw annoations
		for(Object o : plot.getAnnotations()) {
			if(o instanceof XYTextAnnotation) {
				plot.removeAnnotation((XYTextAnnotation)o);
			}
		}
		for(PSSite pss : model.getPSS(threshold)) {
			int aa = pss.getPos();
			int n = aa*3-1;
			double y = pss.getP();
			plot.addAnnotation(new XYTextAnnotation(""+aa+pss.getAa(), n, y+0.02));
		}
		
		repaint();
	}
	
	public void setThreshold(float threshold) {
		annotate(threshold);
	}
	
	private XYSeriesCollection getDataSet(CMLModel m)
	{
		if(!m.isSupportsPSS())
			return null;
		
		XYSeriesCollection dataset = new XYSeriesCollection();

		XYSeries ser = new XYSeries("");
		List<PSSite> pss = m.getPSS(-1f);
		for (int i = 0; i < pss.size(); i++)
		{
			PSSite p = pss.get(i);
			ser.add(p.getPos() * 3 - 1, p.getP());
		}
		dataset.addSeries(ser);

		return dataset;
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

			tooltip = new TreeToolTip(data, result.selectedSeqs);

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

			if (result.useTreeToolTips && getCanvasArea().contains(x, y))
			{
				double[] d = getValuesFromPoint(x, y);

				int winS = (int) d[0] - result.treeToolTipWindow / 2;
				if (winS <= 0)
					winS = 1;
				int winE = winS + result.treeToolTipWindow;
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
