// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

import java.util.List;

public abstract class Annotation implements Comparable<Annotation>
{
	static List<Class<Annotation>> availableAnnotationTypes = null;
	
	int pos = -1;
	int length = -1;
	String comment = "";

	public Annotation() {
	}
	
	public Annotation(int pos, int length) {
		this.pos = pos;
		this.length = length;
	}
	
	public int getPos()
	{
		return pos;
	}

	public void setPos(int pos)
	{
		this.pos = pos;
	}

	public int getLength()
	{
		return length;
	}

	public void setLength(int length)
	{
		this.length = length;
	}

	@Override
	public int compareTo(Annotation o)
	{
		if(o.pos>this.pos)
			return 1;
		else if(o.pos<this.pos)
			return -1;
		else
			return 0;
	}
}
