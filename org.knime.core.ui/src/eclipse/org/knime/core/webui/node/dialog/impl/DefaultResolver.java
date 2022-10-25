/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   3 Mar 2022 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.impl;

import static org.knime.core.webui.node.dialog.impl.JsonFormsDataUtil.createInstance;
import static org.knime.core.webui.node.dialog.impl.JsonFormsDataUtil.createInstanceWithSpecs;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.LRUCache;

import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.FieldScope;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class DefaultResolver implements ConfigFunction<FieldScope, Object> {

    private final PortObjectSpec[] m_specs;

    private final LRUCache<Class<?>, Object> m_defaultObjects = new LRUCache<>(4);

    DefaultResolver(final PortObjectSpec[] specs) {
        m_specs = specs;
    }

    @Override
    public Object apply(final FieldScope field) { // NOSONAR
        if (field.isFakeContainerItemScope()) {
            return null;
        }

        final var declaringClass = field.getDeclaringType().getErasedType();
        try {
            final var defaultObject = m_defaultObjects.computeIfAbsent(declaringClass,
                c -> m_specs == null ? createInstance(c) : createInstanceWithSpecs(c, m_specs));
            if (defaultObject == null) {
                return null;
            }

            final var rawField = field.getRawMember();
            rawField.setAccessible(true); // NOSONAR
            return rawField.get(defaultObject);
        } catch (Exception e) { // NOSONAR
            NodeLogger.getLogger(DefaultResolver.class)
                .error(String.format("Failed to determine default value of field %s in class %s.", field.getName(),
                    declaringClass.getName()), e);
            return null;
        }
    }
}
