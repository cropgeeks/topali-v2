// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class ResultsTracker
{
	private int treeRunCount;
	
	private int pdmRunCount;
	private int hmmRunCount;
	private int dssRunCount;
	private int lrtRunCount;
	
	public ResultsTracker()
	{
	}

	public int getTreeRunCount()
		{ return (this.treeRunCount); }
	public void setTreeRunCount(int treeRunCount)
		{ this.treeRunCount = treeRunCount; }

	public int getPdmRunCount()
		{ return (this.pdmRunCount); }
	public void setPdmRunCount(int pdmRunCount)
		{ this.pdmRunCount = pdmRunCount; }

	public int getHmmRunCount()
		{ return (this.hmmRunCount); }
	public void setHmmRunCount(int hmmRunCount)
		{ this.hmmRunCount = hmmRunCount; }

	public int getDssRunCount()
		{ return (this.dssRunCount); }
	public void setDssRunCount(int dssRunCount)
		{ this.dssRunCount = dssRunCount; }

	public int getLrtRunCount()
		{ return (this.lrtRunCount); }
	public void setLrtRunCount(int lrtRunCount)
		{ this.lrtRunCount = lrtRunCount; }
}