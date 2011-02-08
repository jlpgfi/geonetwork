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

package jeeves.guiservices.services;

import jeeves.constants.Jeeves;
import jeeves.interfaces.Service;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ProfileManager;
import jeeves.server.ServiceConfig;
import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.filter.Filter;

import java.sql.SQLException;
import java.util.*;

//=============================================================================

public class SecProxyLogin implements Auth
{

	String  groupName;
    private HashMap<String, String> rolesMapping ;
	HashSet outFields;

	//--------------------------------------------------------------------------
	//---
	//--- Init
	//---
	//--------------------------------------------------------------------------

    public void init(String appPath, ServiceConfig params) throws Exception {
        rolesMapping = new HashMap<String, String>();

        rolesMapping.put(Profile.ADMINISTRATOR, params.getValue(Profile.ADMINISTRATOR));
        rolesMapping.put(Profile.REVIEWER, params.getValue(Profile.REVIEWER));
        rolesMapping.put(Profile.EDITOR, params.getValue(Profile.EDITOR));
        rolesMapping.put(Profile.REGISTEREDUSER, params.getValue(Profile.REGISTEREDUSER));
        rolesMapping.put(Profile.GUEST, params.getValue(Profile.GUEST));
    }

	//--------------------------------------------------------------------------
	//---
	//--- Service
	//---
	//--------------------------------------------------------------------------

    public Element exec(Element params, ServiceContext context) throws Exception
    {
        UserSession session = context.getUserSession();

        authenticationUser(context, session);
        if(session.getUserId()!=null){
            syncUserRoles(context, session);
        }


        /* END PMT security-proxy Mod */

        String sUsername = session.getUsername();
        String sName     = session.getName();
        String sSurname  = session.getSurname();
        String sProfile  = session.getProfile();

        if (sUsername == null)
            sUsername = ProfileManager.GUEST;

        if (sName == null)
            sName = ProfileManager.GUEST;

        if (sSurname == null)
            sSurname = "";

        if (sProfile == null)
            sProfile = ProfileManager.GUEST;

        Element userId   = new Element("userId")  .addContent(session.getUserId());
        Element username = new Element("username").addContent(sUsername);
        Element name     = new Element("name")    .addContent(sName);
        Element surname  = new Element("surname") .addContent(sSurname);
        Element profile  = new Element("profile") .addContent(sProfile);

        Element sEl = new Element(Jeeves.Elem.SESSION)
            .addContent(userId)
            .addContent(username)
            .addContent(name)
            .addContent(surname)
            .addContent(profile);

        if (groupName != null)
        {
            Hashtable group = (Hashtable)session.getProperty(groupName);
            if (group != null)
            {
                Element gEl = new Element(groupName);
                for (Enumeration i = group.elements(); i.hasMoreElements();)
                {
                    Element child = (Element)i.nextElement();
                    if (outFields == null || outFields.contains(child.getName()))
                         gEl.addContent((Element)child.clone());
                }
                sEl.addContent(gEl);
            }
        }
        return sEl;
    }

    private synchronized void syncUserRoles(ServiceContext context, UserSession session) throws Exception {
        final String GROUP_PREFIX = "ROLE_EL_";
        final Integer userId = Integer.valueOf(session.getUserId());
        final String[] roles = lookUpRoles(context);
        final Dbms dbms = (Dbms) context.getResourceManager().open ("main-db");
        final String query = "INSERT INTO usergroups VALUES (?,?)";
        final String groupIdQuery = "SELECT id FROM groups WHERE name ILIKE ?";

        dbms.execute("DELETE FROM usergroups WHERE userid = ?", userId);
        for (String role : roles) {
            if(role.startsWith(GROUP_PREFIX)){
                String group = role.substring(GROUP_PREFIX.length());
                Element groupId = dbms.select(groupIdQuery, group);
                Iterator ids = groupId.getDescendants(new Filter() {

                    public boolean matches(Object arg0) {
                        if (arg0 instanceof Text) {
                            Text text = (Text) arg0;
                            return "id".equals(text.getParentElement().getName()) && text.getTextTrim().length()>0;
                        }
                        return false;
                    }
                });

                if(ids.hasNext()){
                    Text idText = (Text) ids.next();
                    dbms.execute(query, userId, Integer.valueOf(idText.getTextTrim()));
                }
            }
        }
    }


	//--------------------------------------------------------------------------

    private String lookUpUsername(ServiceContext context) {
        return lookupParam(context, "sec-username");
    }
    private String lookUpEmail(ServiceContext context) {
        return lookupParam(context, "sec-email");
    }
    private String[] lookUpRoles(ServiceContext context) {
        String rolesStr = lookupParam(context, "sec-roles");

        if (rolesStr != null)
        {
            return rolesStr.split(",");
        } else {
            return new String[0];
        }
    }
    private String lookupParam(ServiceContext context, String key) {
        Map<String, String> spHttpParams =  context.getHeaders();

        return (String) spHttpParams.get(key);
    }



