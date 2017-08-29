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
 *   Aug 1, 2017 (hornm): created
 */
package org.knime.gateway.jsonrpc;

import java.util.List;
import java.util.stream.Collectors;

import org.knime.gateway.EntityDefUtil;
import org.knime.gateway.ObjectSpecUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for json rpc stuff.
 *
 * @author Martin Horn, University of Konstanz
 */
public class JsonRpcUtil {

    /**
     * Name of the property to encode the entity type in a json-serialized entity object.
     */
    public static final String ENTITY_TYPE_KEY = "EntityType";

    private JsonRpcUtil() {
        //utility class
    }

    /**
     * Adds entity and entity builder mixin classes to the passed mapper in order to add jackson-annotations to the
     * respective entity (and entity builder) interface for de-/serialization.
     *
     * @param mapper the object mapper to add the mixins to
     */
    public static final void addMixIns(final ObjectMapper mapper) {
        List<Class<?>> entityClasses = getEntityClasses();
        List<Class<?>> entityBuilderClasses = getEntityBuilderClasses();
        List<Class<?>> entityMixInClasses = getEntityMixInClasses();
        List<Class<?>> entityBuilderMixInClasses = getEntityBuilderMixInClasses();

        for (int i = 0; i < entityClasses.size(); i++) {
            mapper.addMixIn(entityClasses.get(i), entityMixInClasses.get(i));
            mapper.addMixIn(entityBuilderClasses.get(i), entityBuilderMixInClasses.get(i));
        }
    }

    private static final List<Class<?>> getEntityMixInClasses() {
        return getThisEntityClassesForSpec("mixin");
    }

    private static final List<Class<?>> getEntityBuilderMixInClasses() {
        return getThisEntityClassesForSpec("builder-mixin");
    }

    private static final List<Class<?>> getEntityClasses() {
        return getApiEntityClassesForSpec("api");
    }

    private static final List<Class<?>> getEntityBuilderClasses() {
        return getApiEntityClassesForSpec("builder");
    }

    private static final List<Class<?>> getThisEntityClassesForSpec(final String specId) {
        return EntityDefUtil.getEntities().stream().map(p -> {
            try {
                return org.knime.gateway.jsonrpc.ObjectSpecUtil.getClassForFullyQualifiedName(p.getRight(), p.getLeft(),
                    specId);
            } catch (ClassNotFoundException ex) {
                // TODO better exception handling
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
    }

    private static final List<Class<?>> getApiEntityClassesForSpec(final String specId) {
        return EntityDefUtil.getEntities().stream().map(p -> {
            try {
                return ObjectSpecUtil.getClassForFullyQualifiedName(p.getRight(), p.getLeft(), specId);
            } catch (ClassNotFoundException ex) {
                // TODO better exception handling
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
    }
}
