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
 * History
 *   Feb 1, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.rename;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * NodeModel implementation for the renaming node.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RenameNodeModel extends SimpleStreamableFunctionNodeModel {
    /**
     * Config identifier for the NodeSettings object contained in the NodeSettings which contains the settings.
     */
    public static final String CFG_SUB_CONFIG = "all_columns";

    /** contains settings for each individual column. */
    private RenameConfiguration m_config;


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            final NodeSettingsWO subSettings = settings.addNodeSettings(CFG_SUB_CONFIG);
            m_config.save(subSettings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RenameConfiguration(settings.getNodeSettings(CFG_SUB_CONFIG));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config = new RenameConfiguration(settings.getNodeSettings(CFG_SUB_CONFIG));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable in = inData[0];
        DataTableSpec inSpec = in.getDataTableSpec();
        final DataTableSpec outSpec = configure(new DataTableSpec[]{inSpec})[0];

        // a data table wrapper that returns the "right" DTS (no iteration)
        BufferedDataTable out = exec.createSpecReplacerTable(in, outSpec);

        //create replace columns if a column type has changed (toString)
        ColumnRearranger colre = createColumnRearranger(inSpec, outSpec);
        if(colre!=null) {
            out = exec.createColumnRearrangeTable(out, colre, exec);
        }
        return new BufferedDataTable[]{out};
    }

    /**
     * @return the colrearranger that replaces the respective columns, or <code>null</code> if no column has to be replaced
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final DataTableSpec outSpec) {
        // indices of columns where to use the toString method
        // (for those columns it is not sufficient to just change the column
        // type, we do need to change the cells, too)
        ArrayList<Integer> toStringColumnsIndex = new ArrayList<Integer>();
        ArrayList<DataColumnSpec> toStringColumns = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataType oldType = inSpec.getColumnSpec(i).getType();
            DataType newType = outSpec.getColumnSpec(i).getType();
            // using toString iff new type is String_Type and the old type
            // is not a string type compatible
            boolean useToStringMethod = newType.equals(StringCell.TYPE) && !StringCell.TYPE.isASuperTypeOf(oldType);
            if (useToStringMethod) {
                toStringColumnsIndex.add(i);
                toStringColumns.add(outSpec.getColumnSpec(i));
            }
        }

        // depending on whether we have to change some of the columns, we
        // create a ReplacedColumnsTable
        if (!toStringColumnsIndex.isEmpty()) {
            DataColumnSpec[] changedColumns = toStringColumns.toArray(new DataColumnSpec[0]);
            int[] changedColumnsIndex = new int[toStringColumnsIndex.size()];
            for (int i = 0; i < changedColumnsIndex.length; i++) {
                changedColumnsIndex[i] = toStringColumnsIndex.get(i);
            }
            ToStringCellsFactory cellsFactory = new ToStringCellsFactory(changedColumns, changedColumnsIndex);
            ColumnRearranger rearranger = new ColumnRearranger(outSpec);
            rearranger.replace(cellsFactory, changedColumnsIndex);
            return rearranger;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) throws InvalidSettingsException {
        DataTableSpec outSpec = configure(new DataTableSpec[]{inSpec})[0];
        ColumnRearranger colre = createColumnRearranger(inSpec, outSpec);
        if (colre == null) {
            colre = new ColumnRearranger(outSpec);
        }
        return colre;
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
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        if (m_config == null) {
            throw new InvalidSettingsException("No configuration available");
        }
        final DataTableSpec resultSpec = m_config.getNewSpec(inSpec);
        final List<String> missingColumnNames = m_config.getMissingColumnNames();
        if (missingColumnNames != null && !missingColumnNames.isEmpty()) {
            setWarningMessage("The following columns are configured but no longer exist: "
                + ConvenienceMethods.getShortStringFrom(missingColumnNames, 5));
        }
        return new DataTableSpec[]{resultSpec};
    }

    /**
     * Helper class being used to use cell's toString() method to return StringCells.
     */
    private static class ToStringCellsFactory implements CellFactory {
        private final DataColumnSpec[] m_returnSpecs;

        private final int[] m_columns;

        /**
         * Create a new factory.
         *
         * @param returnSpecs the new, replacement spec
         * @param columns column indices to replace
         */
        public ToStringCellsFactory(final DataColumnSpec[] returnSpecs, final int[] columns) {
            m_returnSpecs = returnSpecs;
            m_columns = columns;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataColumnSpec[] getColumnSpecs() {
            return m_returnSpecs;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
            final ExecutionMonitor exec) {
            exec.setProgress(curRowNr / (double)rowCount, "Changed row " + curRowNr + "/" + rowCount + " (\"" + lastKey
                + "\")");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] getCells(final DataRow row) {
            DataCell[] result = new DataCell[m_columns.length];
            for (int i = 0; i < result.length; i++) {
                DataCell input = row.getCell(m_columns[i]);
                if (input.isMissing()) {
                    result[i] = DataType.getMissingCell();
                } else {
                    String s = input.toString();
                    result[i] = new StringCell(s);
                }
            }
            return result;
        }
    }
}
