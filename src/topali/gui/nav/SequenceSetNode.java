package topali.gui.nav;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import static topali.gui.WinMainMenuBar.*;

public class SequenceSetNode extends INode
{
	private String nodeName;
	
	private AlignmentPanel alignmentPanel;
	
	SequenceSetNode(AlignmentData data)
	{
		super(data);
		
		// Format the nodeName: Alignment (seqs x length)
		nodeName = Text.format(Text.GuiNav.getString("SequenceSetNode.gui01"),
			"" + ss.getSize(), "" + ss.getLength());
		
		alignmentPanel = new AlignmentPanel(data);
	}
	
	public int getTipsKey()
		{ return WinMainTipsPanel.TIPS_ALN; }
	
	public String getHelpKey()
		{ return "view_alignment"; }
	
	public String toString()
		{ return nodeName; }
	
	public JComponent getPanel()
		{ return alignmentPanel; }
	
	// Convenience method to do the above without casting
	public AlignmentPanel getAlignmentPanel()
		{ return alignmentPanel; }
			
	public void setMenus()
	{
		aFileExportDataSet.setEnabled(true);
		
		aAlgnDisplaySummary.setEnabled(true);
		aAlgnPhyloView.setEnabled(true);
		aAlgnSelectAll.setEnabled(true);
		aAlgnSelectNone.setEnabled(true);
		aAlgnSelectUnique.setEnabled(true);
		aAlgnSelectInvert.setEnabled(true);		
		aAlgnFindSeq.setEnabled(true);
		aAlgnGoTo.setEnabled(true);
		
		aAnlsRunPDM.setEnabled(true);
		aAnlsRunHMM.setEnabled(true);
		aAnlsRunDSS.setEnabled(true);
		aAnlsRunLRT.setEnabled(true);
		aAnlsCreateTree.setEnabled(true);
		
		aVamExport.setEnabled(true);
		
		alignmentPanel.getListPanel().setMenus();
	}
}