// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.models;

import topali.var.utils.Utils;


public class ProteinModel extends Model
{

	int rankingScore = 0;
	boolean isSpecialMatrix = false;
	
	//Model parameters:
	double[][] subRates = new double[20][20];
	double[] aaFreqs = new double[] {1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d,1d/20d};
	
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
		System.arraycopy(m.subRates, 0, this.subRates, 0, this.subRates.length);
		System.arraycopy(m.aaFreqs, 0, this.aaFreqs, 0, this.aaFreqs.length);
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
		return 0;
	}

	@Override
	public int getRankingScore()
	{
		return rankingScore;
	}
	
	public double[][] getSubRates()
	{
		double[][] d = new double[20][20];
		for(int i=0; i<20; i++) {
			double[] tmp = new double[20];
			System.arraycopy(subRates[i], 0, tmp, 0, 20);
			d[i] = tmp;
		}
		return d;
	}

	public void setSubRates(double[][] subRates)
	{
		this.subRates = new double[20][20];
		for(int i=0; i<20; i++) {
			double[] tmp = new double[20];
			System.arraycopy(subRates[i], 0, tmp, 0, 20);
			this.subRates[i] = tmp;
		}
	}

	public double[] getAaFreqs()
	{
		return aaFreqs.clone();
	}

	public void setAaFreqs(double[] aaFreqs)
	{
		this.aaFreqs = aaFreqs.clone();
	}

	public String castorGetSubRates() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<subRates.length; i++) 
			for(int j=0; j<subRates[i].length; j++) {
				sb.append(subRates[i][j]);
				sb.append('|');
			}
		return sb.toString();
	}

	public void castorSetSubRates(String s) {
		String[] tmp = s.split("\\|");
		for(int i=0; i<subRates.length; i++) 
			for(int j=0; j<subRates[i].length; j++) {
				subRates[i][j] = Double.parseDouble(tmp[i*20+j]);
			}
	}
	
	public String castorGetAaFreqs() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<aaFreqs.length; i++) {
			sb.append(aaFreqs[i]);
			sb.append('|');
		}	
		return sb.toString();
	}
	
	public void castorSetAaFreqs(String s) {
		String[] tmp = s.split("\\|");
		for(int i=0; i<subRates.length; i++) 
			aaFreqs[i] = Double.parseDouble(tmp[i]);
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append("AA Frequencies: "+Utils.arrayToString(aaFreqs, ',')+"\n");
		sb.append("Substitution Rates:\n");
		for(int i=0; i<20; i++) {
		    sb.append(Utils.arrayToString(subRates[i], ',')+"\n");
		}
		return sb.toString();
	}
}
