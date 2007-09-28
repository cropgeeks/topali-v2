// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import java.util.Vector;

import junit.framework.TestCase;
import topali.data.*;

public class CMLModelFilterTest extends TestCase
{

	CodeMLResult result;
	CMLModel m0, m01, m02, m1, m2, m21, m22, m3, m7, m71, m72;
	
	Vector<CMLModel> expected;
	
	@Override
	protected void setUp() throws Exception
	{
		result = new CodeMLResult();
		m0 = new CMLModel(CMLModel.MODEL_M0);
		m01 = new CMLModel(CMLModel.MODEL_M0);
		m02 = new CMLModel(CMLModel.MODEL_M0);
		m0.likelihood = -6000;
		m01.likelihood = -7000;
		m02.likelihood = -5000;
		
		m1 = new CMLModel(CMLModel.MODEL_M1);
		m1.likelihood = - 3000;
		
		m2 = new CMLModel(CMLModel.MODEL_M2);
		m21 = new CMLModel(CMLModel.MODEL_M2);
		m22 = new CMLModel(CMLModel.MODEL_M2);
		m2.likelihood = -11000;
		m21.likelihood = -10000;
		m22.likelihood = -12000;
		
		m3 = new CMLModel(CMLModel.MODEL_M3);
		m3.likelihood = -4000;
		
		m7 = new CMLModel(CMLModel.MODEL_M7);
		m71 = new CMLModel(CMLModel.MODEL_M7);
		m72 = new CMLModel(CMLModel.MODEL_M7);
		m7.likelihood = -1000;
		m71.likelihood = -3000;
		m72.likelihood = -2000;
		
		result.models.add(m0);
		result.models.add(m01);
		result.models.add(m02);
		result.models.add(m1);
		result.models.add(m2);
		result.models.add(m21);
		result.models.add(m22);
		result.models.add(m3);
		result.models.add(m7);
		result.models.add(m71);
		result.models.add(m72);
		
		expected = new Vector<CMLModel>();
		expected.add(m02);
		expected.add(m1);
		expected.add(m21);
		expected.add(m3);
		expected.add(m7);
	}

	public void testFilter() {
		this.result.filterModels();
		assertEquals(this.expected, this.result.models);
	}
}
