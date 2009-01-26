// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test.parsers;

import java.io.File;

import junit.framework.TestCase;

import topali.cluster.jobs.modeltest.analysis.ModelTestParser;


public class ModelTestParserTest extends TestCase
{

	//DNA parameters
	double expLnlDNA;
	double expAlphaDNA;
	int expGammaCatDNA;
	double[] expSubRates;
	double[] expBaseFreq;
	
	//Protein parameters
	double expLnlProtein;
	double expAlphaProtein;
	int expGammaCatProtein;
	
	public ModelTestParserTest() {
		expLnlDNA = -9166.59588;
		expAlphaDNA = 100.0;
		expGammaCatDNA = 4;
		expSubRates = new double[] {1.00000, 0.91312, 0.96294, 0.96294, 0.91312, 1.0};
		expBaseFreq = new double[] {0.24387, 0.24433, 0.27220, 0.23960};
		
		expLnlProtein = -2441.05929;
		expAlphaProtein = 1.472;
		expGammaCatProtein = 4;
	}
	
	public void testModelTestParserDNA() throws Exception {
		File in = new File(this.getClass().getResource("/res/testing/modeltestDNA.txt").toURI());
		ModelTestParser mtp = new ModelTestParser(in);
		double lnl = mtp.getLnl();
		double alpha = mtp.getGamma();
		int gammaCat = mtp.getGammaCat();
		double[] subRates = mtp.getSubRates();
		double[] baseFreq = mtp.getBaseFreq();
		
		
		assertEquals(expLnlDNA, lnl);
		assertEquals(expAlphaDNA, alpha);
		assertEquals(expGammaCatDNA, gammaCat);
		
		for(int i=0;i<expSubRates.length; i++)
			assertEquals(expSubRates[i], subRates[i]);
		
		for(int i=0;i<expBaseFreq.length; i++)
			assertEquals(expBaseFreq[i], baseFreq[i]);
	}
	
	public void testModelTestParserProtein() throws Exception {
		File in = new File(this.getClass().getResource("/res/testing/modeltestProtein.txt").toURI());
		ModelTestParser mtp = new ModelTestParser(in);
		double lnl = mtp.getLnl();
		double alpha = mtp.getGamma();
		int gammaCat = mtp.getGammaCat();;
		
		assertEquals(expLnlProtein, lnl);
		assertEquals(expAlphaProtein, alpha);
		assertEquals(expGammaCatProtein, gammaCat);
	}
}
