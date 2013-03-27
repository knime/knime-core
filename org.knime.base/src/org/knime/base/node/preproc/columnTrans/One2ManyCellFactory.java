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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.columnTrans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.Pair;


/**
 * Maps several original nominal columns to their possible values, creates a
 * column for every possible value and when the rows are processed the value is
 * set to 1 if the original column contains this value and to 0 otherwise.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class One2ManyCellFactory implements CellFactory {

    private final Map<Integer/*colIdx*/, List<DataCell>/*possVal*/>
            m_possibleValues;

    private DataColumnSpec[] m_columnSpecs = new DataColumnSpec[0];
    /* A mapping the name of the "splitted" column to a pair of resulting 
     * column name and the value this column is based on. 
     * */
    private final Map<String, List<Pair<String, String>>> m_columnMapping
            = new LinkedHashMap<String, List<Pair<String, String>>>();


    /**
     * Creates for every possible value of one column given by the columnNames
     * an extra column with the values present(1) or absent(0).
     * @param inputSpec the input table spec.
     * @param columnNames the names of the columns to be transformed.
     * @param appendOrgColNames if true original column names will be appended
     *  to the newly generated column name: (possibleValue_originalColName)
     */
    public One2ManyCellFactory(final DataTableSpec inputSpec,
            final List<String>columnNames, final boolean appendOrgColNames) {
        m_possibleValues = new HashMap<Integer, List<DataCell>>();
        final List<DataColumnSpec>colSpecs = new ArrayList<DataColumnSpec>();
        final Set<DataCell>newDomainVals = new HashSet<DataCell>();
        newDomainVals.add(new IntCell(0));
        newDomainVals.add(new IntCell(1));
        final DataColumnDomainCreator domainCreator =
            new DataColumnDomainCreator(newDomainVals, new IntCell(0),
                    new IntCell(1));
        for (final String colName : columnNames) {
            final DataColumnSpec colSpec = inputSpec.getColumnSpec(colName);
            if (colSpec.getDomain().hasValues()) {
                m_possibleValues.put(
                        inputSpec.findColumnIndex(colName),
                        new ArrayList<DataCell>(colSpec
                                .getDomain().getValues()));
                List<Pair<String, String>> linkedCols
                        = new ArrayList<Pair<String, String>>();
                for (final DataCell newCol : colSpec.getDomain().getValues()) {
                    String newColumnName;
                    String value = newCol.toString();
                    if (appendOrgColNames) {
                        newColumnName = value + "_" + colName;
                    } else {
                        newColumnName = value;
                    }
                    final DataColumnSpecCreator creator
                        = new DataColumnSpecCreator(newColumnName,
                                IntCell.TYPE);
                    creator.setDomain(domainCreator.createDomain());
                    colSpecs.add(creator.createSpec());
                    linkedCols.add(new Pair<String, String>(newColumnName,
                            value));

                }
                m_columnMapping.put(colName, linkedCols);
            } else {
                throw new IllegalArgumentException(
                        "No possible values found for column: " + colName);
            }
        }
        m_columnSpecs = colSpecs.toArray(m_columnSpecs);
    }

    /**
     *
     * @return - the columnSpecs for the appended columns.
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_columnSpecs;
    }

    /**
     * @return a mapping of column names to pairs of their associated appended
     *      column names and values
     */
    public Map<String, List<Pair<String, String>>> getColumnMapping() {
        return m_columnMapping;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        final List<DataCell>appendedDataCells = new ArrayList<DataCell>();
        for (int col = 0; col < row.getNumCells(); col++) {
            if (m_possibleValues.containsKey(col)) {
                final List<DataCell>possVals = m_possibleValues.get(col);
                for (final DataCell value : possVals) {
                    if (row.getCell(col).equals(value)) {
                        appendedDataCells.add(new IntCell(1));
                    } else {
                        appendedDataCells.add(new IntCell(0));
                    }
                }
            }
        }
        return appendedDataCells.toArray(new DataCell[]{});
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
       exec.setProgress((double)curRowNr / (double)rowCount);
    }


}
