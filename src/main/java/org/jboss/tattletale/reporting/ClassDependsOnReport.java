/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.tattletale.reporting;

import static org.jboss.tattletale.utils.TattleTaleConstants.TECHNOLOGY;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.jboss.tattletale.Main;
import org.jboss.tattletale.core.Archive;
import org.jboss.tattletale.core.NestableArchive;
import org.jboss.tattletale.utils.TattleTaleConstants;
import org.jboss.tattletale.utils.TattleTaleDataSource;

/**
 * Class level Depends On report
 *
 * @author Jesper Pedersen <jesper.pedersen@jboss.org>
 */
public class ClassDependsOnReport extends CLSReport
{
   /** NAME */
   private static final String NAME = "Class Depends On";

   /** DIRECTORY */
   private static final String DIRECTORY = "classdependson";


   /** Constructor */
   public ClassDependsOnReport()
   {
      super(DIRECTORY, ReportSeverity.INFO, NAME, DIRECTORY);
   }


   /**
    * write out the report's content
    *
    * @param bw the writer to use
    * @throws IOException if an error occurs
    */
   public void writeHtmlBodyContent(BufferedWriter bw) throws IOException
   {
	   Set<String> technologiesUsed = new TreeSet<String>();
      bw.write("<table>" + Dump.newLine());

      bw.write("  <tr>" + Dump.newLine());
      bw.write("     <th>Class</th>" + Dump.newLine());
      bw.write("     <th>Depends On</th>" + Dump.newLine());
      bw.write("  </tr>" + Dump.newLine());

      SortedMap<String, SortedSet<String>> result = new TreeMap<String, SortedSet<String>>();

      for (Archive archive : archives)
      {
         SortedMap<String, SortedSet<String>> classDependencies = getClassDependencies(archive);

         Iterator<Map.Entry<String, SortedSet<String>>> dit = classDependencies.entrySet().iterator();
         while (dit.hasNext())
         {
            Map.Entry<String, SortedSet<String>> entry = dit.next();
            String clz = entry.getKey();
            SortedSet<String> deps = entry.getValue();

            SortedSet<String> newDeps = new TreeSet<String>();

            for (String dep : deps)
            {
               if (!dep.equals(clz))
               {
                  newDeps.add(dep);
               }
            }

            result.put(clz, newDeps);
         }
      }

      Iterator<Map.Entry<String, SortedSet<String>>> rit = result.entrySet().iterator();
      boolean odd = true;

      //rit instance has all Classes and its dependencies.
      /*
       * TODO use this rit instance to query the DB to find the technology.
       * Map the technology result to a HashMap to present that as a result. 
       * Using the above Map, calculate the score for each technology and present the complexity report.
       */
      try {
    	  BasicDataSource dataSource = TattleTaleDataSource.getDataSource();
      try (Connection connection = dataSource.getConnection();
				PreparedStatement pstmt = connection.prepareStatement("Select tech.* from "
						+ "r_factor.technology_t tech join r_factor.api a on a.tech_id=tech.id "
						+ "where a.api LIKE ?");
    		  PreparedStatement insertDepClassesStmt = connection.prepareStatement("INSERT INTO R_FACTOR.DEP_CLASS_T (class_name, dependent_classes) "
						+ "VALUES (?, ?) "))
		{
    	  while (rit.hasNext())
          {
             Map.Entry<String, SortedSet<String>> entry = rit.next();
             String clz = entry.getKey();
             SortedSet<String> deps = entry.getValue();
             //System.out.println(clz);
             //TODO parse only give package
             boolean canWeProcessThisClass = false;
             for(String packageName : Main.packagesToBeScanned) {
            	 if(clz.contains(packageName)) {
            		 canWeProcessThisClass = true;
            	 }
             }
             if(!canWeProcessThisClass) {
            	 continue;
             }
             canWeProcessThisClass = false;
             if (odd)
             {
                bw.write("  <tr class=\"rowodd\">" + Dump.newLine());
             }
             else
             {
                bw.write("  <tr class=\"roweven\">" + Dump.newLine());
             }
             bw.write("     <td>" + clz + "</a></td>" + Dump.newLine());
             bw.write("     <td>");
             bw.write("<pre>");

             Iterator<String> sit = deps.iterator();
             while (sit.hasNext())
             {
                String dep = sit.next();
                bw.write(dep);
                pstmt.setString(1, dep.substring(0,dep.lastIndexOf(".")) +"%");
    			try (ResultSet resultSet = pstmt.executeQuery();)
    			{
    				
    				while (resultSet.next())
    				{
    					//System.out.println(dep);
    					technologiesUsed.add(resultSet.getString(2));
    					//System.out.println(resultSet.getInt(1) + "," + resultSet.getString(2) );
    				}
    			}
    			catch (Exception e)
    			{
    				connection.rollback();
    				e.printStackTrace();
    			}

                if (sit.hasNext())
                {
                   bw.write("\n");
                }
             }
             
             insertDepClassesStmt.setString(1, clz);
             insertDepClassesStmt.setString(2, StringUtils.join(deps, ','));
             insertDepClassesStmt.execute();
             bw.write("</pre>");
             bw.write("</td>" + Dump.newLine());
             bw.write("  </tr>" + Dump.newLine());

             odd = !odd;
          }
    	  
		}
      } catch (Exception e) {
    	  e.printStackTrace();
    	  System.exit(1);
      }
      ((Set)Main.otherInformation.get(TECHNOLOGY)).addAll(technologiesUsed);
      bw.write("</table>" + Dump.newLine());
   }

   private SortedMap<String, SortedSet<String>> getClassDependencies(Archive archive)
   {
      SortedMap<String, SortedSet<String>> classDeps = new TreeMap<String, SortedSet<String>>();

      if (archive instanceof NestableArchive)
      {
         NestableArchive nestableArchive = (NestableArchive) archive;
         List<Archive> subArchives = nestableArchive.getSubArchives();

         for (Archive sa : subArchives)
         {
            classDeps.putAll(getClassDependencies(sa));
         }

         classDeps.putAll(nestableArchive.getClassDependencies());
      }
      else
      {
         classDeps.putAll(archive.getClassDependencies());
      }
      return classDeps;
   }


   /**
    * write out the header of the report's content
    *
    * @param bw the writer to use
    * @throws IOException if an error occurs
    */
   public void writeHtmlBodyHeader(BufferedWriter bw) throws IOException
   {
      bw.write("<body>" + Dump.newLine());
      bw.write(Dump.newLine());

      bw.write("<h1>" + NAME + "</h1>" + Dump.newLine());

      bw.write("<a href=\"../index.html\">Main</a>" + Dump.newLine());
      bw.write("<p>" + Dump.newLine());
   }
}
