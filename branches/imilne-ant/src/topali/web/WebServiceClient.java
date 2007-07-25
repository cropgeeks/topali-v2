/*
 * package topali.web;
 * 
 * import java.io.*; import javax.xml.namespace.QName; import
 * javax.xml.rpc.ParameterMode;
 * 
 * import org.apache.axis.*; import org.apache.axis.client.*; import
 * org.apache.axis.encoding.*; import org.apache.axis.attachments.*;
 * 
 * public abstract class WebServiceClient { // Axis Call object used to
 * communicate with the web service protected Call call = null;
 *  // Tracks the last exception that was caught public Exception exception =
 * null;
 * 
 * protected void setCall(String url, String qname, String methodName) throws
 * Exception { if (call == null) { Service service = new Service(); call =
 * (Call) service.createCall();
 * 
 * call.setTargetEndpointAddress(new java.net.URL(url));
 * call.setMaintainSession(true); call.setTimeout(60000); }
 * 
 * call.setOperationName(new QName(qname, methodName)); } }
 */