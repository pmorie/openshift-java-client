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

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import com.openshift.client.IApplication;
import com.openshift.client.IApplicationGear;
import com.openshift.client.IApplicationGearComponent;
import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.IHttpClient;
import com.openshift.client.OpenShiftException;
import com.openshift.client.utils.RFC822DateUtils;
import com.openshift.internal.client.httpclient.HttpClientException;
import com.openshift.internal.client.response.ApplicationResourceDTO;
import com.openshift.internal.client.response.CartridgeResourceDTO;
import com.openshift.internal.client.response.GearComponentDTO;
import com.openshift.internal.client.response.GearResourceDTO;
import com.openshift.internal.client.response.Link;
import com.openshift.internal.client.utils.IOpenShiftJsonConstants;

/**
 * The Class Application.
 * 
 * @author André Dietisheim
 */
public class ApplicationResource extends AbstractOpenShiftResource implements IApplication {

	private static final long APPLICATION_WAIT_RETRY_DELAY = 2 * 1024;

	private static final String LINK_DELETE_APPLICATION = "DELETE";
	private static final String LINK_START_APPLICATION = "START";
	private static final String LINK_STOP_APPLICATION = "STOP";
	private static final String LINK_FORCE_STOP_APPLICATION = "FORCE_STOP";
	private static final String LINK_RESTART_APPLICATION = "RESTART";
	private static final String LINK_SCALE_UP = "SCALE_UP";
	private static final String LINK_SCALE_DOWN = "SCALE_DOWN";
	private static final String LINK_SHOW_PORT = "SHOW_PORT";
	private static final String LINK_EXPOSE_PORT = "EXPOSE_PORT";
	private static final String LINK_CONCEAL_PORT = "CONCEAL_PORT";
	private static final String LINK_ADD_ALIAS = "ADD_ALIAS";
	private static final String LINK_REMOVE_ALIAS = "REMOVE_ALIAS";
	private static final String LINK_ADD_CARTRIDGE = "ADD_CARTRIDGE";
	private static final String LINK_LIST_CARTRIDGES = "LIST_CARTRIDGES";
	private static final String LINK_LIST_GEARS = "GET_GEARS";

	/** The (unique) uuid of this application. */
	private final String uuid;

	/** The name of this application. */
	private final String name;

	/** The time at which this application was created. */
	private final Date creationTime;

	/** The cartridge (application type/framework) of this application. */
	private final ICartridge cartridge;

	/** The creation log. */
	private final String creationLog;

	/** The domain this application belongs to. */
	private final DomainResource domain;

	/** The url of this application. */
	private final String applicationUrl;

	/** The pathat which the health of this application may be queried. */
	private final String healthCheckUrl;

	/** The url at which the git repo of this application may be reached. */
	private final String gitUrl;

	/** The aliases of this application. */
	private final List<String> aliases;

	/**
	 * List of configured embedded cartridges. <code>null</code> means list if
	 * not loaded yet.
	 */
	// TODO: replace by a map indexed by cartridge names ?
	private List<IEmbeddedCartridge> embeddedCartridges = null;

	/**
	 * List of configured gears. <code>null</code> means list if not loaded yet.
	 */
	// TODO: replace by a map indexed by cartridge names ?
	private List<IApplicationGear> gears = null;

	protected ApplicationResource(ApplicationResourceDTO dto, ICartridge cartridge, DomainResource domain) {
		this(dto.getName(), dto.getUuid(), dto.getCreationTime(), dto.getApplicationUrl(), dto.getGitUrl(),
				dto.getHealthCheckPath(), cartridge, dto.getAliases(), dto.getLinks(), domain);
	}

	/**
	 * Instantiates a new application.
	 * 
	 * @param name
	 *            the name
	 * @param uuid
	 *            the uuid
	 * @param creationTime
	 *            the creation time
	 * @param applicationUrl
	 *            the application url
	 * @param gitUrl
	 *            the git url
	 * @param cartridge
	 *            the cartridge (type/framework)
	 * @param aliases
	 *            the aliases
	 * @param links
	 *            the links
	 * @param domain
	 *            the domain this application belongs to
	 * @throws DatatypeConfigurationException
	 */
	protected ApplicationResource(final String name, final String uuid, final String creationTime,
			final String applicationUrl, final String gitUrl, final String healthCheckPath, final ICartridge cartridge,
			final List<String> aliases,
			final Map<String, Link> links, final DomainResource domain) {
		this(name, uuid, creationTime, null, applicationUrl, gitUrl, healthCheckPath, cartridge, aliases, links, domain);
	}

