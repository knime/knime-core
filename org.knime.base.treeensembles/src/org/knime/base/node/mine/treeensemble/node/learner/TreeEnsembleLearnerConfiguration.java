/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 17, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.node.learner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.random.JDKRandomGenerator;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;
import org.knime.base.node.mine.treeensemble.data.TreeData;
import org.knime.base.node.mine.treeensemble.learner.GainImpurity;
import org.knime.base.node.mine.treeensemble.learner.GainRatioImpurity;
import org.knime.base.node.mine.treeensemble.learner.GiniImpurity;
import org.knime.base.node.mine.treeensemble.learner.IImpurity;
import org.knime.base.node.mine.treeensemble.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble.sample.column.AllColumnSampleStrategy;
import org.knime.base.node.mine.treeensemble.sample.column.ColumnSampleStrategy;
import org.knime.base.node.mine.treeensemble.sample.column.RFSubsetColumnSampleStrategy;
import org.knime.base.node.mine.treeensemble.sample.column.SubsetColumnSampleStrategy;
import org.knime.base.node.mine.treeensemble.sample.row.DefaultRowSample;
import org.knime.base.node.mine.treeensemble.sample.row.RowSample;
import org.knime.base.node.mine.treeensemble.sample.row.SubsetNoReplacementRowSample;
import org.knime.base.node.mine.treeensemble.sample.row.SubsetWithReplacementRowSample;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeEnsembleLearnerConfiguration {

    /**  */
    private static final String KEY_NR_HILITE_PATTERNS = "nrHilitePatterns";

    /**  */
    private static final String KEY_IGNORE_COLUMNS_WITHOUT_DOMAIN =
        "ignoreColumnsWithoutDomain";

    /**  */
    private static final String KEY_IS_DATA_SELECTION_WITH_REPLACEMENT =
            "isDataSelectionWithReplacement";

    private static final String KEY_IS_USE_DIFFERENT_ATTRIBUTES_AT_EACH_NODE =
            "isUseDifferentAttributesAtEachNode";

    private static final String KEY_INCLUDE_ALL_COLUMNS = "includeAllColumns";

    private static final String KEY_INCLUDE_COLUMNS = "includeColumns";

    private static final String KEY_FINGERPRINT_COLUMN = "fingerprintColumn";

    private static final String KEY_USE_AVERAGE_SPLIT_POINTS =
            "useAverageSplitPoints";

    private static final String KEY_SPLIT_CRITERION = "splitCriterion";

    private static final String KEY_NR_MODELS = "nrModels";

    private static final String KEY_COLUMN_FRACTION_LINEAR =
            "columnFractionPerTree";

    private static final String KEY_COLUMN_ABSOLUTE =
        "columnAbsolutePerTree";

    private static final String KEY_ROOT_COLUMN = "hardCodedRootColumn";

    private static final String KEY_DATA_FRACTION = "dataFraction";

    private static final String KEY_MAX_LEVELS = "maxLevels";

    private static final String KEY_MIN_NODE_SIZE = "minNodeSize";

    private static final String KEY_MIN_CHILD_SIZE = "minChildSize";

    private static final String KEY_SEED = "seed";

    private static final String KEY_TARGET_COLUMN = "targetColumn";

    private static final String KEY_COLUMN_SAMPLING_MODE = "columnSamplingMode";

    private static final String KEY_SAVE_TARGET_DISTRIBUTION_IN_NODES = "saveTargetDistributionInNodes";

    public enum SplitCriterion {
        InformationGain("Information Gain"),
        InformationGainRatio("Information Gain Ratio"),
        Gini("Gini Index");

        private final String m_string;

        private SplitCriterion(final String str) {
            m_string = str;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_string;
        };
    }

    public enum ColumnSamplingMode {
        Linear, SquareRoot, Absolute, None
    }

    /** indicates max level parameter is not defined. */
    public static final int MAX_LEVEL_INFINITE = -1;

    /** indicates minimum node size parameter is not defined. */
    public static final int MIN_NODE_SIZE_UNDEFINED = -1;

    /** indicates minimum leaf size parameter is not defined. */
    public static final int MIN_CHILD_SIZE_UNDEFINED = -1;

    static final int DEF_MAX_LEVEL = MAX_LEVEL_INFINITE;

    public static final double DEF_DATA_FRACTION = 1.0;

    public static final double DEF_COLUMN_FRACTION = 1.0;

    public static final ColumnSamplingMode DEF_COLUMN_SAMPLING_MODE = ColumnSamplingMode.SquareRoot;

    public static final int DEF_COLUMN_ABSOLUTE = 10;

    public static final int DEF_NR_MODELS = 100;

    public static final boolean DEF_AVERAGE_SPLIT_POINTS = true;

    public static final boolean DEF_SAVE_TARGET_DISTRIBUTION_IN_NODES = false;

    private String m_targetColumn;

    private Long m_seed = System.currentTimeMillis();

    private int m_maxLevels = DEF_MAX_LEVEL;

    /** see {@link #getMinNodeSize(int)}. */
    private int m_minNodeSize = MIN_NODE_SIZE_UNDEFINED;

    private int m_minChildSize = MIN_CHILD_SIZE_UNDEFINED;

    private double m_dataFractionPerTree = DEF_DATA_FRACTION;

    private boolean m_isDataSelectionWithReplacement;

    private double m_columnFractionLinearValue = DEF_COLUMN_FRACTION;

    private int m_columnAbsoluteValue = DEF_COLUMN_ABSOLUTE;

    private ColumnSamplingMode m_columnSamplingMode = ColumnSamplingMode.SquareRoot;

    private boolean m_isUseDifferentAttributesAtEachNode;

    private int m_nrModels = DEF_NR_MODELS;

    private boolean m_useAverageSplitPoints = DEF_AVERAGE_SPLIT_POINTS;

    private SplitCriterion m_splitCriterion;

    private String m_fingerprintColumn;

    private String[] m_includeColumns;

    private boolean m_includeAllColumns;

    private String m_hardCodedRootColumn;

    private boolean m_ignoreColumnsWithoutDomain;

    private int m_nrHilitePatterns;

    private boolean m_saveTargetDistributionInNodes = DEF_SAVE_TARGET_DISTRIBUTION_IN_NODES;

    private final boolean m_isRegression;

    /**
     *  */
    public TreeEnsembleLearnerConfiguration(final boolean isRegression) {
        m_isRegression = isRegression;
    }

    /** @return the targetColumn (the categorical column to be learned). */
    public String getTargetColumn() {
        return m_targetColumn;
    }

    /**
     * @param targetColumn the targetColumn to set.
     * @throws InvalidSettingsException If arg is null or empty.
     */
    public void setTargetColumn(final String targetColumn)
            throws InvalidSettingsException {
        if (targetColumn == null || targetColumn.isEmpty()) {
            throw new InvalidSettingsException("Target attribute name "
                    + "is null or empty");
        }
        m_targetColumn = targetColumn;
    }

    /**
     * @return the random seed to be used for deterministic behavior or null to
     *         have another random initialization with each run.
     */
    public Long getSeed() {
        return m_seed;
    }

    /** @param seed the seed to set, see {@link #getSeed()} for details. */
    public void setSeed(final Long seed) {
        m_seed = seed;
    }

    /**
     * @return name of the fixed root attribute or null (which should be the
     *         default unless the user knows what attribute is supposedly best).
     */
    public String getHardCodedRootColumn() {
        return m_hardCodedRootColumn;
    }

    /**
     * @param hardCodedRootColumn the hardCodedRootColumn to set, see
     *            {@link #getHardCodedRootColumn()}.
     */
    public void setHardCodedRootColumn(final String hardCodedRootColumn) {
        m_hardCodedRootColumn = hardCodedRootColumn;
    }

    /**
     * The maximum tree depth (root has level 1) or {@link #MAX_LEVEL_INFINITE}
     * if complete tree is to be built. Value is strictly larger than 0 (or
     * {@link #MAX_LEVEL_INFINITE}).
     *
     * @return tree depth as described above
     */
    public int getMaxLevels() {
        return m_maxLevels;
    }

    /**
     * @param maxLevels the maxLevels to set, see {@link #getMaxLevels()} for
     *            details.
     * @throws InvalidSettingsException if bounds are violated, see get method
     *             for details.
     */
    public void setMaxLevels(final int maxLevels)
            throws InvalidSettingsException {
        if (maxLevels == MAX_LEVEL_INFINITE) {
            // ok
        } else if (maxLevels <= 0) {
            throw new InvalidSettingsException("Invalid value for number of "
                    + "maximum tree depth (#level): " + m_maxLevels);
        }
        m_maxLevels = maxLevels;
    }

    /** The minimum number of objects in a node so that a split is attempted. Not evaluated if
     * value is {@link #MIN_NODE_SIZE_UNDEFINED} AND {@link #getMinChildSize()} is unset;
     * otherwise it needs to be twice as large.
     * @return the minNodeSize */
    public int getMinNodeSize() {
        return m_minNodeSize;
    }

    /** The minimum objects in either child node, at most 1/2 {@link #getMinNodeSize()}.
     * @return the minChildSize value or {@link #MIN_CHILD_SIZE_UNDEFINED}.
     */
    public int getMinChildSize() {
        return m_minChildSize;
    }

    /** See size values for minimum split and child node.
     * @param minNodeSize the minChildSize to set (or {@link #MIN_CHILD_SIZE_UNDEFINED}).
     * @param minChildSize the minChildSize to set (or {@link #MIN_CHILD_SIZE_UNDEFINED}).
     * @throws InvalidSettingsException if either value is invalid or min child size is larger than minnodesize/2
     */
    public void setMinSizes(final int minNodeSize, final int minChildSize) throws InvalidSettingsException {
        if (minNodeSize == MIN_NODE_SIZE_UNDEFINED) {
            // ok
        } else if (minNodeSize <= 0) {
            throw new InvalidSettingsException("Invalid minimum node size: " + minNodeSize);
        }

        if (minChildSize == MIN_CHILD_SIZE_UNDEFINED) {
            // ok
        } else if (minChildSize <= 0) {
            throw new InvalidSettingsException("Invalid minimum child size: " + minChildSize);
        } else if (minNodeSize > 0 && minChildSize > minNodeSize / 2) {
            throw new InvalidSettingsException("Invalid minimum child size (" + minChildSize
                       + "); must be at most 2 x minimum node size (" + minNodeSize +")");
        }

        if (minChildSize > 0 && minNodeSize == MIN_NODE_SIZE_UNDEFINED) {
            m_minNodeSize = 2 * minChildSize;
        } else {
            m_minNodeSize = minNodeSize;
        }
        m_minChildSize = minChildSize;
    }

    /**
     * The fraction of data that is used to train a model (each model in the bag
     * gets a different (overlapping) portion of the data).
     *
     * @return The above value. It must be 0 &lt; value &lt;= 1.
     */
    public double getDataFractionPerTree() {
        return m_dataFractionPerTree;
    }

    /**
     * @param value described in {@link #getDataFractionPerTree()}.
     * @throws InvalidSettingsException If value is invalid, see get method.
     */
    public void setDataFractionPerTree(final double value)
            throws InvalidSettingsException {
        if (0.0 < value && value <= 1.0) {
            m_dataFractionPerTree = value;
        } else {
            throw new InvalidSettingsException("Invalid value for \"fraction "
                    + "of data\", must be (0, 1]: " + value);
        }
    }

    /** @return the isDataSelectionWithReplacement */
    public boolean isDataSelectionWithReplacement() {
        return m_isDataSelectionWithReplacement;
    }

    /** @param isDataSelectionWithReplacement the value to set */
    public void setDataSelectionWithReplacement(
            final boolean isDataSelectionWithReplacement) {
        m_isDataSelectionWithReplacement = isDataSelectionWithReplacement;
    }

    /** @return the columnSamplingMode */
    public ColumnSamplingMode getColumnSamplingMode() {
        return m_columnSamplingMode;
    }

    /** @return the isUseDifferentAttributesAtEachNode */
    public boolean isUseDifferentAttributesAtEachNode() {
        return m_isUseDifferentAttributesAtEachNode;
    }

    /** @param isUseDifferentAttributesAtEachNode the value to set */
    public void setUseDifferentAttributesAtEachNode(
            final boolean isUseDifferentAttributesAtEachNode) {
        m_isUseDifferentAttributesAtEachNode =
                isUseDifferentAttributesAtEachNode;
    }

    /**
     * @param columnSamplingMode the columnSamplingMode to set
     * @throws InvalidSettingsException If arg is null
     */
    public void setColumnSamplingMode(
            final ColumnSamplingMode columnSamplingMode)
            throws InvalidSettingsException {
        if (columnSamplingMode == null) {
            throw new InvalidSettingsException("No column sampling mode set");
        }
        m_columnSamplingMode = columnSamplingMode;
    }

    /**
     * The fraction of number of columns/attributes that is used to train a
     * model (each model in the bag learns on a different (overlapping) portion
     * of the attributes). This value is only of relevance if
     * {@link #getColumnSamplingMode()} is {@link ColumnSamplingMode#Linear}.
     *
     * @return The above value. It must be 0 &lt; value &lt;= 1.
     */
    public double getColumnFractionLinearValue() {
        return m_columnFractionLinearValue;
    }

    /**
     * @param value see {@link #getColumnFractionLinearValue()}.
     * @throws InvalidSettingsException If out of bounds, see get method.
     */
    public void setColumnFractionLinearValue(final double value)
            throws InvalidSettingsException {
        if (0.0 < value && value <= 1.0) {
            m_columnFractionLinearValue = value;
        } else {
            throw new InvalidSettingsException("Invalid value for \"fraction "
                    + "of columns\", must be (0, 1]: " + value);
        }
    }

    /** Similar to {@link #getColumnFractionLinearValue()} but with a fixed
     * number of attributes (e.g. controlled by a variable). If this number is
     * larger than the number of appropriate columns/attributes, all atts will
     * be used.
     * <p>Only relevant if {@link #getColumnSamplingMode()} is
     * {@link ColumnSamplingMode#Absolute}.
     * @return number of columns (strictly larger 0) */
    public int getColumnAbsoluteValue() {
        return m_columnAbsoluteValue;
    }

    /** See {@link #getColumnAbsoluteValue()}.
     * @param columnAbsoluteValue the value
     * @throws InvalidSettingsException If out of bounds (see get method). */
    public void setColumnAbsoluteValue(final int columnAbsoluteValue)
    throws InvalidSettingsException {
        if (columnAbsoluteValue < 0) {
            throw new InvalidSettingsException("Invalid number of attributes: "
                    + columnAbsoluteValue);
        }
        m_columnAbsoluteValue = columnAbsoluteValue;
    }

    /** @return the number of models to be learned (> 0). */
    public int getNrModels() {
        return m_nrModels;
    }

    /**
     * @param value the nrModels to learn, see {@link #getNrModels()}.
     * @throws InvalidSettingsException If out of bounds, see get method.
     */
    public void setNrModels(final int value) throws InvalidSettingsException {
        if (value <= 0) {
            throw new InvalidSettingsException("Invalid # models: " + value);
        }
        m_nrModels = value;
    }

    /** @return the splitCriterion */
    public SplitCriterion getSplitCriterion() {
        return m_splitCriterion;
    }

    public IImpurity createImpurityCriterion() {
        switch (m_splitCriterion) {
        case InformationGain:
            return GainImpurity.INSTANCE;
        case InformationGainRatio:
            return GainRatioImpurity.INSTANCE;
        case Gini:
            return GiniImpurity.INSTANCE;
        default:
            throw new IllegalStateException("Unsupport split criterion: "
                    + m_splitCriterion);
        }
    }

    /** @param splitCriterion the splitCriterion to set */
    public void setSplitCriterion(final SplitCriterion splitCriterion)
            throws InvalidSettingsException {
        if (splitCriterion == null) {
            throw new InvalidSettingsException(
                    "Split Criterion must not be null");
        }
        m_splitCriterion = splitCriterion;
    }

    /** @return the useAverageSplitPoints */
    public boolean isUseAverageSplitPoints() {
        return m_useAverageSplitPoints;
    }

    /** @param useAverageSplitPoints the useAverageSplitPoints to set */
    public void setUseAverageSplitPoints(final boolean useAverageSplitPoints) {
        m_useAverageSplitPoints = useAverageSplitPoints;
    }

    /**
     * @return the name of the fingerprint column to learn from (each bit
     *         position is an binary attribute) or null if to learn from a set
     *         of columns.
     */
    public String getFingerprintColumn() {
        return m_fingerprintColumn;
    }

    /**
     * @param fingerprintColumn the fingerprintColumn to set, see
     *            {@link #getFingerprintColumn()}.
     */
    public void setFingerprintColumn(final String fingerprintColumn) {
        m_fingerprintColumn = fingerprintColumn;
    }

    /**
     * @return The names of the columns in the input table to use as attributes,
     *         null when learning from fingerprint data. It might contain the
     *         target column, which will be ignored as learning column.
     */
    public String[] getIncludeColumns() {
        return m_includeColumns;
    }

    /**
     * @param includeColumns the includeColumns to set, see
     *            {@link #isIncludeAllColumns()}.
     */
    public void setIncludeColumns(final String[] includeColumns) {
        m_includeColumns = includeColumns;
    }

    /**
     * @return the includeAllColumns flag. If true all appropriate columns will
     *         be used as attributes.
     */
    public boolean isIncludeAllColumns() {
        return m_includeAllColumns;
    }

    /**
     * @param includeAllColumns the flag as described in
     *            {@link #isIncludeAllColumns()}.
     */
    public void setIncludeAllColumns(final boolean includeAllColumns) {
        m_includeAllColumns = includeAllColumns;
    }

    /** @return the nrHilitePatterns or -1 if hiliting is disabled. */
    public int getNrHilitePatterns() {
        return m_nrHilitePatterns;
    }

    /** @param nrHilitePatterns the nrHilitePatterns to set
     * @see #getNrHilitePatterns() */
    public void setNrHilitePatterns(final int nrHilitePatterns) {
        m_nrHilitePatterns = nrHilitePatterns;
    }

    /** Whether the model should save the target distribution in each tree node (when classification). This is
     * very memory consuming and only useful when exporting to PMML or when viewing at distributions in the tree view.
     *
     * <p>Only applies for classification.
     * @return that property.
     */
    public boolean isSaveTargetDistributionInNodes() {
        return m_saveTargetDistributionInNodes;
    }

    /** Setter for {@link #isSaveTargetDistributionInNodes()}.
     * @param value The value
     */
    public void setSaveTargetDistributionInNodes(final boolean value) {
        m_saveTargetDistributionInNodes = value;
    }

    /** @return the ignoreColumnsWithoutDomain */
    public boolean isIgnoreColumnsWithoutDomain() {
        return m_ignoreColumnsWithoutDomain;
    }

    /** @param value the ignoreColumnsWithoutDomain to set */
    public void setIgnoreColumnsWithoutDomain(final boolean value) {
        m_ignoreColumnsWithoutDomain = value;
    }

    public void save(final NodeSettingsWO settings) {
        settings.addString(KEY_TARGET_COLUMN, m_targetColumn);
        String seedS = m_seed == null ? null : Long.toString(m_seed);
        settings.addString(KEY_SEED, seedS);
        settings.addInt(KEY_MAX_LEVELS, m_maxLevels);
        settings.addInt(KEY_MIN_NODE_SIZE, m_minNodeSize);
        settings.addInt(KEY_MIN_CHILD_SIZE, m_minChildSize);
        settings.addDouble(KEY_DATA_FRACTION, m_dataFractionPerTree);
        settings.addBoolean(KEY_IS_DATA_SELECTION_WITH_REPLACEMENT, m_isDataSelectionWithReplacement);
        settings.addString(KEY_ROOT_COLUMN, m_hardCodedRootColumn);
        settings.addString(KEY_COLUMN_SAMPLING_MODE, m_columnSamplingMode.name());
        settings.addDouble(KEY_COLUMN_FRACTION_LINEAR, m_columnFractionLinearValue);
        settings.addInt(KEY_COLUMN_ABSOLUTE, m_columnAbsoluteValue);
        settings.addBoolean(KEY_IS_USE_DIFFERENT_ATTRIBUTES_AT_EACH_NODE, m_isUseDifferentAttributesAtEachNode);
        settings.addInt(KEY_NR_MODELS, m_nrModels);
        settings.addString(KEY_SPLIT_CRITERION, m_splitCriterion.name());
        settings.addBoolean(KEY_USE_AVERAGE_SPLIT_POINTS, m_useAverageSplitPoints);
        settings.addString(KEY_FINGERPRINT_COLUMN, m_fingerprintColumn);
        settings.addStringArray(KEY_INCLUDE_COLUMNS, m_includeColumns);
        settings.addBoolean(KEY_INCLUDE_ALL_COLUMNS, m_includeAllColumns);
        settings.addBoolean(KEY_IGNORE_COLUMNS_WITHOUT_DOMAIN, m_ignoreColumnsWithoutDomain);
        settings.addInt(KEY_NR_HILITE_PATTERNS, m_nrHilitePatterns);
        settings.addBoolean(KEY_SAVE_TARGET_DISTRIBUTION_IN_NODES, m_saveTargetDistributionInNodes);
    }

    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setTargetColumn(settings.getString(KEY_TARGET_COLUMN));
        String seedS = settings.getString(KEY_SEED);
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
        setSeed(seed);
        setMaxLevels(settings.getInt(KEY_MAX_LEVELS));
        // added after given to first prototype to users but before first public release
        setMinSizes(settings.getInt(KEY_MIN_NODE_SIZE, MIN_NODE_SIZE_UNDEFINED),
                     settings.getInt(KEY_MIN_CHILD_SIZE, MIN_CHILD_SIZE_UNDEFINED));
        setHardCodedRootColumn(settings.getString(KEY_ROOT_COLUMN));
        setDataFractionPerTree(settings.getDouble(KEY_DATA_FRACTION));
        setDataSelectionWithReplacement(settings.getBoolean(KEY_IS_DATA_SELECTION_WITH_REPLACEMENT));

        String columnSamplMode = settings.getString(KEY_COLUMN_SAMPLING_MODE);
        final ColumnSamplingMode colSamplingMode;
        try {
            colSamplingMode = ColumnSamplingMode.valueOf(columnSamplMode);
        } catch (Exception e) {
            throw new InvalidSettingsException("Unable to read column sampling mode", e);
        }
        setColumnSamplingMode(colSamplingMode);
        switch (colSamplingMode) {
            case Linear:
                final double v = settings.getDouble(KEY_COLUMN_FRACTION_LINEAR);
                setColumnFractionLinearValue(v);
                break;
            case Absolute:
                final int a = settings.getInt(KEY_COLUMN_ABSOLUTE);
                setColumnAbsoluteValue(a);
                break;
            default:
                // ignored
        }
        setUseDifferentAttributesAtEachNode(settings.getBoolean(KEY_IS_USE_DIFFERENT_ATTRIBUTES_AT_EACH_NODE));
        setNrModels(settings.getInt(KEY_NR_MODELS));
        String splitCriterionS = settings.getString(KEY_SPLIT_CRITERION);
        try {
            m_splitCriterion = SplitCriterion.valueOf(splitCriterionS);
        } catch (Exception e) {
            throw new InvalidSettingsException("Unable to parse split " + "criterion \"" + splitCriterionS + "\"", e);
        }
        setUseAverageSplitPoints(settings.getBoolean(KEY_USE_AVERAGE_SPLIT_POINTS));
        setFingerprintColumn(settings.getString(KEY_FINGERPRINT_COLUMN));
        setIncludeColumns(settings.getStringArray(KEY_INCLUDE_COLUMNS));
        setIncludeAllColumns(settings.getBoolean(KEY_INCLUDE_ALL_COLUMNS));
        if (m_fingerprintColumn != null) {
            // use fingerprint data, OK
        } else if (m_includeColumns != null && m_includeColumns.length > 0) {
            // some attributes set, OK
        } else if (m_includeAllColumns) {
            // use all appropriate columns, OK
        } else {
            throw new InvalidSettingsException("No attribute columns selected");
        }
        // added after first preview, be backward compatible (true as default)
        setIgnoreColumnsWithoutDomain(settings.getBoolean(KEY_IGNORE_COLUMNS_WITHOUT_DOMAIN, true));
        // added after first preview, be backward compatible (none as default)
        setNrHilitePatterns(settings.getInt(KEY_NR_HILITE_PATTERNS, -1));
        // added in 2.10
        setSaveTargetDistributionInNodes(settings.getBoolean(KEY_SAVE_TARGET_DISTRIBUTION_IN_NODES, true));
    }

    public void loadInDialog(final NodeSettingsRO settings,
                             final DataTableSpec inSpec) throws NotConfigurableException {
        String defTargetColumn = null;
        String defFingerprintColumn = null;
        boolean hasAttributeColumns = false;

        // guess defaults:
        // traverse columns backwards; assign last (i.e. first-seen) appropriate
        // column as target, use any subsequent as valid learning attribute
        Class<? extends DataValue> targetClass = getRequiredTargetClass();
        for (int i = inSpec.getNumColumns() - 1; i >= 0; i--) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);
            DataType colType = colSpec.getType();
            String colName = colSpec.getName();
            if (colType.isCompatible(BitVectorValue.class)) {
                defFingerprintColumn = colName;
            } else if (colType.isCompatible(NominalValue.class) || colType.isCompatible(DoubleValue.class)) {
                if (colType.isCompatible(targetClass)) {
                    if (defTargetColumn == null) { // first categorical column
                        defTargetColumn = colName;
                    } else {
                        hasAttributeColumns = true;
                    }
                } else {
                    hasAttributeColumns = true;
                }
            }
        }
        if (defTargetColumn == null) {
            throw new NotConfigurableException("No categorical data in input "
                    + "(node not connected?) -- unable to configure.");
        }
        if (!hasAttributeColumns && defFingerprintColumn == null) {
            throw new NotConfigurableException("No appropriate learning column "
                    + "in input (need to have at least one additional "
                    + "numeric/categorical column or fingerprint data)");
        }

        // assign fields:
        m_targetColumn = settings.getString(KEY_TARGET_COLUMN, defTargetColumn);
        DataColumnSpec targetColSpec = inSpec.getColumnSpec(m_targetColumn);
        if (targetColSpec == null || !targetColSpec.getType().isCompatible(targetClass)) {
            m_targetColumn = defTargetColumn;
        }

        String hardCodedRootColumn = settings.getString(KEY_ROOT_COLUMN, null);
        if (inSpec.getColumnSpec(hardCodedRootColumn) == null) {
            m_hardCodedRootColumn = null;
        } else {
            m_hardCodedRootColumn = hardCodedRootColumn;
        }

        m_fingerprintColumn = settings.getString(KEY_FINGERPRINT_COLUMN, defFingerprintColumn);
        if (m_fingerprintColumn == null) {
            // null in node settings - leave it
        } else {
            DataColumnSpec fpColSpec = inSpec.getColumnSpec(m_fingerprintColumn);
            if (fpColSpec == null || !fpColSpec.getType().isCompatible(BitVectorValue.class)) {
                m_fingerprintColumn = defFingerprintColumn;
            }
        }

        m_includeColumns = settings.getStringArray(KEY_INCLUDE_COLUMNS, (String[])null);
        m_includeAllColumns = settings.getBoolean(KEY_INCLUDE_ALL_COLUMNS, true);

        Long defSeed = System.currentTimeMillis();
        String seedS = settings.getString(KEY_SEED, Long.toString(defSeed));
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

        m_maxLevels = settings.getInt(KEY_MAX_LEVELS, DEF_MAX_LEVEL);
        if (m_maxLevels != MAX_LEVEL_INFINITE && m_maxLevels <= 0) {
            m_maxLevels = DEF_MAX_LEVEL;
        }

        int minNodeSize = settings.getInt(KEY_MIN_NODE_SIZE, MIN_NODE_SIZE_UNDEFINED);
        int minChildSize = settings.getInt(KEY_MIN_CHILD_SIZE, MIN_CHILD_SIZE_UNDEFINED);
        try {
            setMinSizes(minNodeSize, minChildSize);
        } catch (InvalidSettingsException e) {
            m_minNodeSize = MIN_NODE_SIZE_UNDEFINED;
            m_minChildSize = MIN_CHILD_SIZE_UNDEFINED;
        }

        m_dataFractionPerTree = settings.getDouble(KEY_DATA_FRACTION, DEF_DATA_FRACTION);
        if (m_dataFractionPerTree <= 0.0 || m_dataFractionPerTree > 1.0) {
            m_dataFractionPerTree = DEF_DATA_FRACTION;
        }

        m_columnAbsoluteValue = settings.getInt(KEY_COLUMN_ABSOLUTE, DEF_COLUMN_ABSOLUTE);
        if (m_columnAbsoluteValue <= 0) {
            m_columnAbsoluteValue = DEF_COLUMN_ABSOLUTE;
        }

        m_isDataSelectionWithReplacement = settings.getBoolean(KEY_IS_DATA_SELECTION_WITH_REPLACEMENT, true);

        ColumnSamplingMode defColSamplingMode = DEF_COLUMN_SAMPLING_MODE;
        ColumnSamplingMode colSamplingMode = defColSamplingMode;
        String colSamplingModeS = settings.getString(KEY_COLUMN_SAMPLING_MODE, null);
        if (colSamplingModeS == null) {
            colSamplingMode = defColSamplingMode;
        } else {
            try {
                colSamplingMode = ColumnSamplingMode.valueOf(colSamplingModeS);
            } catch (Exception e) {
                colSamplingMode = defColSamplingMode;
            }
        }
        double colFracLinValue;
        switch (colSamplingMode) {
            case Linear:
                colFracLinValue = settings.getDouble(KEY_COLUMN_FRACTION_LINEAR, DEF_COLUMN_FRACTION);
                if (colFracLinValue <= 0.0 || colFracLinValue > 1.0) {
                    colFracLinValue = DEF_COLUMN_FRACTION;
                }
                break;
            default:
                colFracLinValue = DEF_COLUMN_FRACTION;
        }
        m_columnSamplingMode = colSamplingMode;
        m_columnFractionLinearValue = colFracLinValue;
        m_isUseDifferentAttributesAtEachNode = settings.getBoolean(KEY_IS_USE_DIFFERENT_ATTRIBUTES_AT_EACH_NODE, true);

        m_nrModels = settings.getInt(KEY_NR_MODELS, DEF_NR_MODELS);
        if (m_nrModels <= 0) {
            m_nrModels = DEF_NR_MODELS;
        }

        SplitCriterion defSplitCriterion = SplitCriterion.InformationGainRatio;
        String splitCriterionS = settings.getString(KEY_SPLIT_CRITERION, defSplitCriterion.name());
        SplitCriterion splitCriterion;
        if (splitCriterionS == null) {
            splitCriterion = defSplitCriterion;
        } else {
            try {
                splitCriterion = SplitCriterion.valueOf(splitCriterionS);
            } catch (Exception e) {
                splitCriterion = defSplitCriterion;
            }
        }
        m_splitCriterion = splitCriterion;
        m_useAverageSplitPoints = settings.getBoolean(KEY_USE_AVERAGE_SPLIT_POINTS, DEF_AVERAGE_SPLIT_POINTS);

        if (m_fingerprintColumn != null) {
            // use fingerprint data, OK
        } else if (m_includeColumns != null && m_includeColumns.length > 0) {
            // some attributes set, OK
        } else if (m_includeAllColumns) {
            // use all appropriate columns, OK
        } else if (defFingerprintColumn != null) {
            // no valid columns but fingerprint column found - use it
            m_fingerprintColumn = defFingerprintColumn;
        } else {
            m_includeAllColumns = true;
        }
        m_ignoreColumnsWithoutDomain = settings.getBoolean(KEY_IGNORE_COLUMNS_WITHOUT_DOMAIN, true);
        m_nrHilitePatterns = settings.getInt(KEY_NR_HILITE_PATTERNS, -1);
        m_saveTargetDistributionInNodes =
                settings.getBoolean(KEY_SAVE_TARGET_DISTRIBUTION_IN_NODES, DEF_SAVE_TARGET_DISTRIBUTION_IN_NODES);
    }

    public FilterLearnColumnRearranger filterLearnColumns(final DataTableSpec spec)
            throws InvalidSettingsException {
        // TODO return type should be a derived class of ColumnRearranger
        // (ColumnRearranger is a final class in v2.5)
        if (m_targetColumn == null) {
            throw new InvalidSettingsException("Target column not set");
        }
        DataColumnSpec targetCol = spec.getColumnSpec(m_targetColumn);
        if (targetCol == null || !targetCol.getType().isCompatible(getRequiredTargetClass())) {
            throw new InvalidSettingsException("Target column \"" + m_targetColumn
                       + "\" does not exist or is not of the " + "correct type");
        }
        List<String> noDomainColumns = new ArrayList<String>();
        FilterLearnColumnRearranger rearranger = new FilterLearnColumnRearranger(spec);
        if (m_fingerprintColumn == null) { // use ordinary data
            Set<String> incl =
                    m_includeAllColumns ? null : new HashSet<String>(
                            Arrays.asList(m_includeColumns));
            for (DataColumnSpec col : spec) {
                String colName = col.getName();
                if (colName.equals(m_targetColumn)) {
                    continue;
                }
                DataType type = col.getType();
                boolean ignoreColumn = false;
                boolean isAppropriateType = type.isCompatible(DoubleValue.class)
                || type.isCompatible(NominalValue.class);
                if (m_includeAllColumns) {
                    if (isAppropriateType) {
                        if (shouldIgnoreLearnColumn(col)) {
                            ignoreColumn = true;
                            noDomainColumns.add(colName);
                        } else {
                            // accept column
                        }
                    } else {
                        ignoreColumn = true;
                    }
                } else {
                    if (incl.remove(colName)) { // is attribute in selection set
                        // accept unless type mismatch
                        if (!isAppropriateType) {
                            throw new InvalidSettingsException(
                                    "Attribute column \"" + colName + "\" is "
                                    + "not of the expected type (must be "
                                    + "numeric or nominal).");
                        } else if (shouldIgnoreLearnColumn(col)) {
                            ignoreColumn = true;
                            noDomainColumns.add(colName);
                        } else {
                            // accept
                        }
                    } else {
                        ignoreColumn = true;
                    }
                }
                if (ignoreColumn) {
                    rearranger.remove(colName);
                }

            }
            if (rearranger.getColumnCount() <= 1) {
                StringBuilder b = new StringBuilder("Input table has no valid "
                        + "learning columns (need one additional numeric or "
                        + "nominal column).");
                if (!noDomainColumns.isEmpty()) {
                    b.append(" ").append(noDomainColumns.size());
                    b.append(" column(s) were ignored due to missing domain ");
                    b.append("information -- execute predecessor and/or ");
                    b.append(" use Domain Calculator node.");
                    throw new InvalidSettingsException(b.toString());
                }
            }
            if (!m_includeAllColumns && !incl.isEmpty()) {
                StringBuilder missings = new StringBuilder();
                int i = 0;
                for (Iterator<String> it = incl.iterator(); it.hasNext()
                        && i < 4; i++) {
                    String s = it.next();
                    missings.append(i > 0 ? ", " : "").append(s);
                    it.remove();
                }
                if (!incl.isEmpty()) {
                    missings.append(",...").append(incl.size()).append(" more");
                }
                throw new InvalidSettingsException("Some selected attributes "
                        + "are not present in the input table: " + missings);
            }
        } else { // use fingerprint data
            DataColumnSpec fpCol = spec.getColumnSpec(m_fingerprintColumn);
            if (fpCol == null
                    || !fpCol.getType().isCompatible(BitVectorValue.class)) {
                throw new InvalidSettingsException("Fingerprint columnn \""
                        + m_fingerprintColumn + "\" does not exist or is not "
                        + "of correct type.");
            }
            rearranger.keepOnly(m_targetColumn, m_fingerprintColumn);
        }
        rearranger.move(m_targetColumn, rearranger.getColumnCount());
        String warn = null;
        if (!noDomainColumns.isEmpty()) {
            StringBuilder b = new StringBuilder();
            b.append(noDomainColumns.size());
            b.append(" column(s) were ignored due to missing domain");
            b.append(" information: [");
            int index = 0;
            for (String s : noDomainColumns) {
                if (index > 3) {
                    b.append(", ...");
                    break;
                }
                if (index > 0) {
                    b.append(", ");
                }
                b.append("\"").append(s).append("\"");
                index++;
            }
            b.append("] -- change the node configuration or use a");
            b.append(" Domain Calculator node to fix it");
            warn = b.toString();
        }
        rearranger.setWarning(warn);
        return rearranger;
    }

    private boolean shouldIgnoreLearnColumn(final DataColumnSpec col) {
        DataType type = col.getType();
        if (type.isCompatible(NominalValue.class)) {
            boolean hasDomain = col.getDomain().hasValues();
            if (m_ignoreColumnsWithoutDomain && !hasDomain) {
                return true;
            }
        }
        return false;
    }

    public TreeEnsembleModelPortObjectSpec createPortObjectSpec(
            final DataTableSpec learnSpec) {
        return new TreeEnsembleModelPortObjectSpec(learnSpec);
    }

    public RandomData createRandomData() {
        long seed = m_seed == null ? System.currentTimeMillis() : m_seed;
        return createRandomData(seed);
    }

    public static RandomData createRandomData(final long seed) {
        JDKRandomGenerator randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(seed);
        return new RandomDataImpl(randomGenerator);
    }

    public RowSample createRowSample(final int nrRows, final RandomData rd) {
        if (m_isDataSelectionWithReplacement) {
            return new SubsetWithReplacementRowSample(nrRows,
                    m_dataFractionPerTree, rd);
        } else if (m_dataFractionPerTree >= 1.0) {
            return new DefaultRowSample(nrRows);
        } else {
            return new SubsetNoReplacementRowSample(nrRows,
                    m_dataFractionPerTree, rd);
        }
    }

    public ColumnSampleStrategy createColumnSampleStrategy(final TreeData data,
            final RandomData rd) {
        final int totalColCount = data.getNrAttributes();
        int subsetSize;
        switch (m_columnSamplingMode) {
        case None:
            return new AllColumnSampleStrategy(data);
        case Linear:
            subsetSize =
                (int)Math.round(m_columnFractionLinearValue * totalColCount);
            break;
        case Absolute:
            subsetSize = m_columnAbsoluteValue;
            break;
        case SquareRoot:
            // default in R is "floor(sqrt(ncol(x)))"
            subsetSize = (int)Math.floor(Math.sqrt(totalColCount));
            break;
        default:
            throw new IllegalStateException(
                    "Sampling not implemented: " + m_columnSamplingMode);
        }
        subsetSize = Math.max(1, Math.min(subsetSize, totalColCount));
        if (m_isUseDifferentAttributesAtEachNode) {
            return new RFSubsetColumnSampleStrategy(data, rd, subsetSize);
        } else {
            return new SubsetColumnSampleStrategy(data, rd, subsetSize);
        }
    }

    public boolean isRegression() {
        return m_isRegression;
    }

    private Class<? extends DataValue> getRequiredTargetClass() {
        return m_isRegression ? DoubleValue.class : NominalValue.class;
    }

    /** Column rearranger with "warning" field. */
    public static final class FilterLearnColumnRearranger extends ColumnRearranger {

        private String m_warning;

        /** @param original ...*/
        public FilterLearnColumnRearranger(final DataTableSpec original) {
            super(original);
        }

        /**
         * @return the warning
         */
        public String getWarning() {
            return m_warning;
        }

        /**
         * @param warning the warning to set
         */
        void setWarning(final String warning) {
            m_warning = warning;
        }

    }

}
