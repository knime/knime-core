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

import static org.knime.core.node.util.DataColumnSpecListCellRenderer.isInvalid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.ColumnExistenceHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DataTypeHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DomainHandling;
import org.knime.base.node.preproc.datavalidator.dndpanel.DnDConfigurationPanel;
import org.knime.base.node.preproc.datavalidator.dndpanel.DnDConfigurationPanel.DnDConfigurationSubPanel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckedRadioButtonPanel;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * Panel on a ColSetting object. It holds properties for missing values for one individual column or all columns of one
 * type.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
class DataValidatorColPanel extends DnDConfigurationSubPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataValidatorColPanel.class);

    /** Identifier for property change event when Remove was pressed. */
    public static final String REMOVE_ACTION = "remove_panel";

    /** Identifier for property change event when Clean was pressed. */
    public static final String REMOVED_COLUMNS = "remove_incompatible_typed_col";

    private static final String DISABLED_DOMAIN_CHECK_PANEL =
        "<html><font color=#FFA500>Missing domain info</font><br>";

    /** unique JList item placeholder, which has the plus icon and is grey highlighted. */
    private static final Object DROP_COLUMN_PLACEHOLDER = new Object();

    private JCheckBox m_failOnMissingValueCheckBox;

    private JCheckBox m_caseInsensitiveCheckBox;

    private final DataValidatorColConfiguration m_setting;

    private final DataValidatorNodeDialogPane m_parent;

    private CheckedRadioButtonPanel<DomainHandling> m_checkPossbileValue;

    private CheckedRadioButtonPanel<DomainHandling> m_checkMinMax;

    private CheckedRadioButtonPanel<DataTypeHandling> m_checkDataType;

    private DefaultListModel<Object> m_defaultListModel;

    private CheckedRadioButtonPanel<ColumnExistenceHandling> m_checkColumnExistence;

    private boolean m_forceColListToBeShown;

    /**
     * Default constructor.
     */
    DataValidatorColPanel() {
        this(null, true, new DataValidatorColConfiguration(Collections.<DataColumnSpec> emptyList()),
            Collections.<DataColumnSpec> emptyList());
    }

    /**
     * Constructor for one individual column, invoked when Add in dialog was pressed.
     *
     * @param parent the parent dialog pane
     * @param specs list of column specs
     */
    DataValidatorColPanel(final DataValidatorNodeDialogPane parent, final List<DataColumnSpec> specs) {
        this(parent, false, new DataValidatorColConfiguration(specs), specs);
    }

    /**
     * Constructor that uses settings from <code>setting</code> given a column spec or <code>null</code> if the
     * ColSetting is a meta-config.
     *
     * @param parent the parent dialog pane
     * @param forceColListToBeShown the list of columns (west-layout)
     * @param setting to get settings from
     * @param specs data columns specs
     */
    public DataValidatorColPanel(final DataValidatorNodeDialogPane parent, final boolean forceColListToBeShown,
        final DataValidatorColConfiguration setting, final List<DataColumnSpec> specs) {
        setLayout(new GridBagLayout());
        m_parent = parent;
        m_setting = setting;
        m_forceColListToBeShown = forceColListToBeShown;
        createContent(specs);
    }

    /**
     * @param spec the specs to create the layout from
     */
    private void createContent(final List<DataColumnSpec> spec) {
        final List<String> warningMessages = new ArrayList<String>();

        JPanel parentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        final Border border;
        final JComponent removeButtons;

        JButton requestRemoveButton = new JButton("Remove");
        requestRemoveButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                firePropertyChange(REMOVE_ACTION, null, null);
            }
        });

        removeButtons = new JPanel();
        removeButtons.setLayout(new GridLayout(2, 0));
        removeButtons.add(requestRemoveButton);

        final List<DataColumnSpec> notExistingColumns = getNotExistingColumns(spec);

        if (!notExistingColumns.isEmpty()) {
            warningMessages.add("Some columns no longer exist (red bordered)");
        }

        final Set<DataColumnSpec> invalidColumns = new HashSet<DataColumnSpec>();
        invalidColumns.addAll(notExistingColumns);

        if (!invalidColumns.isEmpty()
        // if all columns are invalid a clean is the same as a remove
            && !(invalidColumns.size() == spec.size())) {

            JButton removeNotExistingColumns = new JButton("Clean");
            removeNotExistingColumns.setToolTipText("Removes all invalid columns from the configuration.");

            removeNotExistingColumns.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    DataValidatorColPanel.this.removeAll();

                    // recreate the content, based on the new settings with removed invalid columns
                    diff(m_setting, invalidColumns);
                    createContent(diff(spec, invalidColumns));
                    firePropertyChange(REMOVED_COLUMNS, null,
                        invalidColumns.toArray(new DataColumnSpec[invalidColumns.size()]));
                }

            });
            removeButtons.add(removeNotExistingColumns);
        }

        if (!warningMessages.isEmpty()) {
            LOGGER.warn("get warnings during panel validation: " + warningMessages);
            border = BorderFactory.createLineBorder(Color.RED, 2);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 10, 10, 10);
            c.anchor = GridBagConstraints.NORTH;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            parentPanel.add(createWarningLabel(warningMessages), c);
        } else {
            border = BorderFactory.createLineBorder(Color.BLACK);
        }

        final JPanel centerPanel = new JPanel(new BorderLayout());
        createCenterLayout(m_setting, centerPanel);
        int xOffset = 0;
        if (m_parent != null || m_forceColListToBeShown) {
            createWestLayout(parentPanel, spec);
            JPanel northPanel = new JPanel(new BorderLayout());
            northPanel.add(removeButtons, BorderLayout.EAST);
            centerPanel.add(northPanel, BorderLayout.NORTH);
            setBorder(border);
            xOffset += 1;
        } else {
            setBorder(BorderFactory.createTitledBorder("Column Settings"));
        }

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(10, 10, 10, 10);
        c.gridx = xOffset;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        parentPanel.add(centerPanel, c);

        if (m_forceColListToBeShown) {
            // Table Validator Configuration
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(0, 0, 0, 0);
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.weightx = 1;
            add(parentPanel, c);
        } else {
            // Table Validator (Reference) Configuration
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 0, 0);
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.weightx = 1;
            add(parentPanel, c);
        }

        updateUiElements(spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBorder(final Border border) {
        super.setBorder(border);
    }

    private void createCenterLayout(final DataValidatorColConfiguration setting, final JPanel panel) {
        JPanel centerPanel = new JPanel(new BorderLayout());
        LeftVerticalFlowLayouter layouter = new LeftVerticalFlowLayouter();

        m_caseInsensitiveCheckBox = new JCheckBox("Case insensitive name matching");
        layouter.add(m_caseInsensitiveCheckBox);

        m_failOnMissingValueCheckBox = new JCheckBox("Fail on missing value");
        layouter.add(m_failOnMissingValueCheckBox);

        m_checkColumnExistence =
            new CheckedRadioButtonPanel<>("Check column existence", "", ColumnExistenceHandling.NONE,
                ColumnExistenceHandling.values());

        m_checkDataType =
            new CheckedRadioButtonPanel<>("Check data type", DISABLED_DOMAIN_CHECK_PANEL, DataTypeHandling.NONE,
                DataTypeHandling.values());

        m_checkPossbileValue =
            new CheckedRadioButtonPanel<>("Check possible values", DISABLED_DOMAIN_CHECK_PANEL, DomainHandling.NONE,
                DomainHandling.values());

        m_checkMinMax =
            new CheckedRadioButtonPanel<>("Check min & max", DISABLED_DOMAIN_CHECK_PANEL, DomainHandling.NONE,
                DomainHandling.values());

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(layouter.toPanel(), BorderLayout.WEST);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(m_checkColumnExistence, BorderLayout.NORTH);
        leftPanel.add(m_checkDataType, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(m_checkPossbileValue, BorderLayout.NORTH);
        rightPanel.add(m_checkMinMax, BorderLayout.SOUTH);

        centerPanel.add(leftPanel, BorderLayout.WEST);
        centerPanel.add(rightPanel, BorderLayout.EAST);
        centerPanel.add(northPanel, BorderLayout.NORTH);

        panel.add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * @param tabPanel
     * @param spec
     * @param setting
     * @param icon
     * @param name
     */
    private void createWestLayout(final JPanel tabPanel, final List<DataColumnSpec> spec) {
        m_defaultListModel = new DefaultListModel<>();
        for (DataColumnSpec s : spec) {
            m_defaultListModel.addElement(s);
        }
        m_defaultListModel.addElement(DROP_COLUMN_PLACEHOLDER);
        final JList<Object> jList = new JList<>(m_defaultListModel);

        jList.setCellRenderer(new DataColumnSpecListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") final JList list,
                final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != DROP_COLUMN_PLACEHOLDER) {
                    return this;
                }
                setIcon(DnDConfigurationPanel.ADD_ICON_16);
                setText("<drop columns>");
                setForeground(isSelected ? Color.white : Color.gray);
                return this;
            }
        });
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.addMouseListener(new MouseAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object selectedItem = jList.getSelectedValue();
                    if (selectedItem != null && selectedItem instanceof DataColumnSpec) {
                        DataColumnSpec selectedValue = (DataColumnSpec)selectedItem;
                        m_defaultListModel.removeElement(selectedValue);
                        String colName = selectedValue.getName();
                        List<String> asList = new ArrayList<>(Arrays.asList(m_setting.getNames()));
                        asList.remove(colName);
                        if (asList.isEmpty()) {
                            firePropertyChange(REMOVE_ACTION, null, null);
                        } else {
                            firePropertyChange(REMOVED_COLUMNS, null, new DataColumnSpec[]{selectedValue});
                            DataColumnSpec[] currSpecs = new DataColumnSpec[m_defaultListModel.size() - 1];
                            // let the last item go
                            for (int i = 0; i < m_defaultListModel.size() - 1; i++) {
                                currSpecs[i] = (DataColumnSpec)m_defaultListModel.get(i);
                            }
                            if (isInvalid(selectedValue)) {
                                DataValidatorColPanel.this.removeAll();
                                createContent(Arrays.asList(currSpecs));
                            } else {
                                updateUiElements(Arrays.asList(currSpecs));
                                DataValidatorColPanel.this.repaint();
                            }
                        }
                        m_setting.setNames(asList.toArray(new String[asList.size()]));
                    }
                }
            }
        });

        JScrollPane columns = new JScrollPane(jList);


        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 10, 10, 10);
        c.gridx = 0;
        c.gridy = 1;
        c.gridheight = 2;
        c.weightx = 1;
        tabPanel.add(columns, c);
    }

    /**
     * @param warningMessages
     * @return
     */
    private static Component createWarningLabel(final List<String> warningMessages) {
        JPanel thin = new JPanel(new GridLayout(warningMessages.size(), 1));
        for (int i = 0; i < warningMessages.size(); i++) {
            String message = warningMessages.get(i);
            thin.add(new JLabel(message));
        }
        return thin;
    }

    private static List<DataColumnSpec> getNotExistingColumns(final List<DataColumnSpec> specs) {
        List<DataColumnSpec> toReturn = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec spec : specs) {
            if (isInvalid(spec)) {
                toReturn.add(spec);
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

    /**
     * @param specs the data columns specs to define this panel for
     */
    private boolean updateUiElements(final List<DataColumnSpec> specs) {
        if (m_setting.isCaseInsensitiveNameMatching() && !m_caseInsensitiveCheckBox.isSelected()) {
            m_caseInsensitiveCheckBox.doClick();
        }
        m_failOnMissingValueCheckBox.setSelected(m_setting.isRejectOnMissingValue());
        m_checkDataType.setSelectedValue(m_setting.getDataTypeHandling());
        m_checkMinMax.setSelectedValue(m_setting.getDomainHandlingMinMax());
        m_checkPossbileValue.setSelectedValue(m_setting.getDomainHandlingPossbileValues());
        m_checkColumnExistence.setSelectedValue(m_setting.getColumnExistenceHandling());

        checkDomainMinMax(specs);
        checkDomainPossibleValues(specs);
        return true;
    }

    /**
     * @param specs
     */
    private void checkDomainMinMax(final List<DataColumnSpec> specs) {
        m_checkMinMax.setEnabled(true);
        for (DataColumnSpec spec : specs) {
            if (spec.getDomain().hasLowerBound() || spec.getDomain().hasUpperBound()) {
                return;
            }
        }
        m_checkMinMax.setEnabled(false);
    }

    /**
     * @param specs
     */
    private void checkDomainPossibleValues(final List<DataColumnSpec> specs) {
        m_checkPossbileValue.setEnabled(true);
        for (DataColumnSpec spec : specs) {
            if (spec.getDomain().hasValues()) {
                return;
            }
        }
        m_checkPossbileValue.setEnabled(false);
    }

    /**
     * Get the settings currently entered in the dialog.
     *
     * @return the current settings
     */
    public DataValidatorColConfiguration getSettings() {

        m_setting.setCaseInsensitiveNameMatching(m_caseInsensitiveCheckBox.isSelected());
        m_setting.setColumnExistenceHandling(m_checkColumnExistence.getSelectedValue());
        m_setting.setRejectOnMissingValue(m_failOnMissingValueCheckBox.isSelected());
        m_setting.setDataTypeHandling(m_checkDataType.getSelectedValue());
        m_setting.setDomainHandlingMinMax(m_checkMinMax.getSelectedValue());
        m_setting.setDomainHandlingPossbileValues(m_checkPossbileValue.getSelectedValue());

        return m_setting;
    }

    /**
     * @return the names for the current configuration
     */
    public String[] getColumnNames() {
        return m_setting.getNames();
    }

    private static List<DataColumnSpec>
        diff(final List<DataColumnSpec> specs, final Set<DataColumnSpec> invalidColumns) {
        List<DataColumnSpec> toReturn = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec currSpec : specs) {
            if (!invalidColumns.contains(currSpec)) {
                toReturn.add(currSpec);
            }
        }
        return toReturn;
    }

    private static DataValidatorColConfiguration diff(final DataValidatorColConfiguration setting,
        final Set<DataColumnSpec> invalidColumns) {
        List<String> toReturn = new ArrayList<String>(Arrays.asList(setting.getNames()));
        for (DataColumnSpec currSpec : invalidColumns) {
            toReturn.remove(currSpec.getName());
        }
        setting.setNames(toReturn.toArray(new String[toReturn.size()]));
        return setting;
    }

    private static class LeftVerticalFlowLayouter {
        private List<Component> m_components = new ArrayList<>();

        void add(final Component comp) {
            m_components.add(comp);
        }

        JPanel toPanel() {
            JPanel toReturn = new JPanel(new BorderLayout());
            for (Component comp : m_components) {
                toReturn.add(comp, BorderLayout.WEST);
                JPanel p = new JPanel(new BorderLayout());
                p.add(toReturn, BorderLayout.NORTH);
                toReturn = p;
            }

            return toReturn;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDropable(final List<DataColumnSpec> data) {
        return m_parent.isDropable(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(final List<DataColumnSpec> specs) {
        List<DataColumnSpec> arrayList = new ArrayList<DataColumnSpec>(specs);

        for (int i = 0; i < m_defaultListModel.size(); i++) {
            Object object = m_defaultListModel.get(i);
            if (object instanceof DataColumnSpec) {
                arrayList.add((DataColumnSpec)object);
            }
        }
        DataValidatorColPanel.this.removeAll();
        String[] newCols = new String[arrayList.size()];
        int index = 0;
        for (DataColumnSpec cls : arrayList) {
            newCols[index++] = cls.getName();
        }
        m_setting.setNames(newCols);
        createContent(arrayList);
        revalidate();
        firePropertyChange(REMOVED_COLUMNS, null, new DataColumnSpec[]{});
        return true;
    }
}
