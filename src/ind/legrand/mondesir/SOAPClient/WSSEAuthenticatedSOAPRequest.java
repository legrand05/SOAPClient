package ind.legrand.mondesir.SOAPClient;

import java.net.MalformedURLException;

import javax.xml.soap.SOAPException;

public class WSSEAuthenticatedSOAPRequest extends SOAPRequest {
	public static final String WSSE_NS = "wsse";
	public static final String WSU_NS = "wsu";
	
	public static final String WSSE_URL = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
	public static final String WSU_URL = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
	public static final String PWD_TYPE_URL = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";

	public static final String INVALID_CRUDENTIAL_ERR_CODE = "BEA-386200";
	public static final String NOT_AUTHORIZED_ERR_CODE = "BEA-386102";
		
	public WSSEAuthenticatedSOAPRequest(String user, String pwd, String url, String... nameSpaces) throws UnsupportedOperationException, SOAPException, MalformedURLException, Exception {
		super(url, nameSpaces);

		SOAPBuilder header = super.getHeader();
		addLocalNameSpace(WSU_NS, WSU_URL, WSSE_NS, WSSE_URL);
		
		header
		.addObject("Security", WSSE_NS)
			.addObject("UsernameToken", WSSE_NS, "Username", user).addAttributeWNS("Id", "UsernameToken-3", WSU_NS)
				.addObject("Password",  WSSE_NS).addAttribute("Type", PWD_TYPE_URL).setValue(pwd);
	}
	
	@Override
	public Response send() {
		Response response = super.send();
		SOAPBuilder result = response.getResult();
		
		if (result == null) return response;
		
		if (result.hasValue("Fault")) {
			String errorCode = result.getStringValue("Fault", "Detail", "fault", "errorCode");
	
			if (NOT_AUTHORIZED_ERR_CODE.equalsIgnoreCase(errorCode)) return Status.NOT_AUTHORIZED.getResponse();
			
			if (INVALID_CRUDENTIAL_ERR_CODE.equalsIgnoreCase(errorCode)) return Status.INVALID_CRUDENTIALS.getResponse();
		
			if (errorCode == null) return Status.UNKNOWN_SERVICE_ERROR.updateResult(result.toXMLString("Fault"));
				
			return Status.UNKNOWN_SERVICE_ERROR.updateResult(result.getStringValue("Fault", "Detail", "fault", "reason"), errorCode);
		}

		return response;
	}
}
