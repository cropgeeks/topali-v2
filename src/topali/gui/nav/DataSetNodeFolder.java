// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import static topali.gui.WinMainMenuBar.*;

import java.awt.*;

import javax.swing.*;

import topali.data.AlignmentData;

class DataSetNodeFolder extends INode
{
	DataSetNodeFolder(AlignmentData data)
	{
		super(data);
	}

	
	public String toString()
	{
		return data.getName();
	}

	
	public void setMenus()
	{
		aFileExportDataSet.setEnabled(true);

		aAlgnRemove.setEnabled(true);
	}

	
	public JPanel getPanel()
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.white);
		p.add(new JLabel(data.getName(), SwingConstants.CENTER));
		return p;
	}
}