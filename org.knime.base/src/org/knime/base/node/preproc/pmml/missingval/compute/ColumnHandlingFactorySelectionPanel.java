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
 *   07.01.2015 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval.compute;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.base.node.preproc.pmml.missingval.MVColumnSettings;
import org.knime.base.node.preproc.pmml.missingval.MissingCellHandlerFactory;
import org.knime.base.node.preproc.pmml.missingval.MissingCellHandlerFactoryManager;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;


/**
 * Panel that allows setting the missing cell handler for an arbitrary group of columns.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class ColumnHandlingFactorySelectionPanel extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ColumnHandlingFactorySelectionPanel.class);

    /** Identifier for property change event when Remove was pressed. */
    public static final String REMOVE_ACTION = "remove_panel";

    /** Identifier for property change event when Clean was pressed. */
    public static final String REMOVED_INVALID_COLUMNS = "remove_incompatible_typed_col";

    private MVColumnSettings m_settings;
    private MissingValueHandlerFactorySelectionPanel m_settingsPanel;

    private final MissingCellHandlerFactoryManager m_factoryManager;

    /**
     * Constructor for ColumnHandlingFactorySelectionPanel.
     * @param cols the columns for which settings are made in this panel
     * @param specs the input specs
     * @param tableIndex the index of the input table in the specs
     * @param factoryManager manager keeping the missing value handler factories
     */
    public ColumnHandlingFactorySelectionPanel(final List<DataColumnSpec> cols,
                                                final PortObjectSpec[] specs,
                                                final int tableIndex,
                                                final MissingCellHandlerFactoryManager factoryManager) {
        m_settings = new MVColumnSettings(factoryManager);
        for (DataColumnSpec cspec : cols) {
            m_settings.getColumns().add(cspec.getName());
        }
        m_factoryManager = factoryManager;
        createContent(specs, tableIndex);
    }

    /**
     * Constructor for the MissingValueHandlerFactorySelectionPanel.
     * @param s the current settings for this data type
     * @param specs the input specs
     * @param tableIndex the index of the input table in the specs
     * @param factoryManager manager keeping the missing value handler factories
     */
    public ColumnHandlingFactorySelectionPanel(final MVColumnSettings s,
                                                final PortObjectSpec[] specs,
                                                final int tableIndex,
                                                final MissingCellHandlerFactoryManager factoryManager) {
        m_settings = s;
        m_factoryManager = factoryManager;
        createContent(specs, tableIndex);
    }

    /**
     * @return The selected factory.
     */
    public MissingCellHandlerFactory getSelectedFactory() {
        return m_settingsPanel.getSelectedFactory();
    }

    /**
     * @return the columns for which this panel handles the settings
     */
    public Set<String> getColumns() {
        return m_settings.getColumns();
    }

    /**
     * @return the settings made for the columns
     * @throws InvalidSettingsException if settings cannot be loaded from a custom panel
     */
    public MVColumnSettings getUpdatedSettings() throws InvalidSettingsException {
        m_settings.setSettings(m_settingsPanel.getSettings());
        return m_settings;
    }

    private void createContent(final PortObjectSpec[] specs, final int tableIndex) {
        this.setLayout(new BorderLayout());

        final DataTableSpec tableSpec = (DataTableSpec)specs[tableIndex];
        final JList<String> jList = new JList<String>(m_settings.getColumns().toArray(new String[0]));
        final Border border;
        final JPanel eastPanel = new JPanel(new BorderLayout());

        jList.setCellRenderer(new DataColumnSpecListCellRenderer() {
            @SuppressWarnings("rawtypes")
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
                DataColumnSpec spec = tableSpec.getColumnSpec(value.toString());
                if (spec == null) {
                    spec = createInvalidSpec(value.toString());
                }

                // The super method will reset the icon if we call this method
                // last. So we let super do its job first and then we take care
                // that everything is properly set.
                super.getListCellRendererComponent(list, spec, index, isSelected, cellHasFocus);

                if (MissingValueHandlerNodeDialog.isIncompatible(spec)) {
                        setBorder(BorderFactory.createLineBorder(Color.YELLOW));
                    }

                return this;
            }
        });
        jList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                jList.setSelectedIndices(new int[0]);
            }
        });

        // Make the list scrollable by embedding it in a scroll pane
        JScrollPane columns = new JScrollPane(jList);
        columns.setMaximumSize(new Dimension(120, 150));
        columns.setPreferredSize(new Dimension(120, 150));

        // This button fires the remove event so that the panel is removed from its parent
        JButton requestRemoveButton = new JButton("Remove");
        requestRemoveButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                firePropertyChange(REMOVE_ACTION, null, null);
            }
        });

        JPanel removePanel = new JPanel();
        removePanel.setLayout(new GridLayout(0, 2));

        final List<String> notExistingColumns = getNotExistingColumns(tableSpec);
        // If the factory allows all types, there are no incompatible columns
        final List<String> incompatibleColumns =
                getIncompatibleTypedColumns(m_settings.getSettings().getFactory(), tableSpec);
        final List<String> warningMessages = new ArrayList<String>();

        if (!notExistingColumns.isEmpty()) {
            warningMessages.add("Some columns no longer exist (red bordered)");
        }

        if (!incompatibleColumns.isEmpty()) {
            warningMessages.add(String.format("Some columns have an incompatible type for factory %s (yellow borderd)",
                m_settings.getSettings().getFactory().getDisplayName()));
        }

        final Set<String> invalidColumns = new HashSet<String>();
        invalidColumns.addAll(notExistingColumns);
        invalidColumns.addAll(incompatibleColumns);

        if (!invalidColumns.isEmpty()
            // if all columns are invalid a clean is the same as a remove
            && !(invalidColumns.size() == m_settings.getColumns().size())) {

            final JButton removeNotExistingColumns = new JButton("Clean");
            removeNotExistingColumns.setToolTipText("Removes all invalid columns from the configuration.");

            removeNotExistingColumns.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    List<String> toRemove = new ArrayList<String>();
                    // recreate the content, based on the new settings with removed invalid columns
                    for (String s : m_settings.getColumns()) {
                        for (String spec : invalidColumns) {
                            if (spec.equals(s)) {
                                toRemove.add(s);
                            }
                        }
                    }
                    for (String s : toRemove) {
                        m_settings.getColumns().remove(s);
                    }
                    removeNotExistingColumns.setVisible(false);

                    jList.setListData(m_settings.getColumns().toArray(new String[0]));
                    ColumnHandlingFactorySelectionPanel.this.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                    firePropertyChange(REMOVED_INVALID_COLUMNS, null,
                        invalidColumns.toArray(new String[invalidColumns.size()]));
                }

            });
            removePanel.add(removeNotExistingColumns);
        } else {
            // Add dummy label to keep the remove button on the right side
            removePanel.add(new JLabel());
        }
        removePanel.add(requestRemoveButton);

        if (!warningMessages.isEmpty()) {
            LOGGER.warn("get warnings during panel validation: " + warningMessages);
            border = BorderFactory.createLineBorder(Color.RED, 2);
            eastPanel.add(createWarningLabel(warningMessages), BorderLayout.NORTH);
        } else {
            border = BorderFactory.createLineBorder(Color.GRAY);
        }
        setBorder(border);
        eastPanel.add(removePanel, BorderLayout.NORTH);

        // Determine data type
        List<String> validCols = new ArrayList<String>();
        validCols.addAll(m_settings.getColumns());
        validCols.removeAll(invalidColumns);

        DataType[] dt = new DataType[validCols.size()];
        if (validCols.size() > 0) {
            for (int i = 0; i < validCols.size(); i++) {
                dt[i] = tableSpec.getColumnSpec(validCols.get(i)).getType();
            }
        }

        m_settingsPanel =
            new MissingValueHandlerFactorySelectionPanel(dt, m_settings.getSettings(), m_factoryManager, specs);
        m_settingsPanel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(MissingValueHandlerFactorySelectionPanel.SELECTED_FACTORY_CHANGE)) {
                    ColumnHandlingFactorySelectionPanel.this.firePropertyChange(
                        MissingValueHandlerFactorySelectionPanel.SELECTED_FACTORY_CHANGE, null, null);
                }
            }
        });
        eastPanel.add(m_settingsPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, columns, eastPanel);
        this.add(splitPane, BorderLayout.CENTER);
        splitPane.setResizeWeight(0.5);
    }

    private static Component createWarningLabel(final List<String> warningMessages) {
        JPanel thin = new JPanel(new GridLayout(warningMessages.size(), 1));
        for (int i = 0; i < warningMessages.size(); i++) {
            String message = warningMessages.get(i);
            thin.add(new JLabel(message));
        }
        return thin;
    }

    private List<String> getNotExistingColumns(final DataTableSpec tableSpec) {
        List<String> toReturn = new ArrayList<String>();
        for (String col : m_settings.getColumns()) {
            if (!tableSpec.containsName(col)) {
                toReturn.add(col);
            }
        }
        return toReturn;
    }

    private List<String> getIncompatibleTypedColumns(final MissingCellHandlerFactory fac,
                                                     final DataTableSpec tableSpec) {
        List<String> toReturn = new ArrayList<String>();
        for (String col : m_settings.getColumns()) {
            DataColumnSpec spec = tableSpec.getColumnSpec(col);
            if (spec != null && !fac.isApplicable(spec.getType())) {
                toReturn.add(col);
            }
        }
        return toReturn;
    }

    /**
     * Register a <code>MouseListener</code> on the label used to display the columns. Used to select all corresponding
     * columns that fall into this individual missing setting property.
     *
     * @param ml the mouse listener to be registered
     */
    protected void registerMouseListener(final MouseListener ml) {
        this.addMouseListener(ml);
    }
}
