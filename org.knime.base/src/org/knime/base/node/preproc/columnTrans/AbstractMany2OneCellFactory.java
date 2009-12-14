/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   14.05.2007 (Fabian Dill): created
 */
package org.knime.base.node.preproc.columnTrans;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractMany2OneCellFactory implements CellFactory {
    
    private String m_appendedColumnName;
    private int[] m_includedColsIndices;
    private Set<DataCell> m_columnNames;
    private DataTableSpec m_inputSpec;
    
    /**
     * 
     * @param inputSpec input spec of the whole table
     * @param appendedColumnName name of the new column
     * @param includedColsIndices indices of columns to condense
     */
    public AbstractMany2OneCellFactory(final DataTableSpec inputSpec,
            final String appendedColumnName, final int[]includedColsIndices) {
        m_inputSpec = inputSpec;
        m_appendedColumnName = appendedColumnName;
        m_includedColsIndices = includedColsIndices;
        m_columnNames = new HashSet<DataCell>();
        for (int i : m_includedColsIndices) {
            m_columnNames.add(new StringCell(
                    m_inputSpec.getColumnSpec(i).getName()));
        }
    }
    
    
    /**
     * 
     * @return name of the appended column
     */
    public String getAppendedColumnName() {
        return m_appendedColumnName;
    }
    
    
    /**
     * 
     * @return the indices of the condensed columns
     */
    public int[] getIncludedColIndices() {
        return m_includedColsIndices;
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        // new column
        DataColumnSpecCreator appendedColumnCreator 
            = new DataColumnSpecCreator(m_appendedColumnName, StringCell.TYPE);
        // possible values depend on allow multi occurences
            DataColumnDomainCreator possibleValuesCreator 
                = new DataColumnDomainCreator(m_columnNames);
            appendedColumnCreator.setDomain(
                    possibleValuesCreator.createDomain());
        return new DataColumnSpec[] {appendedColumnCreator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, 
            final int rowCount, final RowKey lastKey,
            final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount); 
    }
    
    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        // find matching values
        int matchingValue = findColumnIndex(row);
        DataCell newCell;
        if (matchingValue == -1) {
            newCell = DataType.getMissingCell();
        } else {
            newCell = new StringCell(
                    m_inputSpec.getColumnSpec(matchingValue).getName());
        }
        return new DataCell[] {newCell};
    }

    /**
     * Find the column names to put in the condensed column.
     * 
     * @param row row to search for matching columns
     * @return matching  column names as StringCell array
     */
    public abstract int findColumnIndex(final DataRow row);
    
}
