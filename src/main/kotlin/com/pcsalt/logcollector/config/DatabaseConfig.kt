package com.pcsalt.logcollector.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.relational.core.dialect.Dialect
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.DataSourceInitializer
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import javax.sql.DataSource

@Configuration
class DatabaseConfig : AbstractJdbcConfiguration() {

  @Bean
  fun dataSource(): DataSource {
    return DriverManagerDataSource().apply {
      setDriverClassName("org.sqlite.JDBC")
      url = "jdbc:sqlite:${System.getProperty("user.dir")}/logs.db"
    }
  }

  @Bean
  fun namedParameterJdbcOperations(dataSource: DataSource): NamedParameterJdbcOperations {
    return NamedParameterJdbcTemplate(dataSource)
  }

  @Bean
  fun dataSourceInitializer(dataSource: DataSource): DataSourceInitializer {
    return DataSourceInitializer().apply {
      setDataSource(dataSource)
      setDatabasePopulator(ResourceDatabasePopulator().apply {
        addScript(ClassPathResource("schema.sql"))
        setContinueOnError(true)
      })
    }
  }

  @Bean
  override fun jdbcDialect(operations: NamedParameterJdbcOperations): Dialect {
    return SqliteDialect.INSTANCE
  }
}
