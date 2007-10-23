// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.util.*;

public class MBCmdBuilder
{
	public static final String DNAMODEL_JC = "1";
	public static final String DNAMODEL_F81 = "1";
	public static final String DNAMODEL_K80 = "2";
	public static final String DNAMODEL_HKY = "2";
	public static final String DNAMODEL_SYM = "6";
	public static final String DNAMODEL_GTR = "6";
	
	public static final String AAMODEL_JONES = "jones";
	public static final String AAMODEL_DAYHOFF = "dayhoff";
	public static final String AAMODEL_MTREV = "mtrev";
	public static final String AAMODEL_MTMAM = "mtmam";
	public static final String AAMODEL_WAG = "wag";
	public static final String AAMODEL_RTREV = "rtrev";
	public static final String AAMODEL_CPREV = "cprev";
	public static final String AAMODEL_VT = "vt";
	public static final String AAMODEL_BLOSUM = "blosum";
	public static final String AAMODEL_GTR = "gtr";
	
	public List<Partition> parts = new ArrayList<Partition>();
	public boolean dna = true;
	public int nruns = 2;
	public int ngen = 100000;
	public int sampleFreq = 10;
	public double burnin = 0.25;
	
	public String getCmds() {
		StringBuffer sb = new StringBuffer();
		sb.append("begin mrbayes;\n\n");
	
		if(dna)
			sb.append("\t lset nucmodel= 4by4;\n");
		
		String tmp = "";
		for(Partition p : parts) {
			sb.append("\t charset "+p.name+" = "+p.indeces+";\n");
			tmp += p.name+",";
		}
		tmp = tmp.substring(0, tmp.length()-1);
		
		sb.append("\t partition parts = "+parts.size()+":"+tmp+";\n");
		sb.append("\t set partition=parts;\n");
		
		for(int i=1; i<parts.size()+1; i++) {
			Partition p = parts.get(i-1);
			if(dna)
				sb.append("\t lset applyto=("+i+") nst="+p.model+";\n");
			else
				sb.append("\t prset applyto=("+i+") aamodelpr=fixed("+p.model+");\n");
			
			if(p.gamma && p.inv) {
				sb.append("\t lset applyto=("+i+") rates=invgamma;\n");
			}
			else if(p.gamma) {
				sb.append("\t lset applyto=("+i+") rates=gamma;\n");
			}
			else if(p.inv) {
				sb.append("\t lset applyto=("+i+") rates=propinv;\n");
			}
			else {
				sb.append("\t lset applyto=("+i+") rates=equal;\n");
			}
		}
		sb.append("\t lset applyto=(all) ngammacat=4;\n");
		sb.append("\t lset applyto=(all) nbetacat=5;\n");
		sb.append("\t prset applyto=(all) ratepr=variable;\n");
		
		sb.append("\n\t mcmc nruns="+nruns+" ngen=" + ngen + " samplefreq=" + sampleFreq+ ";\n");
		int burn = (int) ((ngen / sampleFreq) * burnin);
		sb.append("\t sump burnin=" + burn + ";\n");
		sb.append("\t sumt burnin=" + burn + ";\n\n");
		sb.append("end;\n");
		return sb.toString();
	}
	
	class Partition {
		String indeces;
		String model;
		String name;
		boolean gamma;
		boolean inv;
		
		public Partition(String indeces, String name, String model, boolean gamma, boolean inv)
		{
			this.indeces = indeces;
			this.model = model;
			this.gamma = gamma;
			this.inv = inv;
		}	
	}
}
