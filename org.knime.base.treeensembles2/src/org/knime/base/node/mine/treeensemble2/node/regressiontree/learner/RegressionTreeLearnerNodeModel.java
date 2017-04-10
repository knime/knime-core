/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 09, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.node.regressiontree.learner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeDataCreator;
import org.knime.base.node.mine.treeensemble2.data.memberships.BitVectorDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.DefaultDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.learner.TreeLearnerRegression;
import org.knime.base.node.mine.treeensemble2.learner.TreeNodeSignatureFactory;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModel;
import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.FilterLearnColumnRearranger;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectHolder;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import com.google.common.math.IntMath;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class RegressionTreeLearnerNodeModel extends NodeModel implements PortObjectHolder {

    /** The file name where to write the internals to. */
    private static final String INTERNAL_DATASAMPLE_FILE = "datasample.zip";
    /** The file name where to write the internals to. */
    private static final String INTERNAL_INFO_FILE = "info.xml";

    private RegressionTreeModelPortObject m_treeModelPortObject;

    private DataTable m_hiliteRowSample;

    private String m_viewMessage;

    private TreeEnsembleLearnerConfiguration m_configuration;

    /**
     *  */
    public RegressionTreeLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
            RegressionTreeModelPortObject.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // guaranteed to not be null (according to API)
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];
        if (m_configuration == null) {
            throw new InvalidSettingsException("No configuration available");
        }
        final FilterLearnColumnRearranger learnRearranger = m_configuration.filterLearnColumns(inSpec);
        final String warn = learnRearranger.getWarning();
        if (warn != null) {
            setWarningMessage(warn);
        }
        m_configuration.checkColumnSelection(inSpec);
        DataTableSpec learnSpec = learnRearranger.createSpec();
        RegressionTreeModelPortObjectSpec treeSpec = new RegressionTreeModelPortObjectSpec(learnSpec);

        return new PortObjectSpec[]{treeSpec};
    }


    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        BufferedDataTable t = (BufferedDataTable)inObjects[0];
        DataTableSpec spec = t.getDataTableSpec();
        final FilterLearnColumnRearranger learnRearranger = m_configuration.filterLearnColumns(spec);
        String warn = learnRearranger.getWarning();
        BufferedDataTable learnTable = exec.createColumnRearrangeTable(t, learnRearranger, exec.createSubProgress(0.0));
        DataTableSpec learnSpec = learnTable.getDataTableSpec();

        ExecutionMonitor readInExec = exec.createSubProgress(0.1);
        ExecutionMonitor learnExec = exec.createSubProgress(0.9);
        TreeDataCreator dataCreator = new TreeDataCreator(m_configuration, learnSpec, learnTable.getRowCount());
        exec.setProgress("Reading data into memory");
        TreeData data = dataCreator.readData(learnTable, m_configuration, readInExec);
        m_hiliteRowSample = dataCreator.getDataRowsForHilite();
        m_viewMessage = dataCreator.getViewMessage();
        String dataCreationWarning = dataCreator.getAndClearWarningMessage();
        if (dataCreationWarning != null) {
            if (warn == null) {
                warn = dataCreationWarning;
            } else {
                warn = warn + "\n" + dataCreationWarning;
            }
        }
        readInExec.setProgress(1.0);
        exec.setMessage("Learning tree");

        RandomData rd = m_configuration.createRandomData();
        final IDataIndexManager indexManager;
        if (data.getTreeType() == TreeType.BitVector) {
            indexManager = new BitVectorDataIndexManager(data.getNrRows());
        } else {
           indexManager = new DefaultDataIndexManager(data);
        }
        TreeNodeSignatureFactory signatureFactory = null;
        int maxLevels = m_configuration.getMaxLevels();
        if (maxLevels < TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            int capacity = IntMath.pow(2, maxLevels - 1);
            signatureFactory = new TreeNodeSignatureFactory(capacity);
        } else {
            signatureFactory = new TreeNodeSignatureFactory();
        }
        final RowSample rowSample = m_configuration.createRowSampler(data).createRowSample(rd);
        TreeLearnerRegression treeLearner = new TreeLearnerRegression(m_configuration, data, indexManager, signatureFactory, rd, rowSample);
        TreeModelRegression regTree = treeLearner.learnSingleTree(learnExec, rd);

        RegressionTreeModel model = new RegressionTreeModel(m_configuration, data.getMetaData(), regTree, data.getTreeType());
        RegressionTreeModelPortObjectSpec treePortObjectSpec = new RegressionTreeModelPortObjectSpec(learnSpec);
        RegressionTreeModelPortObject treePortObject = new RegressionTreeModelPortObject(model, treePortObjectSpec);

        learnExec.setProgress(1.0);
        m_treeModelPortObject = treePortObject;
        if (warn != null) {
            setWarningMessage(warn);
        }
        return new PortObject[]{treePortObject};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_treeModelPortObject = null;
        m_viewMessage = null;
        m_hiliteRowSample = null;
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new TreeEnsembleLearnerConfiguration(true).loadInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(true);
        config.loadInModel(settings);
        m_configuration = config;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.save(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        File file = new File(nodeInternDir, INTERNAL_DATASAMPLE_FILE);
        if (file.exists()) {
            m_hiliteRowSample = DataContainer.readFromZip(file);
        }
        file = new File(nodeInternDir, INTERNAL_INFO_FILE);
        if (file.exists()) {
            NodeSettingsRO sets = NodeSettings.loadFromXML(new FileInputStream(file));
            m_viewMessage = sets.getString("view_warning", null);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        File file;
        ExecutionMonitor sub;
        if (m_hiliteRowSample != null) {
            file = new File(nodeInternDir, INTERNAL_DATASAMPLE_FILE);
            sub = exec.createSubProgress(0.2);
            DataContainer.writeToZip(m_hiliteRowSample, file, sub);
        }
        if (m_viewMessage != null) {
            file = new File(nodeInternDir, INTERNAL_INFO_FILE);
            NodeSettings sets = new NodeSettings("ensembleData");
            sets.addString("view_warning", m_viewMessage);
            sets.saveToXML(new FileOutputStream(file));
        }
    }

    /** {@inheritDoc} */
    @Override
    public PortObject[] getInternalPortObjects() {
        return new PortObject[] {m_treeModelPortObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalPortObjects(final PortObject[] portObjects) {
        m_treeModelPortObject = (RegressionTreeModelPortObject)portObjects[0];
    }

    /**
     * {@inheritDoc}
     */
    public RegressionTreeModel getRegressionTreeModel() {
        return m_treeModelPortObject == null ? null : m_treeModelPortObject.getModel();
    }

    /**
     * {@inheritDoc}
     */
    public DataTable getHiliteRowSample() {
        return m_hiliteRowSample;
    }

    /**
     * {@inheritDoc}
     */
    public String getViewMessage() {
        return m_viewMessage;
    }

}
