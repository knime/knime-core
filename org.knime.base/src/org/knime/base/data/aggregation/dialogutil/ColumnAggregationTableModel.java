/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * History
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.data.aggregation.dialogutil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.table.DefaultTableModel;
import org.knime.base.data.aggregation.NamedAggregationOperator;


/**
 * This {@link DefaultTableModel} holds all aggregation columns and their
 * aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.6
 */
public class ColumnAggregationTableModel
    extends AbstractAggregationTableModel<NamedAggregationOperator> {

    private static final long serialVersionUID = 1;

    /**Constructor for class ColumnAggregatorTableModel.
     *
     */
    protected ColumnAggregationTableModel() {
        super(new String[] {"Column name (double click to change) ",
                "Aggregation method"}, new Class[] {
                NamedAggregationOperator.class, String.class}, true);
    }

    /**
     * @return {@link Set} with all method names
     */
    private Set<String> getOperatorNames() {
        final List<NamedAggregationOperator> operators = getRows();
        final Set<String> methodNames = new HashSet<String>(operators.size());
        for (final NamedAggregationOperator op : operators) {
            methodNames.add(op.getName());
        }
        return methodNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object aValue, final int row,
            final int columnIdx) {
        if (aValue instanceof String) {
            final String newName =
                (String)aValue;
            assert columnIdx == 1;
            try {
                updateOperatorName(row, newName);
            } catch (final IllegalArgumentException e) {
                // this happens if the user removes the method while editing
                //its name notify swing that the table should be redrawn
                fireTableDataChanged();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEditable(final int row, final int columnIdx) {
        switch (columnIdx) {
            case 0:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param row row index to change the name for
     * @param name the new name
     */
    private void updateOperatorName(final int row, final String name) {
        final NamedAggregationOperator operator = getRow(row);
        if (operator.getName().equals(name)) {
            //the name hasn't changed
            return;
        }
        final String uniqueName = getUniqueName(name);
        operator.setName(uniqueName);
        fireTableCellUpdated(row, 0);
    }

    /**
     * @param indices of the rows to change
     */
    protected void revertOperatorNames(final int[] indices) {
        int firstRow = Integer.MAX_VALUE;
        int lastRow = -1;
        for (final int row : indices) {
            final NamedAggregationOperator operator = getRow(row);
            final String origName = operator.getColumnLabel();
            if (!origName.equals(operator.getName())) {
            if (row < firstRow) {
                firstRow = row;
            }
            if (row > lastRow) {
                lastRow = row;
            }
                //change the operator name only if we have to
                final String uniqueName = getUniqueName(origName);
                operator.setName(uniqueName);
            }
        }
        if (lastRow > -1 && firstRow < getRowCount()) {
            fireTableRowsUpdated(firstRow, lastRow);
        }
    }

    /**
     * @param name the name to make unique
     * @return the unique name
     */
    private String getUniqueName(final String name) {
        final Set<String> operatorNames = getOperatorNames();
        return getUniqueName(name, operatorNames);
    }

    /**
     * @param name the name to make unique
     * @param names the existing names to check for uniqueness
     * @return the unique name
     */
    private String getUniqueName(final String name, final Set<String> names) {
        String uniqueName = name;
        int i = 1;
        while (names.contains(uniqueName)) {
            uniqueName = name + "(" + i++ + ")";
        }
        return uniqueName;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getValueAtRow(final int row, final int columnIndex) {
        switch (columnIndex) {
            case 0:
                return getRow(row);
            case 1:
                return getRow(row).getMethodTemplate().getLabel();
            default:
                throw new IllegalStateException("Invalid column index");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final List<NamedAggregationOperator> operators) {
        final Set<String> names = getOperatorNames();
        final List<NamedAggregationOperator> uniqueOperators =
            new ArrayList<NamedAggregationOperator>(operators.size());
        for (final NamedAggregationOperator op : operators) {
            //check if the name of the operator is already used and
            //make it unique
            final String name = op.getName();
            final String uniqueName = getUniqueName(name, names);
            if (!name.equals(uniqueName)) {
                //update the operator name if it is not unique
                op.setName(uniqueName);
            }
            names.add(op.getName());
            uniqueOperators.add(op);
        }
        //add the operators also to the super class after making them unique
        super.add(uniqueOperators);
    }
}
