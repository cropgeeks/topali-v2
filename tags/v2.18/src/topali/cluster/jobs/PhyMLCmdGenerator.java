// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import topali.data.models.*;

public class PhyMLCmdGenerator
{
	private static String CR = System.getProperty("line.separator");

	public static String getModelCmd(String seqFile, Model mod, boolean optTop, boolean optBranch, int bootstraps, String treeFile) {
		String cmd = seqFile+CR;
		
		//switch of alrt tests
		cmd+= "X"+CR;
		cmd+= "N"+CR;
		
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
			for(int i=0; i<model.getNSubRateGroups()-1; i++)
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
		
		if(bootstraps>0) {
			cmd += "B"+CR;
			cmd += ""+bootstraps+CR;
			cmd += "N"+CR;
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
