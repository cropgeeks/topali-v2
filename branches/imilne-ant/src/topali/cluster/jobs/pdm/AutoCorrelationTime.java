// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm;

// Compute the autocorrelation time (ACT) from the energies
// (negative log likelihoods). The energies are assumed to
// be stored in the file --> run1.lpd, which is created
// by BAMBE.
// The program computes the autocorrelation function (ACF)
// unitl ACF<exp(-1).
// The corresponding time is written out as the ACT.
//
//
// READ IN
// infile
// run1.lpd
//
// USAGE
// After each MCMC simulation, all the method --> addACT
// After a finished simulation, get the average ACT with 
// --> showAverage() or print it out with --> printOutAverage.
// Get the standard deviation with --> showSigma() and print it
// out with --> printOutSigma(). To view all the ACTs, call
// --> showAll() or, to print them out, call --> printOutAll.
// A summary of all the information can be obtained with the call
// --> info().

import java.io.*;
import java.util.Vector;

import topali.data.PDMResult;

public class AutoCorrelationTime
{

	private float X[];

	// input data, usually energies
	private Vector<Float> actVector;

	// vectors of ACTs
	private File inFileName;

	// name of the input file
	private int Nsample;

	// Length of input file
	private int Nburn;

	// Number of discarded configurations (burn-in)

	private File wrkDir;

	private PDMResult result;

	// ================= CONSTRUCTORS =================

	// --------------------------------------------------------
	public AutoCorrelationTime(File wrkDir, PDMResult result)
			throws JambeException
	{
		this.wrkDir = wrkDir;
		this.result = result;

		// C o n s t r u c t o r : default filename for input data
		inFileName = new File(wrkDir, "run1.lpd");

		// Find out length of the sampling and the burn-in periods
		Nsample = this.sampleSize();
		Nburn = this.lengthBurnIn();

		if (Nsample <= 0)
			throw new JambeException(
					"AC01: Nsample <= 0 (Bambe settings for "
							+ "'number of cycles' and 'sample interval' may be invalid).");

		X = new float[Nsample];

		// Create global vector of ACTs
		actVector = new Vector<Float>();
	}

	// --------------------------------------------------------
	public AutoCorrelationTime(String specifiedFileName) throws JambeException
	{
		// C o n s t r u c t o r : specified filename for input data
		inFileName = new File(specifiedFileName);
		// Find out length of the sampling and the burn-in periods
		Nsample = this.sampleSize();
		Nburn = this.lengthBurnIn();

		if (Nsample <= 0)
			throw new JambeException(
					"AC02: Nsample <= 0 (Bambe settings for "
							+ "'number of cycles' and 'sample interval' may be invalid).");

		X = new float[Nsample];

		// Create global vector of ACTs
		actVector = new Vector<Float>();
	}

	// ================= PUBLIC METHODS =================

	// --------------------------------------------------------
	public void info() throws JambeException
	{
		System.out.println("Total number of configurations read in = "
				+ Nsample + Nburn);
		System.out.println(Nburn
				+ " configurations have been discarded (burn-in).");
		System.out
				.println(Nsample
						+ " configurations have been used to compute the autocorrelation time (sampling period).");
		System.out.println("Average autocorrelation time = "
				+ this.showAverage());
		System.out.println("Standard deviation = " + this.showSigma());
		System.out
				.println("The individual values of the autocorrelation time can be found in file results_ACTs.out.");
		this.printOutAll();
		System.out
				.println("File results_ACT_Neff contains the effective sample size, the average ACT, its standard deviation, and the total sample size (in this order).");
		this.printOutResults();
	}

	// --------------------------------------------------------
	public void printOutAll() throws JambeException
	{
		// Prints out all the acf times (ACT) to file "results_ACTs.out".
		File outFileName = new File(wrkDir, "results_ACTs.out");
		try
		{
			FileOutputStream outFile = new FileOutputStream(outFileName);
			PrintStream outStream = new PrintStream(outFile);
			float tau;
			int i;
			actVector.trimToSize();
			int nLength = actVector.size();
			for (i = 0; i < nLength; i++)
			{
				tau = ((Float) actVector.elementAt(i)).floatValue();
				outStream.println(tau);
			}
			outStream.close();
		} catch (IOException e)
		{
			throw new JambeException("AC03: Unable to open file "
					+ "results_ACTs.out for writing: " + e);
		}
	}

	// --------------------------------------------------------
	public void printOutAverage()
	{
		// Prints out the average acf time (ACT) to file.
		System.out.println(this.showAverage());
	}

