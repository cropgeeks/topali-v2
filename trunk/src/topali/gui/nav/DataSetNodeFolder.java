// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import java.awt.*;
import javax.swing.*;

import topali.data.*;
import static topali.gui.WinMainMenuBar.*;

class DataSetNodeFolder extends INode
{
	DataSetNodeFolder(AlignmentData data)
	{
		super(data);
	}
	
	public String toString()
		{ return data.name; }
		
	public void setMenus()
	{
		aFileExportDataSet.setEnabled(true);
		
		aAlgnRemove.setEnabled(true);
		
		aVamExport.setEnabled(true);
	}
	
	public JPanel getPanel()
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.white);
		p.add(new JLabel(data.name, JLabel.CENTER));
		return p;
	}
}