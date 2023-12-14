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
 *   Dec 14, 2022 (wiswedel): created
 */
package org.knime.core.node.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.BufferedTableBackend;
import org.knime.core.data.container.filter.CloseableDataRowIterable;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowTableBackendSettings;

/**
 * Represents an issue as described by {@link MessageBuilder#addRowIssue(int, int, long, String)} (error row + context).
 *
 * @author Bernd Wiswedel, KNIME GmbH
 * @since 5.0
 */
final class RowIssue implements Issue {

    private static final int UNKNOWN = -1;

    private static final String CFG_MESSAGE = "message";
    private static final String CFG_ROW_INDEX = "rowIndex";
    private static final String CFG_COLUMN_INDEX = "columnIndex";
    private static final String CFG_PORT_INDEX = "portIndex";

    private final int m_portIndex;
    private final int m_columnIndex;
    private final long m_rowIndex;
    private final String m_description;

    static RowIssue create(final int portIndex, final int columnIndex, final long rowIndex, final String description) {
        CheckUtils.checkArgument(portIndex >= 0, "Invalid port index must be >= 0: %d", portIndex);
        CheckUtils.checkArgument(columnIndex >= 0, "Invalid column index must be >= 0: %d", columnIndex);
        CheckUtils.checkArgument(rowIndex >= 0, "Row index >=0: %d", rowIndex);
        return new RowIssue(portIndex, columnIndex, rowIndex, description);
    }

    static RowIssue create(final int portIndex, final long rowIndex, final String description) {
        CheckUtils.checkArgument(portIndex >= 0, "Invalid port index must be >= 0: %d", portIndex);
        CheckUtils.checkArgument(rowIndex >= 0, "Row index >=0: %d", rowIndex);
        return new RowIssue(portIndex, UNKNOWN, rowIndex, description);
    }

    private RowIssue(final int portIndex, final int columnIndex, final long rowIndex, final String description) {
        m_portIndex = portIndex;
        m_columnIndex = columnIndex;
        m_rowIndex = rowIndex;
        m_description = description;
    }

    /**
     * Attempts to convert this issue to a {@link DefaultIssue} by iterating over the corresponding table and
     * including the previous table rows into the issue context. This may be skipped in case the table is too large or
     * doesn't allow random access (fast table vs. old style KNIME tables), in which case only the message is returned.
     *
     * @param inputs The original input data to a node.
     * @return a representation of this as {@link DefaultIssue}, not null.
     */
    DefaultIssue toDefaultIssue(final PortObject[] inputs) {
        if (m_columnIndex == UNKNOWN) {
            return new DefaultIssue(m_description);
        }
        BufferedDataTable table = (BufferedDataTable)inputs[m_portIndex];
        boolean hasColumnsBefore = m_columnIndex > 1;
        final DataTableSpec spec = table.getSpec();
        final int numColumns = spec.getNumColumns();
        boolean hasColumnsAfter = m_columnIndex < numColumns - 2;
        List<Integer> colIndicesList = new ArrayList<>();
        var underlineColumn = 0;
        if (m_columnIndex > 0) {
            colIndicesList.add(m_columnIndex - 1);
            underlineColumn += 1;
        }
        colIndicesList.add(m_columnIndex);
        if (m_columnIndex < numColumns - 1) {
            colIndicesList.add(m_columnIndex + 1);
        }
        int[] colIndices = colIndicesList.stream().mapToInt(Integer::intValue).toArray();
        List<String> colHeaders = new ArrayList<>();
        if (hasColumnsBefore) {
            colHeaders.add("..");
            underlineColumn += 1;
        }
        Arrays.stream(colIndices) //
            .mapToObj(spec::getColumnSpec) //
            .map(DataColumnSpec::getName) //
            .collect(Collectors.toCollection(() -> colHeaders));

        final var start = Math.max(0, m_rowIndex - 2);
        final var end = (int)Math.min(m_rowIndex, table.size() - 1);
        var iterable = jumpToIf(table, start, end, colIndices).orElse(null);
        if (iterable == null) {
            return new DefaultIssue(m_description);
        }
        List<String> rowHeaders = new ArrayList<>();
        List<List<String>> data = new ArrayList<>();
        try (var iterator = iterable.iterator()) {
            while (iterator.hasNext()) {
                var row = iterator.next();
                final List<String> datalist = new ArrayList<>();
                rowHeaders.add(row.getKey().getString());
                if (hasColumnsBefore) {
                    datalist.add("..");
                }
                Arrays.stream(colIndices) //
                    .mapToObj(row::getCell) //
                    .map(Object::toString) //
                    .collect(Collectors.toCollection(() -> datalist));

                if (hasColumnsAfter) {
                    datalist.add("..");
                }
                data.add(datalist);
            }
            var tab = SimpleTabular.forCellInLastRow(colHeaders, rowHeaders, data, underlineColumn, m_description);
            return new DefaultIssue(tab.toAsciiString());
        }
    }

    @Override
    public String toPreformatted() {
        return StringUtils.defaultString(m_description, "");
    }

    @Override
    public Type getType() {
        return Type.TABLE_ROW;
    }

    @Override
    public void saveTo(final ConfigBaseWO config) {
        config.addInt(CFG_PORT_INDEX, m_portIndex);
        config.addInt(CFG_COLUMN_INDEX, m_columnIndex);
        config.addLong(CFG_ROW_INDEX, m_rowIndex);
        config.addString(CFG_MESSAGE, m_description);
    }

    static RowIssue load(final ConfigBaseRO config) throws InvalidSettingsException {
        var portIndex = config.getInt(CFG_PORT_INDEX);
        var columnIndex = config.getInt(CFG_COLUMN_INDEX);
        var rowIndex = config.getLong(CFG_ROW_INDEX);
        var message = config.getString(CFG_MESSAGE);
        return new RowIssue(portIndex, columnIndex, rowIndex, message);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RowIssue)) {
            return false;
        }
        var m = (RowIssue)obj;
        return new EqualsBuilder() //
            .append(m_portIndex, m.m_portIndex) //
            .append(m_columnIndex, m.m_columnIndex) //
            .append(m_rowIndex, m.m_rowIndex) //
            .append(m_description, m.m_description) //
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder() //
            .append(m_portIndex) //
            .append(m_columnIndex) //
            .append(m_rowIndex) //
            .append(m_description) //
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this) //
            .append(CFG_PORT_INDEX, m_portIndex) //
            .append(CFG_COLUMN_INDEX, m_columnIndex) //
            .append(CFG_ROW_INDEX, m_rowIndex) //
            .append(CFG_MESSAGE, m_description) //
            .toString();
    }

    private static Optional<CloseableDataRowIterable> jumpToIf(final BufferedDataTable table,
        final long rowStart, final long rowEnd, final int... columns) {
        if (rowStart < 1000
                // using columnar backend (supporting almost random access to rows)
            || !(WorkflowTableBackendSettings.getTableBackendForCurrentContext() instanceof BufferedTableBackend)) {
            final var filter = new TableFilter.Builder() //
                .withFromRowIndex(rowStart) //
                .withToRowIndex(rowEnd) //
                .withMaterializeColumnIndices(columns).build();
            return Optional.of(table.filter(filter));
        }
        return Optional.empty();
    }
}
