package ru.sharovse.spring.utils.db.values;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValueDbAutoConfiguration {
	@Bean
	public ValueDbAnnotationBeanPostProcessor createValueDbAnnotationBeanPostProcessor() {
		return new ValueDbAnnotationBeanPostProcessor();
	}

}
