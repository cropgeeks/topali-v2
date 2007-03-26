// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class ResultsTracker
{
	private int treeRunCount;

	private int pdmRunCount;

	private int pdm2RunCount;

	private int hmmRunCount;

	private int dssRunCount;

	private int lrtRunCount;

	private int codeMLRunCount;

	public ResultsTracker()
	{
	}

	public int getTreeRunCount()
	{
		return (this.treeRunCount);
	}

	public void setTreeRunCount(int treeRunCount)
	{
		this.treeRunCount = treeRunCount;
	}

	public int getPdmRunCount()
	{
		return (this.pdmRunCount);
	}

	public void setPdmRunCount(int pdmRunCount)
	{
		this.pdmRunCount = pdmRunCount;
	}

	public int getPdm2RunCount()
	{
		return (this.pdm2RunCount);
	}

	public void setPdm2RunCount(int pdm2RunCount)
	{
		this.pdm2RunCount = pdm2RunCount;
	}

	public int getHmmRunCount()
	{
		return (this.hmmRunCount);
	}

	public void setHmmRunCount(int hmmRunCount)
	{
		this.hmmRunCount = hmmRunCount;
	}

	public int getDssRunCount()
	{
		return (this.dssRunCount);
	}

	public void setDssRunCount(int dssRunCount)
	{
		this.dssRunCount = dssRunCount;
	}

	public int getLrtRunCount()
	{
		return (this.lrtRunCount);
	}

	public void setLrtRunCount(int lrtRunCount)
	{
		this.lrtRunCount = lrtRunCount;
	}

	public int getCodeMLRunCount()
	{
		return (this.codeMLRunCount);
	}

	public void setCodeMLRunCount(int codeMLRunCount)
	{
		this.codeMLRunCount = codeMLRunCount;
	}
}