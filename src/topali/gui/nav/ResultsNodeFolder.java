// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import java.awt.*;
import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import static topali.gui.WinMainMenuBar.*;

public class ResultsNodeFolder extends INode
{	
	ResultsNodeFolder(AlignmentData data)
	{
		super(data);
	}
	
	public String toString()
		{ return Text.GuiNav.getString("ResultsNode.gui01"); }
		
	public void setMenus()
	{
		aFileExportDataSet.setEnabled(true);
		
		aVamExport.setEnabled(true);
	}
	
	public JComponent getPanel()
	{
		String msg = null;
		if (data.getResults().size() == 1)
			msg = data.getResults().size() + " result available";
		else
			msg = data.getResults().size() + " results available";
		if (data.getResults().size() != 0)
			msg += " - expand the Results folder to view individual analyses";
		
		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBackground(Color.white);
		p1.add(new JLabel(msg, JLabel.CENTER));
		return p1;
	}
}
