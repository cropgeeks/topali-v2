// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm;

//         _______  E  n  t  r  o  p  y  _______
// The main program moves a window over the DNA sequence
// alignment and prints out,for each position of the window,
// a sequence of tree topology strings.
// From this, class Entropy computes:
// -- 1 --
// --> histogram
// This is the distribution of tree topologies for each window
// position (the name "histogram" is slightly misleading 
// because the scores are normalised and therefore represent
// a proper probability distribution).
// Rows: window position
// Cols: tree topologies
// -- 2 --
// --> entropy
// Array of entropies computed from the histogram matrix. 
//
// This method has two alternative constructors, both of 
// which translate the topology strings into
// integer labels. The first constructor takes a
// string array as an argument and translates this into
// an integer array. The second, alternative, constructor
// reads in the strings from an external file.
// This is useful for long MCMC runs or long DNA sequence
// alignments, where an excessively large string array
// might throw a memory overflow exception.
// In the second case, the constructor also writes
// out a translation table between array strings
// and integer codes. By default, the name of this
// file is called "resultsStringToIntegerTranslator.out". 

import java.io.*;
import java.text.*;
import java.util.*;
//import java.lang.Math;

public class Entropy
{	
  private String[][] stringMatrix; 
    // Matrix with topology strings. 
    // Only used with Constructor 1. 
    // Cols: Time in the MCMC simulation. 
    // Rows: Section in the alignment.
  private int[][] intMatrix;
    // Integer matrix, where the strings have been replaced by ints.
  float[][] histogram;
    // Histogram matrix. Rows represent mean postions in the alignment; colums represent different topologies of the tree.
  public int nRows, nCols;
    // Number of rows (sections in the alignment= window positions) 
    // and columns (MCMC time steps)
  public int nTopos;
    // Number of different topologies= max int in intMatrix.
  private float[] entropy;
    // Array of entropies computed from the histogram matrix
  private Vector<String> stringList = new Vector<String>(1,1);
    // Complete list of all topology strings that occur.
    // Only used with constructor 2.

  private float[] local;
  // Stores maximum number of non-zero topologies at each location
  private int[] maxNonZero;
  
  // Used to calculate smoothing
  private int hops = 2;
  
  private File wrkDir;
  private boolean isPruning;

	public Entropy(File wrkDir, int windowSize, int stepSize, boolean isPruning)
	{
		this.wrkDir = wrkDir;
		this.isPruning = isPruning;
		
		float percent = stepSize / (float) windowSize * 100;
			
		hops = (int) (50 / percent) + 1;
		
		// Ensure we get an even number
		if (hops % 2 != 0)
			hops++;
		// And make sure it's at least 2
		if (hops < 2)
			hops = 2;
			
		System.out.println(percent + "%");
		System.out.println("hops: " + hops);
	}

  // ----------------------------------------------------------
  public boolean getTopologyStrings(String[][] argumentMatrix) throws JambeException
  {
    // The topology strings are passed on in a matrix.
    // Determine number of rows and cols of the argument
    // matrix, initialise the indicator matrix. 
    stringMatrix= argumentMatrix;
    int i,k;

    // Set nRows and nCols and  check if argumentMatrix is a proper matrix
    nRows = stringMatrix.length;
    nCols = stringMatrix[0].length;
    for (i = 0; i < nRows; i++) {
      if (stringMatrix[i].length != nCols) 
            throw new JambeException("EN01: All rows must have the same "
            	+ "length.");
    }

    // Instantiate intMatrix
    intMatrix= new int[nRows][nCols];
    for (i=0; i<nRows; i++)
      for (k=0; k<nCols; k++)
	intMatrix[i][k]=0;

    // System.out.println("Number of rows="+nRows);
    // System.out.println("Number of cols="+nCols);

    // Creates the matrix intMatrix and computes the number
    // of different topologies, nTopo.
    this.setIntMatrix();
    
    return true;
  }
  
