// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import topali.data.models.*;

public class SequenceSetParams 
{
	public static final String GENETICCODE_UNIVERSAL = "Universal";
	public static final String GENETICCODE_VERTMT = "Vertebrate Mitochondrial DNA";
	public static final String GENETICCODE_MYCOPLASMA = "Mycoplasma";
	public static final String GENETICCODE_YEAST = "Yeast";
	public static final String GENETICCODE_CILIATES = "Ciliates";
	public static final String GENETICCODE_METMT = "Metazoan Mitochondrial DNA";
	
	public static final String[] availCodes = new String[] {GENETICCODE_UNIVERSAL, GENETICCODE_CILIATES, GENETICCODE_METMT, GENETICCODE_VERTMT, GENETICCODE_YEAST};
	
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

	private boolean isDNA = true;
	
	private boolean isAligned = true;
	
	private String codonUsage = null;
	
	private String geneticCode = GENETICCODE_UNIVERSAL;
	
	private Model model = null;
	
	private boolean needCalculation = true;
	
	public SequenceSetParams()
	{
	}

	public SequenceSetParams(SequenceSetParams para) {
		this();
		this.codonUsage = para.codonUsage;
		this.geneticCode = para.geneticCode;
		this.isAligned = para.isAligned;
		this.isDNA = para.isDNA;
		this.model = (para.model instanceof DNAModel) ? new DNAModel((DNAModel)para.model) : new ProteinModel((ProteinModel)para.model);
	}
	
	public double getAlpha()
	{
		return alpha;
	}

	public void setAlpha(double alpha)
	{
		this.alpha = alpha;
	}

	public double getAvgDist()
	{
		return avgDist;
	}

	public void setAvgDist(double avgDist)
	{
		this.avgDist = avgDist;
	}

	public String getCodonUsage()
	{
		return codonUsage;
	}

	public void setCodonUsage(String codonUsage)
	{
		this.codonUsage = codonUsage;
	}

	public double[] getFreqs()
	{
		return freqs;
	}

	public void setFreqs(double[] freqs)
	{
		this.freqs = freqs;
	}

	public String getGeneticCode()
	{
		return geneticCode;
	}

	public void setGeneticCode(String geneticCode)
	{
		this.geneticCode = geneticCode;
	}

	public boolean isAligned()
	{
		return isAligned;
	}

	public void setAligned(boolean isAligned)
	{
		this.isAligned = isAligned;
	}

	public boolean isDNA()
	{
		return isDNA;
	}

	public void setDNA(boolean isDNA)
	{
		this.isDNA = isDNA;
		if(model==null) {
			ModelManager mm = ModelManager.getInstance();
			model = isDNA ? mm.generateModel("HKY", true, false) : mm.generateModel("WAG", true, false);
		}
	}

	public double getKappa()
	{
		return kappa;
	}

	public void setKappa(double kappa)
	{
		this.kappa = kappa;
	}

	public Model getModel()
	{
		return model;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	public double getTRatio()
	{
		return tRatio;
	}

	public void setTRatio(double ratio)
	{
		tRatio = ratio;
	}

	public boolean isNeedCalculation()
	{
		return needCalculation;
	}

	public void setNeedCalculation(boolean needCalculation)
	{
		this.needCalculation = needCalculation;
	}

	
}