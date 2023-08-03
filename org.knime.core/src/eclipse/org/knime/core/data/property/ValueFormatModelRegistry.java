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
 *   2 Aug 2023 (jasper): created
 */
package org.knime.core.data.property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.RegistryFactory;
import org.knime.core.node.NodeLogger;

/**
 * Singleton class that provides the formatters registered at the extension point
 * {@code org.knime.core.DataValueFormatter}.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class ValueFormatModelRegistry {

    static final String EXTENSION_POINT = "org.knime.core.DataValueFormatter";

    private static final String FACTORY_CLASS = "factoryClass";

    private static final String FORMATTER_CLASS = "formatterClass";

    private static final ValueFormatModelRegistry INSTANCE = new ValueFormatModelRegistry();

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ValueFormatModelRegistry.class);

    private final Map<String, ValueFormatModelFactory<ValueFormatModel>> m_formatterFactories = new HashMap<>();

    private final Map<String, String> m_formatterExtensions = new HashMap<>();

    static {
        Stream.of(RegistryFactory.getRegistry().getExtensionPoint(EXTENSION_POINT).getExtensions()) // find extensions
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())) // find all formatters from those extensions
            .forEach(e -> registerFormatter(e));
    }

    private ValueFormatModelRegistry() {
        // hide constructor
    }

    private static void registerFormatter(final IConfigurationElement ext) {
        try {
            final var id = ext.getAttribute(FORMATTER_CLASS);
            @SuppressWarnings("unchecked")
            final var fac = (ValueFormatModelFactory<ValueFormatModel>)ext.createExecutableExtension(FACTORY_CLASS);
            if (!id.equals(fac.getFormatterClass().getName())) {
                // we need to check this because otherwise saving/loading would be inconsistent
                final var msg =
                    String.format("The formatter factory %s's type does not match the registered formatter %s."
                        + " Please fix the formatter (-factory) implementation.", fac.getClass(), id);
                LOGGER.coding(msg);
                return;
            }
            // at this point, we should have a valid factory
            INSTANCE.m_formatterFactories.put(id, fac);
            INSTANCE.m_formatterExtensions.put(id, ext.getContributor().getName());
        } catch (final ClassCastException | CoreException e) {
            final var factoryClass = ext.getAttribute(FACTORY_CLASS);
            LOGGER.warn("The formatter factory " + factoryClass + " cannot be instantiated.", e);
        } catch (final InvalidRegistryObjectException e) {
            LOGGER.warn("Internal error while finding extensions registered for " + EXTENSION_POINT + ".", e);
        }
    }

    /**
     * Get a factory instance for a defined formatter id (i.e. class name)
     *
     * @param id the class name of the desired formatter
     * @return A matching formatter factory instance, or {@code Optional.empty()} if no matching formatter could be
     *         found.
     */
    public static Optional<ValueFormatModelFactory<ValueFormatModel>> getFactory(final String id) {
        return Optional.ofNullable(INSTANCE.m_formatterFactories.get(id));
    }

    /**
     * Get the name of the extension that provided a formatter
     *
     * @param id the class name of the formatter
     * @return The name of the extension that the formatter came with
     */
    public static Optional<String> getFactoryProvider(final String id) {
        return Optional.ofNullable(INSTANCE.m_formatterExtensions.get(id));
    }

}
