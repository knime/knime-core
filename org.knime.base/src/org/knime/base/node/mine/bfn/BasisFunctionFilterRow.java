/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   14.05.2007 (gabriel): created
 */
package org.knime.base.node.mine.bfn;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerTable.MissingValueReplacementFunction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.data.def.StringCell;

/**
 * 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
/**
 * Inner class to separate an data input row into a new row which are the
 * first n-1 double cells and returns the class label.
 */
final class BasisFunctionFilterRow implements DataRow {
    
    private final BasisFunctionLearnerTable m_model;
    private final DataCell[] m_data;
    private final Map<DataCell, Double> m_colNameToDegree;
    private final DataCell m_bestClass;
    private final RowKey m_key;

    /**
     * Create new basisfunction input data row with data and class columns.
     * @param model the underlying basisfunction learner
     * @param row the row to filter in data and class label
     * @param dataColumns indices of the data columns
     * @param classColumns indices of the classification columns
     * @param classColumnNames names of the target columns
     * @param missing the missing value replacement function
     */
    BasisFunctionFilterRow(final BasisFunctionLearnerTable model,
            final DataRow row, final int[] dataColumns,
            final int[] classColumns, final String[] classColumnNames,
            final MissingValueReplacementFunction missing) {
        m_model = model;
        m_key = row.getKey();
        m_colNameToDegree = 
            new LinkedHashMap<DataCell, Double>(classColumns.length);
        StringCell best = new StringCell(classColumnNames[0]);
        DataCell cell = row.getCell(classColumns[0]);
        if (classColumns.length > 1 
                && cell.getType().isCompatible(DoubleValue.class)) {
            double d = Double.NaN;
            if (!cell.isMissing()) {
                d = ((DoubleValue) cell).getDoubleValue();
                assert d >= 0 && d <= 1 : d;
                if (d > 0) {
                    m_colNameToDegree.put(best, d);
                }
            }
            for (int i = 1; i < classColumns.length; i++) {
                cell = row.getCell(classColumns[i]);
                if (!cell.isMissing()) {
                    d = ((DoubleValue) cell).getDoubleValue();
                    assert d >= 0 && d <= 1 : d;
                    if (d > 0) {
                        m_colNameToDegree.put(
                                new StringCell(classColumnNames[i]), d);
                        if (m_colNameToDegree.containsKey(best)) {
                            if (d > m_colNameToDegree.get(best)) {
                                best = new StringCell(classColumnNames[i]);
                            }
                        } else {
                            best = new StringCell(classColumnNames[i]);
                        }
                    }
                }
            }
            if (m_colNameToDegree.isEmpty()) {
                m_bestClass = DataType.getMissingCell();
                m_colNameToDegree.put(m_bestClass, 1.0);
            } else {
                m_bestClass = best;
            }
        } else {
            assert classColumns.length == 1;
            m_bestClass = row.getCell(classColumns[0]);
            m_colNameToDegree.put(m_bestClass, 1.0);
        }
        // init data array
        m_data = new DataCell[dataColumns.length];
        for (int i = 0; i < dataColumns.length; i++) {
            m_data[i] = row.getCell(dataColumns[i]);
        }
        // replace missing values
        DataCell[] newCells = new DataCell[m_data.length];
        for (int i = 0; i < newCells.length; i++) {
            if (m_data[i].isMissing()) {
                newCells[i] = missing.getMissing(this, i, m_model);
            } else {
                newCells[i] = m_data[i];
            }
        }
        // copy everything back to the data array
        for (int i = 0; i < m_data.length; i++) {
            m_data[i] = newCells[i];
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_data.length;
    }

    /**
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_key;
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        assert (index >= 0 && index < getNumCells());
        return m_data[index];
    }
    
    /**
     * @return class with maximum degree
     */
    DataCell getBestClass() {
        return m_bestClass;
    }
    
    /**
     * Matching degree of the given class label.
     * @param oClass class label
     * @return degree of fulfillment
     */
    double getMatch(final DataCell oClass) {
        if (oClass.isMissing()) {
            return m_colNameToDegree.get(m_bestClass);
        } else if (m_bestClass.isMissing()) {
            return 0.0;
        } else {
            Double d = m_colNameToDegree.get(oClass);
            return (d == null ? 0.0 : d);
        }
    }   

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }
    
}
