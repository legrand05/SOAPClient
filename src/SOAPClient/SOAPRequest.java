package SOAPClient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

/**
 * A simpler SOAP request builder using javax soap library.
 * 
 * @author leGrand mondesir
 * 
 * 2015.09.09
 *
 */
public class SOAPRequest {
	private SOAPMessage requestMessage;
	private SOAPEnvelope soapEnvelope;
	private String url;
	private Map<String, String> localNameSpaces = new HashMap<String, String>();
	private static SOAPConnectionFactory soapFactory;
	
	public SOAPRequest(String url, String... nameSpaces) throws UnsupportedOperationException, SOAPException, MalformedURLException {
		MessageFactory messageFactory = MessageFactory.newInstance();
		requestMessage = messageFactory.createMessage();
		SOAPPart soapPart = requestMessage.getSOAPPart();
		soapEnvelope = soapPart.getEnvelope();
		this.url = url;
		
		if (soapFactory == null) soapFactory = SOAPConnectionFactory.newInstance();
		
		addNameSpace(nameSpaces);
	}

	public void addNameSpace(String... nameSpaces) throws SOAPException {
		for (int i = 0, len=nameSpaces.length; i <  len; i+=2) {
			soapEnvelope.addNamespaceDeclaration(nameSpaces[i], nameSpaces[i+1]);
		}
	}
	
	public void addLocalNameSpace(String... nameSpaces) {
		for (int i = 0, len=nameSpaces.length; i <  len; i+=2) {
			localNameSpaces.put(nameSpaces[i], nameSpaces[i+1]);
		}
	}
	
	public SOAPBuilder getBody() throws SOAPException {
		SOAPBody body = soapEnvelope.getBody();
		if (body == null) soapEnvelope.addBody();
		
		return new SOAPBuilder(body, localNameSpaces);
	}

	public SOAPBuilder getHeader() throws SOAPException, Exception {
		SOAPHeader header = soapEnvelope.getHeader();
		if (header==null) soapEnvelope.addHeader();
		
		return new SOAPBuilder(header, localNameSpaces);
	}
	
	public Response send() {
		SOAPMessage response = null;
		SOAPBuilder body = null;
		
		try {
			requestMessage.saveChanges();
		
			SOAPConnection soapConnection = soapFactory.createConnection();
			response = soapConnection.call(requestMessage, new URL(url));
			soapConnection.close();
		
			body = new SOAPBuilder(response.getSOAPBody(), localNameSpaces).getBuilder("Body");
		} catch (Exception e) {
			return Status.FATAL_ERROR.updateResult(getStackTrace(e));
		}
		
		if (body == null) return Status.EMPTY_BODY.updateResult(getMessageAsString(response));
		
		return Status.SUCCESS.updateResult(null, body);
	}
	
	public String toString() {
		return getMessageAsString(requestMessage);
	}

	private String getMessageAsString(SOAPMessage message) {
		final StringBuilder builder = new StringBuilder();
		try {
			message.writeTo(new OutputStream() {
				
				@Override
				public void write(int arg0) throws IOException {
					builder.append((char)arg0);
				}
			});
		} catch (Exception e) {	}
		
		return builder.toString();
	}
	
	private static String getStackTrace(Exception e) {
		final StringBuilder builder = new StringBuilder(512);
		
		if (e.getCause()==null) {
			builder.append(e.getMessage()).append("\n");
		} else {
			builder.append(e.getCause().getMessage()).append("\n");
		}
		
		e.printStackTrace(new PrintStream(new OutputStream() {
			
			@Override
			public void write(int arg0) throws IOException {
				builder.append((char)arg0);
				
			}
		}));
		
		return builder.toString();
	}

	public static class Response {
		private Status status;
		private SOAPBuilder result;
		private String error;
		private String errorCode;
		
		protected Response(Status status) {
			this.status = status;
		}
		
		protected Response(Status status, String message, SOAPBuilder result) {
			this.status = status;
			this.error  = message;
			this.result = result;
		}

		protected Response updateErrMessage(String message) {
			error = message;
			return this;
		}

		protected Response updateErrMessage(Exception e) {
			error = getStackTrace(e);
			return this;
		}

		protected Response updateErrMessage(Object o) {
			if (o != null) error = o.toString();
			
			return this;
		}
		protected Response updateErrCode(String o) {
			errorCode = o;
			
			return this;
		}
		
		protected Response updateResult(String message, SOAPBuilder result) {
			this.error = message;
			this.result = result;
			
			return this;
		}

		public String getErrorCode() {
			return errorCode;
		}
		public String getError() {
			return error;
		}

		public SOAPBuilder getResult() {
			return result;
		}

		public boolean isSuccessful() {
			return this.status == Status.SUCCESS;
		}
		
		public boolean equals(Status status) {
			return this.status == status;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Response)) return false;
			
			return status == ((Response)obj).status;
		}
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder(50 + (error==null ? 0 : error.length())).append(status.toString());
			
			if (errorCode != null) result.append("(").append(errorCode).append(")");
			
			if (error != null)   result.append("\n").append(error);
			
			return result.toString();
		}
	
	}
	
	public static enum Status {

		INVALID_CRUDENTIALS, NOT_AUTHORIZED, FATAL_ERROR, UNKNOWN_SERVICE_ERROR, EMPTY_BODY, SUCCESS;

		public Response updateResult(String message, String errorCode) {
			return new Response(this).updateErrMessage(message).updateErrCode(errorCode);
		}

		public Response updateResult(Exception e) {
			return new Response(this).updateErrMessage(e);
		}

		public Response updateResult(Object e, String errorCode) {
			if (e != null) return new Response(this).updateErrMessage(e.toString()).updateErrCode(errorCode);
			
			return new Response(this).updateErrCode(errorCode);
			
		}
		
		public Response updateResult(Object e) {
			if (e != null) return new Response(this).updateErrMessage(e.toString());
			
			return new Response(this);
		}
		
		public Response updateResult(String message, SOAPBuilder result) {
			return new Response(this, message, result);
		}

		public Response getResponse() {
			return new Response(this);
		}

	}

}
