package com.wellsfargo.transferandpay.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import junit.framework.AssertionFailedError;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;


import static org.hamcrest.CoreMatchers.equalTo;
/**
 * Platform-X WB Helper Class for BEV Events
 * 
 * @author Karthiga Baskaran
 */
public class BEVEventJunitHelpers {

	//BEV Builder log filter 
	//TODO: Currently it is not working and need to fix it
	private LoggerFilter filter = new LoggerFilter();
	// Captor is generalized with ch.qos.logback.classic.spi.LoggingEvent
	private ArgumentCaptor<LoggingEvent> captorLoggingEvent = ArgumentCaptor.forClass(LoggingEvent.class);
	//Spy Log Appenders
	@SuppressWarnings("unchecked")
	private Appender<ILoggingEvent> mockAppender = Mockito.spy(Appender.class);
	//Store the filtered loggEvents
	private List<LoggingEvent> loggingEvent;	
	//Iterate logging events to filter only info messages of BEV events
	private List<String> filteredLoggingEvents = new ArrayList<String>();
	//TOOD: Need to consolidate all asserts and show all failure once. hence commented as of now.
	/*@Rule 
	private ErrorCollector collector = new ErrorCollector(); */
	/**
	 * <p>
	 * Summary : Initialize the helper to instantiate Loggers and attach filters
	 * </p>
	 * <p>
	 * Where to call : In the setup method of the Test class
	 * </p>
	 * 
	 * @input - 
	 * @output -
	 * 
	 * @author - Karthiga Baskaran
	 * @date - 05/26/2016
	 * 
	 */
	public BEVEventJunitHelpers() {
		try {
			//Define the logger for the builder
			final Logger logger = (Logger) LoggerFactory
					.getLogger("com.wellsfargo.secure.connect.bev.builder.BusinessEventJAXBBuilder");
			//Define the appender with a name
			Mockito.when(mockAppender.getName()).thenReturn("BEVEVENTMOCK");
			//Set filter level to INFO as the BEV events are logged as INFO
			filter.setLevel(Level.INFO);
			//Add filters to the MockAppender
    		//TODO: Currently it is not working and need to fix it
			Mockito.doNothing().when(mockAppender).addFilter(filter);
			//Add appender to the logger
			logger.addAppender(mockAppender);

		} catch (Exception ex) {
			fail("Exception while validating the asserts:"+ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	/**
	 * <p>
	 * Summary : Helper Util to retrieve the BEV events and stores the events into a list
	 * </p>
	 * <p>
	 * Where to call : In the test method i.e after the actual call
	 * </p>
	 * 
	 * @input - noOfBEVEventsExpected : noOfEvents to be expected
	 * @output - 
	 * 
	 * @author - Karthiga Baskaran
	 * @date - 05/26/2016
	 * 
	 */
	public void retrieveBEVEvents(int noOfBEVEventsExpected) {
		try {
			//Instantiate loggingevent to capture the log events
			Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(captorLoggingEvent.capture());
			// Having a genricised captor means we don't need to cast
			loggingEvent = captorLoggingEvent.getAllValues();
			assertNotNull("loggingEvent cant be null",loggingEvent);
			
			//Iterate through the log
			Iterator<LoggingEvent> itr = loggingEvent.iterator();
			while(itr.hasNext()) {
				LoggingEvent event = itr.next();
				//BEV Events logs are logged as INFO
				if(event.getLevel().equals(Level.INFO)){
					//BEV Events logs are logged starting with 'Business Event xml'
					if(event.getFormattedMessage().trim().startsWith("Business Event xml")){
						filteredLoggingEvents.add(event.getFormattedMessage().trim()
								.replaceAll("Business Event xml ", ""));
					}
				}
			}

			//Assert the size to be 'noOfBEVEventsExpected' as per this Unit test, 'noOfBEVEventsExpected' BEV events should be logged
			System.out.println("Actual loggingEvent size:" + filteredLoggingEvents.size());
			assertTrue("loggingEvent size is not "+noOfBEVEventsExpected,filteredLoggingEvents.size()==noOfBEVEventsExpected);			

		} catch (Exception ex) {
			fail("Exception while validating the asserts:"+ex.getMessage());
			ex.printStackTrace();
		}
	}


	/**
	 * <p>
	 * Summary : Helper Util to validate or assert BEV Event logs with XPATH
	 * </p>
	 * <p>
	 * Where to call : In the test method 
	 * </p>
	 * 
	 * @input - filteredEventLogNo: The nth log number to be validated. It should be >0 || <=filteredLoggingEvents.size()
	 *          xPathToBeValidated : Map which contains the list of tags(XPATH)
	 *         and expected value to be asserted
	 * @output - 
	 * 
	 * @date - 05/26/2016
	 * 
	 */
	public void assertXPathForBEVEvents(int filteredEventLogNo, Map<String, Object> xPathToBeValidated) {
		try {
			//assert if filteredEventLogNo not >0 || <=filteredLoggingEvents.size()
			assertTrue("filteredEventLogNo should be greater than zero else less than or equal to filteredLoggingEvents count",
					filteredEventLogNo>0||filteredEventLogNo<=filteredLoggingEvents.size());
			//Get the formatted xml message in UTF-8 format
			String formattedXmlMessageUtf8 = new String((filteredLoggingEvents.get(filteredEventLogNo-1)).getBytes("UTF-8"), "UTF-8");
			System.out.println("spied xml utf8:" + formattedXmlMessageUtf8);
			//Instantiate XPATH
			XPath query = XPathFactory.newInstance().newXPath();
			//Create the namespace
			@SuppressWarnings("serial")
			HashMap<String, String> prefMap = new HashMap<String, String>() {
				{
					put("ns3", "http://eai-schemas.wellsfargo.com/provider/aps/distributedSysMngmt/logBusinessEvent/2003/");
					put("ns2", "http://eai-schemas.wellsfargo.com/base/");
					put("ns4", "http://eai-schemas.wellsfargo.com/provider/aps/distributedSysMngmt/logBusinessEvent/2003/");
					put("bos", "http://eai-schemas.wellsfargo.com/serviceprovider/bos/");
				}
			};
			SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
			//set the namespace to the XPATH query
			query.setNamespaceContext(namespaces);
			//Iterate the xPath to be validate map with its expected data
			for (Map.Entry<String, Object> entry : xPathToBeValidated.entrySet()) {
				String xPath = entry.getKey();
				String expectedValue = entry.getValue().toString();
				//Evaluate the XPATH expression
				String actualValue = (String) query.evaluate(xPath,
						new InputSource(new StringReader(formattedXmlMessageUtf8.trim())));
				
				//assert the actual value with its expected value
				/*collector.checkThat("Either expected tag(" + xPath
					+ ") not available or expected value(" + expectedValue
					+ ") not equal(" + actualValue + ")", (String)expectedValue,
					equalTo(actualValue));*/
				assertEquals("Either expected tag(" + xPath
						+ ") not available or expected value(" + expectedValue
						+ ") not equal(" + actualValue + ")", expectedValue,
						actualValue);
				
				System.out.println("Expected tag(" + xPath
						+ ") expected value(" + expectedValue
						+ ") actual value(" + actualValue + ")");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Exception while validating the asserts:"+ex.getMessage());		
		}
	}
	
	/**
	 * <p>
	 * Summary : Helper Util to validate or assert BEV Event logs with json
	 * </p>
	 * <p>
	 * Where to call : In the test method 
	 * </p>
	 * 
	 * @input - filteredEventLogNo: The nth log number to be validated. It should be >0 || <=filteredLoggingEvents.size()
	 *          jsonPropertyToBeValidated : Map which contains the list of tags(json property)
	 *         and expected value to be asserted
	 * @output - 
	 * 
	 * @author - Karthiga Baskaran
	 * @date - 05/26/2016
	 * 
	 */
	public void assertJSONPropertyForBEVEvents(int filteredEventLogNo, Map<String, Object> jsonPropertyToBeValidated) {
		try {
			// TODO Auto-generated method stub
		} catch (Exception ex) {
			fail("Exception while validating the asserts:"+ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	/**
	 * <p>
	 * Summary : Helper Util to detach the logger 
	 * </p>
	 * 
	 * @input - 
	 * @output - 
	 * 
	 * @author - Karthiga Baskaran
	 * @date - 05/26/2016
	 * 
	 */
	public void tearDown() {
		//Get the logger instance
		final Logger logger = (Logger) LoggerFactory
				.getLogger(Logger.ROOT_LOGGER_NAME);
		//detach the appender from the logger instance
		logger.detachAppender(mockAppender);
	}
}
