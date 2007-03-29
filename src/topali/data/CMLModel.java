// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;


/**
 * Holds all data of a CodeML model, predefined settings as well as the results after CodeML had been run.
 */
public class CMLModel
{
	//Predefined models
	public final static int MODEL_M0 = 1;
	public final static int MODEL_M1a = 2;
	public final static int MODEL_M2a = 3;
	public final static int MODEL_M1 = 4;
	public final static int MODEL_M2 = 5;
	public final static int MODEL_M3 = 6;
	public final static int MODEL_M7 = 7;
	public final static int MODEL_M8 = 8;

	private static String nl = System.getProperty("line.separator");
	
	//Init Parameters
	public int model;
	public String name;
	public String abbr;
	public int nParameter;
	public boolean supportsPSS;
	public boolean fixedOmega;
	public Map<String, String> settings = new HashMap<String, String>();
	public Vector<Double> wStart = new Vector<Double>();
	
	//Result values
	public double p0 = -1, p1 = -1, p2 = -1;
	public double w0 = -1, w1 = -1, w2 = -1;
	public double p = -1, q = -1;
	public double likelihood = -1;
	public String pss = null;
	public List<PSSite> pssList; //not stored in Castor XML!
	
	//Just for castor. Don't use this constructor!
	public CMLModel() {	
	}
	
	/**
	 * Instantiate a certain predefined model
	 * @param model
	 */
	public CMLModel(int model) {
		
		this.model = model;

		settings.put("seqtype","1");
		settings.put("CodonFreq","2");
		settings.put("model","0");
		settings.put("icode","0");
		settings.put("fix_kappa","0");
		settings.put("kappa","2");
		
		switch(model) {
		case MODEL_M0:
			name = "M0 (one-ratio)";
			abbr = "M0";
			nParameter = 1;
			supportsPSS = false;
			fixedOmega = false;
			wStart.add(5.0);
			settings.put("fix_omega","0");
			settings.put("NSsites","0");
			break;
		case MODEL_M1a:
			name = "M1a (NearlyNeutral)";
			abbr = "M1a";
			nParameter = 2;
			supportsPSS = false;
			fixedOmega = false;
			wStart.add(5.0);
			settings.put("fix_omega","0");
			settings.put("NSsites","1");
			break;
		case MODEL_M2a:
			name = "M2a (PositiveSelection)";
			abbr = "M2a";
			nParameter = 4;
			supportsPSS = true;
			fixedOmega = false;
			wStart.add(0.1);
			wStart.add(0.2);
			wStart.add(0.4);
			wStart.add(0.8);
			wStart.add(1.6);
			wStart.add(3.2);
			settings.put("fix_omega","0");
			settings.put("NSsites","2");
			break;
		case MODEL_M1:
			name = "M1 (NearlyNeutral)";
			abbr = "M1";
			nParameter = 1;
			supportsPSS = false;
			fixedOmega = true;
			settings.put("fix_omega","1");
			settings.put("omega","1");
			settings.put("NSsites","1");
			break;
		case MODEL_M2:
			name = "M2 (PositiveSelection)";
			abbr = "M2";
			nParameter = 3;
			supportsPSS = true;
			fixedOmega = true;
			settings.put("fix_omega","1");
			settings.put("omega","1");
			settings.put("NSsites","2");
			break;
		case MODEL_M3:
			name = "M3 (discrete (3 categories))";
			abbr = "M3";
			nParameter = 5;
			supportsPSS = true;
			fixedOmega = false;
			wStart.add(5.0);
			settings.put("fix_omega","0");
			settings.put("NSsites","3");
			settings.put("ncatG","3");
			break;
		case MODEL_M7:
			name = "M7 (beta (10 categories))";
			abbr = "M7";
			nParameter = 2;
			supportsPSS = false;
			fixedOmega = false;
			wStart.add(5.0);
			settings.put("fix_omega","0");
			settings.put("NSsites","7");
			settings.put("ncatG","10");
			break;
		case MODEL_M8:
			name = "M8 (beta&w>1 (11 categories))";
			abbr = "M8";
			nParameter = 4;
			supportsPSS = true;
			fixedOmega = false;
			wStart.add(0.1);
			wStart.add(0.2);
			wStart.add(0.4);
			wStart.add(0.8);
			wStart.add(1.6);
			wStart.add(3.2);
			settings.put("fix_omega","0");
			settings.put("NSsites","8");
			settings.put("ncatG","10");
			break;
		}
	}
	
	/**
	 * Use this method, if start omega is not fixed, and there are
	 * different start omega values.
	 * @return A list of models, each with a certain start omage value
	 */
	public List<CMLModel> generateModels() {
		List<CMLModel> res = new ArrayList<CMLModel>();
		if(fixedOmega) {
			res.add(this);
			return res;
		}
		
		for(double d : wStart) {
			CMLModel m = new CMLModel(model);
			m.settings.put("omega", String.valueOf(d));
			res.add(m);
		}
		
		return res;
	}
	
	/**
	 * Get the settings as formatted string (to build codeml command line arguments)
	 * @return
	 */
	public String codemlSettings() {
		StringBuffer sb = new StringBuffer();
		sb.append("seqfile = seq.phy" + nl);
		sb.append("treefile = tree.txt" + nl);
		sb.append("outfile = results.txt" + nl);
		sb.append("noisy = 9" + nl);
		sb.append("verbose = 0" + nl);
		sb.append("runmode = 0" + nl);
		
		Set<String> keys = settings.keySet();
		for(String key : keys) {
			sb.append(key+" = "+settings.get(key)+nl);
		}
		
		return sb.toString();
	}

	/**
	 * Get a list of positive selected sites with p > minP
	 * @param minP
	 * @return List of PSSite, or null if this model doesn't support p. s. sites.
	 */
	public List<PSSite> getPSS(double minP)
	{
		if (pss == null)
			return null;

		//parse the pss string just one time
		if (pssList == null)
		{
			pssList = new LinkedList<PSSite>();
			if (!pss.equals(""))
			{
				for (String s : pss.split("\\s"))
				{
					String[] tmp = s.split("\\|");
					int pos = Integer.parseInt(tmp[0]);
					char c = tmp[1].charAt(0);
					double p = Double.parseDouble(tmp[2]);
					pssList.add(new PSSite(pos, c, p));
				}
			}
		}

		List<PSSite> res = new LinkedList<PSSite>();
		for (PSSite ps : pssList)
		{
			if (ps.getP() > minP)
				res.add(ps);
		}
		return res;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Model: "+name+"\n");
		sb.append("Settings: \n");
		Set<String> keys = settings.keySet();
		for(String s : keys) {
			sb.append(s+" = "+settings.get(s)+"\n");
		}
		return sb.toString();
	}
}