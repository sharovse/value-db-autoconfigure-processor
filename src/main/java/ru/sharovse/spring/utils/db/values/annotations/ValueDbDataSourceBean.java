package ru.sharovse.spring.utils.db.values.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static ru.sharovse.spring.utils.db.values.ValueDbConstants.*;

/** DataSource Annotation for {@link ValueDb}.
 * @author sharov1-se
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueDbDataSourceBean {
	
	/** Name bean for symbolic link {@link ValueDb#dataSourceAnnotation()}.
	 * If {@link #registerToContext()}=true, DataSource bean will register into spring applicationContext by this name.
	 * @return valid bean name.
	 */
	String name();
	
	/** Prefix for properties.
	 * @return String
	 */
	String propertyPrefix() default NOT_SET;

	String driverClassName() default DATASOURCE_PROPERTY_DRIVER_CLASS_NAME;
	String url() default DATASOURCE_PROPERTY_URL;
	String username() default DATASOURCE_PROPERTY_USERNAME;
	String pw() default DATASOURCE_PROPERTY_PASSWORD;
	
	/** Add dataSource bean to spring context.
	 * @return default no.
	 */
	boolean registerToContext() default false;
	
	/**
	 * Execute SQL script after create DataSource.
	 * @return name classpath resource. 
	 */
	String importSql() default NOT_SET;
}
