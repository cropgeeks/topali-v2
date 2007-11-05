// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class ImagePanel extends JPanel
{

	int w = 50, h = 50;
	BufferedImage[] images;
	
    public ImagePanel() {
         super();
    }
        
	public ImagePanel(BufferedImage... images) {
            this();
            setImages(images);
	}

	public void setImages(BufferedImage... images) {
		this.images = images;
		for(BufferedImage img : images) {
			if(img.getWidth()>w)
				w = img.getWidth();
			if(img.getHeight()>h)
				h = img.getHeight();
		}
		validate();
		repaint();
	}

	@Override
	public void paint(Graphics g)
	{
		super.paint(g);
		
		if(images!=null && images.length>0) {
			for(BufferedImage img : images) {
				g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
			}
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		if(w==0&&h==0)
			return super.getPreferredSize();
		else
			return new Dimension(w, h);
	}
}
