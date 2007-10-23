// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.io.*;

public class StringOutputStream extends OutputStream
{
	protected StringBuffer buf = new StringBuffer();

	public StringOutputStream() {}

	@Override
	public void write(int b) throws IOException
	{
		this.buf.append((char)b);
	}

	public String toString() {
		return buf.toString();
	}
	
}
