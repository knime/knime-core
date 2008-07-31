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
 * ---------------------------------------------------------------------
 * 
 * History
 *   01.10.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.predictor;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.svm.learner.SVMLearnerNodeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.config.ConfigRO;

/**
 * NodeModel of the SVM Predictor Node.
 * @author cebron, University of Konstanz
 */
public class SVMPredictorNodeModel extends NodeModel {

     /*
     * DataTableSpec of the training data.
     */
    private DataTableSpec m_trainingSpec;

    /*
     * The class column of the training table.
     */
    private String m_classcol;

    /*
     * The extracted Support Vector Machines.
     */
    private Svm[] m_svms;

    /*
     * Column indices to use.
     */
    private int[] m_colindices;

    /**
     * Constructor, one model and data input, one (classified) data output.
     */
    public SVMPredictorNodeModel() {
        super(1, 1, 1, 0);
    }

    /*
     * Extracts the model parameters from m_predParams.
     */
    private void extractModelParams(final ModelContentRO predParams)
            throws InvalidSettingsException {
        Integer count = predParams.getInt(SVMLearnerNodeModel.KEY_CATEG_COUNT);
        m_svms = new Svm[count.intValue()];
        for (int i = 0; i < m_svms.length; ++i) {
            m_svms[i] = new Svm(predParams, new Integer(i).toString() + "SVM");
        }
        ConfigRO specconf = predParams.getConfig(SVMLearnerNodeModel.KEY_SPEC);
        m_trainingSpec = DataTableSpec.load(specconf);
        m_classcol = predParams.getString(SVMLearnerNodeModel.KEY_CLASSCOL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_svms != null && m_trainingSpec != null && m_classcol != null) {
            DataTableSpec testspec = inSpecs[0];
            // try to find all columns (except the class column)
            Vector<Integer> colindices = new Vector<Integer>();
            for (DataColumnSpec colspec : m_trainingSpec) {
                if (!colspec.getName().equals(m_classcol)) {
                    int colindex = testspec.findColumnIndex(colspec.getName());
                    if (colindex < 0) {
                        throw new InvalidSettingsException("Column " + "\'"
                                + colspec.getName() + "\' not found"
                                + " in test data");
                    }
                    colindices.add(colindex);
                }
            }
            m_colindices = new int[colindices.size()];
            for (int i = 0; i < m_colindices.length; i++) {
                m_colindices[i] = colindices.get(i);
            }
            SVMPredictor svmpredict = new SVMPredictor(m_svms, m_colindices);
            ColumnRearranger colre = new ColumnRearranger(testspec);
            colre.append(svmpredict);
            return new DataTableSpec[]{colre.createSpec()};
        }
        throw new InvalidSettingsException("Model content not available.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (m_svms == null) {
            throw new Exception("Model content not available!");
        }
        SVMPredictor svmpredict = new SVMPredictor(m_svms, m_colindices);
        ColumnRearranger colre =
                new ColumnRearranger(inData[0].getDataTableSpec());
        colre.append(svmpredict);
        BufferedDataTable result =
                exec.createColumnRearrangeTable(inData[0], colre, exec);
        return new BufferedDataTable[]{result};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
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
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        if (index == 0) {
            if (predParams == null) {
                // reset 
                m_svms = null;
                m_classcol = null;
                m_trainingSpec = null;
            } else {
                extractModelParams(predParams);
            }
            
        }
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
