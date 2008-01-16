// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import java.util.*;

import junit.framework.TestCase;
import topali.data.annotations.Annotation;


public class AnnotationTest extends TestCase
{

	public void testAvailableAnnotations() {
		HashSet<String> expected = new HashSet<String>();
		expected.add("topali.data.annotations.PartitionAnnotation");
		expected.add("topali.data.annotations.RecombinantAnnotation");
		expected.add("topali.data.annotations.CodingRegionAnnotation");
		expected.add("topali.data.annotations.StructureAnnotation");
		
		List<Class<Annotation>> annos = Annotation.getAvailableAnnotationTypes();
		
		assertTrue(annos.size()==4);
		
		for(Class<Annotation> c : annos) {
			assertTrue(expected.contains(c.getName()));
			System.out.println(c.getName()+" ("+Annotation.getDescription(c)+")");
		}
		
	}
}
