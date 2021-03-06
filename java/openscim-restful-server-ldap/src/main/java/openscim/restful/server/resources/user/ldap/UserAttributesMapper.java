/*
 * openscim restful scim server
 * http://code.google.com/p/openscim/
 * Copyright (C) 2011 Matthew Crooke <matthewcrooke@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package openscim.restful.server.resources.user.ldap;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import openscim.entities.Name;
import openscim.entities.PluralAttribute;
import openscim.entities.User;
import openscim.entities.User.Emails;
import openscim.entities.User.MemberOf;
import openscim.entities.User.PhoneNumbers;
import openscim.restful.server.resources.group.ldap.GroupAttributesMapper;
import openscim.restful.server.resources.util.ResourceUtilities;

import org.springframework.ldap.core.AttributesMapper;

public class UserAttributesMapper implements AttributesMapper
{
	public static final String LDAP_PREFIX = "ldap.";
	public static final String ATTRIBUTE_PREFIX = LDAP_PREFIX + "attributes.account.";
	public static final String ACCOUNT_OBJECTCLASS_ATTRIBUTE = ATTRIBUTE_PREFIX + "objectclass";
	public static final String UID_ATTRIBUTE = ATTRIBUTE_PREFIX + "uid";
	public static final String DISPLAYNAME_ATTRIBUTE = ATTRIBUTE_PREFIX + "displayName";
	public static final String FAMILYNAME_ATTRIBUTE = ATTRIBUTE_PREFIX + "familyName";
	public static final String GIVENNAME_ATTRIBUTE = ATTRIBUTE_PREFIX + "givenName";
	public static final String MAIL_ATTRIBUTE = ATTRIBUTE_PREFIX + "mail";
	public static final String TELEPHONE_ATTRIBUTE = ATTRIBUTE_PREFIX + "telephone";
	public static final String PASSWORD_ATTRIBUTE = ATTRIBUTE_PREFIX + "password";
	public static final String MEMBEROF_ATTRIBUTE = ATTRIBUTE_PREFIX + "memberOf";
	public static final String DEFAULT_ACCOUNT_OBJECTCLASS_ATTRIBUTE = "inetOrgPerson, organizationalPerson, person, top";
	public static final String DEFAULT_UID_ATTRIBUTE = "cn";
	public static final String DEFAULT_DISPLAYNAME_ATTRIBUTE = "cn";
	public static final String DEFAULT_FAMILYNAME_ATTRIBUTE = "sn";
	public static final String DEFAULT_GIVENNAME_ATTRIBUTE = "givenName";
	public static final String DEFAULT_MAIL_ATTRIBUTE = "mail";
	public static final String DEFAULT_TELEPHONE_ATTRIBUTE = "telephone";
	public static final String DEFAULT_PASSWORD_ATTRIBUTE = "userPassword";
	public static final String DEFAULT_MEMBEROF_ATTRIBUTE = "memberOf";
	public static final String ACCOUNT_PREFIX = "ldap.account.";
	public static final String ACCOUNT_BASEDN = ACCOUNT_PREFIX + "basedn";
	public static final String DEFAULT_ACCOUNT_BASEDN = "ou=users,dc=openscim";
	public static final String CONCEAL_ACCOUNT_DNS = LDAP_PREFIX + "concealdns";
	public static final String DEFAULT_CONCEAL_ACCOUNT_DNS = "true";
	
	private Properties properties = null;
	private String expression = null;
	private Pattern pattern = null;
	
	public UserAttributesMapper(Properties properties)
	{
		this.properties = properties;
		expression = properties.getProperty(GroupAttributesMapper.GID_ATTRIBUTE, GroupAttributesMapper.DEFAULT_GID_ATTRIBUTE) + 
		   "=([^,]*)," + properties.getProperty(GroupAttributesMapper.GROUP_BASEDN, GroupAttributesMapper.DEFAULT_GROUP_BASEDN);
		pattern = Pattern.compile(expression);
	}
	
	public Object mapFromAttributes(Attributes attributes) throws NamingException
	{
		// create a user resource
		User user = ResourceUtilities.FACTORY.createUser();
		
		// get the uid attribute name
		String uidAtttributeName = properties.getProperty(UID_ATTRIBUTE, DEFAULT_UID_ATTRIBUTE);
		
		// get the uid
		//Attribute uidAttribute = attributes.get(uidAtttributeName);
		//if(uidAttribute != null) user.setId((String)uidAttribute.get());
		
		// get the display name attribute name
		String displayAtttributeName = properties.getProperty(DISPLAYNAME_ATTRIBUTE, DEFAULT_DISPLAYNAME_ATTRIBUTE);
		
		// get the display name
		Attribute displayNameAttribute = attributes.get(displayAtttributeName);
		if(displayNameAttribute != null) user.setDisplayName((String)displayNameAttribute.get());
		
		// create a user name resource
		Name name = ResourceUtilities.FACTORY.createName();

		// get the surname attribute name
		String surnameAtttributeName = properties.getProperty(FAMILYNAME_ATTRIBUTE, DEFAULT_FAMILYNAME_ATTRIBUTE);
		
		// get the surname name
		Attribute surnameAttribute = attributes.get(surnameAtttributeName); 
		if(surnameAttribute != null) name.setFamilyName((String)surnameAttribute.get());
		
		// get the given name attribute name
		String givenAtttributeName = properties.getProperty(GIVENNAME_ATTRIBUTE, DEFAULT_GIVENNAME_ATTRIBUTE);
		
		// get the given name
		Attribute givenAttribute = attributes.get(givenAtttributeName);
		if(givenAttribute != null) name.setGivenName((String)givenAttribute.get());
		
		// add the name to the user resource
		user.setName(name);
		
		// get the email attribute name
		String mailAtttributeName = properties.getProperty(MAIL_ATTRIBUTE, DEFAULT_MAIL_ATTRIBUTE);
		
		// get the mails
		if(attributes.get(mailAtttributeName) != null)
		{
			NamingEnumeration mailEnumeration = attributes.get(mailAtttributeName).getAll();
			if(mailEnumeration != null)
			{
				// create a emails resource
				Emails emails = ResourceUtilities.FACTORY.createUserEmails();
	
				while(mailEnumeration.hasMoreElements())
				{
					// get the next email
					String mailAttribute = (String)mailEnumeration.next();
					if(mailAttribute != null)
					{				
						PluralAttribute pluralAttribute = ResourceUtilities.FACTORY.createPluralAttribute();
						pluralAttribute.setValue(mailAttribute);
						
						if(emails.getEmail().isEmpty()) pluralAttribute.setPrimary(true);
						else pluralAttribute.setPrimary(false);
						
						emails.getEmail().add(pluralAttribute);
					}
				}
				
				// add the mails to the user resource
				user.setEmails(emails);
			}
		}
		
		// get the telephone attribute name
		String telephoneAtttributeName = properties.getProperty(TELEPHONE_ATTRIBUTE, DEFAULT_TELEPHONE_ATTRIBUTE);
		
		// get the telephones
		if(attributes.get(telephoneAtttributeName) != null)
		{
			NamingEnumeration telephoneEnumeration = attributes.get(telephoneAtttributeName).getAll();
			if(telephoneEnumeration != null)
			{
				// create a telephones resource
				PhoneNumbers telephones = ResourceUtilities.FACTORY.createUserPhoneNumbers();
	
				while(telephoneEnumeration.hasMoreElements())
				{
					// get the next telephone
					String telephoneAttribute = (String)telephoneEnumeration.next();
					if(telephoneAttribute != null)
					{				
						PluralAttribute pluralAttribute = ResourceUtilities.FACTORY.createPluralAttribute();
						pluralAttribute.setValue(telephoneAttribute);
						
						if(telephones.getPhoneNumber().isEmpty()) pluralAttribute.setPrimary(true);
						else pluralAttribute.setPrimary(false);
						
						telephones.getPhoneNumber().add(pluralAttribute);
					}
				}
				
				// add the telephones to the user resource
				user.setPhoneNumbers(telephones);
			}
		}
		
		// get the password attribute name
		String passwordAtttributeName = properties.getProperty(PASSWORD_ATTRIBUTE, DEFAULT_PASSWORD_ATTRIBUTE);
		
		// get the password
		Attribute passwordAttribute = attributes.get(passwordAtttributeName);
		if(passwordAttribute != null) user.setPassword(new String((byte[])passwordAttribute.get()));

		// get the memberOf attribute name
		String memberOfAtttributeName = properties.getProperty(MEMBEROF_ATTRIBUTE, DEFAULT_MEMBEROF_ATTRIBUTE);
		
		// get the memberOf
		if(attributes.get(memberOfAtttributeName) != null)
		{
			NamingEnumeration memberOfEnumeration = attributes.get(memberOfAtttributeName).getAll();
			if(memberOfEnumeration != null)
			{
				// create a memberof resource
				MemberOf memberof = ResourceUtilities.FACTORY.createUserMemberOf();
				
				while(memberOfEnumeration.hasMoreElements())
				{
					// get the next member
					String memberOfAttribute = (String)memberOfEnumeration.next();
					if(memberOfAttribute != null)
					{				
						PluralAttribute pluralAttribute = ResourceUtilities.FACTORY.createPluralAttribute();
						
						// check if the member dns need to be concealed 
						if(properties.getProperty(UserAttributesMapper.CONCEAL_ACCOUNT_DNS, UserAttributesMapper.DEFAULT_CONCEAL_ACCOUNT_DNS).equalsIgnoreCase(UserAttributesMapper.DEFAULT_CONCEAL_ACCOUNT_DNS))
						{
							Matcher matcher = pattern.matcher(memberOfAttribute);			
							if(matcher.matches())
							{
								memberOfAttribute = matcher.group(1); 
							}
						}
						
						pluralAttribute.setValue(memberOfAttribute);				
						memberof.getGroup().add(pluralAttribute);
					}
				}
				
				// add the memberOf to the user resource
				user.setMemberOf(memberof);
			}
		}
		
		return user;
     }
}