  // ----------------------------------------------------------
  public boolean getTopologyStrings() throws JambeException
  {
    // The topology strings are read in from a file.
    // stringMatrix is not used in this version.
    // intMatrix is computed directly from the file. 
    // nRows and nCols are computed automatically
    // in method  setIntMatrix(String fileName).
    // nTopo is also computed in setIntMatrix(String fileName)

	System.out.println("ENTROPY: call setIntMatrix(File)");
    if (!setIntMatrix(new File(wrkDir, "resultsAllTopos.out")))
    	return false;
    	
    if (!printStringToIntegerTranslator())
    	return false;
 
    return true;
  }
  
   // --------------------------------------------------
  public void stringMatch(String topoString, int m){
    // Compare all elements in stringMatrix with the argument
    // string. If a match occurs and the respective element in
    // the matrix intMatrix(i,k) is still in its virginal state, 0,
    // intMatrix(i,k) is set to the specified integer, m.
    int i,k;
    
    for (i=0; i<nRows; i++)
      for (k=0; k<nCols; k++)
	if (intMatrix[i][k]==0 && topoString.equals(stringMatrix[i][k])){
	    intMatrix[i][k]=m;
	}
  }
  
  // --------------------------------------------------
  public void setIntMatrix(){
    // Creates the matrix intMatrix from the string array
    // stringMAtrix and computes the number
    // of different topologies, nTopo.
    int i,k;
    String topoString;
    
    nTopos=0; // class attribute
    
    for (i=0; i<nRows; i++)
      for (k=0; k<nCols; k++)
	// check for virginal state;
	// discard if state is not virginal
	if (intMatrix[i][k]==0){
	  topoString= stringMatrix[i][k];
	  nTopos++;
	  this.stringMatch(topoString,nTopos);
	    // Note that this updates intMatrix. 
	}  
  }
  
  // -------------------------------------------------------------
  public boolean setIntMatrix(File fileName) throws JambeException
  {
  	System.out.println("setIntMatrix(File) " + nTopos);	
  	System.out.println("-sringlist.size() " + stringList.size());
  	
    // Creates intMatrix by reading in the topologies
    // from a file rather than passing them on as an
    // array. String[][] stringMatrix is not needed.
    // Computes the number of different topologies, nTopo.
    // List Vector stringList allows translating the 
    // integer numbers back to the string topos.
    // Invokes: numberOfRowsAndCols(String fileName)
    // and stringToIntegerTranslator(String topoString).
    int i,j;
    int[] nRowsAndCols= new int[2];
    String topoArrayElement;
    StringTokenizer oTokenizer;
    String inputLine=null;
       //deprecated: FileInputStream inFile=null;
    FileReader inFile=null; 
       //deprecated: DataInputStream inStream=null;
    BufferedReader inStream=null;

    // Find out number of rows and columns in the array
    nRowsAndCols= this.numberOfRowsAndCols(fileName);
    nRows= nRowsAndCols[0];
    nCols= nRowsAndCols[1];

    // Instantiate intMatrix
    intMatrix = new int[nRows][nCols];
    

    // First, try to open the file
    try{
        // deprecated:inFile = new FileInputStream(fileName);
      inFile= new FileReader(fileName);
        // deprecated: inStream = new DataInputStream(inFile);
      inStream= new BufferedReader(inFile);
      } catch (IOException ex) {
      	throw new JambeException("EN02: Unable to open file " + fileName
      		+ " for reading: " + ex);
    }

    // Read in topology strings until EOF and translate them into 
    // an integer code which is saved in array intMatrix. 
    // The program is terminated with an error if the 
    // numbers of rows and columns are not consistent.
    i=0;
    while (true){
      try{
	//inputLine= inStream.readLine();
	if ((inputLine= inStream.readLine()) == null)
	  break;
	oTokenizer= new StringTokenizer(inputLine);
	j=0;
	while (oTokenizer.hasMoreTokens()){
	  if (j>nCols || i>nRows){
	  	throw new JambeException("EN03: Wrong number of rows or columns for "
	  		+ "topoArray.");
	  }
	  topoArrayElement=oTokenizer.nextToken();
	  intMatrix[i][j]=this.stringToIntegerTranslator(topoArrayElement);
	    // Updates Vector stringList
	  j++;
	}
	if (j != nCols){
		throw new JambeException("EN04: In file " + fileName + " the number of "
			+ "columns is = " + j + " rather than " + nCols + ".");
	}
	oTokenizer=null;
	i++;
      } catch (IOException ex) {
      	throw new JambeException("EN05: Unable to read from file " + fileName
      		+ ": " + ex);
      } 
    }
    
    try
    {
    	inStream.close();
    }
    catch (Exception e) {}

    // Get the number of different topologies
    if (isPruning == false)
    	nTopos= stringList.size();    
    System.out.println("File: nTopos=" + nTopos);
      // Note that this list gets updated above in the call: 
      // this.stringToIntegerTranslator(topoArrayElement)
      
     return true;
  }

