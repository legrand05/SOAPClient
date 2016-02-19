package ind.legrand.mondesir.SOAPClient;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Builder for SOAP XML tree structure.
 * 
 * @author leGrand mondesir
 * 
 * 2015.09.09
 *
 */
public class SOAPBuilder {
	
	private SOAPElement element;
	private SOAPBuilder parent = this;
	private Object data;
	private Map<String, String> localNameSpaces;
	
	SOAPBuilder(SOAPElement element, Map<String, String> localNameSpaces) {
		this.element = element;
		this.localNameSpaces = localNameSpaces;
	}

	SOAPBuilder(SOAPElement element, Map<String, String> localNameSpaces, SOAPBuilder parent) {
		this(element, localNameSpaces);
		this.parent = parent;
	}

	private SOAPBuilder(Object data) {
		this.data = data;
	}

	/**
	 * Add a root element along with child elements and there values.
	 * 
	 * Example:
		  addObject("UpdatePaymentStatusRequest", "mes", 
		 				"NewStatus", "APPROVED",
		 				"PaymentIdentifier", "12345");
		 
		 Generates the following xml string:
		 
		 <mes:UpdatePaymentStatusRequest> 
			<mes:NewStatus>APPROVED</mes:NewStatus>
			<mes:PaymentIdentifier>12345</mes:PaymentIdentifier>
		 </mes:UpdatePaymentStatusRequest> 
	 *
	 * @param objectName - root element's name
	 * @param nameSpace
	 * @param argValues - an even numbered String array of 
	 * @return
	 * @throws Exception 
	 */
	public SOAPBuilder addObject(final String objectName, final String nameSpace, String... argValues) throws Exception {
		final String localNSURL = localNameSpaces.remove(nameSpace);
		SOAPElement object;
		if (localNSURL!=null) {
			object = element.addChildElement(objectName, nameSpace, localNSURL);
		} else {
			object = element.addChildElement(objectName, nameSpace);
		}
		int len = argValues.length;
		
		if (len%2!= 0) throw new Exception("Expecting 'argValues' length to be even");
		
		
		for (int i = 0; i <  len; i+=2) {
			SOAPElement arg = object.addChildElement(argValues[i], nameSpace);
			arg.addTextNode(argValues[i+1]);
		}
		
		return new SOAPBuilder(object, localNameSpaces, this);
	}

	/**
	 * 
	 * Adds child elements and there values referenced by current root element.
	 * 
	 * @param attributes - child elements and values to be added
	 * @return current SOAPBuilder (added child elements parent)
	 * 
	 * @throws SOAPException
	 * @throws Exception
	 */
	public SOAPBuilder addAttributeWNS(String... attributes ) throws SOAPException, Exception {
		int len = attributes.length;
	
	
		if (len%3!= 0) throw new Exception("Expecting 'attributes' length to be multiple of three");
		
		for (int i = 0; i <  len; i+=3) {
			String ns = attributes[i+2];
			String url = localNameSpaces.get(ns);
			
			if (url == null) continue;
			
			element.addNamespaceDeclaration(ns, url);
			element.addAttribute(element.createQName(attributes[i], ns), attributes[i+1]);
		}
		
		return this;
	}
	
	/**
	 * Add xml attributes to element referenced by current SOAPBuilder
	 * 
	 * Example:
	 * 
		  Current xml element is <password><password>
		  
		  After calling this method:
			  addAttribute(
			    "Type", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText", 
				"Id",    "UsernameToken-3)
				
		 Generates the following xml string:
		  	 <password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText, Id="UsernameToken-3"><password>
	 
	 * @param attributes attributes to add to current xml element referenced by this SOAPBuilder
	 * 
	 * @return current SOAPBuilder
	 * @throws SOAPException
	 * @throws Exception
	 */
	public SOAPBuilder addAttribute(String... attributes) throws SOAPException, Exception {
		int len = attributes.length;
	
		if (len%2!= 0) throw new Exception("Expecting 'attributes' length to be even");
		
		for (int i = 0; i <  len; i+=2) {
			element.addAttribute(new AttributeName(attributes[i]), attributes[i+1]);
		}
		
		return this;
	}
	
	/**
	 * Set current node value referenced by current SOAPBuilder to incoming value.
	 * 
	 * @param value - value to set current 
	 * @return current SOAPBuilder
	 * @throws SOAPException
	 */
	public SOAPBuilder setValue(String value) throws SOAPException {
		element.addTextNode(value);
		return this;
	}
	
	public SOAPBuilder addObjectToList(String objectName, String nameSpace, String... argValues) throws Exception {
		addObject(objectName, nameSpace, argValues);
		
		return this;
	}
	
	
	public SOAPBuilder addList(String listName, String nameSpace) throws SOAPException {
		SOAPElement object = element.addChildElement(listName, nameSpace);
		
		return  new SOAPBuilder(object, localNameSpaces, this);
	}
	
	/**
	 * String this SOAPBuilder as a xml formated string
	 * 
	 * @param keys
	 * @return
	 */
	public String toXMLString(String... keys) {
		Object result = getValue(keys);
		
		if (result == null) return null;
		
		StringBuilder builder = new StringBuilder(64);
		String wrapper = null;
		
		int keyLength = keys.length;
		if (keyLength >= 1) {
			wrapper = keys[keyLength-1];
			try {
				Integer.parseInt(wrapper);
				
				if (keyLength < 2) return null;
				
				wrapper = keys[keyLength-2];
			} catch (Exception e) {}
				
			builder.append("<").append(wrapper).append(">");
			
			if (!(result instanceof String)) builder.append("\n");
		}
		
		getXMLString(builder, result, wrapper==null ? "" : "\t");
		
		if (wrapper!=null) builder.append("</").append(wrapper).append(">").append("\n");
		
		return builder.toString();
	}
	
