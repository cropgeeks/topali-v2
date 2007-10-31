// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.util.*;

import topali.data.MBPartition;
import topali.data.models.Model;

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
	
	public boolean dna = true;
	public int nruns = 2;
	public int ngen = 100000;
	public int sampleFreq = 10;
	public double burnin = 0.25;

	
	public String getCmds(Vector<MBPartition> parts) {
		StringBuffer sb = new StringBuffer();
		sb.append("begin mrbayes;\n\n");
	
		if(dna)
			sb.append("\t lset nucmodel= 4by4;\n");
		
		String tmp = "";
		for(MBPartition p : parts) {
			sb.append("\t charset "+p.name+" = "+p.indeces+";\n");
			tmp += p.name+",";
		}
		tmp = tmp.substring(0, tmp.length()-1);
		
		sb.append("\t partition parts = "+parts.size()+":"+tmp+";\n");
		sb.append("\t set partition=parts;\n");
		
		for(int i=1; i<parts.size()+1; i++) {
			Model mod = parts.get(i-1).model;
			
			String mbmod = "";
			String prset = null;
			
			if(dna) {
				if(mod.is("jc")) {
					mbmod = DNAMODEL_JC;
					prset = "\t prset applyto=("+i+") statefreqpr=fixed(equal);\n";
				}
				else if(mod.is("f81"))
					mbmod = DNAMODEL_F81;
				
				else if(mod.is("k80")) {
					mbmod = DNAMODEL_K80;
					prset = "\t prset applyto=("+i+") statefreqpr=fixed(equal);\n";
				}
				else if(mod.is("hky"))
					mbmod = DNAMODEL_HKY;
				
				else if(mod.is("sym")) {
					mbmod = DNAMODEL_SYM;
					prset = "\t prset applyto=("+i+") statefreqpr=fixed(equal);\n";
				}
				else if(mod.is("gtr"))
					mbmod = DNAMODEL_GTR;
				
				sb.append("\t lset applyto=("+i+") nst="+mbmod+";\n");
				if(prset!=null)
					sb.append(prset);
			}
			else {
				if(mod.is("jtt"))
					mbmod = AAMODEL_JONES;
				else if(mod.is("day"))
					mbmod = AAMODEL_DAYHOFF;
				else if(mod.is("mtrev"))
					mbmod = AAMODEL_MTREV;
				else if(mod.is("mtmam"))
					mbmod = AAMODEL_MTMAM;
				else if(mod.is("wag"))
					mbmod = AAMODEL_WAG;
				else if(mod.is("rtrev"))
					mbmod = AAMODEL_RTREV;
				else if(mod.is("cprev"))
					mbmod = AAMODEL_CPREV;
				else if(mod.is("vt"))
					mbmod = AAMODEL_VT;
				else if(mod.is("blosum"))
					mbmod = AAMODEL_BLOSUM;
				else if(mod.is("gtr"))
					mbmod = AAMODEL_GTR;
				
				sb.append("\t prset applyto=("+i+") aamodelpr=fixed("+mbmod+");\n");
			}
			
			if(mod.isGamma() && mod.isInv()) {
				sb.append("\t lset applyto=("+i+") rates=invgamma;\n");
			}
			else if(mod.isGamma()) {
				sb.append("\t lset applyto=("+i+") rates=gamma;\n");
			}
			else if(mod.isInv()) {
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
}
