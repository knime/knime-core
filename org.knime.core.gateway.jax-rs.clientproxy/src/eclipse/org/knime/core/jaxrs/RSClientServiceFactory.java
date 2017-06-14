/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 23, 2017 (hornm): created
 */
package org.knime.core.jaxrs;

import java.util.Arrays;
import java.util.List;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.knime.core.gateway.ObjectSpecUtil;
import org.knime.core.gateway.services.ServerServiceConfig;
import org.knime.core.gateway.services.ServiceConfig;
import org.knime.core.gateway.services.ServiceFactory;
import org.knime.core.gateway.v0.workflow.service.GatewayService;
import org.knime.core.jaxrs.providers.OptionalParamConverter;
import org.knime.core.jaxrs.providers.exception.NoSuchElementExceptionMapper;
import org.knime.core.jaxrs.providers.json.EntityCollectionJSONDeserializer;
import org.knime.core.jaxrs.providers.json.EntityCollectionJSONSerializer;
import org.knime.core.jaxrs.providers.json.EntityJSONDeserializer;
import org.knime.core.jaxrs.providers.json.EntityJSONSerializer;
import org.knime.core.jaxrs.providers.json.MapJSONDeserializer;

/**
 * Provides service implementations that communicate with the respective RESTful service interface (see also the
 * JettyRestServer in the ...gateway.jax-rs.serverproxy plugin).
 *
 * TODO: allow the host and port number to be set from the outside somehow
 *
 * @author Martin Horn, University of Konstanz
 */
public class RSClientServiceFactory implements ServiceFactory {

    //TODO get the rest adress from somewhere else!
    private static final String GATEWAY_PATH = "v4/gateway/";

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends GatewayService> S createService(final Class<S> serviceInterface, final ServiceConfig serviceConfig) {
        try {
            String name = ObjectSpecUtil.extractNameFromClass(serviceInterface, "api");
            String namespace = ObjectSpecUtil.extractNamespaceFromClass(serviceInterface, "api");
            String fullyQualifiedName =
                org.knime.core.gateway.jaxrs.ObjectSpecUtil.getFullyQualifiedName(namespace, name, "rest");

            Class<?> rsServiceInterface = Class.forName(fullyQualifiedName);
            List<Object> providers = Arrays.asList(new EntityJSONSerializer(), new EntityJSONDeserializer(),
                new EntityCollectionJSONSerializer(), new EntityCollectionJSONDeserializer(),
                new MapJSONDeserializer(), new OptionalParamConverter(), new NoSuchElementExceptionMapper());
            if(serviceConfig instanceof ServerServiceConfig) {
                ServerServiceConfig serverServiceConfig = (ServerServiceConfig) serviceConfig;
                String url = "http://" + serverServiceConfig.getHost() + ":" + serverServiceConfig.getPort() + serverServiceConfig.getPath() + "/" + GATEWAY_PATH;
                return (S)JAXRSClientFactory.create(url, rsServiceInterface, providers);
            } else {
                throw new IllegalStateException("No server service config given!");
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
