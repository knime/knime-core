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
 *   Jan 1, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.swing.JComponent;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

import com.google.common.collect.Lists;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeEnsembleModelPortObject extends FileStorePortObject {
    /**
     *
     * @author Adrian Nembach
     */
    public static final class Serializer extends PortObjectSerializer<TreeEnsembleModelPortObject> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void savePortObject(final TreeEnsembleModelPortObject portObject, final PortObjectZipOutputStream out,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            portObject.save(out, exec);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TreeEnsembleModelPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            final TreeEnsembleModelPortObject model = new TreeEnsembleModelPortObject();
            model.load(in, spec, exec);
            return model;
        }
    }

    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(TreeEnsembleModelPortObject.class);

    private static final String CFG_NRATTRIBUTES = "nrAttributes";
    private static final String CFG_NRMODELS = "nrModels";
    private static final String CFG_MODELCONTENT = "modelContent";

    private TreeEnsembleModelPortObjectSpec m_spec;

    private WeakReference<TreeEnsembleModel> m_modelRef;

    private int m_nrModels;

    private int m_nrAttributes;

    public static TreeEnsembleModelPortObject createPortObject(final TreeEnsembleModelPortObjectSpec spec,
        final TreeEnsembleModel ensembleModel, final FileStore fileStore) {
        final TreeEnsembleModelPortObject po = new TreeEnsembleModelPortObject(spec, ensembleModel, fileStore);
        try {
            serialize(ensembleModel, fileStore);
        } catch (IOException e) {
            throw new IllegalStateException("Something went wrong during serialization.", e);
        }
        return po;
    }

    /**
     * @param models
     */
    private TreeEnsembleModelPortObject(final TreeEnsembleModelPortObjectSpec spec,
        final TreeEnsembleModel ensembleModel, final FileStore fileStore) {
        super(Lists.newArrayList(fileStore));
        m_spec = spec;
        m_modelRef = new WeakReference<TreeEnsembleModel>(ensembleModel);
        m_nrModels = ensembleModel.getNrModels();
        m_nrAttributes = ensembleModel.getMetaData().getNrAttributes();
    }

    /** Framework constructor, not to be used by node code. */
    public TreeEnsembleModelPortObject() {
        // no op, load method to be called by framework
    }

    /**
     * @return the ensembleModel
     */
    public synchronized TreeEnsembleModel getEnsembleModel() {
        TreeEnsembleModel ensembleModel = m_modelRef.get();
        if (ensembleModel == null) {
            try {
                ensembleModel = deserialize();
            } catch (IOException e) {
                throw new IllegalStateException("Something went wrong during deserialization.", e);
            }
            m_modelRef = new WeakReference<TreeEnsembleModel>(ensembleModel);
        }
        return ensembleModel;
    }

    private TreeEnsembleModel deserialize() throws IOException {
        final File file = getFileStore(0).getFile();
        TreeEnsembleModel ensembleModel;
        try (FileInputStream input = new FileInputStream(file)) {
            ensembleModel = TreeEnsembleModel.load(input);
        }
        return ensembleModel;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        StringBuilder b = new StringBuilder();
        b.append(m_nrModels);
        b.append(" classifiers on ");
        b.append(m_nrAttributes);
        b.append(" attributes");
        return b.toString();
    }

    /** {@inheritDoc} */
    @Override
    public TreeEnsembleModelPortObjectSpec getSpec() {
        return m_spec;
    }

    public PMMLPortObject createDecisionTreePMMLPortObject(final int modelIndex) {
        final TreeEnsembleModel ensembleModel = getEnsembleModel();
        DataTableSpec attributeLearnSpec = ensembleModel.getLearnAttributeSpec(m_spec.getLearnTableSpec());
        DataColumnSpec targetSpec = m_spec.getTargetColumn();
        PMMLPortObjectSpecCreator pmmlSpecCreator =
            new PMMLPortObjectSpecCreator(new DataTableSpec(attributeLearnSpec, new DataTableSpec(targetSpec)));

        try {
            pmmlSpecCreator.setLearningCols(attributeLearnSpec);
        } catch (InvalidSettingsException e) {
            // this exception is not actually thrown in the code
            // (as of KNIME v2.5.1)
            throw new IllegalStateException(e);
        }
        pmmlSpecCreator.setTargetCol(targetSpec);
        PMMLPortObjectSpec pmmlSpec = pmmlSpecCreator.createSpec();
        PMMLPortObject portObject = new PMMLPortObject(pmmlSpec);
        TreeModelClassification model = ensembleModel.getTreeModelClassification(modelIndex);
        portObject.addModelTranslater(new TreeModelPMMLTranslator(model));
        return portObject;
    }

    private void save(final PortObjectZipOutputStream out, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        ModelContent mc = new ModelContent(CFG_MODELCONTENT);
        mc.addInt(CFG_NRATTRIBUTES, m_nrAttributes);
        mc.addInt(CFG_NRMODELS, m_nrModels);
        mc.saveToXML(out);
    }

    private void load(final PortObjectZipInputStream in, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        m_spec = (TreeEnsembleModelPortObjectSpec)spec;
        m_modelRef = new WeakReference<TreeEnsembleModel>(null);
        ModelContentRO mc = ModelContent.loadFromXML(in);
        try {
            m_nrAttributes = mc.getInt(CFG_NRATTRIBUTES);
            m_nrModels = mc.getInt(CFG_NRMODELS);
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException("Unable to restore meta information: " + ise.getMessage());
            ioe.initCause(ise);
            throw ioe;
        }
//        in.getNextEntry();
//        m_modelRef = new WeakReference<TreeEnsembleModel>(TreeEnsembleModel.load(in, exec));
//        in.closeEntry();
        // call to ensure that the framework has established the FileStore
    }


    private static void serialize(final TreeEnsembleModel ensembleModel, final FileStore fileStore) throws IOException {
        final File file = fileStore.getFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            ensembleModel.save(out);
        }

    }

    /** {@inheritDoc} */
    @Override
    public JComponent[] getViews() {
        return null;
    }
}
