package topali.cluster.pdm;

import java.io.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class CollatePDM
{
	private File jobDir;
	private PDMResult result;
	
	public CollatePDM(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
		
//		String xml = ClusterUtils.readFile(new File(jobDir, "result.xml"));
//		result = Castor.getPDMResult(xml);
		result = (PDMResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
	
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
			throw new Exception("PDM error.txt");
		
		// Percentages are tracked by having one file for every 1% stored in a
		// directory called "percent"
		System.out.println("CollatePDM: " + new File(jobDir, "percent").listFiles().length);
		return new File(jobDir, "percent").listFiles().length;
	}
	
	public PDMResult getResult()
		throws Exception
	{
		return result;
	}
}