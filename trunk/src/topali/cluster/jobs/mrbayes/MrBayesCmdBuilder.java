// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

public class MrBayesCmdBuilder
{

	boolean isDNA;
	
	int ngen = 100000;
	int sampleFreq = 10;
	double burnin = 0.25;
	
	public static final String MODEL_4BY4 = "4by4";
	public static final String MODEL_CODON = "codon";
	public static final String MODEL_DOUBLET = "doublet";
	String model = MODEL_4BY4;
	
	public static final String DNAMODEL_JC = "1";
	public static final String DNAMODEL_F81JC = "1";
	public static final String DNAMODEL_K80 = "2";
	public static final String DNAMODEL_HKY = "2";
	public static final String DNAMODEL_SYM = "6";
	public static final String DNAMODEL_GTR = "6";
	String dnaModel = DNAMODEL_F81JC;
	
	public static final String CODE_UNIVERSAL = "universal";
	public static final String CODE_VERTMT = "vertmt";
	public static final String CODE_MYCOPLASMA = "mycoplasma";
	public static final String CODE_YEAST = "yeast";
	public static final String CODE_CILIATES = "ciliates";
	public static final String CODE_METMT = "metmt";
	String code = CODE_UNIVERSAL;
	
	public static final String RATE_EQUAL = "equal";
	public static final String RATE_GAMMA = "gamma";
	public static final String RATE_PROPINV = "propinv";
	public static final String RATE_INVGAMMA = "invgamma";
	public static final String RATE_ADGAMMA = "adgamma";
	String rate = RATE_EQUAL;
	
	int nGammaCat = 4;
	int nBetaCat = 5;
	
	public static final String AAMODEL_MIXED = "mixed";
	public static final String AAMODEL_POISSON = "poisson";
	public static final String AAMODEL_JONES = "jones";
	public static final String AAMODEL_DAYHOFF = "dayhoff";
	public static final String AAMODEL_MTREV = "mtrev";
	public static final String AAMODEL_MTMAM = "mtmam";
	public static final String AAMODEL_WAG = "wag";
	public static final String AAMODEL_RTREV = "rtrev";
	public static final String AAMODEL_CPREV = "cprev";
	public static final String AAMODEL_VT = "vt";
	public static final String AAMODEL_BLOSUM = "blosum";
	public static final String AAMODEL_EQUALIN = "equalin";
	public static final String AAMODEL_GTR = "gtr";
	String aaModel = AAMODEL_POISSON;
	
	public String prset = null;
	
	public MrBayesCmdBuilder(boolean isDNA) {
		this.isDNA = isDNA;
	}
	
	public String getCommands() {
		StringBuffer sb = new StringBuffer();
		sb.append("begin mrbayes;\n");
		
		if(isDNA) {
			sb.append("\tlset nucmodel="+model+";\n");
			if(prset!=null)
				sb.append("\t"+prset+";\n");
			sb.append("\tlset nst="+dnaModel+";\n");
			sb.append("\tlset code="+code+";\n");
			sb.append("\tlset rates="+rate+";\n");
			sb.append("\tlset ngammacat="+nGammaCat+";\n");
			sb.append("\tlset nbetacat="+nBetaCat+";\n");
		}
		else {
			if(aaModel.equals(AAMODEL_MIXED))
				sb.append("\tprset aamodelpr=mixed;\n");
			else
				sb.append("\tprset aamodelpr=fixed("+aaModel+");\n");
			sb.append("\tlset rates="+rate+";\n");
			sb.append("\tlset ngammacat="+nGammaCat+";\n");
		}
		sb.append("\tmcmc nruns=1 ngen="+ngen+" samplefreq="+sampleFreq+";\n");
		int burn = (int)((ngen/sampleFreq)*burnin);
		sb.append("\tsump burnin="+burn+";\n");
		sb.append("\tsumt burnin="+burn+";\n");
		sb.append("end;\n");
		return sb.toString();
	}

	public void setAaModel(String aaModel)
	{
		this.aaModel = aaModel;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public void setNBetaCat(int betaCat)
	{
		nBetaCat = betaCat;
	}

	public void setNGammaCat(int gammaCat)
	{
		nGammaCat = gammaCat;
	}

	public void setNgen(int ngen)
	{
		this.ngen = ngen;
	}

	
	public double getBurnin()
	{
		return burnin;
	}

	public void setBurnin(double burnin)
	{
		this.burnin = burnin;
	}

	public void setDnaModel(String dnaModel, boolean jcK80Sim)
	{
		//jc, k80, sim are submodels of existing models with fixed freq.
		if(jcK80Sim)
			prset = "prset statefreqpr=fixed(equal)";
		else
			prset = null;
		
		this.dnaModel = dnaModel;
	}

	public void setModel(String model)
	{
		this.model = model;
	}

	public void setRate(String rate)
	{
		this.rate = rate;
	}

	public void setSampleFreq(int sampleFreq)
	{
		this.sampleFreq = sampleFreq;
	}
	
	
}
