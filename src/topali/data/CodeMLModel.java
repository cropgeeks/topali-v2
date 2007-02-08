// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.Vector;

/*
 * Class that stores output data from a given (model's) codeML run.
 */
public class CodeMLModel
{
	public String runNumber = "";
	public String name = "";
	public String siteClass = "";
	
	public int params = -1;
	
	public float dnDS = -1;
	public float w = -1;
	public float p0 = -1, p1 = -1, p2 = -1;
	public float w0 = -1, w1 = -1, w2 = -1;
	
	public float p = -1, q = -1, _w = -1;
	
	public float likelihood = -1;
	
	public String pss = "";

	/**
		 * toString methode: creates a String representation of the object
		 * @return the String representation
		 * @author info.vancauwenberge.tostring plugin
	
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("CodeMLModel[");
			buffer.append("runNumber = ").append(runNumber);
			buffer.append(", name = ").append(name);
			buffer.append(", siteClass = ").append(siteClass);
			buffer.append(", params = ").append(params);
			buffer.append(", dnDS = ").append(dnDS);
			buffer.append(", w = ").append(w);
			buffer.append(", p0 = ").append(p0);
			buffer.append(", p1 = ").append(p1);
			buffer.append(", p2 = ").append(p2);
			buffer.append(", w0 = ").append(w0);
			buffer.append(", w1 = ").append(w1);
			buffer.append(", w2 = ").append(w2);
			buffer.append(", p = ").append(p);
			buffer.append(", q = ").append(q);
			buffer.append(", _w = ").append(_w);
			buffer.append(", likelihood = ").append(likelihood);
			buffer.append(", pss = ").append(pss);
			buffer.append("]");
			return buffer.toString();
		}
	
	
}