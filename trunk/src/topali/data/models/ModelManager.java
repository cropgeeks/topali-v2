// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.models;

import java.util.*;

public class ModelManager
{
	//Lists of pre-initialized models
	private List<Model> dnaModels = new ArrayList<Model>();
	private List<Model> proteinModels = new ArrayList<Model>();
	
	private static ModelManager instance = null;
	
	private ModelManager() {
		initDNAModels();
		initProteinModels();
	}
	
	public static ModelManager getInstance() {
		if(instance==null)
			instance = new ModelManager();
		return instance;
	}
	
	public List<Model> listDNAModels() {
		return dnaModels;
	}
	
	public List<Model> listProteinModels() {
		return proteinModels;
	}
	
	public List<Model> listPhymlModels(boolean dna) {
		List<Model> result;
		if(dna) {
			return dnaModels;
		}
		else {
			result = new ArrayList<Model>();
			for(Model m : proteinModels) {
				if(m.is("jtt"))
					result.add(m);
				if(m.is("mtrev"))
					result.add(m);
				if(m.is("wag"))
					result.add(m);
				if(m.is("dcmut"))
					result.add(m);
				if(m.is("rtrev"))
					result.add(m);
				if(m.is("cprev"))
					result.add(m);
				if(m.is("vt"))
					result.add(m);
				if(m.is("blosum"))
					result.add(m);
				if(m.is("mtmam"))
					result.add(m);
				if(m.is("day"))
					result.add(m);
			}
			return result;
		}
	}
	
	public List<Model> listMrBayesModels(boolean dna) {
		List<Model> result;
		if(dna) {
			result = new ArrayList<Model>();
			for(Model m : dnaModels) {
				if(m.is("jc"))
					result.add(m);
				if(m.is("f81"))
					result.add(m);
				if(m.is("k80"))
					result.add(m);
				if(m.is("hky"))
					result.add(m);
				if(m.is("sym"))
					result.add(m);
				if(m.is("gtr"))
					result.add(m);
			}
			return result;
		}
		else {
			result = new ArrayList<Model>();
			for(Model m : proteinModels) {
				if(m.is("poisson"))
					result.add(m);
				if(m.is("jtt"))
					result.add(m);
				if(m.is("day"))
					result.add(m);
				if(m.is("mtrev"))
					result.add(m);
				if(m.is("mtmam"))
					result.add(m);
				if(m.is("wag"))
					result.add(m);
				if(m.is("rtrev"))
					result.add(m);
				if(m.is("cprev"))
					result.add(m);
				if(m.is("vt"))
					result.add(m);
				if(m.is("blosum"))
					result.add(m);
				if(m.is("gtr"))
					result.add(m);
			}
			return result;
		}
	}
	
	/**
	 * Generate a certain model
	 * @param name	Name of the model
	 * @param gamma Use gamma distribution
	 * @param inv   Use invariant sites
	 * @return
	 */
	public Model generateModel(String name, boolean gamma, boolean inv) {
		Model m = null;

		for(Model model : dnaModels) {
			if(model.is(name)) {
				m = new DNAModel((DNAModel)model);
				break;
			}
		}
		
		if(m==null) {
			for(Model model : proteinModels) {
				if(model.is(name)) {
					m = new ProteinModel((ProteinModel)model);
					break;
				}
			}
		}
		
		if(m!=null) {
			m.isGamma(gamma);
			m.isInv(inv);
		}
		
		return m;
	}
	
	/**
	 * Get the closest related model to a certain model
	 * @param m
	 * @return
	 */
	public Model getNearestModel(Model m) {
		if(m instanceof DNAModel) {
			int i = dnaModels.indexOf(m);
			if(i>0)
				return new DNAModel((DNAModel)dnaModels.get(i-1));
		}
		
		else if(m instanceof ProteinModel) {
			if(!((ProteinModel)m).isSpecialMatrix) {
				int i = proteinModels.indexOf(m);
				while(i>0) {
					i--;
					ProteinModel m2 = (ProteinModel)proteinModels.get(i);
					if(!m2.isSpecialMatrix())
						return new ProteinModel(m2);
				}
			}
		}
		
		return m;
	}
	
