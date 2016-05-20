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
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.core.data.container.storage;

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
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DefaultTableStoreFormat;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author wiswedel
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class TableStoreFormatRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableStoreFormatRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.TableFormat";

    private static final String PROPERTY_TABLE_FORMAT = "knime.table.format";

    private static TableStoreFormatRegistry INSTANCE = createInstance();

    private static TableStoreFormatRegistry createInstance() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        List<AbstractTableStoreFormat> formatList = Stream.of(point.getExtensions())
                .flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                .map(cfe -> readFormat(cfe)).filter(f -> f != null).collect(Collectors.toList());

        String prefTableFormat = System.getProperty(PROPERTY_TABLE_FORMAT);
        formatList.sort(new FormatComparator(prefTableFormat));

        boolean hasFallback= formatList.stream().anyMatch(f -> f.getClass().equals(DefaultTableStoreFormat.class));
        CheckUtils.checkState(hasFallback, "No fallback table format registered, expected '%s' but not present in '%s'",
            DefaultTableStoreFormat.class.getName(),
            StringUtils.join(formatList.stream().map(f -> f.getClass().getName()).iterator(), ", "));

        if (StringUtils.isNotBlank(prefTableFormat)) {
            if (formatList.get(0).getClass().getName().equals(prefTableFormat)) {
                LOGGER.debugWithFormat("Choosing '%s' as table format", prefTableFormat);
            } else {
                LOGGER.warnWithFormat("Preferred table format '%s' is not available/installed. (Valid values: '%s')",
                    prefTableFormat, StringUtils.join(formatList, ", "));
            }
        }
        return new TableStoreFormatRegistry(formatList);
    }

    private static AbstractTableStoreFormat readFormat(final IConfigurationElement cfe) {
        try {
            AbstractTableStoreFormat f = (AbstractTableStoreFormat)cfe.createExecutableExtension("formatDefinition");
            LOGGER.debugWithFormat("Added table storage format '%s' from '%s'",
                f.getClass().getName(), cfe.getContributor().getName());
            return f;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s",
                AbstractTableStoreFormat.class.getName(), cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

    /** @return the instance to use. */
    public static TableStoreFormatRegistry getInstance() {
        return INSTANCE;
    }

    private final List<AbstractTableStoreFormat> m_tableStoreFormats;

    private TableStoreFormatRegistry(final List<AbstractTableStoreFormat> tableStoreFormats) {
        m_tableStoreFormats = tableStoreFormats;
    }

    /** @return the preferred format. */
    public AbstractTableStoreFormat getPreferredTableStoreFormat() {
        return m_tableStoreFormats.get(0);
    }

    /** @param spec the spec of the table to write.
     * @return the format accepting to write that schema, if possible {@link #getPreferredTableStoreFormat()}.
     */
    public AbstractTableStoreFormat getFormatFor(final DataTableSpec spec) {
        return m_tableStoreFormats.stream().filter(f -> f.accept(spec)).findFirst().orElseThrow(
            () -> new InternalError("No registered format accepts the current table schema"));
    }

    /** @param fullyQualifiedClassName class name in question
     * @return the table format with the given class name - used to restore a previously saved table. */
    public Optional<AbstractTableStoreFormat> getTableStoreFormat(final String fullyQualifiedClassName) {
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

    private static final class FormatComparator implements Comparator<AbstractTableStoreFormat> {
        private final String m_preferredFormatPerSysProperty;

        /**
         * @param preferredFormatPerSysProperty
         */
        FormatComparator(final String preferredFormatPerSysProperty) {
            m_preferredFormatPerSysProperty = preferredFormatPerSysProperty;
        }

        /** {@inheritDoc} */
        @Override
        public int compare(final AbstractTableStoreFormat o1, final AbstractTableStoreFormat o2) {
            String o1String = o1.getClass().getName();
            String o2String = o2.getClass().getName();
            if (Objects.equals(o1String, m_preferredFormatPerSysProperty)) {
                return -1;
            }
            if (Objects.equals(o2String, m_preferredFormatPerSysProperty)) {
                return 1;
            }
            if (Objects.equals(DefaultTableStoreFormat.class.getName(), o1String)) {
                return -1;
            }
            if (Objects.equals(DefaultTableStoreFormat.class.getName(), o2String)) {
                return 1;
            }
            return 0;
        }
    }
}
