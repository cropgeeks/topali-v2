// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

public class AnalysisUtils
{
	// Given a (sorted) array of floats, returns the value of the float at the
	// array index that is percentile %age along (eg the 95th percentile)
	public static float getArrayValue(float[] values, float percentile)
	{
		int index = Math.round(percentile * values.length);
		if (index >= values.length)
			index = values.length - 1;

		return values[index];
	}
}