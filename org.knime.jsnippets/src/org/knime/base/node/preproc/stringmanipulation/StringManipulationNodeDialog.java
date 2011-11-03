/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   30.09.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
import java.util.Collection;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.base.node.preproc.stringmanipulation.manipulator.StringManipulator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.compile.CompilationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingPanel.DialogFlowVariableProvider;

/**
 * The node dialog of the string manipulation node.
 *
 * @author Heiko Hofer
 */
public class StringManipulationNodeDialog  extends NodeDialogPane {

    private JList m_colList;
    private JList m_flowVarsList;
    private JTextComponent m_expEdit;
    private JRadioButton m_appendRadio;
    private JTextField m_newNameField;
    private JRadioButton m_replaceRadio;
    private ColumnSelectionPanel m_replaceColumnCombo;
    private JCheckBox m_compileOnCloseChecker;
    private JCheckBox m_insertMissingAsNullChecker;
    private DataTableSpec m_currentSpec = null;
    private DialogFlowVariableProvider m_varProvider;
    private JComboBox m_categories;
    private JList m_manipulators;
    private JTextPane m_description;
    private JavaScriptingCompletionProvider m_completionProvider;


    /**
     * Create new instance.
     */
    public StringManipulationNodeDialog() {
        addTab("String Manipulation", createStringManipulationPanel());
    }

    /**
     * @return the controls for the string manipulation node
     */
    private Component createStringManipulationPanel() {
        m_varProvider = new DialogFlowVariableProvider() {
            @Override
            public Map<String, FlowVariable> getAvailableFlowVariables() {
                return StringManipulationNodeDialog.this.
                        getAvailableFlowVariables();
            }
        };
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

        m_newNameField = new JTextField(10);
        String radioButtonName;
        String radioButtonToolTip;

        radioButtonName = "Append Column: ";
        radioButtonToolTip = "Appends a new column to the input "
            + "table with a given name and type.";
        m_appendRadio = new JRadioButton(radioButtonName);
        m_appendRadio.setToolTipText(radioButtonToolTip);

        radioButtonName = "Replace Column: ";
        radioButtonToolTip = "Replaces the column and changes "
            + "the column type accordingly";
        m_replaceRadio = new JRadioButton(radioButtonName);
        m_replaceRadio.setToolTipText(radioButtonToolTip);
        // show all columns
        m_replaceColumnCombo =
            new ColumnSelectionPanel((Border)null, DataValue.class);
        m_replaceColumnCombo.setRequired(false);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_appendRadio);
        buttonGroup.add(m_replaceRadio);
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_replaceColumnCombo.setEnabled(m_replaceRadio.isSelected());
                m_newNameField.setEnabled(m_appendRadio.isSelected());
            }
        };
        m_appendRadio.addActionListener(actionListener);
        m_replaceRadio.addActionListener(actionListener);

        m_compileOnCloseChecker = new JCheckBox("Syntax check on close");
        m_compileOnCloseChecker.setToolTipText("Checks the syntax of the "
                + "expression on close.");

        m_insertMissingAsNullChecker = new JCheckBox("Insert Missing As Null");
        m_insertMissingAsNullChecker.setToolTipText("If unselected, missing "
                + "values in the input will produce a missing cell result");

        StringManipulatorProvider provider =
            StringManipulatorProvider.getDefault();
        m_categories = new JComboBox(provider.getCategories());
        m_categories.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                String category = (String)cb.getSelectedItem();
                updateManipulatorList(category);
            }
        });
        m_manipulators = new JList(new DefaultListModel());
        m_manipulators.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        m_manipulators.setCellRenderer(new ManipulatorListCellRenderer());
        m_manipulators.addKeyListener(new KeyAdapter() {
            /** {@inheritDoc} */
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    Object selected = m_manipulators.getSelectedValue();
                    if (selected != null) {
                        onSelectionInManipulatorList(selected);
                    }
                }
            }
        });
        m_manipulators.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object selected = m_manipulators.getSelectedValue();
                    if (selected != null) {
                        onSelectionInManipulatorList(selected);
                    }
                }
            }
        });
        m_manipulators.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent e) {
                Object selected = m_manipulators.getSelectedValue();
                if (selected != null) {
                    StringManipulator manipulator =
                        (StringManipulator) selected;
                    m_description.setText(manipulator.getDescription());
                    m_description.setCaretPosition(0);
                } else {
                    m_description.setText("");
                }
            }
        });
        m_description = new JTextPane();
        HTMLEditorKit kit = new HTMLEditorKit();
        m_description.setEditorKit(kit);
        m_description.setEditable(false);

        updateManipulatorList(StringManipulatorProvider.ALL_CATEGORY);
        return createPanel();
    }


