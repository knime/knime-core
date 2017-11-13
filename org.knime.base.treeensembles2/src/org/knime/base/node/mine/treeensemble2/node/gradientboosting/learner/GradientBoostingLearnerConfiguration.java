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
 *   13.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner;

import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class GradientBoostingLearnerConfiguration extends TreeEnsembleLearnerConfiguration {

    private static final String KEY_LEARNINGRATE = "learningRate";

    private static final String KEY_ALPHA_FRACTION = "alphaFraction";

    /**
     * Default learning rate (0.1)
     */
    public static final double DEF_LEARNINGRATE = 0.1;

    /**
     * Default alpha (0.95)
     */
    public static final double DEF_ALPHA_FRACTION = 0.95;

    private double m_learningRate = DEF_LEARNINGRATE;

    private double m_alphaFraction = DEF_ALPHA_FRACTION;

    /**
     * @param isRegression
     */
    public GradientBoostingLearnerConfiguration(final boolean isRegression) {
        super(isRegression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeEnsembleModelPortObjectSpec createPortObjectSpec(final DataTableSpec learnSpec) {
        return new TreeEnsembleModelPortObjectSpec(learnSpec);
    }

    /**
     * @return The learning rate
     */
    public double getLearningRate() {
        return m_learningRate;
    }

    /**
     * Sets the learning rate to <b>learningRate</b>
     *
     * @param learningRate
     */
    public void setLearningRate(final double learningRate) {
        m_learningRate = learningRate;
    }

    /**
     * @return The alpha which declares the quantile that is used as threshold in the huber loss function
     */
    public double getAlpha() {
        return m_alphaFraction;
    }

    /**
     * Sets the alpha to <b>alphaFraction</b>
     *
     * @param alphaFraction
     */
    public void setAlpha(final double alphaFraction) {
        m_alphaFraction = alphaFraction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final NodeSettingsWO settings) {
        super.save(settings);
        settings.addDouble(KEY_LEARNINGRATE, m_learningRate);
        settings.addDouble(KEY_ALPHA_FRACTION, m_alphaFraction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadInDialog(final NodeSettingsRO settings, final DataTableSpec inSpec)
        throws NotConfigurableException {
        super.loadInDialog(settings, inSpec);
        String colSamplingModeString = settings.getString(KEY_COLUMN_SAMPLING_MODE, null);
        if (colSamplingModeString == null) {
            try {
                setColumnSamplingMode(ColumnSamplingMode.None);
            } catch (InvalidSettingsException e) {
                throw new NotConfigurableException("Loading column sampling mode failed", e);
            }
        }

        setDataSelectionWithReplacement(settings.getBoolean(KEY_IS_DATA_SELECTION_WITH_REPLACEMENT, false));

        try {
            setMaxLevels(settings.getInt(KEY_MAX_LEVELS, 4));
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException("Loading the maximal tree depth failed.", e);
        }

        setUseDifferentAttributesAtEachNode(settings.getBoolean(KEY_IS_USE_DIFFERENT_ATTRIBUTES_AT_EACH_NODE, false));

        m_learningRate = settings.getDouble(KEY_LEARNINGRATE, DEF_LEARNINGRATE);

        m_alphaFraction = settings.getDouble(KEY_ALPHA_FRACTION, DEF_ALPHA_FRACTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadInModel(settings);
        m_learningRate = settings.getDouble(KEY_LEARNINGRATE);
        m_alphaFraction = settings.getDouble(KEY_ALPHA_FRACTION);
    }

}
