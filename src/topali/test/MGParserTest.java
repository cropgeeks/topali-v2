// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import java.io.File;
import java.net.*;

import topali.cluster.jobs.modelgenerator.MGParser;
import topali.data.MGResult;

public class MGParserTest
{

	/**
	 * @param args
	 * @throws URISyntaxException 
	 */
	public static void main(String[] args) throws URISyntaxException
	{
		MGResult model = new MGResult();
		
		URL url = MGParserTest.class.getResource("/topali/test/modelgenerator-testoutput.txt");
		File f = new File(url.toURI());
		
		model = MGParser.parse(f, model);
		model.sortByAIC1();
		System.out.println(model);
		System.out.println("____________________\n");
		
		model.sortByAIC2();
		System.out.println(model);
		System.out.println("____________________\n");
		
		model.sortByBIC();
		System.out.println(model);
		System.out.println("____________________\n");
		
		model.sortByLNL();
		System.out.println(model);
	}

}
