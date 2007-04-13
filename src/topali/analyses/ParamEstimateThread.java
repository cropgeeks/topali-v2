// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import org.apache.log4j.Logger;

import pal.alignment.*;
import pal.distance.AlignmentDistanceMatrix;
import pal.eval.LikelihoodValue;
import pal.substmodel.SubstitutionModel;
import pal.tree.NeighborJoiningTree;

public class ParamEstimateThread extends Thread
{
	Logger log = Logger.getLogger(this.getClass());
	
	private Alignment alignment = null;

	private LikelihoodValue likelihood = null;

	private SitePattern sitePattern = null;

	private double avgDist = 0;

	private double ratio = 2;

	private double kappa = 0;

	private double alpha = 4;

	private double[] freqs = null;

	// Tracks the current-log-likelihood and the previous-log-likelihood
	private double cLL = 0, pLL = 1;

	public ParamEstimateThread(Alignment alignment)
	{
		this.alignment = alignment;
	}

	public double getRatio()
	{
		return ratio;
	}

	public double getAlpha()
	{
		return alpha;
	}

	public double getKappa()
	{
		return kappa;
	}

	public double getAvgDistance()
	{
		return avgDist;
	}

	public double[] getFreqs()
	{
		return freqs;
	}

	public void run()
	{
		sitePattern = new SitePattern(alignment);

		// Work out the frequencies
		freqs = AlignmentUtils.estimateFrequencies(alignment);

		for (int iteration = 0; iteration < 20; iteration++)
		{
			// Work out Alpha
			alpha = estimateParameter(0.1, 200, 1, true);
			log.info("Iter: " + iteration + ": alpha = " + alpha
					+ ", " + cLL);

			if (isDifferenceSignificant())
				break;

			// Work out the T/T Ratio
			ratio = estimateParameter(0.5, 100, 1, false);
			log.info("Iter: " + iteration + ": ratio = " + ratio
					+ ", " + cLL);

			if (isDifferenceSignificant())
				break;
		}

		// Work out the average distance
		calculateAverageDistance();

		// And finally, work out kappa
		calculateKappa();
	}

	private boolean isDifferenceSignificant()
	{
		double difference = cLL - pLL;
		if (difference < 0)
			difference *= -1;

		pLL = cLL;

		return difference < 0.1;
	}

	private double estimateParameter(double d1, double d2, int iteration,
			boolean isAlpha)
	{
		// Determine likelihood at points between left-point and mid-point, and
		// right-point and mid-point
		double lPoint = getLikelihood(d1, (d1 + d2) / 2, isAlpha);
		double rPoint = getLikelihood(d2, (d1 + d2) / 2, isAlpha);

		double difference = lPoint - rPoint;
		if (difference < 0)
			difference *= -1;

		// BUT...if the difference between the two points is less than 0.1, then
		// just stop now (or if the number of iterations is 20)
		if (difference < 0.1 || iteration == 20)
		{
			cLL = (lPoint + rPoint) / 2;
			return (d1 + d2) / 2;
		}

		// Decide which direction to look in next
		if (lPoint > rPoint)
			return estimateParameter(d1, (d1 + d2) / 2, ++iteration, isAlpha);
		else
			return estimateParameter(d2, (d1 + d2) / 2, ++iteration, isAlpha);
	}

	// Returns value at midpoint of d1 and d2
	private double getLikelihood(double d1, double d2, boolean isAlpha)
	{
		SubstitutionModel sm = null;

		if (isAlpha)
			sm = TreeUtilities.getF84SubstitutionModel(alignment, ratio,
					(d1 + d2) / 2);
		else
			sm = TreeUtilities.getF84SubstitutionModel(alignment,
					(d1 + d2) / 2, alpha);

		return getLikelihood(sm);
	}

	private double getLikelihood(SubstitutionModel model)
	{
		likelihood = new LikelihoodValue(sitePattern);
		likelihood.setModel(model);
		likelihood.setTree(new NeighborJoiningTree(new AlignmentDistanceMatrix(
				sitePattern, model)));

		return likelihood.compute();
	}

	private void calculateAverageDistance()
	{
		SubstitutionModel model = TreeUtilities.getF84SubstitutionModel(
				alignment, ratio, alpha);

		AlignmentDistanceMatrix dist = new AlignmentDistanceMatrix(
				new SitePattern(alignment), model);

		for (int i = 0; i < dist.getSize(); i++)
			for (int j = 0; j < dist.getSize(); j++)
				avgDist += dist.getDistance(i, j);

		avgDist /= (dist.getSize() * (dist.getSize() - 1));
	}

	/* Formula taken from PAL - works out kappa (required by BAMBE). */
	private void calculateKappa()
	{
		double piA = freqs[0];
		double piC = freqs[1];
		double piG = freqs[2];
		double piT = freqs[3];

		double piR = piA + piG;
		double piY = piT + piC;

		double tstv = ratio;

		double rho = (piR * piY * (piR * piY * tstv - (piA * piG + piC * piT)))
				/ (piC * piT * piR + piA * piG * piY);

		kappa = 1.0 + 0.5 * rho * (1.0 / piR + 1.0 / piY);
	}
}