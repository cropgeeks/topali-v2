// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.models.*;
import topali.gui.Prefs;

public class DNAModelDiagram extends ModelDiagram
{
	
	Logger log = Logger.getLogger(this.getClass());

	final int basicFontSize = 13;
	final int basicWidth = 100;
	final int basicHeight = 120;
	
	final int transparency = 200;
	
	final Color c0 = new Color(0, 128, 255, transparency);
	final Color c1 = new Color(255, 128, 0, transparency);
	final Color c2 = new Color(128, 220, 0, transparency);
	final Color c3 = new Color(220, 220, 0, transparency);
	final Color c4 = new Color(255, 50, 0, transparency);
	final Color c5 = new Color(100, 100, 100, transparency);
	
	final Color c10 = new Color(0,255,0, transparency);
	final Color c11 = new Color(255,128,0, transparency);
	final Color c12 = new Color(255,0,0, transparency);
	final Color c13 = new Color(0,0,255, transparency);
	final Color c14 = new Color(100,100,100, transparency);
	
	DNAModel model;
	double[] subRates;
	char[] subRateGroups;
	double[] baseFreqs;
	char[] baseFreqGroups;
	double maxRate = 0;
	double maxFreq = 0;

	public DNAModelDiagram() {
		
	}

	public DNAModelDiagram(DNAModel model) {
		setModel(model);
	}