	@SuppressWarnings("unchecked")
	private void getXMLString(StringBuilder builder, Object data, String indent) {
		
		if (data instanceof String) builder.append((String)data);
		
		if (data instanceof Map) {
			Map<String, Object> mData = (Map<String, Object>)data;
			Iterator<Entry<String, Object>> it = mData.entrySet().iterator(); 
			
			while (it.hasNext()) {
				Entry<String, Object> entry = it.next();
				String key = entry.getKey();
				Object value = entry.getValue();
				
				if (value instanceof String) {
					builder.append(indent).append("<").append(key).append(">").append((String)value).append("</").append(key).append(">").append("\n");
				} else if (value instanceof Map) {
					builder.append(indent).append("<").append(key).append(">").append("\n");
					getXMLString(builder, value, indent+"\t");
					builder.append(indent).append("</").append(key).append(">").append("\n");
				} else if (value instanceof List) {
					for (Object v : (List<Object>)value) {
						builder.append(indent).append("<").append(key).append(">").append("\n");
						getXMLString(builder, v, indent+"\t");
						builder.append(indent).append("</").append(key).append(">").append("\n");
					}
					
				}
				
			}
		}
		
	}
	
	public boolean hasValue(String... keys) {
		return getValue(keys) != null;
	}
	
	public String getStringValue(String... keys) {
		Object result = getValue(keys);
		
		if (result instanceof String) return (String)result;
		
		return null;
	}
	
	public SOAPBuilder getBuilder(String... keys) {
		Object result = getValue(keys);
		
		if (result instanceof Map) return new SOAPBuilder(result);
		
		return null;
	}
	
	/**
	 * Return xml element value.
	 * 
	 * Example:
	 *    SOAPBuilder builds this xml structure
	 *    <Security>
			<UsernameToken>
				<Username>leGrand</UserName>
			</UsernameToken>
			
	      </Security>
	      
	      Calling getValue("Security", "UsernameToken", "Username")
	      
	      Returns : leGrand
	 * 
	 * @param keys - path to child element node name
	 * @return xml element value
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getValue(String... keys) {
		if (data == null) {
			data = new HashMap<String, Object>();
			getData((HashMap<String, Object>) data, element);
		}
		
		Object last = data;
		for (int i = 0, len = keys.length; i < len; i++) {
			if (last == null) return null;
			
			if (last instanceof Map) {
				last = ((Map)last).get(keys[i]);
				
			} else if (last instanceof List) {
				String key = keys[i];
				int index = 0;
				try {
					index = Integer.parseInt(key);
					List list = ((List)last);
					
					if (index >= list.size() || index < 0) return null;
					
					last = ((List)last).get(index);
				} catch(Exception e) {
					return null;
				}
			} else {
				return null;
			}
		}
		
		return last;
	}
	
	public SOAPBuilder getParent() {
		return parent;
	}
	
	public String toString() {
		return toXMLString();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void getData(Map<String, Object> map, Node e) {
		if (!e.hasChildNodes()) return;
		
		NodeList childNodes = e.getChildNodes();
		String localName = e.getLocalName();
		int numChild = childNodes.getLength();
		Node fChild = e.getFirstChild();
		
		if (numChild==1 && !fChild.hasChildNodes()) {
			String nodeValue = fChild.getNodeValue();
			Object oldVal = map.put(localName, nodeValue);
			if (oldVal != null) {
				 if (oldVal instanceof List) {
					 ((List) oldVal).add(nodeValue);
					 map.put(localName, oldVal);
				 } else {
					 // list of strings
					 ArrayList<Object> list = new ArrayList<Object>(4);
					 list.add(oldVal);
					 list.add(nodeValue);
					 map.put(localName, list);
				 }
			 }
		} else {
			 HashMap<String, Object> child = new HashMap<String, Object>();

			 Object oldVal = map.put(localName, child);
			 if (oldVal != null) {
				 if (oldVal instanceof List) {
					 ((List) oldVal).add(child);
					 map.put(localName, oldVal);
				 } else {
					 // list of objects
					 ArrayList<Object> list = new ArrayList<Object>(4);
					 list.add(oldVal);
					 list.add(child);
					 map.put(localName, list);
				 }
			 }
			 
			 for (int i = 0; i < numChild; i++) {
				 Node childNode = childNodes.item(i);
				 fChild = childNode.getFirstChild();
				 
				 if (fChild == null) {
					 // empty; <name></name>
					 if (childNode.getNodeType() == Node.ELEMENT_NODE) child.put(childNode.getLocalName(), "");
					 
					 continue;
				 }
				 
				 getData(child, childNode);
			 }
		}
		
	}

	private static class AttributeName implements Name {

		private String name;

		public AttributeName(String name) {
			this.name = name;
		}
		
		@Override
		public String getLocalName() {
			return "";
		}

		@Override
		public String getPrefix() {
			return "";
		}

		@Override
		public String getQualifiedName() {
			return name;
		}

		@Override
		public String getURI() {
			return "";
		}
		
	}
}
