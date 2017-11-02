/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   02.10.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;

/**
 * Abstract implementation for the translation of gradient boosted trees model from and to PMML.
 *
 * @author Adrian Nembach, KNIME
 * @param <M> The model type that is handled by the translator
 */
public abstract class AbstractGBTModelPMMLTranslator <M extends AbstractGradientBoostingModel>
extends AbstractWarningHolder implements PMMLTranslator {

    private M m_gbtModel;

    /**
     * Default constructor to be used if a model should be read from PMML.
     */
    public AbstractGBTModelPMMLTranslator() {
        // nothing to do
    }

    /**
     * Default constructor to be used if a model should be written to PMML.
     *
     * @param gbtModel
     */
    public AbstractGBTModelPMMLTranslator(final M gbtModel) {
        m_gbtModel = gbtModel;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        PMML pmml = pmmlDoc.getPMML();
        MetaDataMapper<TreeTargetNumericColumnMetaData> metaDataMapper = new RegressionMetaDataMapper(pmmlDoc);
        AbstractGBTModelImporter<M> importer = createImporter(metaDataMapper);
        m_gbtModel = importer.importFromPMML(pmml.getMiningModelList().get(0));
    }

    /**
     * @param metaDataMapper the initialized mapper for meta information
     * @return the corresponding model importer
     */
    protected abstract AbstractGBTModelImporter<M> createImporter(
        final MetaDataMapper<TreeTargetNumericColumnMetaData> metaDataMapper);

    /**
     * @return the exporter for the current model type
     */
    protected abstract AbstractGBTModelExporter<M> createExporter();

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {
        PMML pmml = pmmlDoc.getPMML();
        AbstractGBTModelExporter<M> exporter = createExporter();

        SchemaType st = exporter.writeModelToPMML(pmml.addNewMiningModel(), spec);
        if (exporter.hasWarning()) {
            addWarning(exporter.getWarning());
        }
        return st;
    }

    /**
     * @return the Gradient Boosted Trees model this translator holds
     * @throws IllegalStateException if the model is not initialized yet
     */
    public M getGBTModel() {
        if (m_gbtModel == null) {
            throw new IllegalStateException("This translator currently holds no Gradient Boosted Trees model. "
                + "Please initialize the model from PMML or create a new translator with an exisiting Gradient Boosted"
                + " Trees model.");
        }
        return m_gbtModel;
    }

}
