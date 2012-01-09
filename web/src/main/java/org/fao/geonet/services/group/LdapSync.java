package org.fao.geonet.services.group;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.net.ssl.SSLHandshakeException;

import jeeves.exceptions.JeevesException;
import jeeves.interfaces.Service;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;

import org.fao.geonet.constants.Geonet;
import org.jdom.Element;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSEStartTLSFactory;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldap.LDAPJSSESecureSocketFactory;

/** Returns a specific group given its id
  */

public class LdapSync implements Service
{
	
	private String sldapHost;
	private String sldapPort;
	private String sldapBase;
	private String[] sldapAtt;
	private String sldapDn;
	private String sldapPwd;
	
	private int ildapPort ;
	
	private Exception isDisabled = null ;
	
	// This class allows to describe very basically a group
	
	private class ElementLevelGroup
	{
		private int gid ;
		private String groupName ;
		private String groupDescription ;
		
		public ElementLevelGroup (int _gid, String _groupName, String _gD)
		{
			gid = _gid ;
			groupDescription = new String(_gD);
			groupName  = new String(_groupName) ;
		}
		
		public ElementLevelGroup (int _gid, String _groupName)
		{
			gid = _gid ;
			groupDescription = new String();
			groupName  = new String(_groupName) ;
		}

		public int getGid() { return gid ; }
		public String  getName() {return groupName ; }
		public String getDescription () { return groupDescription; }
	}
	
	
	public void init(String appPath, ServiceConfig params) throws Exception
	{
		try 
		{
			sldapHost = params.getValue("LDAPhost");                   // drebretagne-geobretagne.int.lsn.camptocamp.com
			sldapPort = params.getValue("LDAPport");                   // 389 / 636
			sldapBase = params.getValue("LDAPbase");                   // dc=geobretagne,dc=fr
			sldapAtt  = params.getValue("LDAPattributes").split(",");  // {cn,gidNumbers}
			sldapDn   = params.getValue("LDAPbindDn");                 // cn=admin,dc=geobretagne,dc=fr
			sldapPwd  = params.getValue("LDAPbindPassword");           // blahblah :-)
			
			LDAPConnection lc ;
						
			try 
			{
				ildapPort = Integer.parseInt(sldapPort);
			} catch (NumberFormatException nfe)
			{
				ildapPort = LDAPConnection.DEFAULT_PORT;
			}
			if (ildapPort == 636)
			{
				lc = new LDAPConnection(new LDAPJSSESecureSocketFactory());							
			}
			else // (ildapPort == 389) or else, assume it's not a SSL connection
			{
				lc = new LDAPConnection();
			}
			
			lc.connect(sldapHost, ildapPort);

			lc.bind(LDAPConnection.LDAP_V3, sldapDn, sldapPwd.getBytes());
			
			lc.disconnect();
			
			isDisabled = null;
			

		}
		catch (Exception e)
		{
			isDisabled = e;
		}
		
	}
	

	
	//--------------------------------------------------------------------------
	//---
	//--- Service
	//---
	//--------------------------------------------------------------------------

