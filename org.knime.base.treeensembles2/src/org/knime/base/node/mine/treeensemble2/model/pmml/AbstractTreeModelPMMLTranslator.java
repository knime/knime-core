/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ---------------------------------------------------------------------
 *
 * History
 *   04.09.2017 (Adrian): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.List;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.TreeModelDocument;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;

/**
 *
 * @author Adrian Nembach, KNIME
 * @param <N> the type of node the trees handled by this translator consist of
 * @param <M> the type of tree this translator handles
 * @param <T> the type of meta data information of the target column
 *
 */
public abstract class AbstractTreeModelPMMLTranslator<N extends AbstractTreeNode, M extends AbstractTreeModel<N>,
T extends TreeTargetColumnMetaData> extends AbstractWarningHolder implements PMMLTranslator {

    private AbstractTreeModel<N> m_treeModel;
    private TreeMetaData m_treeMetaData;

    /**
     * @param treeModel a tree model that should be translated to pmml
     * @param metaData the meta data associated with the tree model
     *
     */
    public AbstractTreeModelPMMLTranslator(final AbstractTreeModel<N> treeModel, final TreeMetaData metaData) {
        m_treeModel = treeModel;
        m_treeMetaData = metaData;
    }

    /**
     * Default constructor to be used if a tree model should be initialized from PMML.
     * Note that the getTree and getTreeMetaData methods will throw a IllegalStateException as long as the tree model
     * has not been initialized from PMML.
     */
    public AbstractTreeModelPMMLTranslator() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        PMML pmml = pmmlDoc.getPMML();
        List<TreeModel> trees = pmml.getTreeModelList();
        if (trees.size() > 1) {
            throw new IllegalArgumentException("This translator handles only single trees.");
        } else if (trees.isEmpty()) {
            throw new IllegalArgumentException("The provided PMMLDocument contains no tree models.");
        }

        MetaDataMapper<T> metaDataMapper = createMetaDataMapper(pmmlDoc);
        TreeModelImporter<N, M, T> importer = createImporter(metaDataMapper);
        m_treeModel = importer.importFromPMML(trees.get(0));
        m_treeMetaData = metaDataMapper.getTreeMetaData();
    }

    /**
     * Checks if the provided spec is a valid spec for a model translatable with this translator.
     * @param pmmlSpec the {@link PMMLPortObjectSpec} of a tree that should be imported
     */
    public static void checkPMMLSpec(final PMMLPortObjectSpec pmmlSpec) {
        // it won't be possible to construct a meta data mapper from an incompatible spec
        AbstractMetaDataMapper.createMetaDataMapper(pmmlSpec.getDataTableSpec());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {
        PMML pmml = pmmlDoc.getPMML();
        TreeModelDocument.TreeModel treeModel = pmml.addNewTreeModel();
        AbstractTreeModelExporter<N> exporter = createExporter();
        SchemaType st = exporter.writeModelToPMML(treeModel, spec);
        if (exporter.hasWarning()) {
            addWarning(exporter.getWarning());
        }
        return st;
    }

    /**
     * @return the tree model that this translator operates on
     */
    public AbstractTreeModel<N> getTree() {
        if (m_treeModel == null) {
            throw new IllegalStateException(
                "This translator has no tree model yet. Please read one from PMML or initialize a"
                + " new translator with a tree model.");
        }
        return m_treeModel;
    }

    /**
     * @return the meta data associated with the tree model
     */
    public TreeMetaData getTreeMetaData() {
        if (m_treeMetaData == null) {
            throw new IllegalStateException(
                "This translator has no tree meta data yet. "
                + "Please read a tree model from PMML or create a new translator with the model and its meta data.");
        }
        return m_treeMetaData;
    }

    /**
     * @return an exporter that is used to export the tree model to PMML
     */
    protected abstract AbstractTreeModelExporter<N> createExporter();

    /**
     * @param pmmlDoc the PMML Document from which to read a tree model
     * @return a MetaDataMapper that is used in the translation process
     */
    protected abstract MetaDataMapper<T> createMetaDataMapper(PMMLDocument pmmlDoc);

    /**
     * @param metaDataMapper the MetaDataMapper that holds meta information
     *  obtained from the PMML document from which to import the tree model
     * @return an importer that handles the import of tree models from PMML
     */
    protected abstract TreeModelImporter<N, M, T> createImporter(final MetaDataMapper<T> metaDataMapper);


}
