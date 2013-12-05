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
package org.knime.datageneration.crossjoin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of CrossJoiner.
 *
 *
 * @author Alexander Fillbrunn, Universität Konstanz
 * @author  Iris Adae, Universität Konstanz
 */
public class CrossJoinerNodeModel extends NodeModel {

    /**
     * Configuration key for the right table's duplicate column name suffix.
     */
    static final String CFG_RIGHT_SUFFIX = "rigthSuffix";

    /**
     * Default value for the right table's duplicate column name suffix.
     */
    static final String DEFAULT_RIGHT_SUFFIX = " (#1)";

    /**
     * Constructor for the node model.
     */
    protected CrossJoinerNodeModel() {
        super(2, 1);
    }

    /**
     * Creates a settings model for the suffix of duplicate column names in the right table.
     * @return the settings model
     */
    static SettingsModelString createRightColumnNameSuffixSettingsModel() {
        return new SettingsModelString(CFG_RIGHT_SUFFIX, DEFAULT_RIGHT_SUFFIX);
    }

    private SettingsModelString m_rightColumnNameSuffix = createRightColumnNameSuffixSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataContainer dc = exec.createDataContainer(createSpec(inData[0].getDataTableSpec(),
                           inData[1].getDataTableSpec()));
        int numCols = inData[0].getDataTableSpec().getNumColumns() + inData[1].getDataTableSpec().getNumColumns();

        int numOutRows = inData[0].getRowCount() * inData[1].getRowCount();
        int rowcounter = 0;
        for (DataRow left : inData[0]) {
            for (DataRow right : inData[1]) {
                DataCell[] cells = new DataCell[numCols];

                for (int i = 0; i < left.getNumCells(); i++) {
                    cells[i] = left.getCell(i);
                }
                for (int i = 0; i < right.getNumCells(); i++) {
                    cells[i + left.getNumCells()] = right.getCell(i);
                }
                String newrowkey = left.getKey().getString() + "_" + right.getKey().getString();
                dc.addRowToTable(new DefaultRow(new RowKey(newrowkey), cells));
                exec.checkCanceled();
                exec.setProgress(1d * rowcounter++ / numOutRows, "Generating Row " + newrowkey);
            }
        }
        dc.close();
        return new BufferedDataTable[]{(BufferedDataTable)dc.getTable()};
    }

    private DataTableSpec createSpec(final DataTableSpec left, final DataTableSpec right) {
        DataColumnSpec[] colSpecs = new DataColumnSpec[left.getNumColumns() + right.getNumColumns()];

        final List<String> newcolumns = new LinkedList<String>();
        for (int i = 0; i < left.getNumColumns(); i++) {
            DataColumnSpecCreator c = new DataColumnSpecCreator(left.getColumnSpec(i));
            colSpecs[i] = c.createSpec();
        }
        for (int i = 0; i < right.getNumColumns(); i++) {
            DataColumnSpec spec = right.getColumnSpec(i);
            DataColumnSpecCreator c = new DataColumnSpecCreator(spec);
            String columnname = spec.getName();
            while (left.containsName(columnname) || newcolumns.contains(columnname)) {
                do {
                    columnname += m_rightColumnNameSuffix.getStringValue();
                } while (right.containsName(columnname));
            }
            if (columnname != spec.getName()) {
                // save the new name so we don't generate it twice
                newcolumns.add(columnname);
                // set the new column name to the column spec
                c.setName(columnname);
            }
            colSpecs[i + left.getNumColumns()] = c.createSpec();
        }
        return new DataTableSpec(colSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createSpec(inSpecs[0], inSpecs[1])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_rightColumnNameSuffix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_rightColumnNameSuffix.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_rightColumnNameSuffix.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}

