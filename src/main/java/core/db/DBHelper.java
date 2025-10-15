package core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import core.errors.DBException;
import core.log.LoggerUtil;
import core.log.StructuredLog;

/**
 * Simplified database helper powered by HikariCP to share connection logic across services.
 */
public class DBHelper implements AutoCloseable {

    private static final org.slf4j.Logger LOGGER = LoggerUtil.getLogger(DBHelper.class);

    private final DataSource dataSource;

    /**
     * Creates a helper backed by an internal HikariCP data source.
     *
     * @param jdbcUrl JDBC connection string
     * @param username database username
     * @param password database password
     */
    public DBHelper(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setPoolName("qa-core-pool");
        this.dataSource = new HikariDataSource(config);
        LOGGER.info("DBHelper inicializado con URL {} y usuario {}", jdbcUrl, username);
    }

    /**
     * Creates a helper using a pre-configured {@link DataSource} instance.
     *
     * @param dataSource data source to reuse
     */
    public DBHelper(DataSource dataSource) {
        this.dataSource = dataSource;
        LOGGER.info("DBHelper inicializado con DataSource personalizado {}", dataSource);
    }

    /**
     * Executes a SQL query delegating to {@link #select(String, Object...)} to keep backwards compatibility.
     *
     * @param sql SQL statement to execute
     * @param params parameters bound to the prepared statement
     * @return list of rows represented as column-value maps
     */
    public List<Map<String, Object>> query(String sql, Object... params) {
        LOGGER.debug("query() invocado");
        return select(sql, params);
    }

