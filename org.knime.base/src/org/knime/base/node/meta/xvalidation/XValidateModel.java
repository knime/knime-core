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
 * If you have any quesions please contact the copyright holder:
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.meta.DataInputNodeModel;
import org.knime.core.node.meta.MetaNodeFactory;
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

    private XValidatePartitionModel m_partitionModel;

    private NodeContainer m_partionNode;

    
    static {
        // this if for backwards compatibility with release 1.0.0
        GlobalClassCreator.addLoadedFactory(XValidatePartitionerFactory.class);
    }
    
    /**
     * Creates a new cross validation node.
     * 
     * @param f the factory that created this model
     */
    public XValidateModel(final MetaNodeFactory f) {
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

        return new DataTableSpec[]{new DataTableSpec(new DataColumnSpecCreator(
                "Error in %", DataType.getType(DoubleCell.class)).createSpec())};
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
        Set<DataCell> classSet = new HashSet<DataCell>();
        Map<DataCell, Integer> classToIndex = new HashMap<DataCell, Integer>();
        List<DataCell> allClasses = new ArrayList<DataCell>();
        int classCount = 0;
        int rowCount = 0;
        for (RowIterator it = inData[0].iterator(); it.hasNext(); rowCount++) {
            DataRow row = it.next();
            if (classSet.add(row.getCell(classColIndex))) {
                classToIndex.put(row.getCell(classColIndex), classCount++);
                allClasses.add(row.getCell(classColIndex));
            }
        }

        int[][] confusionMatrix = new int[allClasses.size()][allClasses.size()];

        final double[][] errors = new double[m_settings.validations()][1];
        final String[] errorRowKeys = new String[m_settings.validations()];
        m_partitionModel.setSettings(m_settings);

        for (int i = 0; i < m_settings.validations(); i++) {
            exec.setProgress(i / (double)m_settings.validations(),
                    "Validating in iteration " + (i + 1));

            m_partitionModel.setPartitionNumber((short)i);
            if (i > 0) {
                m_partitionModel.ignoreNextReset();
            }
            internalWFM().resetAndConfigureAll();
            // m_filter.setTestPartition((byte) i);
            KNIMEConstants.GLOBAL_THREAD_POOL.runInvisible(new Runnable() {
                public void run() {
                    internalWFM().executeAll(true);
                }
            });

            exec.checkCanceled();
            if (innerExecCanceled()) {
                throw new CanceledExecutionException("Inner workflow canceled");
            }
            if (dataOutModel(0).getBufferedDataTable() == null) {
                throw new Exception("Cross validation failed in inner node");
            }

            DataTable prediction = dataOutModel(0).getBufferedDataTable();
            int count = 0;
            for (RowIterator it = prediction.iterator(); it.hasNext();) {
                DataRow row = it.next();
                count++;

                DataCell realValue = row.getCell(classColIndex);
                DataCell predictedValue = row.getCell(row.getNumCells() - 1);
                int x = classToIndex.get(realValue);
                int y = classToIndex.get(predictedValue);
                confusionMatrix[x][y]++;
                if (x != y) {
                    errors[i][0]++;
                }
            }
            errors[i][0] /= count;
            errorRowKeys[i] = "Validation" + i;
        }

        String[] keys = new String[allClasses.size()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = allClasses.get(i).toString();
        }
        m_confusionMatrix = new DefaultTable(confusionMatrix, keys, keys);

        return new BufferedDataTable[]{exec.createBufferedDataTable(
                new DefaultTable(errors, errorRowKeys,
                        new String[]{"Error in %"}), exec)};
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
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        super.reset();
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
