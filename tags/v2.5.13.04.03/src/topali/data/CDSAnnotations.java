// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class CDSAnnotations extends RegionAnnotations
{

	public CDSAnnotations()
	{
		label = "Coding Regions";
	}

	
	protected AnnotationElement create(int position)
	{
		return new AnnotationElement(AnnotationElement.CODINGREG, position);
	}

}