	private void initDNAModels() {
		DNAModel m = new DNAModel();
		m.setName("JC");
		m.addAlias("Jukes and Cantor,1969");
		m.setBaseFreqGroups('0','0','0','0');
		m.setSubRateGroups('0','0','0','0','0','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("F81");
		m.addAlias("Felsenstein,1981");
		m.setBaseFreqGroups('0','1','2','3');
		m.setSubRateGroups('0','0','0','0','0','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("K80");
		m.addAlias("K2P");
		m.addAlias("Kimura,1980");
		m.setBaseFreqGroups('0','0','0','0');
		m.setSubRateGroups('0','1','0','0','1','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("HKY");
		m.addAlias("HKY85");
		m.addAlias("Hasegawa, Kishino and Yano,1985");
		m.setBaseFreqGroups('0','1','2','3');
		m.setSubRateGroups('0','1','0','0','1','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("TrNef");
		m.addAlias("TrN, equal frequences");
		m.setBaseFreqGroups('0','0','0','0');
		m.setSubRateGroups('0','1','0','0','2','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("TrN");
		m.addAlias("TN");
		m.addAlias("TN93");
		m.addAlias("Tamura and Nei, 1993");
		m.setBaseFreqGroups('0','1','2','3');
		m.setSubRateGroups('0','1','0','0','2','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("K81");
		m.addAlias("K3P");
		m.addAlias("Kimura,1981");
		m.setBaseFreqGroups('0','0','0','0');
		m.setSubRateGroups('0','1','2','2','1','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("K81uf");
		m.addAlias("K3Puf");
		m.addAlias("K81, unequal frequences");
		m.setBaseFreqGroups('0','1','2','3');
		m.setSubRateGroups('0','1','2','2','1','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("TIMef");
		m.addAlias("TIM, equal frequences");
		m.setBaseFreqGroups('0','0','0','0');
		m.setSubRateGroups('0','1','2','2','3','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("TIM");
		m.addAlias("Transition Model");
		m.setBaseFreqGroups('0','1','2','3');
		m.setSubRateGroups('0','1','2','2','3','0');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("TVMef");
		m.addAlias("TVM, equal frequences");
		m.setBaseFreqGroups('0','0','0','0');
		m.setSubRateGroups('0','1','2','3','1','4');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("TVM");
		m.addAlias("Transversion Model");
		m.setBaseFreqGroups('0','1','2','3');
		m.setSubRateGroups('0','1','2','3','1','4');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("SYM");
		m.addAlias("Symmetrical Model");
		m.setBaseFreqGroups('0','0','0','0');
		m.setSubRateGroups('0','1','2','3','4','5');
		dnaModels.add(m);
		
		m = new DNAModel();
		m.setName("GTR");
		m.addAlias("General time reversible");
		m.setBaseFreqGroups('0','1','2','3');
		m.setSubRateGroups('0','1','2','3','4','5');
		dnaModels.add(m);
		
		Collections.sort(dnaModels);
	}

	private void initProteinModels() { 
		int i = 0;
		
		ProteinModel m = new ProteinModel();
		m.setName("Poisson");
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("DAY");
		m.addAlias("Dayhoff");
		m.addAlias("Dayhoff PAM");
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("Blosum");
		m.addAlias("Blosum62");
		m.setRankScore(i++);
		proteinModels.add(m);
	
		m = new ProteinModel();
		m.setName("JTT");
		m.addAlias("Jones");
		m.addAlias("Jones-Taylor-Thornton");
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("VT");
		m.addAlias("Muller and Vingron,2000");
		m.addAlias("Vingron Resolvent Method");
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("WAG");
		m.addAlias("Whelan and Goldman");
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("DCMut");
		m.addAlias("Direct Computation with Mutabilities");
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("GTR");
		m.addAlias("General Time Reversible");
		m.setRankScore(i++);
		proteinModels.add(m);
		
		
		//special models
		m = new ProteinModel();
		m.setName("RTRev");
		m.addAlias("Retroviral Proteins");
		m.isSpecialMatrix(true);
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("CPRev");
		m.addAlias("Chloroplast Proteins");
		m.isSpecialMatrix(true);
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("MTMam");
		m.addAlias("Mammalian Mitochondrial Proteins");
		m.isSpecialMatrix(true);
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("MTArt");
		m.addAlias("Arthropoda Mitochondrial Proteins");
		m.isSpecialMatrix(true);
		m.setRankScore(i++);
		proteinModels.add(m);
		
		m = new ProteinModel();
		m.setName("MTRev24");
		m.addAlias("Proteins encoded on Mitchondrial DNA");
		m.isSpecialMatrix(true);
		m.setRankScore(i++);
		proteinModels.add(m);
		
		Collections.sort(proteinModels);
	}
}
