package com.apps.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPoolConfig;

@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "redis.hostname", matchIfMissing = false)
public class CacheConfig extends CachingConfigurerSupport {
	
	@Value("${redis.hostname}")
	private String redisHostName;

	@Value("${redis.port}")
	private int redisPort;

	@Value("${redis.23hrs.expiration}")
	private Long expiration23Hrs;

	@Value("${redis.15mins.expiration}")
	private Long expiration15Mins;
	
	@Value("${spring.cache.redis.key-prefix}")
	private String keyPrefix;

	@Bean(name = "redisTemplate")
	public RedisTemplate<String, Object> redisTemplate() {
		log.info("[redisTemplate] Started creating redisTemplate with host: {}, port: {}", redisHostName, redisPort);
		RedisTemplate<String, Object> obj = new RedisTemplate<String, Object>();
		obj.setConnectionFactory(jedisConnectionFactory());
		obj.setKeySerializer(new StringRedisSerializer());
		return obj;
	}

	// Default cache manager is infinite. No expiration
	@Primary
	@Bean(name = "cacheManager")
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		log.info("[cacheManager] cacheManager Bean: {}, prefix : {}", redisConnectionFactory, keyPrefix);
		return RedisCacheManager.builder(redisConnectionFactory)
				.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
						.computePrefixWith(CacheKeyPrefix.prefixed(keyPrefix))).build();
	}
	@Bean(name = "cacheManager23Hrs")
	public CacheManager cacheManager23Hrs(RedisConnectionFactory redisConnectionFactory) {
		log.info("[cacheManager23Hrs] cacheManagerSession Bean : {}, prefix : {}", redisConnectionFactory, keyPrefix);
		return RedisCacheManager
				.builder(redisConnectionFactory).cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
						.computePrefixWith(CacheKeyPrefix.prefixed(keyPrefix))
						.entryTtl(Duration.ofMinutes(expiration23Hrs)))
				.build();
	}

	@Bean(name = "cacheManager15Mins")
	public CacheManager cacheManager15Mins(RedisConnectionFactory redisConnectionFactory) {
		log.info("[cacheManager15Mins] cacheManagerSession Bean : {}, prefix : {}", redisConnectionFactory, keyPrefix);
		return RedisCacheManager
				.builder(redisConnectionFactory).cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
						.computePrefixWith(CacheKeyPrefix.prefixed(keyPrefix))
						.entryTtl(Duration.ofMinutes(expiration15Mins)))
				.build();
	}

  //cluster
	@Bean
	@Profile("!local")
	public JedisConnectionFactory jedisConnectionFactory() {
		log.info("[jedisConnectionFactory] Started creating jedisConnectionFactory " + "with host: {}, port: {}",
				redisHostName, redisPort);
		String[] clusterNodesArr = new String[] { redisHostName + ":" + redisPort };
		RedisClusterConfiguration redisClusterConfiguration = getRedisConfig(clusterNodesArr);
		return new JedisConnectionFactory(redisClusterConfiguration,
				jedisPoolConfig());
	}

  //local
	@Bean("jedisConnectionFactory")
	@Profile("local")
	JedisConnectionFactory jedisConnectionFactoryLocal() {
		log.info("[jedisConnectionFactory] Started creating jedisConnectionFactory local " + "with host: {}, port: {}",
				redisHostName, redisPort);
		String[] clusterNodesArr = new String[] { redisHostName + ":" + redisPort };
		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
		return jedisConnectionFactory;
	}

	private RedisClusterConfiguration getRedisConfig(String[] clusterNodesArr) {
		Collection<String> clusterNodes = Arrays.asList(clusterNodesArr);
		return new RedisClusterConfiguration(clusterNodes);
	}

	private JedisPoolConfig jedisPoolConfig() {
		JedisPoolConfig obj = new JedisPoolConfig();
		obj.setMaxTotal(260);
		obj.setMinIdle(50);
		return obj;
	}
}