    private void authenticationUser(ServiceContext context, UserSession session) throws Exception {
        /* PMT C2C modifications */

		/* This aims to add the security-proxy support
		 * for special headers and auto-authenticate
		 * if necessary
		 */


		String spUser = lookUpUsername(context);
		String[] spRoles = lookUpRoles(context);

		String curProfile = null;


		for (int i = 0 ; i < spRoles.length ; i++)
		{
			/* admin */
            final String currentRole = spRoles[i];
            if (currentRole.equals(rolesMapping.get(Profile.ADMINISTRATOR)))
			{
				curProfile = Profile.ADMINISTRATOR;
				/* stop here since this is the stronger profile */
				break;
			}
			else if (currentRole.equals(rolesMapping.get(Profile.REVIEWER)))
			{
				curProfile = Profile.REVIEWER;
				continue;
			}
			else if (currentRole.equals(rolesMapping.get(Profile.EDITOR)))
			{
				curProfile = Profile.EDITOR;
				continue;
			}
			else if (currentRole.equals(rolesMapping.get(Profile.REGISTEREDUSER)))
			{
				curProfile = Profile.REGISTEREDUSER;
				continue;
			}
			else if (currentRole.equals(rolesMapping.get(Profile.GUEST)))
			{
				curProfile = Profile.GUEST;
				continue;
			}
		}

		/* for loop passed, but no role designated
		 * we authenticate our user as being a guest
		 */
		if (curProfile == null)
		{
			curProfile = Profile.GUEST;
		}

		if (curProfile.equals(Profile.GUEST) == false)
		{
		    String email = lookUpEmail(context);
		    if(email==null) email = "";

	        String userId = lookupUserId(session, context, spUser, curProfile, email);
			session.authenticate(userId, spUser, spUser, " (" +curProfile +")", curProfile);
		}
		else // Guest profile : emptying session attributes
		{
			session.authenticate(null, null, null, null, null);
		}
    }

	private String lookupUserId(UserSession session, ServiceContext context, String userName, String profile, String email) throws Exception {
	    if(session.getUsername() == userName && session.getUserId()!=null) {
	        return session.getUserId();
	    }

        final Dbms dbms = (Dbms) context.getResourceManager().open ("main-db");
        String userIdQuery = "SELECT id FROM users WHERE username = ?";
        Element userId = dbms.select(userIdQuery, userName);
        Iterator ids = userId.getDescendants(new Filter() {

            public boolean matches(Object arg0) {
                if (arg0 instanceof Text) {
                    Text text = (Text) arg0;
                    return "id".equals(text.getParentElement().getName()) && text.getTextTrim().length()>0;
                }
                return false;
            }
        });

        if(ids.hasNext()){
            Text idText = (Text) ids.next();
            return idText.getTextTrim();
        } else {
                int newId = context.getSerialFactory().getSerial(dbms, "Users");
                final String query = "INSERT INTO users VALUES (?,?,'','','',?,'','','','','',?,'','')";
                dbms.execute(query, newId, userName, profile, email);
            return ""+newId;
        }
    }

    /* PMT security-proxy mod */

	// PMT notes : This is a copy / paste from GeoNetwork's source
	// (see org/fao/constants/Geonet.java:380)

	// I could not avoid it because there is no link between Jeeves
	// and GeoNetwork sources, generating a "unresolved symbol"
	// while attempting to compile Jeeves. By the way, I think it
	// is better in term of design to let the two projects "unlinked".

	private final static class Profile
	{
		public static final String ADMINISTRATOR  = "Administrator";
		public static final String USER_ADMIN     = "UserAdmin";
		public static final String REVIEWER       = "Reviewer";
		public static final String EDITOR         = "Editor";
		public static final String REGISTEREDUSER = "RegisteredUser";
		public static final String GUEST          = "Guest";
	}

	/**
	 * Update the user to match the provided details, or create a new record
	 * for them if they don't have one already.
	 *
	 * @param context The Jeeves ServiceContext
	 * @param dbms The database connection.
	 * @param username The user's username, must not be null.
	 * @param surname The surname of the user
	 * @param firstname The first name of the user.
	 * @param profile The name of the user type.
	 * @throws java.sql.SQLException If the record cannot be saved.
	 */
	private void updateUser(ServiceContext context, Dbms dbms, String username,
			String surname, String firstname, String profile) throws SQLException
	{
		//--- update user information into the database

		String query = "UPDATE Users SET name=?, surname=?, profile=?, password=? WHERE username=?";

		int res = dbms.execute(query, firstname, surname, profile, "Via Shibboleth", username);

		//--- if the user was not found --> add it

		if (res == 0)
		{
			int id = context.getSerialFactory().getSerial(dbms, "Users");

			query = 	"INSERT INTO Users(id, username, name, surname, profile, password) "+
						"VALUES(?,?,?,?,?,?)";

			dbms.execute(query, id, username, firstname, surname, profile, "Via Shibboleth");
		}

		dbms.commit();
	}

}