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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.targetshuffling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model for the y-scrambling node. It randomly permutates the values of one column of the table.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Tim-Oliver Buchholz, University of Konstanz
 */
class TargetShufflingNodeModel extends NodeModel {
    private final TargetShufflingSettings m_settings = new TargetShufflingSettings();

    private Random m_random;

    /**
     * Creates a new model with input and one output port.
     */
    public TargetShufflingNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[0].findColumnIndex(m_settings.columnName()) == -1) {
            throw new InvalidSettingsException("Column '" + m_settings + "' does not exist in input table.");
        }

        if (m_settings.getUseSeed()) {
            m_random = new Random(m_settings.getSeed());
        } else {
            m_random = new Random();
        }

        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final int colIndex = inData[0].getDataTableSpec().findColumnIndex(m_settings.columnName());
        final String colName = inData[0].getDataTableSpec().getColumnSpec(colIndex).getName();

        // create a new column rearranger from the input table
        ColumnRearranger colRe = new ColumnRearranger(inData[0].getDataTableSpec());
        for (DataColumnSpec c : inData[0].getDataTableSpec()) {
            if (!c.getName().equals(colName)) {
                // remove all columns except the selected one
                colRe.remove(c.getName());
            }
        }

        // append a new column with a random number for each cell
        String uniqueColumnName = DataTableSpec.getUniqueColumnName(inData[0].getDataTableSpec(), "random_col");
        colRe.append(new SingleCellFactory(new DataColumnSpecCreator(uniqueColumnName, LongCell.TYPE).createSpec()) {

            @Override
            public DataCell getCell(final DataRow row) {
                return new LongCell(m_random.nextLong());
            }
        });

        BufferedDataTable toSort =
            exec.createColumnRearrangeTable(exec.createBufferedDataTable(inData[0], exec), colRe,
                exec.createSilentSubProgress(.2));


        // sort the random numbers ---> shuffles the sorted column
        List<String> include = new ArrayList<String>();
        include.add(toSort.getDataTableSpec().getColumnSpec(1).getName());
        SortedTable sort = new SortedTable(toSort, include, new boolean[]{true}, exec.createSubExecutionContext(.6));
        final BufferedDataTable sorted = sort.getBufferedDataTable();


        // replace the selected column with the shuffled one
        final DataColumnSpec colSpec = inData[0].getDataTableSpec().getColumnSpec(colIndex);
        ColumnRearranger crea = new ColumnRearranger(inData[0].getDataTableSpec());
        crea.replace(new SingleCellFactory(colSpec) {
            private final CloseableRowIterator m_iterator = sorted.iterator();

            @Override
            public DataCell getCell(final DataRow row) {
                return m_iterator.next().getCell(0);
            }
        }, colName);

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], crea, exec.createSubProgress(0.2))};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        TargetShufflingSettings s = new TargetShufflingSettings();
        s.loadSettingsFrom(settings);
        if ((s.columnName() == null) || (s.columnName().length() < 1)) {
            throw new InvalidSettingsException("No column selected");
        }
    }

}
