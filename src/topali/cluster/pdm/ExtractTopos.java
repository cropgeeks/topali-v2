// (C) 2003-2006 Dirk Husmeier & Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm;

// OBJECTIVE
// Reads in run1.topo and adds it as a further column to the
// existing topo array. 
// Alternatively, the tree topologies can also be written out 
// to an external file.

import java.io.*;
import java.util.*;


public class ExtractTopos{
  private String[][] topoArray;
    // String array, in which the nth row vector
    // shows all the topology strings of
    // the nth simulation (the nth window position).
  private Vector<Vector> vectorOfVectors = new Vector<Vector>(1,1);
  private Vector<String> topoStrings= new Vector<String>(1,1);
    // Vector of topology strings
  private int nDiscard;
    // Number of trees discarded (burn-in)
  private File wrkDir;
   

  // -------------------------------------------------------------
  public ExtractTopos(File wrkDir){
  	this.wrkDir = wrkDir;
    // Constructor
    vectorOfVectors= new Vector<Vector>(1,1);
    nDiscard=0;
  }

  // -------------------------------------------------------------
  public ExtractTopos(File wrkDir, int n){
  	this.wrkDir = wrkDir;
    // Constructor
    vectorOfVectors= new Vector<Vector>(1,1);
    nDiscard=n;
  }
  
  // -------------------------------------------------------------
  private void getArrayFromVector(){
    // Transforms vectorOfVectors into a proper array.
    // This array, topoArray, is a string array in which the
    // nth row vector shows all the topology strings of
    // the nth simulation (the nth window position).
    int i,k,nColsTest;
    vectorOfVectors.trimToSize();
    int nRows= this.vectorOfVectors.size();
    Vector topoVector= (Vector) this.vectorOfVectors.elementAt(0);
    int nCols= topoVector.size();
    this.topoArray= new String[nRows][nCols];

    for (i=0; i<nRows; i++){
      topoVector= (Vector) this.vectorOfVectors.elementAt(i);
      nColsTest= topoVector.size();
      if (nColsTest != nCols){
	// Test that all column numbers are the same.
	// Otherwise terminate with error message.
    	System.out.println("nColsTest != nCols");
	for (k=0; k<nRows; k++){
	  topoVector= (Vector) this.vectorOfVectors.elementAt(k);
	  nCols = topoVector.size();
	  System.out.print(nCols+" ");
	}
    	System.exit(0);
      }
      for (k=0; k<nCols; k++){
	this.topoArray[i][k]= (String) topoVector.elementAt(k);
	//System.out.print(this.topoArray[i][k]+"  ");
      }
      //System.out.println();
    }
    System.out.println("topoArray has "+nRows+" rows and "+nCols+" columns."); 
  }

  // -------------------------------------------------------------
	// Read topologies from input file and store them in vector topoStrings
	private boolean readTopos(File filename)
		throws Exception
	{
		BufferedReader in = new BufferedReader(new FileReader(filename));
		
		// Reset the vector used to store the data
		topoStrings = new Vector<String>(1, 1);
		
		// Read in the topology strings
		String str = in.readLine();
		
		for (int i = 1; str != null; i++)
		{
			if (i > nDiscard)
				topoStrings.addElement(str);
			
			str = in.readLine();			
		}
		
		in.close();
		return true;
	}
	
	// If true: adds another vector of topologies to vectorOfVectors
	// If false: appends another vector of topologies to a file on disk
public 	boolean addTopo(boolean writeToFile)
	throws Exception
	{		
		if (!readTopos(new File(wrkDir, "run1.top")))
		{
			return false;
		}
			
		topoStrings.trimToSize();
		
		if (writeToFile)
			return printTopos();
		else
			vectorOfVectors.addElement(topoStrings);
		
		return true;
	}


  // -------------------------------------------------------------
  public String[][] showTopos(){
    this.getArrayFromVector();
    
 /*   System.out.println();
    for (int i = 0; i < topoArray.length; i++)
    {
    	for (int j = 0; j < topoArray[i].length; j++)
    		System.out.print(topoArray[i][j] + " ");
    	System.out.println("");
    }
    System.out.println(topoArray);
    System.out.println();
 */   
    return topoArray;
  }