	// --------------------------------------------------------
	public void printOutResults() throws JambeException
	{
		// Prints out:
		// (1) the effective sample size (exact formula),
		// (2) the effective sample size (approximate formula),
		// (3) the total sample size,
		// (4) the mean acf time (ACT) and
		// (5) its standard deviation
		// to the file "results_ACT_Neff.out".
		File outFileName = new File(wrkDir, "results_ACT_Neff.out");
		int NeffApprox = this.effectiveSampleSizeApprox();
		int NeffExact = this.effectiveSampleSizeExact();
		float actAverage = this.showAverage();
		float actSigma = this.showSigma();
		try
		{
			FileOutputStream outFile = new FileOutputStream(outFileName);
			PrintStream outStream = new PrintStream(outFile);
			outStream.println(NeffExact + "   " + NeffApprox + "   " + Nsample
					+ "   " + actAverage + "   " + actSigma);
			outStream.close();
		} catch (IOException e)
		{
			throw new JambeException("AC04: Unable to open file "
					+ "results_ACT_Neff.out for writing: " + e);
		}
	}

	// --------------------------------------------------------
	public void printOutSigma()
	{
		// Prints out the standard deviation of the acf time (ACT) to file.
		System.out.println(this.showSigma());
	}

	// --------------------------------------------------------
	public void readIn() throws JambeException
	{
		this.readIn(inFileName);
	}

	// --------------------------------------------------------
	public void readIn(File fileName) throws JambeException
	{
		// Reads in data from file.
		int i = 0;
		int iSave = 0;
		String inputLine = null;
		// deprecated: FileInputStream inFile=null;
		FileReader inFile = null;
		// deprecated: DataInputStream inStream=null;
		BufferedReader inStream = null;

		// Check if the length of the input file is correct
		if (Nsample != this.numberOfRows(inFileName) - Nburn)
			throw new JambeException("AC05: " + fileName + " has wrong length.");

		// Try to open the file
		try
		{
			// deprecated:inFile = new FileInputStream(fileName);
			inFile = new FileReader(fileName);
			// deprecated: inStream = new DataInputStream(inFile);
			inStream = new BufferedReader(inFile);
		} catch (IOException ex)
		{
			throw new JambeException("AC06: Unable to open file " + fileName
					+ " " + "for reading: " + ex);
		}

		// Reads in strings until EOF and count number of lines
		while (true)
		{
			try
			{
				if ((inputLine = inStream.readLine()) == null)
					break;
				if (i > (Nburn - 1))
				{
					try
					{
						X[iSave] = new Float(inputLine.trim()).floatValue();
						iSave++; // labels the saved data
					} catch (NumberFormatException e)
					{
						// System.out.println("read: " + e);
					}
				}
				i++; // labels the data
				if (iSave > Nsample)
				{
					throw new JambeException("AC07: " + fileName
							+ " has wrong length. It " + "is " + iSave
							+ " and must be " + Nsample + ".");
				}

			} catch (IOException ex)
			{
				throw new JambeException("AC08: Unable to read from file "
						+ fileName + ": " + ex);
			}
		}

		try
		{
			inStream.close();
			inFile.close();
		} catch (IOException e)
		{
		}
	}

	// --------------------------------------------------------
	public float[] showAll()
	{
		// Returns a vector of all the ACTs
		int i;
		actVector.trimToSize();
		int nLength = actVector.size();
		float[] actArray = new float[nLength];
		for (i = 0; i < nLength; i++)
		{
			actArray[i] = ((Float) actVector.elementAt(i)).floatValue();
		}
		return actArray;
	}

	// --------------------------------------------------------
	public float showAverage()
	{
		// Shows the average acf time (ACT).
		float actAverage = 0;
		int i;
		actVector.trimToSize();
		int nLength = actVector.size();
		for (i = 0; i < nLength; i++)
		{
			actAverage += ((Float) actVector.elementAt(i)).floatValue();
		}
		actAverage /= nLength;
		return actAverage;
	}

	// --------------------------------------------------------
	public float showSigma()
	{
		// Shows the standard deviation of the acf time (ACT).
		float actAverage = 0;
		float act2ndMom = 0;
		float actVariance, actSigma;
		int i;
		actVector.trimToSize();
		int nLength = actVector.size();
		for (i = 0; i < nLength; i++)
		{
			actAverage += ((Float) actVector.elementAt(i)).floatValue();
			act2ndMom += (float) Math.pow(((Float) actVector.elementAt(i))
					.floatValue(), 2);
		}
		actAverage /= nLength;
		act2ndMom /= nLength;
		actVariance = act2ndMom - actAverage * actAverage;
		if (nLength > 2)
		{
			actVariance /= (nLength - 1);
		}
		if (actVariance <= 0)
		{
			actSigma = 0;
		} else
		{
			actSigma = (float) Math.sqrt(actVariance);
		}
		return actSigma;
	}

