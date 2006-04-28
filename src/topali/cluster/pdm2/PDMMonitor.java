package topali.cluster.pdm2;

import java.io.*;
import java.util.logging.*;

import topali.data.*;
import topali.fileio.*;

public class PDMMonitor
{
	private static Logger logger = Logger.getLogger("topali.cluster");
	
	private File jobDir;
	
	public PDMMonitor(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
	}
	
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe("error.txt generated for " + jobDir.getPath());
			throw new Exception("PDM2 error.txt");
		}
		
		// Calculate a running percentage total for each sub-job directory
		int runs = 0, total = 0;
		
		File[] files = jobDir.listFiles();
		for (File f: files)
		{
			if (f.isDirectory() && f.getName().startsWith("run"))
			{
				runs++;
				
				if (new File(f, "error.txt").exists())
				{
					logger.severe("error.txt generated for " + jobDir.getPath() + " on run " + runs);
					throw new Exception("PDM2 error.txt in subjob " + runs);
				}
				
				// Check the percentage complete for this job
				try { total += new File(f, "percent").listFiles().length; }
				catch (Exception e) {}
			}
		}
		
		// Now work out the overal percentage
		System.out.println("runs=" + runs);
		System.out.println("total=" + total);
		return total / (float) runs;
	}
	
	public PDM2Result getResult()
		throws Exception
	{
		return (PDM2Result) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
	
	public static void main(String[] args)
		throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("out.txt")));
		
		File file = new File(args[0]);
		
		File[] dirs = file.listFiles();
		for (File f: dirs)
		{
			if (f.isDirectory() == false)
				continue;
			
			try {
			System.out.println("Reading " + f);
			BufferedReader in = new BufferedReader(new FileReader(new File(f, "out.xls")));
			String str = in.readLine();
			while (str != null)
			{
				out.write(str);
				out.newLine();
				
				str = in.readLine();
			}
			
			in.close();
			
			}
			catch (Exception e) { System.out.println(e); }
			
			out.newLine();
			
		}
		
		out.close();
	}
}