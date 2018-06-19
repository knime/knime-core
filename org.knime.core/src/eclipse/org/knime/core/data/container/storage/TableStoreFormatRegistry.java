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
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.core.data.container.storage;

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
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DefaultTableStoreFormat;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.osgi.framework.FrameworkUtil;

/**
 * Collects extensions from the extension point and provides it to the framework.
 * @author wiswedel
 * @since 3.6
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class TableStoreFormatRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableStoreFormatRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.TableFormat";

    private static final IEclipsePreferences CORE_PREFS = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(
        TableStoreFormatRegistry.class).getSymbolicName());

    private static final IEclipsePreferences CORE_DEFAULT_PREFS = DefaultScope.INSTANCE.getNode(FrameworkUtil
        .getBundle(TableStoreFormatRegistry.class).getSymbolicName());

    /** Preference constant for selecting data storage format. */
    public static final String PREF_KEY_STORAGE_FORMAT = "knime.core.table-store-format";

    private static TableStoreFormatRegistry INSTANCE = createInstance();

    private static TableStoreFormatRegistry createInstance() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        List<TableStoreFormat> formatList = Stream.of(point.getExtensions())
                .flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                .map(cfe -> readFormat(cfe))
                .filter(f -> f != null)
                .sorted(Comparator.comparing(f -> f.getClass().getSimpleName(), (a, b) -> {
                    // sort formats so that the "KNIME standard" format comes first.
                    if (Objects.equals(a, b)) {
                        return 0;
                    } else if (DefaultTableStoreFormat.class.getName().equals(a)) {
                        return -1;
                    } else if (DefaultTableStoreFormat.class.getName().equals(b)) {
                        return +1;
                    } else {
                        return a.compareTo(b);
                    }
                })).collect(Collectors.toList());

        boolean hasFallback= formatList.stream().anyMatch(f -> f.getClass().equals(DefaultTableStoreFormat.class));
        CheckUtils.checkState(hasFallback, "No fallback table format registered, expected '%s' but not present in '%s'",
            DefaultTableStoreFormat.class.getName(),
            StringUtils.join(formatList.stream().map(f -> f.getClass().getName()).iterator(), ", "));

        return new TableStoreFormatRegistry(formatList);
    }

    private static TableStoreFormat readFormat(final IConfigurationElement cfe) {
        try {
            TableStoreFormat f = (TableStoreFormat)cfe.createExecutableExtension("formatDefinition");
            LOGGER.debugWithFormat("Added table storage format '%s' from '%s'",
                f.getClass().getName(), cfe.getContributor().getName());
            return f;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s",
                TableStoreFormat.class.getName(), cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

    /** @return the instance to use. */
    public static TableStoreFormatRegistry getInstance() {
        return INSTANCE;
    }

    private final List<TableStoreFormat> m_tableStoreFormats;

    private TableStoreFormatRegistry(final List<TableStoreFormat> tableStoreFormats) {
        m_tableStoreFormats = Collections.unmodifiableList(tableStoreFormats);
    }

    /** The 'default' format as defined by the default preference scope, or the standard KNIME format if unset. This
     * method is used by the preference page and should not be used by clients otherwise.
     * @return non-null 'default' format.
     * @see #getInstanceTableStoreFormat()
     */
    public TableStoreFormat getDefaultTableStoreFormat() {
        // experimental ORC/hadoop format as default when running nightlies
        String defaultFormatClassName;
        if (KNIMEConstants.isNightlyBuild()) {
            defaultFormatClassName = "org.knime.parquet.ParquetTableStoreFormat";
        } else {
            defaultFormatClassName = DefaultTableStoreFormat.class.getName();
        }
        String defaultID = CORE_DEFAULT_PREFS.get(PREF_KEY_STORAGE_FORMAT, defaultFormatClassName);
        Optional<TableStoreFormat> defaultFormat = m_tableStoreFormats.stream()
                .filter(f -> f.getClass().getName().equals(defaultID)).findFirst();
        if (!defaultFormat.isPresent()) {
            LOGGER.warnWithFormat("Invalid table store format '%s' -- using KNIME standard as default", defaultID);
        }
        return defaultFormat.orElse(m_tableStoreFormats.get(0));
    }

    /** @return the format as defined by the KNIME preferences or the default instead. This is is what is actually
     * used by the core. */
    public TableStoreFormat getInstanceTableStoreFormat() {
        String result = CORE_PREFS.get(PREF_KEY_STORAGE_FORMAT, null);      // instance scope prefs
        if (result == null) {
            return getDefaultTableStoreFormat();
        }
        final String resultFinal = result;
        Optional<TableStoreFormat> match =
                m_tableStoreFormats.stream().filter(f -> f.getClass().getName().equals(resultFinal)).findFirst();
        if (!match.isPresent()) {
            LOGGER.warnWithFormat("Invalid storage format '%s' -- using standard KNIME table format instead", result);
            return m_tableStoreFormats.get(0);
        }
        return match.get();
    }

    /** @return the tableStoreFormats in an unmodifiable list. */
    public List<TableStoreFormat> getTableStoreFormats() {
        return m_tableStoreFormats;
    }

    /** @param spec the spec of the table to write.
     * @return the format accepting to write that schema, if possible {@link #getInstanceTableStoreFormat()}.
     */
    public TableStoreFormat getFormatFor(final DataTableSpec spec) {
        TableStoreFormat instanceTableStoreFormat = getInstanceTableStoreFormat();
        if (instanceTableStoreFormat.accepts(spec)) {
            return instanceTableStoreFormat;
        }
        return m_tableStoreFormats.stream().filter(f -> f.accepts(spec)).findFirst().orElseThrow(
            () -> new InternalError("No registered format accepts the current table schema"));
    }

    /** @param fullyQualifiedClassName class name in question
     * @return the table format with the given class name - used to restore a previously saved table. */
    public Optional<TableStoreFormat> getTableStoreFormat(final String fullyQualifiedClassName) {
        return m_tableStoreFormats.stream()
                .filter(f -> f.getClass().getName().equals(fullyQualifiedClassName)).findFirst();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Table Formats: [");
        b.append(String.join(", ", m_tableStoreFormats.stream().map(
            s -> s.getClass().getName()).collect(Collectors.toList()))).append("]");
        return b.toString();
    }

}