	// --------------------------------------------------------
	public void test() throws JambeException
	{
		// This is just for test purposes
		this.addACT();
		this.addACT();
		this.addACT();
		// this.printOutAll();
		this.printOutAverage();
		this.printOutSigma();
		this.info();
	}

	// ================= PRIVATE METHODS =================

	// --------------------------------------------------------
	public void addACT() throws JambeException
	{
		// Adds new ACT to the vector of ACTs.
		actVector.addElement(new Float(this.computeACT()));
	}

	// --------------------------------------------------------
	public int computeACT() throws JambeException
	{
		// Computes the acf time (ACT) from the data.
		int tau, i;
		float acf;

		// Read in and normalise input data
		this.readIn(inFileName);
		this.normaliseInputs();

		// Compute the autocorrelation function in integer increments
		// until its value is less than exp(-1). The cutoff value is
		// the autocorrelation time ACT.
		for (tau = 1; tau < Nsample / 2; tau++)
		{
			acf = 0;
			for (i = 0; i < (Nsample - tau); i++)
			{
				acf += X[i] * X[i + tau];
			}
			acf /= (Nsample - tau);
			if (acf < Math.exp(-1))
			{
				break;
			}
		}
		tau--;
		return tau;
	}

	// --------------------------------------------------------
	public int effectiveSampleSizeApprox()
	{
		// Effective sample size from approximative formula
		float actAverage = this.showAverage();
		int Neff;
		if (actAverage <= 0.000001)
		{
			Neff = Nsample;
		} else if (actAverage <= 1)
		{
			Neff = (int) (Nsample / (1 + actAverage));
		} else
		{
			Neff = (int) (Nsample / (2 * actAverage));
		}
		return Neff;
	}

	// --------------------------------------------------------
	public int effectiveSampleSizeExact() throws JambeException
	{
		// Effective sample size from the exact formula
		float actAverage = this.showAverage();
		float invTau, factor;
		int Neff;
		if (actAverage <= 0.000001)
		{
			Neff = Nsample;
		} else
		{
			invTau = (float) (1.0 / actAverage);
			factor = (float) Math.exp(invTau);
			factor = factor - 1;
			factor = (float) (1.0 + 2.0 / factor);
			if (factor <= 0)
			{
				throw new JambeException("AC09: factor <= 0");
			}
			Neff = (int) (Nsample / factor);
		}
		return Neff;
	}

	// Returns the number of configurations in the burn-in period
	public int lengthBurnIn()
	{
		// 200 also in BambeInfile.writeFile()
		return result.pdm_burn / 200;

		/*
		 * int nDiscard; BambeInfile oBambeInfile= new BambeInfile();
		 * oBambeInfile.readTopos("infile"); nDiscard=
		 * oBambeInfile.returnBurn()/oBambeInfile.returnInterval(); return
		 * nDiscard;
		 */
	}

	// --------------------------------------------------------
	public void normaliseInputs() throws JambeException
	{
		float mean = 0;
		float sigma = 0;
		float variance = 0;
		int i;

		// Computes the mean
		for (i = 0; i < Nsample; i++)
		{
			mean += X[i];
		}
		mean /= Nsample;

		// Subtracts the mean and compute the variance
		for (i = 0; i < Nsample; i++)
		{
			X[i] -= mean;
			variance += X[i] * X[i];
		}
		if (Nsample == 1)
		{
			variance = 0;
		} else
		{
			variance /= (Nsample - 1);
		}
		if (variance <= 0)
		{
			// throw new JambeException("AC10: variance is zero.");
		}

		// Computes the std
		sigma = (float) Math.sqrt(variance);

		// normalises the data
		for (i = 0; i < Nsample; i++)
		{
			X[i] /= sigma;
		}
	}

	// Returns the number of rows in a file. Used to find out the length of the
	// data vector
	public int numberOfRows(File filename) throws JambeException
	{
		try
		{
			LineNumberReader reader = new LineNumberReader(new FileReader(
					filename));

			while (reader.readLine() != null)
				;
			reader.close();

			return reader.getLineNumber();
		} catch (IOException e)
		{
			throw new JambeException("AC11: Unable to read from file "
					+ filename + ": " + e);
		}
	}

	// Returns the number of configurations in the sampling period
	public int sampleSize()
	{
		return result.pdm_cycles / 200;

		/*
		 * int nSampleSize; BambeInfile oBambeInfile= new BambeInfile();
		 * oBambeInfile.readTopos("infile"); nSampleSize=
		 * oBambeInfile.returnSampleSize(); return nSampleSize;
		 */
	}
}
