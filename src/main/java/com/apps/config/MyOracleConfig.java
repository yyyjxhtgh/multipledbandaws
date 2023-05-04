package com.apps.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class MyOracleConfig {

  /*
  #Novella DB Connection
novella.datasource.username=xxx
novella.datasource.password=xxxxx
novella.datasource.jdbc-url=jdbc:oracle:thin:@novelladev1_high?TNS_ADMIN=/Users/Jack.Hu@mheducation.com/Documents/tools/novelladev/Wallet_novelladev1

  */
    @Bean(name = "novellaDataSource")
    @ConfigurationProperties(prefix = "novella.datasource")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "novellaJdbcTemplate")
    public NamedParameterJdbcTemplate novellaJdbcTemplate(@Qualifier("novellaDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
