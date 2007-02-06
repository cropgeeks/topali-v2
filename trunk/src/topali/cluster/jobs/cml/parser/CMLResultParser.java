package topali.cluster.jobs.cml.parser;

import java.util.LinkedList;
import java.util.List;

import topali.data.*;
import topali.cluster.jobs.cml.Models;

public abstract class CMLResultParser
{
	protected CodeMLModel model = new CodeMLModel();
	
//	double dnds, w, p0, p1, p2, w0, w1, w2, p, q, _w, lnl;
	List<String> pss;
	
	public CMLResultParser() {
		pss = new LinkedList<String>();
	}

	public static CMLResultParser getParser(int model) {
		CMLResultParser parser = null;
		
		switch(model) {
		case Models.MODEL_M0: parser = new Model0Parser(); break;
		case Models.MODEL_M1a: parser = new Model1aParser(); break;
		case Models.MODEL_M2a: parser = new Model2aParser(); break;
		case Models.MODEL_M3: parser = new Model3Parser(); break;
		case Models.MODEL_M7: parser = new Model7Parser(); break;
		case Models.MODEL_M8: parser = new Model8Parser(); break;
		}
		
		if(parser==null)
			throw new RuntimeException("Unknown Model or Parser not implemented yet!");
		
		return parser;
	}
	
	public abstract void parse(String resultFile, String rstFile);

	public CodeMLModel getModelResult()
		{ return model; }

/*	public double getDnds() {
		return dnds;
	}
	
	public double get_w() {
		return _w;
	}

	public double getLnl() {
		return lnl;
	}

	public double getP() {
		return p;
	}

	public double getP0() {
		return p0;
	}

	public double getP1() {
		return p1;
	}

	public double getP2() {
		return p2;
	}

	public List<String> getPss() {
		return pss;
	}

	public double getQ() {
		return q;
	}

	public double getW() {
		return w;
	}

	public double getW0() {
		return w0;
	}

	public double getW1() {
		return w1;
	}

	public double getW2() {
		return w2;
	}
*/
	/**
		 * toString methode: creates a String representation of the object
		 * @return the String representation
		 * @author info.vancauwenberge.tostring plugin
	
		 */
/*		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("CMLResultParser[");
			buffer.append(" dnds = ").append(dnds);
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
			buffer.append(", lnl = ").append(lnl);
			buffer.append(", pss = ").append(pss);
			buffer.append("]");
			return buffer.toString();
		}
*/
	
}