	public void setModel(Model model) {
		this.model = (DNAModel)model;
		
		maxRate = 0;
		maxFreq = 0;
		
		if(model!=null) {
			this.subRates = this.model.getSubRates().clone();
			this.baseFreqs = this.model.getBaseFreqs().clone();
			this.subRateGroups = this.model.getSubRateGroups();
			this.baseFreqGroups = this.model.getBaseFreqGroups();
			for(int i=0; i<this.subRates.length; i++) {
				this.subRates[i] = Math.sqrt(this.subRates[i]);
				if(this.subRates[i]>maxRate)
					maxRate = this.subRates[i];
			}
			
			for(int i=0; i<this.baseFreqs.length; i++) {
				if(this.baseFreqs[i]>maxFreq)
					maxFreq = this.baseFreqs[i];
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
			
			Font font = new Font("Monospaced", Font.BOLD, basicFontSize);
			g2d.setFont(font);
			
			//total w, h
			int w = getWidth();
			int h = getHeight();
			
			double scaleX = w/basicWidth;
			double scaleY = h/basicHeight;
			double scale = (scaleX>scaleY) ? scaleY : scaleX;
			g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, AffineTransform.getScaleInstance(scale, scale)));
			
			//cell w, h
			int cw = w/4;
			int ch = h/6;
			
			int l = (cw<ch) ? cw : ch;
			
			//centers of the cells
			Point[][] mp = new Point[6][4];
			for(int i=0; i<6; i++) {
				for(int j=0; j<4; j++) {
					int x = ((j+1)*cw)-(cw/2);
					int y = ((i+1)*ch)-(ch/2);
					mp[i][j] = new Point(x,y);
				}
			}
			
			//offset from the center to the top,left
			int offsetX = cw/2;
			int offsetY = ch/2;
			
			//draw grid
			Point start = mp[3][1];
			Point end = mp[3][3];
			g2d.drawLine(start.x-offsetX, start.y, end.x+offsetX, end.y);
			
			start = mp[4][2];
			end = mp[4][3];
			g2d.drawLine(start.x-offsetX, start.y, end.x+offsetX, end.y);
			
			start = mp[5][3];
			end = mp[5][3];
			g2d.drawLine(start.x-offsetX, start.y, end.x+offsetX, end.y);
			
			start = mp[3][1];
			end = mp[3][1];
			g2d.drawLine(start.x, start.y-offsetY, end.x, end.y+offsetY);
			
			start = mp[3][2];
			end = mp[4][2];
			g2d.drawLine(start.x, start.y-offsetY, end.x, end.y+offsetY);
			
			start = mp[3][3];
			end = mp[5][3];
			g2d.drawLine(start.x, start.y-offsetY, end.x, end.y+offsetY);
			
			//draw legend
			Point p = mp[2][1];
			drawString(g2d, "C", p.x, p.y);
			p = mp[2][2];
			drawString(g2d, "G", p.x, p.y);
			p = mp[2][3];
			drawString(g2d, "T", p.x, p.y);
			p = mp[3][0];
			drawString(g2d, "A", p.x, p.y);
			p = mp[4][1];
			drawString(g2d, "C", p.x, p.y);
			p = mp[5][2];
			drawString(g2d, "G", p.x, p.y);
			
			p = mp[5][0];
			drawString(g2d, model.getName(), p.x+offsetX, p.y);
			
			//draw numbercode of model
			p = mp[0][1];
			drawString(g2d, g2d.getFont().deriveFont(Font.BOLD, AffineTransform.getScaleInstance(scale*1.3, scale*1.3)), new String(model.getSubRateGroups()), p.x, p.y);
			p = mp[0][3];
			drawString(g2d, ""+model.getBaseFreqGroups()[0], p.x-(cw/4), p.y-(ch/4));
			p = mp[0][3];
			drawString(g2d, ""+model.getBaseFreqGroups()[1], p.x+(cw/4), p.y-(ch/4));
			p = mp[0][3];
			drawString(g2d, ""+model.getBaseFreqGroups()[2], p.x-(cw/4), p.y+(ch/4));
			p = mp[0][3];
			drawString(g2d, ""+model.getBaseFreqGroups()[3], p.x+(cw/4), p.y+(ch/4));
			
			
			Color fixedColor = c5;
			
			//draw sub rate circels
			Color col = (subRateGroups[0]==subRateGroups[5]) ? fixedColor : getSubRateColor(subRateGroups[0]);
			//g2d.setColor(col);
			int r = (int)((subRates[0]/maxRate)*l/2);
			if(r==0)
				r = 1;
			p = mp[3][1];
			g2d.setPaint(new RadialGradientPaint(new Point(p.x+(r/2), p.y-(r/2)), (float)(r), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
			fillCircle(g2d, p.x, p.y, r);
			
			col = (subRateGroups[1]==subRateGroups[5]) ? fixedColor : getSubRateColor(subRateGroups[1]);
			//g2d.setColor(col);
			r = (int)((subRates[1]/maxRate)*l/2);
			if(r==0)
				r = 1;
			p = mp[3][2];
			g2d.setPaint(new RadialGradientPaint(new Point(p.x+(r/2), p.y-(r/2)), (float)(r), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
			fillCircle(g2d, p.x, p.y, r);
			
			col = (subRateGroups[2]==subRateGroups[5]) ? fixedColor : getSubRateColor(subRateGroups[2]);
			//g2d.setColor(col);
			r = (int)((subRates[2]/maxRate)*l/2);
			if(r==0)
				r = 1;
			p = mp[3][3];
			g2d.setPaint(new RadialGradientPaint(new Point(p.x+(r/2), p.y-(r/2)), (float)(r), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
			fillCircle(g2d, p.x, p.y, r);
			
			col = (subRateGroups[3]==subRateGroups[5]) ? fixedColor : getSubRateColor(subRateGroups[3]);
			//g2d.setColor(col);
			r = (int)((subRates[3]/maxRate)*l/2);
			if(r==0)
				r = 1;
			p = mp[4][2];
			g2d.setPaint(new RadialGradientPaint(new Point(p.x+(r/2), p.y-(r/2)), (float)(r), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
			fillCircle(g2d, p.x, p.y, r);
			
			col = (subRateGroups[4]==subRateGroups[5]) ? fixedColor : getSubRateColor(subRateGroups[4]);
			//g2d.setColor(col);
			r = (int)((subRates[4]/maxRate)*l/2);
			if(r==0)
				r = 1;
			p = mp[4][3];
			g2d.setPaint(new RadialGradientPaint(new Point(p.x+(r/2), p.y-(r/2)), (float)(r), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
			fillCircle(g2d, p.x, p.y, r);
			
			//g2d.setColor(c5);
			r = (int)((subRates[5]/maxRate)*l/2);
			if(r==0)
				r = 1;
			p = mp[5][3];
			g2d.setPaint(new RadialGradientPaint(new Point(p.x+(r/2), p.y-(r/2)), (float)(r), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, c5}));
			fillCircle(g2d, p.x, p.y, r);
			
			//draw base freq histograms
			int fw = cw/2;
			
			col = getBaseFreqColor(baseFreqGroups[0]);
			g2d.setColor(col);
			int fh = (int)((baseFreqs[0]*ch)/maxFreq)-2-fw/4;
			p = mp[1][0];
			g2d.fillRect(p.x-fw/2, p.y+offsetY-fh, fw, fh);
//			int ax = p.x-fw/2;
//			int ay = p.y+offsetY-fh;
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
//			g2d.setPaint(new RadialGradientPaint(new Point(ex-(fw/8), ey+(fw/8)), (float)(fw), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
//			g2d.fillPolygon(new int[] {ax, bx, ex, dx}, new int[] {ay, by, ey, dy}, 4);
//			g2d.fillPolygon(new int[] {bx, ex, fx, cx}, new int[] {by, ey, fy, cy}, 4);
			
			col = getBaseFreqColor(baseFreqGroups[1]);
			g2d.setColor(col);
			fh = (int)((baseFreqs[1]*ch)/maxFreq)-2-fw/4;
			p = mp[1][1];
			g2d.fillRect(p.x-fw/2, p.y+offsetY-fh, fw, fh);
//			 ax = p.x-fw/2;
//			 ay = p.y+offsetY-fh;
//			 bx = ax+fw;
//			 by = ay;
//			 cx = bx;
//			 cy = by + fh;
//			 dx = ax + fw/4;
//			 dy = ay - fw/4;
//			 ex = bx  + fw/4;
//			 ey = by - fw/4;
//			 fx = cx + fw/4;
//			 fy = cy - fw/4;
//			g2d.setPaint(new RadialGradientPaint(new Point(ex-(fw/8), ey+(fw/8)), (float)(fw), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
//			g2d.fillPolygon(new int[] {ax, bx, ex, dx}, new int[] {ay, by, ey, dy}, 4);
//			g2d.fillPolygon(new int[] {bx, ex, fx, cx}, new int[] {by, ey, fy, cy}, 4);
			
			col = getBaseFreqColor(baseFreqGroups[2]);
			g2d.setColor(col);
			fh = (int)((baseFreqs[2]*ch)/maxFreq)-2-fw/4;
			p = mp[1][2];
			g2d.fillRect(p.x-fw/2, p.y+offsetY-fh, fw, fh);
//			ax = p.x-fw/2;
//			 ay = p.y+offsetY-fh;
//			 bx = ax+fw;
//			 by = ay;
//			 cx = bx;
//			 cy = by + fh;
//			 dx = ax + fw/4;
//			 dy = ay - fw/4;
//			 ex = bx  + fw/4;
//			 ey = by - fw/4;
//			 fx = cx + fw/4;
//			 fy = cy - fw/4;
//			g2d.setPaint(new RadialGradientPaint(new Point(ex-(fw/8), ey+(fw/8)), (float)(fw), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
//			g2d.fillPolygon(new int[] {ax, bx, ex, dx}, new int[] {ay, by, ey, dy}, 4);
//			g2d.fillPolygon(new int[] {bx, ex, fx, cx}, new int[] {by, ey, fy, cy}, 4);
			
			col = getBaseFreqColor(baseFreqGroups[3]);
			g2d.setColor(col);
			fh = (int)((baseFreqs[3]*ch)/maxFreq)-2-fw/4;
			p = mp[1][3];
			g2d.fillRect(p.x-fw/2, p.y+offsetY-fh, fw, fh);
//			ax = p.x-fw/2;
//			 ay = p.y+offsetY-fh;
//			 bx = ax+fw;
//			 by = ay;
//			 cx = bx;
//			 cy = by + fh;
//			 dx = ax + fw/4;
//			 dy = ay - fw/4;
//			 ex = bx  + fw/4;
//			 ey = by - fw/4;
//			 fx = cx + fw/4;
//			 fy = cy - fw/4;
//			g2d.setPaint(new RadialGradientPaint(new Point(ex-(fw/8), ey+(fw/8)), (float)(fw), new float[]{0.0f, 1f}, new Color[] {Color.WHITE, col}));
//			g2d.fillPolygon(new int[] {ax, bx, ex, dx}, new int[] {ay, by, ey, dy}, 4);
//			g2d.fillPolygon(new int[] {bx, ex, fx, cx}, new int[] {by, ey, fy, cy}, 4);
			
			g2d.setColor(Color.DARK_GRAY);
			fh = (int)(0.25*ch/maxFreq)-2-fw/4;
			p = mp[1][0];
			Point p2 = mp[1][3];
			g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5f, 2f}, 0));
			g2d.drawLine(p.x-fw/2, p.y+offsetY-fh, p2.x+fw/2, p.y+offsetY-fh);
			
		} catch (RuntimeException e)
		{
			log.warn("Model: "+model+"\n", e);
		}
	}
	
	private void fillCircle(Graphics2D g2d, int x, int y, int r) {
		g2d.fillOval(x-r, y-r, 2*r, 2*r);
	}
	
	private void drawString(Graphics2D g2d, String s, int x, int y) {
		FontMetrics fm = getFontMetrics(g2d.getFont());
		int charW = fm.charWidth('G');
		int charH = fm.getHeight();
		int w = s.length()*charW;
		g2d.drawString(s, x-(w/2), y+(charH/4));
	}
	
	private void drawString(Graphics2D g2d, Font font, String s, int x, int y) {
		Font oldFont = g2d.getFont();
		g2d.setFont(font);
		drawString(g2d, s, x, y);
		g2d.setFont(oldFont);
	}
	
	private Color getSubRateColor(char c) {
		int i = Integer.parseInt(""+c);
		switch(i) {
		case 0: return c0;
		case 1: return c1;
		case 2: return c2;
		case 3: return c3;
		case 4: return c4;
		case 5: return c5;
		}
		return c0;
	}
	
	private Color getBaseFreqColor(char c) {
		int i = Integer.parseInt(""+c);
		
		if(model.getNBaseFreqGroups()<=1)
			return c14;
		
		switch(i) {
		case 0: return c10;
		case 1: return c11;
		case 2: return c12;
		case 3: return c13;
		default: return c14;
		}
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
		DNAModel mod = (DNAModel)ModelManager.getInstance().generateModel("gtr", true, true);
		mod.setBaseFreqs(0.2,0.3,0.4,0.1);
		mod.setSubRates(1,2,3,4,5,6);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		DNAModelDiagram d = new DNAModelDiagram(mod);
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		p.add(d, BorderLayout.CENTER);
		frame.getContentPane().add(p);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
