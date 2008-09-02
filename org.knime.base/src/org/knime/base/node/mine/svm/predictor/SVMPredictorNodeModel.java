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

import org.knime.base.node.mine.svm.PMMLSVMPortObject;
import org.knime.base.node.mine.svm.Svm;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * NodeModel of the SVM Predictor Node.
 * @author cebron, University of Konstanz
 */
public class SVMPredictorNodeModel extends GenericNodeModel {

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
        super(new PortType[] {PMMLSVMPortObject.TYPE, BufferedDataTable.TYPE },
                new PortType[] {
                BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
            DataTableSpec testSpec = (DataTableSpec) inSpecs[1];
            PMMLPortObjectSpec trainingSpec = (PMMLPortObjectSpec) inSpecs[0];
            // try to find all columns (except the class column)
            Vector<Integer> colindices = new Vector<Integer>();
            for (DataColumnSpec colspec : trainingSpec.getLearningCols()) {
                if (colspec.getType().isCompatible(DoubleValue.class)) {
                    int colindex = testSpec.findColumnIndex(colspec.getName());
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
            ColumnRearranger colre = new ColumnRearranger(testSpec);
            colre.append(svmpredict);
            return new DataTableSpec[]{colre.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        m_svms = ((PMMLSVMPortObject)inData[0]).getSvms();
        DataTableSpec testSpec =
                ((BufferedDataTable)inData[1]).getDataTableSpec();
        DataTableSpec trainingSpec =
                ((PMMLPortObject)inData[0]).getSpec().getDataTableSpec();
        // try to find all columns (except the class column)
        Vector<Integer> colindices = new Vector<Integer>();
        for (DataColumnSpec colspec : trainingSpec) {
            if (colspec.getType().isCompatible(DoubleValue.class)) {
                int colindex = testSpec.findColumnIndex(colspec.getName());
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
        BufferedDataTable testData = (BufferedDataTable) inData[1];
        ColumnRearranger colre =
                new ColumnRearranger(testData.getDataTableSpec());
        colre.append(svmpredict);
        BufferedDataTable result =
                exec.createColumnRearrangeTable(testData, colre, exec);
        return new BufferedDataTable[]{result};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        //
    }

}
