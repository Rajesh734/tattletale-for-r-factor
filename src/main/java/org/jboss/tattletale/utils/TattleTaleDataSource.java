/*
 * Copyright (C) 2016, Liberty Mutual Group
 *
 * Created on Dec 18, 2016
 */

package org.jboss.tattletale.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * @author n0213628
 *
 */
public class TattleTaleDataSource {

	private static BasicDataSource dataSource;

	private static BasicDataSource getDataSource() {

		if (dataSource == null) {
			BasicDataSource ds = new BasicDataSource();
			ds.setUrl("jdbc:mysql://localhost/R_FACTOR?autoReconnect=true&useSSL=false");
			ds.setUsername("root");
			ds.setPassword("password");

			ds.setMinIdle(5);
			ds.setMaxIdle(10);
			ds.setMaxOpenPreparedStatements(100);

			dataSource = ds;
		}
		return dataSource;
	}
	public static void main(String[] args) throws SQLException
	{

		try (BasicDataSource dataSource = TattleTaleDataSource.getDataSource(); 
				Connection connection = dataSource.getConnection();
				PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM test");)
		{
			try (ResultSet resultSet = pstmt.executeQuery();)
			{
				while (resultSet.next())
				{
					System.out.println(resultSet.getInt(1) + "," + resultSet.getString(2) );
				}
			}
			catch (Exception e)
			{
				connection.rollback();
				e.printStackTrace();
			}
		}
	}

}
