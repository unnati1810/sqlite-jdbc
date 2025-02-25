// --------------------------------------
// sqlite-jdbc Project
//
// ExtendedCommand.java
// Since: Mar 12, 2010
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sqlite.core.DB;

/**
 * parsing SQLite specific extension of SQL command
 *
 * @author leo
 */
public class ExtendedCommand {
    public static interface SQLExtension {
        public void execute(DB db) throws SQLException;
    }

    /**
     * Parses extended commands of "backup" or "restore" for SQLite database.
     *
     * @param sql One of the extended commands:<br>
     *     backup sourceDatabaseName to destinationFileName OR restore targetDatabaseName from
     *     sourceFileName
     * @return BackupCommand object if the argument is a backup command; RestoreCommand object if
     *     the argument is a restore command;
     * @throws SQLException
     */
  
    public static SQLExtension parse(String sql) throws SQLException {
        if (sql == null) {
            return null;
        }
    
        final String backupKeyword = "backup";
        final String restoreKeyword = "restore";
    
        int backupKeywordLength = backupKeyword.length();
        int restoreKeywordLength = restoreKeyword.length();
    
        boolean isBackupCommand = sql.length() > backupKeywordLength &&
                sql.substring(0, backupKeywordLength).equalsIgnoreCase(backupKeyword);
    
        boolean isRestoreCommand = sql.length() > restoreKeywordLength &&
                sql.substring(0, restoreKeywordLength).equalsIgnoreCase(restoreKeyword);
    
        if (isBackupCommand) {
            return BackupCommand.parse(sql);
        } else if (isRestoreCommand) {
            return RestoreCommand.parse(sql);
        }
    
        return null;
    }
    

    /**
     * Remove the quotation mark from string.
     *
     * @param s String with quotation mark.
     * @return String with quotation mark removed.
     */
    /*Refactored Code */
    public static String removeQuotation(String input) {
        if (input == null) {
            return input;
        }
    
        if (isQuotedWith(input, "\"") || isQuotedWith(input, "'")) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
    }
    
    private static boolean isQuotedWith(String input, String quote) {
        return input.startsWith(quote) && input.endsWith(quote);
    }
    
    

    public static class BackupCommand implements SQLExtension {
        public final String srcDB;
        public final String destFile;

        /**
         * Constructs a BackupCommand instance that backup the database to a target file.
         *
         * @param srcDB Source database name.
         * @param destFile Target file name.
         */
        public BackupCommand(String srcDB, String destFile) {
            this.srcDB = srcDB;
            this.destFile = destFile;
        }

        private static Pattern backupCmd =
                Pattern.compile(
                        "backup(\\s+(\"[^\"]*\"|'[^\']*\'|\\S+))?\\s+to\\s+(\"[^\"]*\"|'[^\']*\'|\\S+)",
                        Pattern.CASE_INSENSITIVE);

        /**
         * Parses SQLite database backup command and creates a BackupCommand object.
         *
         * @param sql SQLite database backup command.
         * @return BackupCommand object.
         * @throws SQLException
         */
        public static BackupCommand parse(String sqlCommand) throws SQLException {
            if (sqlCommand != null) {
                Matcher matcher = backupCmd.matcher(sqlCommand);
                if (matcher.matches()) {
                    String databaseName = removeQuotation(matcher.group(2));
                    String destination = removeQuotation(matcher.group(3));
                    if (databaseName == null || databaseName.isEmpty()) {
                        databaseName = "main";
                    }

                    return new BackupCommand(databaseName, destination);
                }
            }
            throw new SQLException("Syntax error: " + sqlCommand);
        }

        public void execute(DB db) throws SQLException {
            int rc = db.backup(srcDB, destFile, null);

            if (rc != SQLiteErrorCode.SQLITE_OK.code) {
                throw DB.newSQLException(rc, "Backup failed");
            }
        }
    }

    public static class RestoreCommand implements SQLExtension {
        public final String targetDB;
        public final String srcFile;
        private static Pattern restoreCmd =
                Pattern.compile(
                        "restore(\\s+(\"[^\"]*\"|'[^\']*\'|\\S+))?\\s+from\\s+(\"[^\"]*\"|'[^\']*\'|\\S+)",
                        Pattern.CASE_INSENSITIVE);

        /**
         * Constructs a RestoreCommand instance that restores the database from a given source file.
         *
         * @param targetDB Target database name
         * @param srcFile Source file name
         */
        public RestoreCommand(String targetDB, String srcFile) {
            this.targetDB = targetDB;
            this.srcFile = srcFile;
        }

        /**
         * Parses SQLite database restore command and creates a RestoreCommand object.
         *
         * @param sql SQLite restore backup command
         * @return RestoreCommand object.
         * @throws SQLException
         */
        public static RestoreCommand parse(String sql) throws SQLException {
            if (sql != null) {
                Matcher m = restoreCmd.matcher(sql);
                if (m.matches()) {
                    String dbName = removeQuotation(m.group(2));
                    String dest = removeQuotation(m.group(3));
                    if (dbName == null || dbName.length() == 0) dbName = "main";
                    return new RestoreCommand(dbName, dest);
                }
            }
            throw new SQLException("syntax error: " + sql);
        }

        /** @see org.sqlite.ExtendedCommand.SQLExtension#execute(org.sqlite.core.DB) */
        public void execute(DB db) throws SQLException {
            int rc = db.restore(targetDB, srcFile, null);

            if (rc != SQLiteErrorCode.SQLITE_OK.code) {
                throw DB.newSQLException(rc, "Restore failed");
            }
        }
    }
}
