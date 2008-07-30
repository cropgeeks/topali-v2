// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

public class XSLTransformer
{
	
	public static void transform(InputStream xmlStream, InputStream xslStream, OutputStream resultStream) throws Exception{
		Source xmlSrc = new StreamSource(xmlStream);
		Source xslSrc = new StreamSource(xslStream);
		Result result = new StreamResult(resultStream);
		Transformer trans = TransformerFactory.newInstance().newTransformer(xslSrc);
		trans.transform(xmlSrc, result);
	}
}
