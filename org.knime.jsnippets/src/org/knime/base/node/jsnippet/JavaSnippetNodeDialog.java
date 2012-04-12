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
 *   24.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.base.node.jsnippet.ui.ColumnList;
import org.knime.base.node.jsnippet.ui.FieldsTableModel;
import org.knime.base.node.jsnippet.ui.FlowVariableList;
import org.knime.base.node.jsnippet.ui.InFieldsTable;
import org.knime.base.node.jsnippet.ui.JSnippetFieldsController;
import org.knime.base.node.jsnippet.ui.JSnippetTextArea;
import org.knime.base.node.jsnippet.ui.JarListPanel;
import org.knime.base.node.jsnippet.ui.OutFieldsTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * The dialog of the java snippet node.
 *
 * @author Heiko Hofer
 */
public class JavaSnippetNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            JavaSnippetNodeDialog.class);

    private JSnippetTextArea m_snippetTextArea;
    protected ColumnList m_colList;
    protected FlowVariableList m_flowVarsList;
    private JavaSnippetSettings m_settings;
    private JavaSnippet m_snippet;
    private InFieldsTable m_inFieldsTable;
    private OutFieldsTable m_outFieldsTable;
    private JSnippetFieldsController m_fieldsController;

    private JarListPanel m_jarPanel;

    /**
     * Create a new Dialog.
     */
    public JavaSnippetNodeDialog() {
        m_settings = new JavaSnippetSettings();
        m_snippet = new JavaSnippet();
        JPanel panel = createPanel();
        m_fieldsController = new JSnippetFieldsController(m_snippet,
                m_inFieldsTable, m_outFieldsTable);
        m_colList.install(m_snippetTextArea);
        m_colList.install(m_fieldsController);
        m_flowVarsList.install(m_snippetTextArea);
        m_flowVarsList.install(m_fieldsController);
        addTab("Java Snippet", panel);
        panel.setPreferredSize(new Dimension(800, 600));
        addTab("Additional Libraries", createJarPanel());
    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JComponent snippet = createSnippetPanel();
        JComponent colsAndVars = createColsAndVarsPanel();

        JPanel centerPanel = new JPanel(new GridLayout(0, 1));
        JSplitPane centerSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT);
        centerSplitPane.setLeftComponent(colsAndVars);
        centerSplitPane.setRightComponent(snippet);

        m_inFieldsTable = createInFieldsTable();
        m_outFieldsTable = createOutFieldsTable();
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        tabbedPane.addTab("Snippet fields for input", m_inFieldsTable);
        tabbedPane.addTab("Snippet fields for output", m_outFieldsTable);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(centerSplitPane);
        mainSplitPane.setBottomComponent(tabbedPane);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setDividerLocation(600);

        centerPanel.add(mainSplitPane);

        p.add(centerPanel, BorderLayout.CENTER);
        return p;
    }


    private JPanel createJarPanel() {
        m_jarPanel = new JarListPanel();
        m_jarPanel.addListDataListener(new ListDataListener() {
            private void updateSnippet() {
                m_snippet.setJarFiles(m_jarPanel.getJarFiles());
                // force reparsing of the snippet
                for (int i = 0; i < m_snippetTextArea.getParserCount(); i++) {
                    m_snippetTextArea.forceReparsing(i);
                }
            }

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                updateSnippet();
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                updateSnippet();
            }

            @Override
            public void contentsChanged(final ListDataEvent e) {
                updateSnippet();
            }
        });
        return m_jarPanel;
    }


    protected InFieldsTable createInFieldsTable() {
        InFieldsTable table = new InFieldsTable();
        return table;
    }

    protected OutFieldsTable createOutFieldsTable() {
        OutFieldsTable table = new OutFieldsTable(false);
        return table;
    }

    /**
     * @return
     */
    protected JComponent createSnippetPanel() {

        m_snippetTextArea = new JSnippetTextArea(m_snippet);

        // reset style which causes a recreation of the folds
        m_snippetTextArea.setSyntaxEditingStyle(
                SyntaxConstants.SYNTAX_STYLE_NONE);
        m_snippetTextArea.setSyntaxEditingStyle(
                SyntaxConstants.SYNTAX_STYLE_JAVA);
        // collapse all folds
        int foldCount = m_snippetTextArea.getFoldManager().getFoldCount();
        for (int i = 0; i < foldCount; i++) {
            Fold fold = m_snippetTextArea.getFoldManager().getFold(i);
            fold.setCollapsed(true);
        }
        JScrollPane snippetScroller = new RTextScrollPane(m_snippetTextArea);
        JPanel snippet = new JPanel(new BorderLayout());
        snippet.add(snippetScroller, BorderLayout.CENTER);
        ErrorStrip es = new ErrorStrip(m_snippetTextArea);
        snippet.add(es, BorderLayout.LINE_END);
        return snippet;
    }

    /**
     * @return
     */
    protected JComponent createColsAndVarsPanel() {
        JSplitPane varSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        m_colList = new ColumnList();
        JScrollPane colListScroller = new JScrollPane(m_colList);
        colListScroller.setBorder(createEmptyTitledBorder("Column List"));
        varSplitPane.setTopComponent(colListScroller);

        // set variable panel
        m_flowVarsList = new FlowVariableList();
        JScrollPane flowVarScroller = new JScrollPane(m_flowVarsList);
        flowVarScroller.setBorder(
                createEmptyTitledBorder("Flow Variable List"));
        varSplitPane.setBottomComponent(flowVarScroller);
        varSplitPane.setOneTouchExpandable(true);
        varSplitPane.setResizeWeight(0.9);

        return varSplitPane;
    }

    /** Create an empty, titled border.
     * @param string Title of the border.
     * @return Such a new border.
     */
    protected Border createEmptyTitledBorder(final String string) {
        return BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(
                5, 0, 0, 0), string, TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.BELOW_TOP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        // do not close on ESC, since ESC is used to close autocomplete popups
        // in the snippets textarea.
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_colList.setSpec(specs[0]);
        m_flowVarsList.setFlowVariables(getAvailableFlowVariables().values());
        m_snippet.setSettings(m_settings);
        m_jarPanel.setJarFiles(m_settings.getJarFiles());

        m_fieldsController.updateData(m_settings, specs[0],
                getAvailableFlowVariables());

        // set caret position to the start of the custom expression
        m_snippetTextArea.setCaretPosition(
                m_snippet.getDocument().getGuardedSection(
                JavaSnippet.GUARDED_BODY_START).getEnd().getOffset() + 1);
        m_snippetTextArea.requestFocusInWindow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen() {
        m_snippetTextArea.requestFocus();
        m_snippetTextArea.requestFocusInWindow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        JavaSnippetSettings s = m_snippet.getSettings();
        // if settings have less fields than defined in the table it means
        // that the tables contain errors
        FieldsTableModel inFieldsModel =
            (FieldsTableModel)m_inFieldsTable.getTable().getModel();
        if (!inFieldsModel.validateValues()) {
            throw new IllegalArgumentException(
                    "The input fields table has errors.");
        }
        FieldsTableModel outFieldsModel =
            (FieldsTableModel)m_outFieldsTable.getTable().getModel();
        if (!outFieldsModel.validateValues()) {
            throw new IllegalArgumentException(
                    "The output fields table has errors.");
        }

        s.saveSettings(settings);
    }
}
