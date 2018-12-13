package ru.sharovse.spring.utils.db.values;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.sharovse.spring.utils.db.values.annotations.ValueDb;
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDataSourceBean;

@Configuration
//@ConditionalOnBean(annotation= {ValueDb.class, ValueDbDataSourceBean.class})
public class ValueDbAutoConfiguration {
	@Bean
	public ValueDbAnnotationBeanPostProcessor createValueDbAnnotationBeanPostProcessor() {
		return new ValueDbAnnotationBeanPostProcessor();
	}

}