  // -------------------------------------------------------------
  public boolean setIntMatrix(File fileName, File intFile) throws JambeException
  {
  	System.out.println("setIntMatrix(File, File)");
  	System.out.println("sringlist.size() " + stringList.size());
  	
    // Creates intMatrix by reading in the topologies
    // from a file rather than passing them on as an
    // array. String[][] stringMatrix is not needed.
    // Computes the number of different topologies, nTopo.
    // List Vector stringList allows translating the 
    // integer numbers back to the string topos.
    // Invokes: numberOfRowsAndCols(String fileName)
    // and stringToIntegerTranslator(String topoString).
    int i,j;
    int[] nRowsAndCols= new int[2];
    String topoArrayElement;
    StringTokenizer oTokenizer;
    String inputLine=null;
       //deprecated: FileInputStream inFile=null;
    FileReader inFile=null; 
       //deprecated: DataInputStream inStream=null;
    BufferedReader inStream=null;
    
    BufferedWriter out = null;

    // Find out number of rows and columns in the array
    nRowsAndCols= this.numberOfRowsAndCols(fileName);
    nRows= nRowsAndCols[0];
    nCols= nRowsAndCols[1];

    // Instantiate intMatrix
    intMatrix = new int[nRows][nCols];
    
    // First, try to open the file
    try{    	
        // deprecated:inFile = new FileInputStream(fileName);
      inFile= new FileReader(fileName);
        // deprecated: inStream = new DataInputStream(inFile);
      inStream= new BufferedReader(inFile);
      } catch (IOException ex) {
      	throw new JambeException("EN02: Unable to open file " + fileName
      		+ " for reading: " + ex);
    }
    
    try { out = new BufferedWriter(new FileWriter(intFile)); }
    catch (IOException e)
    {
    	throw new JambeException("EN02B: Unable to open file "
    		+ intFile + " for writing: " + e);
    }

    // Read in topology strings until EOF and translate them into 
    // an integer code which is saved in array intMatrix. 
    // The program is terminated with an error if the 
    // numbers of rows and columns are not consistent.
    i=0;
    while (true){
      try{
	//inputLine= inStream.readLine();
	if ((inputLine= inStream.readLine()) == null)
	  break;
	oTokenizer= new StringTokenizer(inputLine);
	j=0;
	while (oTokenizer.hasMoreTokens()){
	  if (j>nCols || i>nRows){
	  	throw new JambeException("EN03: Wrong number of rows or columns for "
	  		+ "topoArray.");
	  }
	  topoArrayElement=oTokenizer.nextToken();
	  intMatrix[i][j]=this.stringToIntegerTranslator(topoArrayElement);
///////////////////
	  // Write out integers
	  out.write(intMatrix[i][j]+" ");
	  // Updates Vector stringList
	  j++;
	}
	out.newLine();
	if (j != nCols){
		throw new JambeException("EN04: In file " + fileName + " the number of "
			+ "columns is = " + j + " rather than " + nCols + ".");
	}
	oTokenizer=null;
	i++;
      } catch (IOException ex) {
      	throw new JambeException("EN05: Unable to read from file " + fileName
      		+ ": " + ex);
      } 
    }
    
    try
    {
    	inStream.close();
    	out.close();
    }
    catch (Exception e) {}

    // Get the number of different topologies
    nTopos= stringList.size();
    System.out.println("nTOPOS now " + nTopos + ", stringlist= " + stringList.size());
      // Note that this list gets updated above in the call: 
      // this.stringToIntegerTranslator(topoArrayElement)
      
     return true;
  }

