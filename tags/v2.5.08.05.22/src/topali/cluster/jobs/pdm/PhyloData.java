// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm;

// Read in dna.dat with the whole DNA alignment;
// write out a window of length windowLength starting at
// position windowStart.

import java.io.*;
import java.util.StringTokenizer;

public class PhyloData
{

	// private String x;
	private int nDNA; // Number of nucleotides in the alignment

	private int nSpecies; // Number of species

	private int firstNuc; // First nucleotide

	private int lastNuc; // Last nucleotide

	private String[] speciesName;

	private String[] dnaSeq;

	private File wrkDir;

	public PhyloData(File wrkDir, int windowStart, int windowLength)
			throws JambeException
	{
		this.wrkDir = wrkDir;

		// Constructor
		if (windowStart <= 0 || windowLength <= 0)
			throw new JambeException("PD01: windowStart and windowLength must "
					+ "be positive.");

		firstNuc = windowStart;
		lastNuc = windowStart + windowLength - 1;
	}

	public int showLengthDNA()
	{
		return nDNA;
	}

	// ---------------------------------------------------

	public void write(PrintStream outStream, String x) throws IOException
	{
		// Write out string
		outStream.print(x);
	}

	// ---------------------------------------------------

	public void write(PrintStream outStream, String x, int lineLength)
			throws IOException
	{
		// Write out string, broken up in lines of indicated length
		int i;
		String xNew = "";
		int L = x.length();
		for (i = 0; i < L; i++)
		{
			if (i == (i / lineLength) * lineLength && i > 0)
			{
				xNew = xNew + "\n";
			}
			xNew = xNew + x.substring(i, i + 1);
		}
		outStream.print(xNew);
	}

	// ---------------------------------------------------

	public void write(PrintStream outStream, int x) throws IOException
	{
		// Write out integer
		outStream.print(x);
	}

	// ---------------------------------------------------

	public int[] readN(BufferedReader inStream) throws IOException
	{
		// Read in two integers from one line
		int I1, I2;
		int[] intArray = new int[2];
		String inputLine = inStream.readLine();
		StringTokenizer theTokenizer = new StringTokenizer(inputLine, " ");
		I1 = Integer.parseInt(theTokenizer.nextToken());
		I2 = Integer.parseInt(theTokenizer.nextToken());
		intArray[0] = I1;
		intArray[1] = I2;
		return intArray;
	}

	// ---------------------------------------------------

	public String readName(BufferedReader inStream) throws IOException
	{
		// Read in species names
		String inputLine = inStream.readLine();
		return inputLine;
	}

	// ---------------------------------------------------

	public String readSeq(BufferedReader inStream) throws IOException
	{
		// Read in DNA sequences
		String blank = " ";
		int n = 0;
		int nStart = this.firstNuc;
		int nEnd = this.lastNuc;
		int i, L;
		String outLine = "";
		while (n < this.nDNA)
		{
			String inLine = inStream.readLine();
			L = inLine.length();
			for (i = 0; i < L; i++)
			{
				if (!blank.equals(inLine.substring(i, i + 1)))
				{
					n++;
					if (n >= nStart && n <= nEnd)
					{
						outLine = outLine + inLine.substring(i, i + 1);
					}
				}
			}
		}
		return outLine;
	}

	// ----------------------------------------------------

	public void checkConsistency() throws JambeException
	{
		// Check of assignments are consistent
		if (firstNuc < 0 || lastNuc <= firstNuc || nDNA < lastNuc)
		{
			System.out.println("firstNuc= " + this.firstNuc);
			System.out.println("lastNuc= " + this.lastNuc);
			System.out.println("nDNA= " + this.nDNA);

			throw new JambeException(
					"PD02: Wrong assignments of firstNuc, "
							+ "lastNuc, nDNA (possible incorrect data in alignment file).");
		}
	}

	// ---------------------------------------------------

	public void select() throws JambeException
	{
		this.select(1);
	}

	// ---------------------------------------------------

