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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.data.container.BufferedTableBackend;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.Preferences;

/**
 * Registry for TableBackend extension point.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
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

    /** Preference key for selecting the table backend. */
    public static final String PREF_KEY_TABLE_BACKEND = "knime.core.table-backend";

    private static final String CORE_BUNDLE_SYMBOLIC_NAME =
        FrameworkUtil.getBundle(TableBackendRegistry.class).getSymbolicName();

    private static final IEclipsePreferences CORE_PREFS = InstanceScope.INSTANCE.getNode(CORE_BUNDLE_SYMBOLIC_NAME);

    private static final IEclipsePreferences DEFAULT_CORE_PREFS =
        DefaultScope.INSTANCE.getNode(CORE_BUNDLE_SYMBOLIC_NAME);

    private static final TableBackendRegistry INSTANCE;

    static {
        final Map<String, TableBackend> backendMap = createBackendMap();
        final Optional<TableBackend> propertyBackend = getBackendFromProperty(backendMap);
        final boolean forcePropertyBackend = getForcePropertyBackend(propertyBackend);
        INSTANCE = new TableBackendRegistry(backendMap, propertyBackend, forcePropertyBackend);
    }

    /** @return the value of {@link TableBackendRegistry#PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE} */
    private static boolean getForcePropertyBackend(final Optional<TableBackend> propertyBackend) {
        boolean forceDefaultBackendOnOldWorkflows =
            propertyBackend.isPresent() && Boolean.getBoolean(PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE);
        if (forceDefaultBackendOnOldWorkflows) {
            LOGGER.warnWithFormat(
                "Forcing table format \"%s\" onto existing (old) workflows -- do not rely on any "
                    + "workflow created using this instance (review %s property to change this)",
                propertyBackend.get().getClass().getName(), PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE);
        }
        return forceDefaultBackendOnOldWorkflows;
    }

    /** @return the table backend configured in the VM property "knime.tablebackend". */
    private static Optional<TableBackend> getBackendFromProperty(final Map<String, TableBackend> backendMap) {
        final String propertyBackendName = System.getProperty(PROPERTY_TABLE_BACKEND_IMPLEMENTATION);
        if (propertyBackendName != null) {
            final TableBackend propertyBackend = backendMap.get(propertyBackendName);
            if (propertyBackend != null) {
                LOGGER.debugWithFormat("Using new default table format: \"%s\" (%s)", propertyBackend.getShortName(),
                    propertyBackend.getClass().getName());
                return Optional.of(propertyBackend);
            } else {
                LOGGER.errorWithFormat(
                    "System property \"%s\" refers to an implementation \"%s\" but it's not "
                        + "available. Valid values are {%s}",
                    PROPERTY_TABLE_BACKEND_IMPLEMENTATION, propertyBackendName, backendNamesToString(backendMap));
            }
        }
        return Optional.empty();
    }

    /** Create a string with the class names of all table backends. Use for logging. */
    private static String backendNamesToString(final Map<String, TableBackend> backendMap) {
        return backendMap.keySet() //
            .stream() //
            .map(s -> "\"" + s + "\"") //
            .collect(Collectors.joining(", "));
    }

    /** Create a map of all table backends registered at the extension point */
    private static Map<String, TableBackend> createBackendMap() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        return Stream.of(point.getExtensions()) //
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())) //
            .map(TableBackendRegistry::readBackend) //
            .filter(Objects::nonNull) //
            .collect(Collectors.toMap(t -> t.getClass().getName(), t -> t));
    }

    /** Create the TableBackend instance from the extension point configuration */
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

    /**
     * Initialize the default preferences for the table backend. This method is used by the preference initializer and
     * should not be used by clients otherwise.
     */
    public static void initDefaultPreferences() {
        DEFAULT_CORE_PREFS.put(PREF_KEY_TABLE_BACKEND, BufferedTableBackend.class.getName());
    }

    /** @return the instance to use. */
    public static final TableBackendRegistry getInstance() {
        return INSTANCE;
    }

    private final Map<String, TableBackend> m_tableBackends;

    private final Optional<TableBackend> m_propertyBackend;

    private final boolean m_forcePropertyBackend;

    private TableBackendRegistry(final Map<String, TableBackend> tableBackends,
        final Optional<TableBackend> propertyBackend, final boolean forcePropertyBackend) {
        m_tableBackends = tableBackends;
        m_propertyBackend = propertyBackend;
        m_forcePropertyBackend = forcePropertyBackend;
    }

    /** @return the data container delegate factories in an unmodifiable list. */
    public final List<TableBackend> getTableBackends() {
        return m_tableBackends.values().stream() //
            .sorted(Comparator.comparing(f -> f.getClass().getName(), (a, b) -> { // NOSONAR
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
            })) //
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * @param fullyQualifiedClassName class name in question
     * @return the table backend with the given class name - used to restore a previously saved table.
     * @throws IllegalArgumentException If the backend is unknown (usually means: not installed)
     */
    public final TableBackend getTableBackend(final String fullyQualifiedClassName) {
        final TableBackend backend = m_tableBackends.get(fullyQualifiedClassName);
        if (backend == null) {
            throw new IllegalArgumentException(fullyQualifiedClassName);
        }
        return backend;
    }

    /**
     * @return the {@link BufferedTableBackend} or whatever is specified via
     *         {@link #PROPERTY_TABLE_BACKEND_IMPLEMENTATION} or on the "Table Backend" preference page.
     */
    public final TableBackend getDefaultBackendForNewWorkflows() {
        // The VM property has the highest priority
        if (m_propertyBackend.isPresent()) {
            return m_propertyBackend.get();
        }
        // Look in the preferences
        final String backendName = Platform.getPreferencesService().get(PREF_KEY_TABLE_BACKEND,
            BufferedTableBackend.class.getName(), new Preferences[]{CORE_PREFS, DEFAULT_CORE_PREFS});
        final TableBackend backend = m_tableBackends.get(backendName);
        if (backend == null) {
            throw new IllegalStateException(String.format(
                "The configured table backend '%s' is not available. Available backends are {%s}. Is an extension missing?",
                backendName, backendNamesToString(m_tableBackends)));
        }
        return backend;
    }

    /**
     * @return the backend that was used prior KNIME 4.3, that is before it could be set on a workflow level. Used
     * as default when workflows are loaded that don't have the corresponding option set. ({@link BufferedTableBackend}.
     */
    public final TableBackend getPre43TableBackend() {
        final String defaultBackendName = BufferedTableBackend.class.getName();
        final TableBackend defaultBackend = m_tableBackends.get(defaultBackendName);
        if (defaultBackend == null) {
            throw new IllegalStateException(
                String.format("No fallback table backend registered, expected '%s' but not present in {%s}",
                    defaultBackendName, backendNamesToString(m_tableBackends)));
        }
        return defaultBackend;
    }

    /**
     * @return <code>true</code> if the default table format is forced onto workflows that are created &lt;4.3 (no table
     * backend selected). This should only be true in test set-ups.
     * @see #PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE
     */
    public boolean isForceDefaultBackendOnOldWorkflows() {
        return m_forcePropertyBackend;
    }

    /**
     * @return <code>true</code> if the VM property {@link #PROPERTY_TABLE_BACKEND_IMPLEMENTATION} is set. If it is set
     *         the backend from this property will always be used for new workflows and the configuration on the
     *         preference page will be ignored.
     * @see #PROPERTY_TABLE_BACKEND_IMPLEMENTATION
     * @see #PROPERTY_TABLE_BACKEND_IMPLEMENTATION_FORCE
     * @since 4.5
     */
    public boolean isBackendPropertySet() {
        return m_propertyBackend.isPresent();
    }

    @Override
    public String toString() {
        return new StringBuilder() //
            .append("TableBackends: [") //
            .append(backendNamesToString(m_tableBackends)) //
            .append("]") //
            .toString();
    }
}
