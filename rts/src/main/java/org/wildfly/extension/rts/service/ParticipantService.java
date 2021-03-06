/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.rts.service;

import io.undertow.servlet.api.DeploymentInfo;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.narayana.rest.integration.ParticipantResource;
import org.jboss.narayana.rest.integration.api.ParticipantsManagerFactory;
import org.wildfly.extension.rts.logging.RTSLogger;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class ParticipantService extends AbstractRTSService implements Service<ParticipantService> {

    public static final String CONTEXT_PATH = ParticipantResource.BASE_PATH_SEGMENT;

    private static final String DEPLOYMENT_NAME = "REST-AT Participant";

    private InjectedValue<SocketBinding> injectedSocketBinding = new InjectedValue<>();

    @Override
    public ParticipantService getValue() throws IllegalStateException, IllegalArgumentException {
        if (RTSLogger.ROOT_LOGGER.isTraceEnabled()) {
            RTSLogger.ROOT_LOGGER.trace("ParticipantService.getValue");
        }

        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (RTSLogger.ROOT_LOGGER.isTraceEnabled()) {
            RTSLogger.ROOT_LOGGER.trace("ParticipantService.start");
        }

        deployParticipant();
        ParticipantsManagerFactory.getInstance().setBaseUrl(getBaseUrl());
    }

    @Override
    public void stop(StopContext context) {
        if (RTSLogger.ROOT_LOGGER.isTraceEnabled()) {
            RTSLogger.ROOT_LOGGER.trace("ParticipantService.stop");
        }

        undeployServlet();
    }

    public InjectedValue<SocketBinding> getInjectedSocketBinding() {
        return injectedSocketBinding;
    }

    private void deployParticipant() {
        undeployServlet();

        final Map<String, String> initialParameters = new HashMap<String, String>();
        initialParameters.put("javax.ws.rs.Application", "org.wildfly.extension.rts.jaxrs.ParticipantApplication");

        final DeploymentInfo participantDeploymentInfo = getDeploymentInfo(DEPLOYMENT_NAME, CONTEXT_PATH, initialParameters);

        deployServlet(participantDeploymentInfo);
    }

    private String getBaseUrl() {
        final String address = injectedSocketBinding.getValue().getAddress().getHostAddress();
        final int port = injectedSocketBinding.getValue().getPort();

        if (injectedSocketBinding.getValue().getAddress() instanceof Inet4Address) {
            return "http://" + address + ":" + port;
        } else {
            return "http://[" + address + "]:" + port;
        }
    }

}
