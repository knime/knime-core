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
 *   Nov 8, 2016 (hornm): created
 */
package org.knime.core.gateway.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.knime.core.gateway.entities.EntityBuilderManager;
import org.knime.core.gateway.v0.workflow.service.GatewayService;
import org.knime.core.gateway.v0.workflow.service.WorkflowService;
import org.knime.core.util.ExtPointUtils;
import org.knime.core.util.Pair;

/**
 * Manages services (i.e. {@link GatewayService}s) and gives access to service interface implementations.
 *
 * @author Martin Horn, University of Konstanz
 */
public class ServiceManager {

    private static final Logger LOGGER = Logger.getLogger(EntityBuilderManager.class);

    private static ServiceFactory SERVICE_FACTORY;

    /* SERVICES SINGLEON INSTANCES */
    private static Map<Pair<Class<? extends GatewayService>, ServiceConfig>, GatewayService> SERVICES = new HashMap<>();

    private ServiceManager() {
        //private 'utility' class
    }

    /**
     * Delivers implementations for service interfaces (see {@link GatewayService}. Implementations are injected via
     * {@link ServiceFactory} extension point.
     *
     * In order to be sure that the right service is delivered at any time and stateful
     * services are handled correctly, always use this method to access the desired service and never keep a service
     * reference (unless you know what you are doing).
     *
     * TODO add more shortcuts for services (e.g. ServiceManager.nodeService())
     *
     * @param serviceInterface the service interface the implementation is requested for
     * @param serviceConfig the service configuration required to instantiate a service, e.g. server information etc.
     * @return an implementation of the requested service interface (it returns the same instance with every method
     *         call)
     */
    public static <S extends GatewayService> S service(final Class<S> serviceInterface, final ServiceConfig serviceConfig) {
        Pair<Class<? extends GatewayService>, ServiceConfig> pair = Pair.create(serviceInterface, serviceConfig);
        S service = (S)SERVICES.get(pair);
        if (service == null) {
            if (SERVICE_FACTORY == null) {
                SERVICE_FACTORY = createServiceFactory();
            }
            service = SERVICE_FACTORY.createService(serviceInterface, serviceConfig);
            SERVICES.put(pair, service);
        }
        return service;
    }

    /**
     * Shortcut for {@link #service(WorkflowService.class, ServiceConfig)}.
     *
     * @param serviceConfig see {@link #service(Class, ServiceConfig)}
     * @return the workflow service implementation
     */
    public static WorkflowService workflowService(final ServiceConfig serviceConfig) {
        return service(WorkflowService.class, serviceConfig);
    }

//    /**
//     * @return a list of all available gateway services (as determined by the api definition files) - see also
//     *         {@link ServiceMap}
//     */
//    public static List<Class<? extends GatewayService>> getAllServices() {
//        return ObjectDefMap.getAllServices().stream().map(s -> ObjectDefMap.getServiceInterface(s))
//            .collect(Collectors.toList());
//    }

    private static ServiceFactory createServiceFactory() {
        List<ServiceFactory> instances =
            ExtPointUtils.collectExecutableExtensions(ServiceFactory.EXT_POINT_ID, ServiceFactory.EXT_POINT_ATTR);

        if(instances.size() == 0) {
            throw new IllegalStateException("No service factory registered!");
        } else if(instances.size() > 1) {
            LOGGER.warn("Multiple service factories registered! The one with the highest priority is used.");
            Collections.sort(instances, (o1,o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
        }
        return instances.get(0);
    }
}
