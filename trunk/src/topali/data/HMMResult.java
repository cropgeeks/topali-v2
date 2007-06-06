// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import sbrn.commons.MatrixXML;

/*
 * Class that stores both the results from running a HMM analysis and the
 * settings required to make the run (although not the data itself).
 */
public class HMMResult extends AlignmentResult
{	
	// The location of the BARCE binary
	public String barcePath;

	// Topology (mosaic structure) data
	public int[][] bpArray;

	public String hmm_model;

	public String hmm_initial;

	public float hmm_freq_est_1;

	public float hmm_freq_est_2;

	public float hmm_freq_est_3;

	public float hmm_freq_est_4;

	public String hmm_transition;

	public float hmm_transition_ratio;

	public float hmm_freq_1;

	public float hmm_freq_2;

	public float hmm_freq_3;

	public float hmm_difficulty;

	public int hmm_burn;

	public int hmm_points;

	public int hmm_thinning;

	public int hmm_tuning;

	public String hmm_lambda;

	public String hmm_annealing;

	public String hmm_station;

	public String hmm_update;

	public float hmm_branch;

	// The three graphs
	public float[][] data1, data2, data3;

	public HMMResult()
	{
	}

	// Castor conversion methods so that the 2D data arrays are saved as strings
	public String getBpArray()
	{
		return MatrixXML.arrayToString(bpArray);
	}

	public void setBpArray(String str)
	{
		bpArray = MatrixXML.stringTo2DIntArray(str);
	}

	public String getData1()
	{
		return MatrixXML.arrayToString(data1);
	}

	public void setData1(String str)
	{
		data1 = MatrixXML.stringTo2DFloatArray(str);
	}

	public String getData2()
	{
		return MatrixXML.arrayToString(data2);
	}

	public void setData2(String str)
	{
		data2 = MatrixXML.stringTo2DFloatArray(str);
	}

	public String getData3()
	{
		return MatrixXML.arrayToString(data3);
	}

	public void setData3(String str)
	{
		data3 = MatrixXML.stringTo2DFloatArray(str);
	}

}