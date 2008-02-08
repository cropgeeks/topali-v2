// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.models;

import topali.var.utils.Utils;


public class DNAModel extends Model
{

	//Array positions:
	//Substitution Rates:
	//A-C A-G A-T C-G C-T G-T
	// 0   1   2   3   4   5
	//Base frequences:
	//A C G T
	//0 1 2 3
	
	//Parameter estimation
	char[] subRateGroups = new char[] {'0','0','0','0','0','0'};
	char[] baseFreqGroups = new char[] {'0','0','0','0'};
	
	//Model parameters:
	double[] subRates = new double[] {1d/6d, 1d/6d, 1d/6d, 1d/6d, 1d/6d, 1d/6d};
	double[] baseFreqs = new double[] {0.25,0.25,0.25,0.25};
	
	public DNAModel() {
		super();
	}
	
	public DNAModel(int id) {
		super(id);
	}
	
	public DNAModel(DNAModel m) {
		super(m);
		System.arraycopy(m.subRateGroups, 0, this.subRateGroups, 0, m.subRateGroups.length);
		System.arraycopy(m.baseFreqGroups, 0, this.baseFreqGroups, 0, m.baseFreqGroups.length);
		System.arraycopy(m.subRates, 0, this.subRates, 0, m.subRates.length);
		System.arraycopy(m.baseFreqs, 0, this.baseFreqs, 0, m.baseFreqs.length);
	}

	/**
	 * A-C A-G A-T C-G C-T G-T
	 *  0   1   2   3   4   5
	 * @param subRateGroups
	 */
	public void setSubRateGroups(char... subRateGroups) {
		if(subRateGroups.length!=6) 
			throw new IllegalArgumentException("subRateGroups has to contain exactly 6 chars ");
		this.subRateGroups = subRateGroups;
	}
	
	public void castorSetSubRateGroups(String s) {
		this.subRateGroups = s.toCharArray();
	}
	
	/**
	 * A C G T
	 * 0 1 2 3
	 * @param baseFreqGroups
	 */
	public void setBaseFreqGroups(char... baseFreqGroups) {
		if(baseFreqGroups.length!=4)
			throw new IllegalArgumentException("baseFreqGroups has to contain exactly 4 chars ");
		this.baseFreqGroups = baseFreqGroups;
	}
	
	public void castorSetBaseFreqGroups(String s) {
		this.baseFreqGroups = s.toCharArray();
	}
	
	/**
	 * A-C A-G A-T C-G C-T G-T
	 *  0   1   2   3   4   5
	 * @param subRates
	 */
	public void setSubRates(double... subRates) {
		if(subRates.length!=6)
			throw new IllegalArgumentException("subRates has to contain 6 doubles");
		this.subRates = subRates;
	}
	
	public void castorSetSubRates(String s) {
		try
		{
			Double[] tmp = (Double[])Utils.stringToArray(Double.class, s, ','); 
			subRates = (double[])Utils.castArray(tmp, double.class);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * A C G T
	 * 0 1 2 3
	 * @param aaFreqs
	 */
	public void setBaseFreqs(double... baseFreqs) {
		if(baseFreqs.length!=4)
			throw new IllegalArgumentException("aaFreqs has to contain 4 doubles");
		this.baseFreqs = baseFreqs;
	}
	
	public void castorSetBaseFreqs(String s) {
		try
		{
			Double[] tmp = (Double[])Utils.stringToArray(Double.class, s, ',');
			baseFreqs = (double[])Utils.castArray(tmp, double.class);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Get number of different substitution rate groups
	 * @return
	 */
	public int getNSubRateGroups() {
		boolean[] flag = new boolean[6];
		for(char c : subRateGroups) {
			if(c=='0')
				flag[0] = true;
			else if(c=='1')
				flag[1] = true;
			else if(c=='2')
				flag[2] = true;
			else if(c=='3')
				flag[3] = true;
			else if(c=='4')
				flag[4] = true;
			else if(c=='5')
				flag[5] = true;
		}
		int i = 0;
		for(boolean b : flag) {
			if(b)
				i++;
		}
		return i;
	}
	
	/**
	 * Get number of different base frequency groups
	 * @return
	 */
	public int getNBaseFreqGroups() {
		boolean[] flag = new boolean[4];
		for(char c : baseFreqGroups) {
			if(c=='0')
				flag[0] = true;
			else if(c=='1')
				flag[1] = true;
			else if(c=='2')
				flag[2] = true;
			else if(c=='3')
				flag[3] = true;
		}
		int i = 0;
		for(boolean b : flag) {
			if(b)
				i++;
		}
		return i;
	}
	
	/**
	 * Substitution rate groups
	 * A-C A-G A-T C-G C-T G-T
	 *  0   1   2   3   4   5
	 * @return char[6]
	 */
	public char[] getSubRateGroups()
	{
		return subRateGroups;
	}
	
	public String castorGetSubRateGroups() {
		return new String(subRateGroups);
	}

	/**
	 * Base frequency groups
	 * A C G T
	 * 0 1 2 3
	 * @return char[4]
	 */
	public char[] getBaseFreqGroups()
	{
		return baseFreqGroups;
	}

	public String castorGetBaseFreqGroups() {
		return new String(baseFreqGroups);
	}
	
	/**
	 * Substitution rates
	 * A-C A-G A-T C-G C-T G-T
	 *  0   1   2   3   4   5
	 * @return double[6]
	 */
	public double[] getSubRates()
	{
		return subRates;
	}
	
	public String castorGetSubRates() {
		return Utils.arrayToString(subRates, ',');
	}
	
	/**
	 * Base frequencies
	 * A C G T
	 * 0 1 2 3
	 * @return double[4]
	 */
	public double[] getBaseFreqs()
	{
		return baseFreqs;
	}

	public String castorGetBaseFreqs() {
		return Utils.arrayToString(baseFreqs, ',');
	}
	
	@Override
	public int getFreeParameters() {
		int result = getNBaseFreqGroups() + getNSubRateGroups();
		return result - 2;
	}

	@Override
	public int getRankingScore()
	{
		return getFreeParameters();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append("Substitution Rate Groups: "+Utils.arrayToString(subRateGroups, ',')+"\n");
		sb.append("Base Frequency Groups: "+Utils.arrayToString(baseFreqGroups, ',')+"\n");
		sb.append("Substitution Rates: "+Utils.arrayToString(subRates, ',')+"\n");
		sb.append("Base Frequencies: "+Utils.arrayToString(baseFreqs, ',')+"\n");
		return sb.toString();
	}
}
