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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.constantvalue;

import static org.knime.core.node.util.CheckUtils.checkSetting;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataTypeListCellRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Dialog for the Constant Value Column.
 *
 * @author Marcel Hanser
 */
final class ConstantValueColumnNodeDialogPane extends NodeDialogPane {

    private static final int DEFAULT_TEXT_SIZE = 20;

    private static final int HORIZONTAL_VERTICAL_GAB = 5;

    private static final String FORMAT_HISTORY_KEY = "constant-value-key";

    private final ColumnSelectionPanel m_columnPanel;

    private final JComboBox<DataType> m_fieldType;

    private final DefaultComboBoxModel<String> m_dateTemplates;

    private final JTextField m_columnName;

    private final JTextField m_value;

    private DataTableSpec m_dataTableSpec;

    /** Create new dialog. */
    @SuppressWarnings("unchecked")
    ConstantValueColumnNodeDialogPane() {
        m_columnPanel = new ColumnSelectionPanel((Border)null, DataValue.class);
        m_columnPanel.setRequired(false);

        ButtonGroup bg = new ButtonGroup();
        final JRadioButton replaceColumnRadio = new JRadioButton("Replace");
        final JRadioButton appendColumnRadio = new JRadioButton("Append");
        bg.add(replaceColumnRadio);
        bg.add(appendColumnRadio);

        m_columnName = new JTextField(DEFAULT_TEXT_SIZE);
        m_value = new JTextField(DEFAULT_TEXT_SIZE);

        m_fieldType = new JComboBox<DataType>();
        m_fieldType.setRenderer(new DataTypeListCellRenderer());

        for (TypeCellFactory factory : TypeCellFactory.values()) {
            m_fieldType.addItem(factory.getDataType());
        }
        m_fieldType.setSelectedIndex(0);

        connectRadioAndComponent(replaceColumnRadio, m_columnPanel);
        connectRadioAndComponent(appendColumnRadio, m_columnName);

        replaceColumnRadio.setSelected(true);
        replaceColumnRadio.doClick();

        JPanel columnConfig = new JPanel(new BorderLayout());
        columnConfig.setBorder(BorderFactory.createTitledBorder("Column settings"));

        JPanel nextPanel = verticalFlow(replaceColumnRadio, appendColumnRadio);
        JPanel nochPanel = verticalFlow(m_columnPanel, m_columnName);

        columnConfig.add(nextPanel, BorderLayout.WEST);
        columnConfig.add(nochPanel, BorderLayout.CENTER);

        final JPanel northValuePanel = new JPanel(new BorderLayout(5, 5));

        final FlowVariableModel createFlowVariableModel =
            createFlowVariableModel(ConstantValueColumnConfig.VALUE, FlowVariable.Type.STRING);

        //small hack to be called after the flow-variable button-dialog has been opened and something has
        //actually changed!
        @SuppressWarnings("serial")
        final FlowVariableModelButton flowVariableModelButton = new FlowVariableModelButton(createFlowVariableModel) {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(final ActionEvent e) {
                final AtomicBoolean changed = new AtomicBoolean(false);
                ChangeListener cl = new ChangeListener() {

                    @Override
                    public void stateChanged(final ChangeEvent ce) {
                        changed.set(true);
                    }
                };
                createFlowVariableModel.addChangeListener(cl);
                super.actionPerformed(e);
                createFlowVariableModel.removeChangeListener(cl);
                if (changed.get() && createFlowVariableModel.isVariableReplacementEnabled()) {
                    Map<String, FlowVariable> availableFlowVariables = getAvailableFlowVariables();
                    FlowVariable flowVariable =
                        availableFlowVariables.get(createFlowVariableModel.getInputVariableName());
                    if (flowVariable != null) {
                        switch (flowVariable.getType()) {
                            case DOUBLE:
                                m_fieldType.setSelectedItem(TypeCellFactory.DOUBLE.getDataType());
                                break;
                            case INTEGER:
                                m_fieldType.setSelectedItem(TypeCellFactory.INT.getDataType());
                                break;
                            case STRING:
                                m_fieldType.setSelectedItem(TypeCellFactory.STRING.getDataType());
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        };
        createFlowVariableModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent evt) {
                FlowVariableModel wvm = (FlowVariableModel)evt.getSource();
                m_value.setEnabled(!wvm.isVariableReplacementEnabled());
            }
        });

        northValuePanel.add(m_fieldType, BorderLayout.WEST);
        northValuePanel.add(m_value, BorderLayout.CENTER);
        northValuePanel.add(flowVariableModelButton, BorderLayout.EAST);

        m_dateTemplates = new DefaultComboBoxModel<String>();
        final JComboBox<String> box = new JComboBox<String>(m_dateTemplates);
        box.setEditable(true);
        final JPanel timePatternPanel = ViewUtils.getInFlowLayout(new JLabel("Date format"), box);

        m_fieldType.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    northValuePanel.removeAll();
                    switch (TypeCellFactory.forDataType((DataType)e.getItem())) {
                        case DATE:
                            northValuePanel.add(timePatternPanel, BorderLayout.SOUTH);
                        default:
                            northValuePanel.add(m_fieldType, BorderLayout.WEST);
                            northValuePanel.add(m_value, BorderLayout.CENTER);
                            northValuePanel.add(flowVariableModelButton, BorderLayout.EAST);
                            break;
                    }
                    getPanel().revalidate();
                    getPanel().repaint();
                }
            }
        });

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(BorderFactory.createTitledBorder("Value settings"));
        southPanel.add(northValuePanel, BorderLayout.NORTH);
        southPanel.add(new JLabel("  "), BorderLayout.SOUTH);

        JPanel tabPanel = new JPanel(new BorderLayout());

        tabPanel.add(columnConfig, BorderLayout.NORTH);
        tabPanel.add(southPanel, BorderLayout.CENTER);

        addTab("Settings", tabPanel);
    }

    /**
     * @return
     */
    private static JPanel verticalFlow(final JComponent... components) {
        JPanel nochPanel = new JPanel(new BorderLayout());
        nochPanel.setLayout(new GridLayout(components.length, 0, HORIZONTAL_VERTICAL_GAB, HORIZONTAL_VERTICAL_GAB));
        for (JComponent component : components) {
            nochPanel.add(component);
        }
        return nochPanel;
    }

    /**
     * @param replaceColumnRadio the radio component to connect
     * @param panel the panel component to connect
     */
    private static void connectRadioAndComponent(final JRadioButton replaceColumnRadio, final Component panel) {
        replaceColumnRadio.addItemListener(e -> panel.setEnabled(replaceColumnRadio.isSelected()));

        if (panel instanceof ColumnSelectionPanel) {
            ((ColumnSelectionPanel) panel).addUnderlyingPropertyChangeListener("enabled",
                evt -> replaceColumnRadio.setSelected(panel.isEnabled()));
        } else {
            panel.addPropertyChangeListener("enabled", evt -> replaceColumnRadio.setSelected(panel.isEnabled()));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        ConstantValueColumnConfig config = new ConstantValueColumnConfig();
        config.loadInDialog(settings, specs[0]);
        m_dataTableSpec = specs[0];

        m_columnPanel.setSelectedColumn(config.getReplacedColumn());
        m_columnPanel.update(specs[0], config.getReplacedColumn(), false, true);

        m_value.setText(StringUtils.defaultString(config.getValue(), ""));

        m_fieldType.setSelectedItem(config.getCellFactory().getDataType());

        setText(m_columnName, config.getNewColumnName());
        if (config.getNewColumnName() == null) {
            m_columnPanel.setEnabled(true);
        }

        m_dateTemplates.removeAllElements();
        Collection<String> createPredefinedFormats = createPredefinedFormats();
        if (config.getDateFormat() != null) {
            createPredefinedFormats.add(config.getDateFormat());
        }
        for (String string : createPredefinedFormats) {
            m_dateTemplates.addElement(string);
        }
        if (config.getDateFormat() != null) {
            m_dateTemplates.setSelectedItem(config.getDateFormat());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ConstantValueColumnConfig config = new ConstantValueColumnConfig();
        TypeCellFactory forDataType = TypeCellFactory.forDataType((DataType)m_fieldType.getSelectedItem());
        String pattern = (String)m_dateTemplates.getSelectedItem();
        config.setValue(m_value.getText());

        //Bug-5331 - check if the value is controlled by a flow-variable
        if (m_columnName.isEnabled()) {
            try {
                forDataType.createCell(m_value.getText() == null ? "" : m_value.getText(), pattern);
                config.setValue(m_value.getText());
            } catch (TypeParsingException e) {
                throw new InvalidSettingsException(String.format("Error on creating '%s' from input string: '%s'",
                    forDataType.getDataType(), m_value.getText()));
            }
        }
        if (StringUtils.isNotEmpty(pattern)) {
            StringHistory.getInstance(FORMAT_HISTORY_KEY).add(pattern);
        }
        config.setDateFormat(pattern);
        config.setCellFactory(forDataType);
        config.setNewColumnName(m_columnName.isEnabled() ? getText(m_columnName, "New column name must not be empty.")
            : null);
        checkSetting(!m_columnPanel.isEnabled() || m_dataTableSpec.containsName(m_columnPanel.getSelectedColumn()),
            "Selected column must exist in input table.");

        config.setReplacedColumn(m_columnPanel.isEnabled() ? m_columnPanel.getSelectedColumn() : null);
        config.save(settings);
    }

    @Override
    public void onClose() {
        m_dataTableSpec = null;
    }

    private static void setText(final JTextField appendColumnField, final String newColumnName) {
        appendColumnField.setEnabled(false);
        if (newColumnName != null) {
            appendColumnField.setText(newColumnName);
            appendColumnField.setEnabled(true);
        }
    }

    private static String getText(final JTextField field, final String messageIfNotExist)
        throws InvalidSettingsException {
        String text = StringUtils.defaultIfBlank(field.getText(), "");
        checkSetting(StringUtils.isNotEmpty(text), messageIfNotExist);
        return text;
    }

    private static Collection<String> createPredefinedFormats() {
        // unique values
        Set<String> formats = new LinkedHashSet<String>();
        formats.add("yyyy-MM-dd;HH:mm:ss.S");
        formats.add("dd.MM.yyyy;HH:mm:ss.S");
        formats.add("yyyy/dd/MM");
        formats.add("dd/MM/yyyy");
        formats.add("dd.MM.yyyy");
        formats.add("yyyy-MM-dd");
        formats.add("HH:mm:ss");
        // check also the StringHistory....
        String[] userFormats = StringHistory.getInstance(FORMAT_HISTORY_KEY).getHistory();
        for (String userFormat : userFormats) {
            formats.add(userFormat);
        }
        return formats;
    }
}