  // -------------------------------------------------------------
  public int[]  numberOfRowsAndCols(File fileName) throws JambeException
  {
    // Returns the number of rows and columns of a file.
    // The columns must be seperated by at least one blank.
    // Called by setIntMatrix(String fileName).
    int i=0;
    int j=0;
    int returnValues[];
    String firstInputLine=null;
    String inputLine=null;
       //deprecated: FileInputStream inFile=null;
    FileReader inFile=null; 
       //deprecated: DataInputStream inStream=null;
    BufferedReader inStream=null;

    // First, try to open the file
    try{
        // deprecated:inFile = new FileInputStream(fileName);
      inFile= new FileReader(fileName);
        // deprecated: inStream = new DataInputStream(inFile);
      inStream= new BufferedReader(inFile);
      } catch (IOException ex) {
      	throw new JambeException("EN06: Unable to open file " + fileName
      		+ " for reading: " + ex);
    }

    // Read in topology strings until EOF and count number of lines
    while (true){
      try{
	if ((inputLine= inStream.readLine()) == null)
	  break;
	i++;
	if (i==1)
	  firstInputLine=inputLine;
      } catch (IOException ex) {
      	throw new JambeException("EN07: Unable to read from file " + fileName
      		+ ": " + ex);
      } 
    }
    
    try
    {
    	inStream.close();
      	inFile.close();
    }
    catch (IOException e) {}

    // Determine the number of columns from the first input line. 
    // It is not checked that all the lines have the same number 
    // of columns. This has to be checked by the calling program.
    StringTokenizer oTokenizer= new StringTokenizer(firstInputLine);
    while (oTokenizer.hasMoreTokens()){
      oTokenizer.nextToken();
      j++;
    }

    // Return number of lines and number of columns
    returnValues= new int[2];
    returnValues[0]=i;
    returnValues[1]=j;

    return returnValues;
  }

  // -------------------------------------------------------------
  public int stringToIntegerTranslator(String topoString){
    // Given a topology string, this string is compared to the
    // list of already existing strings. The function returns the
    // corresponding integer (the list number). If the string does 
    // no yet exist, a new list element is created.
    // stringList must be a global Vector in the parent class
    // and must already have been instantiated:
    // private Vector stringList = new Vector(1,1);
    // Called by setIntMatrix(String fileName).

    int i;
    int nListNumber=-1;

    // Initialisation if stringList is empty
    if (stringList.size()==0){
      stringList.addElement(topoString);
    }

    // Go through the whole list and check if the topology
    // string already exists
    for (i=0; i<stringList.size(); i++){
      if ( topoString.equals( (String)stringList.elementAt(i) ) ){
	nListNumber=i;
	break;
      }
    }

    // If the topology string does not yet exist, add
    // it to the list.
    if (nListNumber==-1){
      stringList.addElement(topoString);
      nListNumber= stringList.size()-1;
    }

    return (nListNumber+1); 
    //Start counting at 1 rather than 0
  }



