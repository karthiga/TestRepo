package com.wellsfargo.mwf.testingframework.springframework;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <p>To effectively unit test you need to mock certain services/objects. However those
 * services are looked up the Spring Factory at run-time. Example: you are unit testing
 * object A. Lets say A depends on B. B depends on C. i.e. A --> B --> C.
 * 
 * <p>Now C is the service that you need to mock. There are two options to inject mocked-C
 * object in the object model.
 * 
 * <p><b>a. Bean Definition in XML</b>
 * 
 * <p>You can define mocked-C object in the bean definition XML configuration file. And this XML configuration
 * file should be the last one to be loaded. So that mocked C object will over-ride the real object's bean definition.
 * And at run-time when bean C is looked up mocked-C object will be returned.
 * 
 * <p><b>b. Clever Application Context</b>
 * 
 * <p>CleverApplicationContext extends {@link ClassPathXmlApplicationContext}, which is used in unit tests to wire the beans.
 * In CleverApplicationContext you can override the C object with mocked-C object by invoking the 
 * <b>overrideBean(String beanId, Object object) API</b>. So from this point whenever bean C 
 * is looked up mocked-C will be returned.
 * 
 * <p><b>Advantages of option 'b' over 'a'</b>
 * 
 * <ol>
 *  <li><b>Code Clarity:</b>There is a better code clarity, as you would know that C is being mocked right there in the unit test file. Where as if 
 *  you are using option a, you would have to dig through each configuration file to find out which objects are mocked.</li>
 *  <li><b>Fast:</b>For each unit tests - you want to mock the same object differently. Example if you are mocking a business service, you want
 *  to mock different types of responses
 *  <ul>
 *  	<li>Legitimate Response
 *  	<li>No Data found Response
 *  	<li>Throwing Exception
 *  </ul>
 *  Even with in these responses, there could be multiple variations (i.e. throwing NullPointerException, throwing EnhancedException....)
 *  For each of these scenarios you don't want to be defining mocking beans in each bean definition xml file. Even if you end up doing it, 
 *  for each uni test you would have to reload the application context. As all the mocking beans needs to have same bean id.
 *  <li><b>Ease of use:</b>If you are using mocking frameworks like Mockito, then you would have to deal with creating Factory methods of 
 *  Mockito framework to build your object in bean definition XML files, it's becomes little 
 *  tedious, when compared to coding it in Java.</li>
 * <ol> 
 * 
 * @author Ram Lakshmanan
 */
public class CleverApplicationContext extends ClassPathXmlApplicationContext {

	private Map<String, Object> localContext = new HashMap<>(); 
	
	public CleverApplicationContext(String[] definitions) {
		super(definitions);
	}
	
	/**
	 * Use this constructor to initialize the application context by overriding selective properties 
	 * as set by other property place holder bean post processors.
	 * 
	 * An example use would be as follows - the spring application context will be initialized with
	 * the real values for property place holders. So, for example, the OPS end point URL will be the real SIT URL.
	 * However, in a unit test, we would like to override this with a wiremock URL (running on local host).
	 * 
	 * Note: Setting SYSTEM_PROPERTIES_MODE_OVERRIDE property of the PropertyPlaceholderConfigurer does some thing similar,
	 * but should not be used. System/environment properties are global values share between unit tests and changing these
	 * will have undesired affects on the tests.
	 * 
	 * @param definitions the XML files with bean definitions
	 * @param propertiesToOverRide the properties to be used in preference to the properties set through
	 *        other PropertyPlaceholderConfigurer(s) in the spring application context.
	 *        
	 */
	public CleverApplicationContext(String[] definitions, Properties propertiesToOverRide) {
		super(definitions, false);
		
		PropertyPlaceHolderBeanPostProcessor postProcessor = new PropertyPlaceHolderBeanPostProcessor(propertiesToOverRide);
		addBeanFactoryPostProcessor(postProcessor);
		refresh();
	}
	
	/**
	 * @author Ram Lakshmanan
	 * Use this constructor to initialize the actual development application context 
	 * to over snowball effect of beans defined
	 * 
	 * It avoids duplicating bean definitions while creating JUnit test cases
	 * 
	 * @param definitions the XML files with bean definitions
	 * @param preserveBeans the root bean to be set so that all its reference bean will be marked as no-op beans
	 * Refer: PreservingBeanFactoryPostProcessor
	 *        
	 */
	public CleverApplicationContext(String[] definitions, String[] preserveBeans, String[] notToPreserveBeans) {
		super(definitions, false);
		
		PreservingBeanFactoryPostProcessor preventer = new PreservingBeanFactoryPostProcessor();
		preventer.setRootBeans(preserveBeans);
		preventer.setNotToPreserveBeans(notToPreserveBeans);
		addBeanFactoryPostProcessor(preventer);		
		refresh();
	}
	
	public void overrideBean(String beanId, Object object) {
		
		localContext.put(beanId, object);
	}
	
	@Override
	public Object getBean(String beanId) {
		
		if (localContext.get(beanId) != null) {
						
			return localContext.get(beanId);
		}
		
		return super.getBean(beanId);
	}

	@Override
	public <T> T getBean(String beanId, Class<T> classType) {

		if (localContext.get(beanId) != null) {
			
			@SuppressWarnings("unchecked")
			T t = (T)localContext.get(beanId);
			return t;
		}		
				
		return super.getBean(beanId,classType);
	}
	
	@Override
	public Object getBean(String beanId, Object... args) {

		Object bean = getBean(beanId);
		if (bean != null) {
			
			return bean;
		}
		
		return super.getBean(beanId, args);		
	}
	
 	/**
	 * 
	 * custom bean post processor that will override placeholder
	 * configuration as configured through XML
	 *
	 */
	private class PropertyPlaceHolderBeanPostProcessor extends PropertyPlaceholderConfigurer {

		private Properties properties;
		
		public PropertyPlaceHolderBeanPostProcessor(Properties properties) {
			this.properties = properties;
			setIgnoreUnresolvablePlaceholders(true);
			setOrder(Integer.MIN_VALUE);
		}
		
		@Override
		protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
			if (properties.containsKey(placeholder)) {
				return properties.getProperty(placeholder);
			} else {
				return null;
			}
		}
	}
}
