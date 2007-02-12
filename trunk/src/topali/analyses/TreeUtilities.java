// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import pal.alignment.*;
import pal.distance.AlignmentDistanceMatrix;
import pal.distance.JukesCantorDistanceMatrix;
import pal.substmodel.*;

public class TreeUtilities
{
	// F84 Model
	public static SubstitutionModel getF84SubstitutionModel(Alignment a,
			double ratio, double alpha)
	{
		double[] freqs = AlignmentUtils.estimateFrequencies(a);

		// ratio is ts/tv ratio
		double[] params =
		{ ratio, freqs[0], freqs[1], freqs[2], freqs[3] };

		RateMatrix rmat = RateMatrixUtils.getInstance(a.getDataType()
				.getTypeID(), NucleotideModelID.F84, params, freqs);

		// Number of categories, shape parameter (alpha)
		RateDistribution rdist = new GammaRates(4, alpha);

		return SubstitutionModel.Utils.createSubstitutionModel(rmat, rdist);
	}

	// F84/ML distance matrix
	public static AlignmentDistanceMatrix getMaximumLikelihoodDistanceMatrix(
			Alignment a, double ratio, double alpha)
	{
		SubstitutionModel model = getF84SubstitutionModel(a, ratio, alpha);

		SitePattern sp = new SitePattern(a);

		return new AlignmentDistanceMatrix(sp, model);
	}

	// JC Model
	public static SubstitutionModel getJCSubstitutionModel(Alignment a)
	{
		double[] freqs =
		{ 0.25, 0.25, 0.25, 0.25 };

		double[] params =
		{ 0.5, freqs[0], freqs[1], freqs[2], freqs[3] };

		RateMatrix rmat = RateMatrixUtils.getInstance(a.getDataType()
				.getTypeID(), NucleotideModelID.F84, params, freqs);

		return SubstitutionModel.Utils.createSubstitutionModel(rmat);
	}

	// JC distance matrix
	public static JukesCantorDistanceMatrix getJukesCantorDistanceMatrix(
			Alignment a)
	{
		return new JukesCantorDistanceMatrix(a);
	}
}