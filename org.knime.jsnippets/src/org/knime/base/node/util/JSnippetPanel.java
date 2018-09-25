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
 * ---------------------------------------------------------------------
 *
 * History
 *   21.03.2012 (meinl): created
 */
package org.knime.base.node.util;

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
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.rsyntaxtextarea.KnimeCompletionProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 * This class is a sophisticated panel that offers an editor for almost
 * arbitrary java functions. The user has the ability to choose from a list of
 * functions that are provided by a {@link ManipulatorProvider}. In addition he
 * sees a list of available columns and flow variables from the input table. Subclasses may customize
 * or extend the layout using the {@link #initSubComponents()},
 * {@link #layoutLeftComponent()} and {@link #layoutRightComponent()} methods.
 *
 * @author Heiko Hofer
 * @author Thorsten Meinl, University of Konstanz
 * @author Marcel Hanser
 * @since 2.6
 */
@SuppressWarnings("serial")
public class JSnippetPanel extends JPanel {
    private JList m_colList;

    private JList m_flowVarsList;

    private JTextComponent m_expEdit;

    private JComboBox m_categories;

    private JList m_manipulators;

    private JTextPane m_description;

    private final KnimeCompletionProvider m_completionProvider;

    private final ManipulatorProvider m_manipProvider;

    private final boolean m_showColumns;

    private final boolean m_showFlowVariables;

    /**
     * Create new instance.
     *
     * @param manipulatorProvider a manipulation provider that provides all
     *            available functions
     * @param completionProvider a completion provider used for autocompletion
     *            in the editor
     * @since 3.7
     */
    public JSnippetPanel(final ManipulatorProvider manipulatorProvider,
                         final KnimeCompletionProvider completionProvider) {
        this(manipulatorProvider, completionProvider, true);
    }

    /**
     * Create new instance.
     *
     * @param manipulatorProvider a manipulation provider that provides all
     *            available functions
     * @param completionProvider a completion provider used for autocompletion
     *            in the editor
     * @param showColumns Show the columns panel, or hide it?
     * @since 3.7
     */
    public JSnippetPanel(final ManipulatorProvider manipulatorProvider,
        final KnimeCompletionProvider completionProvider, final boolean showColumns) {
        this(manipulatorProvider, completionProvider, showColumns, true);
    }
    /**
     * Create new instance.
     *
     * @param manipulatorProvider a manipulation provider that provides all
     *            available functions
     * @param completionProvider a completion provider used for autocompletion
     *            in the editor
     * @param showColumns Show the columns panel, or hide it?
     * @param showFlowVariables Show the flow variables panel, or hide it?
     * @since 3.7
     */
    public JSnippetPanel(final ManipulatorProvider manipulatorProvider,
            final KnimeCompletionProvider completionProvider, final boolean showColumns, final boolean showFlowVariables) {
        m_manipProvider = manipulatorProvider;
        m_completionProvider = completionProvider;
        this.m_showColumns = showColumns;
        this.m_showFlowVariables = showFlowVariables;
        setLayout(new BorderLayout());
        initCompletionProvider();
        initComponents();
        createStringManipulationPanel();
    }

    private void createStringManipulationPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(0, 1));
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(layoutLeftComponent());
        mainSplitPane.setRightComponent(layoutRightComponent());
        centerPanel.add(mainSplitPane);

        add(centerPanel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(820, 420));
        setMinimumSize(new Dimension(600, 350));
    }



    /**
     * Hook for subclasses to create and initiates the components. The method is invoked
     * before {@link #layoutLeftComponent()} and {@link #layoutRightComponent()}.
     * @since 2.10
     */
    protected void initSubComponents() {
        //NOOP
    }

    /**
     * Subclasses can override this method to customize the left side layout of the main split pain.
     * If the subclass uses self-defined components make sure that these are already created and
     * initialized during the call of {@link #initSubComponents()}.
     *
     * @return the component for the left side of the main split-panel
     * @since 2.10
     */
    protected JComponent layoutLeftComponent() {
        JComponent leftComponent;
        if (m_showColumns && m_showFlowVariables) {
            final JSplitPane varSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            leftComponent = varSplitPane;
            JScrollPane colListPane = new JScrollPane(m_colList);
            colListPane.setBorder(createEmptyTitledBorder("Column List"));
            varSplitPane.setTopComponent(colListPane);

            // set variable panel
            JScrollPane pane = new JScrollPane(m_flowVarsList);
            pane.setBorder(createEmptyTitledBorder("Flow Variable List"));
            varSplitPane.setBottomComponent(pane);
            varSplitPane.setOneTouchExpandable(true);
            varSplitPane.setResizeWeight(0.9);
        } else if (m_showColumns && !m_showFlowVariables) {
            leftComponent = new JScrollPane(m_colList);
            leftComponent.setBorder(createEmptyTitledBorder("Column List"));
        } else if (m_showFlowVariables) {
            leftComponent = new JScrollPane(m_flowVarsList);
            leftComponent.setBorder(createEmptyTitledBorder("Flow Variable List"));
        } else {
            leftComponent = new JPanel();
        }
        return leftComponent;
    }

    /**
     * Subclasses can override this method to customize the right side layout of the main split pain.
     * If the subclass uses self-defined components make sure that these are already created and
     * initialized during the call of {@link #initSubComponents()}.
     *
     * @return the component for the right side of the main split-panel
     * @since 2.10
     */
    protected JComponent layoutRightComponent() {
       return createFunctionAndExpressionPanel();
    }

    private void initComponents() {
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

        m_categories = new JComboBox(m_manipProvider.getCategories().toArray());
        m_categories.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                String category = (String)cb.getSelectedItem();
                updateManipulatorList(category);
            }
        });
        m_manipulators = new JList(new DefaultListModel());
        m_manipulators.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
                    Manipulator manipulator = (Manipulator)selected;
                    m_description.setText(manipulator.getDescription());
                    m_description.setCaretPosition(0);
                } else {
                    m_description.setText("");
                }
            }
        });
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet css = new StyleSheet();
        css.addRule("* { font-family: sans-serif; }");
        kit.setStyleSheet(css);

        m_description = new JTextPane();
        m_description.setEditorKit(kit);
        m_description.setEditable(false);
        m_description.setBackground(getBackground());
        updateManipulatorList(ManipulatorProvider.ALL_CATEGORY);

        initSubComponents();
    }

    /**
     * Creates the text editor component along with the scrollpane.
     *
     * @return The {@link RSyntaxTextArea} wrapped within a {@link JScrollPane}.
     * @since 2.8
     */
    protected JComponent createEditorComponent() {
        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        JScrollPane scroller = new JScrollPane(textArea);

        // An AutoCompletion acts as a "middle-man" between a text component
        // and a CompletionProvider. It manages any options associated with
        // the auto-completion (the popup trigger key, whether to display a
        // documentation window along with completion choices, etc.). Unlike
        // CompletionProviders, instances of AutoCompletion cannot be shared
        // among multiple text components.
        AutoCompletion ac = new AutoCompletion(m_completionProvider);
        ac.setShowDescWindow(true);

        ac.install(textArea);

        setExpEdit(textArea);
        return scroller;
    }

    private void initCompletionProvider() {
        Collection<? extends Manipulator> manipulators =
                m_manipProvider
                        .getManipulators(ManipulatorProvider.ALL_CATEGORY);
        for (Manipulator m : manipulators) {
            Completion completion =
                    new BasicCompletion(m_completionProvider, m.getName(),
                            m.getDisplayName(), m.getDescription());
            m_completionProvider.addCompletion(completion);
        }
    }

    private void updateManipulatorList(final String category) {
        Object selected = m_manipulators.getSelectedValue();
        DefaultListModel model = (DefaultListModel)m_manipulators.getModel();
        model.clear();
        for (Manipulator manipulator : m_manipProvider
                .getManipulators(category)) {
            model.addElement(manipulator);
        }
        m_manipulators.setSelectedValue(selected, true);
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
        c.weightx = 0.05;
        c.weighty = 1;
        c.insets = new Insets(2, 6, 4, 6);
        JScrollPane manipScroller = new JScrollPane(getManipulators());
        Dimension preferred =
                getManipulators().getPreferredScrollableViewportSize();

        manipScroller.setPreferredSize(new Dimension(preferred.width + 5, 40));
        manipScroller.setMinimumSize(new Dimension(preferred.width - 45, 40));
        manipScroller.setMaximumSize(new Dimension(preferred.width + 10, 1000));
        p.add(manipScroller, c);
        c.weighty = 0;

        c.gridy++;
        c.insets = new Insets(6, 6, 4, 6);
        p.add(new JLabel("Expression"), c);
        c.gridy++;
        c.insets = new Insets(2, 6, 4, 6);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 0.6;

        JComponent editorComponent = createEditorComponent();
        editorComponent.setPreferredSize(editorComponent.getMinimumSize());
        p.add(editorComponent, c);
        c.weighty = 0;

        c.gridy = 0;
        c.gridx = 1;
        c.insets = new Insets(6, 2, 4, 6);
        p.add(new JLabel("Description"), c);
        c.gridy++;
        c.gridheight = 3;
        c.weightx = 0.5;
        c.weighty = 1;
        c.insets = new Insets(2, 2, 4, 6);
        getDescription().setPreferredSize(getDescription().getMinimumSize());
        JScrollPane descScroller = new JScrollPane(getDescription());
        descScroller
                .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        descScroller
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descScroller.setPreferredSize(descScroller.getMinimumSize());
        p.add(descScroller, c);

        return p;
    }

    /**
     * Method that is being called by listener when an object in the column list has been selected.
     *
     * @param selected The selected object.
     * @since 3.6
     */
    protected void onSelectionInColumnList(final Object selected) {
        if (selected != null) {
            String enter;
            if (selected instanceof String) {
                enter = "$$" + selected + "$$";
            } else {
                DataColumnSpec colSpec = (DataColumnSpec)selected;
                String name = colSpec.getName().replace("$", "\\$");
                enter = m_completionProvider.escapeColumnName(name);
            }
            m_expEdit.replaceSelection(enter);
            m_colList.clearSelection();
            m_expEdit.requestFocus();
        }
    }

    /**
     * Method that is being called by listener when an object in the variable list has been selected.
     *
     * @param selected The selected object.
     * @since 3.6
     */
    protected void onSelectionInVariableList(final Object selected) {
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
            String enter =
                    m_completionProvider.escapeFlowVariableName(typeChar
                            + v.getName()/*.replace("\\", "\\\\").replace("}", "\\}")*/);
            m_expEdit.replaceSelection(enter);
            m_flowVarsList.clearSelection();
            m_expEdit.requestFocus();
        }
    }

    /**
     * Inserts text based on the selected manipulator.
     *
     * @param selected A {@link Manipulator}.
     * @since 2.8
     */
    protected void onSelectionInManipulatorList(final Object selected) {
        if (selected != null) {
            Manipulator manipulator = (Manipulator)selected;
            String selectedString = m_expEdit.getSelectedText();
            StringBuilder newStr = new StringBuilder(manipulator.getName());
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
                m_expEdit.setCaretPosition(1 + m_expEdit.getText().indexOf('(',
                        caretPos - newStr.toString().length()));
            }

            m_expEdit.requestFocus();
        }
    }

    /**
     * Create an empty, titled border.
     *
     * @param string Title of the border.
     * @return Such a new border.
     */
    private static final Border createEmptyTitledBorder(final String string) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(5, 0, 0, 0), string,
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP);
    }

    /**
     * Updates the contents of the panel with new values.
     *
     * @param expression the expression in the editor
     * @param spec the data table spec of the input table; used for filling the
     *            list of available columns
     * @param flowVariables a map with all available flow variables; used to
     *            fill the list of available flow variables
     */
    public void update(final String expression, final DataTableSpec spec,
                       final Map<String, FlowVariable> flowVariables) {
        // we have Expression.VERSION_2X
        final String[] expressions = new String[] {Expression.ROWID, Expression.ROWINDEX, Expression.ROWCOUNT};
        update(expression, spec, flowVariables, expressions);

    }
    /**
     * Updates the contents of the panel with new values.
     *
     * @param expression the expression in the editor
     * @param spec the data table spec of the input table; used for filling the
     *            list of available columns
     * @param flowVariables a map with all available flow variables; used to
     *            fill the list of available flow variables
     * @param expressions {@link Expression}s' constants to add to the columns list.
     * @since 2.8
     */
    public void update(final String expression, final DataTableSpec spec,
            final Map<String, FlowVariable> flowVariables, final String[] expressions) {
        m_expEdit.setText(expression);
        m_expEdit.requestFocus();

        DefaultListModel listModel = (DefaultListModel)m_colList.getModel();
        listModel.removeAllElements();

        // we have Expression.VERSION_2X
        for (String exp : expressions) {
            listModel.addElement(exp);
        }

        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            listModel.addElement(colSpec);
        }
        m_completionProvider.setColumns(spec);
        DefaultListModel fvListModel =
                (DefaultListModel)m_flowVarsList.getModel();
        fvListModel.removeAllElements();
        for (FlowVariable v : flowVariables.values()) {
            fvListModel.addElement(v);
        }
        m_completionProvider.setFlowVariables(flowVariables.values());
    }

    /**
     * Returns the expression in the editor.
     *
     * @return a string containing the expression
     */
    public String getExpression() {
        return m_expEdit.getText();
    }

    /**
     * Sets the expression shown in the editor.
     *
     * @param expression a new expression
     */
    public void setExpressions(final String expression) {
        m_expEdit.setText(expression);
        m_expEdit.requestFocus();
    }

    /**
     * @return the colList
     * @since 2.10
     */
    protected JList getColList() {
        return m_colList;
    }

    /**
     * @return the flowVarsList
     * @since 2.10
     */
    protected JList getFlowVarsList() {
        return m_flowVarsList;
    }

    /**
     * @return the expEdit
     * @since 2.10
     */
    protected JTextComponent getExpEdit() {
        return m_expEdit;
    }

    /**
     * @return the categories
     * @since 2.10
     */
    protected JComboBox getCategories() {
        return m_categories;
    }

    /**
     * @return the manipulators
     * @since 2.10
     */
    protected JList getManipulators() {
        return m_manipulators;
    }

    /**
     * @return the description
     * @since 2.10
     */
    protected JTextPane getDescription() {
        return m_description;
    }

    /**
     * @return the showColumns
     * @since 2.10
     */
    protected boolean isShowColumns() {
        return m_showColumns;
    }

    /**
     * @return the showFlowVariables
     * @since 2.10
     */
    protected boolean isShowFlowVariables() {
        return m_showFlowVariables;
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
            Component c =
                    super.getListCellRendererComponent(list, value, index,
                            isSelected, cellHasFocus);
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
            Manipulator m = (Manipulator)value;
            Component c =
                    super.getListCellRendererComponent(list,
                            m.getDisplayName(), index, isSelected, cellHasFocus);
            return c;
        }
    }

    /**
     * @param expEdit the text editor to set
     * @since 2.8
     */
    protected void setExpEdit(final JTextComponent expEdit) {
        this.m_expEdit = expEdit;
    }

    /**
     * @return the completionProvider
     * @since 3.7
     */
    protected KnimeCompletionProvider getCompletionProvider() {
        return m_completionProvider;
    }
}
