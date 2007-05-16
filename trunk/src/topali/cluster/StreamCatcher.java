// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import java.io.*;

// Basic class to handle reading from a stream so that TOPALi can execute
// external programs. Reads from a stream until it ends - programs will not run
// unless their output is read.
public class StreamCatcher extends Thread
{
	protected BufferedReader reader = null;
	protected StringBuffer output = null;
	protected boolean showOutput = false;
	
	public StreamCatcher(InputStream in, boolean showOutput) {
		this(in, showOutput, false);
	}
	
	public StreamCatcher(InputStream in, boolean showOutput, boolean keepOutput)
	{
		reader = new BufferedReader(new InputStreamReader(in));
		this.showOutput = showOutput;
		
		if(keepOutput) 
			output = new StringBuffer();
		
		start();
	}

	public void run()
	{
		try
		{
			String line = reader.readLine();

			while (line != null)
			{
				if (showOutput)
					System.out.println(line);

				if(output!=null) {
					output.append(line);
					output.append('\n');
				}
				
				line = reader.readLine();
			}
		} catch (Exception e)
		{
		}

		try
		{
			reader.close();
		} catch (IOException e)
		{
		}
	}
	
	public String getOutput() {
		if(output==null)
			return null;
		return output.toString();
	}
}