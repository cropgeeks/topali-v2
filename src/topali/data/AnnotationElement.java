// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class AnnotationElement
{
	public static final int PARTITION = 1;
	public static final int CODINGREG = 2;
	
	public int position;
		
	public String displayCharacter;
	public String description;
	public String secondaryStructure;
	public float value;
	
	public AnnotationElement()
	{
	}
	
	public AnnotationElement(int type, int position)
	{
		this.position = position;
		
		switch (type)
		{
			case PARTITION:
			{
				description = "TOPALi Partition Breakpoint";
				displayCharacter = "?";				
				secondaryStructure = "?";
				
				break;
			}
			
			case CODINGREG:
			{
				break;
			}
		}
	}
}