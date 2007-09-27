// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import java.util.LinkedList;

import junit.framework.TestCase;

import topali.var.AssociationMap;


public class AssociationMapTest extends TestCase
{

	public void testAssociationMap() {
		AssociationMap<String> am = new AssociationMap<String>();
		am.put("a", "1");
		am.put("b", "2");
		am.put("b", "21");
		am.put("c", "3");
		am.put("c1", "3");
		am.put("e", "4");
		am.put("e", "41");
		am.put("f", "5");
		am.put("5", "f");
		
		LinkedList<String> res1 = am.get("a");
		assertTrue(res1.size()==1);
		assertTrue(res1.get(0).equals("1"));
		
		LinkedList<String> res2 = am.get("b");
		assertTrue(res2.size()==2);
		assertTrue(res2.get(0).equals("2") && res2.get(1).equals("21"));
		
		LinkedList<String> res3 = am.get("3");
		assertTrue(res3.size()==2);
		assertTrue(res3.get(0).equals("c") && res3.get(1).equals("c1"));
		
		am.remove("3", "c");
		LinkedList<String> res4 = am.get("3");
		assertTrue(res4.size()==1);
		assertTrue(res4.get(0).equals("c1"));
		
		am.remove("e");
		LinkedList<String> res5 = am.get("e");
		assertTrue(res5.size()==0);
		
		LinkedList<String> res6 = am.get("f");
		assertTrue(res6.size()==1);
		assertTrue(res6.get(0).equals("5"));
		
		assertTrue(am.contains("1"));
		
		assertFalse(am.contains("6"));
		
		am.put("w", "x");
		am.put("x", "y");
		am.put("z", "y");
		LinkedList<String> res7 = am.getAll("w");
		assertTrue(res7.size()==3);
		assertTrue(res7.get(0).equals("x") && res7.get(1).equals("y") && res7.get(2).equals("z"));
		
		
		System.out.println(am);
	}
}
