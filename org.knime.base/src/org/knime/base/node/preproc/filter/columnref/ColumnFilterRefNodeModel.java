/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   06.05.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.columnref;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnFilterRefNodeModel extends NodeModel {
    
    private final SettingsModelBoolean m_excudeColumns = 
        ColumnFilterRefNodeDialogPane.createCheckBoxModel();
    
    /**
     * 
     */
    public ColumnFilterRefNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{
                createRearranger(inSpecs[0], inSpecs[1]).createSpec()};
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger cr = 
            createRearranger(inData[0].getSpec(), inData[1].getSpec());
        BufferedDataTable out = 
            exec.createColumnRearrangeTable(inData[0], cr, exec);
        return new BufferedDataTable[]{out};
    }
    
    private ColumnRearranger createRearranger(final DataTableSpec oSpec,
            final DataTableSpec filterSpec) {
        ColumnRearranger cr = new ColumnRearranger(oSpec);
        for (DataColumnSpec cspec : oSpec) {
            String name = cspec.getName();
            if (m_excudeColumns.getBooleanValue()) {
                if (filterSpec.containsName(name)) {
                    cr.remove(name);
                }
            } else {
                if (!filterSpec.containsName(name)) {
                    cr.remove(name);
                }
            }
        }
        return cr;
        
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc} 
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_excudeColumns.loadSettingsFrom(settings);
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
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc} 
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_excudeColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_excudeColumns.validateSettings(settings);
    }

}
