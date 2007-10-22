// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml;

import java.io.File;
import java.util.LinkedList;

import topali.cluster.AnalysisThread;
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
	PhymlAnalysis(File runDir)
	{
		super(runDir);
	}
	
	@Override
	public void runAnalysis() throws Exception
	{
//		 Read the SubstitutionModel
		result = (PhymlResult) Castor
				.unmarshall(new File(runDir, "submit.xml"));
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(runDir, "ss.xml"));

		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(runDir, "seq"), indices, result.getPartitionStart(), result.getPartitionEnd(), Filters.PHY_I, true);

		result.phymlParameters = determinePhymlParameters(result, ss.getParams());
		result.info = treeInfo();
		
		PhymlProcess pp = new PhymlProcess(runDir, result);
		pp.run();

		result = PhymlParser.parse(new File(runDir, "seq_phyml_tree.txt"), result); 

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
		 sb.append("Optimize Topology: "+result.optTopology+"\n");
		 sb.append("Optimize branch lengths/rate para.: "+result.optBranchPara+"\n\n");
		 
		 sb.append("\n\nApplication: PhyML (Version 2.4.4)\n");
		 sb.append("Guindon S, Gascuel O. (2003), A simple, fast, and accurate\n" +
		 		"algorithm to estimate large phylogenies by maximum likelihood,\n" +
		 		"Systematic Biology. 52(5) pp 696-704. ");
		 
		 return sb.toString();
	}
	
	private String[] determinePhymlParameters(PhymlResult res, SequenceSetParams para) {
		LinkedList<String> list = new LinkedList<String>();
		
		//path
		list.add(result.phymlPath);
		//sequence file
		list.add("seq");
		//sequence type
		if(para.isDNA())
			list.add("0");
		else
			list.add("1");
		//sequence file format (i = interleaved)
		list.add("i");
		//number of datasets
		list.add("1");
		//bootstrap
		list.add(""+res.bootstrap);
		//model
		String model = "GTR";
		if(para.getModel().is("blosum"))
			model = "Blosum62";
		if(para.getModel().is("cprev"))
			model = "CpREV";
		if(para.getModel().is("day"))
			model = "Dayhoff";
		if(para.getModel().is("gtr"))
			model = "GTR";
		if(para.getModel().is("jtt"))
			model = "JTT";
		if(para.getModel().is("mtmam"))
			model = "MtMam";
		if(para.getModel().is("mtrev"))
			model = "mtREV";
		if(para.getModel().is("rtrev"))
			model = "RtREV";
		if(para.getModel().is("vt"))
			model = "VT";
		if(para.getModel().is("wag"))
			model = "WAG";
		
		if(para.getModel().is("jc"))
			model = "JC69";
		if(para.getModel().is("k80"))
			model = "K80";
		if(para.getModel().is("f81"))
			model = "F81";
		if(para.getModel().is("f84"))
			model = "F84";
		if(para.getModel().is("hky"))
			model = "HKY85";
		if(para.getModel().is("trn"))
			model = "TN93";
		if(para.getModel().is("gtr"))
			model = "GTR";
		
		list.add(model);
		//ts/tv
		if(para.isDNA())
			list.add("2");
		//inv. sites
		if(para.getModel().isInv())
			list.add("e");
		else
			list.add("0");
		//categories
		if(para.getModel().isGamma())
			list.add("4");
		else
			list.add("1");
		//gamma 
		list.add("e");
		//starting tree
		list.add("BIONJ");
		//optimise topology
		if(result.optTopology)
			list.add("y");
		else
			list.add("n");
		//optimise branch lengths and rate para.
		if(result.optBranchPara)
			list.add("y");
		else
			list.add("n");
		
		String[] result = new String[list.size()];
		return list.toArray(result);
	}
}
