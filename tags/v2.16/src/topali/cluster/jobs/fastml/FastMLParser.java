// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.fastml;

import java.io.*;

import org.apache.log4j.Logger;

import topali.cluster.JobStatus;
import topali.data.*;
import topali.fileio.AlignmentLoadException;

public class FastMLParser
{
	 static Logger log = Logger.getLogger(FastMLParser.class);
	
	public static FastMLResult parseTree(File file, FastMLResult result) {
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			StringBuffer sb = new StringBuffer();
			String line = null;
			while((line=reader.readLine())!=null)
				sb.append(line);
			
			TreeResult tree = new TreeResult(sb.toString());
			tree.guiName = "Ancestral sequence tree";
			tree.status = JobStatus.COMPLETED;
			result.alignment.addResult(tree);
			
		} catch (Exception e)
		{
			log.warn("Could not read tree file!", e);
		} 
		
		return result;
	}
	
	public static FastMLResult parseSeq(File file, FastMLResult result) {
		try
		{
			SequenceSet ss = new SequenceSet(file, true);
			for(Sequence s : ss.getSequences()) {
				s.setSequence(s.getSequence().replaceAll("\\*", "-"));
			}
			result.alignment.setSequenceSet(ss);
		} catch (AlignmentLoadException e)
		{
			log.warn("Could not read sequences!", e);
		}
		return result;
	}
}
