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
 *   Feb 23, 2018 (ortmann): created
 */
package org.knime.base.algorithms.outlier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.base.algorithms.outlier.options.OutlierReplacementStrategy;
import org.knime.base.algorithms.outlier.options.OutlierTreatmentOption;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 * Port object that is passed to outlier apply node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class OutlierPortObject extends AbstractSimplePortObject {

    /** Convenience accessor for the port type. */
    @SuppressWarnings("hiding")
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(OutlierPortObject.class);

    /** Config key of the outlier treatment. */
    private static final String CFG_OUTLIER_TREATMENT = "outlier-treatment";

    /** Config key of the outlier replacement strategy. */
    private static final String CFG_OUTLIER_REPLACEMENT = "replacement-strategy";

    /** Config key of the domain policy. */
    private static final String CFG_DOMAIN_POLICY = "update-domain";

    /** Config key of the outlier treatment policy. */
    private static final String CFG_REVISER = "outlier-treatment";

    /** Config key of the permitted intervals. */
    private static final String CFG_INTERVALS = "permitted intervals";

    /** The summary (tooltip) text. */
    private final String m_summary;

    /** The outlier reviser. */
    private final OutlierReviser m_reviser;

    /** The outlier reviser builder, */
    private OutlierReviser.Builder m_reviserBuilder;

    /** The outlier model. */
    private OutlierModel m_permIntervalsModel;

    /** Empty constructor required by super class, should not be used. */
    public OutlierPortObject() {
        m_summary = "";
        m_reviser = null;
    }

    /**
     * Create new port object given the arguments.
     *
     * @param summary the tooltip
     * @param permIntervalsTable ther permitted intervals table
     * @param reviser the {@link OutlierReviser}
     */
    OutlierPortObject(final String summary, final OutlierModel permIntervalsModel, final OutlierReviser reviser) {
        // store the summary
        m_summary = summary;

        // store the spec holding groups and outlier column names
        m_permIntervalsModel = permIntervalsModel;

        // store the reviser
        m_reviser = reviser;
    }

    /**
     * Returns the proper instance of the outlier reviser
     *
     * @return properly instantiated outlier reviser
     */
    public OutlierReviser.Builder getOutRevBuilder() {
        return m_reviserBuilder;
    }

    /**
     * Returns the outlier model only containing columns that that exist in the in spec.
     *
     * @param inSpec the in spec of the table whose outlier have to be treated
     * @return the filtered outlier model
     */
    public OutlierModel getOutlierModel(final DataTableSpec inSpec) {
        // remove all entries related to outlier columns not existent in the input spec
        m_permIntervalsModel.dropOutliers(Arrays.stream(m_permIntervalsModel.getOutlierColNames())
            .filter(s -> !inSpec.containsName(s)).collect(Collectors.toList()));
        return m_permIntervalsModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {
        return m_permIntervalsModel.getModelSpec();
    }

    /**
     * Drops the given outlier columns from the outlier port spec.
     *
     * @param outlierPortSpec the outlier port spec to be adapted
     * @param outlierColsToDrop the columns to be dropped
     * @return the filtered port spec
     * @throws IllegalArgumentException if any of the outlier columns to be drop is not available in the provided spec
     */
    public static DataTableSpec dropOutlierColsFromSpec(final DataTableSpec outlierPortSpec,
        final List<String> outlierColsToDrop) throws IllegalArgumentException {
        return OutlierModel.dropOutlierColsFromSpec(outlierPortSpec, outlierColsToDrop);
    }

    /**
     * Returns the group column names used to calculate the given outlier port spec.
     *
     * @param outlierPortSpec the outlier port spec
     * @return the group column names used to calculate the outlier port spec
     */
    public static String[] getGroups(final DataTableSpec outlierPortSpec) {
        return OutlierModel.getGroups(outlierPortSpec);
    }

    /**
     * Returns the outlier column names used to calculate the given outlier port spec.
     *
     * @param outlierPortSpec the outlier port spec
     * @return the outlier column names used to calculate the outlier port spec
     */
    public static String[] getOutliers(final DataTableSpec outlierPortSpec) {
        return OutlierModel.getOutliers(outlierPortSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return m_summary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec) throws CanceledExecutionException {
        saveReviser(model.addModelContent(CFG_REVISER));
        m_permIntervalsModel.saveModel(model.addModelContent(CFG_INTERVALS));
    }

    /**
     * Saves the reviser to the model content.
     *
     * @param model the model to save to
     */
    private void saveReviser(final ModelContentWO model) {
        model.addString(CFG_OUTLIER_TREATMENT, m_reviser.getTreatmentOption().toString());
        model.addString(CFG_OUTLIER_REPLACEMENT, m_reviser.getReplacementStrategy().toString());
        model.addBoolean(CFG_DOMAIN_POLICY, m_reviser.updateDomain());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        // initialize the reviser builder
        final ModelContentRO reviserModel = model.getModelContent(CFG_REVISER);
        m_reviserBuilder = new OutlierReviser.Builder()//
            .setTreatmentOption(OutlierTreatmentOption.getEnum(reviserModel.getString(CFG_OUTLIER_TREATMENT)))//
            .setReplacementStrategy(OutlierReplacementStrategy.getEnum(reviserModel.getString(CFG_OUTLIER_REPLACEMENT)))//
            .updateDomain(reviserModel.getBoolean(CFG_DOMAIN_POLICY));

        // initialize the permitted intervals model
        m_permIntervalsModel = OutlierModel.loadModel(model.getModelContent(CFG_INTERVALS), (DataTableSpec)spec);
    }

}
