// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

/*
 * Class that stores output data from a given (model's) codeML run.
 */
public class CodeMLModel
{
	public String runNumber;
	public String name;
	public String siteClass;
	
	public int params;
	
	public float dnDS;
	public float w;
	public float p0, p1, p2;
	public float w0, w1, w2;
	
	public float p, q, _w;
	
	public float likelihood;
}