//    private JComponent createEditorComponent() {
//        JEditorPane editor = new JEditorPane();
//        Font font = editor.getFont();
//        Font newFont = new Font(Font.MONOSPACED, Font.PLAIN,
//                (font == null ? 12 : font.getSize()));
//        editor.setFont(newFont);
//        m_expEdit = editor;
//        JScrollPane bodyScroller = new JScrollPane(m_expEdit);
//        return bodyScroller;
//    }

    private JComponent createEditorComponent() {
        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        JScrollPane scroller = new RTextScrollPane(textArea);

        // A CompletionProvider is what knows of all possible completions, and
        // analyzes the contents of the text area at the caret position to
        // determine what completion choices should be presented. Most
        // instances of CompletionProvider (such as DefaultCompletionProvider)
        // are designed so that they can be shared among multiple text
        // components.
        CompletionProvider provider = createCompletionProvider();

        // An AutoCompletion acts as a "middle-man" between a text component
        // and a CompletionProvider. It manages any options associated with
        // the auto-completion (the popup trigger key, whether to display a
        // documentation window along with completion choices, etc.). Unlike
        // CompletionProviders, instances of AutoCompletion cannot be shared
        // among multiple text components.
        AutoCompletion ac = new AutoCompletion(provider);
        ac.setShowDescWindow(true);

        ac.install(textArea);

        m_expEdit = textArea;
        return scroller;
    }

    /**
     * Create a simple provider that adds some Java-related completions.
     *
     * @return The completion provider.
     */
    private CompletionProvider createCompletionProvider() {
        m_completionProvider = new JavaScriptingCompletionProvider();

        Collection<StringManipulator> manipulators =
            StringManipulatorProvider.getDefault().getManipulators(
                        StringManipulatorProvider.ALL_CATEGORY);
        for (StringManipulator m : manipulators) {
            // A BasicCompletion is just a straightforward word completion.
            m_completionProvider.addCompletion(
                    new BasicCompletion(m_completionProvider,
                    m.getName(), m.getDisplayName(), m.getDescription()));
        }

        return m_completionProvider;
    }


    private void updateManipulatorList(final String category) {
        Object selected = m_manipulators.getSelectedValue();
        DefaultListModel model = (DefaultListModel)m_manipulators.getModel();
        model.clear();
        StringManipulatorProvider provider =
            StringManipulatorProvider.getDefault();
        for (StringManipulator manipulator
                : provider.getManipulators(category)) {
            model.addElement(manipulator);
        }
        m_manipulators.setSelectedValue(selected, true);
    }

    private JPanel createPanel() {
        final JSplitPane varSplitPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JScrollPane colListPane = new JScrollPane(m_colList);
        colListPane.setBorder(createEmptyTitledBorder("Column List"));
        varSplitPane.setTopComponent(colListPane);

        // set variable panel
        JScrollPane pane = new JScrollPane(m_flowVarsList);
        pane.setBorder(createEmptyTitledBorder("Flow Variable List"));
        varSplitPane.setBottomComponent(pane);
        varSplitPane.setOneTouchExpandable(true);
        varSplitPane.setResizeWeight(0.9);

        JPanel centerPanel = new JPanel(new GridLayout(0, 1));
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(varSplitPane);

        mainSplitPane.setRightComponent(createFunctionAndExpressionPanel());
        centerPanel.add(mainSplitPane);

        JPanel southPanel = new JPanel(new GridLayout(0, 2));
        JPanel replaceOrAppend = createAndOrReplacePanel();
        southPanel.add(replaceOrAppend);


        JPanel returnTypeAndCompilation = createReturnTypeAndCompilationPanel();
        southPanel.add(returnTypeAndCompilation);

        JPanel p = new JPanel(new BorderLayout());
        p.add(centerPanel, BorderLayout.CENTER);
        p.add(southPanel, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(820, 420));
        return p;
    }

    private JPanel createFunctionAndExpressionPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 6, 4, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        p.add(new JLabel("Category"), c);
        c.gridy++;
        c.insets = new Insets(2, 6, 4, 6);
        p.add(m_categories, c);

        c.gridy++;
        c.insets = new Insets(6, 6, 4, 6);
        p.add(new JLabel("Function"), c);
        c.gridy++;
        c.weighty = 1;
        c.insets = new Insets(2, 6, 4, 6);
        JScrollPane manipScroller = new JScrollPane(m_manipulators);
        manipScroller.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        manipScroller.setMinimumSize(new Dimension(
                manipScroller.getPreferredSize().width - 45,
                manipScroller.getMinimumSize().height));
        manipScroller.setPreferredSize(manipScroller.getMinimumSize());
        p.add(manipScroller, c);
        c.weighty = 0;

        c.gridy++;
        c.insets = new Insets(6, 6, 4, 6);
        p.add(new JLabel("Expression"), c);
        c.gridy++;
        c.insets = new Insets(2, 6, 4, 6);
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 0.6;

        JComponent editor = createEditorComponent();
        editor.setPreferredSize(editor.getMinimumSize());
        p.add(editor, c);
        c.weightx = 0;
        c.weighty = 0;

        c.gridy = 0;
        c.gridx = 1;
        c.insets = new Insets(6, 2, 4, 6);
        p.add(new JLabel("Description"), c);
        c.gridy++;
        c.gridheight = 3;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(2, 2, 4, 6);
        m_description.setPreferredSize(m_description.getMinimumSize());
        JScrollPane descScroller = new JScrollPane(m_description);
        descScroller.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        descScroller.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descScroller.setPreferredSize(descScroller.getMinimumSize());
        p.add(descScroller, c);

        return p;
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

    private void onSelectionInManipulatorList(final Object selected) {
        if (selected != null) {
            StringManipulator manipulator = (StringManipulator) selected;
            String selectedString = m_expEdit.getSelectedText();
            StringBuilder newStr = new StringBuilder(
                    manipulator.getName());
            newStr.append('(');
            for (int i = 0; i < manipulator.getNrArgs(); i++) {
                newStr.append(i > 0 ? ", " : "");
                if (i == 0 && selectedString != null) {
                    newStr.append(selectedString);
                }
            }
            newStr.append(')');

            m_expEdit.replaceSelection(newStr.toString());
            if (manipulator.getNrArgs() > 0 && selectedString == null) {
                int caretPos = m_expEdit.getCaretPosition();
                m_expEdit.setCaretPosition(
                        1 + m_expEdit.getText().indexOf('(',
                        caretPos - newStr.toString().length()));
            }

            m_expEdit.requestFocus();
        }
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

    private JPanel createAndOrReplacePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        m_appendRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_newNameField.requestFocus();
                m_newNameField.selectAll();
            }
        });

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 6, 2, 6);
        c.gridx = 0;
        c.gridy = 0;
        panel.add(m_appendRadio, c);

        c.gridx += 1;
        c.insets = new Insets(6, 0, 2, 6);
        panel.add(m_newNameField, c);

        c.gridy += 1;
        c.gridx = 0;
        c.insets = new Insets(2, 6, 4, 6);
        panel.add(m_replaceRadio, c);

        c.gridx += 1;
        c.insets = new Insets(2, 0, 4, 6);
        panel.add(m_replaceColumnCombo, c);

        return panel;
    }

    private JPanel createReturnTypeAndCompilationPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 6, 4, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        p.add(m_insertMissingAsNullChecker, c);
        c.gridy++;
        c.insets = new Insets(2, 6, 4, 6);
        p.add(m_compileOnCloseChecker, c);

        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        DataTableSpec spec = specs[0];
        StringManipulationSettings s = new StringManipulationSettings();
        s.loadSettingsInDialog(settings, spec);

        String exp = s.getExpression();

        String defaultNewName = "new column";
        String newName = s.getColName();
        boolean isReplace = s.isReplace();
        boolean isTestCompilation = s.isTestCompilationOnDialogClose();
        boolean isInsertMissingAsNull = s.isInsertMissingAsNull();

        m_newNameField.setText("");
        // will select newColName only if it is in the spec list
        try {
            m_replaceColumnCombo.update(spec, newName);
        } catch (NotConfigurableException e1) {
            NodeLogger.getLogger(getClass()).coding("Combo box throws "
                    + "exception although content is not required", e1);
        }

        m_currentSpec = spec;
        // whether there are variables or columns available
        // -- which of two depends on the customizer
        boolean fieldsAvailable = m_replaceColumnCombo.getNrItemsInList() > 0;

        if (isReplace && fieldsAvailable) {
            m_replaceRadio.doClick();
        } else {
            m_appendRadio.doClick();
            String newNameString = (newName != null ? newName
                    : defaultNewName);
            m_newNameField.setText(newNameString);
        }
        m_replaceRadio.setEnabled(fieldsAvailable);
        m_expEdit.setText(exp);
        m_expEdit.requestFocus();

        DefaultListModel listModel = (DefaultListModel)m_colList.getModel();
        listModel.removeAllElements();

        // we have Expression.VERSION_2X
        listModel.addElement(Expression.ROWID);
        listModel.addElement(Expression.ROWINDEX);
        listModel.addElement(Expression.ROWCOUNT);

        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            listModel.addElement(colSpec);
        }
        m_completionProvider.setColumns(spec);
        DefaultListModel fvListModel =
            (DefaultListModel)m_flowVarsList.getModel();
        fvListModel.removeAllElements();
        Map<String, FlowVariable> m = m_varProvider.getAvailableFlowVariables();
        for (FlowVariable v : m.values()) {
            fvListModel.addElement(v);
        }
        m_completionProvider.setFlowVariables(m.values());

        m_compileOnCloseChecker.setSelected(isTestCompilation);
        m_insertMissingAsNullChecker.setSelected(isInsertMissingAsNull);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        StringManipulationSettings s = new StringManipulationSettings();

        String newColName = null;
        boolean isReplace = m_replaceRadio.isSelected();
        if (isReplace) {
            newColName = m_replaceColumnCombo.getSelectedColumn();
        } else {
            newColName = m_newNameField.getText();
        }
        s.setReplace(isReplace);
        s.setColName(newColName);
        String exp = m_expEdit.getText();
        s.setExpression(exp);
        boolean isTestCompilation = m_compileOnCloseChecker.isSelected();
        s.setTestCompilationOnDialogClose(isTestCompilation);
        if (isTestCompilation && m_currentSpec != null) {
            try {
                Expression.compile(s.createJavaScriptingSettings(),
                        m_currentSpec);
            } catch (CompilationFailedException cfe) {
                throw new InvalidSettingsException(cfe.getMessage(), cfe);
            }
        }
        s.setInsertMissingAsNull(m_insertMissingAsNullChecker.isSelected());
        s.saveSettingsTo(settings);
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

    /**
     * Renderer the name of string manipulators.
     */
    private static class ManipulatorListCellRenderer extends
            DefaultListCellRenderer {
        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            StringManipulator m = (StringManipulator)value;
            Component c = super.getListCellRendererComponent(list,
                    m.getDisplayName(),
                    index, isSelected, cellHasFocus);
            return c;
        }
    }
}
