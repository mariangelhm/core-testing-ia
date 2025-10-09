package core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import core.errors.DBException;
import core.log.LoggerUtil;

/**
 * Simplified database helper powered by HikariCP to share connection logic across services.
 */
public class DBHelper {

    private final DataSource dataSource;

    public DBHelper(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setPoolName("qa-core-pool");
        this.dataSource = new HikariDataSource(config);
    }

    public DBHelper(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Map<String, Object>> query(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql, params);
             ResultSet resultSet = statement.executeQuery()) {
            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                }
                results.add(row);
            }
            return results;
        } catch (SQLException e) {
            throw new DBException("Error executing query", e);
        }
    }

    public int execute(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql, params)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DBException("Error executing statement", e);
        }
    }

    private PreparedStatement prepareStatement(Connection connection, String sql, Object... params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
        }
        return statement;
    }

    public void close() {
        if (dataSource instanceof HikariDataSource) {
            LoggerUtil.getLogger(DBHelper.class).info("Closing database connection pool");
            ((HikariDataSource) dataSource).close();
        }
    }
}
