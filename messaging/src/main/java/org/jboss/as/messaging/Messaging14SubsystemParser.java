/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.messaging.Attribute.HOST;
import static org.jboss.as.messaging.Attribute.SOCKET_BINDING;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.SERVLET_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Messaging subsystem 1.4 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging14SubsystemParser extends Messaging13SubsystemParser {

    private static final Messaging14SubsystemParser INSTANCE = new Messaging14SubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    protected Messaging14SubsystemParser() {
    }

    void processConnectors(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            String serverId = null;
            String host = null;

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    }
                    case SOCKET_BINDING: {
                        socketBinding = attrValue;
                        break;
                    }
                    case SERVER_ID: {
                        serverId = attrValue;
                        break;
                    }
                    case HOST: {
                        host = attrValue;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            final ModelNode connectorAddress = address.clone();
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR: {
                    connectorAddress.add(CONNECTOR, name);
                    if (socketBinding != null) {
                        operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    }
                    parseTransportConfiguration(reader, operation, true);
                    break;
                } case NETTY_CONNECTOR: {
                    connectorAddress.add(REMOTE_CONNECTOR, name);
                    if (socketBinding == null) {
                        throw missingRequired(reader, Collections.singleton(Attribute.SOCKET_BINDING));
                    }
                    operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    parseTransportConfiguration(reader, operation, false);
                    break;
                } case SERVLET_CONNECTOR: {
                    if (socketBinding == null) {
                        throw missingRequired(reader, Collections.singleton(SOCKET_BINDING));
                    }
                    if (host == null) {
                        throw missingRequired(reader, Collections.singleton(HOST));
                    }
                    connectorAddress.add(SERVLET_CONNECTOR, name);
                    ServletConnectorDefinition.SOCKET_BINDING.parseAndSetParameter(socketBinding, operation, reader);
                    ServletConnectorDefinition.HOST.parseAndSetParameter(host, operation, reader);
                    parseTransportConfiguration(reader, operation, false);
                    break;
                } case IN_VM_CONNECTOR: {
                    connectorAddress.add(IN_VM_CONNECTOR, name);
                    if (serverId != null) {
                        InVMTransportDefinition.SERVER_ID.parseAndSetParameter(serverId, operation, reader);
                    }
                    parseTransportConfiguration(reader, operation, false);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }

            operation.get(OP_ADDR).set(connectorAddress);
            updates.add(operation);
        }
    }

    @Override
    protected void handleUnknownBridgeAttribute(XMLExtendedStreamReader reader, Element element, ModelNode bridgeAdd) throws XMLStreamException {
        switch (element) {
            case RECONNECT_ATTEMPTS_ON_SAME_NODE:
                handleElementText(reader, element, bridgeAdd);
                break;
            default:
                super.handleUnknownBridgeAttribute(reader, element, bridgeAdd);
        }
    }
}
