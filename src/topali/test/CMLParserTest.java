package topali.test;

import topali.cluster.jobs.cml.Models;
import topali.cluster.jobs.cml.parser.CMLResultParser;

public class CMLParserTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("M8");
		CMLResultParser p = CMLResultParser.getParser(Models.MODEL_M8);
		p.parse("c:\\results8.txt", "c:\\rst8");
		System.out.println(p);

		System.out.println("M7");
		p = CMLResultParser.getParser(Models.MODEL_M7);
		p.parse("c:\\results7.txt", "c:\\rst7");
		System.out.println(p);
		
		System.out.println("M2a");
		p = CMLResultParser.getParser(Models.MODEL_M2a);
		p.parse("c:\\results2a.txt", "c:\\rst2a");
		System.out.println(p);
		
		System.out.println("M1a");
		p = CMLResultParser.getParser(Models.MODEL_M1a);
		p.parse("c:\\results1a.txt", "c:\\rst1a");
		System.out.println(p);
		
		System.out.println("M3");
		p = CMLResultParser.getParser(Models.MODEL_M3);
		p.parse("c:\\results3.txt", "c:\\rst3");
		System.out.println(p);
		
		System.out.println("M0");
		p = CMLResultParser.getParser(Models.MODEL_M0);
		p.parse("c:\\results0.txt", "c:\\rst0");
		System.out.println(p);
	}

}
