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
package org.knime.ext.sun.nodes.script.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.compile.CompilationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 * Panel of the Java Snippet node.
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class JavaScriptingPanel extends JPanel {

    private final JList m_colList;

    private final JList m_flowVarsList;

    private final JEditorPane m_expEdit;

    private final JEditorPane m_headerEdit;

    private final JRadioButton m_appendRadio;

    private final JTextField m_newNameField;

    private final JRadioButton m_replaceRadio;

    private final ColumnSelectionPanel m_replaceColumnCombo;
    private final JComboBox m_replaceVariableCombo;

    private final ButtonGroup m_returnTypeButtonGroup;

    private final JCheckBox m_isArrayReturnChecker;

    private final JCheckBox m_compileOnCloseChecker;

    private final JCheckBox m_insertMissingAsNullChecker;

    private DataTableSpec m_currentSpec = null;

    private int m_currentVersion;

    private final DialogFlowVariableProvider m_varProvider;

    private final JavaScriptingCustomizer m_customizer;

    /** Inits GUI.
     * @param customizer Sets static config options.
     * @param varProvider Provider of flow vars (dialog). */
    @SuppressWarnings("unchecked")
    public JavaScriptingPanel(final JavaScriptingCustomizer customizer,
            final DialogFlowVariableProvider varProvider) {
        super(new BorderLayout());
        m_customizer = customizer;
        m_varProvider = varProvider;
        m_colList = new JList(new DefaultListModel());
        m_colList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_colList.addKeyListener(new KeyAdapter() {
            /** {@inheritDoc} */
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    Object selected = m_colList.getSelectedValue();
                    if (selected != null) {
                        onSelectionInColumnList(selected);
                    }
                }
            }
        });
        m_colList.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object selected = m_colList.getSelectedValue();
                    if (selected != null) {
                        onSelectionInColumnList(selected);
                    }
                }
            }
        });
        m_colList.setCellRenderer(new ListRenderer());
        m_flowVarsList = new JList(new DefaultListModel());
        m_flowVarsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_flowVarsList.setToolTipText(""); // enable tooltip
        m_flowVarsList.addKeyListener(new KeyAdapter() {
            /** {@inheritDoc} */
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    Object selected = m_flowVarsList.getSelectedValue();
                    if (selected != null) {
                        onSelectionInVariableList(selected);
                    }
                }
            }
        });
        m_flowVarsList.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object selected = m_flowVarsList.getSelectedValue();
                    if (selected != null) {
                        onSelectionInVariableList(selected);
                    }
                }
            }
        });
        m_flowVarsList.setCellRenderer(new FlowVariableListCellRenderer());
        m_expEdit = new JEditorPane();
        Font font = m_expEdit.getFont();
        Font newFont = new Font(Font.MONOSPACED, Font.PLAIN,
                (font == null ? 12 : font.getSize()));
        m_expEdit.setFont(newFont);

        m_headerEdit = new JEditorPane();
        m_headerEdit.setFont(newFont);

        m_newNameField = new JTextField(10);
        String radioButtonName;
        String radioButtonToolTip;
        if (m_customizer.getOutputIsVariable()) {
            radioButtonName = "Define Variable: ";
            radioButtonToolTip = "Defines new variable with the given name.";
        } else { // output is variable
            radioButtonName = "Append Column: ";
            radioButtonToolTip = "Appends a new column to the input "
                + "table with a given name and type.";
        }
        m_appendRadio = new JRadioButton(radioButtonName);
        m_appendRadio.setToolTipText(radioButtonToolTip);

        if (m_customizer.getOutputIsVariable()) {
            radioButtonName = "Overwrite Variable: ";
            radioButtonToolTip = "Overwrites value of the selected variable "
                + "(must have same type as the calculated variable).";
        } else { // output is variable
            radioButtonName = "Replace Column: ";
            radioButtonToolTip = "Replaces the column and changes "
                + "the column type accordingly";
        }
        m_replaceRadio = new JRadioButton(radioButtonName);
        m_replaceRadio.setToolTipText(radioButtonToolTip);
        // show all columns
        m_replaceColumnCombo =
            new ColumnSelectionPanel((Border)null, DataValue.class);
        m_replaceColumnCombo.setRequired(false);
        m_replaceVariableCombo = new JComboBox(new DefaultComboBoxModel());
        m_replaceVariableCombo.setRenderer(new FlowVariableListCellRenderer());

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_appendRadio);
        buttonGroup.add(m_replaceRadio);
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_replaceColumnCombo.setEnabled(m_replaceRadio.isSelected());
                m_replaceVariableCombo.setEnabled(m_replaceRadio.isSelected());
                m_newNameField.setEnabled(m_appendRadio.isSelected());
            }
        };
        m_appendRadio.addActionListener(actionListener);
        m_replaceRadio.addActionListener(actionListener);

        m_compileOnCloseChecker = new JCheckBox("Compile on close");
        m_compileOnCloseChecker.setToolTipText("Compiles the code on close "
                + "to identify potential syntax problems.");

        m_insertMissingAsNullChecker = new JCheckBox("Insert Missing As Null");
        m_insertMissingAsNullChecker.setToolTipText("If unselected, missing "
                + "values in the input will produce a missing cell result");

        m_returnTypeButtonGroup = new ButtonGroup();
        for (JavaSnippetType<?, ?, ?> type : m_customizer.getReturnTypes()) {
            Class<?> cl = type.getJavaClass(false);
            JRadioButton radio = new JRadioButton(cl.getSimpleName());
            radio.setActionCommand(cl.getName());
            m_returnTypeButtonGroup.add(radio);
        }
        m_isArrayReturnChecker = new JCheckBox("Array Return");
        createPanel();
    }

    private void createPanel() {
        final JSplitPane varSplitPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        if (m_customizer.getShowColumnList()) {
            JScrollPane pane = new JScrollPane(m_colList);
            pane.setBorder(createEmptyTitledBorder("Column List"));
            varSplitPane.setTopComponent(pane);
        }
        // set variable panel
        JScrollPane pane = new JScrollPane(m_flowVarsList);
        pane.setBorder(createEmptyTitledBorder("Flow Variable List"));
        varSplitPane.setBottomComponent(pane);
        varSplitPane.setOneTouchExpandable(true);
        varSplitPane.setResizeWeight(0.9);
        JComponent editorMainPanel = createEditorPanel();

        JPanel centerPanel = new JPanel(new GridLayout(0, 1));
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(varSplitPane);
        mainSplitPane.setRightComponent(editorMainPanel);
        centerPanel.add(mainSplitPane);

        JPanel southPanel = new JPanel(new GridLayout(0, 2));
        JPanel replaceOrAppend = createAndOrReplacePanel();
        if (m_customizer.isShowOutputPanel()) {
            southPanel.add(replaceOrAppend);
        }

        JPanel returnTypeAndCompilation = createReturnTypeAndCompilationPanel();
        southPanel.add(returnTypeAndCompilation);
        add(centerPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void onSelectionInColumnList(final Object selected) {
        if (selected != null) {
            String enter;
            if (selected instanceof String) {
                enter = "$$" + selected + "$$";
            } else {
                DataColumnSpec colSpec = (DataColumnSpec)selected;
                String name = colSpec.getName().replace("$", "\\$");
                enter = "$" + name + "$";
            }
            m_expEdit.replaceSelection(enter);
            m_colList.clearSelection();
            m_expEdit.requestFocus();
        }
    }

    private void onSelectionInVariableList(final Object selected) {
        if (selected instanceof FlowVariable) {
            FlowVariable v = (FlowVariable)selected;
            String typeChar;
            switch (v.getType()) {
            case DOUBLE:
                typeChar = "D";
                break;
            case INTEGER:
                typeChar = "I";
                break;
            case STRING:
                typeChar = "S";
                break;
            default:
                return;
            }
            String enter = "$${" + typeChar + v.getName() + "}$$";
            m_expEdit.replaceSelection(enter);
            m_flowVarsList.clearSelection();
            m_expEdit.requestFocus();
        }
    }

    private JComponent createEditorPanel() {
        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setOneTouchExpandable(true);

        if (m_customizer.getShowGlobalDeclarationList()) {
            JScrollPane headerScroller = new JScrollPane(m_headerEdit);
            String borderTitle = "Global Variable Declaration";
            headerScroller.setBorder(createEmptyTitledBorder(borderTitle));
            split.setTopComponent(headerScroller);
        }
        JScrollPane bodyScroller = new JScrollPane(m_expEdit);
        String borderTitle = "Method Body";
        bodyScroller.setBorder(createEmptyTitledBorder(borderTitle));

        split.setBottomComponent(bodyScroller);
        split.setResizeWeight(0.2);
        return split;
    }

    private JPanel createAndOrReplacePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        String title;
        if (m_customizer.getOutputIsVariable()) {
            title = "Overwrite or define new variable";
        } else {
            title = "Replace or append result";
        }
        panel.setBorder(BorderFactory.createTitledBorder(title));
        m_appendRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_newNameField.requestFocus();
                m_newNameField.selectAll();
            }
        });

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_appendRadio, gbc);

        gbc.gridx += 1;
        panel.add(m_newNameField, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        panel.add(m_replaceRadio, gbc);

        gbc.gridx += 1;
        if (m_customizer.getOutputIsVariable()) {
            panel.add(m_replaceVariableCombo, gbc);
        } else {
            panel.add(m_replaceColumnCombo, gbc);
        }
        return panel;
    }

    private JPanel createReturnTypeAndCompilationPanel() {
        JPanel returnTypeAndCompilation = new JPanel(new BorderLayout());
        JPanel miscPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (m_customizer.getShowInsertMissingAsNull()) {
            miscPanel.add(m_insertMissingAsNullChecker);
        }
        miscPanel.add(m_compileOnCloseChecker);
        returnTypeAndCompilation.add(miscPanel, BorderLayout.NORTH);

        JPanel returnType = new JPanel(new BorderLayout());
        returnType.setBorder(BorderFactory.createTitledBorder("Return type"));
        JPanel returnFieldType = new JPanel(new GridLayout(0, 2));
        for (Enumeration<?> e = m_returnTypeButtonGroup.getElements(); e
                .hasMoreElements();) {
            returnFieldType.add((AbstractButton)e.nextElement());
        }
        returnType.add(returnFieldType, BorderLayout.CENTER);
        if (m_customizer.getShowArrayReturn()) {
            JPanel arrayPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            arrayPanel.add(m_isArrayReturnChecker);
            returnType.add(arrayPanel, BorderLayout.SOUTH);
        }
        if (m_customizer.getShowOutputTypePanel()) {
            returnTypeAndCompilation.add(returnType, BorderLayout.CENTER);
        }

        return returnTypeAndCompilation;

    }

    /** Load settings from arg.
     * @param s To load from.
     * @param spec The input spec.
     */
    public void loadSettingsFrom(final JavaScriptingSettings s,
            final DataTableSpec spec) {
        String exp = s.getExpression();
        String header = s.getHeader();

        String rType = s.getReturnType().getName();
        boolean isArrayReturn = s.isArrayReturn();
        String defaultNewName = m_customizer.getOutputIsVariable()
            ? "new variable" : "new column";
        String newName = s.getColName();
        boolean isReplace = s.isReplace();
        boolean isTestCompilation = s.isTestCompilationOnDialogClose();
        boolean isInsertMissingAsNull = s.isInsertMissingAsNull();
        m_currentVersion = s.getExpressionVersion();
        m_newNameField.setText("");
        // will select newColName only if it is in the spec list
        try {
            m_replaceColumnCombo.update(spec, newName);
        } catch (NotConfigurableException e1) {
            NodeLogger.getLogger(getClass()).coding("Combo box throws "
                    + "exception although content is not required", e1);
        }
        DefaultComboBoxModel cmbModel =
            (DefaultComboBoxModel)m_replaceVariableCombo.getModel();
        cmbModel.removeAllElements();
        final Map<String, FlowVariable> availableFlowVariables =
            m_varProvider.getAvailableFlowVariables();
        for (FlowVariable v : availableFlowVariables.values()) {
            switch (v.getScope()) {
            case Flow:
                cmbModel.addElement(v);
                break;
            default:
                // ignore
            }
        }
        if (isReplace && availableFlowVariables.containsKey(newName)) {
            m_replaceVariableCombo.setSelectedItem(
                    availableFlowVariables.get(newName));
        }

        m_currentSpec = spec;
        // whether there are variables or columns available
        // -- which of two depends on the customizer
        boolean fieldsAvailable;
        if (m_customizer.getOutputIsVariable()) {
            fieldsAvailable = m_replaceVariableCombo.getModel().getSize() > 0;
        } else {
            fieldsAvailable = m_replaceColumnCombo.getNrItemsInList() > 0;
        }
        if (isReplace && fieldsAvailable) {
            m_replaceRadio.doClick();
        } else {
            m_appendRadio.doClick();
            String newNameString = (newName != null ? newName
                    : defaultNewName);
            m_newNameField.setText(newNameString);
        }
        m_replaceRadio.setEnabled(fieldsAvailable);
        m_headerEdit.setText(header);
        m_expEdit.setText(exp);
        m_expEdit.requestFocus();
        ButtonModel firstButton = null;
        for (Enumeration<?> e = m_returnTypeButtonGroup.getElements(); e
                .hasMoreElements();) {
            AbstractButton b = (AbstractButton)e.nextElement();
            if (firstButton == null) {
                firstButton = b.getModel();
            }
            if (b.getActionCommand().equals(rType)) {
                m_returnTypeButtonGroup.setSelected(b.getModel(), true);
            }
        }
        if (m_returnTypeButtonGroup.getSelection() == null) {
            m_returnTypeButtonGroup.setSelected(firstButton, true);
        }
        m_isArrayReturnChecker.setSelected(isArrayReturn);
        DefaultListModel listModel = (DefaultListModel)m_colList.getModel();
        listModel.removeAllElements();
        if (m_currentVersion == Expression.VERSION_1X) {
            listModel.addElement(Expression.ROWKEY);
            listModel.addElement(Expression.ROWNUMBER);
        } else {
            listModel.addElement(Expression.ROWID);
            listModel.addElement(Expression.ROWINDEX);
            listModel.addElement(Expression.ROWCOUNT);
        }
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            listModel.addElement(colSpec);
        }
        DefaultListModel fvListModel =
            (DefaultListModel)m_flowVarsList.getModel();
        fvListModel.removeAllElements();
        for (FlowVariable v : availableFlowVariables.values()) {
            fvListModel.addElement(v);
        }
        m_compileOnCloseChecker.setSelected(isTestCompilation);
        m_insertMissingAsNullChecker.setSelected(isInsertMissingAsNull);
    }

    /** Save current settings.
     * @param s To save to.
     * @throws InvalidSettingsException If compilation fails.
     */
    public void saveSettingsTo(final JavaScriptingSettings s)
            throws InvalidSettingsException {
        String newColName = null;
        boolean isReplace = m_replaceRadio.isSelected();
        if (isReplace) {
            if (m_customizer.getOutputIsVariable()) {
                FlowVariable item =
                    (FlowVariable)m_replaceVariableCombo.getSelectedItem();
                newColName = item.getName();
            } else {
                newColName = m_replaceColumnCombo.getSelectedColumn();
            }
        } else {
            newColName = m_newNameField.getText();
        }
        s.setReplace(isReplace);
        s.setColName(newColName);
        String type = m_returnTypeButtonGroup.getSelection().getActionCommand();
        s.setReturnType(type);
        s.setArrayReturn(m_isArrayReturnChecker.isSelected());
        String exp = m_expEdit.getText();
        s.setExpression(exp);
        s.setHeader(m_headerEdit.getText());
        s.setExpressionVersion(m_currentVersion);
        boolean isTestCompilation = m_compileOnCloseChecker.isSelected();
        s.setTestCompilationOnDialogClose(isTestCompilation);
        if (isTestCompilation && m_currentSpec != null) {
            try (Expression tempExp = Expression.compile(s, m_currentSpec)) {
            } catch (CompilationFailedException cfe) {
                throw new InvalidSettingsException(cfe.getMessage(), cfe);
            } catch (IOException e) {
                NodeLogger.getLogger(getClass()).error("Unable to clean-up Expression instance: " + e.getMessage(), e);
            }
        }
        s.setInsertMissingAsNull(m_insertMissingAsNullChecker.isSelected());
    }

    /** Create an empty, titled border.
     * @param string Title of the border.
     * @return Such a new border.
     */
    private static final Border createEmptyTitledBorder(final String string) {
        return BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(
                5, 0, 0, 0), string, TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.BELOW_TOP);
    }

    /** Interface implemented by dialog.
     * It provides the current set of flow vars. */
    public static interface DialogFlowVariableProvider {
        /** @return current map of variables. */
        public Map<String, FlowVariable> getAvailableFlowVariables();
    }

    /**
     * Renderer that will display the rowindex and rowkey with different
     * background.
     */
    private static class ListRenderer extends DataColumnSpecListCellRenderer {
        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            if (value instanceof String) {
                c.setFont(list.getFont().deriveFont(Font.ITALIC));
            }
            return c;
        }
    }
}
