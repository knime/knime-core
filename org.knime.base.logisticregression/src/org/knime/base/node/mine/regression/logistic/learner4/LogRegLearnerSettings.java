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
public class LogRegLearnerSettings {

    /**
     * The different solvers that the Logistic Regression node supports.
     *
     * @author Adrian Nembach, KNIME.com
     */
    public enum Solver {
        /**
         * Uses Iteratively reweighted least squares to estimate the model.
         * This algorithm is widely used but suffers from scaling problems if the number of features is high.
         * It also can't find solution to linearly separable problems or problems where there are more features than samples,
         * due to the lack of a regularization.
         */
        IRLS("Iteratively reweighted least squares", false, EnumSet.noneOf(LearningRateStrategies.class), EnumSet.noneOf(Prior.class)),
        /**
         * A fast and robust stochastic gradient descent variant that can be used for large scale problems.
         * It also allows to add regularization and is hence suitable for problems where the IRLS algorithm fails.
         * Limitations of SAG are that it has to store the raw gradients for all samples (sounds worse than it is since this
         * corresponds to a single double/float for each row) and it requires real random drawing of samples.
         */
        SAG("Stochastic average gradient", true, EnumSet.of(LearningRateStrategies.Fixed, LearningRateStrategies.LineSearch),
            EnumSet.allOf(Prior.class)),
        /**
         * Vanilla stochastic gradient descent. Converges slower than SAG but doesn't require to store any data.
         * Also supports regularization.
         */
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

        /**
         * Indicates whether the solver supports lazy calculation.
         * @return true if lazy calculation is supported
         */
        public boolean supportsLazy() {
            return m_supportsLazy;
        }

        /**
         * Returns an EnumSet containing the compatible priors.
         * @return EnumSet of compatible priors
         */
        public EnumSet<Prior> getCompatiblePriors() {
            return m_compatiblePriors;
        }

        /**
         * Returns an EnumSet of compatible {@link LearningRateStrategies}.
         * @return EnumSet of compatible {@link LearningRateStrategies}
         */
        public EnumSet<LearningRateStrategies> getCompatibleLearningRateStrategies() {
            return m_compatibleLRS;
        }

        /**
         * Indicates whether the solver is compatible with <b>lrs</b>.
         * @param lrs the {@link LearningRateStrategies} to check for compatibility
         * @return true if <b>lrs</b> is compatible
         */
        public boolean compatibleWith(final LearningRateStrategies lrs) {
            return m_compatibleLRS.contains(lrs);
        }

        /**
         * Indicates whether the solver is compatible with <b>prior</b>.
         * @param prior the {@link Prior} to check for compatibility
         * @return true if <b>prior</b> is compatible
         */
        public boolean compatibleWith(final Prior prior) {
            return m_compatiblePriors.contains(prior);
        }
    }

    /**
     * Enum that represents the different weight priors that are supported by the SG framework.
     * @author Adrian Nembach, KNIME.com
     */
    public enum Prior {
        /**
         * A Uniform prior for the weights is equivalent to using no prior at all.
         */
        Uniform(false),
        /**
         * A Gauss prior for the weights is equivalent to L2 regularization.
         */
        Gauss(true),
        /**
         * A Laplace prior for the weights is equivalent to L1 regularization.
         */
        Laplace(true);

        private final boolean m_hasVariance;
        private Prior(final boolean hasVariance) {
            m_hasVariance = hasVariance;
        }

        /**
         * Indicates whether the prior requires a value for the variance.
         * @return true if the prior requires a variance value
         */
        public boolean hasVariance() {
            return m_hasVariance;
        }
    }

    /**
     * Enum that contains the learning rate strategies supported by the SG framework and their properties.
     *
     * @author Adrian Nembach, KNIME.com
     */
    public enum LearningRateStrategies {
        /**
         * Uses the initial learning rate for the whole training.
         */
        Fixed(true, false),
        /**
         * Anneals the learning rate after each epoch.
         */
        Annealing(true, true),
        /**
         * Performs a line search for the Lipschitz constant and uses the estimate
         * to calculate the optimal learning rate. Can only be used with the SAG algorithm.
         */
        LineSearch(false, false);

        private final boolean m_hasInitialValue;
        private final boolean m_hasDecayRate;

        private LearningRateStrategies(final boolean hasInitialValue, final boolean hasDecayRate) {
            m_hasInitialValue = hasInitialValue;
            m_hasDecayRate = hasDecayRate;
        }

        /**
         * Indicates whether the strategy requires an initial value.
         * @return true if the strategy requires an initial value
         */
        public boolean hasInitialValue() {
            return m_hasInitialValue;
        }