	public void select(int NreplicateWin) throws JambeException
	{
		// NreplicateWin = number of times the same window is replicated.
		// NreplicateWin==1 --> correct results (T=1)
		// NreplicateWin>1 --> simulated annealing (T<1)
		int i, j;
		int[] intArray = new int[2];

		if (NreplicateWin < 1)
			throw new JambeException("PD03: NreplicateWin < 1");

		// Read data from file
		try
		{
			// Open inout file
			FileReader inFile = new FileReader(new File(wrkDir, "dna.dat"));
			BufferedReader inStream = new BufferedReader(inFile);
			// Read in number of species and length of alignment
			intArray = this.readN(inStream);
			this.nSpecies = intArray[0];
			this.nDNA = intArray[1];
			// Check that this is consistent with the chosen window length
			this.checkConsistency();

			this.dnaSeq = new String[this.nSpecies];
			this.speciesName = new String[this.nSpecies];

			for (i = 0; i < this.nSpecies; i++)
			{
				this.speciesName[i] = this.readName(inStream);
				this.dnaSeq[i] = this.readSeq(inStream);
			}

			inStream.close();
			inFile.close();
		} catch (IOException e)
		{
			throw new JambeException(
					"PD04: Unable to open file dna.dat for reading: " + e);
		}

		// Write data from file
		try
		{
			FileOutputStream outFile = new FileOutputStream(new File(wrkDir,
					"dna.in"));
			PrintStream outStream = new PrintStream(outFile);
			this.write(outStream, this.nSpecies);
			this.write(outStream, " ");
			this.write(outStream, NreplicateWin
					* (this.lastNuc - this.firstNuc + 1));
			this.write(outStream, "\n");
			for (i = 0; i < this.nSpecies; i++)
			{
				this.write(outStream, this.speciesName[i]);
				this.write(outStream, "\n");
				for (j = 0; j < NreplicateWin; j++)
				{
					this.write(outStream, this.dnaSeq[i], 50);
					if (j < NreplicateWin - 1)
						this.write(outStream, "\n"); // aesthetic format
				}
				this.write(outStream, "\n");
			}

			outStream.close();
			outFile.close();
		} catch (IOException e)
		{
			throw new JambeException(
					"PD05: Unable to open file dna.in for writing: " + e);
		}

		// System.out.println("Window of "+(this.lastNuc-this.firstNuc+1)+" base
		// pairs, from "+this.firstNuc+" to "+this.lastNuc+", now in file
		// dna.in");
	}

	// ---------------------------------------------------
	/*
	 * public static void main (String args[]){ int windowStart=0; int
	 * windowEnd=0; int windowLength=0; String inputLine=null; //deprecated:
	 * FileInputStream inFile=null; FileReader inFile=null; //deprecated:
	 * DataInputStream inStream=null; BufferedReader inStream=null; // First,
	 * try to open the file try{ // deprecated:inFile = new
	 * FileInputStream(fileName); inFile= new FileReader("in.windowStartEnd"); //
	 * deprecated: inStream = new DataInputStream(inFile); inStream= new
	 * BufferedReader(inFile); } catch (IOException ex) {
	 * gui.MsgBox.dirkError("ERROR in PhyloData: Couldn't read from file
	 * in.windowStartEnd"); } // Now read in windowStart and windowLength try{
	 * inputLine= inStream.readLine(); } catch (IOException ex) {
	 * gui.MsgBox.dirkError("ERROR: Wrong format of input file"); }
	 * StringTokenizer oTokenizer= new StringTokenizer(inputLine); if
	 * (oTokenizer.hasMoreTokens()){ windowStart=
	 * Integer.parseInt(oTokenizer.nextToken()); } else {
	 * gui.MsgBox.dirkError("ERROR in PhyloData when reading in file
	 * in.windowStartEnd"); } if (oTokenizer.hasMoreTokens()){ windowEnd=
	 * Integer.parseInt(oTokenizer.nextToken()); } else {
	 * gui.MsgBox.dirkError("ERROR in PhyloData when reading in file
	 * in.windowStartEnd"); } windowLength=windowEnd-windowStart+1; PhyloData
	 * oPhyloData= new PhyloData(windowStart, windowLength);
	 * oPhyloData.select(); }
	 */

}
