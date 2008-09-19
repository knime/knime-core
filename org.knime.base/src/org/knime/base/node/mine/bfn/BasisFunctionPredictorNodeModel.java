/*
 * --------------------------------------------------------------------- *
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
 */
package org.knime.base.node.mine.bfn;

import java.io.File;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
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
 * The basis function predictor model performing a prediction on the data from
 * the first input and the radial basisfunction model from the second.
 * 
 * @see BasisFunctionPredictorCellFactory
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionPredictorNodeModel extends NodeModel {
    
    private String m_applyColumn = "Winner";

    private double m_dontKnow = -1.0;
    
    private boolean m_ignoreDontKnow = false;
    
    private boolean m_appendClassProps = true;
    
    /**
     * Creates a new basisfunction predictor model with two inputs, the first
     * one which contains the data and the second with the model.
     * @param model type of the basisfunction model at the in-port
     */
    protected BasisFunctionPredictorNodeModel(final PortType model) {
        super(new PortType[]{model, BufferedDataTable.TYPE},
              new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] execute(final PortObject[] portObj,
            final ExecutionContext exec) 
            throws CanceledExecutionException, InvalidSettingsException {
        BasisFunctionPortObject pred = (BasisFunctionPortObject) portObj[0];
        final DataTableSpec modelSpec = pred.getSpec();
        final BufferedDataTable data = (BufferedDataTable) portObj[1];
        final DataTableSpec dataSpec = data.getDataTableSpec();
        final ColumnRearranger colreg = new ColumnRearranger(dataSpec);
        colreg.append(new BasisFunctionPredictorCellFactory(
                dataSpec, modelSpec, pred.getBasisFunctions(), 
                m_applyColumn, m_dontKnow, normalizeClassification(),
                m_appendClassProps));
       return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                data, colreg, exec)};
    }
    
    /**
     * @return <code>true</code> if normalization is required for output
     */
    public abstract boolean normalizeClassification();
    
    /**
     * @return the column name contained the winner prediction
     */
    public String getApplyColumn() {
        return m_applyColumn;
    }
    
    /**
     * @return the <i>don't know</i> class probability between 0.0 and 1.0
     */
    public double getDontKnowClassDegree() {
        return m_dontKnow;
    }
    
    /**
     * @return true if class probability columns should be appended
     */
    public boolean appendClassProbabilities() {
        return m_appendClassProps;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec[] configure(final PortObjectSpec[] portObjSpec)
            throws InvalidSettingsException {
        // get model spec
        final DataTableSpec modelSpec = (DataTableSpec) portObjSpec[0];
        // get data spec
        final DataTableSpec dataSpec = (DataTableSpec) portObjSpec[1];
        
        final ColumnRearranger colreg = createRearranger(dataSpec, modelSpec);
        colreg.append(new BasisFunctionPredictorCellFactory(
                modelSpec, m_applyColumn, m_appendClassProps));
        return new DataTableSpec[]{colreg.createSpec()};
    }
    
    /**
     * Creates a column rearranger based on the data spec. The new apply column
     * is appended.
     * @param dataSpec data spec
     * @param modelSpec model spec
     * @return column rearranger from data spec
     * @throws InvalidSettingsException if the settings are not valid against
     *      data and/or model spec
     */
    public final ColumnRearranger createRearranger(final DataTableSpec dataSpec,
            final DataTableSpec modelSpec) throws InvalidSettingsException {
        if (modelSpec.getNumColumns() == 0) {
            throw new InvalidSettingsException("Model spec must not be empty.");
        }
        // all model columns need to be in the data spec
        for (int i = 0; i < modelSpec.getNumColumns() - 5; i++) {
            DataColumnSpec cspec = modelSpec.getColumnSpec(i);
            int idx = dataSpec.findColumnIndex(cspec.getName());
            if (idx >= 0) {
                DataType dataType = dataSpec.getColumnSpec(idx).getType();
                if (!dataType.isCompatible(DoubleValue.class)) {
                    throw new InvalidSettingsException("Data column \""
                        + dataSpec.getColumnSpec(idx).getName() + "\"" 
                        + " is not compatible with DoubleValue.");
                }
            } else {
                throw new InvalidSettingsException("Model column \""
                        + cspec.getName() + "\" not in data spec.");
            }
        }
        m_applyColumn = DataTableSpec.getUniqueColumnName(
                dataSpec, m_applyColumn);
        return new ColumnRearranger(dataSpec);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // prediction column name
        m_applyColumn = settings
                .getString(BasisFunctionPredictorNodeDialog.APPLY_COLUMN);
        // don't know class
        m_dontKnow = settings
                .getDouble(BasisFunctionPredictorNodeDialog.DONT_KNOW_PROP);
        m_ignoreDontKnow = settings.getBoolean(
                BasisFunctionPredictorNodeDialog.CFG_DONT_KNOW_IGNORE, false);
        // append class probability columns
        m_appendClassProps = settings.getBoolean(
                BasisFunctionPredictorNodeDialog.CFG_CLASS_PROPS, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        // prediction column name
        settings.addString(BasisFunctionPredictorNodeDialog.APPLY_COLUMN,
                m_applyColumn);
        // don't know class
        settings.addDouble(BasisFunctionPredictorNodeDialog.DONT_KNOW_PROP,
                m_dontKnow);
        settings.addBoolean(
                BasisFunctionPredictorNodeDialog.CFG_DONT_KNOW_IGNORE, 
                m_ignoreDontKnow);
        // append class probability columns
        settings.addBoolean(BasisFunctionPredictorNodeDialog.CFG_CLASS_PROPS,
                m_appendClassProps);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        StringBuffer sb = new StringBuffer();
        // prediction column name
        String s = null;
        try {
            s = settings.getString(
                    BasisFunctionPredictorNodeDialog.APPLY_COLUMN);
        } catch (InvalidSettingsException ise) {
            sb.append(ise.getMessage() + "\n");
        }
        if (s == null || s.length() == 0) {
            sb.append("Empty prediction column name not allowed.\n");
        }
        // don't know class
        try {
            settings.getDouble(BasisFunctionPredictorNodeDialog.DONT_KNOW_PROP);
        } catch (InvalidSettingsException ise) {
            sb.append(ise.getMessage());
        }

        if (sb.length() > 0) {
            throw new InvalidSettingsException(sb.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadInternals(final File internDir,
            final ExecutionMonitor exec) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveInternals(final File internDir,
            final ExecutionMonitor exec) {

    }
}
