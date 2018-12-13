package ru.sharovse.spring.utils.db.values.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.sharovse.spring.utils.db.values.ValueDbConstants;

/** DataSource Annotation for {@link ValueDb}.
 * @author sharov1-se
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueDbDataSourceBean {
	
	/** Name for reference from {@link ValueDb#dataSourceAnnotation()}.
	 * @return not null name.
	 */
	String name();
	/** Prefix for properties.
	 * @return String
	 */
	String propertyPrefix() default "";

	String driverClassName() default ValueDbConstants.DATASOURCE_PROPERTY_DRIVER_CLASS_NAME;
	String url() default ValueDbConstants.DATASOURCE_PROPERTY_URL;
	String username() default ValueDbConstants.DATASOURCE_PROPERTY_USERNAME;
	String pw() default ValueDbConstants.DATASOURCE_PROPERTY_PASSWORD;
	
	/** Add dataSource bean to context.
	 * @return default no.
	 */
	boolean registerToContext() default false;
	
	String importSql() default "";
}