        /**
         * Indicates whether the strategy requires a decay rate.
         * @return true if the strategy requires a decay rate
         */
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
    private static final String CFG_EPSILON = "epsilon";
    private static final String CFG_PRIOR = "prior";
    private static final String CFG_PRIOR_VARIANCE = "priorVariance";
    private static final String CFG_LEARNING_RATE_STRATEGY = "learningRateStrategy";
    private static final String CFG_INITIAL_LEARNING_RATE = "initialLearningRate";
    private static final String CFG_LEARNING_RATE_DECAY = "learningRateDecay";
    private static final String CFG_SEED = "seed";
    private static final String CFG_IN_MEMORY = "inMemory";
    private static final String CFG_CHUNK_SIZE = "chunkSize";
    private static final String CFG_CALC_COVMATRIX = "calcCovMatrix";

    static final Solver DEFAULT_SOLVER = Solver.SAG;
    static final boolean DEFAULT_PERFORM_LAZY = false;
    static final int DEFAULT_MAX_EPOCH = 100;
    static final double DEFAULT_EPSILON = 1e-5;
    static final LearningRateStrategies DEFAULT_LEARNINGRATE_STRATEGY = LearningRateStrategies.Fixed;
    static final double DEFAULT_INITIAL_LEARNING_RATE = 0.01;
    static final double DEFAULT_LEARNING_RATE_DECAY = 1;
    static final Prior DEFAULT_PRIOR = Prior.Uniform;
    static final double DEFAULT_PRIOR_VARIANCE = 0.1;
    static final boolean DEFAULT_IN_MEMORY = true;
    static final int DEFAULT_CHUNK_SIZE = 10000;
    static final boolean DEFAULT_CALC_COVMATRIX = true;


    private String m_targetColumn;

    /** The selected learning columns configuration. */
    private DataColumnSpecFilterConfiguration m_includedColumns = LogRegLearnerNodeModel.createDCSFilterConfiguration();

    /** The target reference category, if not set it is the last category. */
    private DataCell m_targetReferenceCategory;
    /** True when target categories should be sorted. */
    private boolean m_sortTargetCategories;
    /** True when categories of nominal data in the include list should be sorted. */
    private boolean m_sortIncludesCategories;

    // solver and relevant parameters
    private Solver m_solver;
    private int m_maxEpoch;
    private boolean m_performLazy;
    private double m_epsilon;
    private boolean m_calcCovMatrix;
    // learning rate strategy and relevant parameters
    private LearningRateStrategies m_learningRateStrategy;
    private double m_initialLearningRate;
    private double m_learningRateDecay;
    // prior and relevant parameters
    private Prior m_prior;
    private double m_priorVariance;

    // data handling
    private boolean m_inMemory;
    private Long m_seed;
    private int m_chunkSize;


    /**
     * Create default settings.
     */
    public LogRegLearnerSettings() {
        m_targetColumn = null;
        m_targetReferenceCategory = null;
        m_sortTargetCategories = true;
        m_sortIncludesCategories = true;
        m_solver = DEFAULT_SOLVER;
        m_maxEpoch = DEFAULT_MAX_EPOCH;
        m_performLazy = DEFAULT_PERFORM_LAZY;
        m_epsilon = DEFAULT_EPSILON;
        m_learningRateStrategy = DEFAULT_LEARNINGRATE_STRATEGY;
        m_initialLearningRate = DEFAULT_INITIAL_LEARNING_RATE;
        m_learningRateDecay = DEFAULT_LEARNING_RATE_DECAY;
        m_prior = DEFAULT_PRIOR;
        m_priorVariance = DEFAULT_PRIOR_VARIANCE;
        setInMemory(DEFAULT_IN_MEMORY);
        m_seed = System.currentTimeMillis();
        m_chunkSize = DEFAULT_CHUNK_SIZE;
        m_calcCovMatrix = DEFAULT_CALC_COVMATRIX;
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
        m_epsilon = settings.getDouble(CFG_EPSILON);
        m_learningRateStrategy = LearningRateStrategies.valueOf(settings.getString(CFG_LEARNING_RATE_STRATEGY));
        m_initialLearningRate = settings.getDouble(CFG_INITIAL_LEARNING_RATE);
        m_learningRateDecay = settings.getDouble(CFG_LEARNING_RATE_DECAY);
        m_prior = Prior.valueOf(settings.getString(CFG_PRIOR));
        m_priorVariance = settings.getDouble(CFG_PRIOR_VARIANCE);

        setInMemory(settings.getBoolean(CFG_IN_MEMORY));
        String seedS = settings.getString(CFG_SEED);
        Long seed;
        if (seedS == null) {
            seed = null;
        } else {
            try {
                seed = Long.parseLong(seedS);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Unable to parse seed \"" + seedS + "\"", nfe);
            }
        }
        m_seed = seed;

        m_chunkSize = settings.getInt(CFG_CHUNK_SIZE);

