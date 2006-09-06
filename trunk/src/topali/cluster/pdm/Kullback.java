// (C) 2003-2006 Dirk Husmeier & Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm;

// Read in files results_entropy.java and results_histo.java.
// The latter file is a matrix in which the rows represent
// different positions of the moving window, and the columns
// represent the probabilities for the various tree topologies.
// From this, compute two Kullback-Leibler measures.
// -- 1 --
// Compute the average probability distribution, averaged over
// all the rows. Call this distribution q, whereas the 
// distribution at row t (that is, window position t) is p_t.
// The Kullback-Leibler divergence between q and p_t is
// given by KL(p_t,q)= H(p_t,q)-H(p_t). Here, the first term
// is the cross entropy between p_t and q, and the second term 
// is the entropy of p_t, which is read in from file 
// results_entropy.out.
// -- 2 --
// Let r be a sharply peaked distribution, r(i)= delta_{iI}, 
// where I is the index of the best topology:
// I =argmax{q}. The Kullback-Leibler divergence between 
// r and p_t is given by: KL(r,p_t)= -ln p_t(I)
//
// Argument:
// The constructor takes as an argument on object of class
// Entropy, which contains the probability distributions
// of topologies (histogram) and the array of entropies.

import java.io.*;


public class Kullback{

  private float epsilon= (float) 0.000001;
  private float[] entropy;
    // Array of entropies computed from the histogram matrix
  private float[][] histogram;
    // Histogram matrix. Rows represent mean postions in the alignment; colums represent different topologies of the tree.
  private int nWindows;
    // Number of rows in entropy and histogram, which is the
    // number of window positions (=sections in the alignment).
  private int nTopos;
    // Number of different topologies.
  private float[] histogramAveraged;
    // Average histogram, averaged over all window positions
  private int bestTopo=-1;
    // Best topology= argument of the mode of the 
    // average distribution.
    // Note that arrays in Java strat with 0, so if the mth element
    // is the mode of the average distribution, bestTopo=m-1.
  private float[] kullbackMode;
  private float[] kullbackMean;
  
  private File wrkDir; 

  // --------------------------------------------------------
  public Kullback(File wrkDir, Entropy oEntropy) throws JambeException
  {
  	this.wrkDir = wrkDir;
  	
    // Constructor
    nTopos= oEntropy.showNTopos();
    nWindows= oEntropy.showNWindowPositions();
    histogram= new float[nWindows][nTopos];
    histogram= oEntropy.returnHistogram();
    entropy= new float[nWindows];
    entropy= oEntropy.returnEntropy();
    histogramAveraged= new float[nTopos];
    kullbackMode= new float[nWindows];
    kullbackMean= new float[nWindows];
    // Error checking
    if (nTopos <= 0 || nWindows <= 0){
    	throw new JambeException("KB01: Wrong parameters in constructor of "
    		+ "Kullback.");
    }
  }

  // --------------------------------------------------------
  public void computeAverageDistribution(){
    // Compute the average distribution from the array histogram
    // and assign this distribution to the global array
    // histogramAveraged. 
    for (int nTopo=0; nTopo<nTopos; nTopo++){
      histogramAveraged[nTopo]=0;
      for (int t=0; t<nWindows; t++){
	histogramAveraged[nTopo]+=histogram[t][nTopo];
      }
      histogramAveraged[nTopo] /= nWindows;
      if (histogramAveraged[nTopo]<=epsilon)
	 histogramAveraged[nTopo]=epsilon;
      // To prevent overflow in the computation of the KL distance.
    }
    bestTopo=0; 
    // Set label to indicate that histogramAveraged
    // has been computed.
  }

