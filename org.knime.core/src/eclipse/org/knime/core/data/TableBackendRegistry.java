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
import org.knime.core.node.util.CheckUtils;

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

    private static final String EXT_POINT_ID = "org.knime.core.TableBackend";

    private static final TableBackendRegistry INSTANCE = createInstance();

    private static TableBackendRegistry createInstance() {
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

        boolean hasFallback = backendList.stream().anyMatch(f -> f.getClass().equals(BufferedTableBackend.class));
        CheckUtils.checkState(hasFallback,
            "No fallback table backend registered, expected '%s' but not present in '%s'",
            BufferedTableBackend.class.getName(),
            StringUtils.join(backendList.stream().map(f -> f.getClass().getName()).iterator(), ", "));

        return new TableBackendRegistry(backendList);
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

    private TableBackendRegistry(final List<TableBackend> dataContainerFactories) {
        m_backends = Collections.unmodifiableList(dataContainerFactories);
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
    public final TableBackend getTableBackend(final String fullyQualifiedClassName) throws IllegalArgumentException {
        return m_backends.stream()//
            .filter(f -> f.getClass().getName().equals(fullyQualifiedClassName))//
            .findFirst()//
            .orElseThrow(() -> new IllegalArgumentException(fullyQualifiedClassName));
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("TableBackends: [");
        b.append(String.join(", ", m_backends.stream().map(s -> s.getClass().getName()).collect(Collectors.toList())))
            .append("]");
        return b.toString();
    }

    /** @return the {@link BufferedTableBackend} instance (never null). */
    public final TableBackend getDefaultBackend() {
        return m_backends.stream() //
            .filter(tb -> Objects.equals(tb.getClass(), BufferedTableBackend.class)) //
            .findFirst() //
            .orElseThrow(
                () -> new IllegalStateException(BufferedTableBackend.class.getSimpleName() + " not available"));
    }
}
