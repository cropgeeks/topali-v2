// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class SubstitutionModel  {
	String name;
	double aic1, aic2, bic, lnl;
	
	public SubstitutionModel() {

	}

	public double getAic1()
	{
		return aic1;
	}

	public void setAic1(double aic1)
	{
		this.aic1 = aic1;
	}

	public double getAic2()
	{
		return aic2;
	}

	public void setAic2(double aic2)
	{
		this.aic2 = aic2;
	}

	public double getBic()
	{
		return bic;
	}

	public void setBic(double bic)
	{
		this.bic = bic;
	}

	public double getLnl()
	{
		return lnl;
	}

	public void setLnl(double lnl)
	{
		this.lnl = lnl;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/**
		 * toString methode: creates a String representation of the object
		 * @return the String representation
		 * @author info.vancauwenberge.tostring plugin
	
		 */
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("SubstitutionModel[");
			buffer.append("name = ").append(name);
			buffer.append(", aic1 = ").append(aic1);
			buffer.append(", aic2 = ").append(aic2);
			buffer.append(", bic = ").append(bic);
			buffer.append(", lnl = ").append(lnl);
			buffer.append("]");
			return buffer.toString();
		}
	
}
