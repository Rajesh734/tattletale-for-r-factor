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
package org.jboss.tattletale.analyzers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.tattletale.core.Archive;
import org.jboss.tattletale.core.JarArchive;
import org.jboss.tattletale.core.Location;
import org.jboss.tattletale.profiles.Profile;
import org.jboss.tattletale.utils.TattleTaleConstants;
import org.jboss.tattletale.utils.XmlUtils;

/**
 * Java archive scanner
 *
 * @author Jesper Pedersen <jesper.pedersen@jboss.org>
 * @author Navin Surtani
 */
public class JarScanner extends AbstractScanner
{

   /**
    * Scan an archive
    * @param file        The file
    * @return The archive
    */
   public Archive scan(File file)
   {
      return scan(file, null, null, null, null);
   }

   /**
    * Scan an archive
    *
    * @param file        The file
    * @param gProvides   The global provides map
    * @param known       The set of known archives
    * @param blacklisted The set of black listed packages
    * @return The archive
    */
   public Archive scan(File file, Map<String, SortedSet<String>> gProvides, List<Profile> known,
                       Set<String> blacklisted, Map<String, Object> otherInformation)
   {
      Archive archive = null;
      JarFile jarFile = null;
      String fileName = file.getName();
      Set<String> techSet = otherInformation.get(TattleTaleConstants.TECHNOLOGY) instanceof Set<?>
		? (Set<String>) otherInformation.get(TattleTaleConstants.TECHNOLOGY) 
		: new TreeSet<String>();
      try
      {
         String canonicalPath = file.getCanonicalPath();
         jarFile = new JarFile(file);
         Integer classVersion = null;
         SortedSet<String> requires = new TreeSet<String>();
         SortedMap<String, Long> provides = new TreeMap<String, Long>();
         SortedSet<String> profiles = new TreeSet<String>();
         SortedMap<String, SortedSet<String>> classDependencies = new TreeMap<String, SortedSet<String>>();
         SortedMap<String, SortedSet<String>> packageDependencies = new TreeMap<String, SortedSet<String>>();
         SortedMap<String, SortedSet<String>> blacklistedDependencies = new TreeMap<String, SortedSet<String>>();
         List<String> lSign = null;
         Enumeration<JarEntry> jarEntries = jarFile.entries();

         while (jarEntries.hasMoreElements())
         {
            JarEntry jarEntry = jarEntries.nextElement();
            String entryName = jarEntry.getName();
            InputStream entryStream = null;
            if (entryName.endsWith(".class"))
            {
               try
               {
                  entryStream = jarFile.getInputStream(jarEntry);
                  classVersion = scanClasses(entryStream, blacklisted, known, classVersion, provides, requires,
                        profiles, classDependencies, packageDependencies, blacklistedDependencies, otherInformation);
               }
               catch (Exception ie)
               {
                  ie.printStackTrace();
               }
               finally
               {
                  if (entryStream != null)
                  {
                     entryStream.close();
                  }
               }
            }
            else if (entryName.endsWith(".xml")) {
                InputStream is = null;
                try
					{
						is = jarFile.getInputStream(jarEntry);

						try (InputStreamReader isr = new InputStreamReader(is);
								LineNumberReader lnr = new LineNumberReader(isr)) {

							String s = lnr.readLine();
							while (s != null) {
								s = lnr.readLine();
								/*
								 * if(s.contains("dataSource")) { for(String
								 * attribute : s.split(" ")) {
								 * if(attribute.contains("version")) {
								 * techList.add("J2EE " +
								 * attribute.split("=")[1].replace("\"", ""));
								 * continue; } } } else if(s.contains("<ejb>"))
								 * { techList.add("EJB"); } else
								 */if (s.contains("<dataSourceType>")) {
									techSet.add(XmlUtils.getTagValue(s, "dataSourceType").trim());
								}
							}
						}
					}
                catch (Exception ie)
                {
                   // Ignore
                }
                
             
            
            } else if(entryName.contains("ibm") && entryName.endsWith(".xml")) {
            	otherInformation.put("SERVER", "WAS");
            } else if (entryName.contains("META-INF") && entryName.endsWith(".SF"))
            {
                InputStream is = null;
                try	{
 					is = jarFile.getInputStream(jarEntry);

 					try (InputStreamReader isr = new InputStreamReader(is);
 							LineNumberReader lnr = new LineNumberReader(isr)) {

 						if (lSign == null) {
 							lSign = new ArrayList<String>();
 						}

 						String s = lnr.readLine();
 						while (s != null) {
 							lSign.add(s);
 							s = lnr.readLine();
 						}
 					}
 				}
                catch (Exception ie)
                {
                  ie.printStackTrace();
                  System.err.println(ie);
                }
                
             }
         }

         if (provides.size() == 0)
         {
            return null;
         }

         String version = null;
         List<String> lManifest = null;
         Manifest manifest = jarFile.getManifest();
         if (manifest != null)
         {
            version = versionFromManifest(manifest);
            lManifest = readManifest(manifest);
         }
         Location location = new Location(fileName, canonicalPath, version);

         if (classVersion == null)
            classVersion = Integer.valueOf(0);

         archive = new JarArchive(fileName, classVersion, lManifest, lSign, requires, provides,
                     classDependencies, packageDependencies, blacklistedDependencies, location);
         addProfilesToArchive(archive, profiles);

         Iterator<String> it = provides.keySet().iterator();
         while (it.hasNext())
         {
            String provide = it.next();

            if (gProvides != null)
            {
               SortedSet<String> ss = gProvides.get(provide);
               if (ss == null)
               {
                  ss = new TreeSet<String>();
               }

               ss.add(archive.getName());
               gProvides.put(provide, ss);
            }

            requires.remove(provide);
         }
      }
      catch (IOException ioe)
      {
         ioe.printStackTrace();
         // Probably not a JAR archive
      }
      catch (Exception e)
      {
         System.err.println("Scan: " + e.getMessage());
         e.printStackTrace(System.err);
      }
      finally
      {
         try
         {
            if (jarFile != null)
            {
               jarFile.close();
            }
         }
         catch (IOException ioe)
         {
            // Ignore
         }
      }
      return archive;
   }

}
