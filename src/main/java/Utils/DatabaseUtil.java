package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Utility class for database connection and query execution.
 * This class handles low-level database operations only - no business logic.
 * 
 * Responsibilities:
 * - Database connection management
 * - Query execution
 * - Transaction management
 * - Resource cleanup
 */
public class DatabaseUtil {
    
    
    // Database connection parameters - Can be set via system properties
    // Default URL uses localhost:5432 with SSL disabled, as SSH tunnel forwards this port.
    private static String DB_URL = System.getProperty("db.url", "jdbc:postgresql://localhost:5432/DEVServer?sslmode=disable");
    private static String DB_USER = System.getProperty("db.user", "postgres");
    private static String DB_PASSWORD = System.getProperty("db.password", "postgres");
    
    /**
     * Gets the database password from system property.
     * Throws RuntimeException if password is not provided.
     * 
     * @return Database password
     * @throws RuntimeException if password is missing or empty
     */


    /**
     * Establishes a database connection.
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        System.out.println("[DB] Connecting to database: " + DB_URL);
        System.out.println("[DB] Database user: " + DB_USER);

        int maxRetries = 3;
        int attempt = 0;
        SQLException lastException = null;

        while (attempt < maxRetries) {
            attempt++;
            System.out.println("[DB] Connection attempt " + attempt + " of " + maxRetries + "...");
            try {
                Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("[DB] ✅ Database connection established successfully");
                return connection;
            } catch (SQLException e) {
                lastException = e;
                System.out.println("[DB] ❌ Connection attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    System.out.println("[DB] Waiting 5 seconds before next retry...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.out.println("[DB] Retry wait interrupted, stopping further retries.");
                        break;
                    }
                }
            }
        }

        System.out.println("[DB] ❌ All " + maxRetries + " database connection attempts failed.");
        if (lastException != null) {
            throw lastException;
        } else {
            throw new SQLException("Database connection failed for unknown reasons after " + maxRetries + " attempts.");
        }
    }
    
    /**
     * Executes a DELETE query with parameters.
     * Handles transaction management (auto-commit off, commit on success, rollback on error).
     * 
     * @param query SQL DELETE query with placeholders (?)
     * @param parameters List of parameters to bind to the query placeholders
     * @return Number of rows affected
     * @throws SQLException if query execution fails
     */
    public static int executeDelete(String query, List<String> parameters) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        System.out.println("[DB] Executing DELETE query: " + query);
        if (parameters != null && !parameters.isEmpty()) {
            System.out.println("[DB] Parameters: " + parameters);
        }
        
        try (Connection connection = getConnection()) {
            // Set transaction safety
            connection.setAutoCommit(false);
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                // Set parameters if provided
                if (parameters != null) {
                    for (int i = 0; i < parameters.size(); i++) {
                        statement.setString(i + 1, parameters.get(i));
                        System.out.println("[DB] Parameter " + (i + 1) + ": " + parameters.get(i));
                    }
                }
                
                // Execute the DELETE statement
                int deletedCount = statement.executeUpdate();
                
                // Commit transaction on success
                connection.commit();
                System.out.println("[DB] ✅ Transaction committed successfully");
                System.out.println("[DB] ✅ Successfully deleted " + deletedCount + " row(s)");
                
                return deletedCount;
                
            } catch (SQLException e) {
                // Rollback transaction on exception
                try {
                    connection.rollback();
                    System.out.println("[DB] ⚠️ Transaction rolled back due to error");
                } catch (SQLException rollbackEx) {
                    System.out.println("[DB] ❌ ERROR: Failed to rollback transaction: " + rollbackEx.getMessage());
                }
                
                System.out.println("[DB] ❌ ERROR: Failed to execute DELETE query");
                System.out.println("[DB] Error message: " + e.getMessage());
                System.out.println("[DB] SQL State: " + e.getSQLState());
                throw e;
            }
        }
    }
    
    /**
     * Executes a generic query with parameters.
     * Can be used for SELECT, UPDATE, INSERT, DELETE operations.
     * 
     * @param query SQL query with placeholders (?)
     * @param parameters List of parameters to bind to the query placeholders
     * @return Number of rows affected (for UPDATE, INSERT, DELETE) or 0 (for SELECT)
     * @throws SQLException if query execution fails
     */
    public static int executeUpdate(String query, List<String> parameters) throws SQLException {
        return executeDelete(query, parameters);
    }
    
    /**
     * Updates database connection parameters.
     * 
     * @param url Database URL
     * @param user Database username
     * @param password Database password (will be stored but never logged)
     */
    public static void setConnectionParams(String url, String user, String password) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL cannot be null or empty");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("Database username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Database password cannot be null or empty");
        }
        
        DB_URL = url;
        DB_USER = user;
        DB_PASSWORD = password;
        
        System.out.println("[DB] Updated connection parameters");
        System.out.println("[DB] URL: " + DB_URL);
        System.out.println("[DB] User: " + DB_USER);
    }
    
    /**
     * Tests database connection.
     * 
     * @return true if connection successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            System.out.println("[DB] ✅ Database connection test successful!");
            return true;
        } catch (Exception e) {
            System.out.println("[DB] ❌ Database connection test FAILED!");
            System.out.println("[DB] Reason: " + e.getMessage());
            return false;
        }
    }
}
