// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class CMLHypothesis
{
	static String nl = System.getProperty("line.separator");
	
	public int model;
	public String tree;
	public String settings;
	
	public double likelihood;
	public String omegaTree;
	
	public CMLHypothesis() {
		StringBuffer set = new StringBuffer();
		set.append("seqfile = seq.phy" + nl);
		set.append("treefile = tree.txt" + nl);
		set.append("outfile = results.txt" + nl);
		set.append("noisy = 9" + nl);
		set.append("verbose = 0" + nl);
		set.append("runmode = 0" + nl);
		set.append("seqtype = 1" + nl);
		set.append("CodonFreq = 2" + nl);
		set.append("NSsites = 0" + nl);
		set.append("icode = 0" + nl);
		set.append("fix_kappa = 0" + nl);
		set.append("kappa = 2" + nl);
		set.append("fix_omega = 0" + nl);
		set.append("omega = 0.2" + nl);
		
		settings = set.toString();
	}

	public String getCTL() {
		String settings = this.settings;
		settings += "model = "+model+" "+nl;
		return settings;
	}
	
	public String getNoBranchLengthTree() {
		String result = tree.replaceAll(":\\d+.\\d+", "");
		return result;
	}
}
