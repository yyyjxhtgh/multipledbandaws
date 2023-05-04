package com.apps.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.mhe.goldy.constants.GoldyConstants;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.sql.DataSource;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Slf4j
@Configuration
public class MySqlConfig {


	@Bean
	public SpringLiquibase liquibase(DataSource dataSource, Environment environment) {
		String activeProfile = environment.getProperty("spring.profiles.active");
		log.info("Initializing liquibase.... with env {}", activeProfile);
		SpringLiquibase liquibase = null;
		if(StringUtils.isNotEmpty(activeProfile)) {
			liquibase = new SpringLiquibase();
			liquibase.setChangeLog("classpath:/liquibase/dbchangelog.xml");
			liquibase.setDataSource(dataSource);
		}
		return liquibase;
	}
	
	@Bean
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
		log.info("Initializing namedParameterJdbcTemplate.... ");
		return new NamedParameterJdbcTemplate(dataSource);
	}

//default data source for my sql
	@Bean
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource dataSource() {
		log.info("Initializing DataSource!!");
		return DataSourceBuilder.create().build();
	}

	@Bean
	public RegistryEventConsumer<Retry> circuitBreakerRetryRegistryEventConsumer() {

		return new RegistryEventConsumer<Retry>() {
			@Override
			public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
				entryAddedEvent.getAddedEntry().getEventPublisher()
						.onEvent(event -> log.info("Retry attempt number : {} , for event : {}",
								event.getNumberOfRetryAttempts(), event));
			}

			@Override
			public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {

			}

			@Override
			public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {

			}
		};
	}
	
}
