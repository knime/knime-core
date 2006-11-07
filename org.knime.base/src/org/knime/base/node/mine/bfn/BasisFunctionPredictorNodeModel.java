/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
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
    
    private String m_applyColumn = "BF (Predictor)";

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
     * Executes this basisfunction predictor node model with two given
     * {@link org.knime.core.data.DataTable} elements. The first one
     * contains the data and the second one the model.
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        // check input data
        assert (data != null && data.length == 1);
        assert (data[0] != null);

        DataTableSpec dataSpec = data[0].getDataTableSpec();
        ColumnRearranger colreg = new ColumnRearranger(dataSpec);
        DataColumnSpec targetSpec = new DataColumnSpecCreator(
                m_applyColumn, 
                m_modelSpec[m_modelSpec.length - 1].getType()).createSpec();
        colreg.append(new BasisFunctionPredictorCellFactory(
                dataSpec, m_modelSpec, m_bfs, targetSpec, m_dontKnow));
        
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                data[0], colreg, exec.createSubProgress(1.0))};
    }

    /**
     * @see NodeModel#loadModelContent(int, ModelContentRO)
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
                DataType type = modelInfo.getDataType(key);
                DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                        key, type);
                m_modelSpec[idx] = specCreator.createSpec();
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
     * @return the <i>don't know</i> class probability between 0.0 and 1.0
     */
    protected double getDontKnowClassDegree() {
        return m_dontKnow;
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_bfs.size() <= 0) {
            throw new InvalidSettingsException("No model found.");
        }
        if (m_modelSpec == null || m_modelSpec.length == 0) {
            throw new InvalidSettingsException("No model spec found.");
        } // first n-1 model columns need to be in the data
        for (int i = 0; i < m_modelSpec.length - 1; i++) {
            int idx = inSpecs[0].findColumnIndex(m_modelSpec[i].getName());
            if (idx >= 0) {
                DataType dataType = inSpecs[0].getColumnSpec(idx).getType();
                if (!m_modelSpec[i].getType().isASuperTypeOf(dataType)) {
                    throw new InvalidSettingsException("Model type "
                            + m_modelSpec[i].getType()
                            + " is not a super type of " + dataType);
                }
            } else {
                throw new InvalidSettingsException("Model column name "
                        + m_modelSpec[i].getName() + " not in data spec.");
            }
        }
        return new DataTableSpec[]{createSpec(inSpecs[0]).createSpec()};
    }
    
    private ColumnRearranger createSpec(final DataTableSpec oSpec) {
        ColumnRearranger colreg = new ColumnRearranger(oSpec);
        DataColumnSpec targetSpec = new DataColumnSpecCreator(
                m_applyColumn, 
                m_modelSpec[m_modelSpec.length - 1].getType()).createSpec();
        colreg.append(new BasisFunctionPredictorCellFactory(targetSpec));
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
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
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
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
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
     * @see NodeModel#validateSettings(NodeSettingsRO)
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
