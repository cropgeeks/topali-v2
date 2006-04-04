// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm2;

import java.util.*;

import topali.data.*;

/* Class designed to work out how best to parallize a PDM run over a cluster
 * given [n] cluster nodes (or CPUs), an alignment length, and a set window and
 * step size for the analysis.
 *
 * See documentation in "pdm partitions.jpg"
 */
class WindowChopperUpper
{
	// List to hold the regions created
	private LinkedList<RegionAnnotations.Region> list =
		new LinkedList<RegionAnnotations.Region>();
	
	// Window size
	private int w;
	// Step size
	private int s;
	// Alignment length
	private int length;
	
	WindowChopperUpper(SequenceSet ss, PDMResult result)
	{
		w = result.pdm_window;
		s = result.pdm_step;
		length = ss.getLength();
	}
	
	RegionAnnotations.Region[] getRegions(int nProcessors)
	{
		// Total number of windows that need to be PDMed
		int tW = (int) ((length - w) / s) + 1;
		System.out.println("tW=" + tW);

		// Number of windows per partition
		int wp = (int) ((tW / nProcessors) + 1);
		
		// Nuc position of the final window's end point
		int finalWinNuc = ((tW-1)*s) + w;
		
		
		int P = 1;
		while (true)
		{
			// Region's starting nucleotide
			int B = (P-1) * (wp*s) + 1;
			// Region's ending nucleotide
			int E = (B-1) + w + ((wp-1)*s);
			
			if (E >= finalWinNuc)
				E = finalWinNuc;
			
			// Create the region (and add it to the list)
			RegionAnnotations.Region r = new RegionAnnotations.Region(B, E);
			list.add(r);			
			
			// Either quit or continue
			if (E >= finalWinNuc)
				break;
			else			
				P++;
		}
		
		// Convert the list to an array and return
		return list.toArray(new RegionAnnotations.Region[] {});
	}
}