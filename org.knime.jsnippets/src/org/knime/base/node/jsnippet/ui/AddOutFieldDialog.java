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
 * History
 *   26.03.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.ui.FieldsTableModel.Column;
import org.knime.base.node.jsnippet.util.field.JavaField;
import org.knime.base.node.jsnippet.util.field.JavaField.FieldType;
import org.knime.base.node.jsnippet.util.field.OutCol;
import org.knime.base.node.jsnippet.util.field.OutVar;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.DataTypeListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A dialog with most basic settings for an output field.
 * <p>
 * This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("serial")
public final class AddOutFieldDialog extends JDialog {
    private JavaField m_result;

    private final OutFieldsTableModel m_model;

    private final DataTableSpec m_spec;

    private final Map<String, FlowVariable> m_flowVars;

    private final boolean m_flowVarsOnly;

    private final JComboBox m_fieldType;

    private final JCheckBox m_isArray;

    private final JRadioButton m_replace;

    private final JRadioButton m_append;

    private final JComboBox m_replacedKnimeName;

    private final JTextField m_knimeName;

    private final JComboBox m_knimeType;

    /**
     * Create a new dialog.
     *
     * @param parent frame who owns this dialog
     * @param model the model used for validation
     * @param spec the DataTableSpec of the input table
     * @param flowVars the flow variables on the input
     * @param flowVarsOnly true when only flow variables can be defined
     */
    private AddOutFieldDialog(final Frame parent, final OutFieldsTableModel model, final DataTableSpec spec,
        final Map<String, FlowVariable> flowVars, final boolean flowVarsOnly) {
        super(parent, true);

        if (KNIMEConstants.KNIME16X16 != null) {
            setIconImage(KNIMEConstants.KNIME16X16.getImage());
        }

        m_model = model;
        m_spec = spec;
        m_flowVars = flowVars;
        m_flowVarsOnly = flowVarsOnly;

        setTitle("Add output field");

        // initialize fields
        m_replace = new JRadioButton("Replace:");
        m_replace.setSelected(true);
        m_append = new JRadioButton("Append:");
        m_replacedKnimeName = new JComboBox();

        m_fieldType = new JComboBox();
        m_knimeName = new JTextField();
        m_knimeType = new JComboBox();
        m_isArray = new JCheckBox("Is array");

        // instantiate the components of the dialog
        JPanel p = createPanel();

        // the OK and Cancel button
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        // add action listener
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onOK();
            }
        });
        JButton cancel = new JButton("Cancel");
        // add action listener
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                onCancel();
            }
        });
        control.add(ok);
        control.add(cancel);

        // add dialog and control panel to the content pane
        Container cont = getContentPane();
        cont.setLayout(new BorderLayout());
        cont.add(p, BorderLayout.CENTER);
        cont.add(control, BorderLayout.SOUTH);

        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        Insets leftInsets = new Insets(3, 8, 3, 8);
        Insets rightInsets = new Insets(3, 0, 3, 8);
        Insets leftCategoryInsets = new Insets(11, 8, 3, 8);
        Insets rightCategoryInsets = new Insets(11, 0, 3, 8);

        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 1;
        c.weightx = 0;

        m_fieldType.addItem(FieldType.Column);
        m_fieldType.addItem(FieldType.FlowVariable);
        if (!m_flowVarsOnly) {
            p.add(new JLabel("Field type:"), c);

            c.gridx++;
            c.insets = rightCategoryInsets;
            m_fieldType.setSelectedIndex(0);
            m_fieldType.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    initKnimeTypeComboBox();
                    initKnimeNameComboBox();
                    if (m_fieldType.getSelectedItem().equals(FieldType.FlowVariable)) {
                        m_isArray.setSelected(false);
                        m_isArray.setEnabled(false);
                    } else {
                        m_isArray.setEnabled(true);
                    }
                }
            });
            p.add(m_fieldType, c);

            c.gridy++;
            c.gridx = 0;
            c.insets = leftInsets;

        } else {
            m_fieldType.setSelectedIndex(1);
        }

        m_replace.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_replacedKnimeName.setEnabled(m_replace.isSelected());
                m_knimeName.setEnabled(!m_replace.isSelected());
            }
        });
        p.add(m_replace, c);

        c.gridx++;
        c.insets = rightInsets;

        initKnimeNameComboBox();
        p.add(m_replacedKnimeName, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;

        m_append.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_replacedKnimeName.setEnabled(!m_append.isSelected());
                m_knimeName.setEnabled(m_append.isSelected());
                if (m_append.isSelected()) {
                    m_knimeName.requestFocus();
                }
            }
        });
        p.add(m_append, c);

        c.gridx++;
        c.insets = rightInsets;
        c.weightx = 1.0;
        p.add(m_knimeName, c);
        c.weightx = 0.0;

        ButtonGroup group = new ButtonGroup();
        group.add(m_replace);
        group.add(m_append);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        p.add(new JLabel("Output type:"), c);

        c.gridx++;
        c.insets = rightInsets;

        initKnimeTypeComboBox();
        p.add(m_knimeType, c);

        if (!m_flowVarsOnly) {
            c.gridy++;
            c.gridx = 1;
            c.insets = rightInsets;
            p.add(m_isArray, c);
        }

        m_replacedKnimeName.setEnabled(m_replace.isSelected());
        m_knimeName.setEnabled(!m_replace.isSelected());

        return p;
    }

    /** Initialize the selection list for the knime name. */
    private void initKnimeNameComboBox() {
        m_replacedKnimeName.removeAllItems();
        if (m_fieldType.getSelectedItem().equals(FieldType.Column)) {
            for (DataColumnSpec colSpec : m_spec) {
                m_replacedKnimeName.addItem(colSpec);
            }
            m_replacedKnimeName.setRenderer(new DataColumnSpecListCellRenderer());
        } else {
            for (FlowVariable flowVar : m_flowVars.values()) {
                // test if a flow variable of this name might be
                // created.
                if (FieldsTableUtil.verifyNameOfFlowVariable(flowVar.getName())) {
                    m_replacedKnimeName.addItem(flowVar);
                }
            }
            m_replacedKnimeName.setRenderer(new FlowVariableListCellRenderer());
        }
        if (m_replacedKnimeName.getItemCount() <= 0) {
            m_replacedKnimeName.setEnabled(false);
            m_knimeName.setEnabled(true);
            m_replace.setEnabled(false);
            m_replace.setSelected(false);
            m_append.setSelected(true);
        } else {
            m_replacedKnimeName.setEnabled(true);
            m_replace.setEnabled(true);
        }
    }

    /** Initialize the selection list for the knime type. */
    private void initKnimeTypeComboBox() {
        m_knimeType.removeAllItems();
        if (m_fieldType.getSelectedItem().equals(FieldType.Column)) {
            for (final DataType type : ConverterUtil.getAllDestinationDataTypes()) {
                m_knimeType.addItem(type);
            }
            m_knimeType.setRenderer(new DataTypeListCellRenderer());
        } else {
            TypeProvider typeProvider = TypeProvider.getDefault();
            for (FlowVariable.Type type : typeProvider.getTypes()) {
                m_knimeType.addItem(type);
            }
            m_knimeType.setRenderer(new TypeListCellRender());
        }
        // string is the default value
        m_knimeType.setSelectedItem(StringCell.TYPE);
    }

    /** Save settings in field m_result. */
    @SuppressWarnings("rawtypes")
    private JavaField takeOverSettings() {
        if (m_fieldType.getSelectedItem().equals(FieldType.Column)) {
            OutCol outCol = new OutCol();
            boolean isReplacing = m_replace.isSelected();
            outCol.setReplaceExisting(isReplacing);
            String colName =
                isReplacing ? ((DataColumnSpec)m_replacedKnimeName.getSelectedItem()).getName() : m_knimeName.getText();
            outCol.setKnimeName(colName);

            Set<String> taken = new HashSet<>();
            for (int i = 0; i < m_model.getRowCount(); i++) {
                taken.add((String)m_model.getValueAt(i, Column.JAVA_FIELD));
            }

            // collection is now done by a separate checkbox
            //            DataType dataType = (DataType)m_knimeType.getSelectedItem();
            //            DataType elemType =
            //                    dataType.isCollectionType() ? dataType
            //                            .getCollectionElementType() : dataType;
            // boolean isCollection = dataType.isCollectionType();

            DataType dataType = (DataType)m_knimeType.getSelectedItem();
            if (m_isArray.isSelected()) {
                dataType = ListCell.getCollectionType(dataType);
            }

            final Optional<JavaToDataCellConverterFactory<?>> selectedFactory =
                ConverterUtil.getPreferredFactoryForDestinationType(dataType);

            if (!selectedFactory.isPresent()) {
                // Default to string converter, which always exists. Should not happen, though.
                outCol.setConverterFactory(ConverterUtil.getPreferredFactoryForDestinationType(StringCell.TYPE).get());
            } else {
                outCol.setConverterFactory(selectedFactory.get());
            }

            final String javaName = FieldsTableUtil.createUniqueJavaIdentifier(colName, taken, "out_");
            outCol.setJavaName(javaName);
            return outCol;
        } else { // flow variable
            OutVar outVar = new OutVar();
            boolean isReplacing = m_replace.isSelected();
            outVar.setReplaceExisting(isReplacing);
            String colName =
                isReplacing ? ((FlowVariable)m_replacedKnimeName.getSelectedItem()).getName() : m_knimeName.getText();
            outVar.setKnimeName(colName);

            Set<String> taken = new HashSet<>();
            for (int i = 0; i < m_model.getRowCount(); i++) {
                taken.add((String)m_model.getValueAt(i, Column.JAVA_FIELD));
            }
            String javaName = FieldsTableUtil.createUniqueJavaIdentifier(colName, taken, "out_");

            FlowVariable.Type type = (FlowVariable.Type)m_knimeType.getSelectedItem();
            TypeProvider typeProvider = TypeProvider.getDefault();
            Class javaType = typeProvider.getTypeConverter(type).getPreferredJavaType();

            outVar.setFlowVarType(type);
            outVar.setJavaName(javaName);
            outVar.setJavaType(javaType);
            return outVar;
        }
    }

    /**
     * Called when user presses the ok button.
     */
    void onOK() {
        m_result = takeOverSettings();
        if (m_result != null) {
            shutDown();
        }
    }

    /**
     * Called when user presses the cancel button or closes the window.
     */
    void onCancel() {
        m_result = null;
        shutDown();
    }

    /** Blows away the dialog. */
    private void shutDown() {
        setVisible(false);
    }

    /**
     * Opens a Dialog to receive user settings. If the user cancels the dialog <code>null</code> will be returned. If
     * okay is pressed, the settings from the dialog will be stored in a new {@link JavaField} object, which is either
     * of type {@link OutCol} or {@link OutVar}.<br>
     * If user's settings are incorrect an error dialog pops up and the user values are discarded.
     *
     * @param parent frame who owns this dialog
     * @param model the model used for validation
     * @param spec the DataTableSpec of the input table
     * @param flowVars the flow variables on the input
     * @param flowVarsOnly true when only flow variables can be defined
     * @return new settings are null in case of cancellation
     */
    public static JavaField openUserDialog(final Frame parent, final OutFieldsTableModel model,
        final DataTableSpec spec, final Map<String, FlowVariable> flowVars, final boolean flowVarsOnly) {
        AddOutFieldDialog dialog = new AddOutFieldDialog(parent, model, spec, flowVars, flowVarsOnly);
        return dialog.showDialog();
    }

    /**
     * Shows the dialog and waits for it to return. If the user pressed Ok it returns the OutCol definition
     */
    private JavaField showDialog() {
        pack();
        centerDialog();

        setVisible(true);
        /* ---- won't come back before dialog is disposed -------- */
        /* ---- on Ok we transfer the settings into the m_result -- */
        return m_result;
    }

    /**
     * Sets this dialog in the center of the screen observing the current screen size.
     */
    private void centerDialog() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        setBounds(Math.max(0, (screenSize.width - size.width) / 2), Math.max(0, (screenSize.height - size.height) / 2),
            Math.min(screenSize.width, size.width), Math.min(screenSize.height, size.height));
    }

    /** Renders the flow variable type. */
    private static class TypeListCellRender extends FlowVariableListCellRenderer {
        private Map<FlowVariable.Type, FlowVariable> m_flowVars;

        public TypeListCellRender() {
            m_flowVars = new HashMap<>();
            m_flowVars.put(FlowVariable.Type.DOUBLE, new FlowVariable("double", 1.0));
            m_flowVars.put(FlowVariable.Type.INTEGER, new FlowVariable("int", 1));
            m_flowVars.put(FlowVariable.Type.STRING, new FlowVariable("string", "1.0"));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {
            Object v = m_flowVars.get(value);
            // let super class do the work
            super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus);

            return this;
        }
    }
}
