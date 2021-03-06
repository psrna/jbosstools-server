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
package org.jboss.ide.eclipse.as.rse.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.extensions.polling.WebPortPoller;
import org.jboss.ide.eclipse.as.core.server.IJBossServer;
import org.jboss.ide.eclipse.as.core.server.internal.ExtendedServerPropertiesAdapterFactory;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;
import org.jboss.ide.eclipse.as.core.server.internal.v7.JBoss7ManagerServicePoller;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI;

public class RSEDeploymentPreferenceUI implements IDeploymentTypeUI {

	public RSEDeploymentPreferenceUI() {
		// Do nothing
	}

	@Override 
	public void fillComposite(Composite parent, IServerModeUICallback callback) {
		parent.setLayout(new FillLayout());
		RSEDeploymentPreferenceComposite composite = null;
		
		IServerWorkingCopy cServer = callback.getServer();
		IJBossServer jbs = cServer.getOriginal() == null ? 
				ServerConverter.getJBossServer(cServer) :
					ServerConverter.getJBossServer(cServer.getOriginal());
		ServerExtendedProperties sep = ExtendedServerPropertiesAdapterFactory.getServerExtendedProperties(cServer);
		if( jbs == null || sep == null)
			composite = new DeployOnlyRSEPrefComposite(parent, SWT.NONE, callback);
		else if( sep.getFileStructure() == ServerExtendedProperties.FILE_STRUCTURE_SERVER_CONFIG_DEPLOY){
			composite = new JBossRSEDeploymentPrefComposite(parent, SWT.NONE, callback);
		} else if( sep.getFileStructure() == ServerExtendedProperties.FILE_STRUCTURE_CONFIG_DEPLOYMENTS){
			composite = new JBoss7RSEDeploymentPrefComposite(parent, SWT.NONE, callback);
		}
		// NEW_SERVER_ADAPTER potential location for new server details
	}
	
	@Override
	@Deprecated
	public void performFinish(IServerModeUICallback callback, IProgressMonitor monitor) throws CoreException {
		// Override the pollers to more sane defaults for RSE
		// For now, hard code these options. One day, we might need an additional
		// adapter factory for rse-specific initialization questions on a per-server basis
		IServerWorkingCopy wc = callback.getServer();
		// an as7-only key
		boolean exposed = wc.getAttribute(IJBossToolingConstants.EXPOSE_MANAGEMENT_SERVICE, false);
		if( !exposed ) {
			// as<7 || ( as==7 && !exposed) uses poller
			wc.setAttribute(IJBossToolingConstants.STARTUP_POLLER_KEY, WebPortPoller.WEB_POLLER_ID);
			wc.setAttribute(IJBossToolingConstants.SHUTDOWN_POLLER_KEY, WebPortPoller.WEB_POLLER_ID);
		} else {
			// as7 && exposed
			// TODO THIS NEEDS TO LIVE ELSEWHERE
			String pollId = wc.getServerType().getId().equals(IJBossToolingConstants.SERVER_WILDFLY_80) ? JBoss7ManagerServicePoller.WILDFLY_POLLER_ID : JBoss7ManagerServicePoller.POLLER_ID;
			wc.setAttribute(IJBossToolingConstants.STARTUP_POLLER_KEY, pollId);
			wc.setAttribute(IJBossToolingConstants.SHUTDOWN_POLLER_KEY, pollId);
		}
	}
}
