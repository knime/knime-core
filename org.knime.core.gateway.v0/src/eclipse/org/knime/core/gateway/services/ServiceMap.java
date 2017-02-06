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
 *   Jan 25, 2017 (hornm): created
 */
package org.knime.core.gateway.services;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.gateway.codegen.ServiceGenerator;
import org.knime.core.gateway.codegen.types.ServiceDef;
import org.knime.core.gateway.v0.workflow.service.GatewayService;

/**
 * A map of {@link GatewayService} names/interfaces to derived or related services of type <code>T</code>.
 *
 * @author Martin Horn, University of Konstanz
 * @param <T>
 */
public abstract class ServiceMap<T> {

    private static List<ServiceDef> SERVICE_DEFS;

    private static Map<String, Class<? extends GatewayService>> INTERFACE_MAP = null;

    private Map<String, T> m_serviceMap;

    {
        try {
            SERVICE_DEFS = ServiceGenerator.readAll(ServiceDef.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Creates a new service map and builds the internal maps (see {@link #mapIntern(String)}.
     */
    protected ServiceMap() {
        m_serviceMap = new HashMap<>();
        SERVICE_DEFS.forEach(sd -> {
            m_serviceMap.put(sd.getName(), mapIntern(sd.getName()));
        });
    }

    /**
     * @param serviceName the service name without the name space
     * @return the mapped object
     */
    protected abstract T mapIntern(String serviceName);

    /**
     * @param serviceClass
     * @return the object (e.g. a service implementation) mapped to the given service interface
     */
    public T get(final Class<? extends GatewayService> serviceClass) {
        return m_serviceMap.get(getServiceName(serviceClass));
    }

    /**
     * @param serviceName
     * @return the object (e.g. a service implementation) mapped to service given by its name
     */
    public T get(final String serviceName) {
        return m_serviceMap.get(serviceName);
    }

    /**
     * Helper method to determine the service name (unique identifier) for a service class/interface that
     * implements/extends a gateway service interface.
     *
     * @param serviceClass
     * @return the service name, <code>null</code> if class is not derived from a service interface
     */
    public static synchronized String getServiceName(final Class<?> serviceClass) {
        //find the interface that directly inherits from the GatewayService interface (api)
        //- it should have only one super-interface (as defined within this plug-in)
        Class<?>[] interfaces = serviceClass.getInterfaces();
        if (interfaces.length == 1 && GatewayService.class.equals(interfaces[0])) {
            return serviceClass.getSimpleName();
        } else if (interfaces.length > 0) {
            //recursively searches through the class and interface hierarchy
            for (Class<?> i : interfaces) {
                String n = getServiceName(i);
                if (n != null) {
                    return n;
                }
            }
            return getServiceName(serviceClass.getSuperclass());
        } else {
            return null;
        }
    }

    /**
     * @return a list of all services identified by it's name
     */
    public static synchronized Collection<String> getAllServices() {
        fillServiceInterfaceMap();
        return Collections.unmodifiableCollection(INTERFACE_MAP.keySet());
    }

    /**
     * @param serviceName
     * @return the service interface class for the given service name
     */
    public static synchronized Class<? extends GatewayService> getServiceInterface(final String serviceName) {
        fillServiceInterfaceMap();
        return INTERFACE_MAP.get(serviceName);
    }

    private static void fillServiceInterfaceMap() {
        if (INTERFACE_MAP == null) {
            INTERFACE_MAP = new HashMap<>();
            SERVICE_DEFS.forEach(sd -> {
                try {
                    INTERFACE_MAP.put(sd.getName(), (Class<? extends GatewayService>)Class
                        .forName("org.knime.core.gateway.v0.workflow.service." + sd.getName()));
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
    }

}
