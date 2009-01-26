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

	private boolean isAligned = true;

	private Model model = null;
	private Model cpModel1, cpModel2, cpModel3;

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

	private Model createModel()
	{
		ModelManager mm = ModelManager.getInstance();

	    if(type==SequenceSetProperties.TYPE_RNA)
	    	return mm.generateModel("HKY", true, false);
	    else if(type==SequenceSetProperties.TYPE_PROTEIN)
	    	return mm.generateModel("WAG", true, false);

//	    if(type==SequenceSetProperties.TYPE_DNA)
	    	return mm.generateModel("HKY", true, false);
	}

	public Model getModel()
	{
	    if (model == null) model = createModel();
	    return model;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	public Model getCpModel1()
	{
	    if (cpModel1 == null) cpModel1 = createModel();
	    return cpModel1;
	}

	public void setCpModel1(Model cpModel1)
	{
		this.cpModel1 = cpModel1;
	}

	public Model getCpModel2()
	{
	    if (cpModel2 == null) cpModel2 = createModel();
	    return cpModel2;
	}

	public void setCpModel2(Model cpModel2)
	{
		this.cpModel2 = cpModel2;
	}

	public Model getCpModel3()
	{
	    if (cpModel3 == null) cpModel3 = createModel();
	    return cpModel3;
	}

	public void setCpModel3(Model cpModel3)
	{
		this.cpModel3 = cpModel3;
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