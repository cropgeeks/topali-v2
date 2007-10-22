// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import topali.data.models.*;

public class PhyMLCmdGenerator
{
	private static String CR = System.getProperty("line.separator");

	public static String getModelCmd(String seqFile, Model mod, boolean optTop, boolean optBranch, String treeFile) {
		String cmd = seqFile+CR;
		
		if(mod instanceof DNAModel) {
			DNAModel model = (DNAModel)mod;
			
			//cmds for using custom model:
			// 4 x M
			cmd += "M"+CR+"M"+CR+"M"+CR+"M"+CR;
			
			//set substitution rate categories:
			// K + 012345
			cmd += "K"+CR;
			for(char c : model.getSubRateGroups())
				cmd += c;
			cmd += CR;
			//set each rate to 1.00
			for(int i=0; i<model.getNSubRateGroups(); i++)
				cmd += "1.00"+CR;
			//optimise rate parameters:
			cmd += "W"+CR;
			
			//set base frequencies if they are fixed (nBaseFreqGroups==1)
			//otherwise they are free to vary
			if(model.getNBaseFreqGroups()==1) {
				cmd += "F"+CR;
				//set them equally to 0.25
				cmd += "0.25"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR;
			}
		}
		
		else if(mod instanceof ProteinModel) {
			//switch to protein mode
			cmd += "D"+CR;
			
			if(mod.is("MTRev"))
				cmd += "M"+CR;
			
			else if(mod.is("WAG"))
				cmd += "M"+CR+"M"+CR;
			
			else if(mod.is("DCMut"))
				cmd += "M"+CR+"M"+CR+"M"+CR;
			
			else if(mod.is("RTRev"))
				cmd += "M"+CR+"M"+CR+"M"+CR+"M"+CR;
			
			else if(mod.is("CpRev"))
				cmd += "M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR;
			
			else if(mod.is("VT"))
				cmd += "M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR;
			
			else if(mod.is("Blosum"))
				cmd += "M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR;
			
			else if(mod.is("MTMam"))
				cmd += "M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR;
			
			else if(mod.is("Dayhoff"))
				cmd += "M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR;
			
		}
		
		//enable inv. sites
		if(mod.isInv()) {
			cmd += "V"+CR+"Y"+CR;
		}
		
		//enable gamma dist.
		if(mod.isGamma()) {
			cmd += "R"+CR+"A"+CR+"Y"+CR;
		}
		
		if(!optTop) {
			cmd += "O"+CR;
			if(!optBranch)
				cmd += "L"+CR;
		}
		
		if(treeFile!=null) {
			cmd += "U"+CR;
			cmd += treeFile+CR;
		}
		
		//add the final Y
		cmd += "Y"+CR;
		
		return cmd;
	}
}

/**
if(mod.is("JC")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR;
			}
			else if(mod.is("F81")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR;
			}
			else if(mod.is("K80")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"M"+CR+"T"+CR+"Y"+CR;
			}
			else if(mod.is("HKY")) {
				cmd = "T"+CR+"Y"+CR+"E"+CR;
			}
			else if(mod.is("TRNef")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"F"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"K"+CR+"010020"+CR+"1.00"+CR+"1.00"+CR+"W"+CR;
			}
			else if(mod.is("TrN")) {
				cmd = "M"+CR+"M"+CR+"E"+CR+"T"+CR+"Y"+CR;
			}
			else if(mod.is("K81")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"F"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"K"+CR+"012210"+CR+"1.00"+CR+"1.00"+CR+"W"+CR;
			}
			else if(mod.is("K81uf")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"K"+CR+"012210"+CR+"1.00"+CR+"1.00"+CR+"W"+CR;
			}
			else if(mod.is("TIMef")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"F"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"K"+CR+"012230"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"W"+CR;
			}
			else if(mod.is("TIM")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"K"+CR+"012230"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"W"+CR;
			}
			else if(mod.is("TVMef")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"F"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"K"+CR+"012314"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"W"+CR;
			}
			else if(mod.is("TVM")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"K"+CR+"012314"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"W"+CR;
			}
			else if(mod.is("SYM")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"M"+CR+"F"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"0.25"+CR+"K"+CR+"012345"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"1.00"+CR+"W"+CR;
			}
			else if(mod.is("GTR")) {
				cmd = "M"+CR+"M"+CR+"M"+CR+"E"+CR+"Y"+CR;
			}
			
			**/