 // -------------------------------------------------------------
  public boolean printStringToIntegerTranslator() throws JambeException
  {
    // Prints out stringList to a file. This allows translating
    // the string topology into its integer number and vice versa.
    // The results are written out to the file 
    // "resultsStringToIntegerTranslator.out".
    
    File oFile = new File(wrkDir, "intree");

    try
    {
    	BufferedWriter out = new BufferedWriter(new FileWriter(oFile));
    	
    	for (int i=0; i<stringList.size(); i++)
	    {
	//      oPrintWriter.println((i+1)+" : "+stringList.elementAt(i));
			out.write(stringList.elementAt(i) + ";");
			out.newLine();
	    }
    	
    	out.close();
    }
    catch (IOException ex)
    {
    	throw new JambeException("EN08: Unable to open file " + oFile
      		+ " for writing: " + ex);
    }
    
	return true;
  }

  // --------------------------------------------------
  public void getHistogram() throws JambeException
  {
    // Creates the matrix of histograms from intMatrix and nTopos.
    // Rows: Window position. Cols: State (phylogenetic topology).
    int i,j,k;

    if (nTopos<=0){
    	throw new JambeException("EN12: Cannot create histogram because nTopos<=0");
    }
    histogram= new float[nRows][nTopos];

    // Initialise
    for (i=0; i<nRows; i++)
      for (k=0; k<nTopos; k++)
	histogram[i][k]=0;
    // Count to generate histogram
    for (i=0; i<nRows; i++){
      for (j=0; j<nCols; j++){
	k= intMatrix[i][j]-1;
	// -1, because array indices range from 0 to N-1
	histogram[i][k]++;
      }
    }
    // Normalisation
    for (i=0; i<nRows; i++)
      for (k=0; k<nTopos; k++)
	histogram[i][k] /= nCols;  
  }


  // --------------------------------------------------
  public void compEntropy(){
  	
  	System.out.println("nRows = " + nRows);
  	System.out.println("nTopos = " + nTopos);
  	System.out.println("histogram.length = " + histogram.length);
  	System.out.println("histogram[0].length = " + histogram[0].length);
  	
  	
    // Compute the entropy from the histogram matrix
    int n,j;
    entropy = new float[nRows];
    for (n=0; n<nRows; n++){
      entropy[n]=0;
      for (j=0; j<nTopos; j++){
	if (histogram[n][j]>0.000001)
	  entropy[n]-= histogram[n][j]*Math.log(histogram[n][j]);
      }
      //entropy[n] /= Math.log(nTopos); // normalisation
    }
  }

  // --------------------------------------------------
  public void showIntMatrix(){
    // Print out intMatrix
    int i,k;
    for (i=0; i<nRows; i++){
      for (k=0; k<nCols; k++){
	System.out.print(intMatrix[i][k]+"  ");
      }
      System.out.println();
    }	
  }

  // --------------------------------------------------
  public float[][] returnHistogram(){
    return histogram;
  }


  // --------------------------------------------------
  public void showHistogram(){
    // Print out histogram to screen
    int i,k;
    for (i=0; i<nRows; i++){
      for (k=0; k<nTopos; k++){
	System.out.print(histogram[i][k]+"  ");
      }
      System.out.println();
    }	
  }

  // --------------------------------------------------
  public void showHistogram(File fileName) throws JambeException
  {
  	DecimalFormat d = new DecimalFormat("0.0000");
  	
    // Print out histogram to file with name specified in the argument
    int i,k;
    try{
      FileOutputStream outFile = new FileOutputStream(fileName);
      PrintStream outStream= new PrintStream(outFile);
      for (i=0; i<nRows; i++){
	for (k=0; k<nTopos; k++){
	  outStream.print(d.format(histogram[i][k]) + " ");
	}
	outStream.println();
      }	
      System.out.println("Histograms are in file "+fileName);
    } catch (IOException ex) {
    	throw new JambeException("EN10: Unable to write to file " + fileName
      		+ ": " + ex);
    }
  }