    /**
     * Executes a {@code SELECT} returning all rows as a list of column-name/value maps.
     *
     * @param sql SQL statement to execute
     * @param params parameters bound to the prepared statement
     * @return list of rows represented as column-value maps
     */
    public List<Map<String, Object>> select(String sql, Object... params) {
        StructuredLog.Block block = StructuredLog.open(LOGGER, "BASE DE DATOS", "SELECT");
        block.line("SQL", sql);
        block.line("Parámetros", Arrays.toString(params));
        long start = System.nanoTime();
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
            block.section("RESULTADO");
            block.line("Filas", results.size());
            block.close(String.format(Locale.ROOT, "SELECT completado | %s",
                    StructuredLog.formatDuration(Duration.ofNanos(System.nanoTime() - start))));
            return results;
        } catch (SQLException e) {
            block.section("ERROR");
            block.error("SQLState", e.getSQLState());
            block.error("Código", e.getErrorCode());
            block.error("Mensaje", e.getMessage());
            block.close(String.format(Locale.ROOT, "SELECT con error | %s",
                    StructuredLog.formatDuration(Duration.ofNanos(System.nanoTime() - start))));
            throw new DBException("Error executing query", e);
        }
    }

    /**
     * Returns the first row of a {@code SELECT} or {@code null} when no data matches the query.
     *
     * @param sql SQL statement to execute
     * @param params parameters bound to the prepared statement
     * @return first row of the result set or {@code null}
     */
    public Map<String, Object> selectFirst(String sql, Object... params) {
        LOGGER.debug("selectFirst() invocado para SQL {}", sql);
        List<Map<String, Object>> results = select(sql, params);
        if (results.isEmpty()) {
            LOGGER.info("selectFirst() no encontró resultados para SQL {}", sql);
            return null;
        }
        LOGGER.info("selectFirst() retornó la primera fila para SQL {}", sql);
        return results.get(0);
    }

    /**
     * Returns the first column of the first row from a {@code SELECT}. Useful for scalar queries.
     *
     * @param <T> expected value type
     * @param sql SQL statement to execute
     * @param params parameters bound to the prepared statement
     * @return first column of the first row or {@code null}
     */
    public <T> T selectValue(String sql, Object... params) {
        LOGGER.debug("selectValue() invocado para SQL {}", sql);
        Map<String, Object> firstRow = selectFirst(sql, params);
        if (firstRow == null) {
            LOGGER.info("selectValue() no encontró valor para SQL {}", sql);
            return null;
        }
        if (firstRow.isEmpty()) {
            LOGGER.warn("selectValue() encontró fila vacía para SQL {}", sql);
            return null;
        }
        @SuppressWarnings("unchecked")
        T value = (T) firstRow.values().iterator().next();
        LOGGER.info("selectValue() obtuvo valor {} para SQL {}", value, sql);
        return value;
    }

    /**
     * Extracts a value from a materialised result set, enforcing bounds and column presence.
     *
     * @param rows previously materialised result set
     * @param rowIndex zero-based row index to retrieve
     * @param column column name to extract
     * @return cell value from the requested position
     */
    public Object extractValue(List<Map<String, Object>> rows, int rowIndex, String column) {
        LOGGER.debug("extractValue() invocado - filas: {}, índice: {}, columna: {}",
                rows != null ? rows.size() : 0, rowIndex, column);
        if (rows == null || rows.isEmpty()) {
            throw new DBException("Result set is empty; cannot extract value");
        }
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new DBException("Row index " + rowIndex + " is out of range (size " + rows.size() + ")");
        }
        Map<String, Object> row = rows.get(rowIndex);
        if (!row.containsKey(column)) {
            throw new DBException("Column '" + column + "' not present in result set");
        }
        Object value = row.get(column);
        LOGGER.info("extractValue() obtuvo valor {} de la fila {} columna {}", value, rowIndex, column);
        return value;
    }

    /**
     * Executes a SQL statement delegating to {@link #update(String, Object...)} for backward compatibility.
     *
     * @param sql SQL statement to execute
     * @param params parameters bound to the prepared statement
     * @return number of affected rows
     */
    public int execute(String sql, Object... params) {
        LOGGER.debug("execute() invocado");
        return update(sql, params);
    }

    /**
     * Executes an {@code INSERT}, {@code UPDATE} or {@code DELETE} statement returning the affected row count.
     *
     * @param sql SQL statement to execute
     * @param params parameters bound to the prepared statement
     * @return number of affected rows
     */
    public int update(String sql, Object... params) {
        StructuredLog.Block block = StructuredLog.open(LOGGER, "BASE DE DATOS", "UPDATE");
        block.line("SQL", sql);
        block.line("Parámetros", Arrays.toString(params));
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql, params)) {
            int affected = statement.executeUpdate();
            block.section("RESULTADO");
            block.line("Filas afectadas", affected);
            block.close(String.format(Locale.ROOT, "UPDATE completado | %s",
                    StructuredLog.formatDuration(Duration.ofNanos(System.nanoTime() - start))));
            return affected;
        } catch (SQLException e) {
            block.section("ERROR");
            block.error("SQLState", e.getSQLState());
            block.error("Código", e.getErrorCode());
            block.error("Mensaje", e.getMessage());
            block.close(String.format(Locale.ROOT, "UPDATE con error | %s",
                    StructuredLog.formatDuration(Duration.ofNanos(System.nanoTime() - start))));
            throw new DBException("Error executing statement", e);
        }
    }

    /**
     * Verifies the first value returned by a query against the expected value.
     *
     * @param sql SQL statement to execute
     * @param expected value expected from the first column of the first row
     * @param params parameters bound to the prepared statement
     * @return {@code true} when the value matches the expectation
     */
    public boolean validateValueEquals(String sql, Object expected, Object... params) {
        Object actual = selectValue(sql, params);
        boolean matches = Objects.equals(expected, actual);
        if (!matches) {
            LOGGER.warn("Validation failed for SQL '{}'. Expected: {}, Actual: {}", sql, expected, actual);
        } else {
            LOGGER.info("Validation passed for SQL '{}'. Value: {}", sql, actual);
        }
        return matches;
    }

    /**
     * Throws a {@link DBException} if the value returned by the query does not match the expectation.
     *
     * @param sql SQL statement to execute
     * @param expected value expected from the first column of the first row
     * @param params parameters bound to the prepared statement
     */
    public void assertValueEquals(String sql, Object expected, Object... params) {
        if (!validateValueEquals(sql, expected, params)) {
            throw new DBException("Expected value '" + expected + "' does not match query result");
        }
    }

    /**
     * Returns {@code true} when a {@code SELECT} yields no rows.
     *
     * @param sql SQL statement to execute
     * @param params parameters bound to the prepared statement
     * @return {@code true} when the result set is empty
     */
    public boolean isEmpty(String sql, Object... params) {
        LOGGER.debug("isEmpty() invocado para SQL {}", sql);
        List<Map<String, Object>> results = select(sql, params);
        boolean empty = results.isEmpty();
        if (!empty) {
            LOGGER.warn("Expected empty result set for SQL '{}' but found {} rows", sql, results.size());
        } else {
            LOGGER.info("Result set for SQL '{}' is empty as expected", sql);
        }
        return empty;
    }

    /**
     * Throws a {@link DBException} when the query returns at least one row.
     *
     * @param sql SQL statement to execute
     * @param params parameters bound to the prepared statement
     */
    public void assertEmpty(String sql, Object... params) {
        LOGGER.debug("assertEmpty() invocado para SQL {}", sql);
        if (!isEmpty(sql, params)) {
            throw new DBException("Query returned results when an empty set was expected");
        }
    }

    private PreparedStatement prepareStatement(Connection connection, String sql, Object... params) throws SQLException {
        LOGGER.debug("Preparando PreparedStatement para SQL {} con parámetros {}", sql, Arrays.toString(params));
        PreparedStatement statement = connection.prepareStatement(sql);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
        }
        return statement;
    }

    @Override
    public void close() {
        if (dataSource instanceof HikariDataSource) {
            LOGGER.info("Closing database connection pool");
            ((HikariDataSource) dataSource).close();
        } else {
            LOGGER.debug("close() invocado sin DataSource de tipo HikariDataSource");
        }
    }
}
