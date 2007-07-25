// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.JComponent;

import topali.data.AlignmentData;
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
	{
		return WinMainTipsPanel.TIPS_NONE;
	}

	public String toString()
	{
		return data.name;
	}

	public JComponent getPanel()
	{
		return panel;
	}

	public void setMenus()
	{
	}
}