/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   09.05.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.pmml.importer;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.model.GradientBoostingModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.pmml.AbstractGBTModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.model.pmml.AbstractTreeModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.model.pmml.ClassificationGBTModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.model.pmml.RegressionGBTModelPMMLTranslator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 *
 * @author Adrian Nembach, KNIME
 */
class GBTPMMLImporterNodeModel extends NodeModel {

    /**
     */
    protected GBTPMMLImporterNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE}, new PortType[]{GradientBoostingModelPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        PMMLPortObject pmmlPO = (PMMLPortObject)inObjects[0];
        AbstractGBTModelPMMLTranslator<?> pmmlTranslator;
        DataType targetType = pmmlPO.getSpec().getTargetCols().get(0).getType();
        if (targetType.isCompatible(DoubleValue.class)) {
            pmmlTranslator = new RegressionGBTModelPMMLTranslator();
        } else if (targetType.isCompatible(StringValue.class)) {
            pmmlTranslator = new ClassificationGBTModelPMMLTranslator();
        } else {
            throw new IllegalArgumentException(
                "Only numeric (regression) and nominal (classification) targets are supported.");
        }
        pmmlPO.initializeModelTranslator(pmmlTranslator);
        TreeEnsembleModelPortObjectSpec spec = new TreeEnsembleModelPortObjectSpec(pmmlPO.getSpec().getDataTableSpec());
        if (pmmlTranslator.hasWarning()) {
            setWarningMessage(pmmlTranslator.getWarning());
        }
        // Currently only ordinary trees can be imported (otherwise the initialization will fail)
        return new PortObject[]{new GradientBoostingModelPortObject(spec, pmmlTranslator.getGBTModel())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)inSpecs[0];
        // check if the spec is compatible
        try {
            AbstractTreeModelPMMLTranslator.checkPMMLSpec(pmmlSpec);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage());
        }
        return new PortObjectSpec[] {new TreeEnsembleModelPortObjectSpec(pmmlSpec.getDataTableSpec())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals to load

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals to save

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // no settings to save

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings to validate

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings to load

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset

    }

}
