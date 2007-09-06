// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.Serializable;

public class CDSAnnotations extends RegionAnnotations implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6590308900181519467L;

	public CDSAnnotations()
	{
		label = "Coding Regions";
	}

	@Override
	protected AnnotationElement create(int position)
	{
		return new AnnotationElement(AnnotationElement.CODINGREG, position);
	}

}