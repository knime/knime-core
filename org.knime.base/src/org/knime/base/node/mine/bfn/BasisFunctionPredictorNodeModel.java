/*
 * --------------------------------------------------------------------- *
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
 */
package org.knime.base.node.mine.bfn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

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

    private final List<BasisFunctionPredictorRow> m_bfs = 
        new ArrayList<BasisFunctionPredictorRow>();

    private DataColumnSpec[] m_modelSpec;
    
    /**
     * Creates a new basisfunction predictor model with two inputs, the first
     * one which contains the data and the second with the model.
     */
    protected BasisFunctionPredictorNodeModel() {
        super(1, 1, 1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        // data spec
        final DataTableSpec dataSpec = data[0].getDataTableSpec();
        final ColumnRearranger colreg = new ColumnRearranger(dataSpec);
        colreg.append(new BasisFunctionPredictorCellFactory(
                dataSpec, m_modelSpec, m_bfs, m_applyColumn, m_dontKnow,
                normalizeClassification()));
        
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                data[0], colreg, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        assert index == 0;
        if (predParams != null) {
            // load rules
            ModelContentRO ruleModel = predParams.getModelContent("rules");
            for (String key : ruleModel.keySet()) {
                ModelContentRO bfParam = ruleModel.getModelContent(key);
                BasisFunctionPredictorRow bf = createPredictorRow(bfParam);
                m_bfs.add(bf);
            }
            // load model info
            ModelContentRO modelInfo = predParams.getModelContent("model_spec");
            Set<String> keySet = modelInfo.keySet();
            m_modelSpec = new DataColumnSpec[keySet.size()];
            int idx = 0;
            for (String key : keySet) {
                m_modelSpec[idx] = 
                    DataColumnSpec.load(modelInfo.getConfig(key));
                idx++;
            }
        } else {
            // reset model
            reset();
        }
    }

    /**
     * Return specific predictor row for the given <code>ModelContent</code>.
     * 
     * @param pp the content the read the predictive row from
     * @return a new predictor row
     * @throws InvalidSettingsException if the rule can be read from model
     *             content
     */
    protected abstract BasisFunctionPredictorRow createPredictorRow(
            ModelContentRO pp) throws InvalidSettingsException;
    
    /**
     * @return <code>true</code> if normalization is required for output
     */
    protected abstract boolean normalizeClassification();

    /**
     * @return the <i>don't know</i> class probability between 0.0 and 1.0
     */
    protected double getDontKnowClassDegree() {
        return m_dontKnow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_bfs.size() == 0) {
            throw new InvalidSettingsException("No rules available!");
        }
        if (m_modelSpec == null || m_modelSpec.length == 0) {
            throw new InvalidSettingsException("No model spec found.");
        }
                
        // data model columns need to be in the data
        for (int i = 0; i < m_modelSpec.length - 5; i++) {
            int idx = inSpecs[0].findColumnIndex(m_modelSpec[i].getName());
            if (idx >= 0) {
                DataType dataType = inSpecs[0].getColumnSpec(idx).getType();
                Class<? extends DataValue> prefValue = 
                    m_modelSpec[i].getType().getPreferredValueClass();
                if (!dataType.isCompatible(prefValue)) {
                    throw new InvalidSettingsException("Model type "
                            + m_modelSpec[i].getType()
                            + " is not a super type of " + dataType);
                }
            } else {
                throw new InvalidSettingsException("Model column \""
                        + m_modelSpec[i].getName() + "\" not in data spec.");
            }
        }
        return new DataTableSpec[]{createSpec(inSpecs[0]).createSpec()};
    }
    
    private ColumnRearranger createSpec(final DataTableSpec oSpec) {
        String newColumn = m_applyColumn;
        int idx = 0;
        final String dupString = "_duplicate";
        while (true) {
            // if apply column exist, "_duplicate<id>" is appended 
            if (oSpec.containsName(newColumn)) {
                newColumn = m_applyColumn + dupString;
                if (idx > 0) {
                    newColumn += idx;
                }
                idx++;
            } else {
                if (!m_applyColumn.equals(newColumn)) {
                    String msg = 
                        "The apply column name \"" + m_applyColumn 
                        + "\" has changed to \"" + newColumn
                        + "\" to avoid duplicate column names.";
                    setWarningMessage(msg);
                    NodeLogger.getLogger(
                            BasisFunctionPredictorNodeModel.class).warn(msg);
                    // set new column name
                    m_applyColumn = newColumn;
                }     
                break;
            }
        }
        ColumnRearranger colreg = new ColumnRearranger(oSpec);
        colreg.append(new BasisFunctionPredictorCellFactory(
                m_modelSpec, m_applyColumn));
        return colreg;
    }
    
    /**
     * Resets the translator.
     */
    @Override
    protected final void reset() {
        // remove list of basisfunctions
        m_bfs.clear();
        // clear model spec
        m_modelSpec = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
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
    protected void validateSettings(final NodeSettingsRO settings)
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
    protected void loadInternals(final File internDir,
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
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) {

    }
}
