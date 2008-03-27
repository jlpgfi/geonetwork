//=============================================================================
//===	Copyright (C) 2001-2007 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.kernel;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import jeeves.constants.Jeeves;
import jeeves.resources.dbms.Dbms;
import jeeves.server.UserSession;
import jeeves.utils.Util;
import jeeves.utils.Xml;
import org.fao.geonet.util.ISODate;
import org.jdom.Element;

//=============================================================================

/** This class is responsible of reading and writing xml on the database. It
  * works on tables like (id, data, lastChangeDate)
  */

public class XmlSerializer
{
	//--------------------------------------------------------------------------
	//---
	//--- API
	//---
	//--------------------------------------------------------------------------

	/** Retrieve the xml element which id matches the given one. The element is
	  * read from 'table' and the string read is converted into xml
	  */

	public static Element select(Dbms dbms, String table, String id) throws Exception
	{
		String query = "SELECT * FROM " + table + " WHERE id = ?";

		Element rec = dbms.select(query, new Integer(id)).getChild(Jeeves.Elem.RECORD);

		if (rec == null)
			return null;

		String xmlData = rec.getChildText("data");

//		if (!xmlData.startsWith("<?xml"))
//			xmlData  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" + xmlData;

		rec = Xml.loadString(xmlData, false);

		return (Element) rec.detach();
	}

	//--------------------------------------------------------------------------

	public static String insert(Dbms dbms, String schema, Element xml, int serial,
										 String source, String uuid, int owner, String groupOwner) throws SQLException
	{
		return insert(dbms, schema, xml, serial, source, uuid, null, null, "n", null, owner, groupOwner);
	}

	//--------------------------------------------------------------------------

	public static String insert(Dbms dbms, String schema, Element xml, int serial,
										 String source, String uuid, String isTemplate,
										 String title, int owner, String groupOwner) throws SQLException
	{
		return insert(dbms, schema, xml, serial, source, uuid, null, null, isTemplate, title, owner, groupOwner);
	}

	//--------------------------------------------------------------------------

	public static String insert(Dbms dbms, String schema, Element xml, int serial,
										 String source, String uuid, String createDate,
										 String changeDate, String isTemplate, String title,
										 int owner, String groupOwner) throws SQLException
	{
		String date = new ISODate().toString();

		if (createDate == null)
			createDate = date;

		if (changeDate == null)
			changeDate = date;

		fixCR(xml);

		StringBuffer fields = new StringBuffer("id, schemaId, data, createDate, changeDate, source, "+
															"uuid, isTemplate, isHarvested, root, owner");
		StringBuffer values = new StringBuffer("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");

		Vector args = new Vector();
		args.add(new Integer(serial));
		args.add(schema);
		args.add(Xml.getString(xml));
		args.add(createDate);
		args.add(changeDate);
		args.add(source);
		args.add(uuid.toLowerCase());
		args.add(isTemplate);
		args.add("n");
		args.add(xml.getQualifiedName());
		args.add(owner);

		if (groupOwner != null) {
			fields.append(", groupOwner");
			values.append(", ?");
			args.add(new Integer(groupOwner));
		}

		if (title != null)
		{
			fields.append(", title");
			values.append(", ?");
			args.add(title);
		}

		String query = "INSERT INTO Metadata (" + fields + ") VALUES(" + values + ")";
		dbms.execute(query, args.toArray());

		return Integer.toString(serial);
	}

	//--------------------------------------------------------------------------
	/** Updates an xml element into the database. The new data replaces the old one
	  */

	public static void update(Dbms dbms, String id, Element xml) throws SQLException
	{
		update(dbms, id, xml, null);
	}

	//--------------------------------------------------------------------------

	public static void update(Dbms dbms, String id, Element xml, String changeDate) throws SQLException
	{
		String query = "UPDATE Metadata SET data=?, changeDate=?, root=? WHERE id=?";

		Vector args = new Vector();

		fixCR(xml);
		args.add(Xml.getString(xml));

		if (changeDate == null)	args.add(new ISODate().toString());
			else                 args.add(changeDate);

		args.add(xml.getQualifiedName());
		args.add(new Integer(id));

		dbms.execute(query, args.toArray());
	}

	//--------------------------------------------------------------------------
	/** Deletes an xml element given its id
	  */

	public static void delete(Dbms dbms, String table, String id) throws SQLException
	{
		String query = "DELETE FROM " + table + " WHERE id="+id;

		dbms.execute(query);
	}
	//--------------------------------------------------------------------------

	private static void fixCR(Element xml)
	{
		List list = xml.getChildren();

		if (list.size() == 0)
		{
			String text = xml.getText();

			xml.setText(Util.replaceString(text, "\r\n", "\n"));
		}

		else for (Object o : list)
			fixCR((Element) o);
	}

	//---------------------------------------------------------------------------

//	public static void dump(String name, String text)
//	{
//		System.out.print("name: "+name+", value:");
//
//		char[] chars = text.toCharArray();
//
//		for(char c: chars)
//		{
//			int i = c;
//
//			if (i>=32)	System.out.print(c);
//				else		System.out.print("["+ i +"]");
//		}
//
//		System.out.println("");
//	}
}

//=============================================================================

