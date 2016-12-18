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
package org.jboss.tattletale.core;

import java.io.Serializable;

/**
 * Location
 *
 * @author Jesper Pedersen <jesper.pedersen@jboss.org>
 */
public class Location implements Serializable, Comparable {

	/** SerialVersionUID */
	private static final long serialVersionUID = -1914129664725967921L;

	/** The file Name */
	private String fileName;

	/** The file Name with complete path */
	private String fileNameWithFullPath;

	/** Version */
	private String version;

	/**
	 * Constructor
	 *
	 * @param fileNameWithPath
	 *            The file name with full path
	 * @param version
	 *            The version
	 */
	public Location(String fileNameWithPath, String version) {
		this.fileNameWithFullPath = fileNameWithPath;
		this.version = version;
	}

	/**
	 * @param fileName
	 *            The file name
	 * @param fileNameWithPath
	 *            The file name with full path
	 * @param version
	 *            The version
	 */
	public Location(String fileName, String fileNameWithPath, String version) {
		this.fileName = fileName;
		this.fileNameWithFullPath = fileNameWithPath;
		this.version = version;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Get the filename
	 *
	 * @return The value
	 */
	public String getFileNameWithFullPath() {
		return fileNameWithFullPath;
	}

	/**
	 * Get the version
	 *
	 * @return The value
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Comparable
	 *
	 * @param o
	 *            The other object
	 * @return The compareTo value
	 */
	public int compareTo(Object o) {
		Location l = (Location) o;

		int result = fileName.compareTo(l.getFileName());

		if (result == 0) {
			result = (version != null ? version.compareTo(l.getVersion()) : 0);
		}

		return result;
	}

	/**
	 * Equals
	 *
	 * @param obj
	 *            The other object
	 * @return True if equals; otherwise false
	 */
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Location)) {
			return false;
		}

		Location l = (Location) obj;

		return fileName.equals(l.fileName) && fileNameWithFullPath.equals(l.getFileNameWithFullPath())
				&& (version != null ? version.equals(l.getVersion()) : true);
	}

	/**
	 * Hash code
	 *
	 * @return The hash code
	 */
	public int hashCode() {
		int hash = 7;

		if (null != fileNameWithFullPath) {
			hash += 31 * fileNameWithFullPath.hashCode();
		}

		if (null != fileName) {
			hash += 31 * fileName.hashCode();
		}

		if (version != null) {
			hash += 31 * version.hashCode();
		}

		return hash;
	}

	/**
	 * String representation
	 *
	 * @return The string
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb = sb.append(getClass().getName());
		sb = sb.append("(\n");

		sb = sb.append("fileName=");
		sb = sb.append(fileName);
		sb = sb.append("\n");

		sb = sb.append("fileNameWithFullPath=");
		sb = sb.append(fileNameWithFullPath);
		sb = sb.append("\n");

		sb = sb.append("version=");
		sb = sb.append(version);
		sb = sb.append("\n");

		sb = sb.append(")");

		return sb.toString();
	}
}
