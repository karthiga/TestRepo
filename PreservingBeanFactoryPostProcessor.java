package com.wellsfargo.mwf.testingframework.springframework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import com.google.common.base.Strings;

/**
 * 
 * @author Ram Lakshmanan
 */
public final class PreservingBeanFactoryPostProcessor implements
BeanFactoryPostProcessor {
	private static final Logger LOG = LoggerFactory
			.getLogger(PreservingBeanFactoryPostProcessor.class);
	private String[] rootBeans;
	private String[] notToPreserveBeans;

	public String[] getRootBeans() {
		return rootBeans;
	}

	public void setRootBeans(String[] noOpBeans) {
		this.rootBeans = noOpBeans;
	}

	public String[] getNotToPreserveBeans() {
		return notToPreserveBeans;
	}

	public void setNotToPreserveBeans(String[] notToPreserveBeans) {
		this.notToPreserveBeans = notToPreserveBeans;
	}

	List<String> beanDefinitionsToBePreserved = new ArrayList<>();

	@SuppressWarnings("unchecked")
	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

			// Step 1: Walk through all beans and store it Map.
			String[] beanNames = beanFactory.getBeanDefinitionNames();

			for (String beanName : beanNames) {

				beanDefinitions.put(beanName,
						beanFactory.getBeanDefinition(beanName));
			}

			// Step 2: Get all the root Beans and it's dependencies
			for (String rootBean : rootBeans) {

				if (!beanFactory.containsBean(rootBean)) {
					continue;
				}

				preserveBeanDefintion(beanFactory, rootBean);

			}

			// Step 3: Other than the beans which needs to be preserved mark
			// others a no-op beans

			// Step 3.1: Remove the bean definitions which are not preserved
			// from the super set. Such that
			// super set will only contain essential beans which needs to be
			// made No-op
			//LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). PreservedBeans- "+beanDefinitionsToBePreserved+ "  ");
			List <String> notToPreserveBeansList = (List<String>) (null!=notToPreserveBeans?Arrays.asList(notToPreserveBeans):Collections.emptyList());;
			for (String beanName : beanDefinitionsToBePreserved) {
				if(!notToPreserveBeansList.contains(beanName)){
					beanDefinitions.remove(beanName);
				}
			}

			// Step 3.2: Making the bean no-op
			for (String beanName : beanDefinitions.keySet()) {
				try{
					BeanDefinition beanDefinition = beanDefinitions.get(beanName);
					//LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Processing ");
					
					String beanClass = null!=beanDefinition.getBeanClassName()?beanDefinition.getBeanClassName():null;					
					
					//Get the decorator bean if any using originatingBeanDefinition[to update any scoped target bean]
					String decoratedBean= null!=beanDefinition.getOriginatingBeanDefinition()?beanDefinition.getOriginatingBeanDefinition().getBeanClassName():null;

					// Omit beanDefinition if it is from a Spring package for
					// example: org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
					if (null!=beanClass && beanClass.contains("org.springframework") && !beanClass.contains("org.springframework.aop.scope")) {
					//	LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Omit beanDefinition if it is from a Spring package for ");
						continue;
					}

					//If any bean contains factory method, nullify the property
					if(null!=beanDefinition.getFactoryMethodName()){
					//	LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Nullifying the factory method");
						beanDefinition.setFactoryMethodName(null);
					}
					
					//If any bean contains parent, nullify the property
					/*if(null!=beanDefinition.getParentName()){
					//	LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Nullifying the parent property");
						beanDefinition.setParentName(null);
					}*/
					
					//Empty the scope if any
					if (!Strings.isNullOrEmpty(beanDefinition.getScope())) {
				//		LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Nullifying the scope");
						beanDefinition.setScope(null);
					}
					//Update scoped target bean with its orginated/decorator bean as scope becomes invalid
					if(null!=beanClass && beanClass.contains("org.springframework.aop.scope")){
					//	LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Updating spring aop proxy scope with '"+decoratedBean +"' ");
						beanDefinition.getPropertyValues().removePropertyValue("targetBeanName");
						beanDefinition.setBeanClassName(decoratedBean);
						continue;
					}				
					
					//Skip the ScopedTarget (initialized when aop scope proxy enabled in a definition), so that the original.decorated bean class can be associated to the bean else targeted proxy scope will be added
					if(beanName.contains("scopedTarget")){
					//	LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - is a scoped target, hence skipped ");
						continue;
					}
					
					//LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Empty Properties ");			
					//Nullify all properties being referred
					((GenericBeanDefinition) beanDefinition).setPropertyValues(null);
					
				//	LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Empty Constructor Arguments ");	
					//Nullify all constructor Arguments being referred
					((GenericBeanDefinition) beanDefinition).setConstructorArgumentValues(null);
					
				//	LOG.info(" PreservingBeanFactoryPostProcessor.postProcessBeanFactory(). - "+beanName+ " - Set bean class name to Object ");	
					beanDefinition.setBeanClassName("java.lang.Object");
				}
				catch (Exception e) {
					LOG.error("Exception while updating the beandefintion for the beans not to be preserved PreservingBeanFactoryPostProcessor.postProcessBeanFactory().",e);
				}
			}
		} catch (Exception e) {
			LOG.error("Exception in preserving bean post processor PreservingBeanFactoryPostProcessor.postProcessBeanFactory().",e);
		}
	}

	@SuppressWarnings("unchecked")
	public void preserveBeanDefintion(
			ConfigurableListableBeanFactory beanFactory, String rootBean) {

		if (!beanFactory.containsBean(rootBean)) {
			return;
		}

		BeanDefinition originalBeanDefinition = (BeanDefinition) beanFactory
				.getBeanDefinition(rootBean);
		beanDefinitionsToBePreserved.add(rootBean);

		// Add the child element definitions to beanDefinitionsToBePreserved
		List<PropertyValue> propertyValues = originalBeanDefinition
				.getPropertyValues().getPropertyValueList();
		for (PropertyValue propertyValue : propertyValues) {

			if (propertyValue.getValue() instanceof BeanReference) {

				preserveBeanDefintion(beanFactory,
						((RuntimeBeanReference) propertyValue.getValue())
						.getBeanName());
			}

			// Add mapped child elements to beanDefinitionsToBePreserved
			if (propertyValue.getValue() instanceof Map) {
				for (Map.Entry<String, Object> propertyMapEntry : ((Map<String, Object>) propertyValue
						.getValue()).entrySet()) {
					if (propertyMapEntry.getValue() instanceof BeanReference) {
						preserveBeanDefintion(beanFactory,
								((RuntimeBeanReference) propertyMapEntry
										.getValue()).getBeanName());
					}
				}
			}

			// Add list child elements to beanDefinitionsToBePreserved
			if (propertyValue.getValue() instanceof List) {
				for (Object propertyMapEntry : ((List<Object>) propertyValue
						.getValue())) {
					if (propertyMapEntry instanceof BeanReference) {
						preserveBeanDefintion(beanFactory,
								((RuntimeBeanReference) propertyMapEntry)
								.getBeanName());
					}
				}
			}
		}

		// Add the child element definitions to beanDefinitionsToBePreserved ii)
		// defined as constructors
		Map<Integer, ValueHolder> constructorValues = originalBeanDefinition
				.getConstructorArgumentValues().getIndexedArgumentValues();
		for (Map.Entry<Integer, ValueHolder> constructorValue : constructorValues
				.entrySet()) {

			if (constructorValue.getValue().getValue() instanceof BeanReference
					|| constructorValue.getValue().getValue() instanceof RuntimeBeanReference) {

				preserveBeanDefintion(beanFactory,
						((RuntimeBeanReference) constructorValue.getValue()
								.getValue()).getBeanName());
			}
		}
	}
}
