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
package com.openshift.express.internal.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;

import com.openshift.express.client.HttpMethod;
import com.openshift.express.client.IHttpClient;
import com.openshift.express.client.InvalidCredentialsOpenShiftException;
import com.openshift.express.client.NotFoundOpenShiftException;
import com.openshift.express.client.OpenShiftEndpointException;
import com.openshift.express.client.OpenShiftException;
import com.openshift.express.client.OpenShiftRequestParameterException;
import com.openshift.express.client.configuration.OpenShiftConfiguration;
import com.openshift.express.internal.client.httpclient.HttpClientException;
import com.openshift.express.internal.client.httpclient.NotFoundException;
import com.openshift.express.internal.client.httpclient.UnauthorizedException;
import com.openshift.express.internal.client.response.OpenShiftResponse;
import com.openshift.express.internal.client.response.unmarshalling.dto.Link;
import com.openshift.express.internal.client.response.unmarshalling.dto.LinkParameter;
import com.openshift.express.internal.client.response.unmarshalling.dto.LinkParameterType;
import com.openshift.express.internal.client.response.unmarshalling.dto.ResourceDTOFactory;
import com.openshift.express.internal.client.response.unmarshalling.dto.RestResponse;
import com.openshift.express.internal.client.utils.StringUtils;

/**
 * @author André Dietisheim
 */
public class RestService implements IRestService {

	private static final String HTTP = "http";
	private static final String SERVICE_PATH = "/broker/rest/";
	private static final char SLASH = '/';

	private static final String SYSPROPERTY_PROXY_PORT = "proxyPort";
	private static final String SYSPROPERTY_PROXY_HOST = "proxyHost";
	private static final String SYSPROPERTY_PROXY_SET = "proxySet";

	private String baseUrl;
	private IHttpClient client;

	public RestService(String clientId, IHttpClient client) throws FileNotFoundException, IOException,
			OpenShiftException {
		this(new OpenShiftConfiguration().getLibraServer(), clientId, client);
	}

	public RestService(String baseUrl, String clientId, IHttpClient client) {
		this.baseUrl = baseUrl;
		this.client = client;
		client.setUserAgent(new RestServiceProperties().getUseragent(clientId));
	}

	public RestResponse execute(Link link)
			throws OpenShiftException, MalformedURLException, UnsupportedEncodingException {
		return execute(link, null);
	}

	public RestResponse execute(Link link, Map<String, Object> parameters)
			throws OpenShiftException, MalformedURLException, UnsupportedEncodingException {
		validateParameters(parameters, link);
		try {
			HttpMethod httpMethod = link.getHttpMethod();
			URL url = getUrl(link.getHref());
			String response = request(httpMethod, parameters, url);
			return ResourceDTOFactory.get(response);
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftException(e, "Could not encode parameters: {0}", e.getMessage());
		} catch (MalformedURLException e) {
			throw new OpenShiftException(e, "Could not encode parameters: {0}", e.getMessage());
		} catch (UnauthorizedException e) {
			throw new InvalidCredentialsOpenShiftException(link.getHref(), e);
		} catch (NotFoundException e) {
			throw new NotFoundOpenShiftException(link.getHref(), e);
		} catch (SocketTimeoutException e) {
			throw new OpenShiftEndpointException(link.getHref(), e, e.getMessage());
		} catch (HttpClientException e) {
			throw new OpenShiftEndpointException(link.getHref(), e, createRestResponse(e.getMessage()), e.getMessage());
		}
	}

	private String request(HttpMethod httpMethod, Map<String, Object> parameters, URL url)
			throws HttpClientException, SocketTimeoutException, OpenShiftException, UnsupportedEncodingException {
		switch (httpMethod) {
		case GET:
			return client.get(url);
		case POST:
			return client.post(parameters, url);
		case PUT:
			return client.put(parameters, url);
		case DELETE:
			return client.delete(url);
		default:
			throw new OpenShiftException("Unexpected Http method {0}", httpMethod.toString());
		}
	}

	private URL getUrl(String href) throws MalformedURLException, OpenShiftException {
		if (href == null) {
			throw new OpenShiftException("Invalid empty url");
		}

		if (href.startsWith(HTTP)) {
			return new URL(href);
		}

		if (href.startsWith(SERVICE_PATH)) {
			return new URL(baseUrl + href);
		}

		if (href.charAt(0) == SLASH) {
			href = href.substring(1, href.length());
		}

		return new URL(getServiceUrl() + href);
	}

	private void validateParameters(Map<String, Object> parameters, Link link)
			throws OpenShiftRequestParameterException {
		if (link.getRequiredParams() != null) {
			for (LinkParameter requiredParameter : link.getRequiredParams()) {
				validateRequiredParameter(requiredParameter, parameters, link);
			}
		}
		if (link.getOptionalParams() != null) {
			for (LinkParameter optionalParameter : link.getOptionalParams()) {
				validateOptionalParameters(optionalParameter, link);
			}
		}
	}

	private void validateRequiredParameter(LinkParameter parameter, Map<String, Object> parameters, Link link)
			throws OpenShiftRequestParameterException {
		if (parameters == null
				|| !parameters.containsKey(parameter.getName())) {
			throw new OpenShiftRequestParameterException(
					"Requesting {0}: required request parameter \"{1}\" is missing", link.getHref(),
					parameter.getName());
		}

		Object parameterValue = parameters.get(parameter.getName());
		if (parameterValue == null
				|| isEmptyString(parameter, parameterValue)) {
			throw new OpenShiftRequestParameterException("Requesting {0}: required request parameter \"{1}\" is empty",
					link.getHref(), parameter.getName());
		}
		// TODO: check valid options (still reported in a very incosistent way)
	}

	private void validateOptionalParameters(LinkParameter optionalParameter, Link link) {
		// TODO: implement
	}

	private boolean isEmptyString(LinkParameter parameter, Object parameterValue) {
		return parameter.getType() == LinkParameterType.STRING
				&& StringUtils.isEmpty((String) parameterValue);
	}

	private RestResponse createRestResponse(String response) throws OpenShiftException {
		return ResourceDTOFactory.get(response);
	}

	public void setProxySet(boolean proxySet) {
		if (proxySet) {
			System.setProperty(SYSPROPERTY_PROXY_SET, "true");
		} else {
			System.setProperty(SYSPROPERTY_PROXY_SET, "false");
		}
	}

	public void setProxyHost(String proxyHost) {
		System.setProperty(SYSPROPERTY_PROXY_HOST, proxyHost);
	}

	public void setProxyPort(String proxyPort) {
		System.setProperty(SYSPROPERTY_PROXY_PORT, proxyPort);
	}

	public String getServiceUrl() {
		return baseUrl + SERVICE_PATH;
	}

	public String getPlatformUrl() {
		return baseUrl;
	}
}
