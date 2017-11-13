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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.MININGFUNCTION.Enum;
import org.dmg.pmml.MiningModelDocument.MiningModel;
import org.dmg.pmml.SegmentationDocument.Segmentation;
import org.dmg.pmml.TargetDocument.Target;
import org.dmg.pmml.TargetsDocument.Targets;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;

/**
 * Handles the translation of {@link GradientBoostedTreesModel}s to PMML.
 *
 * @author Adrian Nembach, KNIME
 */
final class RegressionGBTModelExporter extends AbstractGBTModelExporter<GradientBoostedTreesModel> {

    /**
     * @param gbtModel the model that should be translated to PMML
     */
    public RegressionGBTModelExporter(final GradientBoostedTreesModel gbtModel) {
        super(gbtModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doWrite(final MiningModel model) {
        // write the initial value
        Targets targets = model.addNewTargets();
        Target target = targets.addNewTarget();
        GradientBoostedTreesModel gbtModel = getGBTModel();
        target.setField(gbtModel.getMetaData().getTargetMetaData().getAttributeName());
        target.setRescaleConstant(gbtModel.getInitialValue());

        // write the model
        Segmentation segmentation = model.addNewSegmentation();
        List<TreeModelRegression> trees = IntStream.range(0, gbtModel.getNrModels())
                .mapToObj(i -> gbtModel.getTreeModelRegression(i))
                .collect(Collectors.toList());
        writeSumSegmentation(segmentation, trees, gbtModel.getCoeffientMaps());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Enum getMiningFunction() {
        return MININGFUNCTION.REGRESSION;
    }

}
