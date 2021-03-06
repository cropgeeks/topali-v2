// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

/**
 * A Positive Selected Site
 */
public class PSSite 
{

	//nuc position
	public int pos;
	//one-letter aa
	public char aa;
	//propability
	public double p;

	public PSSite() {
		
	}
	
	public PSSite(int pos, char aa, double p)
	{
		this.pos = pos;
		this.aa = aa;
		this.p = p;
	}

	public char getAa()
	{
		return aa;
	}

	public void setAa(char aa)
	{
		this.aa = aa;
	}

	public double getP()
	{
		return p;
	}

	public void setP(double p)
	{
		this.p = p;
	}

	public int getPos()
	{
		return pos;
	}

	public void setPos(int pos)
	{
		this.pos = pos;
	}

	
	public String toString()
	{
		return aa+"("+pos+")["+p+"]";
	}
}
