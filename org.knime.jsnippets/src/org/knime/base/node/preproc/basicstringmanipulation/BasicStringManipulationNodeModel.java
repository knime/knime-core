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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.basicstringmanipulation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * Base class for very simple string manipulation nodes that only
 * require overwrite of one method.
 * 
 * @author M. Berthold
 */
public abstract class BasicStringManipulationNodeModel extends NodeModel {

    final static String CFG_FILTERSTRING = "COL_SELECTION";

    private SettingsModelFilterString m_colSettings =
        new SettingsModelFilterString(
                BasicStringManipulationNodeModel.CFG_FILTERSTRING);

    /** One in, one out). */
    public BasicStringManipulationNodeModel() {
        super(1, 1);
    }
    
    /** Overwrite to specify type of string processing.
     * 
     * @param val input value
     * @return new string
     */
    public abstract String processString(final String val);

    /** Creates column rearranger doing all the work.
     * @param spec The input spec.
     * @return The rearranger creating the output table/spec. */
    private ColumnRearranger createColumnRearranger(
            final DataTableSpec spec) throws InvalidSettingsException {
        List<String> includedCols = m_colSettings.getIncludeList();
        ColumnRearranger result = new ColumnRearranger(spec);
        for (String colName : includedCols) {
            final int colIndex = spec.findColumnIndex(colName);
            if (colIndex < 0) {
                throw new InvalidSettingsException(
                        "No such column: " + colName);
            }
            DataColumnSpec outColSpec =
                new DataColumnSpecCreator(colName, StringCell.TYPE).createSpec();
            SingleCellFactory fac = new SingleCellFactory(outColSpec) {
                /** {@inheritDoc} */
                @Override
                public DataCell getCell(final DataRow row) {
                    DataCell cell = row.getCell(colIndex);
                    if (cell.isMissing()) {
                        return cell;
                    }
                    String val = ((StringValue)cell).toString();
                    String newVal = processString(val);
                    return new StringCell(newVal);
                }
            };
            result.replace(fac, colIndex);
            break;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[] {rearranger.createSpec()};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = inData[0].getDataTableSpec();
        ColumnRearranger rearranger = createColumnRearranger(spec);
        BufferedDataTable out = exec.createColumnRearrangeTable(
                inData[0], rearranger, exec);
        return new BufferedDataTable[] {out};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSettings.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_colSettings.validateSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_colSettings.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

}
