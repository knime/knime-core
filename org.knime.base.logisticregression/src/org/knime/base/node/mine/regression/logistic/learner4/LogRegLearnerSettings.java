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
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner4;

import java.util.EnumSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * This class hold the settings for the Logistic Learner Node.
 *
 * @author Heiko Hofer
 * @author Gabor Bakos
 * @author Adrian Nembach, KNIME.com
 * @since 3.1
 */
class LogRegLearnerSettings {

    enum Solver {
        IRLS("Iteratively reweighted least squares", false, EnumSet.noneOf(LearningRateStrategies.class), EnumSet.noneOf(Prior.class)),
        SAG("Stochastic average gradient", true, EnumSet.of(LearningRateStrategies.Fixed, LearningRateStrategies.LineSearch),
            EnumSet.allOf(Prior.class)),
        SGD("Stochastic gradient descent", false, EnumSet.of(LearningRateStrategies.Fixed, LearningRateStrategies.Annealing),
            EnumSet.allOf(Prior.class));

        private final boolean m_supportsLazy;
        private final String m_toString;
        private final EnumSet<LearningRateStrategies> m_compatibleLRS;
        private final EnumSet<Prior> m_compatiblePriors;

        private Solver(final String toString, final boolean supportsLazy, final EnumSet<LearningRateStrategies> compatibleLRS,
            final EnumSet<Prior> compatiblePriors) {
            m_supportsLazy = supportsLazy;
            m_toString = toString;
            m_compatibleLRS = compatibleLRS;
            m_compatiblePriors = compatiblePriors;
        }

        @Override
        public String toString() {
            return m_toString;
        }

        public boolean supportsLazy() {
            return m_supportsLazy;
        }

        public EnumSet<Prior> getCompatiblePriors() {
            return m_compatiblePriors;
        }

        public EnumSet<LearningRateStrategies> getCompatibleLearningRateStrategies() {
            return m_compatibleLRS;
        }

        public boolean compatibleWith(final LearningRateStrategies lrs) {
            return m_compatibleLRS.contains(lrs);
        }

        public boolean compatibleWith(final Prior prior) {
            return m_compatiblePriors.contains(prior);
        }
    }

    enum Prior {
        Uniform(false),
        Gauss(true),
        Laplace(true);

        private final boolean m_hasVariance;
        private Prior(final boolean hasVariance) {
            m_hasVariance = hasVariance;
        }

        public boolean hasVariance() {
            return m_hasVariance;
        }
    }

    enum LearningRateStrategies {
        Fixed(true, false),
        Annealing(true, true),
        LineSearch(false, false);

        private final boolean m_hasInitialValue;
        private final boolean m_hasDecayRate;

        private LearningRateStrategies(final boolean hasInitialValue, final boolean hasDecayRate) {
            m_hasInitialValue = hasInitialValue;
            m_hasDecayRate = hasDecayRate;
        }

        public boolean hasInitialValue() {
            return m_hasInitialValue;
        }

        public boolean hasDecayRate() {
            return m_hasDecayRate;
        }
    }


    private static final String CFG_TARGET = "target";
    private static final String CFG_TARGET_REFERENCE_CATEGORY = "target_reference_category";
    private static final String CFG_SORT_TARGET_CATEGORIES = "sort_target_categories";
    private static final String CFG_SORT_INCLUDES_CATEGORIES = "sort_includes_categories";
    private static final String CFG_SOLVER = "solver";
    private static final String CFG_MAX_EPOCH = "maxEpoch";
    private static final String CFG_PERFORM_LAZY = "performLazy";
    private static final String CFG_PRIOR = "prior";
    private static final String CFG_PRIOR_VARIANCE = "priorVariance";
    private static final String CFG_LEARNING_RATE_STRATEGY = "learningRateStrategy";
    private static final String CFG_INITIAL_LEARNING_RATE = "initialLearningRate";
    private static final String CFG_LEARNING_RATE_DECAY = "learningRateDecay";


    private String m_targetColumn;

    /** The selected learning columns configuration. */
    private DataColumnSpecFilterConfiguration m_includedColumns = LogRegLearnerNodeModel.createDCSFilterConfiguration();

    /** The target reference category, if not set it is the last category. */
    private DataCell m_targetReferenceCategory;
    /** True when target categories should be sorted. */
    private boolean m_sortTargetCategories;
    /** True when categories of nominal data in the include list should be sorted. */
    private boolean m_sortIncludesCategories;

