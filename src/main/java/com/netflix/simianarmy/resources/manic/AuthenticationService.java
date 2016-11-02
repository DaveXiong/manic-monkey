/**
 * 
 */
package com.netflix.simianarmy.resources.manic;

import java.io.IOException;
import java.util.Base64;
import java.util.StringTokenizer;

/**
 * @author dxiong
 *
 */
public class AuthenticationService {
	//TODO:need to replace this with ldap auth
	private static final String TOKEN = "Basic bW9ua2V5Om1hbjFjQE0wbmtleQ==";

	public boolean authenticate(String authCredentials) {

		if (null == authCredentials)
			return false;
		// header value format will be "Basic encodedstring" for Basic
		// authentication. Example "Basic YWRtaW46YWRtaW4="
//		final String encodedUserPassword = authCredentials.replaceFirst("Basic" + " ", "");
//		String usernameAndPassword = null;
//		try {
//			byte[] decodedBytes = Base64.getDecoder().decode(encodedUserPassword);
//			usernameAndPassword = new String(decodedBytes, "UTF-8");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
//		final String username = tokenizer.nextToken();
//		final String password = tokenizer.nextToken();
//
//		// we have fixed the userid and password as admin
//		// call some UserService/LDAP here
//		boolean authenticationStatus = "admin".equals(username) && "admin".equals(password);
//		return authenticationStatus;
		
		return TOKEN.equalsIgnoreCase(authCredentials);
	}
}
