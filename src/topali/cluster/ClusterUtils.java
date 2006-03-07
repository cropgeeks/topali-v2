// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import topali.data.*;

import java.io.*;

public class ClusterUtils
{
	public static boolean isWindows =
		System.getProperty("os.name").startsWith("Windows");
	
	private static String sep = System.getProperty("line.separator");
	
	// Creates a file handle to a temp directory for working on an analysis,
	// based on the actual jobID and unitID of a particular run
	public static File getWorkingDirectory(AnalysisResult result, String jobID, String unitID)
	{
		File wrkDir = new File(result.tmpDir, jobID + "_" + unitID);		
		wrkDir.mkdirs();
		
		return wrkDir;
	}
	
	// Empties, then (optionally) deletes the given directory
	// TODO: an analysis should empty its data directory after it is finished,
	// in addition to its working directory (that this method empties)
	public static void emptyDirectory(File dir, boolean delete)
	{
		File[] files = dir.listFiles();
		for (File f: files)
		{
			if (f.isDirectory())
				emptyDirectory(f, delete);
			f.delete();
		}
		
		if (delete)
			dir.delete();
	}
	
	public static void writeError(File filename, Exception exception)
	{
		System.out.println("Error information written to " + filename);
		
		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(new FileWriter(filename));
//			out.write(exception.toString());
			out.newLine();
			exception.printStackTrace(new PrintWriter(out));
		}
		catch (Exception e) {}
		
		try { if (out != null) out.close(); }
		catch (Exception e) {}
	}
	
	// Reads the contents of the given file and returns it as a string
	public static String readFile(File filename)
		throws Exception
	{
		BufferedReader in = new BufferedReader(new FileReader(filename));
		StringBuffer sb = new StringBuffer((int)filename.length());
		
		String str = in.readLine();
		while (str != null)
		{
			sb.append(str + sep);
			str = in.readLine();
		}
		
		return sb.toString();
	}
}
