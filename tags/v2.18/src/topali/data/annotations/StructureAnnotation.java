// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

public class StructureAnnotation extends Annotation
{

	char sign = '-';
	
	public StructureAnnotation() {
		super("Secondary Structure");
	}
	
	public StructureAnnotation(int pos, char sign) {
		this(pos, 1, sign);
	}
	
	public StructureAnnotation(int pos, int length, char sign) {
		this();
		this.sign = sign;
		super.pos = pos;
		super.length = length;
	}
}
