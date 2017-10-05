/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.base.node.preproc.missingval;

import static org.knime.core.node.util.DataColumnSpecListCellRenderer.isInvalid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.ViewUtils;

/**
 * Panel on a ColSetting object. It holds properties for missing values for one individual column or all columns of one
 * type.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @deprecated See new Missing node that incorporates a PMML outport in package
 * org.knime.base.node.preproc.pmml.missingval
 */
@SuppressWarnings("serial")
@Deprecated
final class MissingValueHandling2Panel extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MissingValueHandling2Panel.class);

    /** Identifier for property change event when Remove was pressed. */
    public static final String REMOVE_ACTION = "remove_panel";

    /** Identifier for property change event when Clean was pressed. */
    public static final String REMOVED_INVALID_COLUMNS = "remove_incompatible_typed_col";

    private JRadioButton m_nothingButton;

    private JRadioButton m_removeButton;

    private JRadioButton m_minButton;

    private JRadioButton m_maxButton;

    private JRadioButton m_meanButton;

    private JRadioButton m_mostFrequentButton;

    private JRadioButton m_fixButton;

    private JComponent m_fixText;

    private MissingValueHandling2ColSetting m_setting;

    /**
     * Constructor for one individual column, invoked when Add in dialog was pressed.
     *
     * @param spec the spec to that column
     */
    public MissingValueHandling2Panel(final DataColumnSpec spec) {
        this(new MissingValueHandling2ColSetting(spec), spec);
    }

    /**
     * Constructor for one individual column, invoked when Add in dialog was pressed.
     *
     * @param specs list of column specs
     */
    public MissingValueHandling2Panel(final List<DataColumnSpec> specs) {
        this(new MissingValueHandling2ColSetting(specs), specs.toArray(new DataColumnSpec[0]));
    }

    /**
     * Constructor that uses settings from <code>setting</code> given a column spec or <code>null</code> if the
     * ColSetting is a meta-config.
     *
     * @param setting to get settings from
     * @param spec the spec of the column or <code>null</code>
     */
    public MissingValueHandling2Panel(final MissingValueHandling2ColSetting setting, final DataColumnSpec... spec) {
        super(new FlowLayout(FlowLayout.CENTER, 10, 10));
        createContent(setting, spec);
    }

    /**
     * @param setting
     * @param spec
     * @throws InternalError
     */
    private void createContent(final MissingValueHandling2ColSetting setting, final DataColumnSpec... spec)
        throws InternalError {
        final List<String> warningMessages = new ArrayList<String>();

        // if we got incompatible types the original type is overwritten by unkown.
        int settingTypeBackup = setting.getType();

        JPanel tabPanel = new JPanel(new BorderLayout(0, 5));
        final JPanel panel = new JPanel(new GridLayout(0, 2));
        final Icon icon;
        final String name;
        final Border border;
        final JComponent removePanel;
        if (setting.isMetaConfig()) {
            switch (setting.getType()) {
                case MissingValueHandling2ColSetting.TYPE_INT:
                    icon = IntCell.TYPE.getIcon();
                    name = "Integer";
                    border = BorderFactory.createTitledBorder("Integer Columns");
                    break;
                case MissingValueHandling2ColSetting.TYPE_STRING:
                    icon = StringCell.TYPE.getIcon();
                    name = "String";
                    border = BorderFactory.createTitledBorder("String Columns");
                    break;
                case MissingValueHandling2ColSetting.TYPE_DOUBLE:
                    icon = DoubleCell.TYPE.getIcon();
                    name = "Double";
                    border = BorderFactory.createTitledBorder("Double Columns");
                    break;
                case MissingValueHandling2ColSetting.TYPE_UNKNOWN:
                    icon = DataType.getType(DataCell.class).getIcon();
                    name = "Unknown";
                    border = BorderFactory.createTitledBorder("Unknown Columns");
                    break;
                default:
                    throw new InternalError("No such type.");
            }
            removePanel = new JLabel();
        } else {
            final List<String> names = new ArrayList<String>(Arrays.asList(setting.getNames()));
            for (DataColumnSpec cspec : spec) {
                names.remove(cspec.getName());
            }
            if (!names.isEmpty()) {
                throw new NullPointerException("Not equal on init: '" + Arrays.toString(setting.getNames()) + "' vs. '"
                    + Arrays.toString(spec) + "'.");
            }
            name = setting.getDisplayName();
            icon = spec[0].getType().getIcon();

            JButton requestRemoveButton = new JButton("Remove");
            requestRemoveButton.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    firePropertyChange(REMOVE_ACTION, null, null);
                }
            });

            removePanel = new JPanel();
            removePanel.setLayout(new GridLayout(2, 0));
            removePanel.add(requestRemoveButton);

            final List<DataColumnSpec> notExistingColumns = getNotExistingColumns(spec);
            final List<DataColumnSpec> incompatibleColumns = getIncompatibleTypedColumns(setting.getType(), spec);

            if (!notExistingColumns.isEmpty()) {
                warningMessages.add("Some columns no longer exist (red bordered)");
            }

            if (!incompatibleColumns.isEmpty()) {
                warningMessages.add(String.format("Some columns have an incompatible type to %s (yellow borderd)",
                    typeToString(setting.getType())));
            }

            final Set<DataColumnSpec> invalidColumns = new HashSet<DataColumnSpec>();
            invalidColumns.addAll(notExistingColumns);
            invalidColumns.addAll(incompatibleColumns);

            if (!incompatibleColumns.isEmpty()) {
                setting.setType(MissingValueHandling2ColSetting.TYPE_UNKNOWN);
            }

            if (!invalidColumns.isEmpty()
            // if all columns are invalid a clean is the same as a remove
                && !(invalidColumns.size() == spec.length)) {

                JButton removeNotExistingColumns = new JButton("Clean");
                removeNotExistingColumns.setToolTipText("Removes all invalid columns from the configuration.");

                removeNotExistingColumns.addActionListener(new ActionListener() {
                    /** {@inheritDoc} */
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        MissingValueHandling2Panel.this.removeAll();

                        // recreate the content, based on the new settings with removed invalid columns
                        createContent(diff(m_setting, invalidColumns), diff(spec, invalidColumns));
                        firePropertyChange(REMOVED_INVALID_COLUMNS, null,
                            invalidColumns.toArray(new DataColumnSpec[invalidColumns.size()]));
                    }

                });
                removePanel.add(removeNotExistingColumns);
            }

            if (!warningMessages.isEmpty()) {
                LOGGER.warn("get warnings during panel validation: " + warningMessages);
                border = BorderFactory.createLineBorder(Color.RED, 2);
                tabPanel.add(createWarningLabel(warningMessages), BorderLayout.NORTH);
            } else {
                border = BorderFactory.createLineBorder(Color.BLACK);
            }

        }

        createWestLayout(setting, tabPanel, icon, name, spec);

        panel.add(createSpacer(1));
        panel.add(removePanel);

        createEastLayout(setting, panel, spec);

        setting.setType(settingTypeBackup);
        m_setting = setting;

        setBorder(border);

        tabPanel.add(panel, BorderLayout.CENTER);
        tabPanel.add(createSpacer(65), BorderLayout.SOUTH);
        add(tabPanel);
    }

    /**
     * @param type
     * @return
     */
    private static String typeToString(final int type) {
        switch (type) {
            case MissingValueHandling2ColSetting.TYPE_DOUBLE:
                return "Double";
            case MissingValueHandling2ColSetting.TYPE_INT:
                return "Int";
            case MissingValueHandling2ColSetting.TYPE_STRING:
                return "String";
            default:
                break;
        }
        return null;
    }

    /**
     * @param setting
     * @param panel
     * @param spec
     */
    private void createEastLayout(final MissingValueHandling2ColSetting setting, final JPanel panel,
        final DataColumnSpec... spec) {
        ButtonGroup buttonGroup = new ButtonGroup();
        ActionListener actionListener = new ButtonListener();
        // NO HANDLING Button
        m_nothingButton = new JRadioButton("Do Nothing");
        m_nothingButton.setToolTipText("No missing value handling.");
        m_nothingButton.addActionListener(actionListener);
        buttonGroup.add(m_nothingButton);
        panel.add(m_nothingButton);

        // REMOVE Button
        m_removeButton = new JRadioButton("Remove Row");
        m_removeButton.setToolTipText("Ignore rows that contain a " + "missing value");
        m_removeButton.addActionListener(actionListener);
        buttonGroup.add(m_removeButton);
        panel.add(m_removeButton);

        if (setting.getType() == MissingValueHandling2ColSetting.TYPE_DOUBLE
            || setting.getType() == MissingValueHandling2ColSetting.TYPE_INT) {
            // MIN Button
            m_minButton = new JRadioButton("Min");
            m_minButton.setToolTipText("Replaces missing values by the minimum " + "of the values in a column");
            m_minButton.addActionListener(actionListener);
            buttonGroup.add(m_minButton);
            panel.add(m_minButton);

            // MAX Button
            m_maxButton = new JRadioButton("Max");
            m_maxButton.setToolTipText("Replaces missing values by the " + "maximum of the values in a column");
            m_maxButton.addActionListener(actionListener);
            buttonGroup.add(m_maxButton);
            panel.add(m_maxButton);

            // MEAN Button
            m_meanButton = new JRadioButton("Mean");
            m_meanButton.setToolTipText("Replaces missing values by the mean " + "of the values in a column");
            m_meanButton.addActionListener(actionListener);
            buttonGroup.add(m_meanButton);
            panel.add(m_meanButton);
            if (setting.getType() == MissingValueHandling2ColSetting.TYPE_DOUBLE) {
                panel.add(new JLabel()); // even number of components
            }
        } else {
            m_meanButton = null;
            m_minButton = null;
            m_maxButton = null;
        }
        if (setting.getType() == MissingValueHandling2ColSetting.TYPE_INT
            || setting.getType() == MissingValueHandling2ColSetting.TYPE_STRING) {
            m_mostFrequentButton = new JRadioButton("Most Frequent");
            m_mostFrequentButton.setToolTipText("Replaces missing values " + "by the most frequent value in a column");
            m_mostFrequentButton.addActionListener(actionListener);
            buttonGroup.add(m_mostFrequentButton);
            panel.add(m_mostFrequentButton);
            if (setting.getType() == MissingValueHandling2ColSetting.TYPE_STRING) {
                panel.add(new JLabel()); // even number of components
            }
        } else {
            m_mostFrequentButton = null;
        }

        if (setting.getType() != MissingValueHandling2ColSetting.TYPE_UNKNOWN) {
            // FIX Button
            m_fixButton = new JRadioButton("Fix Value: ");
            m_fixButton.setToolTipText("Replaces missing values by a fixed value");
            m_fixButton.addActionListener(actionListener);
            buttonGroup.add(m_fixButton);
            panel.add(m_fixButton);
            m_fixText = getFixTextField(setting, spec);
            JPanel fixPanel = new JPanel(new BorderLayout());
            fixPanel.add(minHeight(createSpacer(1)), BorderLayout.NORTH);
            fixPanel.add(ViewUtils.getInFlowLayout(m_fixText), BorderLayout.CENTER);
            fixPanel.add(minHeight(createSpacer(1)), BorderLayout.SOUTH);
            panel.add(fixPanel);
        } else {
            m_fixButton = null;
            m_fixText = null;

        }
        switch (setting.getMethod()) {
            case MissingValueHandling2ColSetting.METHOD_FIX_VAL:
                m_fixButton.doClick();
                break;
            case MissingValueHandling2ColSetting.METHOD_IGNORE_ROWS:
                m_removeButton.doClick();
                break;
            case MissingValueHandling2ColSetting.METHOD_MOST_FREQUENT:
                m_mostFrequentButton.doClick();
                break;
            case MissingValueHandling2ColSetting.METHOD_MAX:
                m_maxButton.doClick();
                break;
            case MissingValueHandling2ColSetting.METHOD_MEAN:
                m_meanButton.doClick();
                break;
            case MissingValueHandling2ColSetting.METHOD_MIN:
                m_minButton.doClick();
                break;
            default:
                m_nothingButton.doClick();
        }
    }

    /**
     * @param setting
     * @param tabPanel
     * @param icon
     * @param name
     * @param spec
     */
    private void createWestLayout(final MissingValueHandling2ColSetting setting, final JPanel tabPanel,
        final Icon icon, final String name, final DataColumnSpec... spec) {
        String shortName = name;
        if (setting.isMetaConfig()) {
            if (name.length() > 15) {
                shortName = name.substring(0, 14).concat("...");
            }
            JLabel jLabel = new JLabel(shortName, icon, SwingConstants.LEFT);
            jLabel.setToolTipText(name);

            JPanel typePanel = new JPanel(new BorderLayout());
            typePanel.add(jLabel, BorderLayout.NORTH);
            tabPanel.add(typePanel, BorderLayout.WEST);
        } else {
            final JList jList = new JList(spec);
            jList.setCellRenderer(new DataColumnSpecListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                    // The super method will reset the icon if we call this method
                    // last. So we let super do its job first and then we take care
                    // that everything is properly set.
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                    String text = null;
                    if (value instanceof DataColumnSpec
                        && MissingValueHandling2NodeDialogPane.isIncompatible((DataColumnSpec)value)) {
                        setBorder(BorderFactory.createLineBorder(Color.YELLOW));
                        text = "Column " + ((DataColumnSpec)value).getName() + " is incompatible to the current settings";
                    }
                    setToolTipText(text);
                    return this;
                }
            });
            jList.addListSelectionListener(new ListSelectionListener() {

                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    jList.setSelectedIndices(new int[0]);
                }
            });
            // jList.setSE
            JScrollPane columns = new JScrollPane(jList);
            columns.setMaximumSize(new Dimension(150, 150));
            columns.setPreferredSize(new Dimension(100, 150));

            tabPanel.add(columns, BorderLayout.WEST);

        }
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

    private static List<DataColumnSpec> getNotExistingColumns(final DataColumnSpec... specs) {
        List<DataColumnSpec> toReturn = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec spec : specs) {
            if (isInvalid(spec)) {
                toReturn.add(spec);
            }
        }
        return toReturn;
    }

    /**
     * @param type
     * @param spec
     * @return
     */
    private static List<DataColumnSpec> getIncompatibleTypedColumns(final int type, final DataColumnSpec[] specs) {
        List<DataColumnSpec> toReturn = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < specs.length; i++) {
            DataColumnSpec spec = specs[i];
            if (MissingValueHandling2NodeDialogPane.isIncompatible(type, spec)) {
                toReturn.add(spec);
            }
        }
        return toReturn;
    }

    /**
     * @param createSpacer
     * @return
     */
    private Component minHeight(final JLabel createSpacer) {
        createSpacer.setPreferredSize(new Dimension(createSpacer.getWidth(), 5));
        return createSpacer;
    }

    /**
     * @return
     */
    private JLabel createSpacer(final int size) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append("L");
        }
        JLabel jLabel = new JLabel(builder.toString());
        jLabel.setForeground(getBackground());
        return jLabel;
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
     * Get the settings currently entered in the dialog.
     *
     * @return the current settings
     */
    public MissingValueHandling2ColSetting getSettings() {
        int method;
        if (m_nothingButton.isSelected()) {
            method = MissingValueHandling2ColSetting.METHOD_NO_HANDLING;
        } else if (m_removeButton.isSelected()) {
            method = MissingValueHandling2ColSetting.METHOD_IGNORE_ROWS;
        } else if (m_fixButton != null && m_fixButton.isSelected()) {
            method = MissingValueHandling2ColSetting.METHOD_FIX_VAL;
            DataCell cell;
            switch (m_setting.getType()) {
                case MissingValueHandling2ColSetting.TYPE_INT:
                    Object value = ((JFormattedTextField)m_fixText).getValue();
                    cell = new IntCell(((Number)value).intValue());
                    break;
                case MissingValueHandling2ColSetting.TYPE_DOUBLE:
                    value = ((JFormattedTextField)m_fixText).getValue();
                    cell = new DoubleCell(((Number)value).doubleValue());
                    break;
                case MissingValueHandling2ColSetting.TYPE_STRING:
                    value = ((JComboBox)m_fixText).getEditor().getItem();
                    cell = new StringCell(value.toString());
                    break;
                default:
                    throw new RuntimeException("You shouldn't have come here.");
            }
            m_setting.setFixCell(cell);
        } else if (m_maxButton != null && m_maxButton.isSelected()) {
            method = MissingValueHandling2ColSetting.METHOD_MAX;
        } else if (m_minButton != null && m_minButton.isSelected()) {
            method = MissingValueHandling2ColSetting.METHOD_MIN;
        } else if (m_meanButton != null && m_meanButton.isSelected()) {
            method = MissingValueHandling2ColSetting.METHOD_MEAN;
        } else if (m_mostFrequentButton != null && m_mostFrequentButton.isSelected()) {
            method = MissingValueHandling2ColSetting.METHOD_MOST_FREQUENT;
        } else {
            assert false : "One button must be selected.";
            method = MissingValueHandling2ColSetting.METHOD_NO_HANDLING;
        }
        m_setting.setMethod(method);
        return m_setting;
    }

    /*
     * Helper in constructor, generates the text field to enter the replacement
     * value.
     */
    private static JComponent getFixTextField(final MissingValueHandling2ColSetting setting,
        final DataColumnSpec... specs) {
        JComponent fixText;
        // FIX text field
        DataCell fixCell = setting.getFixCell();
        switch (setting.getType()) {
            case MissingValueHandling2ColSetting.TYPE_DOUBLE:
                fixText = new JFormattedTextField();
                ((JFormattedTextField)fixText).setColumns(11);
                Double doubel;
                if (fixCell == null) {
                    doubel = new Double(0.0);
                } else {
                    double d = ((DoubleValue)fixCell).getDoubleValue();
                    doubel = new Double(d);
                }
                ((JFormattedTextField)fixText).setValue(doubel);
                break;
            case MissingValueHandling2ColSetting.TYPE_INT:
                fixText = new JFormattedTextField();
                ((JFormattedTextField)fixText).setColumns(11);
                Integer integer;
                if (fixCell == null) {
                    integer = 0;
                } else {
                    integer = ((IntValue)fixCell).getIntValue();
                }
                ((JFormattedTextField)fixText).setValue(integer);
                break;
            case MissingValueHandling2ColSetting.TYPE_STRING:
                final ArrayList<DataCell> vals = new ArrayList<DataCell>();
                if (specs != null) {
                    for (DataColumnSpec spec : specs) {
                        if (spec != null && spec.getDomain().hasValues()) {
                            vals.addAll(spec.getDomain().getValues());
                        }
                    }
                }
                DefaultComboBoxModel model = new DefaultComboBoxModel(vals.toArray(new DataCell[0]));
                fixText = new JComboBox(model);
                ((JComboBox)fixText).setPrototypeDisplayValue("#########");
                ((JComboBox)fixText).setEditable(true);
                ((JComboBox)fixText).setRenderer(new DefaultListCellRenderer() {
                    /**
                     * Overridden to set tooltip text properly.
                     *
                     * @see DefaultListCellRenderer#getListCellRendererComponent(JList, Object, int, boolean, boolean)
                     */
                    @Override
                    public Component getListCellRendererComponent(final JList list, final Object value,
                        final int index, final boolean isSelected, final boolean cellHasFocus) {
                        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (c instanceof JComponent) {
                            ((JComponent)c).setToolTipText(value.toString());
                        }
                        return c;
                    }
                });
                String string;
                if (fixCell == null) {
                    string = "";
                } else {
                    string = ((StringValue)fixCell).getStringValue();
                }
                model.setSelectedItem(string);
                break;
            default:
                throw new InternalError("No such type");
        }
        return fixText;
    }

    private static DataColumnSpec[] diff(final DataColumnSpec[] specs, final Set<DataColumnSpec> invalidColumns) {
        List<DataColumnSpec> toReturn = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec currSpec : specs) {
            if (!invalidColumns.contains(currSpec)) {
                toReturn.add(currSpec);
            }
        }
        return toReturn.toArray(new DataColumnSpec[toReturn.size()]);
    }

    private static MissingValueHandling2ColSetting diff(final MissingValueHandling2ColSetting setting,
        final Set<DataColumnSpec> invalidColumns) {
        List<String> toReturn = new ArrayList<String>(Arrays.asList(setting.getNames()));
        for (DataColumnSpec currSpec : invalidColumns) {
            toReturn.remove(currSpec.getName());
        }
        setting.setNames(toReturn.toArray(new String[toReturn.size()]));
        return setting;
    }

    /** Action Listener for buttons. */
    private class ButtonListener implements ActionListener {
        /** {@inheritDoc} */
        @Override
        public void actionPerformed(final ActionEvent e) {
            if (m_fixButton == null) {
                return;
            }
            m_fixText.setEnabled(m_fixButton.isSelected());
            if (m_fixButton.isSelected()) {
                m_fixText.requestFocus();
            }
        }
    }
}
