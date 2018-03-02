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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Jan 31, 2018 (ortmann): created
 */
package org.knime.base.node.stats.outlier.detector;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.knime.base.algorithms.outlier.OutlierDetector;
import org.knime.base.algorithms.outlier.OutlierPortObject;
import org.knime.base.algorithms.outlier.listeners.Warning;
import org.knime.base.algorithms.outlier.listeners.WarningListener;
import org.knime.base.algorithms.outlier.options.OutlierReplacementStrategy;
import org.knime.base.algorithms.outlier.options.OutlierTreatmentOption;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.InputFilter;

/**
 * Model to identify outliers based on interquartile ranges.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class OutlierDetectorNodeModel extends NodeModel implements WarningListener {

    /** Invalid input exception text. */
    private static final String INVALID_INPUT_EXCEPTION = "No double compatible columns in input";

    /** Missing outlier column exception text. */
    private static final String MISSING_OUTLIER_COLUMN_EXCEPTION = "Please include at leaste one numerical column!";

    /** Scalar exception text. */
    private static final String SCALAR_EXCEPTION = "The IQR scalar has to be greater than or equal 0.";

    /** Config key of the columns defining the groups. */
    static final String CFG_GROUP_COLS = "groups-list";

    /** Config key of the (outlier)-columns to process. */
    static final String CFG_OUTLIER_COLS = "outlier-list";

    /** Config key for the apply to groups setting. */
    private static final String CFG_USE_GROUPS = "use-groups";

    /** Config key of the iqr scalar. */
    private static final String CFG_SCALAR_PAR = "iqr-scalar";

    /** Config key of the memory policy. */
    private static final String CFG_MEM_POLICY = "memory-policy";

    /** Config key of the estimation type used for in-memory computation. */
    private static final String CFG_ESTIMATION_TYPE = "estimation-type";

    /** Config key of the outlier treatment. */
    private static final String CFG_OUTLIER_TREATMENT = "outlier-treatment";

    /** Config key of the outlier replacement strategy. */
    private static final String CFG_OUTLIER_REPLACEMENT = "replacement-strategy";

    /** Config key of the domain policy. */
    private static final String CFG_DOMAIN_POLICY = "update-domain";

    /** Default scalar to scale the interquartile range */
    private static final double DEFAULT_SCALAR = 1.5d;

    /** Default domain policy. */
    private static final boolean DEFAULT_DOMAIN_POLICY = false;

    /** Default memory policy */
    private static final boolean DEFAULT_MEM_POLICY = true;

    /** Settings model of the selected groups. */
    private SettingsModelColumnFilter2 m_groupSettings;

    /** Settings model of the columns to check for outliers. */
    private SettingsModelColumnFilter2 m_outlierSettings;

    /** Settings model indicating whether the algorithm should be executed in or out of memory. */
    private SettingsModelBoolean m_memorySetting;

    /** Settings model holding information on how the quartiles are calculated if the algorithm is running in-memory. */
    private SettingsModelString m_estimationSettings;

    /** Settings model holding the information on the outlier treatment. */
    private SettingsModelString m_outlierTreatmentSettings;

    /** Settings model holding the information on the outlier replacement strategy. */
    private SettingsModelString m_outlierReplacementSettings;

    /** Settings model holding the factor to scale the interquartile range. */
    private SettingsModelDouble m_scalarModel;

    /** Settings model indicating whether the algorithm has to use the provided groups information. */
    private SettingsModelBoolean m_useGroupsSetting;

    /** Settings model indicating whether the algorithm has to update the domain of the output table, or not. */
    private SettingsModelBoolean m_domainSetting;

    /** Init the outlier detector node model with one input and output. */
    OutlierDetectorNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE,
            BufferedDataTable.TYPE, OutlierPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable in = (BufferedDataTable)inData[0];

        final OutlierDetector outDet = createOutlierDetector(in.getDataTableSpec());

        outDet.execute(in, exec);

        return new PortObject[]{outDet.getOutTable(), outDet.getOutliersTable(), outDet.getSummaryTable(),
            outDet.getOutlierPort()};
    }

    /**
     * Create the outlier detector instance for the given settings.
     *
     * @param inSpec the input data table spec
     * @return an instance of outlier detector
     */
    private OutlierDetector createOutlierDetector(final DataTableSpec inSpec) {
        return new OutlierDetector.Builder(getOutlierColNames(inSpec))//
            .addWarningListener(this)//
            .calcInMemory(m_memorySetting.getBooleanValue())//
            .setEstimationType(EstimationType.valueOf(m_estimationSettings.getStringValue()))//
            .setGroupColumnNames(getGroupColNames(inSpec))//
            .setIQRMultiplier(m_scalarModel.getDoubleValue())//
            .setReplacementStrategy(OutlierReplacementStrategy.getEnum(m_outlierReplacementSettings.getStringValue()))//
            .setTreatmentOption(OutlierTreatmentOption.getEnum(m_outlierTreatmentSettings.getStringValue()))//
            .updateDomain(m_domainSetting.getBooleanValue())//
            .build();
    }

    /**
     * Returns the outlier column names.
     *
     * @param inSpec the input data table spec
     * @return the outlier column names
     */
    private String[] getOutlierColNames(final DataTableSpec inSpec) {
        return m_outlierSettings.applyTo(inSpec).getIncludes();
    }

    /**
     * Convenience method returning the group column names stored in a list. If no group columns are selected an empty
     * list is returned.
     *
     * @param inSpec the input data table spec
     * @return array of group column names
     */
    private String[] getGroupColNames(final DataTableSpec inSpec) {
        final String[] groupColNames;
        if (m_useGroupsSetting.getBooleanValue()) {
            final List<String> outliers = Arrays.stream(getOutlierColNames(inSpec)).collect(Collectors.toList());
            // remove columns for which the outliers have to be computed
            groupColNames = Arrays.stream(m_groupSettings.applyTo(inSpec).getIncludes())
                .filter(s -> !outliers.contains(s)).toArray(String[]::new);
        } else {
            groupColNames = new String[]{};
        }
        return groupColNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // check if the table contains any row holding numerical values
        DataTableSpec inSpec = inSpecs[0];
        if (!inSpec.containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException(INVALID_INPUT_EXCEPTION);
        }

        // check and initialize the groups settings model
        if (m_groupSettings == null) {
            m_groupSettings = createGroupFilterModel();
            // don't add anything to the include list during auto-configure
            m_groupSettings.loadDefaults(inSpec, new InputFilter<DataColumnSpec>() {

                @Override
                public boolean include(final DataColumnSpec name) {
                    return false;
                }
            }, true);
        }
        String[] includes;

        // check and initialize the outlier settings model
        if (m_outlierSettings == null) {
            m_outlierSettings = createOutlierFilterModel();
            m_outlierSettings.loadDefaults(inSpec);
            includes = m_outlierSettings.applyTo(inSpec).getIncludes();
            if (includes.length > 0) {
                setWarningMessage(
                    "Auto configuration: Outliers use all suitable columns (in total " + includes.length + ").");
            }
        }
        includes = m_outlierSettings.applyTo(inSpec).getIncludes();
        if (includes.length == 0) {
            throw new InvalidSettingsException(MISSING_OUTLIER_COLUMN_EXCEPTION);
        }

        // initialize the remaining settings models if necessary
        init();

        // test if flow variables violate settings related to enums
        try {
            EstimationType.valueOf(m_estimationSettings.getStringValue());
            OutlierTreatmentOption.getEnum(m_outlierTreatmentSettings.getStringValue());
            OutlierReplacementStrategy.getEnum(m_outlierReplacementSettings.getStringValue());
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage());
        }

        // test if IQR scalar is < 0
        if (m_scalarModel.getDoubleValue() < 0) {
            throw new InvalidSettingsException(SCALAR_EXCEPTION);
        }

        // return the output spec
        final String[] outlierColNames = getOutlierColNames(inSpec);
        final String[] groupColNames = getGroupColNames(inSpec);

        return new DataTableSpec[]{OutlierDetector.getOutTableSpec(inSpec),
            OutlierDetector.getOutliersTableSpec(inSpec, groupColNames, outlierColNames),
            OutlierDetector.getSummaryTableSpec(inSpec, groupColNames),
            OutlierDetector.getOutlierPortSpec(inSpec, groupColNames, outlierColNames)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_estimationSettings != null) {
            m_estimationSettings.saveSettingsTo(settings);
        }
        if (m_groupSettings != null) {
            m_groupSettings.saveSettingsTo(settings);
        }
        if (m_memorySetting != null) {
            m_memorySetting.saveSettingsTo(settings);
        }
        if (m_outlierSettings != null) {
            m_outlierSettings.saveSettingsTo(settings);
        }
        if (m_scalarModel != null) {
            m_scalarModel.saveSettingsTo(settings);
        }
        if (m_useGroupsSetting != null) {
            m_useGroupsSetting.saveSettingsTo(settings);
        }
        if (m_outlierReplacementSettings != null) {
            m_outlierReplacementSettings.saveSettingsTo(settings);
        }
        if (m_outlierTreatmentSettings != null) {
            m_outlierTreatmentSettings.saveSettingsTo(settings);
        }
        if (m_domainSetting != null) {
            m_domainSetting.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel model : getSettings()) {
            model.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel model : getSettings()) {
            model.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * Creates not yet initialized settings and returns an array storing all them.
     *
     * @return array holding all used settings models.
     */
    private SettingsModel[] getSettings() {
        init();
        return new SettingsModel[]{m_groupSettings, m_outlierSettings, m_estimationSettings, m_scalarModel,
            m_memorySetting, m_useGroupsSetting, m_outlierReplacementSettings, m_outlierTreatmentSettings,
            m_domainSetting};
    }

    /**
     * Creates all non-initialized settings.
     */
    private void init() {
        if (m_groupSettings == null) {
            m_groupSettings = createGroupFilterModel();
        }
        if (m_outlierSettings == null) {
            m_outlierSettings = createOutlierFilterModel();
        }
        if (m_estimationSettings == null) {
            m_estimationSettings = createEstimationModel();
        }
        if (m_scalarModel == null) {
            m_scalarModel = createScalarModel();
        }
        if (m_memorySetting == null) {
            m_memorySetting = createMemoryModel();
        }
        if (m_useGroupsSetting == null) {
            m_useGroupsSetting = createUseGroupsModel();
        }
        if (m_outlierReplacementSettings == null) {
            m_outlierReplacementSettings = createOutlierReplacementModel();
        }
        if (m_outlierTreatmentSettings == null) {
            m_outlierTreatmentSettings = createOutlierTreatmentModel();
        }
        if (m_domainSetting == null) {
            m_domainSetting = createDomainModel();
        }
    }

    /**
     * Returns the settings model holding the factor to scale the IQR.
     *
     * @return the IQR scalar settings model
     */
    public static SettingsModelDouble createScalarModel() {
        return new SettingsModelDoubleBounded(CFG_SCALAR_PAR, DEFAULT_SCALAR, 0, Double.MAX_VALUE);
    }

    /**
     * Returns the settings model of the columns to check for outliers.
     *
     * @return the outlier settings model
     */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createOutlierFilterModel() {
        return new SettingsModelColumnFilter2(CFG_OUTLIER_COLS, DoubleValue.class);

    }

    /**
     * Returns the settings model holding the selected groups.
     *
     * @return the groups settings model
     */
    public static SettingsModelColumnFilter2 createGroupFilterModel() {
        return new SettingsModelColumnFilter2(CFG_GROUP_COLS);
    }

    /**
     * Returns the settings model indicating whether the algorithm should be executed in or out of memory.
     *
     * @return the memory settings model
     */
    public static SettingsModelBoolean createMemoryModel() {
        return new SettingsModelBoolean(CFG_MEM_POLICY, DEFAULT_MEM_POLICY);
    }

    /**
     * Returns the settings model holding information on how the quartiles are calculated if the algorithm is running
     * in-memory.
     *
     * @return the estimation type settings model
     */
    public static SettingsModelString createEstimationModel() {
        return new SettingsModelString(CFG_ESTIMATION_TYPE, EstimationType.values()[0].name());
    }

    /**
     * Returns the settings model telling whether to apply the algorithm to the selected groups or not.
     *
     * @return the use groups settings model
     */
    public static SettingsModelBoolean createUseGroupsModel() {
        return new SettingsModelBoolean(CFG_USE_GROUPS, false);
    }

    /**
     * Returns the settings model informing about the treatment of outliers (replace or filter).
     *
     * @return the outlier treatment settings model
     */
    public static SettingsModelString createOutlierTreatmentModel() {
        return new SettingsModelString(CFG_OUTLIER_TREATMENT, OutlierTreatmentOption.values()[0].toString());
    }

    /**
     * Returns the settings model informing about the selected replacement strategy (Missings or IQR).
     *
     * @return the outlier replacement settings model
     */
    public static SettingsModelString createOutlierReplacementModel() {
        return new SettingsModelString(CFG_OUTLIER_REPLACEMENT, OutlierReplacementStrategy.values()[0].toString());
    }

    /**
     * Returns the settings model informing about the selected domain policy.
     *
     * @return the domain policy settings model
     */
    public static SettingsModelBoolean createDomainModel() {
        return new SettingsModelBoolean(CFG_DOMAIN_POLICY, DEFAULT_DOMAIN_POLICY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warning(final Warning warning) {
        setWarningMessage(warning.getMessage());
    }

}