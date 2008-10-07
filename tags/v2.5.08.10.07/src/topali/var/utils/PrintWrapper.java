// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.utils;

import java.awt.*;
import java.awt.print.*;

public class PrintWrapper implements Printable {

	Component toPrint;
	
	public PrintWrapper(Component comp) {
		this.toPrint = comp;
	}
	
	public int print(Graphics g, PageFormat pf, int pageIndex) {
	    int response = NO_SUCH_PAGE;

	    Graphics2D g2 = (Graphics2D) g;

	    Dimension d = toPrint.getSize(); //get size of document
	    double panelWidth = d.width; //width in pixels
	    double panelHeight = d.height; //height in pixels

	    double pageHeight = pf.getImageableHeight(); //height of printer page
	    double pageWidth = pf.getImageableWidth(); //width of printer page

	    double scale = pageWidth / panelWidth;
	    int totalNumPages = (int) Math.ceil(scale * panelHeight / pageHeight);

	    //  make sure not print empty pages
	    if (pageIndex >= totalNumPages) {
	      response = NO_SUCH_PAGE;
	    }
	    else {

	      //  shift Graphic to line up with beginning of print-imageable region
	      g2.translate(pf.getImageableX(), pf.getImageableY());

	      //  shift Graphic to line up with beginning of next page to print
	      g2.translate(0f, -pageIndex * pageHeight);

	      //  scale the page so the width fits...
	      g2.scale(scale, scale);

	      toPrint.paint(g2); //repaint the page for printing
	      
	      response = Printable.PAGE_EXISTS;
	    }
	    return response;
	  }
	

}