	/**
	 * Instantiates a new application.
	 * 
	 * @param name
	 *            the name
	 * @param uuid
	 *            the uuid
	 * @param creationTime
	 *            the creation time
	 * @param creationLog
	 *            the creation log
	 * @param applicationUrl
	 *            the application url
	 * @param gitUrl
	 *            the git url
	 * @param cartridge
	 *            the cartridge (type/framework)
	 * @param aliases
	 *            the aliases
	 * @param links
	 *            the links
	 * @param domain
	 *            the domain this application belongs to
	 * @throws DatatypeConfigurationException
	 */
	protected ApplicationResource(final String name, final String uuid, final String creationTime,
			final String creationLog, final String applicationUrl, final String gitUrl, final String healthCheckPath,
			final ICartridge cartridge, final List<String> aliases, final Map<String, Link> links, final DomainResource domain) {
		super(domain.getService(), links);
		this.name = name;
		this.uuid = uuid;
		this.creationTime = RFC822DateUtils.safeGetDate(creationTime);
		this.creationLog = creationLog;
		this.cartridge = cartridge;
		this.applicationUrl = applicationUrl;
		this.gitUrl = gitUrl;
		this.healthCheckUrl = applicationUrl + healthCheckPath;
		this.domain = domain;
		this.aliases = aliases;
	}

	public String getName() {
		return name;
	}

	public String getUUID() {
		return uuid;
	}

