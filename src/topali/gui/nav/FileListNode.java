package topali.gui.nav;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;

class FileListNode extends INode
{
	private FileListPanel panel;
	
	FileListNode(AlignmentData data)
	{
		super(data);		
		panel = new FileListPanel(data);
	}
	
	public int getTipsKey()
		{ return WinMainTipsPanel.TIPS_NONE; }
	
	public String toString()
		{ return data.name; }
	
	public JComponent getPanel()
		{ return panel; }
	
	public void setMenus()
	{
	}
}