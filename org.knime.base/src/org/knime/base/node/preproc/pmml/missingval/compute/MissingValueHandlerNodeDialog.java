 package org.knime.base.node.preproc.pmml.missingval.compute;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.knime.base.node.preproc.pmml.missingval.MVColumnSettings;
import org.knime.base.node.preproc.pmml.missingval.MVIndividualSettings;
import org.knime.base.node.preproc.pmml.missingval.MVSettings;
import org.knime.base.node.preproc.pmml.missingval.MissingCellHandlerFactoryManager;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestEvent;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestListener;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfiguredColumnDeterminer;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ListModifier;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.SearchedItemsSelectionMode;

/**
 * <code>NodeDialog</code> for the "CompiledModelReader" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class MissingValueHandlerNodeDialog extends NodeDialogPane {

    private static final String INCOMPATIBLE_COLUMN = "!---INCOMPATIBLE_COLUMN---!";

    private static final String PMML_WARNING = "Options marked with an asterisk (*) will result in non-standard PMML.";

    private final JPanel m_defaultsPanel;

    private final JPanel m_columnsPanel;

    private final JPanel m_typeSettingsPanel;

    private final JPanel m_individualsPanel;

    private final JButton m_addButton;

    private final ColumnSelectionSearchableListPanel m_searchableListPanel;

    private PortObjectSpec[] m_specs;

    private ListModifier m_searchableListModifier;

    private JLabel m_pmmlLabel1;

    private JLabel m_pmmlLabel2;

    private JLabel m_warnings;

    private JScrollPane m_scrollPane;

    private LinkedHashMap<DataType, MissingValueHandlerFactorySelectionPanel> m_types;

    /**
     * New pane for configuring the CompiledModelReader node.
     */
    public MissingValueHandlerNodeDialog() {
        // Create panels for the default tab
        m_defaultsPanel = new JPanel(new BorderLayout());
        //m_defaultsPanel.setPreferredSize(new Dimension(500, 300));
        m_typeSettingsPanel = new JPanel(new GridBagLayout());
        m_defaultsPanel.add(m_typeSettingsPanel, BorderLayout.CENTER);
        m_defaultsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        final boolean showPMMLWarning = getHandlerFactoryManager().hasNonStandardPMMLHandlers();
        m_pmmlLabel1 = new JLabel(PMML_WARNING);
        m_pmmlLabel2 = new JLabel(PMML_WARNING);
        m_pmmlLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        m_pmmlLabel2.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.gridx = 0;
        gbc1.gridy = 0;
        gbc1.weightx = 1.0;

        m_warnings = new JLabel();
        m_warnings.setForeground(Color.RED);
        messagePanel.add(m_warnings, gbc1);
        if (showPMMLWarning) {
            gbc1.gridy = 1;
            messagePanel.add(m_pmmlLabel1, gbc1);
        }

        m_defaultsPanel.add(messagePanel, BorderLayout.SOUTH);

        m_scrollPane = new JScrollPane(m_defaultsPanel);
        addTab("Default", m_scrollPane);

        // Panel for the tab where the user selects column specific missing cell handlers
        m_columnsPanel = new JPanel(new BorderLayout());
        m_individualsPanel = new IndividualsPanel();

        // The panel that has the "Add"-button and the warning about PMML compatibility
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        southPanel.add(buttonPanel, gbc);

        if (showPMMLWarning) {
            gbc.gridy = 1;
            southPanel.add(m_pmmlLabel2, gbc);
        }
        m_addButton = new JButton("Add");
        m_addButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                onAdd(m_searchableListPanel.getSelectedColumns());
            }
        });
        buttonPanel.add(m_addButton);
        m_columnsPanel.add(southPanel, BorderLayout.SOUTH);

        // Init the panel where the user selects columns to configure a handler for
        m_searchableListPanel =
                new ColumnSelectionSearchableListPanel(SearchedItemsSelectionMode.SELECT_ALL,
                        new ConfiguredColumnDeterminer() {
                            @Override
                            public boolean isConfiguredColumn(final DataColumnSpec cspec) {
                                final Component[] c = m_individualsPanel.getComponents();
                                for (int j = 0; j < c.length; j++) {
                                    final ColumnHandlingFactorySelectionPanel p =
                                            (ColumnHandlingFactorySelectionPanel)c[j];

                                    for (String cs : p.getColumns()) {
                                        if (cs.equals(cspec.getName())) {
                                            return true;
                                        }
                                    }
                                }
                                return false;
                            }
                        });

        m_searchableListPanel.addConfigurationRequestListener(new ConfigurationRequestListener() {
            @Override
            public void configurationRequested(final ConfigurationRequestEvent creationEvent) {
                checkButtonStatus();
                if (creationEvent.getType().equals(ConfigurationRequestEvent.Type.CREATION)) {
                    if (!m_addButton.isEnabled()) {
                        return;
                    }
                    onAdd(m_searchableListPanel.getSelectedColumns());
                }
            }
        });

        m_columnsPanel.add(m_searchableListPanel, BorderLayout.WEST);
        m_columnsPanel.setPreferredSize(new Dimension(650, 500));

        JScrollPane scroller = new JScrollPane(m_individualsPanel);
        m_columnsPanel.add(scroller, BorderLayout.CENTER);
        addTab("Column Settings", new JScrollPane(m_columnsPanel));
    }

    private void onAdd(final List<DataColumnSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }

        final ColumnHandlingFactorySelectionPanel p =
            new ColumnHandlingFactorySelectionPanel(specs, m_specs, 0, getHandlerFactoryManager());
        p.registerMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent me) {
                selectColumns(p);
            }
        });
        addToIndividualPanel(p);
        checkButtonStatus();
    }

    /** Enables/disables the button according to list selection. */
    private void checkButtonStatus() {
        List<DataColumnSpec> selectedColumns = m_searchableListPanel.getSelectedColumns();
        if (selectedColumns.isEmpty()) {
            m_addButton.setEnabled(false);
        } else {
            m_addButton.setEnabled(true);
        }
    }

    private void selectColumns(final ColumnHandlingFactorySelectionPanel setting) {
        m_searchableListPanel.setSelectedColumns(setting.getColumns().toArray(new String[0]));
        m_searchableListPanel.ensureSelectedColumnsVisible();
    }

    private void removeFromIndividualPanel(final ColumnHandlingFactorySelectionPanel panel) {
        for (String cs : panel.getColumns()) {
            if (m_searchableListPanel.isAdditionalColumn(cs)) {
                m_searchableListModifier.removeAdditionalColumn(cs);
            }
        }
        m_individualsPanel.remove(panel);
        m_individualsPanel.revalidate();
        m_individualsPanel.repaint();
        m_searchableListPanel.revalidate();
        m_searchableListPanel.repaint();
        checkButtonStatus();
    }

    private void addToIndividualPanel(final ColumnHandlingFactorySelectionPanel panel) {
        panel.addPropertyChangeListener(ColumnHandlingFactorySelectionPanel.REMOVE_ACTION,
                                        new PropertyChangeListener() {
                                            /** {@inheritDoc} */
                                            @Override
                                            public void propertyChange(final PropertyChangeEvent evt) {
                                                removeFromIndividualPanel((ColumnHandlingFactorySelectionPanel)evt
                                                        .getSource());
                                            }
                                        });

        panel.addPropertyChangeListener(ColumnHandlingFactorySelectionPanel.REMOVED_INVALID_COLUMNS,
                                        new PropertyChangeListener() {
                                            /** {@inheritDoc} */
                                            @Override
                                            public void propertyChange(final PropertyChangeEvent evt) {
                                                String[] removedSpecs = (String[])evt.getNewValue();
                                                if (removedSpecs != null) {
                                                    for (String spec : removedSpecs) {
                                                        if (m_searchableListPanel.isAdditionalColumn(spec)) {
                                                            m_searchableListModifier.removeAdditionalColumn(spec);
                                                        }
                                                    }
                                                }
                                                checkButtonStatus();
                                                m_searchableListPanel.repaint();
                                            }
                                        });
        panel.addPropertyChangeListener(new FactoryChangedListener());
        m_individualsPanel.add(panel);
        m_individualsPanel.revalidate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        MVSettings mvSettings = createEmptySettings();
        for (DataType type : m_types.keySet()) {
            MissingValueHandlerFactorySelectionPanel panel = m_types.get(type);
            mvSettings.setSettingsForDataType(type, panel.getSettings());
        }
        for (Component panel : m_individualsPanel.getComponents()) {
            mvSettings.getColumnSettings().add(((ColumnHandlingFactorySelectionPanel)panel).getUpdatedSettings());
        }
        mvSettings.saveToSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        MVSettings mvSettings = createEmptySettings();
        m_specs = specs;
        DataTableSpec spec = (DataTableSpec)specs[0];

        StringBuffer warning = new StringBuffer();
        if (spec.getNumColumns() == 0) {
            throw new NotConfigurableException("There are no columns for missing value replacement available.");
        }

        m_searchableListModifier = m_searchableListPanel.update(spec);

        try {
             mvSettings.loadSettings(settings, true);
        } catch (Exception e) {
            if (warning.length() > 0) {
                warning.append("\n");
            }
            warning.append("The settings are malformed and had to be reset");
            mvSettings = new MVSettings(spec);
        }

        m_warnings.setText(warning.toString());

        m_types = new LinkedHashMap<DataType, MissingValueHandlerFactorySelectionPanel>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            m_types.put(spec.getColumnSpec(i).getType(), null);
        }

        m_individualsPanel.removeAll();
        for (MVColumnSettings colSetting : mvSettings.getColumnSettings()) {
            ColumnHandlingFactorySelectionPanel p =
                new ColumnHandlingFactorySelectionPanel(colSetting, specs, 0, getHandlerFactoryManager());
            addToIndividualPanel(p);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        m_typeSettingsPanel.removeAll();

        for (DataType type : m_types.keySet()) {
            gbc.gridx = 0;
            gbc.ipadx = 10;

            JLabel l = new JLabel(type.toPrettyString());
            m_typeSettingsPanel.add(l, gbc);
            gbc.gridx = 1;
            gbc.ipadx = 0;

            MVIndividualSettings s = mvSettings.getSettingsForDataType(type);
            // Should not happen if node model is properly configured,
            // but check anyways and fall back to do nothing factory
            if (s == null) {
                s = new MVIndividualSettings(getHandlerFactoryManager());
            }
            MissingValueHandlerFactorySelectionPanel p =
                    new MissingValueHandlerFactorySelectionPanel(type, s, getHandlerFactoryManager(), specs);
            p.setBorder(BorderFactory.createBevelBorder(1));
            p.addPropertyChangeListener(new FactoryChangedListener());
            m_typeSettingsPanel.add(p, gbc);

            m_types.put(type, p);
            gbc.gridy++;
        }
        m_scrollPane.setPreferredSize(new Dimension(m_defaultsPanel.getPreferredSize().width + 20, 500));
        updatePMMLLabelColor();
    }

    /** Makes the PMML warning red if a PMML-incompatible missing cell handler was selected. **/
    private void updatePMMLLabelColor() {
        boolean allValidPMML = true;
        // Go through type settings
        for (Component comp : m_typeSettingsPanel.getComponents()) {
            if (!(comp instanceof MissingValueHandlerFactorySelectionPanel)) {
                continue;
            }
            if (!((MissingValueHandlerFactorySelectionPanel)comp).getSelectedFactory().producesPMML4_2()) {
                allValidPMML = false;
                break;
            }
        }
        // Now check the column settings
        if (allValidPMML) {
            for (Component comp : m_individualsPanel.getComponents()) {
                if (!((ColumnHandlingFactorySelectionPanel)comp).getSelectedFactory().producesPMML4_2()) {
                    allValidPMML = false;
                    break;
                }
            }
        }
        if (allValidPMML) {
            m_pmmlLabel1.setForeground(Color.black);
            m_pmmlLabel2.setForeground(Color.black);
        } else {
            m_pmmlLabel1.setForeground(Color.red);
            m_pmmlLabel2.setForeground(Color.red);
        }
    }

    /**
     * Listener that updates the PMML warning when a factory selection changes.
     */
    private class FactoryChangedListener implements PropertyChangeListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (!evt.getPropertyName().equals(MissingValueHandlerFactorySelectionPanel.SELECTED_FACTORY_CHANGE)) {
                return;
            }
            updatePMMLLabelColor();
        }
    }

    /**
     * Panel hosting the individual panels. It implements {@link Scrollable} to allow for correct jumping to the next
     * enclosed panel. It allows overwrites getPreferredSize() to return the sum of all individual heights.
     */
    @SuppressWarnings("serial")
    private class IndividualsPanel extends JPanel implements Scrollable {

        private final ColumnHandlingFactorySelectionPanel m_dummy = createDummyPanel();

        private ColumnHandlingFactorySelectionPanel createDummyPanel() {
            DataColumnSpec cspec = new DataColumnSpecCreator("____________________", DoubleCell.TYPE).createSpec();
            List<DataColumnSpec> cspecs = Arrays.asList(cspec);
            return new ColumnHandlingFactorySelectionPanel(cspecs,
                new PortObjectSpec[]{new DataTableSpec(cspec)}, 0, getHandlerFactoryManager());
        }

        /** Set box layout. */
        public IndividualsPanel() {
            BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
            setLayout(layout);
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, //
                                               final int direction) {
            int rh = getComponentCount() > 0 ? getComponent(0).getHeight() : 0;
            return (rh > 0) ? Math.max(rh, (visibleRect.height / rh) * rh) : visibleRect.height;
        }

        /** {@inheritDoc} */
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return getComponentCount() > 0 ? getComponent(0).getHeight() : 100;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize() {
            if (getComponentCount() < 1) {
                return m_dummy.getPreferredSize();
            }
            int height = 0;
            int width = 0;
            for (Component c : getComponents()) {
                Dimension h = c.getPreferredSize();
                height += h.height;
                width = Math.max(width, h.width);
            }
            return new Dimension(width, height);
        }
    }

    /**
     * @param spec the spec to check
     * @return <code>true</code> if the given spec is marked as incompatible.
     */
    static boolean isIncompatible(final DataColumnSpec spec) {
        return spec.getProperties().containsProperty(INCOMPATIBLE_COLUMN);
    }

    /**
     * @param type the expected type
     * @param dataColumnSpec the spec to check
     * @return <code>false</code> if the actual type of the dataColumnSpec is not compatible to the expected one
     */
    static boolean isIncompatible(final DataType type, final DataColumnSpec dataColumnSpec) {
        DataType colType = dataColumnSpec.getType();
        return !(colType.equals(type) || colType.isASuperTypeOf(type));
    }

    /** @return empty default settings */
    protected MVSettings createEmptySettings() {
        return new MVSettings();
    }

    /** @return manager keeping the missing value handler factories */
    protected MissingCellHandlerFactoryManager getHandlerFactoryManager() {
        return MissingCellHandlerFactoryManager.getInstance();
    }
}
