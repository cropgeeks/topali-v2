// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;


public class RaxPartition 
{

	public String indeces;
	public String name;
	public String model;
	public boolean dna;
	
	public RaxPartition() {
		
	}
	
	public RaxPartition(String indeces, String name, String model, boolean dna)
	{
		this.indeces = indeces;
		this.name = name;
		this.model = model;
		this.dna = dna;
	}
	
}
