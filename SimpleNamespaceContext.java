package com.wellsfargo.transferandpay.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
/**
 * Platform-X WB Helper Class for parsing XML to support Namespaces defined in XML
 * 
 * @author Karthiga Baskaran
 */
public class SimpleNamespaceContext implements NamespaceContext {

	    private final Map<String, String> PREF_MAP = new HashMap<String, String>();

	    public SimpleNamespaceContext(final Map<String, String> prefMap) {
	        PREF_MAP.putAll(prefMap);       
	    }

	    public String getNamespaceURI(String prefix) {
	        return PREF_MAP.get(prefix);
	    }

	    public String getPrefix(String uri) {
	        throw new UnsupportedOperationException();
	    }

	    @SuppressWarnings("rawtypes")
		public Iterator getPrefixes(String uri) {
	        throw new UnsupportedOperationException();
	    }

	}