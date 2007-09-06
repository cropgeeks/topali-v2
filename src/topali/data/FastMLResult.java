// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.Serializable;
import java.util.Vector;

/**
 * Just a result wrapper around a alignment data
 * (The result of a fastml job is a new alignment containing the corresponding tree)
 */
public class FastMLResult extends AlignmentResult implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1107224363568642146L;

	public static String MODEL_DNA_JC = "mn";
	
	public static String MODEL_AA_JC = "ma";
	public static String MODEL_AA_JTT = "mj";
	public static String MODEL_AA_DAY = "md";
	public static String MODEL_AA_WAG = "mw";
	public static String MODEL_AA_MTREV = "mr";
	public static String MODEL_AA_CPREV = "mc";
	
	public String fastmlPath;
	
	public String model;
	public boolean gamma;
	
	//The tree fastml will be based on
	public String origTree;
	
	//The alignment, which contains the ancestral sequences
	public AlignmentData alignment;
	
	//Mapping between seq safenames[0] and original names[1]
	public String[][] seqNameMapping;
	
	public FastMLResult() {
		alignment = new AlignmentData();
		isRemote = false;
	}
	
	public void setCastorSeqNameMapping(String sn) {
		String[] tmp = sn.split("\\s+");
		seqNameMapping = new String[tmp.length][2];
		for(int i=0; i<tmp.length; i++) {
			String[] tmp2 = tmp[i].split(",");
			seqNameMapping[i][0] = tmp2[0];
			seqNameMapping[i][1] = tmp2[1];
		}
	}
	
	public String getCastorSeqNameMapping() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<seqNameMapping.length; i++) {
			sb.append(seqNameMapping[i][0]+","+seqNameMapping[i][1]+" ");
		}
		return sb.toString();
	}
	
	/**
	 * Restores the original seq names, if there is a seqNameMapping
	 */
	public void restoreSeqNames() {
		
		for(int i=0; i<seqNameMapping.length; i++) {
			Vector<Sequence> seqs = alignment.getSequenceSet().getSequences();
			for(Sequence seq : seqs) {
				if(seq.name.equals(seqNameMapping[i][0])) {
					seq.name = seqNameMapping[i][1];
				}
			}
		}
	}
}
