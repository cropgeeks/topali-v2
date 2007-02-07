// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class SequenceSetParams
{
	// Transition/transvertion ratio for this alignment
	private double tRatio = -1;
	// Gamma rates, alpha (shape) parameter
	private double alpha = -1;
	// Kappa (for Bambe)
	private double kappa = -1;
	// Average distance
	private double avgDist = -1;
	// Nucleotide frequencies
	private double[] freqs = new double[0];
	
	public SequenceSetParams()
	{
	}
	
	public SequenceSetParams(double t, double a, double k, double d, double[] f)
	{
		tRatio = t;
		alpha = a;
		kappa = k;
		avgDist = d;
		freqs = f;
	}
		
	public double getTRatio()
		{ return tRatio; }
	
	public void setTRatio(double tRatio)
		{ this.tRatio = tRatio; }
	
	public double getAlpha()
		{ return alpha;	}
	
	public void setAlpha(double alpha)
		{ this.alpha = alpha; }
	
	public double[] getFrequencies()
		{ return freqs; }
	
	public void setFrequencies(double[] freqs)
		{ this.freqs = freqs; }
	
	public double getAvgDistance()
		{ return avgDist; }
	
	public void setAvgDistance(double avgDist)
		{ this.avgDist = avgDist; }
	
	public double getKappa()
		{ return kappa;	}
	
	public void setKappa(double kappa)
		{ this.kappa = kappa; }
}