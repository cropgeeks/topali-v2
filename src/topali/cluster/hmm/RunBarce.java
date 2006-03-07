package topali.cluster.hmm;

import java.io.*;

import topali.cluster.*;
import topali.data.*;

class RunBarce
{
	private HMMResult result;
	private File wrkDir, jobDir;
	
	RunBarce(HMMResult result, File wrkDir, File jobDir)
	{
		this.result = result;
		this.wrkDir = wrkDir;
		this.jobDir = jobDir;
	}
	
	void runBarce()
		throws Exception
	{
		ProcessBuilder pb = new ProcessBuilder(result.barcePath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);
		
		Process proc = pb.start();
		
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
			proc.getOutputStream()));
			
		// Read output from barce
		new BarceCatcher(proc.getInputStream(), false);		
		
		// Send barce all its settings!
		
		// Model settings
		writer.println("1");
				
		if (result.hmm_model.equals("JC+gaps"))
			writer.println("m");
		else if (result.hmm_model.equals("K2P+gaps"))
		{
			writer.println("m");
			writer.println("m");
		}
		else if (result.hmm_model.equals("F81+gaps"))
		{
			writer.println("m");
			writer.println("m");
			writer.println("m");
		}
		
		if (result.hmm_initial.equals("No"))
		{
			writer.println("e");
		
			writer.println("f");
			writer.println("y");
			writer.println(result.hmm_freq_est_1 + " "
				+ result.hmm_freq_est_2 + " "
				+ result.hmm_freq_est_3 + " "
				+ result.hmm_freq_est_4);
		}
		
		if (result.hmm_transition.equals("No"))
			writer.println("r");
		
//			writer.println("p");
//			writer.println(result.hmm_freq_1 + " " + result.hmm_freq_2 + " "
//				+ result.hmm_freq_3);
		
		writer.println("d");
		writer.println(result.hmm_difficulty);
		
		////////////////////////////
//		if (result.hmm_use_mosaic)
			writer.println("j");
		////////////////////////////
		
		writer.println("x");
		writer.flush();
		
		
		// Run settings
		writer.println("2");
		
		writer.println("b");
		writer.println(result.hmm_burn);
		
		writer.println("n");
		writer.println(result.hmm_points);
		
		writer.println("i");
		writer.println(result.hmm_thinning);
		
		writer.println("c");
		writer.println(result.hmm_tuning);
		
		if (result.hmm_lambda.equals("No"))
			writer.println("w");
		else
		{
			if (result.hmm_annealing.equals("PAR"))
				writer.println("q");
			else if (result.hmm_annealing.equals("PROB"))
			{
				writer.println("q");
				writer.println("q");
			}
		}
		
		if (result.hmm_station.equals("No"))
			writer.println("u");
		
		if (result.hmm_update.equals("No"))
			writer.println("a");
		
		writer.println("o");
		writer.println(result.hmm_branch);
		
		writer.println("x");
		writer.flush();
		
		
		writer.println("y");
		writer.flush();
		writer.close();

		System.out.println("ALL SETTINGS SENT");

		try { proc.waitFor(); }
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	// This is an extension of the normal StreamCatcher that deals with Barce's
	// specific output, in particular, reading the numbers it prints out in
	// order to try and determine how far through the run (percentage complete)
	// Barce has reached.
	class BarceCatcher extends StreamCatcher
	{
		BarceCatcher(InputStream in, boolean showOutput)
			{ super(in, showOutput); }
		
		public void run()
		{
			BufferedWriter out = null;
			File percentDir = new File(jobDir, "percent");
			
			int read = 0;
			int percent = 0;
			
			try
			{
				String line = reader.readLine();
										
				while (line != null)
				{
					line = reader.readLine();
					
					if (showOutput)
						System.out.println(line);
					
					// If "::END" is read, then we know it's 100% complete
					if (line.equals("::END"))
						read = 100;
					
					if (percent != 100 && line.startsWith("p="))
						read = Integer.parseInt(line.substring(2));
					
					if (read != percent)
					{						
						// Create a file for each difference
						for (int i = read; i > percent; i--)
							new File(percentDir, "p" + i).createNewFile();
						
						percent = read;
					}
				}
			}
			catch (Exception e) {}
			
			try { reader.close(); }
			catch (IOException e) {}
		}
	}
}