  // --------------------------------------------------------
  public void findBestTopo() throws JambeException
  {
    // Find the best tree topology, where "best" is the 
    // argument of the mode of the average distribution.
    // This method requires that the method "averageDistribution"
    // has been called before.
    // The method updates the global variable bestTopo.
    if (bestTopo==-1){
      // Check that computeAverageDistribution() has been
      // invoked prior to the call of this method. 
      throw new JambeException("KB02: Method findBestTopo must be called after "
      	+ "invocation of computeAverageDistribution.");
    };
    float pBestTopo=0;
    for (int nTopo=0; nTopo<nTopos; nTopo++){
      if (histogramAveraged[nTopo]>pBestTopo){
	bestTopo=nTopo;
	pBestTopo=histogramAveraged[nTopo];
      }
    }
  }

  // --------------------------------------------------------
  public void showBestTopo(){
    System.out.println("Best topology=  "+(bestTopo+1));
    // +1, because arrays in Java start at the index 0
    System.out.println("Probability of the best topology=  "+histogramAveraged[bestTopo]);
  }

  // --------------------------------------------------------
  public void computeKullbackMean(){
    // Compute the 1st Kullback-Leibler divergence; see
    // the information in the header under  "-- 1 --".
    float crossEntropy_t,p_nTopo,q_nTopo;
    for (int t=0; t<nWindows; t++){
      crossEntropy_t=0;
      for (int nTopo=0; nTopo<nTopos; nTopo++){
	p_nTopo= histogram[t][nTopo];
	q_nTopo= histogramAveraged[nTopo];
	crossEntropy_t -=p_nTopo*Math.log(q_nTopo);
      }
    kullbackMean[t]= crossEntropy_t- entropy[t];
    } 
  }

  // --------------------------------------------------------
  public void computeKullbackMode(){
    // Compute the 2nd Kullback-Leibler divergence; see
    // the information in the header under  "-- 2 --".
    for (int t=0; t<nWindows; t++){
      if (histogram[t][bestTopo]<=epsilon)
	histogram[t][bestTopo]=epsilon;
        // to prevent numerical overflow
      kullbackMode[t]= -(float)Math.log(histogram[t][bestTopo]);
    } 
  }

  // --------------------------------------------------
  public void printKullbackMean(File fileName) throws JambeException
  {
    // Print out Kullback-Leibler divergence between p and the
    // averaged distribution to the file with the name specified in 
    // the argument.
    try{
      FileOutputStream outFile = new FileOutputStream(fileName);
      PrintStream outStream= new PrintStream(outFile);
      for (int i=0; i<nWindows; i++){
	outStream.println(kullbackMean[i]);
      }	
      
      outStream.close();
      outFile.close();
      System.out.println("Kullback-Leibler divergences between p and the averaged distributionare in file "+fileName);
    } catch (IOException e) {
    	throw new JambeException("KB03: Unable to open file " + fileName
    		+ " for writing: " + e);
    }
  }

	public float[] getKullbackMean()
	{
		return kullbackMean;
	}


  // --------------------------------------------------
  public void printKullbackMode(File fileName)  throws JambeException
  {
    // Print out Kullback-Leibler divergence between p and the
    // mode of the averaged  distribution to the file with the 
    // name specified in the argument.
    try{
      FileOutputStream outFile = new FileOutputStream(fileName);
      PrintStream outStream= new PrintStream(outFile);
      for (int i=0; i<nWindows; i++){
	outStream.println(kullbackMode[i]);
      }	
      
      outStream.close();
      outFile.close();
      System.out.println("Kullback-Leibler divergences  between p and the  mode of the averaged  distribution are in file "+fileName);           
    } catch (IOException e) {
    	throw new JambeException("KB04: Unable to open file " + fileName
    		+ " for writing: " + e);
    }
  }

  // --------------------------------------------------------
  public void doItAll() throws JambeException
  {
    this.computeAverageDistribution();
    this.findBestTopo();
    this.showBestTopo();
    this.computeKullbackMean();
    this.computeKullbackMode();
    this.printKullbackMean(new File(wrkDir, "results_kullback_mean.out"));
    this.printKullbackMode(new File(wrkDir, "results_kullback_mode.out"));
  }

// ----------------- The End --------------------------------
}