        m_calcCovMatrix = settings.getBoolean(CFG_CALC_COVMATRIX);


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
        String solverString = settings.getString(CFG_SOLVER, DEFAULT_SOLVER.name());
        m_solver = Solver.valueOf(solverString);
        m_maxEpoch = settings.getInt(CFG_MAX_EPOCH, DEFAULT_MAX_EPOCH);
        m_performLazy = settings.getBoolean(CFG_PERFORM_LAZY, DEFAULT_PERFORM_LAZY);
        m_epsilon = settings.getDouble(CFG_EPSILON, DEFAULT_EPSILON);
        m_learningRateStrategy = LearningRateStrategies.valueOf(settings.getString(
            CFG_LEARNING_RATE_STRATEGY, DEFAULT_LEARNINGRATE_STRATEGY.name()));
        m_initialLearningRate = settings.getDouble(CFG_INITIAL_LEARNING_RATE, DEFAULT_INITIAL_LEARNING_RATE);
        m_learningRateDecay = settings.getDouble(CFG_LEARNING_RATE_DECAY, DEFAULT_LEARNING_RATE_DECAY);
        m_prior = Prior.valueOf(settings.getString(CFG_PRIOR, DEFAULT_PRIOR.name()));
        m_priorVariance = settings.getDouble(CFG_PRIOR_VARIANCE, DEFAULT_PRIOR_VARIANCE);

        setInMemory(settings.getBoolean(CFG_IN_MEMORY, DEFAULT_IN_MEMORY));
        Long defSeed = System.currentTimeMillis();
        String seedS = settings.getString(CFG_SEED, Long.toString(defSeed));
        Long seed;
        if (seedS == null) {
            seed = null;
        } else {
            try {
                seed = Long.parseLong(seedS);
            } catch (NumberFormatException nfe) {
                seed = m_seed;
            }
        }
        m_seed = seed;

        m_chunkSize = settings.getInt(CFG_CHUNK_SIZE, DEFAULT_CHUNK_SIZE);

        m_calcCovMatrix = settings.getBoolean(CFG_CALC_COVMATRIX, DEFAULT_CALC_COVMATRIX);

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
        // if the solver doesn't support laziness, we shouldn't store that the calculations
        // should be performed lazily
        settings.addBoolean(CFG_PERFORM_LAZY, m_solver.m_supportsLazy && m_performLazy);
        settings.addDouble(CFG_EPSILON, m_epsilon);
        settings.addString(CFG_LEARNING_RATE_STRATEGY, m_learningRateStrategy.name());
        settings.addString(CFG_PRIOR, m_prior.name());
        settings.addDouble(CFG_INITIAL_LEARNING_RATE, m_initialLearningRate);
        settings.addDouble(CFG_LEARNING_RATE_DECAY, m_learningRateDecay);
        settings.addDouble(CFG_PRIOR_VARIANCE, m_priorVariance);
        settings.addBoolean(CFG_IN_MEMORY, m_inMemory);
        String seedS = m_seed.toString();
        settings.addString(CFG_SEED, seedS);
        settings.addInt(CFG_CHUNK_SIZE, m_chunkSize);

        settings.addBoolean(CFG_CALC_COVMATRIX, m_calcCovMatrix);
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
     * @return the epsilon
     */
    public double getEpsilon() {
        return m_epsilon;
    }


    /**
     * @param epsilon the epsilon to set
     */
    public void setEpsilon(final double epsilon) {
        m_epsilon = epsilon;
    }

    /**
     * Checks if <b>epsilon</b> is a valid value and throws an IllegalArgumentException if
     * the value is invalid.
     *
     * @param epsilon value to check
     * @throws IllegalArgumentException if <b>epsilon</b> is no valid value
     */
    public static void checkEpsilon(final double epsilon) throws IllegalArgumentException {
        if (epsilon < 0) {
            throw new IllegalArgumentException("Epsilon must be larger than 0");
        }
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


    /**
     * @return the inMemory
     */
    public boolean isInMemory() {
        return m_inMemory;
    }


    /**
     * @param inMemory the inMemory to set
     */
    public void setInMemory(final boolean inMemory) {
        m_inMemory = inMemory;
    }

    /**
     * @return the seed
     */
    public Long getSeed() {
        return m_seed;
    }


    /**
     * @param seed the seed to set
     */
    public void setSeed(final Long seed) {
        m_seed = seed;
    }


    /**
     * @return the chunkSize
     */
    public int getChunkSize() {
        return m_chunkSize;
    }


    /**
     * @param chunkSize the chunkSize to set
     */
    public void setChunkSize(final int chunkSize) {
        m_chunkSize = chunkSize;
    }


    /**
     * @return the calcCovMatrix
     */
    public boolean isCalcCovMatrix() {
        return m_calcCovMatrix;
    }


    /**
     * @param calcCovMatrix the calcCovMatrix to set
     */
    public void setCalcCovMatrix(final boolean calcCovMatrix) {
        m_calcCovMatrix = calcCovMatrix;
    }
}

