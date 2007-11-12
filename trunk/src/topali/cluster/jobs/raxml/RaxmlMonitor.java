// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.raxml;

import java.io.*;
import java.util.logging.Logger;

import topali.cluster.*;
import topali.cluster.jobs.raxml.analysis.RaxmlParser;
import topali.data.*;
import topali.fileio.Castor;
import topali.gui.TOPALi;

public class RaxmlMonitor
{
	private static  Logger logger = Logger.getLogger("topali.cluster.info-log");
	
	File jobDir;
	RaxmlResult result;
	
	public RaxmlMonitor(File jobDir) throws Exception {
		this.jobDir = jobDir;
		result = (RaxmlResult) Castor.unmarshall(new File(jobDir, "submit.xml"));
	}
	
	public JobStatus getPercentageComplete() throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe(jobDir.getName() + " - error.txt found");
			throw new Exception("RaxML error.txt");
		}
		
		int fin = 0;
		for(int i=1; i<=result.bootstrap+1; i++) {
			File runDir = new File(jobDir, "run"+i);
			File tmp = new File(runDir, "finished");
			if(tmp.exists())
				fin++;
			
			if (new File(runDir, "error.txt").exists())
			{
				logger.severe(jobDir.getName() + " - error.txt found for run "
						+ i);
				throw new Exception("RaxML error.txt (run " + i + ")");
			}
		}
		
		float progress;
		if(result.bootstrap==0 && fin==1)
			progress = 100;
		else 
			progress = (float)(fin)/(float)(result.bootstrap+1)*100f;

		return new JobStatus(progress, 0, "_status");
	}
	
	public RaxmlResult getResult() throws Exception
	{
		if(result.bootstrap==0) {
			File dir = new File(jobDir, "run1");
			result.setTreeStr(RaxmlParser.getTree(new File(dir, "RAxML_result.out.txt")));
			result.setLnl(RaxmlParser.getLikelihood(new File(dir, "RAxML_info.out.txt")));
		}
		
		else {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(jobDir, "trees.txt")));
			double bestLnl = 0;
			int bestLnlRun = 0;
			for(int i=1; i<=result.bootstrap+1; i++) {
				File dir = new File(jobDir, "run"+i);
				String tree = RaxmlParser.getTree(new File(dir, "RAxML_result.out.txt"));
				out.write(tree+"\n");
				if(i==1) {
					bestLnl = RaxmlParser.getLikelihood(new File(dir, "RAxML_info.out.txt"));
					bestLnlRun = 1;
				}
				else {
					double lnl = RaxmlParser.getLikelihood(new File(dir, "RAxML_info.out.txt"));
					if(lnl>bestLnl) {
						bestLnl = lnl;
						bestLnlRun = i;
					}
				}
			}
			out.close();
			
			out = new BufferedWriter(new FileWriter(new File(jobDir, "tree.txt")));
			File dir = new File(jobDir, "run"+bestLnlRun);
			out.write(RaxmlParser.getTree(new File(dir, "RAxML_result.out.txt")));
			out.close();
			
			RaxPartition p = result.partitions.get(result.partitions.size()-1);
			
			String[] cmds = new String[] {result.raxmlPath, "-f",  "b", "-m", getModel(result, p), "-q", "partitions", "-s", "seq", "-z", "trees.txt", "-t", "tree.txt", "-n", "bstree.txt"};
			if(TOPALi.debugJobs) {
				StringBuffer tmp = new StringBuffer();
				for(String s : cmds)
					tmp.append(s+" ");
				ClusterUtils.writeFile(new File(jobDir, "cmds.txt"), tmp.toString());
			}
			
			ProcessBuilder pb = new ProcessBuilder(cmds);
			pb.directory(jobDir);
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
			sc.start();

			try
			{
				proc.waitFor();
				proc.destroy();
			} catch (Exception e)
			{
				logger.warning("Problem running raxml\n"+e);
			}
			
			result.setTreeStr(RaxmlParser.getTree(new File(jobDir, "RAxML_bipartitions.bstree.txt")));
			result.setLnl(bestLnl);
		}
		
		result.info = getInfo();
		
		Castor.saveXML(result, new File(jobDir, "result.xml"));
		
		return result;
	}
	
	private String getModel(RaxmlResult res, RaxPartition p) {
		if(p.dna) {
			String model = "GTR"+res.rateHet;
			return model;
		}
		else {
			String model = "PROT"+res.rateHet;
			model += p.model.toUpperCase();
			if(p.model.toUpperCase().equals("BLOSUM"))
				model += "62";
			if(res.empFreq) {
				model+="F";
			}
			return model;
		}
	}
	
	private String getInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("Algorithm: RaxML\n");
		sb.append("Model: "+result.partitions.get(0).model+"\n");
		sb.append("Rate Heterogenity: "+result.rateHet+" \n");
		String tmp = "--";
		if(!result.partitions.get(0).dna) {
			tmp = ""+result.empFreq;
		}
		sb.append("Emp. frequencies: "+tmp+" \n");
		sb.append("Bootstraps: "+result.bootstrap+"\n");
		sb.append("Partitions:\n");
		for(RaxPartition p : result.partitions) {
			if(!p.dna)
				sb.append(p.model+", "+ p.name+" = "+p.indeces+"\n");
			else
				sb.append(p.name+" = "+p.indeces+"\n");
		}
		
		sb.append("\n\nApplication: RaxML (Version 2.2.3)\n");
		sb.append("A Stamatakis, 2006, RAxML-VI-HPC: Maximum Likelihood-based\n" +
				"Phylogenetic Analyses with Thousands of Taxa and Mixed Models,\n" +
				"Bioinformatics 22(21), pp 2688–2690");
		return sb.toString();
	}
}
