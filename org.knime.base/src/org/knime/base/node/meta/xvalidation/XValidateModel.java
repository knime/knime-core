/*
 * -------------------------------------------------------------------
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
 */
package org.knime.base.node.meta.xvalidation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.meta.DataInputNodeModel;
import org.knime.core.node.meta.MetaNodeModel;
import org.knime.core.node.workflow.NodeContainer;


/**
 * This model represents a cross validation node. The internal workflow must at
 * least consist of any learner and its predictor. The internal workflow has two
 * input nodes, the first provides the training data, the second the test data.
 * The output node must be feeded with the output table of the predictor which
 * must have the prediction in its last column.
 * 
 * The input into the meta node is just any data table, the output consists of a
 * simple performance value at the first port and a confusion matrix at the
 * second.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateModel extends MetaNodeModel {
    private final XValidateSettings m_settings = new XValidateSettings();

    private DataTable m_confusionMatrix;
    
    private int m_correctCount;
    private int m_falseCount;
    private int m_nrRows;

    private XValidatePartitionModel m_partitionModel;

    private NodeContainer m_partionNode;

    
    static {
        // this if for backwards compatibility with release 1.0.0
        NodeFactory.addLoadedFactory(XValidatePartitionerFactory.class);
    }
    
    /**
     * Creates a new cross validation node.
     */
    public XValidateModel() {
        super(1, 1, 0, 0);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        super.configure(inSpecs);

        if (inSpecs[0].findColumnIndex(m_settings.classColumnName()) == -1) {
            throw new InvalidSettingsException("Column with class labels"
                    + " is not set");
        }
        return new DataTableSpec[]{createOutSpec()};
    }
    
    private DataTableSpec createOutSpec() {
        return new DataTableSpec(
                new DataColumnSpecCreator(
                        "Error in %", DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator(
                        "Size of Test Set", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator(
                        "Error Count", IntCell.TYPE).createSpec());
    }

    /**
     * @see org.knime.core.node.NodeModel #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final int classColIndex = inData[0].getDataTableSpec().findColumnIndex(
                m_settings.classColumnName());
        LinkedHashSet<DataCell> allClasses = new LinkedHashSet<DataCell>();
        Map<DataCell, Integer> classToIndex = new HashMap<DataCell, Integer>();
        int classCount = 0;
        int rowCount = 0;
        for (RowIterator it = inData[0].iterator(); it.hasNext(); rowCount++) {
            DataRow row = it.next();
            if (allClasses.add(row.getCell(classColIndex))) {
                classToIndex.put(row.getCell(classColIndex), classCount++);
            }
        }
        if (allClasses.add(DataType.getMissingCell())) {
            classToIndex.put(DataType.getMissingCell(), classCount++);
        }

        int[][] confusionMatrix = new int[allClasses.size()][allClasses.size()];

        final double[] errors = new double[m_settings.validations()];
        final int[] testSize = new int[m_settings.validations()];
        final int[] errorCountTest = new int[m_settings.validations()];
        m_partitionModel.setSettings(m_settings);

        for (int i = 0; i < m_settings.validations(); i++) {
            exec.setProgress(i / (double)m_settings.validations(),
                    "Validating in iteration " + (i + 1));

            m_partitionModel.setPartitionNumber((short)i);
            if (i > 0) {
                m_partitionModel.setIgnoreNextReset(true);
            }
            resetAndConfigureInternalWF();
            executeInternalWF();
            
//            internalWFM().resetAndConfigureAll();
            m_partitionModel.setIgnoreNextReset(false);
//            KNIMEConstants.GLOBAL_THREAD_POOL.runInvisible(new Runnable() {
//                public void run() {
//                    internalWFM().executeAll(true);
//                }
//            });

            exec.checkCanceled();
            if (innerExecCanceled()) {
                throw new CanceledExecutionException("Inner workflow canceled");
            }
            if (dataOutModel(0).getBufferedDataTable() == null) {
                throw new Exception("Cross validation failed in inner node");
            }

            DataTable prediction = dataOutModel(0).getBufferedDataTable();
            for (RowIterator it = prediction.iterator(); it.hasNext();) {
                DataRow row = it.next();

                DataCell realValue = row.getCell(classColIndex);
                DataCell predictedValue = row.getCell(row.getNumCells() - 1);
                int x = classToIndex.get(realValue);
                int y = classToIndex.get(predictedValue);
                confusionMatrix[x][y]++;
                if (x != y) {
                    errorCountTest[i]++;
                }
                testSize[i]++;
            }
            errors[i] = 100.0 * errorCountTest[i] / testSize[i];
        }

        String[] keys = new String[allClasses.size()];
        int j = 0;
        for (DataCell c : allClasses) {
            keys[j++] = c.toString();
        }
        m_confusionMatrix = new DefaultTable(confusionMatrix, keys, keys);

        DataTableSpec out = createOutSpec();
        BufferedDataContainer con = exec.createDataContainer(out);
        m_nrRows = 0;
        m_correctCount = 0;
        m_falseCount = 0;
        for (int i = 0; i < m_settings.validations(); i++) {
            DataRow r = new DefaultRow(
                    new RowKey("Validation" + (i + 1)),
                    new DoubleCell(errors[i]),
                    new IntCell(testSize[i]),
                    new IntCell(errorCountTest[i]));
            con.addRowToTable(r);
            m_nrRows += testSize[i];
            m_falseCount += errorCountTest[i];
            m_correctCount += testSize[i] - errorCountTest[i];
        }
        con.close();
        return new BufferedDataTable[]{con.getTable()};
    }

    /**
     * Returns the confusion matrix of the cross validation.
     * 
     * @return a data table containing the confusion matrix
     */
    public DataTable getConfusionMatrix() {
        return m_confusionMatrix;
    }
    
    /**
     * Get the correct classification count, i.e. where both columns agree.
     * 
     * @return the count of rows where the two columns have an equal value or -1
     *         if the node is not executed
     */
    public int getCorrectCount() {
        return m_correctCount;
    }
    
    /**
     * Returns the error of wrong classfied pattern in percentage of the number
     * of patterns.
     * 
     * @return the 1.0 - classification accuracy
     */
    public float getError() {
        float error;
        long totalNumberDataSets = getFalseCount() + getCorrectCount();
        if (totalNumberDataSets == 0) {
            error = Float.NaN;
        } else {
            float ratio = 100.0f / totalNumberDataSets;
            error = ratio * getFalseCount();
        }
        return error;
    }

    /**
     * Get the misclassification count, i.e. where both columns have different
     * values.
     * 
     * @return the count of rows where the two columns have an unequal value or
     *         -1 if the node is not executed
     */
    public int getFalseCount() {
        return m_falseCount;
    }

    /**
     * Get the number of rows in the input table. This count can be different
     * from {@link #getFalseCount()} + {@link #getCorrectCount()}, though it
     * must be at least the sum of both. The difference is the number of rows
     * containing a missing value in either of the target columns.
     * 
     * @return number of rows in input table
     */
    public int getNrRows() {
        return m_nrRows;
    }

    /**
     * @see org.knime.core.node.SpecialNodeModel
     *      #validateSettings(java.io.File, NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final File nodeFile,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(nodeFile, settings);
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * @see org.knime.core.node.SpecialNodeModel
     *      #loadValidatedSettingsFrom(java.io.File, NodeSettingsRO,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadValidatedSettingsFrom(final File nodeDir,
            final NodeSettingsRO settings, final ExecutionMonitor exec)
            throws InvalidSettingsException, IOException {
        super.loadValidatedSettingsFrom(nodeDir, settings, exec);

        m_settings.loadSettingsFrom(settings);
        m_partitionModel.setSettings(m_settings);

        File internals = new File(nodeDir, "internals.xml");
        if (internals.exists()) {
            InputStream in = new BufferedInputStream(
                    new FileInputStream(internals));
            NodeSettingsRO s = NodeSettings.loadFromXML(in);
            in.close();
            try {
                m_partionNode = internalWFM().getNodeContainerById(
                        s.getInt("partitionerNodeID"));
                m_partionNode.retrieveModel(this);
                m_partionNode.setDeletable(false);
            } catch (Exception ex) {
                throw new InvalidSettingsException(
                        "Could not find id of partitioner node");
            }
        } else if (m_partionNode == null) {
            throw new InvalidSettingsException(
                "Could not find id of partitioner node");            
        }
    }

    /**
     * Load internals.
     * 
     * @param internDir The intern node directory.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "confusionMatrix.zip");
        m_confusionMatrix = DataContainer.readFromZip(f);
        File statistics = new File(internDir, "statistics.xml");
        NodeSettingsRO s = NodeSettings.loadFromXML(
                new BufferedInputStream(new FileInputStream(statistics)));
        m_nrRows = s.getInt("nrRows", -1);
        m_falseCount = s.getInt("errorCount", -1);
        m_correctCount = s.getInt("correctCount", -1);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_confusionMatrix != null) {
            File f = new File(internDir, "confusionMatrix.zip");
            DataContainer.writeToZip(m_confusionMatrix, f, exec);
            File statistics = new File(internDir, "statistics.xml");
            NodeSettings s = new NodeSettings("statistics");
            s.addInt("nrRows", m_nrRows);
            s.addInt("errorCount", m_falseCount);
            s.addInt("correctCount", m_correctCount);
            s.saveToXML(new BufferedOutputStream(
                    new FileOutputStream(statistics)));
        }
    }

    /**
     * @see org.knime.core.node.SpecialNodeModel
     *      #saveSettingsTo(java.io.File,
     *      org.knime.core.node.NodeSettingsWO,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveSettingsTo(final File nodeDir,
            final NodeSettingsWO settings, final ExecutionMonitor exec)
            throws InvalidSettingsException, IOException {
        super.saveSettingsTo(nodeDir, settings, exec);

        NodeSettings s = new NodeSettings("XValInternals");
        s.addInt("partitionerNodeID", m_partionNode.getID());
        OutputStream out = new BufferedOutputStream(new FileOutputStream(
                new File(nodeDir, "internals.xml")));
        s.saveToXML(out);
        out.close();
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel#addInternalConnections()
     */
    @Override
    protected void addInternalConnections() {
        super.addInternalConnections();

        for (NodeContainer nc : internalWFM().getNodes()) {
            if (DataInputNodeModel.class.isAssignableFrom(nc.getModelClass())) {
                internalWFM().addConnection(nc.getID(), 0,
                        m_partionNode.getID(), 0);
                break;
            }
        }
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel#addInternalNodes()
     */
    @Override
    protected void addInternalNodes() {
        super.addInternalNodes();

        m_partionNode = internalWFM().addNewNode(
                new XValidatePartitionerFactory());
        m_partionNode.retrieveModel(this);
        m_partionNode.setDeletable(false);
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel
     *      #receiveModel(org.knime.core.node.NodeModel)
     */
    @Override
    public void receiveModel(final NodeModel model) {
        super.receiveModel(model);
        if (model instanceof XValidatePartitionModel) {
            m_partitionModel = (XValidatePartitionModel)model;
        }
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }
}
