package com.coffeeshop.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DemoDataService demoDataService;

    public DataInitializer(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Demo profile active — seeding demo data if database is empty...");
        demoDataService.seedIfEmpty();
    }
}
