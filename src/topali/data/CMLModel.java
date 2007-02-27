// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;

import topali.mod.Util;


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
	private int model;
	private String name;
	private String abbr;
	private int nParameter;
	private boolean supportsPSS;
	private String settings;

	//Result values
	private double dnDS = -1;
	private double w = -1;
	private double p0 = -1, p1 = -1, p2 = -1;
	private double w0 = -1, w1 = -1, w2 = -1;
	private double p = -1, q = -1, _w = -1;
	private double likelihood = -1;
	private String pss = null;
	private List<PSSite> pssList; //not stored in Castor XML!
	
	//Just for castor. Don't use this constructor!
	public CMLModel() {	
	}
	
	/**
	 * Instantiate a certain predefined model
	 * @param model
	 */
	public CMLModel(int model) {
		
		this.model = model;
		
		StringBuffer sb = new StringBuffer();
		switch(model) {
		case MODEL_M0:
			name = "M0 (one-ratio)";
			abbr = "M0";
			nParameter = 1;
			supportsPSS = false;
			sb.append("seqtype=1;");
			sb.append("CodonFreq=2;");
			sb.append("model=0;");
			sb.append("icode=0;");
			sb.append("fix_kappa=0;");
			sb.append("kappa=2;");
			sb.append("fix_omega=0;");
			sb.append("omega=5;");
			sb.append("NSsites=0;");
			break;
		case MODEL_M1a:
			name = "M1a (NearlyNeutral)";
			abbr = "M1a";
			nParameter = 2;
			supportsPSS = false;
			sb.append("seqtype=1;");
			sb.append("CodonFreq=2;");
			sb.append("model=0;");
			sb.append("icode=0;");
			sb.append("fix_kappa=0;");
			sb.append("kappa=2;");
			sb.append("fix_omega=0;");
			sb.append("omega=5;");
			sb.append("NSsites=1;");
			break;
		case MODEL_M2a:
			name = "M2a (PositiveSelection)";
			abbr = "M2a";
			nParameter = 4;
			supportsPSS = true;
			sb.append("seqtype=1;");
			sb.append("CodonFreq=2;");
			sb.append("model=0;");
			sb.append("icode=0;");
			sb.append("fix_kappa=0;");
			sb.append("kappa=2;");
			sb.append("fix_omega=0;");
			sb.append("omega=5;");
			sb.append("NSsites=2;");
			break;
		case MODEL_M1:
			name = "M1 (NearlyNeutral)";
			abbr = "M1";
			nParameter = 1;
			supportsPSS = false;
			sb.append("seqtype=1;");
			sb.append("CodonFreq=2;");
			sb.append("model=0;");
			sb.append("icode=0;");
			sb.append("fix_kappa=0;");
			sb.append("kappa=2;");
			sb.append("fix_omega=1;");
			sb.append("omega=1;");
			sb.append("NSsites=1;");
			break;
		case MODEL_M2:
			name = "M2 (PositiveSelection)";
			abbr = "M2";
			nParameter = 3;
			supportsPSS = true;
			sb.append("seqtype=1;");
			sb.append("CodonFreq=2;");
			sb.append("model=0;");
			sb.append("icode=0;");
			sb.append("fix_kappa=0;");
			sb.append("kappa=2;");
			sb.append("fix_omega=1;");
			sb.append("omega=1;");
			sb.append("NSsites=2;");
			break;
		case MODEL_M3:
			name = "M3 (discrete (3 categories))";
			abbr = "M3";
			nParameter = 5;
			supportsPSS = true;
			sb.append("seqtype=1;");
			sb.append("CodonFreq=2;");
			sb.append("model=0;");
			sb.append("icode=0;");
			sb.append("fix_kappa=0;");
			sb.append("kappa=2;");
			sb.append("fix_omega=0;");
			sb.append("omega=5;");
			sb.append("NSsites=3;");
			sb.append("ncatG=3;");
			break;
		case MODEL_M7:
			name = "M7 (beta (10 categories))";
			abbr = "M7";
			nParameter = 2;
			supportsPSS = false;
			sb.append("seqtype=1;");
			sb.append("CodonFreq=2;");
			sb.append("model=0;");
			sb.append("icode=0;");
			sb.append("fix_kappa=0;");
			sb.append("kappa=2;");
			sb.append("fix_omega=0;");
			sb.append("omega=5;");
			sb.append("NSsites=7;");
			sb.append("ncatG=10;");
			break;
		case MODEL_M8:
			name = "M8 (beta&w>1 (11 categories))";
			abbr = "M8";
			nParameter = 4;
			supportsPSS = true;
			sb.append("seqtype=1;");
			sb.append("CodonFreq=2;");
			sb.append("model=0;");
			sb.append("icode=0;");
			sb.append("fix_kappa=0;");
			sb.append("kappa=2;");
			sb.append("fix_omega=0;");
			sb.append("omega=5;");
			sb.append("NSsites=8;");
			sb.append("ncatG=10;");
			break;
		}
		settings = sb.toString();
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
		
		String[] tokens = settings.split(";");
		for(String token : tokens) {
			String[] tmp = token.split("=");
			sb.append(tmp[0]+" = "+tmp[1]+nl);
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
	
	public double get_w()
	{
		return _w;
	}

	public void set_w(double _w)
	{
		this._w = _w;
	}

	public String getAbbr()
	{
		return abbr;
	}

	public void setAbbr(String abbr)
	{
		this.abbr = abbr;
	}

	public double getDnDS()
	{
		return dnDS;
	}

	public void setDnDS(double dnDS)
	{
		this.dnDS = dnDS;
	}

	public double getLikelihood()
	{
		return likelihood;
	}

	public void setLikelihood(double likelihood)
	{
		this.likelihood = likelihood;
	}

	public int getModel()
	{
		return model;
	}

	public void setModel(int model)
	{
		this.model = model;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getNParameter()
	{
		return nParameter;
	}

	public void setNParameter(int parameter)
	{
		nParameter = parameter;
	}

	public double getP()
	{
		return p;
	}

	public void setP(double p)
	{
		this.p = p;
	}

	public double getP0()
	{
		return p0;
	}

	public void setP0(double p0)
	{
		this.p0 = p0;
	}

	public double getP1()
	{
		return p1;
	}

	public void setP1(double p1)
	{
		this.p1 = p1;
	}

	public double getP2()
	{
		return p2;
	}

	public void setP2(double p2)
	{
		this.p2 = p2;
	}

	public String getPss()
	{
		return pss;
	}

	public void setPss(String pss)
	{
		this.pss = pss;
	}

	public double getQ()
	{
		return q;
	}

	public void setQ(double q)
	{
		this.q = q;
	}

	public boolean isSupportsPSS()
	{
		return supportsPSS;
	}

	public void setSupportsPSS(boolean supportsPSS)
	{
		this.supportsPSS = supportsPSS;
	}

	public double getW()
	{
		return w;
	}

	public void setW(double w)
	{
		this.w = w;
	}

	public double getW0()
	{
		return w0;
	}

	public void setW0(double w0)
	{
		this.w0 = w0;
	}

	public double getW1()
	{
		return w1;
	}

	public void setW1(double w1)
	{
		this.w1 = w1;
	}

	public double getW2()
	{
		return w2;
	}

	public void setW2(double w2)
	{
		this.w2 = w2;
	}

	public String getSettings()
	{
		return settings;
	}

	public void setSettings(String settings)
	{
		this.settings = settings;
	}

	public Map<String, String> getSettingsMap()
	{
		return Util.stringToMap(settings, '=', ';');
	}

	public void setSettings(Map<String, String> map)
	{
		this.settings = Util.mapToString(map, '=', ';');
	}
	
}