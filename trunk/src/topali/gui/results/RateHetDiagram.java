// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import pal.statistics.GammaDistribution;

import topali.data.models.*;

public class RateHetDiagram extends JPanel
{

	Logger log = Logger.getLogger(this.getClass());
	
	final int basicWidth = 200;
	final int basicHeight = 100;
	
	final int maxX = 3;
	final int maxY = 2;
	
	final Color cGamma = new Color(0,0,200);
	final Color cInv = new Color(200,0,0);
	
	Model model;
	
	public RateHetDiagram() {
		
	}
	
	public RateHetDiagram(Model model) {
		this.model = model;
	}
	
	public void setModel(Model model) {
		this.model = model;
	}
	
	
	public void paint(Graphics g)
	{
		super.paint(g);
		if(model==null)
			return;
		
		try
		{
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
			
			//total w, h
			int w = getWidth();
			int h = getHeight();
			
			//draw pInv
			if(model.isInv()) {
				g2d.setColor(cInv);
				int pInvH = (int)(h*model.getInvProp());
				int pInvY = h-pInvH;
				g2d.fillRect(0, pInvY, (int)(w/4d), pInvH);
			}
			
			//draw gamma dist.
			if(model.isGamma()) {
				double dX = (double)maxX/(double)w;
				double dY = (double)maxY/(double)h;
				double[] xValues = new double[w];
				int[] xValuesPixel = new int[w];
				double[] yValues = new double[w];
				int[] yValuesPixel = new int[w];
				xValues[0] = 0;
				xValuesPixel[0]=0;
				xValues[1] = dX;
				xValuesPixel[1] = 1;
				for(int i=2; i<w; i++) {
					xValues[i] = xValues[i-1]+dX;
					xValuesPixel[i] = i;
				}
				yValues = calcGamma(xValues);
				
				for(int i=0; i<yValues.length; i++) {
					yValuesPixel[i] = h-(int)(yValues[i]/dY);
				}
				
				g2d.setColor(cGamma);
				g2d.setStroke(new BasicStroke(2));
				g2d.drawPolyline(xValuesPixel, yValuesPixel, xValuesPixel.length);
			}
			else {
				int x = w/maxX;
				g2d.setColor(cGamma);
				g2d.setStroke(new BasicStroke(2));
				g2d.drawLine(x, 0, x, h);
			}
			
			g2d.setColor(Color.BLACK);
			g2d.setStroke(new BasicStroke(1));
			//draw axis
			g2d.drawLine(0, 0, 0, h);
			g2d.drawLine(0, h-1, w, h-1);
		} catch (RuntimeException e)
		{
			log.warn("Model: "+model+"\n", e);
		}
	}
	
	private double[] calcGamma(double[] x) {
		double[] y = new double[x.length];
		double alpha = model.getAlpha();
		double beta = 1/alpha;
		for(int i=0; i<x.length; i++) {
			y[i] = GammaDistribution.pdf(x[i], alpha, beta);
		}
		return y;
	}
	
	
	public Dimension getPreferredSize()
	{
		return getMinimumSize();
	}
	
	
	public Dimension getMinimumSize()
	{
		return new Dimension(basicWidth, basicHeight);
	}
	
	public static void main(String[] args) {
		DNAModel mod = (DNAModel)ModelManager.getInstance().generateModel("gtr", true, true);
		mod.setBaseFreqs(0.2,0.3,0.4,0.1);
		mod.setSubRates(1,2,3,4,5,6);
		mod.setAlpha(0.5);
		mod.setInvProp(0.25);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		RateHetDiagram d = new RateHetDiagram(mod);
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		p.add(d, BorderLayout.CENTER);
		frame.getContentPane().add(p);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
