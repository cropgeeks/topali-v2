package topali.gui;

import java.awt.*;

import javax.swing.JPanel;

public class GradientPanel extends JPanel
{
	public static final int ORIGINAL = 1;
	public static final int OFFICE2003 = 2;
	
	private static Color pColor = new JPanel().getBackground();
	private String title = "";
	private int style;
	
	private static Color OFFICE_COLOR1, OFFICE_COLOR2;
	private static Color OFFICE_TEXT1, OFFICE_TEXT2;
	
	static
	{
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		String style = (String) toolkit.getDesktopProperty("win.xpstyle.colorName");
		
		// Windows classic
		if (style == null)
		{
			OFFICE_COLOR1 = new Color(242, 241, 239);
			OFFICE_COLOR2 = new Color(212, 208, 200);
			OFFICE_TEXT1 = Color.black;
			OFFICE_TEXT2 = Color.white;
		}
		// Blue
		else if (style.equals("NormalColor"))
		{
			OFFICE_COLOR1 = new Color(196, 218, 250);
			OFFICE_COLOR2 = new Color(160, 191, 255);
			OFFICE_TEXT1 = new Color(15, 65, 141);
			OFFICE_TEXT2 = Color.white;
		}
		// Silver
		else if (style.equals("Metallic"))
		{
			OFFICE_COLOR1 = new Color(241, 241, 245);
			OFFICE_COLOR2 = new Color(216, 216, 230);
			OFFICE_TEXT1 = Color.black;
			OFFICE_TEXT2 = Color.white;
		}
		// Olive
		else if (style.equals("HomeStead"))
		{
			OFFICE_COLOR1 = new Color(239, 238, 220);
			OFFICE_COLOR2 = new Color(218, 218, 170);
			OFFICE_TEXT1 = Color.black;
			OFFICE_TEXT2 = Color.white;
		}
	}
	
	public GradientPanel(String title)
	{ 
		this.title = title;
		setMinimumSize(new Dimension(100, 22));
		setPreferredSize(new Dimension(100, 22));
		
		style = ORIGINAL;
	}
	
	public void setStyle(int style)
		{ this.style = style; }
	
	public void setTitle(String title)
	{
		this.title = title;
		repaint();
	}		
	
	public void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics;
		
		switch (style)
		{
			case ORIGINAL : paintOriginal(g); break;
			case OFFICE2003 : paintOffice2003(g); break;
		}
	}
	
	private void paintOriginal(Graphics2D g)
	{
		g.setPaint(new GradientPaint(
			0, 0, new Color(140, 165, 214),
			getWidth() - 25, 0, pColor));
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(new Font("Dialog", Font.BOLD, 11));
		g.setColor(Color.black);		
		g.drawString(title, 20, 15);
		g.setColor(Color.white);
		g.drawString(title, 21, 14);
	}
	
	private void paintOffice2003(Graphics2D g)
	{
		g.setPaint(
			new GradientPaint(0, 0, OFFICE_COLOR1, 0, getHeight(), OFFICE_COLOR2));
		
//		g.setColor(new Color(175, 203, 247));
//		g.setPaint(new GradientPaint(
//			0, 0, new Color(175, 203, 247),
//			getWidth() - 25, 0, pColor));
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(new Font("Dialog", Font.BOLD, 11));
		g.setColor(OFFICE_TEXT1);		
		g.drawString(title, 20, 15);
		g.setColor(OFFICE_TEXT2);
		g.drawString(title, 21, 14);
		
		g.setColor(new Color(158, 190, 245));
		g.drawLine(0, 0, getWidth(), 0);
	}
}