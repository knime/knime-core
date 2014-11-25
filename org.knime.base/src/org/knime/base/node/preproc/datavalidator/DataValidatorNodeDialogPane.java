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
 */
package org.knime.base.node.preproc.datavalidator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.lang.ArrayUtils;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.RejectBehavior;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.UnknownColumnHandling;
import org.knime.base.node.preproc.datavalidator.dndpanel.DnDColumnSelectionSearchableListPanel;
import org.knime.base.node.preproc.datavalidator.dndpanel.DnDConfigurationPanel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestEvent;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestListener;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfiguredColumnDeterminer;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ListModifier;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.SearchedItemsSelectionMode;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.RadionButtonPanel;
import org.knime.core.node.workflow.DataTableSpecView;

/**
 * Main dialog panel for the DataValidator node.
 *
 * @author Marcel Hanser, University of Konstanz
 */
public class DataValidatorNodeDialogPane extends NodeDialogPane {

    private static final DataValidatorColPanel DUMMY_PANEL = new DataValidatorColPanel();

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataValidatorNodeDialogPane.class);

    private final DnDConfigurationPanel<DataValidatorColPanel> m_individualsPanel;

    private final JPanel m_refTableSpecTab;

    private final DnDColumnSelectionSearchableListPanel m_searchableListPanel;

    private ListModifier m_searchableListModifier;

    private DataTableSpec m_referenceDataTableSpec;

    private DataTableSpec m_inputDataTableSpec;

    private Component m_applyDataTableSpecPanel;

    private RadionButtonPanel<UnknownColumnHandling> m_unkownColumnsHandling;

    private RadionButtonPanel<RejectBehavior> m_failBehavior;

    /**
     * Constructs new dialog with an appropriate dialog title.
     */
    @SuppressWarnings("serial")
    public DataValidatorNodeDialogPane() {
        super();
        GridBagConstraints c = new GridBagConstraints();


        m_applyDataTableSpecPanel = createApplyDataTableSpecPanel();

        m_failBehavior = new RadionButtonPanel<>("Behavior on validation issue", RejectBehavior.values());
        m_unkownColumnsHandling = new RadionButtonPanel<>("Handling of unkown columns", UnknownColumnHandling.values());

        Dimension failBehPrefSize = m_failBehavior.getPreferredSize();
        Dimension unknownColPrefSize = m_unkownColumnsHandling.getPreferredSize();
        int width = Math.max(failBehPrefSize.width, unknownColPrefSize.width);
        int height = Math.max(failBehPrefSize.height, unknownColPrefSize.height);

        Dimension d = new Dimension(width, height);
        m_failBehavior.setPreferredSize(d);
        m_unkownColumnsHandling.setPreferredSize(d);


        JPanel southernPanel = new JPanel(new BorderLayout());

        JPanel generalConfigPanel = new JPanel(new GridBagLayout());
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        generalConfigPanel.add(m_failBehavior, c);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        generalConfigPanel.add(m_unkownColumnsHandling, c);
        generalConfigPanel.setBorder(BorderFactory.createTitledBorder("General settings"));

        southernPanel.add(generalConfigPanel, BorderLayout.NORTH);
        southernPanel.add(m_applyDataTableSpecPanel, BorderLayout.SOUTH);

        // Individual Handling, second tab
        m_searchableListPanel =
            new DnDColumnSelectionSearchableListPanel(SearchedItemsSelectionMode.SELECT_ALL,
                new ConfiguredColumnDeterminer() {

                    @Override
                    public boolean isConfiguredColumn(final DataColumnSpec cspec) {
                        for (DataValidatorColPanel p : m_individualsPanel) {
                            final List<String> names = Arrays.asList(p.getSettings().getNames());

                            if (names.contains(cspec.getName())) {
                                return true;
                            }
                        }
                        return false;
                    }
                });

        m_searchableListPanel.addConfigurationRequestListener(new ConfigurationRequestListener() {

            @Override
            public void configurationRequested(final ConfigurationRequestEvent event) {
                boolean checkConfigurationsStatus = checkConfigurationsStatus();
                switch (event.getType()) {
                    case CREATION:
                        if (!checkConfigurationsStatus) {
                            return;
                        }
                        onAdd(m_searchableListPanel.getSelectedColumns());
                        break;

                    case SELECTION:
                        moveToSelection();
                        break;
                    default:
                }
            }

        });

        final JPanel tabPanel = new JPanel(new GridBagLayout());
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.4;
        c.gridx = 0;
        c.gridy = 0;
        tabPanel.add(m_searchableListPanel, c);

        m_individualsPanel = new DnDConfigurationPanel<DataValidatorColPanel>() {

            @Override
            protected Dimension getDefaultPreferredSize() {
                return DUMMY_PANEL.getPreferredSize();
                //return new Dimension(800,120);
            }

            @Override
            protected DataValidatorColPanel createConfigurationPanel(final List<DataColumnSpec> specs) {
                DataValidatorColPanel dataValidatorColPanel =
                    new DataValidatorColPanel(DataValidatorNodeDialogPane.this, specs);
                addRemoveListeners(dataValidatorColPanel);
                return dataValidatorColPanel;
            }

            @Override
            public boolean isDropable(final List<DataColumnSpec> data) {
                return DataValidatorNodeDialogPane.this.isDropable(data);
            }
        };

        m_individualsPanel.addPropertyChangeListener(DnDConfigurationPanel.CONFIGURATION_CHANGED,
            new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                    m_searchableListPanel.repaint();
                }
            });

        m_searchableListPanel.enableDragAndDropSupport(m_individualsPanel);


        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.6;
        c.gridx = 1;
        c.gridy = 0;
        tabPanel.add(m_individualsPanel, c);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 1;
        tabPanel.add(southernPanel, c);

        addTab("Validation Settings", tabPanel);
        m_refTableSpecTab = new JPanel(new GridLayout(0, 1));
        addTab("Reference Spec", m_refTableSpecTab);
    }

    /**
     * @return
     */
    private Component createApplyDataTableSpecPanel() {
        JPanel toReturn = new JPanel(new BorderLayout());
        JButton applyDataTablesSpecButon = new JButton("<html><center>Set Input Table <br>as Reference</html>");

        applyDataTablesSpecButon
            .setToolTipText("Sets the current input data table spec as the reference specification.");
        applyDataTablesSpecButon.setBorder(BorderFactory.createLineBorder(Color.orange));
        applyDataTablesSpecButon.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                NodeSettings nodeSettings = new NodeSettings("Tmp");
                try {
                    // resetting the dialog is in principle the same procedure
                    // done by #saveSettingsTo and #loadSettingsFrom.
                    // As the m_dataTableSpec is saved in the configuration the only
                    // thing we have to do is to set it to the given spec.
                    m_referenceDataTableSpec = m_inputDataTableSpec;
                    saveSettingsTo(nodeSettings);
                    loadSettingsFrom(nodeSettings, new DataTableSpec[]{m_inputDataTableSpec});
                } catch (InvalidSettingsException | NotConfigurableException e1) {
                    LOGGER.info("Problem while applying new configuration.", e1);
                }
            }
        });
        toReturn.add(applyDataTablesSpecButon, BorderLayout.CENTER);
        return toReturn;
    }

    private boolean checkConfigurationsStatus() {
        List<DataColumnSpec> selectedColumns = m_searchableListPanel.getSelectedColumns();
        Set<String> selectedNames = new HashSet<String>();
        for (DataColumnSpec dcs : selectedColumns) {
            selectedNames.add(dcs.getName());
        }

        if (selectedColumns.isEmpty()) {
            return false;
        } else {
            for (DataValidatorColPanel p : m_individualsPanel) {
                for (String dcs : p.getColumnNames()) {
                    if (selectedNames.contains(dcs)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose() {
        m_referenceDataTableSpec = null;
        m_inputDataTableSpec = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        m_inputDataTableSpec = specs[0];

        DataValidatorConfiguration dataValidatorConfiguration = new DataValidatorConfiguration();
        dataValidatorConfiguration.loadConfigurationInDialog(settings, specs[0]);

        m_referenceDataTableSpec = dataValidatorConfiguration.getReferenceTableSpec();

        m_applyDataTableSpecPanel.setVisible(!m_inputDataTableSpec.equals(m_referenceDataTableSpec));

        List<DataValidatorColConfiguration> individuals = dataValidatorConfiguration.getIndividualConfigurations();

        m_individualsPanel.removeAllConfigurationPanels();
        Set<String> invalidColumns = new LinkedHashSet<String>();

        for (int i = 0; i < individuals.size(); i++) {
            DataValidatorColConfiguration currentColConfig = individuals.get(i);
            String[] names = currentColConfig.getNames();
            ArrayList<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
            for (int j = 0; j < names.length; j++) {
                final DataColumnSpec cspec = m_referenceDataTableSpec.getColumnSpec(names[j]);
                if (cspec == null) {
                    LOGGER.debug("No such column in spec: " + names[j]);
                    DataColumnSpec createUnkownSpec = DataColumnSpecListCellRenderer.createInvalidSpec(names[j]);
                    colSpecs.add(createUnkownSpec);
                    invalidColumns.add(names[j]);
                } else {
                    colSpecs.add(cspec);
                }
            }
            if (!colSpecs.isEmpty()) {
                final DataValidatorColPanel p = new DataValidatorColPanel(this, true, currentColConfig, colSpecs);

                p.registerMouseListener(new MouseAdapter() {
                    /** {@inheritDoc} */
                    @Override
                    public void mouseClicked(final MouseEvent me) {
                        selectColumns(p.getSettings());
                    }
                });
                addToIndividualPanel(p);
            }
        }
        m_searchableListModifier = m_searchableListPanel.update(m_referenceDataTableSpec);
        m_searchableListModifier.addInvalidColumns(invalidColumns.toArray(new String[invalidColumns.size()]));

        m_individualsPanel.setPreferredSize(DUMMY_PANEL.getPreferredSize());

        m_failBehavior.setSelectedValue(dataValidatorConfiguration.getFailingBehavior());
        m_unkownColumnsHandling.setSelectedValue(dataValidatorConfiguration.getUnkownColumnsHandling());

        updateDataTableSpecComparison();
    }

    private void updateDataTableSpecComparison() {
        m_refTableSpecTab.removeAll();

        JPanel northern = new JPanel(new GridLayout(0, 1));
        DataTableSpecView refTableSpecView = new DataTableSpecView(m_referenceDataTableSpec);
        refTableSpecView.setPreferredSize(new Dimension(-1, 150));
        northern.add(new JScrollPane(refTableSpecView, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        northern.setBorder(BorderFactory.createTitledBorder("Reference Table Spec"));

        if (!m_referenceDataTableSpec.equals(m_inputDataTableSpec)) {
            m_refTableSpecTab.add(northern);
            JPanel southern = new JPanel(new GridLayout(0, 1));
            southern.setBorder(BorderFactory.createTitledBorder("Input Table Spec"));
            DataTableSpecView inputTableSpec = new DataTableSpecView(m_inputDataTableSpec);
            inputTableSpec.setPreferredSize(new Dimension(-1, 150));
            southern.add(new JScrollPane(inputTableSpec, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));

            m_refTableSpecTab.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, northern, southern));
        } else {
            m_refTableSpecTab.add(northern);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        DataValidatorConfiguration dataValidatorConfiguration = new DataValidatorConfiguration();

        List<DataValidatorColConfiguration> individuals = new ArrayList<>();
        for (DataValidatorColPanel p : m_individualsPanel) {
            individuals.add(p.getSettings());
        }
        dataValidatorConfiguration.setIndividualConfigurations(individuals);
        dataValidatorConfiguration.setReferenceTableSpec(m_referenceDataTableSpec);
        dataValidatorConfiguration.setRemoveUnkownColumns(m_unkownColumnsHandling.getSelectedValue());
        dataValidatorConfiguration.setFailingBehavior(m_failBehavior.getSelectedValue());

        dataValidatorConfiguration.saveSettings(settings);
    }

    private void onAdd(final List<DataColumnSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        final DataValidatorColPanel p = new DataValidatorColPanel(this, specs);
        p.registerMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent me) {
                selectColumns(p.getSettings());
            }
        });
        addToIndividualPanel(p);
        moveToSelection();
    }

    private void moveToSelection() {
        List<DataColumnSpec> selectedColumns = m_searchableListPanel.getSelectedColumns();
        if (!selectedColumns.isEmpty()) {
            DataColumnSpec dataColumnSpec = selectedColumns.get(0);
            DataValidatorColPanel curr = null;
            for (DataValidatorColPanel p : m_individualsPanel) {
                if (ArrayUtils.contains(p.getSettings().getNames(), dataColumnSpec.getName())) {
                    curr = p;
                    break;
                }
            }
            if (curr != null) {
                m_individualsPanel.ensureConfigurationPanelVisible(curr);
            }
        }
    }

    private void removeFromIndividualPanel(final DataValidatorColPanel panel) {
        for (String name : panel.getColumnNames()) {
            if (m_searchableListPanel.isAdditionalColumn(name)) {
                m_searchableListModifier.removeAdditionalColumn(name);
            }
        }
        m_individualsPanel.removeConfigurationPanel(panel);
    }

    private void addToIndividualPanel(final DataValidatorColPanel panel) {
        addRemoveListeners(panel);
        m_individualsPanel.addConfigurationPanel(panel);
    }

    /**
     * @param panel
     */
    private void addRemoveListeners(final DataValidatorColPanel panel) {
        panel.addPropertyChangeListener(DataValidatorColPanel.REMOVE_ACTION, new PropertyChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                removeFromIndividualPanel((DataValidatorColPanel)evt.getSource());
            }
        });
        panel.addPropertyChangeListener(DataValidatorColPanel.REMOVED_COLUMNS, new PropertyChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                DataColumnSpec[] removedSpecs = (DataColumnSpec[])evt.getNewValue();
                if (removedSpecs != null) {
                    for (DataColumnSpec spec : removedSpecs) {
                        if (m_searchableListPanel.isAdditionalColumn(spec.getName())) {
                            m_searchableListModifier.removeAdditionalColumn(spec.getName());
                        }
                    }
                }
                m_searchableListPanel.repaint();
            }
        });
    }

    private void selectColumns(final DataValidatorColConfiguration setting) {
        m_searchableListPanel.setSelectedColumns(setting.getNames());
        m_searchableListPanel.ensureSelectedColumnsVisible();
    }

    /**
     * @param data check if the button is enabled...
     * @return <code>true</code> if the data columns are not configured currently
     */
    public boolean isDropable(final List<DataColumnSpec> data) {
        return checkConfigurationsStatus();
    }
}