	public synchronized Element exec(Element params, ServiceContext context) throws Exception
	{
		
		if (isDisabled != null)
		{
			throw new LdapSyncException("Error while initializing synchronization service. Service disabled", isDisabled);
		}
		
		ArrayList<ElementLevelGroup> gEnum = new ArrayList<ElementLevelGroup>();
		
		// Step 1 : LDAP connection / bind
		try 
		{
			LDAPConnection lc ;		
			
			if (ildapPort == 636)
			{
				lc = new LDAPConnection(new LDAPJSSESecureSocketFactory());			
			}
			else // (ildapPort == 389), assume the LDAP connection is not SSL.
			{
				lc = new LDAPConnection();
			}
			
			lc.connect(sldapHost, ildapPort);

			lc.bind(LDAPConnection.LDAP_V3, sldapDn, sldapPwd.getBytes());

			LDAPSearchResults searchResults = lc.search(sldapBase,
					lc.SCOPE_SUB, null, sldapAtt, false);
	
			while (searchResults.hasMore()) // iteration on each LDAP entries
			{
				LDAPEntry nextEntry = searchResults.next();
				LDAPAttributeSet attributeSet = nextEntry.getAttributeSet();
			
				Iterator allAttributes = attributeSet.iterator();

				int tempGid = -1;
				String tempGrName = null;
				String tempGrDesc = new String();
				
				while (allAttributes.hasNext()) // iteration on each entry attributes
				{
					LDAPAttribute attribute = (LDAPAttribute) allAttributes.next();
					String attributeName = attribute.getName();
					Enumeration allValues = attribute.getStringValues();

					if (attributeName.equalsIgnoreCase("gidNumber"))
					{
						while (allValues.hasMoreElements())
						{
							String value = (String) allValues.nextElement();
							tempGid = Integer.parseInt(value);
						}
					}
					
					else if (attributeName.equalsIgnoreCase("cn"))
					{
						while (allValues.hasMoreElements())
						{
							String value = (String) allValues.nextElement();
							// Match only the ElementLevel groups
							// PIGMA issue #2438: need to match SP_* too
							if ((value.startsWith("EL_") == true) || (value.startsWith("SP_") == true))
							{
								tempGrName = value.substring(3);
							}	
						}
					}
					else if (attributeName.equalsIgnoreCase("description"))
					{
						while (allValues.hasMoreElements())
						{
							tempGrDesc = (String) allValues.nextElement();
						}
					}
					
				} // end iterating on attributes
				if ((tempGid > 1) && (tempGrName != null))
				{
					// some hardcoded filters on group names (issue #1532)
					if (! tempGrName.startsWith("CA_") && ! tempGrName.startsWith("ED_") && ! tempGrName.startsWith("PT_"))
					{
						gEnum.add(new ElementLevelGroup(tempGid, tempGrName, tempGrDesc));	
					}
				}
			} // end search result
			lc.disconnect();
			
		} catch (LDAPException e)
		{
			throw new LdapSyncException("Error while fetching datas from LDAP", e);
			
		} // we now have our groups ; end of LDAP connection
		
				
		// Step 3 : drop every existing groups infos from the GN db
		// (we are trustworthy on LDAP infos)
		
		// /!\ What about integrity constraints into the postgresql DB ?
		// We do not want a drop cascade as well, since it will nuke
		// every existing access rights (operationallowed table) and
		// groups is linked to groupsdes as well.
		
		// -> We have to drop temporarily the integrity constraints
		// This implies that the user can modify the db structure
		
		// /!\ This may be COMPLETELY PostGreSQL specific /!\
		try
		{
			Dbms dbms = (Dbms) context.getResourceManager().open (Geonet.Res.MAIN_DB);

			dbms.execute("ALTER TABLE groupsdes DROP CONSTRAINT groupsdes_iddes_fkey");
			dbms.execute("ALTER TABLE metadata DROP CONSTRAINT metadata_groupowner_fkey");
			dbms.execute("ALTER TABLE operationallowed DROP CONSTRAINT operationallowed_groupid_fkey");
			dbms.execute("ALTER TABLE usergroups DROP CONSTRAINT usergroups_groupid_fkey");
		
			// groups <= 1 are GeoNetwork-reserved 
			dbms.execute("delete FROM groups WHERE id >= 2");
		
			// prepare the special group "Orphan"
			Element elTestOrphan = dbms.select("SELECT * FROM groups WHERE id = -2");
		
			if (elTestOrphan.getChildren().size() == 0)
			{
				dbms.execute("INSERT INTO groups VALUES (-2, 'ORPHAN', 'Orphan group', null, null)");
				
				// Ensure the table is clean before trying to do the following insertions
				dbms.execute("DELETE FROM groupsdes WHERE iddes = -2");
				// insert some descriptions in order to preserve integrity of the database
				dbms.execute("INSERT INTO groupsdes VALUES(-2,'cn', 'Orphan group')");
				dbms.execute("INSERT INTO groupsdes VALUES(-2,'de', 'Orphan group')");
				dbms.execute("INSERT INTO groupsdes VALUES(-2,'en', 'Orphan group')");
				dbms.execute("INSERT INTO groupsdes VALUES(-2,'fr', 'Groupe orphelin')");
				dbms.execute("INSERT INTO groupsdes VALUES(-2,'es', 'Orphan group')");
				dbms.execute("INSERT INTO groupsdes VALUES(-2,'nl', 'Orphan group')");
				dbms.execute("INSERT INTO groupsdes VALUES(-2,'ru', 'Orphan group')");
				
			}
		
			
			// Step 4 : Iterate on groups (name, id) :

		
			// /!\ Same problem as in Sextant : What if someone create a ServiceLevel
			// group named "*_SV_EL_...*" ? We have to make sure that the application
			// cannot allow such a case.

			for (int i = 0; i < gEnum.size(); i++)
			{
				int iGid = gEnum.get(i).getGid();
				String grName = gEnum.get(i).getName();
				String grDesc = gEnum.get(i).getDescription();
				// Some minor verification, but this
				// *should* not happen
				if (iGid >= 2)
				{
					dbms.execute("INSERT INTO groups VALUES (?, ?,  ?, null, null)",iGid,grName,grDesc);
					dbms.execute("DELETE FROM groupsdes WHERE iddes = ?", iGid);
					dbms.execute("INSERT INTO groupsdes VALUES(?, 'cn', ?)", iGid, grName);
					dbms.execute("INSERT INTO groupsdes VALUES(?, 'de', ?)", iGid, grName);
					dbms.execute("INSERT INTO groupsdes VALUES(?, 'en', ?)", iGid, grName);
					dbms.execute("INSERT INTO groupsdes VALUES(?, 'fr', ?)", iGid, grName);
					dbms.execute("INSERT INTO groupsdes VALUES(?, 'es', ?)", iGid, grName);
					dbms.execute("INSERT INTO groupsdes VALUES(?, 'nl', ?)", iGid, grName);
					dbms.execute("INSERT INTO groupsdes VALUES(?, 'ru', ?)", iGid, grName);
				}
			}
		
		
			// Step 5 : add every orphans to groupId -2 (orphan)
		
			// The groupsdes table is a bit specific since it does not
			// seem to be used. Let's drop every entries that have no relationship
			// with the groups table anymore
			dbms.execute("DELETE FROM groupsdes WHERE iddes NOT in (SELECT id from groups)");
			// This is an UPDATE since we want to keep the metadatas
			dbms.execute("UPDATE metadata SET groupowner = -2 WHERE groupowner NOT in (SELECT id from groups)");
		
			// Orphaned metadatas will lose the access rights currently defined
			// (or else there are some risks of failure relying on the db constraints checks
			// Then, only the administrator could have the right to do something with these orphaned metadatas
			dbms.execute("DELETE FROM operationallowed WHERE groupid NOT in (SELECT id from groups)");
		
			// Same here : Nonsense to try to put some users into groups that does not exist
			// anymore (and this could lead to integrity error) ; let's remove the unwanted groups
			dbms.execute("DELETE FROM usergroups WHERE groupid NOT in (SELECT id from groups)");
		
			// Step 6 : restores the integrity constraints
		
			dbms.execute("ALTER TABLE groupsdes ADD CONSTRAINT groupsdes_iddes_fkey FOREIGN KEY(iddes) REFERENCES groups(id)");
			dbms.execute("ALTER TABLE metadata ADD CONSTRAINT metadata_groupowner_fkey FOREIGN KEY(groupowner) REFERENCES groups(id)");
			dbms.execute("ALTER TABLE operationallowed ADD CONSTRAINT operationallowed_groupid_fkey FOREIGN KEY(groupid) REFERENCES groups(id)");
			dbms.execute("ALTER TABLE usergroups ADD CONSTRAINT usergroups_groupid_fkey FOREIGN KEY(groupid) REFERENCES groups(id)");
		
			return new Element("success");	
		
		} catch (Exception e)
		{
			throw new LdapSyncException("Error while trying to synchronize LDAP against PostGreSQL", e);
		}
	}		
}
