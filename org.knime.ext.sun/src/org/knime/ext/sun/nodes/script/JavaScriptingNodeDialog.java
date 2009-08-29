/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 */
package org.knime.ext.sun.nodes.script;

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
import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.SimpleFileFilter;
import org.knime.ext.sun.nodes.script.expression.CompilationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JavaScriptingNodeDialog extends NodeDialogPane {

    private static final String COMMENT_ON_RETURN_STATEMENT =
          "/* Please note that as of KNIME 2.0\n"
        + " * you must use the \"return\" keyword\n"
        + " * to specify the return value.\n"
        + " */\n";

    private final JList m_colList;

    private final JList m_flowVarsList;

    private final JEditorPane m_expEdit;

    private final JEditorPane m_headerEdit;

    private final JRadioButton m_appendRadio;

    private final JTextField m_newColNameField;

    private final JRadioButton m_replaceRadio;

    private final ColumnSelectionPanel m_replaceCombo;

    private final ButtonGroup m_returnTypeButtonGroup;

    private final JCheckBox m_isArrayReturnChecker;

    private final JCheckBox m_compileOnCloseChecker;

    private final JList m_addJarList;

    private DataTableSpec m_currentSpec = null;

    private int m_currentVersion;

    /** Inits GUI. */
    @SuppressWarnings("unchecked")
    public JavaScriptingNodeDialog() {
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

        m_newColNameField = new JTextField(10);
        m_appendRadio = new JRadioButton("Append Column: ");
        m_appendRadio.setToolTipText("Appends a new column to the input "
                + "table with a given name and type.");
        m_replaceRadio = new JRadioButton("Replace Column: ");
        m_replaceRadio.setToolTipText("Replaces the column and changes "
                + "the column type accordingly");
        // show all columns
        m_replaceCombo =
            new ColumnSelectionPanel((Border)null, DataValue.class);
        m_replaceCombo.setRequired(false);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_appendRadio);
        buttonGroup.add(m_replaceRadio);
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_replaceCombo.setEnabled(m_replaceRadio.isSelected());
                m_newColNameField.setEnabled(m_appendRadio.isSelected());
            }
        };
        m_appendRadio.addActionListener(actionListener);
        m_replaceRadio.addActionListener(actionListener);

        m_compileOnCloseChecker = new JCheckBox("Compile on close");
        m_compileOnCloseChecker.setToolTipText("Compiles the code on close "
                + "to identify potential syntax problems.");

        JRadioButton intReturnRadio = new JRadioButton("Integer");
        intReturnRadio.setActionCommand(Integer.class.getName());
        JRadioButton doubleReturnRadio = new JRadioButton("Double");
        doubleReturnRadio.setActionCommand(Double.class.getName());
        JRadioButton stringReturnRadio = new JRadioButton("String");
        stringReturnRadio.setActionCommand(String.class.getName());
        JRadioButton dateReturnRadio = new JRadioButton("Date");
        dateReturnRadio.setActionCommand(Date.class.getName());
        m_returnTypeButtonGroup = new ButtonGroup();
        m_returnTypeButtonGroup.add(intReturnRadio);
        m_returnTypeButtonGroup.add(doubleReturnRadio);
        m_returnTypeButtonGroup.add(stringReturnRadio);
        m_returnTypeButtonGroup.add(dateReturnRadio);

        m_isArrayReturnChecker = new JCheckBox("Array Return");

        m_addJarList = new JList(new DefaultListModel()) {
            /** {@inheritDoc} */
            @Override
            protected void processComponentKeyEvent(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
                    int end = getModel().getSize() - 1;
                    getSelectionModel().setSelectionInterval(0, end);
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    onJarRemove();
                }
            }
        };

        addTab("Java Snippet", createPanel());
        addTab("Additional Libraries", createJarPanel());
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

    private JPanel createPanel() {
        JPanel finalPanel = new JPanel(new BorderLayout());
        final JSplitPane varSplitPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JScrollPane pane = new JScrollPane(m_colList);
        pane.setBorder(createEmptyTitledBorder("Column List"));
        varSplitPane.setTopComponent(pane);
        // set variable panel only if expert mode is enabled.
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_EXPERT_MODE)) {
            pane = new JScrollPane(m_flowVarsList);
            pane.setBorder(createEmptyTitledBorder("Flow Variable List"));
            varSplitPane.setBottomComponent(pane);
            varSplitPane.setOneTouchExpandable(true);
            varSplitPane.setResizeWeight(0.9);
        }
        JComponent editorMainPanel = createEditorPanel();

        JPanel centerPanel = new JPanel(new GridLayout(0, 1));
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(varSplitPane);
        mainSplitPane.setRightComponent(editorMainPanel);
        centerPanel.add(mainSplitPane);

        JPanel southPanel = new JPanel(new GridLayout(0, 2));
        JPanel replaceOrAppend = createAndOrReplaceColumnPanel();
        southPanel.add(replaceOrAppend);

        JPanel returnTypeAndCompilation = createReturnTypeAndCompilationPanel();
        southPanel.add(returnTypeAndCompilation);

        southPanel.add(returnTypeAndCompilation);
        finalPanel.add(centerPanel, BorderLayout.CENTER);
        finalPanel.add(southPanel, BorderLayout.SOUTH);
        return finalPanel;
    }

    private JComponent createEditorPanel() {
        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setOneTouchExpandable(true);

        JScrollPane headerScroller = new JScrollPane(m_headerEdit);
        String borderTitle = "Global Variable Declaration";
        headerScroller.setBorder(createEmptyTitledBorder(borderTitle));
        split.setTopComponent(headerScroller);

        JScrollPane bodyScroller = new JScrollPane(m_expEdit);
        borderTitle = "Method Body";
        bodyScroller.setBorder(createEmptyTitledBorder(borderTitle));

        split.setBottomComponent(bodyScroller);
        split.setResizeWeight(0.2);
        return split;
    }

    private JPanel createAndOrReplaceColumnPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBorder(BorderFactory
                .createTitledBorder("Replace or append result"));
        m_appendRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_newColNameField.requestFocus();
                m_newColNameField.selectAll();
            }
        });

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_appendRadio, gbc);

        gbc.gridx += 1;
        panel.add(m_newColNameField, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        panel.add(m_replaceRadio, gbc);

        gbc.gridx += 1;
        panel.add(m_replaceCombo, gbc);
        return panel;
    }

    private JPanel createReturnTypeAndCompilationPanel() {
        JPanel returnTypeAndCompilation = new JPanel(new BorderLayout());
        JPanel compilationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        compilationPanel.add(m_compileOnCloseChecker);
        returnTypeAndCompilation.add(compilationPanel, BorderLayout.NORTH);

        JPanel returnType = new JPanel(new BorderLayout());
        returnType.setBorder(BorderFactory.createTitledBorder("Return type"));
        JPanel returnFieldType = new JPanel(new GridLayout(0, 2));
        for (Enumeration<?> e = m_returnTypeButtonGroup.getElements(); e
                .hasMoreElements();) {
            returnFieldType.add((AbstractButton)e.nextElement());
        }
        returnType.add(returnFieldType, BorderLayout.CENTER);
        JPanel returnArrayPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        returnArrayPanel.add(m_isArrayReturnChecker);
        returnType.add(returnArrayPanel, BorderLayout.SOUTH);

        returnTypeAndCompilation.add(returnType, BorderLayout.CENTER);
        return returnTypeAndCompilation;

    }

    private JPanel createJarPanel() {
        m_addJarList.setCellRenderer(new ConvenientComboBoxRenderer());
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(m_addJarList), BorderLayout.CENTER);
        JPanel southP = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton addButton = new JButton("Add...");
        addButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                onJarAdd();
            }
        });
        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                onJarRemove();
            }
        });
        m_addJarList.addListSelectionListener(new ListSelectionListener() {
            /** {@inheritDoc} */
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                removeButton.setEnabled(!m_addJarList.isSelectionEmpty());
            }
        });
        removeButton.setEnabled(!m_addJarList.isSelectionEmpty());
        southP.add(addButton);
        southP.add(removeButton);
        p.add(southP, BorderLayout.SOUTH);

        JPanel northP = new JPanel(new FlowLayout());
        JLabel label = new JLabel("<html><body>Specify additional jar files "
                + "that are necessary for the snippet to run</body></html>");
        northP.add(label);
        p.add(northP, BorderLayout.NORTH);
        return p;
    }

    private JFileChooser m_jarFileChooser;

    private void onJarAdd() {
        DefaultListModel model = (DefaultListModel)m_addJarList.getModel();
        Set<Object> hash = new HashSet<Object>();
        for (Enumeration<?> e = model.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        StringHistory history =
            StringHistory.getInstance("java_snippet_jar_dirs");
        if (m_jarFileChooser == null) {
            File dir = null;
            for (String h : history.getHistory()) {
                File temp = new File(h);
                if (temp.isDirectory()) {
                    dir = temp;
                    break;
                }
            }
            m_jarFileChooser = new JFileChooser(dir);
            m_jarFileChooser.setFileFilter(
                    new SimpleFileFilter(".zip", ".jar"));
            m_jarFileChooser.setMultiSelectionEnabled(true);
        }
        int result = m_jarFileChooser.showDialog(m_addJarList, "Select");

        if (result == JFileChooser.APPROVE_OPTION) {
            for (File f : m_jarFileChooser.getSelectedFiles()) {
                String s = f.getAbsolutePath();
                if (hash.add(s)) {
                    model.addElement(s);
                }
            }
            history.add(
                    m_jarFileChooser.getCurrentDirectory().getAbsolutePath());
        }
    }

    private void onJarRemove() {
        DefaultListModel model = (DefaultListModel)m_addJarList.getModel();
        int[] sels = m_addJarList.getSelectedIndices();
        int last = Integer.MAX_VALUE;
        // traverse backwards (editing list in loop body)
        for (int i = sels.length - 1; i >= 0; i--) {
            assert sels[i] < last : "Selection list not ordered";
            model.remove(sels[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        JavaScriptingSettings s = new JavaScriptingSettings();
        s.loadSettingsInDialog(settings, specs[0]);
        String exp = s.getExpression();
        String header = s.getHeader();

        String rType = s.getReturnType().getName();
        boolean isArrayReturn = s.isArrayReturn();
        String defaultColName = "new column";
        String newColName = s.getColName();
        boolean isReplace = s.isReplace();
        String[] jarFiles = s.getJarFiles();
        boolean isTestCompilation = s.isTestCompilationOnDialogClose();
        m_currentVersion = s.getExpressionVersion();
        if (m_currentVersion == Expression.VERSION_2X) {
            if (exp == null || exp.length() == 0) {
                exp = COMMENT_ON_RETURN_STATEMENT;
            }
        }
        m_newColNameField.setText("");
        // will select newColName only if it is in the spec list
        m_replaceCombo.update(specs[0], newColName);
        m_currentSpec = specs[0];
        if (isReplace && m_replaceCombo.getNrItemsInList() > 0) {
            m_replaceRadio.doClick();
        } else {
            m_appendRadio.doClick();
            String newColString = (newColName != null ? newColName
                    : defaultColName);
            m_newColNameField.setText(newColString);
        }
        m_replaceRadio.setEnabled(m_replaceCombo.getNrItemsInList() > 0);
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
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            DataColumnSpec colSpec = specs[0].getColumnSpec(i);
            listModel.addElement(colSpec);
        }
        DefaultListModel fvListModel =
            (DefaultListModel)m_flowVarsList.getModel();
        fvListModel.removeAllElements();
        for (FlowVariable v : getAvailableFlowVariables().values()) {
            fvListModel.addElement(v);
        }
        m_compileOnCloseChecker.setSelected(isTestCompilation);
        DefaultListModel jarListModel =
            (DefaultListModel)m_addJarList.getModel();
        jarListModel.removeAllElements();
        for (String jarFile : jarFiles) {
            jarListModel.addElement(jarFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        JavaScriptingSettings s = new JavaScriptingSettings();
        String newColName = null;
        boolean isReplace = m_replaceRadio.isSelected();
        if (isReplace) {
            newColName = m_replaceCombo.getSelectedColumn();
        } else {
            newColName = m_newColNameField.getText();
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
        DefaultListModel jarListModel =
            (DefaultListModel)m_addJarList.getModel();
        if (jarListModel.getSize() > 0) {
            String[] copy = new String[jarListModel.getSize()];
            jarListModel.copyInto(copy);
            s.setJarFiles(copy);
        }
        if (isTestCompilation && m_currentSpec != null) {
            try {
                Expression.compile(s, m_currentSpec);
            } catch (CompilationFailedException cfe) {
                throw new InvalidSettingsException(cfe.getMessage());
            }
        }
        s.saveSettingsTo(settings);
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
