// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.*;

import topali.cluster.AnalysisThread;
import topali.data.*;
import topali.data.models.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class MrBayesAnalysis extends AnalysisThread
{

	public static final String VERSION = "3.1";
	
	private SequenceSet ss;

	private MBTreeResult result;
	
	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new MrBayesAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	MrBayesAnalysis(File runDir)
	{
		super(runDir);
	}

	@Override
	public void runAnalysis() throws Exception
	{
		// Read the MBTreeResult
		result = (MBTreeResult) Castor
				.unmarshall(new File(runDir, "submit.xml"));
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(runDir, "ss.xml"));

		// Temporary working directory
//		wrkDir = ClusterUtils.getWorkingDirectory(result, runDir.getName(),
//				"mrbayes");

		// We need to save out the SequenceSet for MrBayes to read, ensuring
		// that only the sequences meant to be processed are saved
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		ss.save(new File(runDir, "mb.nex"), indices, result.getPartitionStart(), result.getPartitionEnd(), Filters.NEX_B, true);

		// Add nexus commands to tell MrBayes what to do
		addNexusCommands();

		MrBayesProcess mb = new MrBayesProcess(runDir, result);
		mb.run();

		readTree();

		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));

		//ClusterUtils.emptyDirectory(wrkDir, true);
	}

	private void addNexusCommands() throws Exception
	{
		MrBayesCmdBuilder cmd = new MrBayesCmdBuilder(ss.isDNA());
		cmd.setCDNA(result.isCDNA);
		cmd.setNgen(result.nGen);
		cmd.setSampleFreq(result.sampleFreq);
		cmd.setBurnin(result.burnin);
		
		SequenceSetParams para = ss.getParams();
		String gCode = para.getGeneticCode();
		if (gCode.equals(SequenceSetParams.GENETICCODE_UNIVERSAL))
		{
			cmd.setCode(MrBayesCmdBuilder.CODE_UNIVERSAL);
		} else if (gCode.equals(SequenceSetParams.GENETICCODE_CILIATES))
		{
			cmd.setCode(MrBayesCmdBuilder.CODE_CILIATES);
		} else if (gCode.equals(SequenceSetParams.GENETICCODE_METMT))
		{
			cmd.setCode(MrBayesCmdBuilder.CODE_METMT);
		} else if (gCode.equals(SequenceSetParams.GENETICCODE_MYCOPLASMA))
		{
			cmd.setCode(MrBayesCmdBuilder.CODE_MYCOPLASMA);
		} else if (gCode.equals(SequenceSetParams.GENETICCODE_VERTMT))
		{
			cmd.setCode(MrBayesCmdBuilder.CODE_VERTMT);
		} else if (gCode.equals(SequenceSetParams.GENETICCODE_YEAST))
		{
			cmd.setCode(MrBayesCmdBuilder.CODE_YEAST);
		}

		Model model = para.getModel();
		if(model.is("blossum")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_BLOSUM);
		}
		else if(model.is("cprev")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_CPREV);
		}
		else if(model.is("day")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_DAYHOFF);
		}
		else if(model.is("equalin")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_EQUALIN);
		}
		else if(model.is("gtr") && (model instanceof ProteinModel)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_GTR);
		}
		else if(model.is("jtt")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_JONES);
		}
		else if(model.is("mtmam")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_MTMAM);
		}
		else if(model.is("mtrev")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_MTREV);
		}
		else if(model.is("poisson")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_POISSON);
		}
		else if(model.is("rtrev")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_RTREV);
		}
		else if(model.is("vt")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_VT);
		}
		else if(model.is("wag")) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_WAG);
		}
		else if(model.is("jc")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_JC, true);
		}
		else if(model.is("f81")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_F81JC, false);
		}
		else if(model.is("gtr") && (model instanceof DNAModel)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_GTR, false);
		}
		else if(model.is("hky")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_HKY, false);
		}
		else if(model.is("k81")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_HKY, false);
		}
		else if(model.is("k80")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_K80, true);
		}
		else if(model.is("sym")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_SYM, true);
		}
		else if(model.is("tim")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_GTR, false);
		}
		else if(model.is("trn")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_HKY, false);
		}
		else if(model.is("tvm")) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_GTR, false);
		}
		
		if(para.getModel().isGamma())
			cmd.setRate(MrBayesCmdBuilder.RATE_GAMMA);
		if(para.getModel().isInv())
			cmd.setRate(MrBayesCmdBuilder.RATE_PROPINV);
		if(para.getModel().isGamma() && para.getModel().isInv())
			cmd.setRate(MrBayesCmdBuilder.RATE_INVGAMMA);
		
		 BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
		 "mb.nex"), true));
		 out.write("\n\n"+cmd.getCommands());
		 out.flush();
		 out.close();
		 
		 StringBuffer sb = new StringBuffer();
		 if(!result.isCDNA) {
			 sb.append("Genetic Code: "+para.getGeneticCode()+"\n");
			 sb.append("Sub. Model: "+para.getModel().getName()+"\n");
			 sb.append("Rate Model: ");
			 if(!(para.getModel().isGamma() || para.getModel().isInv()))
				 sb.append("Uniform\n");
			 if(para.getModel().isGamma() && para.getModel().isInv())
				 sb.append("Gamma + Inv. sites\n");
			 else 
				 {
				 if(para.getModel().isGamma())
					 sb.append("Gamma\n");
				 if(para.getModel().isInv())
					 sb.append("Inv. sites\n");
				 }
		 }
		 else {
			 sb.append("Model: cDNA\n");
		 }
		 sb.append("Algorithm: MrBayes\n");
		 sb.append("Generations: "+result.nGen+"\n");
		 sb.append("Sample Freq.: "+result.sampleFreq+"\n");
		 sb.append("Burnin: "+((int)(result.burnin*100))+"%\n\n");
		 sb.append("MrBayes Commands:\n");
		 sb.append(cmd.getCommands());
		 
		 sb.append("\n\nApplication: MrBayes (Version 3.1.1)\n");
		 sb.append("F Ronquist, JP Huelsenbeck, 2003, MrBayes 3: Bayesian phylogenetic\n" +
					"inference under mixed models, Bioinformatics, 19(12), pp 1572-1574");
			
		 result.info = sb.toString();
		 result.nexusCommands = cmd.getCommands();
	}

	private void readTree() throws Exception
	{

		File mbFile = new File(runDir, "mb.nex.con");
		if (mbFile.exists() == false)
			throw new Exception("MrBayes did not create a tree file");

		String treeStr = null;

		BufferedReader in = new BufferedReader(new FileReader(mbFile));
		String str = in.readLine();
		while (str != null)
		{
			if (str.startsWith("   tree"))
			{
				treeStr = str.substring(str.indexOf("=") + 2);
				break;
			}

			str = in.readLine();
		}

		in.close();

		if (treeStr == null)
			throw new Exception("No tree found in mb.nex.con");

		// Update the result
		result.setTreeStr(treeStr);

		// TODO: Is this worth it (other than a percentage tracker)?
		// And write the data to tree.txt too
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
				"tree.txt")));
		out.write(treeStr);
		out.close();
	}

}
