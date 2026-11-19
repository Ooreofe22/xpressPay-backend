package com.payxpress.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Accepts DATABASE_URL in either JDBC form (jdbc:postgresql://host:5432/db)
 * or the postgres://user:pass@host/db form that Render and Heroku hand out.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(@Value("${spring.datasource.url}") String url,
                                 @Value("${spring.datasource.username:}") String username,
                                 @Value("${spring.datasource.password:}") String password) {
        String jdbcUrl = url.trim();
        if (jdbcUrl.startsWith("postgres://") || jdbcUrl.startsWith("postgresql://")) {
            URI uri = URI.create(jdbcUrl);
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath()
                    + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
            String userInfo = uri.getRawUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                String[] parts = userInfo.split(":", 2);
                username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                if (parts.length > 1) {
                    password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }
        }
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}
