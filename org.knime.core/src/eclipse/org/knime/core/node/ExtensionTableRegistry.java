/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 26, 2020 (dietzc): created
 */
package org.knime.core.node;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.ExtensionTable.LoadContext;

/**
 * Collects extensions from the extension point and provides it to the framework.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz
 * @since 4.2
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ExtensionTableRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExtensionTableRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.ExtensionTable";

    private static ExtensionTableRegistry INSTANCE = createInstance();

    private static ExtensionTableRegistry createInstance() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        List<ExtensionTableLoader> factoryList =
            Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                .map(cfe -> readFactory(cfe)).filter(f -> f != null).collect(Collectors.toList());

        return new ExtensionTableRegistry(factoryList);
    }

    private static ExtensionTableLoader readFactory(final IConfigurationElement cfe) {
        try {
            ExtensionTableLoader ext = (ExtensionTableLoader)cfe.createExecutableExtension("extensionTableLoader");
            LOGGER.debugWithFormat("Added Extension Table '%s' from '%s'", ext.getClass().getName(),
                cfe.getContributor().getName());
            return ext;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s",
                ExtensionTableLoader.class.getName(), cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

    /** @return the instance to use. */
    public static ExtensionTableRegistry getInstance() {
        return INSTANCE;
    }

    private final List<ExtensionTableLoader> m_extTables;

    private ExtensionTableRegistry(final List<ExtensionTableLoader> extensionTableLoaders) {
        m_extTables = Collections.unmodifiableList(extensionTableLoaders);
    }

    /** @return the extension tables in an unmodifiable list. */
    public List<ExtensionTableLoader> getExtensionTableLoadersFactories() {
        return m_extTables;
    }

    /**
     * @param context used to load extension table.
     *
     * @param extensionTableType fully qualified class name of the saved table
     * @return the loaded extension table. Can be of different type than the extensionTableType
     * @throws IllegalArgumentException If the factory is unknown (usually means: not installed)
     */
    public ExtensionTable loadExtensionTable(final LoadContext context, final String extensionTableType)
        throws IllegalArgumentException {
        try {
            final ExtensionTableLoader loader = m_extTables.stream()//
                .filter(f -> f.canLoad(extensionTableType))//
                .findFirst()//
                .orElseThrow(() -> new IllegalArgumentException(extensionTableType));

            return loader.load(context);
        } catch (InvalidSettingsException ex) {
            throw new IllegalArgumentException("Error when loading extension table " + extensionTableType + ".", ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Extension Tables: [");
        b.append(String.join(", ", m_extTables.stream().map(s -> s.getClass().getName()).collect(Collectors.toList())))
            .append("]");
        return b.toString();
    }

}
