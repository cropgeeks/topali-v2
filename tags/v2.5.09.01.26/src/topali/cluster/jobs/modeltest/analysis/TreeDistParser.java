// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest.analysis;

import java.io.*;

public class TreeDistParser
{

	public int[][] parse(File file) throws Exception {
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		String lastLine = null;
		while((line=in.readLine())!=null){
			lastLine = line;
		}
		in.close();
		
		String[] t = lastLine.split("\\s+");
		int x = Integer.parseInt(t[1]);
		int y = Integer.parseInt(t[0]) - x;
		
		int stop = x*y-1;
		int count = 0;
		int[][] result = new int[x][y];
		in = new BufferedReader(new FileReader(file));
		while((line=in.readLine())!=null) {
			t = line.split("\\s+");
			int i = (Integer.parseInt(t[0])-1);
			int j = (Integer.parseInt(t[1])-x-1);
			int dist = Integer.parseInt(t[2]);
			result[i][j] = dist;
			count++;
			
			if(count>stop)
				break;
		}
		
		return result;
	}
}
