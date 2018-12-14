package ru.sharovse.spring.utils.db.values;

import static org.junit.Assert.*;

import org.junit.Test;

public class ValueDbAutoConfigurationTest {

	ValueDbAutoConfiguration service = new ValueDbAutoConfiguration();
	
	@Test
	public void testCreateValueDbAnnotationBeanPostProcessor() {
		assertNotNull(service.createValueDbAnnotationBeanPostProcessor());
	}

}
