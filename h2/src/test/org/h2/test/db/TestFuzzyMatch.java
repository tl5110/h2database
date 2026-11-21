package org.h2.test.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.test.TestBase;
import org.h2.test.TestDb;

public class TestFuzzyMatch extends TestDb {
    public static void main(String... args) throws Exception {
        TestBase.createCaller().init().testFromMain();
    }

    @Override
    public void test() throws Exception {
        deleteDb("fuzzyMatch");

        try (Connection conn = getConnection("fuzzyMatch")) {
            testConstants(conn);
            testParameters(conn);
            testTableQuery(conn);
            testInvalidCalls(conn);
        } finally {
            deleteDb("fuzzyMatch");
        }
    }

    private void testConstants(Connection conn) throws SQLException {
        checkSql(conn,
                "SELECT FUZZY_MATCH('cat', 'cut', 1)",
                Boolean.TRUE);

        checkSql(conn,
                "SELECT FUZZY_MATCH('cat', 'cut', 0)",
                Boolean.FALSE);

        checkSql(conn,
                "SELECT FUZZY_MATCH('HELLO', 'hello', 0)",
                Boolean.TRUE);

        checkSql(conn,
                "SELECT FUZZY_MATCH('kitten', 'sitting', 3)",
                Boolean.TRUE);

        checkSql(conn,
                "SELECT FUZZY_MATCH(NULL, 'abc', 1)",
                null);

        checkSql(conn,
                "SELECT FUZZY_MATCH('abc', NULL, 1)",
                null);

        checkSql(conn,
                "SELECT FUZZY_MATCH('abc', 'abc', NULL)",
                null);
    }

    private void testParameters(Connection conn) throws SQLException {
        try (PreparedStatement prep = conn.prepareStatement(
                "SELECT FUZZY_MATCH(?, ?, ?)")) {

            prep.setString(1, "kitten");
            prep.setString(2, "sitting");

            prep.setInt(3, 3);
            try (ResultSet rs = prep.executeQuery()) {
                checkResult(rs, Boolean.TRUE);
            }

            // Reuse the same prepared statement with new input.
            prep.setInt(3, 2);
            try (ResultSet rs = prep.executeQuery()) {
                checkResult(rs, Boolean.FALSE);
            }

            prep.setNull(1, java.sql.Types.VARCHAR);
            try (ResultSet rs = prep.executeQuery()) {
                checkResult(rs, null);
            }
        }
    }

    private void testTableQuery(Connection conn) throws SQLException {
        try (Statement stat = conn.createStatement()) {
            stat.execute(
                    "CREATE TABLE fuzzy_cases("
                            + "id INT PRIMARY KEY, "
                            + "a VARCHAR, "
                            + "b VARCHAR, "
                            + "k INT, "
                            + "expected BOOLEAN)");

            stat.executeUpdate(
                    "INSERT INTO fuzzy_cases VALUES "
                            + "(1, 'cat', 'cut', 1, TRUE),"
                            + "(2, 'cat', 'cut', 0, FALSE),"
                            + "(3, '', 'abc', 3, TRUE),"
                            + "(4, NULL, 'abc', 1, NULL),"
                            + "(5, 'abc', 'abc', NULL, NULL)");

            try (ResultSet rs = stat.executeQuery(
                    "SELECT COUNT(*) FROM fuzzy_cases "
                            + "WHERE FUZZY_MATCH(a, b, k) "
                            + "IS DISTINCT FROM expected")) {

                if (!rs.next() || rs.getInt(1) != 0) {
                    throw new AssertionError(
                            "Table-based fuzzy matching failed");
                }
            }
        }
    }

    private void testInvalidCalls(Connection conn) throws SQLException {
        expectSqlError(conn,
                "SELECT FUZZY_MATCH('abc', 'abc', -1)");

        expectSqlError(conn,
                "SELECT FUZZY_MATCH('a', 'b')");

        expectSqlError(conn,
                "SELECT FUZZY_MATCH('a', 'b', 1, 2)");
    }

    private void checkSql(Connection conn, String sql,
            Boolean expected) throws SQLException {
        try (Statement stat = conn.createStatement();
                ResultSet rs = stat.executeQuery(sql)) {
            checkResult(rs, expected);
        }
    }

    private void checkResult(ResultSet rs, Boolean expected)
            throws SQLException {
        if (!rs.next()) {
            throw new AssertionError("Expected one result row");
        }

        boolean value = rs.getBoolean(1);
        Boolean actual = rs.wasNull() ? null : Boolean.valueOf(value);

        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(
                    "Expected " + expected + ", got " + actual);
        }

        if (rs.next()) {
            throw new AssertionError("Expected only one result row");
        }
    }

    private void expectSqlError(Connection conn, String sql)
            throws SQLException {
        try (Statement stat = conn.createStatement()) {
            try {
                stat.execute(sql);
            } catch (SQLException expected) {
                return;
            }
        }

        throw new AssertionError("Expected SQL error: " + sql);
    }

}
