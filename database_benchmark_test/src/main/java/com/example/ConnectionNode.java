package com.example;

import java.sql.*;

public class ConnectionNode {
    private static String url = "jdbc:mysql://localhost:3306/zm?allowPublicKeyRetrieval=true&useSSL=false";
    private static String u = "root";
    private static String p = "yanweijun12!Y";

    ConnectionNode next = null;
    Connection con;
    public Statement stm;

    public ConnectionNode() {
        try {
            con = DriverManager.getConnection(url, u, p);
            stm = con.createStatement();
        } catch (Exception e) {

        }
    }
}
