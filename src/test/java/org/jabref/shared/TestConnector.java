package org.jabref.shared;

import java.sql.SQLException;

import org.jabref.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.testutils.category.DatabaseTest;

@DatabaseTest
public class TestConnector {

    public static DBMSConnection getTestDBMSConnection(DBMSType dbmsType) throws SQLException, InvalidDBMSConnectionPropertiesException {
        DBMSConnectionProperties properties = getTestConnectionProperties(dbmsType);
        return new DBMSConnection(properties);
    }

    public static DBMSConnectionProperties getTestConnectionProperties(DBMSType dbmsType) {

        if (dbmsType == DBMSType.MYSQL) {
            return new DBMSConnectionProperties(dbmsType, "localhost", dbmsType.getDefaultPort(), "jabref", "root", "");
        }

        if (dbmsType == DBMSType.POSTGRESQL) {
            return new DBMSConnectionProperties(dbmsType, "localhost", dbmsType.getDefaultPort(), "jabref", "postgres", "");
        }

        if (dbmsType == DBMSType.ORACLE) {
            return new DBMSConnectionProperties(dbmsType, "localhost", dbmsType.getDefaultPort(), "xe", "travis", "travis");
        }

        return new DBMSConnectionProperties();
    }
}
