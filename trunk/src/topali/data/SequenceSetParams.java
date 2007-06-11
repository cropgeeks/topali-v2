// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class SequenceSetParams
{
	public static final String GENETICCODE_UNIVERSAL = "Universal";
	public static final String GENETICCODE_VERTMT = "Vertebrate Mitochondrial DNA";
	public static final String GENETICCODE_MYCOPLASMA = "Mycoplasma";
	public static final String GENETICCODE_YEAST = "Yeast";
	public static final String GENETICCODE_CILIATES = "Ciliates";
	public static final String GENETICCODE_METMT = "Metazoan Mitochondrial DNA";
	
	public static final String MODEL_AA_POISSON = "Poisson";
	public static final String MODEL_AA_JONES = "Jones";
	public static final String MODEL_AA_DAYHOFF = "Dayhoff";
	public static final String MODEL_AA_MTREV = "MTRev";
	public static final String MODEL_AA_MTMAM = "MTMam";
	public static final String MODEL_AA_WAG = "WAG";
	public static final String MODEL_AA_RTREV = "RTRev";
	public static final String MODEL_AA_CPREV = "CPRev";
	public static final String MODEL_AA_VT = "VT";
	public static final String MODEL_AA_BLOSUM = "Blosum";
	public static final String MODEL_AA_EQUALIN = "Equalin";
	public static final String MODEL_AA_GTR = "GTR";
	
	public static final String MODEL_DNA_JC = "JC";
	public static final String MODEL_DNA_F81 = "F81";
	public static final String MODEL_DNA_K80 = "K80";
	public static final String MODEL_DNA_HKY = "HKY";
	public static final String MODEL_DNA_TRN = "TRN";
	public static final String MODEL_DNA_K3P = "K3P";
	public static final String MODEL_DNA_TIM = "TIM";
	public static final String MODEL_DNA_TVM = "TVM";
	public static final String MODEL_DNA_SYM = "SYM";
	public static final String MODEL_DNA_GTR = "GTR";
	
	public static final String[] availCodes = new String[] {GENETICCODE_UNIVERSAL, GENETICCODE_CILIATES, GENETICCODE_METMT, GENETICCODE_MYCOPLASMA, GENETICCODE_VERTMT, GENETICCODE_YEAST};
	public static final String[] availAAModels = new String[] {MODEL_AA_WAG, MODEL_AA_BLOSUM, MODEL_AA_CPREV, MODEL_AA_DAYHOFF, MODEL_AA_EQUALIN, MODEL_AA_GTR, MODEL_AA_JONES, MODEL_AA_MTMAM, MODEL_AA_MTREV, MODEL_AA_POISSON, MODEL_AA_RTREV, MODEL_AA_VT};
	public static final String[] availDNAModels = new String[] {MODEL_DNA_JC, MODEL_DNA_F81, MODEL_DNA_K80, MODEL_DNA_HKY, MODEL_DNA_SYM, MODEL_DNA_GTR};
	
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
	
	private String model = null;
	
	private boolean modelGamma = false;
	
	private boolean modelInv = false;
	
	private boolean needCalculation = true;
	
	public SequenceSetParams()
	{
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
			model = isDNA ? MODEL_DNA_F81 : MODEL_AA_WAG;
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

	public String getModel()
	{
		return model;
	}

	public void setModel(String model)
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

	public boolean isModelGamma()
	{
		return modelGamma;
	}

	public void setModelGamma(boolean modelGamma)
	{
		this.modelGamma = modelGamma;
	}

	public boolean isModelInv()
	{
		return modelInv;
	}

	public void setModelInv(boolean modelInv)
	{
		this.modelInv = modelInv;
	}

	
}