// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.geom.AffineTransform;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.models.*;

public class ProteinModelDiagram extends ModelDiagram
{
	Logger log = Logger.getLogger(this.getClass());

	final String[] aa = new String[] {"A","R","N","D","C","Q","E","G","H","I","L","K","M","F","P","S","T","W","Y","V",};
	
	final int basicFontSize = 13;
	final int basicWidth = 100;
	final int basicHeight = 120;
	
	final int transparency = 200;
	final Color fontColor = Color.BLACK;
	final Color c0 = new Color(100,100,100, transparency);
	final Color c1 = new Color(100,100,100, transparency);
	
	ProteinModel model;
	double[][] subRates;
	double[] baseFreqs;
	double maxRate = 0;
	double maxFreq = 0;

	Font font;
	double scale;
	
	public ProteinModelDiagram() {
		
	}

	public ProteinModelDiagram(ProteinModel model) {
		setModel(model);
	}

	public void setModel(Model model) {
		this.model = (ProteinModel)model;
		maxRate = 0;
		maxFreq = 0;
		if(model!=null) {
			this.subRates = this.model.getSubRates().clone();
			this.baseFreqs = this.model.getAaFreqs().clone();
			for(int i=0; i<this.subRates.length; i++)
				for(int j=0; j<this.subRates[i].length; j++) {
					this.subRates[i][j] = Math.sqrt(this.subRates[i][j]);
					if(this.subRates[i][j]>maxRate)
						maxRate = this.subRates[i][j];
				}
			
			for(double d: this.baseFreqs) {
				if(d>maxFreq)
					maxFreq = d;
			}
		}
	}
	
	@Override
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
			double scaleX = w/basicWidth;
			double scaleY = h/basicHeight;
			scale = (scaleX>scaleY) ? scaleY : scaleX;
			
			font = (new Font("Monospaced", Font.BOLD, basicFontSize)).deriveFont(Font.BOLD, AffineTransform.getScaleInstance(scale, scale));
			g2d.setFont(font);
			
			//draw freq
			int xf = 0;
			int yf = 0;
			int hf = h/6;
			int wf = w;
			drawBaseFreqs(g2d, new Rectangle(xf, yf, wf, hf));
			
			int xm = 0;
			int ym = hf;
			int hm = (h-hf);
			int wm = w;
			drawSubRateMatrix(g2d, new Rectangle(xm, ym, wm ,hm));
			
		} catch (RuntimeException e)
		{
			log.warn("Model: "+model+"\n", e);
		}
	}
	
	private void drawBaseFreqs(Graphics2D g2d, Rectangle rect) {
		g2d.setColor(c1);
		int x = rect.x;
		int y = rect.y;
		int ch = rect.height/2;
		int cw = rect.width/20;
		int fw = cw/2;
		int offsetX = cw/2;
		Font f = font.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(0.5*scale, 0.5*scale));
		int xs = x + offsetX - fw/2;
		int ys = y+ch + fw/4;
		for(int i=0; i<20; i++) {
			int fh = (int)((baseFreqs[i]*ch)/maxFreq);
			g2d.fillRect(xs, ys-fh, fw, fh);
			
//			int ax = xs;
//			int ay = ys-fh;
//			int bx = ax+fw;
//			int by = ay;
//			int cx = bx;
//			int cy = by + fh;
//			int dx = ax + fw/4;
//			int dy = ay - fw/4;
//			int ex = bx  + fw/4;
//			int ey = by - fw/4;
//			int fx = cx + fw/4;
//			int fy = cy - fw/4;
//			g2d.setPaint(new RadialGradientPaint(new Point(ex-(fw/8), ey+(fw/8)), (float)(fw), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, c1}));
//			g2d.fillPolygon(new int[] {ax, bx, ex, dx}, new int[] {ay, by, ey, dy}, 4);
//			g2d.fillPolygon(new int[] {bx, ex, fx, cx}, new int[] {by, ey, fy, cy}, 4);
			
			
			if(i>0)
				drawString(g2d, f, aa[i], xs+(fw/2), ys+(ch/2));
			xs += cw;
		}
		
		ys = (y + ch) - (int)(((1d/20d)*ch)/maxFreq);
		//g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{1f, 1f}, 0));
		g2d.setColor(Color.BLACK);
		g2d.drawLine(x, ys, rect.width, ys);
		
	}
	
	private void drawSubRateMatrix(Graphics2D g2d, Rectangle rect) {
		Font f = font.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(0.5*scale, 0.5*scale));
		g2d.setColor(c0);
		int x = rect.x;
		int y = rect.y;
		int ch = rect.height/20;
		int cw = rect.width/20;
		int cmax = (ch>cw) ? cw : ch;
		int offsetX = cw/2;
		int offsetY = ch/2;
		int xs = x + cw + offsetX;
		int ys = y + offsetY;
		for(int i=0; i<20; i++) { 
			drawString(g2d, f, aa[i], xs-cw, ys);
			for(int j=i+1; j<20; j++) {
				int r = (int)((subRates[i][j]/maxRate)*cmax/2d*1.5);
				if(r<1) {
					if(subRates[i][j]>0)
						drawPoint(g2d, xs, ys);
				}
				else {
					g2d.setPaint(new RadialGradientPaint(new Point(xs+(r/2), ys-(r/2)), (float)(r), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, c0}));
					fillCircle(g2d, xs, ys, r);
				}
				xs += cw;
			}
			xs = (cw*(i+1)) + cw + offsetX;
			ys += ch;
		}
		
		x = (int)(rect.width/5);
		y = (int)(rect.height);
		drawString(g2d, model.getName(), x, y);
	}
	
	private void fillCircle(Graphics2D g2d, int x, int y, int r) {
		g2d.fillOval(x-r, y-r, 2*r, 2*r);
	}
	
	private void drawPoint(Graphics2D g2d, int x, int y) {
		g2d.drawRect(x, y, 0, 0);
	}
	
	private void drawString(Graphics2D g2d, String s, int x, int y) {
		Color oldColor = g2d.getColor();
		g2d.setColor(fontColor);
		FontMetrics fm = getFontMetrics(g2d.getFont());
		int charW = fm.charWidth('G');
		int charH = fm.getHeight();
		int w = s.length()*charW;
		g2d.drawString(s, x-(w/2), y+(charH/4));
		g2d.setColor(oldColor);
	}
	
	private void drawString(Graphics2D g2d, Font font, String s, int x, int y) {
		Font oldFont = g2d.getFont();
		g2d.setFont(font);
		drawString(g2d, s, x, y);
		g2d.setFont(oldFont);
	}
	
//	@Override
//	public Dimension getPreferredSize()
//	{
//		return getMinimumSize();
//	}
//	
//	@Override
//	public Dimension getMinimumSize()
//	{
//		return new Dimension(basicWidth, basicHeight);
//	}
	
	public static void main(String[] args) {
		ProteinModel mod = (ProteinModel)ModelManager.getInstance().generateModel("mtart", true, true);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ProteinModelDiagram d = new ProteinModelDiagram(mod);
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		p.add(d, BorderLayout.CENTER);
		frame.getContentPane().add(p);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
