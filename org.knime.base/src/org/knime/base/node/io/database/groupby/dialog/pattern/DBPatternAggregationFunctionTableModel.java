/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */

package org.knime.base.node.io.database.groupby.dialog.pattern;

import java.awt.Component;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.knime.base.data.aggregation.dialogutil.AbstractAggregationTableModel;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRow;
import org.knime.core.data.DataType;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;


/**
 * This {@link DefaultTableModel} holds all aggregation columns and their
 * aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class DBPatternAggregationFunctionTableModel
    extends AbstractAggregationTableModel<DBAggregationFunction, DBPatternAggregationFunctionRow> {

    private static final long serialVersionUID = 1L;
    private JPanel m_panel;

    /**
     * Constructor.
     */
    public DBPatternAggregationFunctionTableModel() {
        this(null);
    }
    /**Constructor for class AggregationColumnTableModel.
     * @param provider {@link AggregationFunctionProvider}
     */
    public DBPatternAggregationFunctionTableModel(final AggregationFunctionProvider<DBAggregationFunction> provider) {
        super(new String[] {"Search pattern (double click to change)", "RegEx", "Aggregation (click to change)"},
            new Class<?>[] {DBPatternAggregationFunctionRow.class, Boolean.class,
            DBPatternAggregationFunctionRow.class}, false, provider);
    }

    /**
     * @param type the type to check for compatibility
     * @return indices of all rows that are compatible with the given type
     * or an empty collection if none is compatible
     */
    public Collection<Integer> getCompatibleRowIdxs(final DataType type) {
        return getRowIdxs(true, type);
    }

    /**
     * @param type the type to check for compatibility
     * @return indices of all rows that are not compatible with the given type
     * or an empty collection if all are compatible
     */
    public Collection<Integer> getNotCompatibleRowIdxs(final DataType type) {
        return getRowIdxs(false, type);
    }

    private Collection<Integer> getRowIdxs(final boolean compatible, final DataType type) {
        final Collection<Integer> result = new LinkedList<>();
        for (int i = 0, length = getRowCount(); i < length; i++) {
            if ((compatible && isCompatible(i, type)) || (!compatible && !isCompatible(i, type))) {
                result.add(Integer.valueOf(i));
            }
        }
        return result;
    }

    /**
     * @param row the index of the row to check
     * @param type the type to check for compatibility
     * @return <code>true</code> if the row contains a numerical column
     */
    public boolean isCompatible(final int row, final DataType type) {
        final AggregationFunctionRow<?> colAggr = getRow(row);
        return colAggr.getFunction().isCompatible(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(final Object aValue, final int rowIdx, final int columnIdx) {
        if (aValue == null) {
            return;
        }
        if (columnIdx == 0) {
            updatePattern(rowIdx, aValue.toString());
        }
        if (columnIdx == 1) {
            updateIsRegex(rowIdx, ((Boolean)aValue).booleanValue());
        }
        if (columnIdx == 2) {
            if (aValue instanceof AggregationFunction) {
                updateFunction(rowIdx, (AggregationFunction)aValue);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getValueAtRow(final int rowIdx, final int columnIndex) {
        final DBPatternAggregationFunctionRow row = getRow(rowIdx);
        switch (columnIndex) {
            case 0:
                return row;
            case 1:
                return Boolean.valueOf(row.isRegex());
            case 2:
                return row;

            default:
                break;
        }
        return null;
    }

    /**
     * @param selectedRows the rows to update
     * @param method the {@link DBAggregationFunction} to use
     */
    protected void setAggregationFunction(final int[] selectedRows, final AggregationFunction method) {
        for (final int row : selectedRows) {
            updateFunction(row, method);
        }
    }

    private void updatePattern(final int rowIdx, final String inputPattern) {
        final DBPatternAggregationFunctionRow old = getRow(rowIdx);
        if (old.getInputPattern().equals(inputPattern)) {
            //check if the method has changed
            return;
        }
        updateRow(rowIdx, inputPattern, old.isRegex(), old.getFunction().getId(), old.isValid());
    }

    private void updateIsRegex(final int rowIdx, final boolean isRegex) {
        final DBPatternAggregationFunctionRow old = getRow(rowIdx);
        if (old.isRegex() == isRegex) {
            //check if the method has changed
            return;
        }
        updateRow(rowIdx, old.getInputPattern(), isRegex, old.getFunction().getId(), old.isValid());
    }

    private void updateFunction(final int row, final AggregationFunction function) {
        final DBPatternAggregationFunctionRow old = getRow(row);
        if (old.getFunction().getId().equals(function.getId())) {
            //check if the method has changed
            return;
        }
        updateRow(row, old.getInputPattern(), old.isRegex(), function.getId(), old.isValid());
    }

    private void updateRow(final int row, final String pattern, final boolean isRegex,
        final String functionId, final boolean isValid) {
      //create a new operator each time it is updated to guarantee that
        //each column has its own operator instance
        final DBAggregationFunction methodClone = getAggregationFunctionProvider().getFunction(functionId);
        final DBPatternAggregationFunctionRow regexAggregator =
                new DBPatternAggregationFunctionRow(pattern, isRegex, methodClone);
        if (!regexAggregator.isValid()) {
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                final Component root = SwingUtilities.getRoot(m_panel);
                JOptionPane.showMessageDialog(root, "<html><body><p>Invalid regular expression:</p><p>"
                        + pattern + "</p><p>" + e.getDescription() + " at position " + e.getIndex() + "</p>",
                    "Invalid regular expression", JOptionPane.ERROR_MESSAGE);
            }
        }
        regexAggregator.setValid(isValid);
        updateRow(row, regexAggregator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int columnIdx) {
        final DBPatternAggregationFunctionRow operator = getRow(row);
        if (!operator.isValid()) {
            //only the pattern and the is regex option are editable if the row is invalid
            return columnIdx == 0 || columnIdx == 1;
        }
        return super.isCellEditable(row, columnIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEditable(final int row, final int columnIdx) {
        switch (columnIdx) {
            case 0:
                return true;
            case 1:
                return true;
            case 2:
                return true;
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toggleMissingCellOption(final int row) {
        final AggregationFunctionRow<DBAggregationFunction> funcRow = getRow(row);
        funcRow.setInclMissingCells(!funcRow.inclMissingCells());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateInclMissing(final int row, final boolean inclMissingVals) {
        final AggregationFunctionRow<DBAggregationFunction> funcRow = getRow(row);
        funcRow.setInclMissingCells(inclMissingVals);
    }

    /**
     * @param panel the {@link JPanel} this model is used in
     */
    public void setRootPanel(final JPanel panel) {
        m_panel = panel;
    }
}
