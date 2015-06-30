/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
package org.knime.timeseries.node.timemissvaluehandler;

import static org.knime.core.node.util.CheckUtils.checkArgumentNotNull;
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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.timeseries.node.timemissvaluehandler.TimeMissingValueHandlingColSetting.ConfigType;
import org.knime.timeseries.node.timemissvaluehandler.TimeMissingValueHandlingColSetting.HandlingMethod;

/**
 * Panel on a ColSetting object. It holds properties for missing values for one individual column or all columns of one
 * type.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Marcel Hanser, University of Konstanz
 * @deprecated See new missing node that incorporates time series handling in package
 * org.knime.base.node.preproc.pmml.missingval
 */
@Deprecated
@SuppressWarnings("serial")
final class TimeMissingValueHandlingPanel extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TimeMissingValueHandlingPanel.class);

    /** Identifier for property change event when Remove was pressed. */
    public static final String REMOVE_ACTION = "remove_panel";

    /** Identifier for property change event when Clean was pressed. */
    public static final String REMOVED_INVALID_COLUMNS = "remove_incompatible_typed_col";

    private JComboBox<HandlingMethod> m_comboBox = new JComboBox<HandlingMethod>();

    private TimeMissingValueHandlingColSetting m_setting;

    /**
     * Constructor for one individual column, invoked when Add in dialog was pressed.
     *
     * @param spec the spec to that column
     */
    public TimeMissingValueHandlingPanel(final DataColumnSpec spec) {
        this(new TimeMissingValueHandlingColSetting(spec), spec);
    }

    /**
     * Constructor for one individual column, invoked when Add in dialog was pressed.
     *
     * @param specs list of column specs
     */
    public TimeMissingValueHandlingPanel(final List<DataColumnSpec> specs) {
        this(new TimeMissingValueHandlingColSetting(specs), specs.toArray(new DataColumnSpec[0]));
    }

    /**
     * Constructor that uses settings from <code>setting</code> given a column spec or <code>null</code> if the
     * ColSetting is a meta-config.
     *
     * @param setting to get settings from
     * @param spec the spec of the column or <code>null</code>
     */
    public TimeMissingValueHandlingPanel(final TimeMissingValueHandlingColSetting setting, //
        final DataColumnSpec... spec) {
        super(new FlowLayout(FlowLayout.CENTER, 10, 10));
        createContent(setting, spec);
    }

    private void createContent(final TimeMissingValueHandlingColSetting setting, final DataColumnSpec... spec)
        throws InternalError {
        final List<String> warningMessages = new ArrayList<String>();

        // if we got incompatible types the original type is overwritten by unkown.
        ConfigType settingTypeBackup = setting.getType();

        JPanel tabPanel = new JPanel(new BorderLayout(0, 5));
        final JPanel comboRemovePanel = new JPanel(new BorderLayout(0, 2));
        Border border;
        final JComponent removePanel;
        final String typeName =
            ConfigType.NUMERICAL.equals(setting.getType()) ? "Numeric Columns" : "Non-numeric Columns";
        if (setting.isMetaConfig()) {
            border = BorderFactory.createTitledBorder(typeName);
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

            JButton requestRemoveButton = new JButton("Remove");
            requestRemoveButton.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    firePropertyChange(REMOVE_ACTION, null, null);
                }
            });

            removePanel = new JPanel();
            removePanel.setLayout(new BorderLayout());
            removePanel.add(requestRemoveButton, BorderLayout.NORTH);

            final List<DataColumnSpec> notExistingColumns = getNotExistingColumns(spec);
            final List<DataColumnSpec> incompatibleColumns = getIncompatibleTypedColumns(setting.getType(), spec);

            if (!notExistingColumns.isEmpty()) {
                warningMessages.add("Some columns no longer exist (red bordered)");
            }

            if (!incompatibleColumns.isEmpty()) {
                warningMessages.add(String.format("Some columns have an incompatible type to %s (yellow borderd)",
                    ConfigType.typeToString(setting.getType())));
            }

            final Set<DataColumnSpec> invalidColumns = new HashSet<DataColumnSpec>();
            invalidColumns.addAll(notExistingColumns);
            invalidColumns.addAll(incompatibleColumns);

            if (!invalidColumns.isEmpty()
            // if all columns are invalid a clean is the same as a remove
                && !(invalidColumns.size() == spec.length)) {

                JButton removeNotExistingColumns = new JButton("Clean");
                removeNotExistingColumns.setToolTipText("Removes all invalid columns from the configuration.");

                removeNotExistingColumns.addActionListener(new ActionListener() {
                    /** {@inheritDoc} */
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        TimeMissingValueHandlingPanel.this.removeAll();

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

        createWestLayout(setting, tabPanel, typeName, spec);

        comboRemovePanel.add(m_comboBox, BorderLayout.CENTER);
        comboRemovePanel.add(removePanel, BorderLayout.EAST);

        fillComboBox(setting);

        setting.setType(settingTypeBackup);
        m_setting = setting;

        setBorder(border != null ? BorderFactory.createTitledBorder(border, typeName) : null);

        tabPanel.add(comboRemovePanel, BorderLayout.NORTH);
        tabPanel.add(createSpacer(65), BorderLayout.SOUTH);
        add(tabPanel);
    }

    private void fillComboBox(final TimeMissingValueHandlingColSetting setting) {
        m_comboBox.removeAllItems();
        m_comboBox.addItem(HandlingMethod.DO_NOTHING);
        m_comboBox.addItem(HandlingMethod.NEXT);
        m_comboBox.addItem(HandlingMethod.LAST);
        switch (setting.getType()) {
            case NUMERICAL:
                m_comboBox.addItem(HandlingMethod.LINEAR);
                m_comboBox.addItem(HandlingMethod.AVERAGE);
                break;
            default:
                break;
        }
        m_comboBox.setSelectedItem(setting.getMethod());
    }

    private void createWestLayout(final TimeMissingValueHandlingColSetting setting, final JPanel tabPanel,
        final String typeName, final DataColumnSpec... spec) {
        if (setting.isMetaConfig()) {
            JPanel typePanel = new JPanel(new BorderLayout());
            typePanel.add(m_comboBox, BorderLayout.NORTH);
            tabPanel.add(typePanel, BorderLayout.WEST);
        } else {
            final JList<?> jList = new JList<Object>(spec);
            jList.setCellRenderer(new DataColumnSpecListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") final JList list,
                    final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                    // The super method will reset the icon if we call this method
                    // last. So we let super do its job first and then we take care
                    // that everything is properly set.
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                    String text = null;
                    if (value instanceof DataColumnSpec
                        && TimeMissingValueHandlingNodeDialogPane.isIncompatible((DataColumnSpec)value)) {
                        setBorder(BorderFactory.createLineBorder(Color.YELLOW));
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
            JScrollPane columns = new JScrollPane(jList);
            columns.setPreferredSize(new Dimension(250, 50));
            tabPanel.add(columns, BorderLayout.CENTER);
        }
    }

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

    private static List<DataColumnSpec>
        getIncompatibleTypedColumns(final ConfigType type, final DataColumnSpec[] specs) {
        List<DataColumnSpec> toReturn = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < specs.length; i++) {
            DataColumnSpec spec = specs[i];
            if (TimeMissingValueHandlingNodeDialogPane.isIncompatible(type, spec)) {
                toReturn.add(spec);
            }
        }
        return toReturn;
    }

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
    public TimeMissingValueHandlingColSetting getSettings() {
        HandlingMethod method;
        method = (HandlingMethod)m_comboBox.getSelectedItem();
        checkArgumentNotNull(method, "Sanity check failed: no method selected");
        m_setting.setMethod(method);
        return m_setting;
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

    private static TimeMissingValueHandlingColSetting diff(final TimeMissingValueHandlingColSetting setting,
        final Set<DataColumnSpec> invalidColumns) {
        List<String> toReturn = new ArrayList<String>(Arrays.asList(setting.getNames()));
        for (DataColumnSpec currSpec : invalidColumns) {
            toReturn.remove(currSpec.getName());
        }
        setting.setNames(toReturn.toArray(new String[toReturn.size()]));
        return setting;
    }
}
