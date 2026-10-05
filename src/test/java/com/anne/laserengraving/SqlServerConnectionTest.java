package com.anne.laserengraving;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class SqlServerConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testSqlServerConnection() throws Exception {
        assertNotNull(dataSource, "DataSource should not be null");
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection should be established");
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println(">>> Connected successfully to: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());
            System.out.println(">>> Catalog / Database name: " + connection.getCatalog());
            assertTrue(connection.isValid(5), "Connection should be valid");
        }
    }
}
