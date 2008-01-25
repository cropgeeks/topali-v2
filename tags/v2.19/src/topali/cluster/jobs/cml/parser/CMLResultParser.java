// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml.parser;

import topali.data.CMLModel;

/**
 * Base class for the CodeML result file parsers
 */
public abstract class CMLResultParser
{
	CMLModel model;
	
	public CMLResultParser(CMLModel model)
	{
		this.model = model;
	}

	/**
	 * Get a parser for a certain model
	 * 
	 * @param model
	 * @return
	 */
	public static CMLResultParser getParser(CMLModel model)
	{
		CMLResultParser parser = null;

		if(model.model.equals(CMLModel.MODEL_M0))
			parser = new Model0Parser(model);
		
		else if(model.model.equals(CMLModel.MODEL_M1)) 
			parser = new Model1aParser(model);
		
		else if(model.model.equals(CMLModel.MODEL_M2))
			parser = new Model2aParser(model);
		
		else if(model.model.equals(CMLModel.MODEL_M1a))
			parser = new Model1aParser(model);
			
		else if(model.model.equals(CMLModel.MODEL_M2a))
			parser = new Model2aParser(model);
			
		else if(model.model.equals(CMLModel.MODEL_M3))
			parser = new Model3Parser(model);
			
		else if(model.model.equals(CMLModel.MODEL_M7))
			parser = new Model7Parser(model);
			
		else if(model.model.equals(CMLModel.MODEL_M8))
			parser = new Model8Parser(model);
			

		if (parser == null)
			throw new RuntimeException(
					"Unknown Model or Parser not implemented yet!");

		return parser;
	}

	/**
	 * Parses the files and writes the data to the the CMLModel model
	 * @param resultFile
	 * @param rstFile
	 */
	public abstract void parse(String resultFile, String rstFile);

}
