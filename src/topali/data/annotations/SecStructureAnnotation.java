// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

public class SecStructureAnnotation extends Annotation
{

	char sign = '-';
	
	public SecStructureAnnotation() {
		super();
	}
	
	public SecStructureAnnotation(int pos, int length, char sign) {
		super(pos, length);
		this.sign = sign;
	}
}
