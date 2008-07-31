/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 *
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnFilterRefNodeModel extends NodeModel {

    /** Settings model to included or exclude columns. */
    private final SettingsModelString m_inexcudeColumns =
        ColumnFilterRefNodeDialogPane.createInExcludeModel();

    /** Settings model to check column type compatibility. */
    private final SettingsModelBoolean m_typeComp =
        ColumnFilterRefNodeDialogPane.createTypeModel();

    /**
     * Creates a new node model of the Reference Column Filter node with two
     * inputs and one output.
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

    /**
     * Creates a <code>ColumnRearranger</code> that is a filter on the input 
     * table spec.
     * @param oSpec original table spec to filter
     * @param filterSpec the reference table spec
     * @return a rearranger object that filters the original spec
     */
    private ColumnRearranger createRearranger(final DataTableSpec oSpec,
            final DataTableSpec filterSpec) {
        ColumnRearranger cr = new ColumnRearranger(oSpec);

        boolean exclude = m_inexcudeColumns.getStringValue().equals(
                ColumnFilterRefNodeDialogPane.EXCLUDE);

        for (DataColumnSpec cspec : oSpec) {
            String name = cspec.getName();
            if (exclude) {
                if (filterSpec.containsName(name)) {
                    DataType fType = filterSpec.getColumnSpec(name).getType();
                    if (!m_typeComp.getBooleanValue()
                            || cspec.getType().isASuperTypeOf(fType)) {
                        cr.remove(name);
                    }
                }
            } else {
                if (!filterSpec.containsName(name)) {
                    cr.remove(name);
                } else {
                    DataType fType = filterSpec.getColumnSpec(name).getType();
                    if (m_typeComp.getBooleanValue()
                            && !cspec.getType().isASuperTypeOf(fType)) {
                        cr.remove(name);
                    }
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
        m_inexcudeColumns.loadSettingsFrom(settings);
        m_typeComp.loadSettingsFrom(settings);
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
        m_inexcudeColumns.saveSettingsTo(settings);
        m_typeComp.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inexcudeColumns.validateSettings(settings);
        m_typeComp.validateSettings(settings);
    }

}
