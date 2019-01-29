package com.geekbrains.server;

import java.sql.*;

import static com.geekbrains.server.SQLHandler.connect;
import static com.geekbrains.server.SQLHandler.disconnect;

public class Test {
    private static Connection connection;
    private static Statement stmt;

    public static void main(String[] args) {
        try {
            connect();
            readEx();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }
    private static void readEx() throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id > 0;");
        while (rs.next()) {
            System.out.println(rs.getInt(1) + " " + rs.getString("login") + " " + rs.getString("password" + " " +rs.getString("nickname")));
        }
    }

}
