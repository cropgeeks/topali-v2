// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml.analysis;

import java.io.File;
import java.util.LinkedList;

import topali.cluster.AnalysisThread;
import topali.cluster.jobs.phyml.*;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class PhymlAnalysis extends AnalysisThread
{

	PhymlResult result;
	SequenceSet ss;
	
	//If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new PhymlAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	public PhymlAnalysis(File runDir)
	{
		super(runDir);
	}
	
	@Override
	public void runAnalysis() throws Exception
	{
		File resultFile = new File(runDir, "submit.xml");
		File ssFile = new File(runDir, "ss.xml");
		result = (PhymlResult)Castor.unmarshall(resultFile);
		ss = (SequenceSet)Castor.unmarshall(ssFile);
		
		PhymlProcess proc = new PhymlProcess(runDir, result);
		proc.run();
		
		result = PhymlParser.parse(new File(runDir, "seq_phyml_tree.txt"), new File(runDir, "seq_phyml_stat.txt"), result);
		result.info = treeInfo();
		Castor.saveXML(result, new File(runDir, "result.xml"));
	}
	
	private String treeInfo() {
		SequenceSetParams para = ss.getParams();
		
		StringBuffer sb = new StringBuffer();
		 sb.append("Sub. Model: "+para.getModel().getName()+"\n");
		 sb.append("Rate Model: ");
		 
		 if(!(para.getModel().isGamma() || para.getModel().isInv()))
			 sb.append("Uniform\n");
		 if(para.getModel().isGamma() && para.getModel().isInv())
			 sb.append("Gamma + Inv. sites (est. parameters)\n");
		 else 
			 {
			 if(para.getModel().isGamma())
				 sb.append("Gamma (est. parameters)\n");
			 if(para.getModel().isInv())
				 sb.append("Inv. sites (est. parameters)\n");
			 }
		 
		 sb.append("Algorithm: Maximum Likelihood (PhyML)\n");
		 sb.append("Bootstrap runs: "+result.bootstrap+"\n");
		 sb.append("Optimize Topology: true\n");
		 sb.append("Optimize branch lengths/rate para.: true\n\n");
		 
		 sb.append("\n\nApplication: PhyML-aLRT (Version 2.4.5)\n");
		 sb.append("M. Anisimova, O. Gascuel (2006), Approximate likelihood ratio\n" +
		 		"test for branchs: A fast, accurate and powerful alternative,\n" +
		 		"Systematic Biology, 55(4), pp 539-552.\n");
		 sb.append("Guindon S, Gascuel O. (2003), A simple, fast, and accurate\n" +
		 		"algorithm to estimate large phylogenies by maximum likelihood,\n" +
		 		"Systematic Biology. 52(5) pp 696-704. ");
		 
		 return sb.toString();
	}
}
