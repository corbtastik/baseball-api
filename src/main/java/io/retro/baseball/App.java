package io.retro.baseball;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableCaching
public class App {

    private final MeterRegistry meterRegistry;

    @Autowired
    public App(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

    @Bean
    @Primary
    @ConfigurationProperties("io.retro.baseball.datasource.hikari")
    public HikariDataSource batchDataSource() {
        return dataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    @ConfigurationProperties("io.retro.baseball.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

}
