// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml.parser;

import topali.cluster.jobs.cml.Models;
import topali.data.CodeMLModel;

/**
 * Base class for the CodeML result file parsers
 */
public abstract class CMLResultParser
{
	protected CodeMLModel model = new CodeMLModel();

	public CMLResultParser()
	{
	}

	/**
	 * Get a parser for a certain model
	 * 
	 * @param model
	 * @return
	 */
	public static CMLResultParser getParser(int model)
	{
		CMLResultParser parser = null;

		switch (model)
		{
		case Models.MODEL_M0:
			parser = new Model0Parser();
			break;
		case Models.MODEL_M1:
			parser = new Model1aParser();
			break; // same as M1a
		case Models.MODEL_M2:
			parser = new Model2aParser();
			break; // same as M2a
		case Models.MODEL_M1a:
			parser = new Model1aParser();
			break;
		case Models.MODEL_M2a:
			parser = new Model2aParser();
			break;
		case Models.MODEL_M3:
			parser = new Model3Parser();
			break;
		case Models.MODEL_M7:
			parser = new Model7Parser();
			break;
		case Models.MODEL_M8:
			parser = new Model8Parser();
			break;
		}

		if (parser == null)
			throw new RuntimeException(
					"Unknown Model or Parser not implemented yet!");

		return parser;
	}

	public abstract void parse(String resultFile, String rstFile);

	public CodeMLModel getModelResult()
	{
		return model;
	}

}
