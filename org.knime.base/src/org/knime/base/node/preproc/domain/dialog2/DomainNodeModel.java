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
 * -------------------------------------------------------------------
 *
 * History
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain.dialog2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DomainCreatorColumnSelection;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class DomainNodeModel extends NodeModel {

    /** Config identifier for columns for which possible values
     * must be determined. */
    static final String CFG_POSSVAL_COLS = "possible_values_columns";

    /** Config identifier whether possible value domain should be retained
     * for non-selected columns (will otherwise be dropped). */
    static final String CFG_POSSVAL_RETAIN_UNSELECTED =
        "possible_values_unselected_retain";

    /** Config identifier for columns for which min and max values
     * must be determined. */
    static final String CFG_MIN_MAX_COLS = "min_max_columns";

    /** Config identifier whether min/max values should be retained
     * for non-selected columns (will otherwise be dropped). */
    static final String CFG_MIN_MAX_RETAIN_UNSELECTED =
        "min_max_unselected_retain";

    /** Config identifier for columns for which min and max values
     * must be determined. */
    static final String CFG_MAX_POSS_VALUES = "max_poss_values";

    private DataColumnSpecFilterConfiguration m_possValConfig;
    private DataColumnSpecFilterConfiguration m_minMaxConfig;
    private boolean m_possValRetainUnselected = true;
    private boolean m_minMaxRetainUnselected = true;
    private int m_maxPossValues = DataContainer.MAX_POSSIBLE_VALUES;

    /** Constructor, inits one input, one output. */
    public DomainNodeModel() {
        super(1, 1);
    }

    private DataTableDomainCreator getDomainCreator(final DataTableSpec inputSpec) {
        final Set<String> possValCols = new HashSet<String>();
        possValCols.addAll(Arrays.asList(m_possValConfig.applyTo(inputSpec).getIncludes()));
        int maxPoss = m_maxPossValues >= 0 ? m_maxPossValues : Integer.MAX_VALUE;

        final Set<String> minMaxCols = new HashSet<String>();
        minMaxCols.addAll(Arrays.asList(m_minMaxConfig.applyTo(inputSpec).getIncludes()));

        DomainCreatorColumnSelection possValueSelection = new DomainCreatorColumnSelection() {

            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return possValCols.contains(colSpec.getName());
            }

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return possValCols.contains(colSpec.getName())
                        || !m_possValRetainUnselected;
            }

        };
        DomainCreatorColumnSelection minMaxSelection = new DomainCreatorColumnSelection() {

            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return minMaxCols.contains(colSpec.getName());
            }

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return minMaxCols.contains(colSpec.getName()) || !m_minMaxRetainUnselected;
            }

        };
        DataTableDomainCreator domainCreator =
            new DataTableDomainCreator(inputSpec, possValueSelection, minMaxSelection);
        domainCreator.setMaxPossibleValues(maxPoss);
        return domainCreator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableDomainCreator domainCreator = getDomainCreator(inData[0].getDataTableSpec());
        domainCreator.updateDomain(inData[0], exec, inData[0].size());
        return new BufferedDataTable[]{exec.createSpecReplacerTable(inData[0], domainCreator.createSpec())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        // auto-configuration in case no user settings are available
        DataTableSpec spec = inSpecs[0];
        if (m_possValConfig == null) {
            m_possValConfig = createDCSFilterConfigurationPossVals();
            m_possValConfig.loadDefaults(DomainNodeModel.getAllCols(NominalValue.class, spec), null,
                EnforceOption.EnforceInclusion);
        }
        if (m_minMaxConfig == null) {
            m_minMaxConfig = createDCSFilterConfigurationMinMax();
            m_minMaxConfig.loadDefaults(DomainNodeModel.getAllCols(BoundedValue.class, spec), null,
                EnforceOption.EnforceInclusion);
        }

        FilterResult filter = m_possValConfig.applyTo(spec);
        String[] rmFromIncl = filter.getRemovedFromIncludes();
        if (m_possValConfig.isEnforceInclusion() && rmFromIncl.length != 0) {
            throw new InvalidSettingsException("Input table does not contain the following column(s) selected in the "
                + "possible value panel: "
                + ConvenienceMethods.getShortStringFrom(new HashSet<String>(Arrays.asList(rmFromIncl)), 3));
        }
        filter = m_minMaxConfig.applyTo(spec);
        rmFromIncl = filter.getRemovedFromIncludes();
        if (m_possValConfig.isEnforceInclusion() && rmFromIncl.length != 0) {
            throw new InvalidSettingsException("Input table does not contain the following column(s) selected in the "
                + "Min/Max value panel: "
                + ConvenienceMethods.getShortStringFrom(new HashSet<String>(Arrays.asList(rmFromIncl)), 3));
        }

        DataTableDomainCreator domainCreator = getDomainCreator(spec);
        return new DataTableSpec[]{domainCreator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_possValConfig != null && m_minMaxConfig != null) {
            m_possValConfig.saveConfiguration(settings);
            m_minMaxConfig.saveConfiguration(settings);
            settings.addInt(CFG_MAX_POSS_VALUES, m_maxPossValues);
            settings.addBoolean(
                    CFG_POSSVAL_RETAIN_UNSELECTED, m_possValRetainUnselected);
            settings.addBoolean(
                    CFG_MIN_MAX_RETAIN_UNSELECTED, m_minMaxRetainUnselected);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration config = createDCSFilterConfigurationPossVals();
        config.loadConfigurationInModel(settings);
        config = createDCSFilterConfigurationMinMax();
        config.loadConfigurationInModel(settings);
        settings.getInt(CFG_MAX_POSS_VALUES);
        settings.getBoolean(CFG_POSSVAL_RETAIN_UNSELECTED);
        settings.getBoolean(CFG_MIN_MAX_RETAIN_UNSELECTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration possConfig = createDCSFilterConfigurationPossVals();
        possConfig.loadConfigurationInModel(settings);
        m_possValConfig = possConfig;

        DataColumnSpecFilterConfiguration minMaxConfig = createDCSFilterConfigurationMinMax();
        minMaxConfig.loadConfigurationInModel(settings);
        m_minMaxConfig = minMaxConfig;

        m_maxPossValues = settings.getInt(CFG_MAX_POSS_VALUES);
        m_minMaxRetainUnselected =
            settings.getBoolean(CFG_MIN_MAX_RETAIN_UNSELECTED);
        m_possValRetainUnselected =
            settings.getBoolean(CFG_POSSVAL_RETAIN_UNSELECTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /** Finds all columns in a spec whose type is compatible to cl.
     * @param cl The value to be compatible to.
     * @param spec The spec to query.
     * @return The identified columns.
     */
    static String[] getAllCols(
            final Class<? extends DataValue> cl, final DataTableSpec spec) {
        ArrayList<String> result = new ArrayList<String>();
        for (DataColumnSpec c : spec) {
            if (c.getType().isCompatible(cl)) {
                result.add(c.getName());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * A new configuration to store the settings.
     *
     * @return filter configuration
     */
    static final DataColumnSpecFilterConfiguration createDCSFilterConfigurationPossVals() {
        return new DataColumnSpecFilterConfiguration("colFilter_possVals");
    }

    /**
     * A new configuration to store the settings.
     *
     * @return filter configuration
     */
    static final DataColumnSpecFilterConfiguration createDCSFilterConfigurationMinMax() {
        return new DataColumnSpecFilterConfiguration("colFilter_minMax");
    }
}

