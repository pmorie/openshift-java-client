/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package com.openshift.express.client;

import java.net.SocketTimeoutException;

import com.openshift.express.internal.client.httpclient.HttpClientException;

/**
 * @author André Dietisheim
 */
public interface IHttpClient {

	public static final int STATUS_OK = 200;
	public static final int STATUS_INTERNAL_SERVER_ERROR = 200;
	public static final int STATUS_BAD_REQUEST = 400;
	public static final int STATUS_UNAUTHORIZED = 401;
	public static final int STATUS_NOT_FOUND = 404;
	
	public static final String USER_AGENT = "User-Agent"; //$NON-NLS-1$

	public String post(String data) throws HttpClientException, SocketTimeoutException;

	public String get() throws HttpClientException, SocketTimeoutException;
}
