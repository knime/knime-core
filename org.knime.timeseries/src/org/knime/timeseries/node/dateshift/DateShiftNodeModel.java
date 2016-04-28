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
 * ------------------------------------------------------------------------
 */
package org.knime.timeseries.node.dateshift;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * The node model for the Date/Time Shift node.
 *
 * @author Iris Adae, University Konstanz
 */
public class DateShiftNodeModel extends NodeModel {

    private DateShiftConfigure m_confObj = new DateShiftConfigure();

    /**
     * Constructor for the node model with one in and one out port.
     */
    protected DateShiftNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = ((BufferedDataTable)inData[0]).getDataTableSpec();
        // create rearranger
        ColumnRearranger rearranger = DateShiftConfigure.getTimeToValueRearranger(m_confObj, spec);
        BufferedDataTable out = exec.createColumnRearrangeTable(((BufferedDataTable)inData[0]), rearranger, exec);
        return new PortObject[]{out};
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // check for one date columns and auto configure
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];

        // if the user selected to use a column as shift, we need to check the int column is in the data.
        if (m_confObj.gettypeofshift().getStringValue().equals(DateShiftNodeDialog.CFG_COLUMN_SHIFT)) {
            String colName = m_confObj.getNumColumnModel().getStringValue();
            if (colName == null || colName.isEmpty()) {
                // Autoguessing
                for (DataColumnSpec dcs : inSpec) {
                    if (dcs.getType().isCompatible(IntValue.class)) {
                        m_confObj.getNumColumnModel().setStringValue(dcs.getName());
                        break;
                    }
                }

                colName = m_confObj.getNumColumnModel().getStringValue();
                if (colName == null || colName.isEmpty()) {
                    throw new InvalidSettingsException("No valid shift column in the data, please reconfigure!");
                }
            }
            // check for column in input spec
            if (inSpec.findColumnIndex(colName) < 0) {
                throw new InvalidSettingsException("Column " + colName
                    + " not found in input table, Please reconfigure!");
            }
        }

        // if the user selected to use a column as baseline, we need to check the date column is in the data.
        if (m_confObj.gettypeofreference().getStringValue().equals(DateShiftNodeDialog.CFG_COLUMN)) {
            String secondColName = m_confObj.getDateColumnModel().getStringValue();
            if (secondColName == null || secondColName.isEmpty()) {
                // Autoguessing
                for (DataColumnSpec dcs : inSpec) {
                    if (dcs.getType().isCompatible(DateAndTimeValue.class)) {
                        m_confObj.getDateColumnModel().setStringValue(dcs.getName());
                        break;
                    }
                }
            }


            secondColName = m_confObj.getDateColumnModel().getStringValue();
            if (secondColName == null || secondColName.isEmpty()) {
                throw new InvalidSettingsException("No valid date column in the data, please reconfigure!");
            }
            // check for date column in input spec
            if (inSpec.findColumnIndex(m_confObj.getDateColumnModel().getStringValue()) < 0) {
                throw new InvalidSettingsException("Column " + m_confObj.getDateColumnModel().getStringValue()
                    + " not found in input table, Please reconfigure!");
            }
        }

        // Get the spec from the column rearranger
        ColumnRearranger rearranger = DateShiftConfigure.getTimeToValueRearranger(m_confObj, inSpec);
        return new DataTableSpec[]{rearranger.createSpec()};
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_confObj.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_confObj.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_confObj.validateSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing
    }

}
