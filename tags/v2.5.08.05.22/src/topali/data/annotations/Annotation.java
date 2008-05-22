// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

import topali.data.*;


public abstract class Annotation extends DataObject implements Comparable<Annotation>
{	
	int start = -1;
	int end = -1;
	String comment = null;

	int seqType = SequenceSetProperties.TYPE_UNKNOWN;
	int linkId = -1;

	public Annotation() {
	}
	
	public Annotation(int id) {
		super(id);
	}
	
	public Annotation(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public Annotation(Annotation anno) {
		this.start = anno.getStart();
		this.end = anno.getEnd();
		this.comment = anno.getComment();
		this.seqType = anno.getSeqType();
		this.linkId = anno.getLinkId();
	}
	
	public int getStart()
	{
		return start;
	}

	public void setStart(int start)
	{
		this.start = start;
	}

	public int getEnd()
	{
		return end;
	}

	public void setEnd(int end)
	{
		this.end = end;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getLinkId() {
		return linkId;
	}

	public void setLinkId(int linkId) {
		this.linkId = linkId;
	}
	
	public int getSeqType() {
		return seqType;
	}

	public void setSeqType(int seqType) {
		this.seqType = seqType;
	}

	public int compareTo(Annotation o)
	{
		if(o.start>this.start)
			return -1;
		else if(o.start<this.start)
			return 1;
		else
			return 0;
	}

	
	public String toString() {
		return start+" - "+end;
	}
	
	
}
