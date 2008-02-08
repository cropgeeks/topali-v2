// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.print.Printable;

import javax.swing.JPanel;

public abstract class DataVisPanel extends JPanel {

    public static final int FORMAT_CSV = 0;
    public static final int FORMAT_TXT = 1;
    public static final int FORMAT_IMAGE = 2;
    
    String friendlyName;
    
    public DataVisPanel(String friendlyName) {
	this.friendlyName = friendlyName;
    }
    
    public abstract Object getExportable(int format);
    
    public abstract Printable getPrintable();

    public String getFriendlyName() {
        return friendlyName;
    }
    
}
