/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Oct 17, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.normalize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.normalize.AffineTransTable;
import org.knime.base.data.normalize.Normalizer;
import org.knime.base.data.normalize.NormalizerPortObject;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class NormalizerApplyNodeModel extends NodeModel {

    /**
     * Constructor.
     */
    public NormalizerApplyNodeModel() {
        super(new PortType[]{NormalizerPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec modelSpec = (DataTableSpec)inSpecs[0];
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        List<String> unknownCols = new ArrayList<String>();
        List<String> knownCols = new ArrayList<String>(); 
        for (DataColumnSpec c : modelSpec) {
            DataColumnSpec inDataCol = dataSpec.getColumnSpec(c.getName());
            if (inDataCol == null) {
                unknownCols.add(c.getName());
            } else if (!inDataCol.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column \"" + c.getName() 
                        + "\" is to be normalized, but is not numeric");
            } else {
                knownCols.add(c.getName());
            }
        }
        if (!unknownCols.isEmpty()) {
            setWarningMessage("Some column(s) as specified by the model is not "
                    + "present in the data: " + unknownCols);
        }
        String[] ar = knownCols.toArray(new String[knownCols.size()]);
        DataTableSpec s = Normalizer.generateNewSpec(dataSpec, ar);
        return new DataTableSpec[]{s};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        NormalizerPortObject model = (NormalizerPortObject)inData[0];
        BufferedDataTable table = (BufferedDataTable)inData[1];
        AffineTransTable t = new AffineTransTable(
                table, model.getConfiguration());
        BufferedDataTable bdt = exec.createBufferedDataTable(t, exec);
        if (t.getErrorMessage() != null) {
            setWarningMessage(t.getErrorMessage());
        }
        return new BufferedDataTable[]{bdt};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec) 
    throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
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
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

}
