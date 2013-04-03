// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

public class SecStructureAnnotation extends Annotation
{

	public static final char LOOP = '.';
	public static final char PAIR1 = '(';
	public static final char PAIR2 = ')';
	
	char type = LOOP;
	
	public SecStructureAnnotation() {
		super();
	}
	
	public SecStructureAnnotation(int start, int end, char type) {
		super(start, end);
		this.type = type;
	}

	public SecStructureAnnotation(SecStructureAnnotation anno) {
		super(anno);
		this.type = anno.getType();
	}
	
	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}
	
	
}