	public ICartridge getCartridge() {
		return cartridge;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public String getCreationLog() {
		return creationLog;
	}

	public IDomain getDomain() {
		return this.domain;
	}

	public void destroy() throws OpenShiftException, SocketTimeoutException {
		new DeleteApplicationRequest().execute();
		domain.removeApplication(this);
	}

	public void start() throws OpenShiftException, SocketTimeoutException {
		new StartApplicationRequest().execute();
	}

	public void restart() throws OpenShiftException, SocketTimeoutException {
		new RestartApplicationRequest().execute();
	}

	public void stop() throws OpenShiftException, SocketTimeoutException {
		stop(false);
	}

	public void stop(boolean force) throws OpenShiftException, SocketTimeoutException {
		if (force) {
			new ForceStopApplicationRequest().execute();
		} else {
			new StopApplicationRequest().execute();

		}
	}

	public void exposePort() throws SocketTimeoutException, OpenShiftException {
		new ExposePortRequest().execute();
	}

	public void concealPort() throws SocketTimeoutException, OpenShiftException {
		new ConcealPortRequest().execute();
	}

	public void showPort() throws SocketTimeoutException, OpenShiftException {
		new ShowPortRequest().execute();
	}

	public void getDescriptor() {
		throw new UnsupportedOperationException();
	}

	public void scaleDown() throws SocketTimeoutException, OpenShiftException {
		new ScaleDownRequest().execute();
	}

	public void scaleUp() throws SocketTimeoutException, OpenShiftException {
		new ScaleUpRequest().execute();
	}

	public void addAlias(String alias) throws SocketTimeoutException, OpenShiftException {
		ApplicationResourceDTO applicationDTO = new AddAliasRequest().execute(alias);
		updateAliases(applicationDTO);

	}

	private void updateAliases(ApplicationResourceDTO applicationDTO) {
		this.aliases.clear();
		this.aliases.addAll(applicationDTO.getAliases());
	}

	public List<String> getAliases() {
		return Collections.unmodifiableList(this.aliases);
	}

	public boolean hasAlias(String name) {
		return aliases.contains(name);
	}

	public void removeAlias(String alias) throws SocketTimeoutException, OpenShiftException {
		ApplicationResourceDTO applicationDTO = new RemoveAliasRequest().execute(alias);
		updateAliases(applicationDTO);
	}

	public String getGitUrl() {
		return this.gitUrl;
	}

	public String getApplicationUrl() {
		return applicationUrl;
	}

	public String getHealthCheckUrl() {
		return healthCheckUrl;
	}

	protected String getHealthCheckSuccessResponse() throws OpenShiftException {
		return "1";
	}

	/**
	 * Adds the given embedded cartridge to this application.
	 * 
	 * @param cartridge
	 *            the embeddable cartridge that shall be added to this
	 *            application
	 */
	public IEmbeddedCartridge addEmbeddableCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException {
		final CartridgeResourceDTO embeddedCartridgeDTO = new AddEmbeddedCartridgeRequest()
				.execute(cartridge.getName());
		final EmbeddedCartridgeResource embeddedCartridge = new EmbeddedCartridgeResource(
				embeddedCartridgeDTO.getName(),
				embeddedCartridgeDTO.getType(),
				embeddedCartridgeDTO.getLinks(), this);
		this.embeddedCartridges.add(embeddedCartridge);
		return embeddedCartridge;
	}

	public List<IEmbeddedCartridge> addEmbeddableCartridges(List<IEmbeddableCartridge> cartridges) throws OpenShiftException,
			SocketTimeoutException {
		final List<IEmbeddedCartridge> addedCartridge = new ArrayList<IEmbeddedCartridge>();
		for (IEmbeddableCartridge cartridge : cartridges) {
			// TODO: catch exceptions when removing cartridges, contine removing
			// and report the exceptions that occurred<
			addedCartridge.add(addEmbeddableCartridge(cartridge));
		}
		return addedCartridge;
	}

	/**
	 * "callback" from the embeddedCartridge once it has been destroyed.
	 * @param embeddedCartridge
	 * @throws OpenShiftException
	 */
	protected void removeEmbeddedCartridge(IEmbeddedCartridge embeddedCartridge) throws OpenShiftException {
		this.embeddedCartridges.remove(embeddedCartridge);
	}

	public List<IEmbeddedCartridge> getEmbeddedCartridges() throws OpenShiftException, SocketTimeoutException {
		// load collection if necessary
		if (embeddedCartridges == null) {
			this.embeddedCartridges = new ArrayList<IEmbeddedCartridge>();
			List<CartridgeResourceDTO> embeddableCartridgeDTOs = new ListEmbeddableCartridgesRequest().execute();
			for (CartridgeResourceDTO embeddableCartridgeDTO : embeddableCartridgeDTOs) {
				IEmbeddedCartridge embeddableCartridge =
						new EmbeddedCartridgeResource(
								embeddableCartridgeDTO.getName(),
								embeddableCartridgeDTO.getType(),
								embeddableCartridgeDTO.getLinks(),
								this);
				this.embeddedCartridges.add(embeddableCartridge);
			}
		}
		return embeddedCartridges;
	}

	public boolean hasEmbeddedCartridge(String cartridgeName) throws OpenShiftException, SocketTimeoutException {
		return getEmbeddedCartridge(cartridgeName) != null;
	}

	public boolean hasEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException {
		return getEmbeddedCartridge(cartridge) != null;
	}

	public IEmbeddedCartridge getEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException,
			SocketTimeoutException {
		return getEmbeddedCartridge(cartridge.getName());
	}

	public IEmbeddedCartridge getEmbeddedCartridge(String cartridgeName) throws OpenShiftException,
			SocketTimeoutException {
		for (IEmbeddedCartridge embeddedCartridge : getEmbeddedCartridges()) {
			if (cartridgeName.equals(embeddedCartridge.getName())) {
				return embeddedCartridge;
			}
		}
		return null;
	}
	
	/**
	 * Returns the gears that this application is running on.
	 * 
	 * @return
	 * 
	 * @return the gears
	 * @throws OpenShiftException
	 * @throws SocketTimeoutException
	 */
	public List<IApplicationGear> getGears() throws SocketTimeoutException, OpenShiftException {
		// load collection if necessary
		if (gears == null) {
			this.gears = new ArrayList<IApplicationGear>();
			List<GearResourceDTO> gearDTOs = new ListGearsRequest().execute();
			for (GearResourceDTO gearDTO : gearDTOs) {
				final List<IApplicationGearComponent> components = new ArrayList<IApplicationGearComponent>();
				for (GearComponentDTO gearComponentDTO : gearDTO.getComponents()) {
					components.add(
							new ApplicationGearComponentResource(gearComponentDTO));
				}
				IApplicationGear gear =
						new ApplicationGearResource(
								gearDTO, components, this);
				this.gears.add(gear);
			}
		}
		return gears;
	}

	public boolean waitForAccessible(long timeout) throws OpenShiftException {
		try {
			IHttpClient client = ((RestService) getService()).getClient();
			String response = "";
			long startTime = System.currentTimeMillis();
			URL healthCheckUrl = new URL(getHealthCheckUrl());
			while (!response.startsWith(getHealthCheckSuccessResponse())
					&& System.currentTimeMillis() < startTime + timeout) {
				try {
					Thread.sleep(APPLICATION_WAIT_RETRY_DELAY);
					response = client.get(healthCheckUrl);
				} catch (HttpClientException e) {
				}
			}
			
			return response.startsWith(getHealthCheckSuccessResponse());
		} catch (InterruptedException e) {
			return false;
		} catch (MalformedURLException e) {
			throw new OpenShiftException(e, "Application URL {0} is invalid", healthCheckUrl);
		} catch (SocketTimeoutException e) {
			throw new OpenShiftException(e, "Could not reach {0}, connection timeouted", healthCheckUrl);
		}

	}

	public void refresh() throws SocketTimeoutException, OpenShiftException {
		this.embeddedCartridges = null;
		this.gears = null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null)
			return false;
		if (getClass() != object.getClass())
			return false;
		ApplicationResource other = (ApplicationResource) object;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

	private class DeleteApplicationRequest extends ServiceRequest {

		protected DeleteApplicationRequest() {
			super(LINK_DELETE_APPLICATION);
		}
	}

	private class StartApplicationRequest extends ServiceRequest {

		protected StartApplicationRequest() {
			super(LINK_START_APPLICATION);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_START));
		}
	}

	private class StopApplicationRequest extends ServiceRequest {

		protected StopApplicationRequest() {
			super(LINK_STOP_APPLICATION);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_STOP));
		}
	}

	private class ForceStopApplicationRequest extends ServiceRequest {

		protected ForceStopApplicationRequest() {
			super(LINK_FORCE_STOP_APPLICATION);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_FORCESTOP));
		}
	}

	private class RestartApplicationRequest extends ServiceRequest {

		protected RestartApplicationRequest() {
			super(LINK_RESTART_APPLICATION);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_RESTART));
		}
	}

	private class ExposePortRequest extends ServiceRequest {

		protected ExposePortRequest() {
			super(LINK_EXPOSE_PORT);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_EXPOSE_PORT));
		}
	}

	private class ConcealPortRequest extends ServiceRequest {

		protected ConcealPortRequest() {
			super(LINK_CONCEAL_PORT);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_CONCEAL_PORT));
		}
	}

	private class ShowPortRequest extends ServiceRequest {

		protected ShowPortRequest() {
			super(LINK_SHOW_PORT);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_SHOW_PORT));
		}
	}

	private class ScaleUpRequest extends ServiceRequest {

		protected ScaleUpRequest() {
			super(LINK_SCALE_UP);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_SCALE_UP));
		}
	}

	private class ScaleDownRequest extends ServiceRequest {

		protected ScaleDownRequest() {
			super(LINK_SCALE_DOWN);
		}

		public <DTO> DTO execute() throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_SCALE_DOWN));
		}
	}

	private class AddAliasRequest extends ServiceRequest {

		protected AddAliasRequest() {
			super(LINK_ADD_ALIAS);
		}

		public <DTO> DTO execute(String alias) throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_ADD_ALIAS),
					new ServiceParameter(IOpenShiftJsonConstants.PROPERTY_ALIAS, alias));
		}
	}

	private class RemoveAliasRequest extends ServiceRequest {

		protected RemoveAliasRequest() {
			super(LINK_REMOVE_ALIAS);
		}

		public <DTO> DTO execute(String alias) throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_EVENT,
							IOpenShiftJsonConstants.VALUE_REMOVE_ALIAS),
					new ServiceParameter(IOpenShiftJsonConstants.PROPERTY_ALIAS, alias));
		}
	}

	private class AddEmbeddedCartridgeRequest extends ServiceRequest {

		protected AddEmbeddedCartridgeRequest() {
			super(LINK_ADD_CARTRIDGE);
		}

		public <DTO> DTO execute(String embeddedCartridgeName) throws OpenShiftException, SocketTimeoutException {
			return super.execute(
					new ServiceParameter(
							IOpenShiftJsonConstants.PROPERTY_CARTRIDGE, embeddedCartridgeName));
		}
	}

	private class ListEmbeddableCartridgesRequest extends ServiceRequest {

		protected ListEmbeddableCartridgesRequest() {
			super(LINK_LIST_CARTRIDGES);
		}
	}

	private class ListGearsRequest extends ServiceRequest {

		protected ListGearsRequest() {
			super(LINK_LIST_GEARS);
		}
	}

}
