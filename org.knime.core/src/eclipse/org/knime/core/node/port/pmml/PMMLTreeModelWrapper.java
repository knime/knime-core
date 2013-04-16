/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 */
package org.knime.core.node.port.pmml;

import java.util.List;

import org.dmg.pmml.DataDictionaryDocument.DataDictionary;
import org.dmg.pmml.MININGFUNCTION.Enum;
import org.dmg.pmml.MiningSchemaDocument.MiningSchema;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.SegmentDocument.Segment;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.knime.core.pmml.PMMLModelType;


/**
 *
 * @author Alexander Fillbrunn, Universitaet Konstanz
 * @since 2.8
 */
public class PMMLTreeModelWrapper extends PMMLModelWrapper {

    private TreeModel m_model;

    /**
     * Creates a wrapper for a org.dmg.pmml.TreeModelDocument.TreeModel.
     * @param model The org.dmg.pmml.TreeModelDocument.TreeModel
     */
    public PMMLTreeModelWrapper(final TreeModel model) {
        m_model = model;
    }

    /**
     * Getter for the original model.
     * @return The original model
     */
    public TreeModel getModel() {
        return m_model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLModelType getModelType() {
        return PMMLModelType.TreeModel;
    }

    @Override
    public List<String> getTargetCols() {
        return getTargetCols(m_model.getTargets());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MiningSchema getMiningSchema() {
        return m_model.getMiningSchema();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMiningSchema(final MiningSchema miningSchema) {
        m_model.setMiningSchema(miningSchema);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enum getFunctionName() {
        return m_model.getFunctionName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFunctionName(final Enum functionName) {
        m_model.setFunctionName(functionName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAlgorithmName() {
        return m_model.getAlgorithmName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSetAlgorithmName() {
        return m_model.isSetAlgorithmName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAlgorithmName(final String algorithmName) {
        m_model.setAlgorithmName(algorithmName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToSegment(final Segment s) {
        s.setTreeModel(m_model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLDocument createPMMLDocument(final DataDictionary dataDict) {
        PMMLDocument doc = createEmptyDocument(dataDict);
        doc.getPMML().setTreeModelArray(new TreeModel[]{m_model});
        return doc;
    }
}
