// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

import topali.data.Sequence;

public class RecombinantAnnotation extends Annotation
{
	public RecombinantAnnotation() {
		super("Recombinants");
	}
	
	public RecombinantAnnotation(int pos, int length, Sequence parent) {
		this();
		super.pos = pos;
		super.length = length;
		super.link = parent.safeName;
	}
	
}