  // -------------------------------------------------------------
  void addWindowedTopos(int windowSize, int stepSize)
  	throws Exception
  {
    // ASIDE (for equilibration):
    // Break up a set of topology strings into several
    // (possibly overlapping) windows.
    // Store these subsets in vectorOfVectors.
    // vectorOfVectors can be transformed into the integer
    // array topoArray with getArrayFromVector.
    // Application: A single MCMC run, where you want to study
    // the evolution of the probability distribution over 
    // topologies.
    Vector<String> windowTopoStrings= new Vector<String>(1,1);
    int i, iStart, iEnd;

    // Read in  data
    this.readTopos(new File(wrkDir, "run1.top")); //That gets --> topoStrings
    iEnd=topoStrings.size();

    // Make sure parameter choice is sensible
    if (iEnd<=windowSize){
    	throw new JambeException("ET01: Wrong parameters for addWindowedTopos");
    }

    for (iStart=0; iStart<=iEnd-windowSize; iStart=iStart+stepSize){
      windowTopoStrings= new Vector<String>(1,1);
      for (i=iStart;i<iStart+windowSize;i++){
	windowTopoStrings.addElement (topoStrings.elementAt(i));
      }
      vectorOfVectors.addElement(windowTopoStrings);
    }
   }

	// Appends the topology array for the current iteration to resultsAllTopos
	private boolean printTopos()
		throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(
			new File(wrkDir, "resultsAllTopos.out"), true));
		
		for (int i = 0; i < topoStrings.size(); i++)
			out.write(topoStrings.elementAt(i) + "  ");
		
		out.newLine();
		out.close();
		
		return true;
	}

 // -------------------------------------------------------------
/*  private void printTopos(String fileName){
    // --> Temporary version: Needs to be improved !!
    // Print out the topology array for the current iteration to 
    // a file with the specified name. If this file already exist,
    // the program terminates and an error message is returned 
    // (this makes sure that results are written out to a new file).
    // Since I have not yet found a way to write to the end
    // of a file, this method calls UNIX shell commands
    // to concatenate files.
    

 //   if (fileName != "stercus"){
   //   System.err.println("At current, the file name must be stercus");
//      System.exit(0);
//    }
    
    File oFile;
    //FileWriter oFileWriter=null; (not deprecated, but doesn't work)
    FileOutputStream oFileWriter=null;
    //PrintWriter oPrintWriter=null; (not deprecated, but doesn't work)
    PrintStream oPrintWriter=null;
    oFile= new File("resultsAllTopos.out");
 //   if (!oFile.exists()){
   ///    System.err.println("ERROR_03: Create resultsAllTopos.out");
//       System.exit(0);
//    }

    // Write results out to file
    System.out.println("Open file "+fileName);
    try{
	//oFileWriter= new FileWriter(fileName); (not deprecated, but doesn't work)
	oFileWriter= new FileOutputStream(fileName);
	//oPrintWriter= new PrintWriter(oFileWriter); (not deprecated, but doesn't work)
	oPrintWriter= new PrintStream(oFileWriter);
    } catch (IOException oException){
	  System.err.println("ERROR_04: Could not open output file");
	  System.exit(0);
    }
    for (int i=0; i<topoStrings.size(); i++){
      oPrintWriter.print(topoStrings.elementAt(i)+"  ");
    }
    oPrintWriter.println();  

    oPrintWriter.close();
    try{
      oFileWriter.close();
    } catch (IOException ex) {
	System.out.println("ERROR_05 in readTopos: Couldn't close file");
	System.exit(0);
    } 

    // --------- Use UNIX to write to end of file -----

	try
    {
    	BufferedReader in = new BufferedReader(new FileReader(Prefs.mcmc_scratch + Prefs.sepF+"stercus"));
    	BufferedWriter out = new BufferedWriter(new FileWriter("resultsAllTopos.out", true));
    	
  //  	out.newLine();
    	int read = in.read();
    	while (read != -1)
    	{
    		out.write(read);
    		read = in.read();
    	}
    	

    	out.close();
    	in.close();
    	
   // 	System.out.println("Delete stercus: " + new File(Prefs.mcmc_scratch + Prefs.sepF+"stercus").delete());
    }
    catch (Exception e)
    {
    	System.out.println("cat stercus: "+e);
    }
/*
   Runtime myRuntime = Runtime.getRuntime();
   Process proc=null;
   try {
     try{
      proc= myRuntime.exec(new String[] {"/bin/sh","-c","cat resultsAllTopos.out stercus > stercus2"});
      proc.waitFor();
      proc= myRuntime.exec(new String[] {"/bin/sh","-c","mv stercus2 resultsAllTopos.out"});
      proc.waitFor();
      proc= myRuntime.exec(new String[] {"/bin/sh","-c","rm stercus"});
      proc.waitFor();
     }catch (InterruptedException interuptex){
      System.out.println("IOException: " + interuptex.toString());
      System.exit(0);
     }
   } catch (IOException e) {
     System.out.println("IOException: " + e.toString());
     System.exit(0);
   }
   */
//  }

// ----------------------- END ---------------------------------
}
// -------------------------------------------------------------





