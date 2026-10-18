package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.luispuchol.selfbill.selfbill_api.shared.SearchText;

// Java because the normalization (accents, case) cannot be expressed in SQLite
public class V4__Search_text extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table clients add column search_text text not null default ''");
            statement.execute("alter table articles add column search_text text not null default ''");
        }
        backfill(connection);
    }

    /** Fills search_text of every row, deleted ones included. */
    public static void backfill(Connection connection) throws SQLException {
        backfill(connection, "select id, name, business_name, nif from clients",
                "update clients set search_text = ? where id = ?",
                rs -> SearchText.normalize(rs.getString(2), rs.getString(3), rs.getString(4)));
        backfill(connection, "select id, name from articles",
                "update articles set search_text = ? where id = ?",
                rs -> SearchText.normalize(rs.getString(2)));
    }

    private interface TextOf {
        String of(ResultSet row) throws SQLException;
    }

    private static void backfill(Connection connection, String select, String update, TextOf textOf)
            throws SQLException {
        try (Statement read = connection.createStatement();
                ResultSet rows = read.executeQuery(select);
                PreparedStatement write = connection.prepareStatement(update)) {
            while (rows.next()) {
                write.setString(1, textOf.of(rows));
                write.setInt(2, rows.getInt(1));
                write.addBatch();
            }
            write.executeBatch();
        }
    }
}
