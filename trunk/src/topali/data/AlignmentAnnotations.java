// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.Serializable;
import java.util.Vector;

public class AlignmentAnnotations implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7575283316076221645L;

	protected Vector<AnnotationElement> annotations;

	protected String label;

	protected String description;

	public AlignmentAnnotations()
	{
		annotations = new Vector<AnnotationElement>();
	}

	public Vector<AnnotationElement> getAnnotations()
	{
		return annotations;
	}

	public void setAnnotations(Vector<AnnotationElement> annotations)
	{
		this.annotations = annotations;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public int size()
	{
		return annotations.size();
	}
}