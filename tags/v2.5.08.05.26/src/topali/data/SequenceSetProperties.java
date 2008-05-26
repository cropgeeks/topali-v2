// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import topali.data.models.*;

public class SequenceSetProperties 
{
	public static final int TYPE_DNA = 1;
	public static final int TYPE_RNA = 2;
	public static final int TYPE_PROTEIN = 4;
	public static final int TYPE_UNKNOWN = 8;
	
	// Sequence type
	private int type = TYPE_UNKNOWN;
	
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
	
	private boolean isAligned = false;
	
	private Model model = null;
	
	private boolean needsCalculation = true;
	
	public SequenceSetProperties()
	{
	}

	public SequenceSetProperties(SequenceSetProperties para) {
		this();
		this.type = para.type;
		this.isAligned = para.isAligned;
		this.model = (para.model instanceof DNAModel) ? new DNAModel((DNAModel)para.model) : new ProteinModel((ProteinModel)para.model);
	}
	
	public boolean isNucleotides() {
		return (type==TYPE_DNA || type==TYPE_RNA || type==(TYPE_DNA+TYPE_RNA));
	}
	
	public boolean isProtein() {
		return type==TYPE_PROTEIN;
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
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

	public double[] getFreqs()
	{
		return freqs;
	}

	public void setFreqs(double[] freqs)
	{
		this.freqs = freqs;
	}

	public boolean isAligned()
	{
		return isAligned;
	}

	public void isAligned(boolean isAligned)
	{
		this.isAligned = isAligned;
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
	    if(model==null) {
	    	ModelManager mm = ModelManager.getInstance();
	    	if(type==SequenceSetProperties.TYPE_DNA)
	    		this.model = mm.generateModel("HKY", true, false);
	    	else if(type==SequenceSetProperties.TYPE_RNA)
	    		this.model = mm.generateModel("HKY", true, false);
	    	else if(type==SequenceSetProperties.TYPE_PROTEIN)
	    		this.model = mm.generateModel("WAG", true, false);
	    }
	    
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

	public boolean needsCalculation()
	{
		return needsCalculation;
	}

	public void needsCalculation(boolean needsCalculation)
	{
		this.needsCalculation = needsCalculation;
	}

	
}