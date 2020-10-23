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
 */
package org.knime.core.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.container.BufferedTableBackend;
import org.knime.core.node.NodeLogger;

/**
 * Registry for TableBackend extension point.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.3
 *
 * @noreference This interface is not intended to be referenced by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public final class TableBackendRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableBackendRegistry.class);

    /** Denotes the class name of the backend that is used for newly created workflows. By default it's the standard
     * buffered data table backend ({@link BufferedTableBackend}) but can be changed to a columnar table backend
     * (value is <code>org.knime.core.data.columnar.ColumnarTableBackend</code>).
     */
    public static final String PROPERTY_TABLE_BACKEND_IMPLEMENTATION = "knime.tablebackend";

    /** If {@link #PROPERTY_TABLE_BACKEND_IMPLEMENTATION} is set, force this backend implementation also onto nodes
     * that exist in old workflows that were executed with a different. It's highly recommended to not set this
     * property as this can lead to workflows having tables stored in different backend implementations. It should only
     * be used in test set-ups.
     *
     * <p>
     * Value is <code>true</code> or <code>false</code> (default).
     */
    public static final String PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE = "knime.tablebackend.force";

    private static final String EXT_POINT_ID = "org.knime.core.TableBackend";

    private static final TableBackendRegistry INSTANCE;

    static {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        List<TableBackend> backendList = Stream.of(point.getExtensions())
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())).map(TableBackendRegistry::readBackend)
            .filter(Objects::nonNull).sorted(Comparator.comparing(f -> f.getClass().getName(), (a, b) -> { // NOSONAR
                // sort formats so that the "KNIME standard" format comes first.
                if (Objects.equals(a, b)) {
                    return 0;
                } else if (BufferedTableBackend.class.getName().equals(a)) {
                    return -1;
                } else if (BufferedTableBackend.class.getName().equals(b)) {
                    return +1;
                } else {
                    return a.compareTo(b);
                }
            })).collect(Collectors.toList());

        TableBackend defaultBackend = backendList.stream() //
            .filter(f -> f.getClass().equals(BufferedTableBackend.class)) //
            .findFirst() //
            .orElseThrow(() -> new IllegalStateException(
                String.format("No fallback table backend registered, expected '%s' but not present in '%s'",
                    BufferedTableBackend.class.getName(),
                    StringUtils.join(backendList.stream().map(f -> f.getClass().getName()).iterator(), ", "))));

        String defBackendString = System.getProperty(PROPERTY_TABLE_BACKEND_IMPLEMENTATION);
        if (defBackendString != null) {
            Optional<TableBackend> customDefaultBackendOptional = backendList.stream()
                .filter(backend -> Objects.equals(backend.getClass().getName(), defBackendString)).findFirst();
            if (customDefaultBackendOptional.isPresent()) {
                defaultBackend = customDefaultBackendOptional.get();
                LOGGER.debugWithFormat("Using new default table format: \"%s\" (%s)", defaultBackend.getShortName(),
                    defaultBackend.getClass().getName());
            } else {
                LOGGER.errorWithFormat(
                    "System property \"%s\" refers to an implementation \"%s\" but it's not "
                        + "available. Valid values are {%s}",
                    PROPERTY_TABLE_BACKEND_IMPLEMENTATION, defBackendString, backendList.stream() //
                        .map(TableBackend::getClass) //
                        .map(Class::getName) //
                        .map(s -> "\"" + s + "\"") //
                        .collect(Collectors.joining(", ")));
            }
        }

        boolean forceDefaultBackendOnOldWorkflows = Boolean.getBoolean(PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE);
        if (forceDefaultBackendOnOldWorkflows) {
            LOGGER.warnWithFormat("Forcing table format \"%s\" onto existing (old) workflows -- do not rely on any "
                    + "workflow created using this instance (review %s property to change this)",
                defaultBackend.getClass().getName(), PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE);
        }

        INSTANCE = new TableBackendRegistry(backendList, defaultBackend, forceDefaultBackendOnOldWorkflows);
    }

    private static TableBackend readBackend(final IConfigurationElement cfe) {
        try {
            TableBackend f = (TableBackend)cfe.createExecutableExtension("backend");
            LOGGER.debugWithFormat("Added table backend '%s' from '%s'", f.getClass().getName(),
                cfe.getContributor().getName());
            return f;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s", TableBackend.class.getName(),
                cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

    /** @return the instance to use. */
    public static final TableBackendRegistry getInstance() {
        return INSTANCE;
    }

    private final List<TableBackend> m_backends;

    private final TableBackend m_defaultBackend;

    private final boolean m_forceDefaultBackendOnOldWorkflows;

    private TableBackendRegistry(final List<TableBackend> dataContainerFactories, final TableBackend defaultBackend,
        final boolean forceDefaultBackendOnOldWorkflows) {
        m_backends = Collections.unmodifiableList(dataContainerFactories);
        m_defaultBackend = defaultBackend;
        m_forceDefaultBackendOnOldWorkflows = forceDefaultBackendOnOldWorkflows;
    }

    /** @return the data container delegate factories in an unmodifiable list. */
    public final List<TableBackend> getTableBackends() {
        return m_backends;
    }

    /**
     * @param fullyQualifiedClassName class name in question
     * @return the table backend with the given class name - used to restore a previously saved table.
     * @throws IllegalArgumentException If the backend is unknown (usually means: not installed)
     */
    public final TableBackend getTableBackend(final String fullyQualifiedClassName) {
        return m_backends.stream()//
            .filter(f -> f.getClass().getName().equals(fullyQualifiedClassName))//
            .findFirst()//
            .orElseThrow(() -> new IllegalArgumentException(fullyQualifiedClassName));
    }

    /**
     * @return the {@link BufferedTableBackend} or whatever is specified via
     *         {@link #PROPERTY_TABLE_BACKEND_IMPLEMENTATION}
     */
    public final TableBackend getDefaultBackendForNewWorkflows() {
        return m_defaultBackend;
    }

    /**
     * @return the backend that was used prior KNIME 4.3, that is before it could be set on a workflow level. Used
     * as default when workflows are loaded that don't have the corresponding option set. ({@link BufferedTableBackend}.
     */
    public final TableBackend getPre43TableBackend() {
        return m_backends.stream().filter(b -> b.getClass().equals(BufferedTableBackend.class)).findFirst()
            .orElseThrow(() -> new IllegalStateException(BufferedTableBackend.class.getName() + " not found in list"));
    }

    /**
     * @return <code>true</code> if the default table format is forced onto workflows that are created &lt;4.3 (no table
     * backend selected). This should only be true in test set-ups.
     * @see #PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE
     */
    public boolean isForceDefaultBackendOnOldWorkflows() {
        return m_forceDefaultBackendOnOldWorkflows;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("TableBackends: [");
        b.append(String.join(", ", m_backends.stream().map(s -> s.getClass().getName()).collect(Collectors.toList())))
            .append("]");
        return b.toString();
    }
}
