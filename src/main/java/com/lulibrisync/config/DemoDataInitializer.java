package com.lulibrisync.config;

import com.lulibrisync.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@ConditionalOnProperty(name = "lulibrisync.demo-data.enabled", havingValue = "true", matchIfMissing = true)
@SuppressWarnings("null")
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String DEMO_ADMIN_EMAIL = "admin@lulibrisync.edu";

    private final UserRepository userRepository;
    private final DataSource dataSource;
    private final Resource demoDataScript;

    public DemoDataInitializer(UserRepository userRepository,
                               DataSource dataSource,
                               @Value("classpath:seed/demo-data.sql") Resource demoDataScript) {
        this.userRepository = userRepository;
        this.dataSource = dataSource;
        this.demoDataScript = demoDataScript;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean demoAdminExists = userRepository.existsByEmailIgnoreCase(DEMO_ADMIN_EMAIL);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(demoDataScript);
        populator.setContinueOnError(false);

        try (Connection connection = dataSource.getConnection()) {
            populator.populate(connection);
        }

        if (demoAdminExists) {
            logger.info("Demo data sync completed for existing local records and catalog.");
        } else {
            logger.info("Demo data initialized because '{}' was not found in the database.", DEMO_ADMIN_EMAIL);
        }
    }
}
