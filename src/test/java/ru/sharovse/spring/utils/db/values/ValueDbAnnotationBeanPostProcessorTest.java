package ru.sharovse.spring.utils.db.values;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;

@RunWith(MockitoJUnitRunner.class)
public class ValueDbAnnotationBeanPostProcessorTest {

	@Mock
	ConfigurableEnvironment environment;
	
	ValueDbAnnotationBeanPostProcessor service;
	
	@Before
	public void setUp() throws Exception {
		service = new ValueDbAnnotationBeanPostProcessor();
		service.environment = environment;
	}

	@Test
	public void testEvaluateProperty() {
		String value = "value";
		String var = "var"; 
		String name = "${"+var+"}";
		String prefix = "prefix";
		Mockito.doReturn(value).when(environment).getProperty(Mockito.eq(prefix+ ValueDbConstants.DOT+var));
		assertEquals(value, service.evaluateProperty(name, prefix));
	}

	/*
	@Test
	public void testOnApplicationEvent() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOrder() {
		fail("Not yet implemented");
	}

	@Test
	public void testPostProcessBeforeInitialization() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddToValueDbDataSourceStore() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddToInnerValueDbStore() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddToValueDbStore() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetKeyInAnnonatedFields() {
		fail("Not yet implemented");
	}

	@Test
	public void testPostProcessAfterInitialization() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetTemplateAsBeanNameApplicationContextValueDb() {
		fail("Not yet implemented");
	}



	@Test
	public void testGetTemplateAsBeanNameApplicationContextString() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateJdbcTemplate() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetValueFromDb() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetValue() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetListColumValue() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOneColumnValue() {
		fail("Not yet implemented");
	}

	@Test
	public void testPostProcessEnvironment() {
		fail("Not yet implemented");
	}
*/
	
}
