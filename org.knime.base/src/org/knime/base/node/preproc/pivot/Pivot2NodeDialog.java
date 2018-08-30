/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.pivot;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.groupby.GroupByNodeDialog;
import org.knime.base.node.preproc.pivot.Pivot2NodeModel.ColNameOption;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;

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

    private DialogComponentStringSelection m_colNameAggComponent;

    private DialogComponentBoolean m_sortingComponent;

    /** Constructor for class Pivot2NodeDialog. */
    @SuppressWarnings("unchecked")
    public Pivot2NodeDialog() {
        //pivot column box
        m_pivotCol = new DialogComponentColumnFilter(m_pivotCols, 0, false,
            new ColumnFilterPanel.ValueClassFilter(DataValue.class), false);
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
        pivotColPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Pivot settings "));

        //create missing value option
        m_missComponent =
            new DialogComponentBoolean(Pivot2NodeModel.createSettingsMissingValues(), "Ignore missing values");
        m_missComponent.setToolTipText("Ignore rows " + "containing missing values in pivot column.");

        //create total aggregation option
        m_totalComponent = new DialogComponentBoolean(Pivot2NodeModel.createSettingsTotal(), "Append overall totals");
        m_missComponent.setToolTipText("Appends the overall pivot totals with "
            + "each aggregation performed together on all selected pivot " + "columns.");

        //create domain option
        m_domainComponent = new DialogComponentBoolean(Pivot2NodeModel.createSettingsIgnoreDomain(), "Ignore domain");
        m_domainComponent
            .setToolTipText("Ignore domain and use only the " + "possible values available in the input data.");

        //build pivot column filter and missing value panel
        JPanel pivotAllPanel = new JPanel(new BorderLayout());
        pivotAllPanel.add(pivotColPanel, BorderLayout.CENTER);
        JPanel pivotOptions = new JPanel(new FlowLayout());
        pivotOptions.add(m_missComponent.getComponentPanel());
        pivotOptions.add(m_totalComponent.getComponentPanel());
        pivotOptions.add(m_domainComponent.getComponentPanel());
        pivotAllPanel.add(pivotOptions, BorderLayout.SOUTH);
        addPanel(pivotAllPanel, "Pivots", 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JComponent createAdvancedOptionsBox() {
        final SettingsModelString colNameModel = Pivot2NodeModel.createSettingsColNameOption();
        m_colNameAggComponent = new DialogComponentStringSelection(colNameModel, "Column name:",
            Arrays.stream(ColNameOption.values())//
                .map(val -> val.toString())//
                .collect(Collectors.toList())//
        );

        m_sortingComponent =
            new DialogComponentBoolean(Pivot2NodeModel.createSettingsLexicographical(), "Sort lexicographically");

        final JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Advanced settings "));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        rootPanel.add(m_colNameAggComponent.getComponentPanel(), c);

        c.gridx += 1;
        c.gridwidth = 3;
        rootPanel.add(createColNamePolicyDialog("Aggregation name:", null).getComponentPanel(), c);

        c.gridwidth = 1;
        c.gridx += 3;
        rootPanel.add(m_sortingComponent.getComponentPanel(), c);

        c.gridy++;
        c.gridx = 0;
        rootPanel.add(createMaxNoneNumValsDialog().getComponentPanel(), c);

        c.gridx++;
        rootPanel.add(createValueDelDialog().getComponentPanel(), c);

        c.gridx++;
        rootPanel.add(createInMemoryDialog().getComponentPanel(), c);

        c.gridx++;
        rootPanel.add(createRetainOrderDialog().getComponentPanel(), c);

        ++c.gridx;
        rootPanel.add(createHiliteDialog().getComponentPanel(), c);

        return rootPanel;
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
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final DataTableSpec spec = (DataTableSpec)specs[0];
        m_pivotCol.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_missComponent.loadSettingsFrom(settings, specs);
        m_totalComponent.loadSettingsFrom(settings, specs);
        m_domainComponent.loadSettingsFrom(settings, specs);
        m_colNameAggComponent.loadSettingsFrom(settings, specs);
        m_sortingComponent.loadSettingsFrom(settings, specs);
        super.loadSettingsFrom(settings, specs);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
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
        m_colNameAggComponent.saveSettingsTo(settings);
        m_sortingComponent.saveSettingsTo(settings);
    }

}
