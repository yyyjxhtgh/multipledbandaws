package com.mhe.goldy.config;

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
public class RestConfig {

	// Determines the timeout in milliseconds until a connection is established.
	@Value("${resttemplate.connection.timeout}")
	private int connectTimeout;

	// The timeout when requesting a connection from the connection manager.
	@Value("${resttemplate.request.timeout}")
	private int requestTimeout;

	// The timeout for waiting for data
	@Value("${resttemplate.socket.timeout}")
	private int socketTimeout;
	@Value("${resttemplate.connection.pool.max_connection}")
	private int maxTotalConnections;
	@Value("${resttemplate.connection.pool.keep_alive_time}")
	private int defaultKeepAliveTimeMillis;
  
  //support https and pooling
	@Bean
	public RestTemplate restTemplate() {
		SSLConnectionSocketFactory csf = sslConnectionSocketFactory();
		CloseableHttpClient httpClient = clientBuilder().setSSLSocketFactory(csf).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		return new RestTemplate(requestFactory);
	}

	private PoolingHttpClientConnectionManager poolingConnectionManager() {
		SSLConnectionSocketFactory csf = sslConnectionSocketFactory();
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
				.<ConnectionSocketFactory>create().register("https", csf)
				.build();

		PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		poolingConnectionManager.setMaxTotal(maxTotalConnections);
		return poolingConnectionManager;
	}

	private ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
		return new ConnectionKeepAliveStrategy() {
			@Override
			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				HeaderElementIterator it = new BasicHeaderElementIterator
						(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					HeaderElement he = it.nextElement();
					String param = he.getName();
					String value = he.getValue();

					if (value != null && param.equalsIgnoreCase(GoldyConstants.TIMEOUT)) {
						return Long.parseLong(value) * 1000;
					}
				}
				return defaultKeepAliveTimeMillis;
			}
		};
	}
	
	private SSLConnectionSocketFactory sslConnectionSocketFactory() {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
		SSLContext sslContext;
		try {
		    sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
		    throw new RuntimeException("Failed to load SSL context", e);
		}
		return new SSLConnectionSocketFactory(sslContext);
	}

	private HttpClientBuilder clientBuilder() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(requestTimeout)
				.setConnectTimeout(connectTimeout)
				.setSocketTimeout(socketTimeout).build();
		//This Will Return HttpClientBuilder, which can be used in completing httpClient with preset pool and keep-alive strategy.
		return HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.setConnectionManager(poolingConnectionManager())
				.setKeepAliveStrategy(connectionKeepAliveStrategy());
	}
}
