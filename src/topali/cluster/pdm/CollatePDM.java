package topali.cluster.pdm;

import java.io.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class CollatePDM
{
	private File jobDir;
	
	public CollatePDM(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
	}
	
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
			throw new Exception("PDM error.txt");
		
		// Percentages are tracked by having one file for every 1% stored in a
		// directory called "percent"
		try { return new File(jobDir, "percent").listFiles().length; }
		catch (Exception e)	{
			return 0;
		}
	}
	
	public PDMResult getResult()
		throws Exception
	{
		return (PDMResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}