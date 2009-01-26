// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

public class ASCIIGraph {

    int rows;
    int cols;
    double[][] data;
    double threshold;

    public ASCIIGraph(double[][] data, double threshold, int rows, int cols) {
	this.data = data;
	this.threshold = threshold;
	this.rows = rows;
	this.cols = cols;
    }

    public String plot() {
	
	//init display
	char[][] plot = new char[rows][cols];
	for (int i = 0; i < rows; i++) {
	    for (int j = 0; j < cols; j++)
		plot[i][j] = ' ';
	}

	//determine max values
	double maxX = 0, maxY = 0;
	for (int i = 0; i < data.length; i++) {
	    if (data[i][0] > maxX)
		maxX = data[i][0];
	    if (data[i][1] > maxY)
		maxY = data[i][1];
	}
	if(threshold>0 && threshold>maxY)
	    maxY = threshold;

	//determine increments
	double xInc = maxX / cols;
	double yInc = maxY / rows;

	//draw threshold line
	if(threshold>0) {
	    int l = rows - (int) (threshold/yInc);
	    for(int i=0; i<cols; i++) {
		if(l<rows)
		    plot[l][i] = '-';
	    }
	}
	
	//plot data
	for (int i = 0; i < data.length; i++) {
	    int k = (int) (data[i][0] / xInc);
	    int l = rows - (int) (data[i][1] / yInc);

	    if (l < rows && k < cols)
		plot[l][k] = '*';
	}

	//draw legend and plot
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < rows; i++) {
	    String prefix = fillup("", ' ', 20)+"|";
	    if(i==0 || i%10==0) {
		prefix = String.valueOf((rows - i)*yInc);
		prefix = fillup(prefix, ' ', 20)+"-";
	    }
	    String line = new String(plot[i]);
	    line = prefix + line;
	    sb.append(line + "\n");
	}
	String bottom1 = fillup("", ' ', 20);
	String bottom2 = fillup("", ' ', 20);
	int l = 0;
	for(int i=0; i<cols; i++) {
	    if(i%20==0) {
		bottom1 += '|';
		String tmp = String.valueOf(i*xInc);
		bottom2 += tmp;
		l = tmp.length();
	    }
	    else {
		bottom1 += '-';
		if(--l < 1)
		    bottom2 += ' ';
	    }
	}
	sb.append(bottom1+"\n");
	sb.append(bottom2+"\n");
	return sb.toString();
    }

    private String fillup(String s, char c, int length) {
	while (s.length() < length) {
	    s += c;
	}
	return s;
    }
}
