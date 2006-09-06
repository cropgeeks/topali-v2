// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class AlignmentResult extends AnalysisResult
{
	// Sequences to be analysed
	public String[] selectedSeqs;
	
	
	// Are tree tooltips enabled for this result?
	public boolean useTreeToolTips = false;
	// If so, what window size is used when creating the trees?
	public int treeToolTipWindow = 500;
	
	
	public AlignmentResult()
	{
	}
}