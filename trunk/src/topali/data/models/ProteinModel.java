// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.models;


public class ProteinModel extends Model
{

	int rankingScore = 0;
	boolean isSpecialMatrix = false;
	
	public ProteinModel() {
		super();
	}
	
	public ProteinModel(int id) {
		super(id);
	}
	
	public ProteinModel(ProteinModel m) {
		super(m);
		this.rankingScore = m.rankingScore;
		this.isSpecialMatrix = m.isSpecialMatrix;
	}
	
	public void isSpecialMatrix(boolean b) {
		this.isSpecialMatrix = b;
	}
	
	public boolean isSpecialMatrix() {
		return this.isSpecialMatrix;
	}
	
	public void setRankScore(int rankScore)
	{
		this.rankingScore = rankScore;
	}

	@Override
	public int getFreeParameters()
	{
		return 19;
	}

	@Override
	public int getRankingScore()
	{
		return rankingScore;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append("Special matrix: "+this.isSpecialMatrix);
		return sb.toString();
	}
}
