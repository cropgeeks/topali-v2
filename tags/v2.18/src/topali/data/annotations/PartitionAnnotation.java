// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;


public class PartitionAnnotation extends Annotation
{

	public PartitionAnnotation() {
		super("Partitions");
	}
	
	public PartitionAnnotation(int pos, int length) {
		this();
		super.pos = pos;
		super.length = length;
	}
	
	
}
