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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.pivot;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.groupby.GroupByNodeDialog;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The node dialog of the pivot node.
 *
 * @author Thomas Gabriel, KNIME.com AG, Switzerland
 */
public class Pivot2NodeDialog extends GroupByNodeDialog {

    private final SettingsModelFilterString m_pivotCols =
        new SettingsModelFilterString(Pivot2NodeModel.CFG_PIVOT_COLUMNS);

    private final DialogComponentColumnFilter m_pivotCol;

    private final DialogComponentBoolean m_missComponent;

    private final DialogComponentBoolean m_totalComponent;

    private final DialogComponentBoolean m_domainComponent;

    /** Constructor for class Pivot2NodeDialog. */
    public Pivot2NodeDialog() {
//pivot column box
        m_pivotCol = new DialogComponentColumnFilter(m_pivotCols, 0, false);
        //we are only interested in showing the invalid include columns
        m_pivotCol.setShowInvalidIncludeColumns(true);
        m_pivotCol.setIncludeTitle(" Pivot column(s) ");
        m_pivotCol.setExcludeTitle(" Available column(s) ");
        m_pivotCols.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                //remove all pivot columns from the aggregation column list
                columnsChanged();
            }
        });
        final JPanel pivotColPanel = m_pivotCol.getComponentPanel();
        pivotColPanel.setLayout(new GridLayout(1, 1));
        pivotColPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Pivot settings "));

//create missing value option
        m_missComponent = new DialogComponentBoolean(
                createSettingsMissingValues(), "Ignore missing values");
        m_missComponent.setToolTipText("Ignore rows "
            + "containing missing values in pivot column.");

//create total aggregation option
        m_totalComponent = new DialogComponentBoolean(
                createSettingsTotal(), "Append overall totals");
        m_missComponent.setToolTipText("Appends the overall pivot totals with "
                + "each aggregation performed together on all selected pivot "
                + "columns.");

//create domain option
        m_domainComponent = new DialogComponentBoolean(
                createSettingsIgnoreDomain(), "Ignore domain");
        m_domainComponent.setToolTipText("Ignore domain and use only the "
            + "possible values available in the input data.");

//build pivot column filter and missing value panel
        JPanel pivotAllPanel = new JPanel(new BorderLayout());
        pivotAllPanel.add(pivotColPanel, BorderLayout.CENTER);
        JPanel pivotOptions = new JPanel(new FlowLayout());
        pivotOptions.add(m_missComponent.getComponentPanel());
        pivotOptions.add(m_totalComponent.getComponentPanel());
        pivotOptions.add(m_domainComponent.getComponentPanel());
        pivotAllPanel.add(pivotOptions, BorderLayout.SOUTH);
        addPanel(pivotAllPanel, "Pivots");
    }

    /** @return settings model boolean for ignoring missing values */
    static final SettingsModelBoolean createSettingsMissingValues() {
        return new SettingsModelBoolean("missing_values", true);
    }

    /** @return settings model boolean for total aggregation */
    static final SettingsModelBoolean createSettingsTotal() {
        return new SettingsModelBoolean("total_aggregation", false);
    }

    /** @return settings model boolean for ignoring domain */
    static final SettingsModelBoolean createSettingsIgnoreDomain() {
        return new SettingsModelBoolean("ignore_domain", true);
    }

    /** {@inheritDoc} */
    @Override
    protected void excludeColumns(final List<String> columns) {
        final List<String> list = new ArrayList<String>(columns);
        list.addAll(m_pivotCols.getIncludeList());
        super.excludeColumns(list);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        final DataTableSpec spec = (DataTableSpec)specs[0];
        m_pivotCol.loadSettingsFrom(settings, new DataTableSpec[] {spec});
        m_missComponent.loadSettingsFrom(settings, specs);
        m_totalComponent.loadSettingsFrom(settings, specs);
        m_domainComponent.loadSettingsFrom(settings, specs);
        super.loadSettingsFrom(settings, specs);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        //check if the dialog contains invalid pivot columns
        final Set<String> invalidInclCols = m_pivotCol.getInvalidIncludeColumns();
        if (invalidInclCols != null && !invalidInclCols.isEmpty()) {
            throw new InvalidSettingsException(invalidInclCols.size() + " invalid pivot columns found.");
        }
        m_pivotCol.saveSettingsTo(settings);
        m_missComponent.saveSettingsTo(settings);
        m_totalComponent.saveSettingsTo(settings);
        m_domainComponent.saveSettingsTo(settings);
    }

}
