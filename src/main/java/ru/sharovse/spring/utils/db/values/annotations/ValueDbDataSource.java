package ru.sharovse.spring.utils.db.values.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.sql.DataSource;

import static ru.sharovse.spring.utils.db.values.ValueDbConstants.*;

/** DataSource Annotation for {@link ValueDb}.
 * Create instance {@link DataSource} of {@link #className()}.
 * Get instance fields, find and set values ​​from properties of the same name.
 * @author sharov1-se
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueDbDataSource {
	
	/** Name bean for symbolic link {@link ValueDb#dataSourceAnnotation()}.
	 * If {@link #registerToContext()}=true, DataSource bean will register into spring applicationContext by this name.
	 * @return valid bean name.
	 */
	String name();
	
	/** Prefix for properties.
	 * @return String
	 */
	String propertyPrefix() default NOT_SET;

	String className();
	
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
