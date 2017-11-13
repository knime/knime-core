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
 *   02.10.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.MININGFUNCTION.Enum;
import org.dmg.pmml.MULTIPLEMODELMETHOD;
import org.dmg.pmml.MiningModelDocument.MiningModel;
import org.dmg.pmml.SegmentDocument.Segment;
import org.dmg.pmml.SegmentationDocument.Segmentation;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Handles the export of {@link GradientBoostedTreesModel}s on an abstract level.
 *
 * @author Adrian Nembach, KNIME
 */
abstract class AbstractGBTModelExporter<M extends AbstractGradientBoostingModel> extends AbstractWarningHolder {
    private final M m_gbtModel;
    private PMMLPortObjectSpec m_pmmlSpec;

    public AbstractGBTModelExporter(final M gbtModel) {
        m_gbtModel = gbtModel;
    }

    /**
     * Writes the Gradient Boosted Trees model to the provided PMML {@link MiningModel}
     * @param model the mining model to write to
     * @param pmmlSpec the PMML port object spec
     * @return the schema type (mining model)
     */
    public SchemaType writeModelToPMML(final MiningModel model, final PMMLPortObjectSpec pmmlSpec) {
        PMMLMiningSchemaTranslator.writeMiningSchema(pmmlSpec, model);
        m_pmmlSpec = pmmlSpec;
        model.setFunctionName(getMiningFunction());
        doWrite(model);
        return MiningModel.type;
    }

    protected abstract Enum getMiningFunction();

    protected abstract void doWrite(final MiningModel model);

    protected void writeSumSegmentation(final Segmentation segmentation, final Collection<TreeModelRegression> trees,
        final Collection<Map<TreeNodeSignature, Double>> coefficientMaps) {
        assert trees.size() == coefficientMaps.size() :
            "The number of trees does not match the number of coefficient maps.";
        segmentation.setMultipleModelMethod(MULTIPLEMODELMETHOD.SUM);
        Iterator<TreeModelRegression> treeIterator = trees.iterator();
        Iterator<Map<TreeNodeSignature, Double>> coefficientMapIterator = coefficientMaps.iterator();
        for (int i = 1; i <= trees.size(); i++) {
            Segment segment = segmentation.addNewSegment();
            segment.setId(Integer.toString(i));
            segment.addNewTrue();
            writeTreeIntoSegment(segment, treeIterator.next(), coefficientMapIterator.next());
        }
    }

    protected M getGBTModel() {
        return m_gbtModel;
    }

    protected PMMLPortObjectSpec getPMMLSpec() {
        return m_pmmlSpec;
    }

    private void writeTreeIntoSegment(final Segment segment, final TreeModelRegression tree,
        final Map<TreeNodeSignature, Double> coefficientMap) {
        assert m_pmmlSpec != null : "The pmml spec is null, this indicates an implementation mistake.";
        GBTRegressionTreeModelExporter exporter = new GBTRegressionTreeModelExporter(tree, coefficientMap);
        if (exporter.hasWarning()) {
            addWarning(exporter.getWarning());
        }
        TreeModel treeModel = segment.addNewTreeModel();
        exporter.writeModelToPMML(treeModel, m_pmmlSpec);
    }

}
