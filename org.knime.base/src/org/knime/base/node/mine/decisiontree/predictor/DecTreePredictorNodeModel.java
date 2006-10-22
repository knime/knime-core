/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   27.07.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree.predictor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.knime.base.node.mine.decisiontree.predictor.decisiontree.DecisionTree;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
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
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class DecTreePredictorNodeModel extends NodeModel {
    /** Index of input data port. */
    public static final int INDATAPORT = 0;

    /** Index of input model (=decision tree) port. */
    public static final int INMODELPORT = 1;

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DecTreePredictorNodeModel.class);

    /** XML tag name in configuration file for max num of covered pattern. */
    public static final String MAXCOVERED = "UseGainRatio";

    private int m_maxNumCoveredPattern = 10000;

    private DecisionTree m_decTree;

    /**
     * Default constructor.
     */
    protected DecTreePredictorNodeModel() {
        super(1, 1, 1, 0);
        m_decTree = null;
    }

    /**
     * @return internal tree structure or <code>null</code> if it does not
     *         exist
     */
    protected DecisionTree getDecisionTree() {
        return m_decTree;
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(DecTreePredictorNodeModel.MAXCOVERED,
                m_maxNumCoveredPattern);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_maxNumCoveredPattern = settings
                    .getInt(DecTreePredictorNodeModel.MAXCOVERED);
        } catch (InvalidSettingsException ise) {
            m_maxNumCoveredPattern = 10000;
        }
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        assert m_decTree != null;
        LOGGER.info("Decision Tree Predictor: start execution.");
        DataTableSpec outSpec = createOutTableSpec(inData[INDATAPORT]
                .getDataTableSpec());
        DataContainer outData = new DataContainer(outSpec);
        int coveredPattern = 0;
        int nrPattern = 0;
        int rowCount = 0;
        double numberRows = inData[INDATAPORT].getRowCount();
        exec.setMessage("Classifying...");
        for (DataRow thisRow : inData[INDATAPORT]) {
            DataCell cl = null;
            try {
                cl = m_decTree.classifyPattern(thisRow, inData[INDATAPORT]
                        .getDataTableSpec());
                if (coveredPattern < m_maxNumCoveredPattern) {
                    // remember this one for HiLite support
                    m_decTree.addCoveredPattern(thisRow, inData[INDATAPORT]
                            .getDataTableSpec());
                    coveredPattern++;
                } else {
                    // too many patterns for HiLite - at least remember color
                    m_decTree.addCoveredColor(thisRow, inData[INDATAPORT]
                            .getDataTableSpec());
                }
                nrPattern++;
            } catch (Exception e) {
                LOGGER.error("Decision Tree evaluation failed: "
                        + e.getMessage());
                throw e;
            }
            if (cl == null) {
                LOGGER.error("Decision Tree evaluation failed: result empty");
                throw new Exception("Decision Tree evaluation failed.");
            }
            DataCell[] newCells = new DataCell[thisRow.getNumCells() + 1];
            for (int i = 0; i < thisRow.getNumCells(); i++) {
                newCells[i] = thisRow.getCell(i);
            }
            newCells[thisRow.getNumCells()] = cl;
            outData.addRowToTable(new DefaultRow(thisRow.getKey(), newCells));

            rowCount++;
            if (rowCount % 100 == 0) {
                exec.setProgress(rowCount / numberRows, "Classifying... Row "
                        + rowCount + " of " + numberRows);
            }
            exec.checkCanceled();
        }
        if (coveredPattern < nrPattern) {
            // let the user know that we did not store all available pattern
            // for HiLiting.
            this.setWarningMessage("Tree only stored first "
                    + m_maxNumCoveredPattern + " (of " + nrPattern
                    + ") rows for HiLiting!");
        }
        outData.close();
        LOGGER.info("Decision Tree Predictor: end execution.");
        return new BufferedDataTable[]{exec.createBufferedDataTable(outData
                .getTable(), exec)};
    }

    /**
     * @see NodeModel#loadModelContent(int, ModelContentRO)
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        assert index == 0 : index;
        if (predParams == null) {
            m_decTree = null;
            LOGGER.info("Decision Tree Predictor: Nothing to load.");
            return;
        }
        LOGGER.info("Decision Tree Predictor: Loading predictor...");
        m_decTree = new DecisionTree(predParams);
        LOGGER.info("Decision Tree Predictor: Loading predictor succesful.");
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_decTree = null;
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createOutTableSpec(inSpecs[INDATAPORT])};
    }

    private static DataTableSpec createOutTableSpec(
            final DataTableSpec inSpec) {
        DataColumnSpec newCol = new DataColumnSpecCreator(
                "Prediction (DecTree)", StringCell.TYPE).createSpec();
        DataTableSpec newColSpec = new DataTableSpec(
                new DataColumnSpec[]{newCol});
        return new DataTableSpec(inSpec, newColSpec);
    }

    private static final String INTERNALS_FILE_NAME = "DecTreeClassifier.bin";

    /**
     * Load internals.
     * 
     * @param nodeInternDir The intern node directory to load tree from.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(nodeInternDir, INTERNALS_FILE_NAME);
        if (!f.exists()) {
            m_decTree = null;
        }
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
        try {
            m_decTree = (DecisionTree)in.readObject();
        } catch (ClassNotFoundException e) {
            LOGGER.error(e);
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        } finally {
            in.close();
        }
    }

    /**
     * Save internals.
     * 
     * @param nodeInternDir The intern node directory to save table to.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(nodeInternDir, INTERNALS_FILE_NAME);
        ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(f));
        out.writeObject(m_decTree);
        out.close();
    }
}
