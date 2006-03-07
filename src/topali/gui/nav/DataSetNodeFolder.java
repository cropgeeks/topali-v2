package topali.gui.nav;

import java.awt.*;
import javax.swing.*;

import topali.data.*;
import topali.gui.*;
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