  // --------------------------------------------------
  public float[] returnEntropy(){
    return entropy;
  }

  // --------------------------------------------------
  public void showEntropy(File fileName) throws JambeException
  {
    // Print out entropies to file with name specified in the argument
    int i;
    try{
      FileOutputStream outFile = new FileOutputStream(fileName);
      PrintStream outStream= new PrintStream(outFile);
      for (i=0; i<nRows; i++){
	outStream.println(entropy[i]);
      }	
      System.out.println("Entropies are in file "+fileName);
    } catch (IOException ex) {
    	throw new JambeException("EN11: Unable to write to file " + fileName
    		+ ": " + ex);
    }
  }

  // --------------------------------------------------
  public int showNTopos(){
    // Show nTopos, the number of different topologies.
    return nTopos;
  }

  // --------------------------------------------------
  public int showNWindowPositions(){
    // Show nRows, the number of different window positions=
    // the number of different sections in the alignment.
    return nRows;
  }

	private float computePQ(float[] p, float[] q)
	{
		float[] r = new float[nTopos];
		float pqSum = 0;
		
		// Sum of column data...
		for (int k = 0; k < nTopos; k++)
		{
//			if (p[k] <= Prefs.mcmc_cutoff && q[k] <= Prefs.mcmc_cutoff)
//				continue;
			
			// Compute r
			r[k] = 0.5f * (p[k] + q[k]);
		
			// Compute p log (p/r)
			float pResult = p[k] / r[k];
			if (p[k] == 0)
				pResult = 0;
			else
				pResult = p[k] * (float) Math.log(pResult);
			
			// Compute q log (q/r)
			float qResult = q[k] / r[k];
			if (q[k] == 0)
				qResult = 0;
			else
				qResult = q[k] * (float) Math.log(qResult);
			
			
			pqSum += pResult + qResult;
		}
		
		return pqSum;
	}

	private void computeLocalStatistic(int window)
	{
		// Histogram information
		if (histogram.length < hops || window < (hops-1))
			return;
				
		// Extract the two rows
		float[] p = histogram[window-1];	// nRows-2
		float[] q = histogram[window];		// nRows-1
		
		// Count the number of non-zero topologies at this location			
		for (int k = 0; k < nTopos; k++)
			if (q[k] > 0)
				maxNonZero[window]++;
		
		// Repaint the histogram
//		if (Jambe2.updateGUI)
//			histoPanel.setData(q);

		
		// Now do the local statistic	
		if (histogram.length < hops)
			return;
		
		// Example with 6 hops
		// Compares n with n-5, n-1 with n-4, and n-2 with n-3
		// (a, b, c, d, e, f) : a/f  b/e  c/d
		// With result plotted at midpoint of c/d

		int index = window; // nRows - 1;
		float score = 0;
		for (int i = 0; i < hops / 2; i++)
			score += computePQ(histogram[index-i], histogram[index-(hops-i-1)]);

		local[window-(hops/2)] = score / (hops/2);
	}
	
	// Returns the histogram array data so it can be used elsewhere in the gui
	public float[][] getHistogramArray()
	{
		return histogram;
	}
	
