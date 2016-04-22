package us.rockhopper.spider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A class for managing a database connection.
 * 
 * @author Tim Clancy
 * @version 9.7.15 - ability to execute queries on database.
 * 
 *          Acknowledgments: this code was learned from ryanlr's excellent
 *          web-crawler tutorial at
 *          http://www.programcreek.com/2012/12/how-to-make-a-web-crawler-using-
 *          java/.
 */
public class Database {

	public Connection connection = null;

	public Database() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			// Scrubbed user login information
			String url = "jdbc:mysql://localhost:3306/web_crawler";
			connection = DriverManager.getConnection(url, "_", "__");
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		Statement sta = connection.createStatement();
		return sta.executeQuery(sql);
	}

	public boolean execute(String sql) throws SQLException {
		Statement sta = connection.createStatement();
		return sta.execute(sql);
	}
}