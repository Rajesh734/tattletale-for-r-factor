/*
 * Copyright (C) 2016, Liberty Mutual Group
 *
 * Created on Dec 23, 2016
 */

package org.jboss.tattletale.utils;

/**
 * @author n0213628
 *
 */
public final class XmlUtils {
	public static String getTagValue(String xml, String tagName){
	    return xml.split("<"+tagName+">")[1].split("</"+tagName+">")[0];
	}
}
