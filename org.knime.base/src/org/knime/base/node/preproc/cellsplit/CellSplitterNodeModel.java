/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jun 19, 2007 (ohl): created
 */
package org.knime.base.node.preproc.cellsplit;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Model of the node that splits one column into many, based on a user specified
 * delimiter.
 * 
 * @author ohl, University of Konstanz
 */
public class CellSplitterNodeModel extends NodeModel {

    private CellSplitterSettings m_settings = new CellSplitterSettings();

    /**
     * The constructor.
     */
    public CellSplitterNodeModel() {
        super(1, 1); // one data input, one data output
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        String errMsg = m_settings.getStatus(inSpecs[0]);
        if (errMsg != null) {
            throw new InvalidSettingsException(errMsg);
        }

        // obsolete as we allow only string columns in the getStatus now.

        // // warn the user if a column of other than string type is selected
        // if ((inSpecs != null) && (inSpecs[0] != null)) {
        // DataColumnSpec cSpec =
        // inSpecs[0].getColumnSpec(m_settings.getColumnName());
        // if ((cSpec != null)
        // && (!cSpec.getType().isCompatible(StringValue.class))) {
        // setWarningMessage("The selected column is not of "
        // + "type 'String'");
        // }
        // }

        // only if we don't need to guess the type we set it here.
        // Guessing is done in the execute method
        if (!m_settings.isGuessNumOfCols()) {
            try {
                m_settings =
                        CellSplitterCellFactory.createNewColumnTypes(null,
                                m_settings, null);
            } catch (CanceledExecutionException cee) {
                // can't happen
            }
        }

        DataTableSpec outSpec = null;

        if ((inSpecs[0] != null) && (!m_settings.isGuessNumOfCols())) {
            // if we are supposed to guess we don't know the num of cols here
            outSpec = createColumnRearranger(inSpecs[0]).createSpec();
        }

        return new DataTableSpec[]{outSpec};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        // sanity check. Shouldn't go off.
        String err = m_settings.getStatus(inData[0].getDataTableSpec());
        if (err != null) {
            throw new IllegalStateException(err);
        }

        m_settings =
                CellSplitterCellFactory.createNewColumnTypes(inData[0],
                        m_settings, exec.createSubExecutionContext(0.5));

        BufferedDataTable outTable =
                exec.createColumnRearrangeTable(inData[0],
                        createColumnRearranger(inData[0].getDataTableSpec()),
                        exec.createSubExecutionContext(0.5));

        return new BufferedDataTable[]{outTable};
    }

    private ColumnRearranger createColumnRearranger(
            final DataTableSpec inTableSpec) {
        ColumnRearranger c = new ColumnRearranger(inTableSpec);
        c.append(new CellSplitterCellFactory(inTableSpec, m_settings));
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings = new CellSplitterSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset today
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing worth saving
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        CellSplitterSettings s = new CellSplitterSettings(settings);
        String msg = s.getStatus(null);
        if (msg != null) {
            throw new InvalidSettingsException(msg);
        }
    }

}
