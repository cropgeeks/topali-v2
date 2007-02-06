// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.print.*;
import java.io.*;
import javax.swing.*;

import topali.analyses.*;
import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.*;

import doe.*;

public class CodeMLResultsPanel extends JPanel
{
	private AlignmentData data;
	private CodeMLResult result;
	
	public CodeMLResultsPanel(AlignmentData data, CodeMLResult result)
	{
		this.data = data;
		this.result = result;
		
		
		setLayout(new BorderLayout());
		add(new JLabel(result.guiName));
	}
		
	
}