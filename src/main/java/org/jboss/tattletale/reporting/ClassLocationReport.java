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
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jboss.tattletale.Main;
import org.jboss.tattletale.utils.TattleTaleConstants;
import org.jboss.tattletale.utils.TattleTaleDataSource;

/**
 * Class location report
 *
 * @author Jesper Pedersen <jesper.pedersen@jboss.org>
 * @author <a href="mailto:torben.jaeger@jit-consulting.de">Torben Jaeger</a>
 */
public class ClassLocationReport extends AbstractReport
{
   /** NAME */
   private static final String NAME = "Class Location";

   /** DIRECTORY */
   private static final String DIRECTORY = "classlocation";

   /** Globally provides */
   private SortedMap<String, SortedSet<String>> gProvides;

   /** Constructor */
   public ClassLocationReport()
   {
      super(DIRECTORY, ReportSeverity.INFO, NAME, DIRECTORY);
   }

   /**
    * Set the globally provides map to be used in generating this report
    *
    * @param gProvides the map of global provides
    */
   public void setGlobalProvides(SortedMap<String, SortedSet<String>> gProvides)
   {
      this.gProvides = gProvides;
   }

   /**
    * write the report's content
    *
    * @param bw the BufferedWriter to use
    * @throws IOException if an error occurs
    */
   @Override
   public void writeHtmlBodyContent(BufferedWriter bw) throws IOException
   {
      bw.write("<table>" + Dump.newLine());

      bw.write("  <tr>" + Dump.newLine());
      bw.write("     <th>Class</th>" + Dump.newLine());
      bw.write("     <th>Jar files</th>" + Dump.newLine());
      bw.write("  </tr>" + Dump.newLine());
      Set<String> technologiesUsed = new TreeSet<String>();
      boolean odd = true;
      Set<String> dependentJars = new TreeSet<String>();
      for (Map.Entry<String, SortedSet<String>> entry : gProvides.entrySet())
      {
         String clz = (String) ((Map.Entry) entry).getKey();
         SortedSet archives = (SortedSet) ((Map.Entry) entry).getValue();
         boolean filtered = isFiltered(clz);

         if (!filtered)
         {
            if (archives.size() > 1)
            {
               status = ReportStatus.YELLOW;
            }
         }

         if (odd)
         {
            bw.write("  <tr class=\"rowodd\">" + Dump.newLine());
         }
         else
         {
            bw.write("  <tr class=\"roweven\">" + Dump.newLine());
         }
         bw.write("     <td>" + clz + "</td>" + Dump.newLine());
         if (!filtered)
         {
            bw.write("        <td>");
         }
         else
         {
            bw.write("        <td style=\"text-decoration: line-through;\">");
         }

         Iterator sit = archives.iterator();
         while (sit.hasNext())
         {
            String archive = (String) sit.next();
            int finalDot = archive.lastIndexOf(".");
            String extension = archive.substring(finalDot + 1);

            bw.write("<a href=\"../" + extension + "/" + archive + ".html\">" + archive + "</a>" + Dump.newLine());
            dependentJars.add(archive);
            Pattern pattern = Pattern.compile("(.*)-(\\d)*(\\.\\d)*(\\.)*([a-zA-Z])*(\\.jar)*$");
            Matcher matcher = pattern.matcher(archive);
            String jarNameOnly = "";
            if(matcher.find()) {
            	jarNameOnly= matcher.group(1);
            }
            try {
          	  BasicDataSource dataSource = TattleTaleDataSource.getDataSource();
          	  try (Connection connection = dataSource.getConnection();
        				PreparedStatement pstmt = connection.prepareStatement("SELECT tech_name "
        						+ "FROM r_factor.tech_based_on_jar_t techjar "
        						+ "JOIN r_factor.technology_t tech on tech.id = techjar.tech_id "
        						+ "WHERE ? LIKE CONCAT(CONCAT('%',dep_jar_names),'%') ;");
            		 )
        		{
          		  pstmt.setString(1, jarNameOnly);
          		try (ResultSet resultSet = pstmt.executeQuery();)
    			{
    				
    				while (resultSet.next())
    				{
    					//System.out.println(dep);
    					technologiesUsed.add(resultSet.getString(1));
    					//System.out.println(resultSet.getInt(1) + "," + resultSet.getString(2) );
    				}
    			}
    			catch (Exception e)
    			{
    				connection.rollback();
    				e.printStackTrace();
    			}
        		}
            } catch (Exception e) {
          	  
            }
            if (sit.hasNext())
            {
               bw.write(", ");
            }
         }

         bw.write("</td>" + Dump.newLine());
         bw.write("  </tr>" + Dump.newLine());

         odd = !odd;
      }
      if(null == Main.otherInformation.get(TattleTaleConstants.DEPENDENT_JARS)) {
    	  Main.otherInformation.put(TattleTaleConstants.DEPENDENT_JARS, dependentJars);
      } else {
    	  ((Set<String>)Main.otherInformation.get(TattleTaleConstants.DEPENDENT_JARS)).addAll(dependentJars);
      }
      ((Set)Main.otherInformation.get(TECHNOLOGY)).addAll(technologiesUsed);
      
      
      
      
      bw.write("</table>" + Dump.newLine());
   }

   @Override
   public void writeHtmlBodyHeader(BufferedWriter bw) throws IOException
   {
      bw.write("<body>" + Dump.newLine());
      bw.write(Dump.newLine());

      bw.write("<h1>" + NAME + "</h1>" + Dump.newLine());

      bw.write("<a href=\"../index.html\">Main</a>" + Dump.newLine());
      bw.write("<p>" + Dump.newLine());
   }

   /**
    * Create filter
    *
    * @return The filter
    */
   @Override
   protected Filter createFilter()
   {
      return new KeyFilter();
   }
}
