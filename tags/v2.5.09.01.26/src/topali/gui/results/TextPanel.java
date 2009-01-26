// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.awt.print.Printable;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.SysPrefs;
import topali.var.utils.*;

public class TextPanel extends DataVisPanel
{
	
	 Logger log = Logger.getLogger(this.getClass());

//	Toolbar positions
	public static final int NO = 0;

	public static final int TOP = 1;

	public static final int LEFT = 2;

	public static final int BOTTOM = 3;

	public static final int RIGHT = 4;
	
	int toolbarPos;
	
	JTextArea text;
	
	public TextPanel(String name) {
	    super(name);
	    
		this.setLayout(new BorderLayout());
		
		text = new JTextArea();
		text.setEditable(false);
		Utils.setTextAreaDefaults(text);
		this.add(new JScrollPane(text), BorderLayout.CENTER);
	}
	
	public void setText(String text) {
		this.text.setText(text);
	}

	
	public Object getExportable(int format) {
	    if(format==FORMAT_TXT) {
		return text.getText();
	    }
	    return null;
	}

	
	public Printable getPrintable() {
		if(SysPrefs.javaVersion<6)
			return new PrintWrapper(text);
		else
			return text.getPrintable(null, null);
	}
	
	
}
