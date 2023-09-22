import java.sql.*;

public class ConnectionNode {
    private static String url = "jdbc:mysql://localhost:3306/zm?allowPublicKeyRetrieval=true&useSSL=false";
//    private static String url = "jdbc:mysql://localhost:3307/zm?useSSL=false";
    private static String u = "root";
    private static String p = "yanweijun12!Y";
//    private static String p = "";

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
