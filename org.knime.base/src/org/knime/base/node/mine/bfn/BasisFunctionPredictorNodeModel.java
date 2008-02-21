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
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;

/**
 * The basis function predictor model performing a prediction on the data from
 * the first input and the radial basisfunction model from the second.
 * 
 * @see BasisFunctionPredictorCellFactory
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionPredictorNodeModel extends GenericNodeModel {
    
    private String m_applyColumn = "Winner";

    private double m_dontKnow = -1.0;
    
    private boolean m_ignoreDontKnow = false;
    
    /**
     * Creates a new basisfunction predictor model with two inputs, the first
     * one which contains the data and the second with the model.
     */
    protected BasisFunctionPredictorNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, 
                BasisFunctionModelContent.TYPE},
              new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] execute(final PortObject[] portObj,
            final ExecutionContext exec) 
            throws CanceledExecutionException, InvalidSettingsException {
        BasisFunctionModelContent pred = (BasisFunctionModelContent) portObj[1];
        final DataTableSpec modelSpec = pred.loadSpec();
        final BufferedDataTable data = (BufferedDataTable) portObj[0];
        final DataTableSpec dataSpec = data.getDataTableSpec();
        final ColumnRearranger colreg = new ColumnRearranger(dataSpec);
        colreg.append(new BasisFunctionPredictorCellFactory(
                dataSpec, modelSpec, pred.loadBasisFunctions(this), 
                m_applyColumn, m_dontKnow, normalizeClassification()));
        
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                data, colreg, exec)};
    }

    /**
     * Return specific predictor row for the given <code>ModelContent</code>.
     * 
     * @param pp the content the read the predictive row from
     * @return a new predictor row
     * @throws InvalidSettingsException if the rule can be read from model
     *             content
     */
    public abstract BasisFunctionPredictorRow createPredictorRow(
            ModelContentRO pp) throws InvalidSettingsException;
    
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
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec[] configure(final PortObjectSpec[] portObjSpec)
            throws InvalidSettingsException {
        // get model spec
        DataTableSpec modelSpec = (DataTableSpec) portObjSpec[1];
        if (modelSpec.getNumColumns() == 0) {
            throw new InvalidSettingsException("Model spec is empty.");
        }
        // get data spec
        final DataTableSpec dataSpec = (DataTableSpec) portObjSpec[0];
        // data model columns need to be in the data
        for (int i = 0; i < modelSpec.getNumColumns() - 5; i++) {
            DataColumnSpec cspec = modelSpec.getColumnSpec(i);
            int idx = dataSpec.findColumnIndex(cspec.getName());
            if (idx >= 0) {
                DataType dataType = dataSpec.getColumnSpec(idx).getType();
                Class<? extends DataValue> prefValue = 
                    cspec.getType().getPreferredValueClass();
                if (!dataType.isCompatible(prefValue)) {
                    throw new InvalidSettingsException("Model column '"
                            + cspec.getName() + "' of type '"
                            + cspec.getType() 
                            + "' is not a super type of '" + dataType + "'");
                }
            } else {
                throw new InvalidSettingsException("Model column name '"
                        + cspec.getName() + "' not in data spec.");
            }
        }
        DataTableSpec outSpec = createSpec(dataSpec, modelSpec).createSpec();
        return new DataTableSpec[]{outSpec};
    }
    
    private ColumnRearranger createSpec(
            final DataTableSpec dataSpec, final DataTableSpec modelSpec) {
        m_applyColumn = DataTableSpec.getUniqueColumnName(
                dataSpec, m_applyColumn);
        ColumnRearranger colreg = new ColumnRearranger(dataSpec);
        colreg.append(new BasisFunctionPredictorCellFactory(
                modelSpec, m_applyColumn));
        return colreg;
    }
    
    /**
     * Resets the translator.
     */
    @Override
    public final void reset() {

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
            s = settings
                    .getString(BasisFunctionPredictorNodeDialog.APPLY_COLUMN);
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
     * Load internals.
     * 
     * @param internDir the intern node directory
     * @param exec used to report progress or cancel saving
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    public void loadInternals(final File internDir,
            final ExecutionMonitor exec) {

    }

    /**
     * Save internals.
     * 
     * @param internDir the intern node directory
     * @param exec used to report progress or cancel saving
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    public void saveInternals(final File internDir,
            final ExecutionMonitor exec) {

    }
}
