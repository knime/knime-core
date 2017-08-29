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
 *   Nov 9, 2016 (hornm): created
 */
package org.knime.gateway.entities;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.knime.core.util.ExtPointUtils;
import org.knime.gateway.v0.workflow.entity.impl.DefaultWorkflowEntBuilder;
import org.knime.gateway.workflow.entity.GatewayEntity;
import org.knime.gateway.workflow.entity.builder.GatewayEntityBuilder;

/**
 * Manages entity builders (i.e. {@link GatewayEntityBuilder}s) and gives access to they implementations (that are
 * injected via the {@link EntityBuilderFactory} extension point).
 *
 * @author Martin Horn, University of Konstanz
 */
public class EntityBuilderManager {

    private static final Logger LOGGER = Logger.getLogger(EntityBuilderManager.class);

    private static EntityBuilderFactory BUILDER_FACTORY;

    private EntityBuilderManager() {
        //utility class
    }

    /**
     * Delivers implementations for entity builder interfaces (see {@link GatewayEntityBuilder}). Implementations are
     * injected via {@link EntityBuilderFactory} extension point.
     *
     * @param builderInterface the builder interface the implementation is requested for
     * @return an implementation of the requested builder interface (it returns a new instance with every method call)
     */
    public static <E extends GatewayEntity, B extends GatewayEntityBuilder<E>> B
        builder(final Class<B> builderInterface) {
        if (BUILDER_FACTORY == null) {
            BUILDER_FACTORY = createBuilderFactory();
        }
        return BUILDER_FACTORY.createEntityBuilder(builderInterface);
    }

    /**
     * @param builderInterface
     * @return the default implementation for the given builder interface (e.g. {@link DefaultWorkflowEntBuilder})
     */
    public static <E extends GatewayEntity, B extends GatewayEntityBuilder<E>> B
        defaultBuilder(final Class<B> builderInterface) {
        try {
            return (B)builderInterface.getMethod("defaultBuilder").invoke(null);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static EntityBuilderFactory createBuilderFactory() {
        List<EntityBuilderFactory> instances = ExtPointUtils
            .collectExecutableExtensions(EntityBuilderFactory.EXT_POINT_ID, EntityBuilderFactory.EXT_POINT_ATTR);
        if (instances.size() == 0) {
            LOGGER.warn("No entity builder factory registered. Default factory used.");
            return new DefaultEntityBuilderFactory();

        } else if (instances.size() > 1) {
            LOGGER.warn("Multiple entity builder factories registered. The one with the highest priority used.");
            Collections.sort(instances, (o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
        }
        return instances.get(0);
    }

}
