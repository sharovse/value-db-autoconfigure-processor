package ru.sharovse.spring.utils.db.values.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.core.env.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static ru.sharovse.spring.utils.db.values.ValueDbConstants.*;

/**
 * Simple {@link DriverManagerDataSource} Annotation for {@link ValueDb#dataSourceAnnotation()}.
 * 
 * @author sharov1-se
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueDbDriverManagerDataSource {

	/**
	 * Name bean for symbolic link {@link ValueDb#dataSourceAnnotation()}. If
	 * {@link #registerToContext()}=true, DataSource bean will register into spring
	 * applicationContext by this name.
	 * 
	 * @return valid bean name.
	 */
	String name();

	/**
	 * Prefix for properties.
	 * 
	 * @return String
	 */
	String propertyPrefix() default NOT_SET;

	/** JDBC Driver class name.
	 * Use ${name} for read value from {@link PropertySource}.
	 * @return String
	 */
	String driverClassName() default DATASOURCE_PROPERTY_DRIVER_CLASS_NAME;

	/** JDBC connection url.
	 * Use ${name} for read value from {@link PropertySource}.
	 * @return String
	 */
	String url() default DATASOURCE_PROPERTY_URL;

	/** JDBC username.
	 * Use ${name} for read value from {@link PropertySource}.
	 * @return String
	 */
	String username() default DATASOURCE_PROPERTY_USERNAME;

	/** JDBC username password.
	 * Use ${name} for read value from {@link PropertySource}.
	 * @return String
	 */
	String pw() default DATASOURCE_PROPERTY_PW;

	/**
	 * Add dataSource bean to spring context.
	 * 
	 * @return default no.
	 */
	boolean registerToContext() default false;

	/**
	 * Execute SQL script after create DataSource.
	 * 
	 * @return name classpath resource.
	 */
	String importSql() default NOT_SET;
}
