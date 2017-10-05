/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 */

package org.knime.base.node.io.database.groupby.dialog.pattern;

import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;

import org.knime.base.data.aggregation.dialogutil.AbstractAggregationFunctionTableCellEditor;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;


/**
 * Extends the {@link DefaultCellEditor} class to provide the cell editor.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.11
 */
public class DBPatternAggregationFunctionRowTableCellEditor
    extends AbstractAggregationFunctionTableCellEditor<DBAggregationFunction, DBPatternAggregationFunctionRow> {

    private static final long serialVersionUID = 1L;

    /**
     * @param provider might be <code>null</code>
     */
    public DBPatternAggregationFunctionRowTableCellEditor(
        final AggregationFunctionProvider<DBAggregationFunction> provider) {
        super(provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AggregationFunction getSelectedAggregationMethod(final JTable table,
        final DBPatternAggregationFunctionRow row, final boolean isSelected, final int rowIdx, final int column) {
        return row.getFunction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DBAggregationFunction> getCompatibleMethods(final DataType type) {
        return getAggregationFunctionProvider().getFunctions(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final JTable table, final DBPatternAggregationFunctionRow value,
        final boolean isSelected, final int row, final int column) {
        return StringCell.TYPE;
    }
}