	public void setPrunedHistogramArray(float[][] newArray)
	{
		histogram = newArray;
		
		this.nTopos = newArray[0].length;
		this.nRows = newArray.length;
		
		System.out.println("## nTopos = " + nTopos);
	}
	
/*	void computeThreshold(float N)
	{
		// What was the maximum number of non-zero topologies?
		int max = 0;
		for (int i = 0; i < maxNonZero.length; i++)
			if (maxNonZero[i] > max)
				max = maxNonZero[i];
		

		// Max per window
		for (int i = 0; i < threshold.length; i++)
		{
			int df = maxNonZero[i] - 1;
			
			if (df == 0)
				threshold[i] = 0;
			else
			{
				double chi2 = ChiSquareDistribution.quantile(0.95, df);
				threshold[i] = (float) chi2 / (2 * N);
			}
		}

		
		// Max per alignment
		int df = max -1;
		if (df == 0) df = 1;
		double chi2 = ChiSquareDistribution.quantile(0.95, df);
		float t = (float) chi2 / (2 * N);
				
		for (int i = 0; i < threshold.length; i++)
		{
//			threshold[i] = t;
		}
		
		
		// Max per histogram
		df = histogram[0].length -1;
		System.out.println("HISTO MAX " + (df+1));
		if (df == 0) df = 1;
		chi2 = ChiSquareDistribution.quantile(0.95, df);
		t = (float) chi2 / (2 * N);
				
		for (int i = 0; i < threshold.length; i++)
		{
//			threshold[i] = t;
		}
		
		
		// Max per max-top
/*		df = getMaxNumberOfTopologies(*** NUMBER OF SEQUENCES? ***) -1;
		System.out.println("HISTO MAX " + (df+1));
		if (df == 0) df = 1;
		chi2 = ChiSquareDistribution.quantile(0.95, df);
		t = (float) chi2 / (2 * N);
				
		for (int i = 0; i < threshold.length; i++)
		{
//			threshold[i] = t;
		}
	*/
/*	}

	
	long getMaxNumberOfTopologies(int n)
	{
		long top = F(2 * n - 5);
			
		long bot = (long)( Math.pow(2, n-3)) * F(n-3);
			
		return top / bot;
	}
	
	long F(long f)
	{
		long result = f;
		for (long i = f-1; i > 0; i--)
			result *= i;
		
		System.out.println("F: " + f + ": " + result);
		return result;
	}
*/

	void createLocalArray(int size)
	{
		// Array has a length one less than the number of windows
		// (because each data point forms the midpoint of two windows)
	
		System.out.println("CREATING ARRAY OF SIZE: " + size);
	
		local = new float[size];
		for (int i = 0; i < local.length; i++)
			local[i] = -1;
			
		// Maximum number of non zero topologies array has to be the same size
		// as the number of windows though
		maxNonZero = new int[size+1];
	}
  
	public float[] getLocalData()
	{
/*		float[] actual = new float[local.length];
	
		int bValue = (int) (Prefs.mcmc_window_size / 2f / Prefs.mcmc_step_size);
	
		for (int i = 0; i < local.length; i++)
		{
			int point = i + bValue;
			if (point < local.length)
				actual[i] = local[point];
			else
				actual[i] = 0;
		}
	
		return actual;
*/	
		return local;
	}
  
	// What was the maximum number of non-zero topologies?
/*	public int getMaxNonZero()
	{
  		int max = 0;
		for (int i = 0; i < maxNonZero.length; i++)
			if (maxNonZero[i] > max)
				max = maxNonZero[i];
		
		return max;
	}
*/	
	// Return degrees of freedom based on maximum number of topologies found
	public int getHistogramDF()
	{
		int df = histogram[0].length -1;
		
		if (df < 1)
			return 1;
		else
			return df;
	}

  // --------------------------------------------------
  public void doItAll(int window) throws JambeException
  {
  	// Only write histo/entropy to disk at the end of the run
  	boolean save = (window == local.length ? true : false);  	
  
    if (isPruning == false)
    {
    	this.getHistogram();
    }
    //this.showIntMatrix();
//    this.getHistogram();
    //this.showHistogram();
//    if (save)
//    	this.showHistogram(new File(Prefs.mcmc_scratch, "results_histo.out"));
    this.compEntropy();
 //   if (window != -2)
	    this.computeLocalStatistic(window);
    
    if (save)
    	this.showEntropy(new File(wrkDir, "results_entropy.out"));
//    System.out.println("There are "+nTopos+" distinct topologies.");
  }

// --------------  The End -----------------------
}
