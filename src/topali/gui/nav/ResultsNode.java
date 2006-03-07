// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.*;
import javax.swing.tree.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.results.*;
import static topali.gui.WinMainMenuBar.*;

public abstract class ResultsNode extends INode
{
	protected AnalysisResult result;
	
	public ResultsNode(AlignmentData data, AnalysisResult result)
	{
		super(data);
		
		this.result = result;
	}
	
	AnalysisResult getResult()
		{ return result; }
	
	public void setMenus()
	{
		aFileExportDataSet.setEnabled(true);
		
		aAnlsCreateTree.setEnabled(true);
		aAnlsPartition.setEnabled(true);
		aAnlsRename.setEnabled(true);
		aAnlsRemove.setEnabled(true);
		
		aVamExport.setEnabled(true);
	}
}