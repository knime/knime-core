/*
 * ------------------------------------------------------------------------
 *
r *  Copyright by KNIME AG, Zurich, Switzerland
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
 *   Dec 1, 2021 (konrad-amtenbrink): created
 */
package org.knime.core.webui.node.view.table.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.webui.node.view.table.TableViewViewSettings;
import org.knime.core.webui.node.view.table.data.render.DataValueImageRendererRegistry;
import org.knime.core.webui.node.view.table.data.render.DataValueRendererFactory;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class TableViewInitialDataImpl implements TableViewInitialData {

    private static Map<org.knime.core.data.DataType, DataType> coreDataTypeToDataTypeMap = new HashMap<>();

    private final TableViewViewSettings m_settings;

    private final Supplier<BufferedDataTable> m_table;

    private final TableViewDataService m_dataService;

    /**
     * @param settings
     * @param table
     * @param tableId
     * @param rendererFactory see
     *            {@link TableViewDataServiceImpl#TableViewDataServiceImpl(Supplier, String, DataValueRendererFactory, DataValueImageRendererRegistry)}
     * @param rendererRegistry see
     *            {@link TableViewDataServiceImpl#TableViewDataServiceImpl(Supplier, String, DataValueRendererFactory, DataValueImageRendererRegistry)}
     */
    public TableViewInitialDataImpl(final TableViewViewSettings settings, final Supplier<BufferedDataTable> table,
        final String tableId, final DataValueRendererFactory rendererFactory,
        final DataValueImageRendererRegistry rendererRegistry) {
        m_settings = settings;
        m_table = table;
        m_dataService = new TableViewDataServiceImpl(table, tableId, rendererFactory, rendererRegistry);
    }

    @Override
    public Table getTable() {
        final var displayedColumns = m_settings.getDisplayedColumns();
        final var pageSize = m_settings.m_enablePagination ? m_settings.m_pageSize : 0;
        return m_dataService.getTable(displayedColumns, 0, pageSize, new String[displayedColumns.length], true);
    }

    @Override
    public Map<String, DataType> getDataTypes() {
        var res = new HashMap<String, DataType>();
        for (DataColumnSpec colSpec : m_table.get().getDataTableSpec()) {
            var coreDataType = colSpec.getType();
            var dataType = coreDataTypeToDataTypeMap.computeIfAbsent(coreDataType, DataType::create);
            var dataTypeId = String.valueOf(coreDataType.hashCode());
            res.putIfAbsent(dataTypeId, dataType);
        }
        return res;
    }

    @Override
    public Map<String, String[]> getColumnDomainValues() {
        var res = new HashMap<String, String[]>();
        for (var colSpec : m_table.get().getDataTableSpec()) {
            final var rawDomainValues = colSpec.getDomain().getValues();
            if (rawDomainValues == null) {
                res.put(colSpec.getName(), null);
            } else {
                res.put(colSpec.getName(), rawDomainValues.stream().map(DataCell::toString).toArray(String[]::new));
            }
        }
        return res;
    }

    @Override
    public TableViewViewSettings getSettings() {
        return m_settings;
    }

    @Override
    public long getColumnCount() {
        return m_table.get().getDataTableSpec().getNumColumns();
    }
}
