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
package com.openshift.internal.client;

import java.util.List;
import java.util.Map;

import com.openshift.client.ICartridge;
import com.openshift.client.IJenkinsApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.internal.client.response.Link;

/**
 * @author William DeCoste
 * @author Andre Dietisheim
 */
public class JenkinsApplication extends ApplicationResource implements IJenkinsApplication {

	

	public JenkinsApplication(String name, String uuid, String creationTime, String applicationUrl, String gitUrl,
			String healthCheckPath, String gearProfile, boolean scalable, ICartridge cartridge, List<String> aliases,
			Map<String, Link> links, DomainResource domain) {
		super(name, uuid, creationTime, applicationUrl, gitUrl, healthCheckPath, gearProfile, scalable, cartridge, aliases,
				links, domain);
	}

	public JenkinsApplication(String name, String uuid, String creationTime, String creationLog, String applicationUrl,
			String gitUrl, String healthCheckPath, String gearProfile, boolean scalable, ICartridge cartridge,
			List<String> aliases, Map<String, Link> links, DomainResource domain) {
		super(name, uuid, creationTime, creationLog, applicationUrl, gitUrl, healthCheckPath, gearProfile, scalable, cartridge,
				aliases, links, domain);
	}

	public String getHealthCheckUrl() {
		return getApplicationUrl() + "login?from=%2F";
	}

	public String getHealthCheckSuccessResponse() throws OpenShiftException {
		return "<html>";
	}
}
