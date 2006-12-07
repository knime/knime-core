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

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.meta.MetaNodeModel;
import org.knime.core.node.workflow.NodeContainer;

/**
 * This model represents a cross validation node. The internal workflow must at
 * least consist of any learner and its predictor. The internal workflow has two
 * input nodes, the first provides the training data, the second the test data.
 * The output node must be fed with the output table of the predictor which must
 * have the prediction in its last column.
 * 
 * The input into the meta node is just any data table, the output consists of a
 * simple performance value at the first port and a confusion matrix at the
 * second.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateModel extends MetaNodeModel {
    private final XValidateSettings m_settings = new XValidateSettings();

    private XValidatePartitionModel m_partitionModel;

    private AggregateOutputNodeModel m_aggregateModel;

    private NodeContainer m_partitionNode;

    private NodeContainer m_aggregateNode;

    static {
        // this if for backwards compatibility with release 1.0.0
        NodeFactory.addLoadedFactory(XValidatePartitionerFactory.class);
    }

    /**
     * Creates a new cross validation node.
     */
    public XValidateModel() {
        super(1, 2, 0, 0);
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        super.configure(inSpecs);
        DataTableSpec in = inSpecs[0];
        DataTableSpec outSpec0 = null;
        try {
            ColumnRearranger r =
                    m_aggregateModel.createColumnRearrangerPort0(in);
            outSpec0 = r.createSpec();
        } catch (InvalidSettingsException ise) {
            // ignore here (happens when inner flow is not properly connected
        }
        DataTableSpec outSpec1 = m_aggregateModel.createSpecPort1();
        return new DataTableSpec[]{outSpec0, outSpec1};
    }

    /**
     * @see NodeModel #execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_partitionModel.setSettings(m_settings);

        for (int i = 0; i < m_settings.validations(); i++) {
            exec.setProgress(i / (double)m_settings.validations(),
                    "Validating in iteration " + (i + 1));

            m_partitionModel.setPartitionNumber((short)i);
            m_partitionModel.setIgnoreNextReset(i > 0);
            m_aggregateModel.setIgnoreReset(i > 0);
            resetAndConfigureInternalWF();
            executeInternalWF();

            exec.checkCanceled();
            if (innerExecCanceled()) {
                throw new CanceledExecutionException("Inner workflow canceled");
            }
            if (dataOutModel(0).getBufferedDataTable() == null) {
                throw new Exception("Cross validation failed in inner node");
            }
        }
        
        m_aggregateModel.setIgnoreReset(false);
        m_partitionModel.setIgnoreNextReset(false);        
        
        ColumnRearranger colRePort0 =
                m_aggregateModel.createColumnRearrangerPort0(inData[0]
                        .getDataTableSpec());
        BufferedDataTable outPort0 =
                exec.createColumnRearrangeTable(inData[0], colRePort0, exec
                        .createSubProgress(0.0));
        BufferedDataTable outPort1 = m_aggregateModel.createOutputPort1(exec);
        return new BufferedDataTable[]{outPort0, outPort1};
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel
     *      #validateSettings(java.io.File, org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final File nodeFile,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(nodeFile, settings);
        (new XValidateSettings()).loadSettingsFrom(settings);
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel
     *      #loadValidatedSettingsFrom(java.io.File,
     *      org.knime.core.node.NodeSettingsRO,
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
            InputStream in =
                    new BufferedInputStream(new FileInputStream(internals));
            NodeSettingsRO s = NodeSettings.loadFromXML(in);
            in.close();
            try {
                m_partitionNode =
                        internalWFM().getNodeContainerById(
                                s.getInt("partitionerNodeID"));
                m_partitionNode.retrieveModel(this);
                m_partitionNode.setDeletable(false);
            } catch (Exception ex) {
                throw new InvalidSettingsException(
                        "Could not find id of partitioner node");
            }
            try {
                m_aggregateNode =
                        internalWFM().getNodeContainerById(
                                s.getInt("aggregateNodeID"));
                m_aggregateNode.retrieveModel(this);
                m_aggregateNode.setDeletable(false);
            } catch (Exception ex) {
                throw new InvalidSettingsException(
                        "Could not find id of aggregator node");
            }
        } else if (m_partitionNode == null) {
            throw new InvalidSettingsException(
                    "Could not find ids of internal nodes");
        }
    }

    /**
     * Load internals.
     * 
     * @param internDir The intern node directory.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see NodeModel#loadInternals(File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
    }

    /**
     * @see NodeModel#saveInternals(File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel #saveSettingsTo(java.io.File,
     *      org.knime.core.node.NodeSettingsWO,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveSettingsTo(final File nodeDir,
            final NodeSettingsWO settings, final ExecutionMonitor exec)
            throws InvalidSettingsException, IOException {
        super.saveSettingsTo(nodeDir, settings, exec);

        NodeSettings s = new NodeSettings("XValInternals");
        s.addInt("partitionerNodeID", m_partitionNode.getID());
        s.addInt("aggregateNodeID", m_aggregateNode.getID());
        OutputStream out =
                new BufferedOutputStream(new FileOutputStream(new File(nodeDir,
                        "internals.xml")));
        s.saveToXML(out);
        out.close();
    }

    /**
     * @see MetaNodeModel#addInternalConnections()
     */
    @Override
    protected void addInternalConnections() {
        super.addInternalConnections();
        internalWFM().addConnection(dataInNodeContainer(0).getID(), 0,
                m_partitionNode.getID(), 0);
        internalWFM().addConnection(m_aggregateNode.getID(), 0,
                dataOutNodeContainer(0).getID(), 0);
        internalWFM().addConnection(m_aggregateNode.getID(), 1,
                dataOutNodeContainer(1).getID(), 0);
    }

    /**
     * @see MetaNodeModel#addInternalNodes()
     */
    @Override
    protected void addInternalNodes() {
        super.addInternalNodes();
        m_partitionNode =
                internalWFM().addNewNode(new XValidatePartitionerFactory());
        m_partitionNode.retrieveModel(this);
        m_partitionNode.setDeletable(false);
        m_aggregateNode =
                internalWFM().addNewNode(new AggregateOutputNodeFactory());
        m_aggregateNode.retrieveModel(this);
        m_aggregateNode.setDeletable(false);
    }

    /**
     * @see MetaNodeModel#receiveModel(NodeModel)
     */
    @Override
    public void receiveModel(final NodeModel model) {
        super.receiveModel(model);
        if (model instanceof XValidatePartitionModel) {
            m_partitionModel = (XValidatePartitionModel)model;
        } else if (model instanceof AggregateOutputNodeModel) {
            m_aggregateModel = (AggregateOutputNodeModel)model;
        }
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel#reset()
     */
    @Override
    protected void reset() {
        super.reset();
        m_aggregateModel.setIgnoreReset(false);
        m_partitionModel.setIgnoreNextReset(false);        
    }
}
