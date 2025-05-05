package com.neighbor.eventmosaic.processor.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Эта конфигурация отключает планировщик задач Spring в тестах.
 * Нужно для тестирования непосредственно метода планировщика.
 * <a href="https://stackoverflow.com/a/41567159">...</a>
 */
@Component
@Profile("test")
public class DisableSchedulingPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] beanNames = beanFactory.getBeanNamesForType(ScheduledAnnotationBeanPostProcessor.class);
        for (String beanName : beanNames) {
            ((DefaultListableBeanFactory) beanFactory).removeBeanDefinition(beanName);
        }
    }
}

