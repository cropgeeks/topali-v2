// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.*;

import topali.cluster.AnalysisThread;
import topali.cluster.ClusterUtils;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class MrBayesAnalysis extends AnalysisThread
{

	private SequenceSet ss;

	private MBTreeResult result;

	int nGen = 100000;
	
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
		ss.save(new File(runDir, "mb.nex"), indices, Filters.NEX_B, true);

		// Add nexus commands to tell MrBayes what to do
		addNexusCommands();

		MrBayesProcess mb = new MrBayesProcess(runDir, result, nGen);
		mb.run();

		readTree();

		// Save final data back to drive where it can be retrieved
		Castor.saveXML(result, new File(runDir, "result.xml"));

		//ClusterUtils.emptyDirectory(wrkDir, true);
	}

	private void addNexusCommands() throws Exception
	{
		MrBayesCmdBuilder cmd = new MrBayesCmdBuilder(ss.isDNA());
		cmd.setNgen(nGen);
		
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

		String model = para.getModel();
		if(model.equals(SequenceSetParams.MODEL_AA_BLOSUM)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_BLOSUM);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_CPREV)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_CPREV);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_DAYHOFF)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_DAYHOFF);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_EQUALIN)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_EQUALIN);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_GTR)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_GTR);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_JONES)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_JONES);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_MTMAM)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_MTMAM);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_MTREV)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_MTREV);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_POISSON)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_POISSON);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_RTREV)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_RTREV);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_VT)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_VT);
		}
		else if(model.equals(SequenceSetParams.MODEL_AA_WAG)) {
			cmd.setAaModel(MrBayesCmdBuilder.AAMODEL_WAG);
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_JC)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_F81JC);
			cmd.prset = "prset statefreqpr=fixed(equal)";
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_F81)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_F81JC);
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_GTR)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_GTR);
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_HKY)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_HKY);
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_K3P)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_HKY);
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_K80)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_HKY);
			cmd.prset = "prset statefreqpr=fixed(equal)";
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_SIM)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_GTR);
			cmd.prset = "prset statefreqpr=fixed(equal)";
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_TIM)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_GTR);
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_TRN)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_HKY);
		}
		else if(model.equals(SequenceSetParams.MODEL_DNA_TVM)) {
			cmd.setDnaModel(MrBayesCmdBuilder.DNAMODEL_GTR);
		}
		
		if(para.isModelGamma())
			cmd.setRate(MrBayesCmdBuilder.RATE_GAMMA);
		if(para.isModelInv())
			cmd.setRate(MrBayesCmdBuilder.RATE_PROPINV);
		if(para.isModelGamma() && para.isModelInv())
			cmd.setRate(MrBayesCmdBuilder.RATE_INVGAMMA);
		
		 BufferedWriter out = new BufferedWriter(new FileWriter(new File(runDir,
		 "mb.nex"), true));
		 out.write("\n\n"+cmd.getCommands());
		 out.flush();
		 out.close();
		 
		 ((MBTreeResult)result).mbCmds = cmd.getCommands();
	}

	// private void addNexusCommands() throws Exception
	// {
	// BufferedWriter out = new BufferedWriter(new FileWriter(new File(wrkDir,
	// "mb.nex"), true));
	//
	//		
	// out.newLine();
	// out.newLine();
	// out.write("begin mrbayes;");
	// out.newLine();
	//
	// if(ss.isDNA()) {
	// out.write(" lset nst=6 rates=gamma;");
	// out.newLine();
	// out.write(" mcmc nruns=1 ngen=10000 samplefreq=10;");
	// out.newLine();
	// out.write(" sump burnin=1000;");
	// out.newLine();
	// out.write(" sumt burnin=1000;");
	// }
	// else {
	// //out.write("lset aamodel=jones mcmcp samplefreq=50 printfreq=50
	// nchains=2 startingtree=random mcmcp savebrlens=yes filename=testseq1c
	// mcmc ngen=20000 sump;");
	// out.write("prset aamodelpr=fixed(jones);");
	// out.write("mcmcp samplefreq=50 printfreq=50 nchains=2
	// startingtree=random;");
	// out.write("mcmcp savebrlens=yes;");
	// out.write("mcmc ngen=20000;");
	// out.write("sump;");
	// out.write("sumt;");
	// }
	//		
	// out.newLine();
	// out.write("end;");
	//
	// out.close();
	// }

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