    private Solver m_solver;

    private int m_maxEpoch;
    private boolean m_performLazy;
    private double m_priorVariance;
    private double m_initialLearningRate;
    private double m_learningRateDecay;
    private LearningRateStrategies m_learningRateStrategy;
    private Prior m_prior;

    /**
     * Create default settings.
     */
    public LogRegLearnerSettings() {
        m_targetColumn = null;
        m_targetReferenceCategory = null;
        m_sortTargetCategories = true;
        m_sortIncludesCategories = true;
        m_solver = Solver.SAG;
    }


    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_targetColumn = settings.getString(CFG_TARGET);
        m_includedColumns.loadConfigurationInModel(settings);
        m_targetReferenceCategory = settings.getDataCell(CFG_TARGET_REFERENCE_CATEGORY);
        m_sortTargetCategories = settings.getBoolean(CFG_SORT_TARGET_CATEGORIES);
        m_sortIncludesCategories = settings.getBoolean(CFG_SORT_INCLUDES_CATEGORIES);

        String solverString = settings.getString(CFG_SOLVER);
        m_solver = Solver.valueOf(solverString);
        m_maxEpoch = settings.getInt(CFG_MAX_EPOCH);
        m_performLazy = settings.getBoolean(CFG_PERFORM_LAZY);
        m_learningRateStrategy = LearningRateStrategies.valueOf(settings.getString(CFG_LEARNING_RATE_STRATEGY));
        m_initialLearningRate = settings.getDouble(CFG_INITIAL_LEARNING_RATE);
        m_learningRateDecay = settings.getDouble(CFG_LEARNING_RATE_DECAY);
        m_prior = Prior.valueOf(settings.getString(CFG_PRIOR));
        m_priorVariance = settings.getDouble(CFG_PRIOR_VARIANCE);

    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     * @param inputTableSpec The input table's spec.
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final DataTableSpec inputTableSpec) {
        m_targetColumn = settings.getString(CFG_TARGET, null);
        m_includedColumns.loadConfigurationInDialog(settings, inputTableSpec);
        m_targetReferenceCategory = settings.getDataCell(CFG_TARGET_REFERENCE_CATEGORY, null);
        m_sortTargetCategories = settings.getBoolean(CFG_SORT_TARGET_CATEGORIES, true);
        m_sortIncludesCategories = settings.getBoolean(CFG_SORT_INCLUDES_CATEGORIES, true);
        String solverString = settings.getString(CFG_SOLVER, Solver.SAG.name());
        m_solver = Solver.valueOf(solverString);
        m_maxEpoch = settings.getInt(CFG_MAX_EPOCH, 100);
        m_performLazy = settings.getBoolean(CFG_PERFORM_LAZY, false);
        m_learningRateStrategy = LearningRateStrategies.valueOf(settings.getString(
            CFG_LEARNING_RATE_STRATEGY, LearningRateStrategies.Fixed.name()));
        m_initialLearningRate = settings.getDouble(CFG_INITIAL_LEARNING_RATE, 1e-3);
        m_learningRateDecay = settings.getDouble(CFG_LEARNING_RATE_DECAY, 1.0);
        m_prior = Prior.valueOf(settings.getString(CFG_PRIOR, Prior.Uniform.name()));
        m_priorVariance = settings.getDouble(CFG_PRIOR_VARIANCE, 0.1);

    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(CFG_TARGET, m_targetColumn);
        m_includedColumns.saveConfiguration(settings);
        settings.addDataCell(CFG_TARGET_REFERENCE_CATEGORY, m_targetReferenceCategory);
        settings.addBoolean(CFG_SORT_TARGET_CATEGORIES, m_sortTargetCategories);
        settings.addBoolean(CFG_SORT_INCLUDES_CATEGORIES, m_sortIncludesCategories);
        settings.addString(CFG_SOLVER, m_solver.name());
        settings.addInt(CFG_MAX_EPOCH, m_maxEpoch);
        settings.addBoolean(CFG_PERFORM_LAZY, m_performLazy);
        settings.addString(CFG_LEARNING_RATE_STRATEGY, m_learningRateStrategy.name());
        settings.addString(CFG_PRIOR, m_prior.name());
        settings.addDouble(CFG_INITIAL_LEARNING_RATE, m_initialLearningRate);
        settings.addDouble(CFG_LEARNING_RATE_DECAY, m_learningRateDecay);
        settings.addDouble(CFG_PRIOR_VARIANCE, m_priorVariance);
    }

    /**
     * The target column which is the dependent variable.
     *
     * @return the targetColumn
     */
    public String getTargetColumn() {
        return m_targetColumn;
    }

    /**
     * Set the target column which is the dependent variable.
     *
     * @param targetColumn the targetColumn to set
     */
    public void setTargetColumn(final String targetColumn) {
        m_targetColumn = targetColumn;
    }

    /**
     * @return the targetReferenceCategory
     */
    public DataCell getTargetReferenceCategory() {
        return m_targetReferenceCategory;
    }


    /**
     * @param targetReferenceCategory the targetReferenceCategory to set
     */
    public void setTargetReferenceCategory(final DataCell targetReferenceCategory) {
        m_targetReferenceCategory = targetReferenceCategory;
    }


    /**
     * @return the sortTargetCategories
     */
    public boolean getSortTargetCategories() {
        return m_sortTargetCategories;
    }


    /**
     * @param sortTargetCategories the sortTargetCategories to set
     */
    public void setSortTargetCategories(final boolean sortTargetCategories) {
        m_sortTargetCategories = sortTargetCategories;
    }


    /**
     * @return the sortIncludesCategories
     */
    public boolean getSortIncludesCategories() {
        return m_sortIncludesCategories;
    }


    /**
     * @param sortIncludesCategories the sortIncludesCategories to set
     */
    public void setSortIncludesCategories(final boolean sortIncludesCategories) {
        m_sortIncludesCategories = sortIncludesCategories;
    }

    /**
     * @return the includedColumns
     */
    public DataColumnSpecFilterConfiguration getIncludedColumns() {
        return m_includedColumns;
    }

    /**
     * @param includedColumns the includedColumns to set
     */
    public void setIncludedColumns(final DataColumnSpecFilterConfiguration includedColumns) {
        m_includedColumns = includedColumns;
    }


    /**
     * @return the solver
     */
    public Solver getSolver() {
        return m_solver;
    }


    /**
     * @param solver the solver to set
     */
    public void setSolver(final Solver solver) {
        m_solver = solver;
    }


    /**
     * @return the maxEpoch
     */
    public int getMaxEpoch() {
        return m_maxEpoch;
    }


    /**
     * @param maxEpoch the maxEpoch to set
     */
    public void setMaxEpoch(final int maxEpoch) {
        m_maxEpoch = maxEpoch;
    }


    /**
     * @return the priorVariance
     */
    public double getPriorVariance() {
        return m_priorVariance;
    }


    /**
     * @param priorVariance the priorVariance to set
     */
    public void setPriorVariance(final double priorVariance) {
        m_priorVariance = priorVariance;
    }


    /**
     * @return the initialLearningRate
     */
    public double getInitialLearningRate() {
        return m_initialLearningRate;
    }


    /**
     * @param initialLearningRate the initialLearningRate to set
     */
    public void setInitialLearningRate(final double initialLearningRate) {
        m_initialLearningRate = initialLearningRate;
    }


    /**
     * @return the learningRateDecay
     */
    public double getLearningRateDecay() {
        return m_learningRateDecay;
    }


    /**
     * @param learningRateDecay the learningRateDecay to set
     */
    public void setLearningRateDecay(final double learningRateDecay) {
        m_learningRateDecay = learningRateDecay;
    }


    /**
     * @return the learningRateStrategy
     */
    public LearningRateStrategies getLearningRateStrategy() {
        return m_learningRateStrategy;
    }


    /**
     * @param learningRateStrategy the learningRateStrategy to set
     */
    public void setLearningRateStrategy(final LearningRateStrategies learningRateStrategy) {
        m_learningRateStrategy = learningRateStrategy;
    }


    /**
     * @return the prior
     */
    public Prior getPrior() {
        return m_prior;
    }


    /**
     * @param prior the prior to set
     */
    public void setPrior(final Prior prior) {
        m_prior = prior;
    }


    /**
     * @return the performLazy
     */
    public boolean isPerformLazy() {
        return m_performLazy;
    }


    /**
     * @param performLazy the performLazy to set
     */
    public void setPerformLazy(final boolean performLazy) {
        m_performLazy = performLazy;
    }
}

