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
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;

import org.fife.rsta.ac.LanguageSupport;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.rsta.ac.java.JarManager;
import org.fife.rsta.ac.java.JavaLanguageSupport;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.base.node.jsnippet.guarded.GuardedDocument;
import org.knime.base.node.jsnippet.template.AddTemplateDialog;
import org.knime.base.node.jsnippet.template.DefaultTemplateController;
import org.knime.base.node.jsnippet.template.TemplateProvider;
import org.knime.base.node.jsnippet.template.TemplatesPanel;
import org.knime.base.node.jsnippet.ui.ColumnList;
import org.knime.base.node.jsnippet.ui.FieldsTableModel;
import org.knime.base.node.jsnippet.ui.FieldsTableModel.Column;
import org.knime.base.node.jsnippet.ui.FlowVariableList;
import org.knime.base.node.jsnippet.ui.InFieldsTable;
import org.knime.base.node.jsnippet.ui.JSnippetFieldsController;
import org.knime.base.node.jsnippet.ui.JSnippetTextArea;
import org.knime.base.node.jsnippet.ui.JarListPanel;
import org.knime.base.node.jsnippet.ui.OutFieldsTable;
import org.knime.base.node.jsnippet.ui.OutFieldsTableModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;


/**
 * The dialog of the java snippet node.
 *
 * @author Heiko Hofer
 */
public class JavaSnippetNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            JavaSnippetNodeDialog.class);

    private static final String SNIPPET_TAB = "Java Snippet";

    private JSnippetTextArea m_snippetTextArea;
    /** Component with a list of all input columns. */
    protected ColumnList m_colList;
    /** Component with a list of all input flow variables. */
    protected FlowVariableList m_flowVarsList;

    private JavaSnippetSettings m_settings;
    private JavaSnippet m_snippet;
    private InFieldsTable m_inFieldsTable;
    private OutFieldsTable m_outFieldsTable;
    private JSnippetFieldsController m_fieldsController;

    private JarListPanel m_jarPanel;

    private DefaultTemplateController m_templatesController;
    private boolean m_isEnabled;

    private File[] m_autoCompletionJars;

    @SuppressWarnings("rawtypes")
    private Class m_templateMetaCategory;
    private JLabel m_templateLocation;


    /**
     * Create a new Dialog.
     * @param templateMetaCategory the meta category used in the templates
     * tab or to create templates
     */
    @SuppressWarnings("rawtypes")
    public JavaSnippetNodeDialog(final Class templateMetaCategory) {
        this(templateMetaCategory, false);
    }

    /**
     * Create a new Dialog.
     * @param templateMetaCategory the meta category used in the templates
     * tab or to create templates
     * @param isPreview if this is a preview used for showing templates.
     */
    @SuppressWarnings("rawtypes")
    private JavaSnippetNodeDialog(final Class templateMetaCategory,
            final boolean isPreview) {
        m_templateMetaCategory = templateMetaCategory;
        m_settings = new JavaSnippetSettings();
        m_snippet = new JavaSnippet();
        JPanel panel = createPanel(isPreview);
        m_fieldsController = new JSnippetFieldsController(m_snippet,
                m_inFieldsTable, m_outFieldsTable);
        m_colList.install(m_snippetTextArea);
        m_colList.install(m_fieldsController);
        m_flowVarsList.install(m_snippetTextArea);
        m_flowVarsList.install(m_fieldsController);
        addTab(SNIPPET_TAB, panel);
        if (!isPreview) {
            panel.setPreferredSize(new Dimension(800, 600));
        }
        addTab("Additional Libraries", createJarPanel());
        if (!isPreview) {
            // The preview does not have the templates tab
            addTab("Templates", createTemplatesPanel());
        }
        m_isEnabled = true;
        setEnabled(!isPreview);
        m_outFieldsTable.addPropertyChangeListener(
                OutFieldsTable.PROP_FIELD_ADDED, new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                // add write statement to the snippet
                OutFieldsTableModel model =
                    (OutFieldsTableModel)m_outFieldsTable.getTable().getModel();
                String javaName = (String)model.getValueAt(
                        model.getRowCount() - 1,
                        Column.JAVA_FIELD);
                String enter = "\n" + javaName + " = ";
                if (null != m_snippetTextArea) {
                    GuardedDocument doc = m_snippet.getDocument();
                    int min = doc.getGuardedSection(
                            JavaSnippetDocument.GUARDED_BODY_START).getEnd().
                            getOffset() + 1;
                    int pos = doc.getGuardedSection(
                            JavaSnippetDocument.GUARDED_BODY_END).getStart().
                            getOffset() - 1;
                    try {
                        while (doc.getText(pos, 1).equals("\n")
                               && doc.getText(pos - 1, 1).equals("\n")
                               && pos > min) {
                            pos--;
                        }
                    } catch (BadLocationException e) {
                        // do nothing, not critical
                    }
                    m_snippetTextArea.setCaretPosition(pos);
                    m_snippetTextArea.replaceSelection(enter);
                    m_snippetTextArea.requestFocus();
                }
            }
        });
    }

    private JPanel createPanel(final boolean isPreview) {
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

        // use split pane for fields
        m_inFieldsTable.setBorder(BorderFactory.createTitledBorder("Input"));
        m_outFieldsTable.setBorder(BorderFactory.createTitledBorder("Output"));
        JSplitPane fieldsPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        fieldsPane.setTopComponent(m_inFieldsTable);
        fieldsPane.setBottomComponent(m_outFieldsTable);
        fieldsPane.setOneTouchExpandable(true);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(centerSplitPane);
        // minimize size of tables at the bottom
        fieldsPane.setPreferredSize(fieldsPane.getMinimumSize());
        mainSplitPane.setBottomComponent(fieldsPane);
        mainSplitPane.setOneTouchExpandable(true);

        centerPanel.add(mainSplitPane);

        p.add(centerPanel, BorderLayout.CENTER);
        JPanel templateInfoPanel = createTemplateInfoPanel(isPreview);
        p.add(templateInfoPanel, BorderLayout.NORTH);
        return p;
    }


    /**
     * The panel at the to with the "Create Template..." Button.
     */
    private JPanel createTemplateInfoPanel(final boolean isPreview) {
        final JButton addTemplateButton = new JButton("Create Template...");
        addTemplateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Frame parent = (Frame)SwingUtilities.getAncestorOfClass(
                        Frame.class, addTemplateButton);
                JSnippetTemplate newTemplate =
                    AddTemplateDialog.openUserDialog(
                        parent, m_snippet,
                        m_templateMetaCategory);
                if (null != newTemplate) {
                    TemplateProvider.getDefault().addTemplate(newTemplate);
                    // update the template UUID of the current snippet
                    m_settings.setTemplateUUID(newTemplate.getUUID());
                    String loc = TemplateProvider.getDefault().
                        getDisplayLocation(newTemplate);
                    m_templateLocation.setText(loc);
                    JavaSnippetNodeDialog.this.getPanel().validate();
                }
            }
        });
        JPanel templateInfoPanel = new JPanel(new BorderLayout());
        TemplateProvider provider = TemplateProvider.getDefault();
        String uuid = m_settings.getTemplateUUID();
        JSnippetTemplate template = null != uuid ? provider.getTemplate(
                UUID.fromString(uuid)) : null;
        String loc = null != template
                ? createTemplateLocationText(template)
                : "";
        m_templateLocation = new JLabel(loc);
        if (isPreview) {
            templateInfoPanel.add(m_templateLocation, BorderLayout.CENTER);
        } else {
            templateInfoPanel.add(addTemplateButton, BorderLayout.LINE_END);
        }
        templateInfoPanel.setBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return templateInfoPanel;
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
                // update autocompletion
                updateAutocompletion();
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

    /** Create the templates tab. */
    private JPanel createTemplatesPanel() {
        JavaSnippetNodeDialog preview = createPreview();

        m_templatesController = new DefaultTemplateController(
                this, preview);
        TemplatesPanel templatesPanel = new TemplatesPanel(
                Collections.singleton(m_templateMetaCategory),
                m_templatesController);
        return templatesPanel;
    }

    /**
     * Create a non editable preview to be used to display a template. This
     * method is typically overriden by subclasses.
     * @return a new instance prpared to display a preview.
     */
    protected JavaSnippetNodeDialog createPreview() {
        return new JavaSnippetNodeDialog(m_templateMetaCategory, true);
    }

    /**
     * Create table do display the input fields.
     * @return the table
     */
    protected InFieldsTable createInFieldsTable() {
        InFieldsTable table = new InFieldsTable();
        return table;
    }

    /**
     * Create table do display the ouput fields.
     * @return the table
     */
    protected OutFieldsTable createOutFieldsTable() {
        OutFieldsTable table = new OutFieldsTable(false);
        FieldsTableModel model = (FieldsTableModel)table.getTable().getModel();
        table.getTable().getColumnModel().getColumn(model.getIndex(
                Column.FIELD_TYPE)).setPreferredWidth(30);
        table.getTable().getColumnModel().getColumn(model.getIndex(
                Column.REPLACE_EXISTING)).setPreferredWidth(15);
        table.getTable().getColumnModel().getColumn(model.getIndex(
                Column.IS_COLLECTION)).setPreferredWidth(15);
        return table;
    }

    /**
     * Create the panel with the snippet.
     */
    private JComponent createSnippetPanel() {
        updateAutocompletion();

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
     * The panel at the left with the column and variables at the input.
     * Override this method when the columns are variables should not be
     * displayed.
     * @return the panel at the left with the column and variables at the
     * input.
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

    private void updateAutocompletion() {
        LanguageSupportFactory lsf = LanguageSupportFactory.get();
        LanguageSupport support = lsf.getSupportFor(
                org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JAVA);
        JavaLanguageSupport jls = (JavaLanguageSupport)support;
        JarManager jarManager = jls.getJarManager();

        try {
            boolean doUpdate = false;
            if (null == m_autoCompletionJars) {
                m_autoCompletionJars = m_snippet.getClassPath();
                doUpdate = true;
            } else {
                if (!Arrays.equals(m_autoCompletionJars, m_snippet.getClassPath())) {
                    m_autoCompletionJars = m_snippet.getClassPath();
                    doUpdate = true;
                }
            }

            if (doUpdate) {
                jarManager.clearClassFileSources();
                jarManager.addCurrentJreClassFileSource();
                for (File jarFile : m_autoCompletionJars) {
                    jarManager.addClassFileSource(jarFile);
                }
            }


        } catch (IOException ioe) {
            LOGGER.error(ioe.getMessage(), ioe);
        }

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
     * Determines whether this component is enabled. An enabled component
     * can respond to user input and generate events.
     * @return <code>true</code> if the component is enabled,
     *          <code>false</code> otherwise
     */
    public boolean isEnabled() {
        return m_isEnabled;
    }

    /**
     * Sets whether or not this component is enabled.
     * A component that is enabled may respond to user input,
     * while a component that is not enabled cannot respond to
     * user input.
     * @param enabled true if this component should be enabled, false otherwise
     */
    public void setEnabled(final boolean enabled) {
        if (m_isEnabled != enabled) {
            m_colList.setEnabled(enabled);
            m_flowVarsList.setEnabled(enabled);
            m_inFieldsTable.setEnabled(enabled);
            m_outFieldsTable.setEnabled(enabled);
            m_jarPanel.setEnabled(enabled);
            m_snippetTextArea.setEnabled(enabled);
        }
        m_isEnabled = enabled;

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
                JavaSnippetDocument.GUARDED_BODY_START).getEnd().getOffset()
                + 1);
        m_snippetTextArea.requestFocusInWindow();

        m_templatesController.setDataTableSpec(specs[0]);
        m_templatesController.setFlowVariables(getAvailableFlowVariables());

        // update template info panel
        TemplateProvider provider = TemplateProvider.getDefault();
        String uuid = m_settings.getTemplateUUID();
        JSnippetTemplate template = null != uuid ? provider.getTemplate(
                UUID.fromString(uuid)) : null;
        String loc = null != template ? createTemplateLocationText(template)
                : "";
        m_templateLocation.setText(loc);
    }

    /**
     * Reinitialize with the given blueprint.
     * @param template the template
     * @param flowVariables the flow variables at the input
     * @param spec the input spec
     */
    public void applyTemplate(final JSnippetTemplate template,
            final DataTableSpec spec,
            final Map<String, FlowVariable> flowVariables) {
        // save and read settings to decouple objects.
        NodeSettings settings = new NodeSettings(template.getUUID());
        template.getSnippetSettings().saveSettings(settings);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            settings.saveToXML(os);
            NodeSettingsRO settingsro = NodeSettings.loadFromXML(
                    new ByteArrayInputStream(
                            os.toString("UTF-8").getBytes("UTF-8")));
            m_settings.loadSettings(settingsro);
        } catch (Exception e) {
            LOGGER.error("Cannot apply template.", e);
        }

        m_colList.setSpec(spec);
        m_flowVarsList.setFlowVariables(flowVariables.values());
        m_snippet.setSettings(m_settings);
        m_jarPanel.setJarFiles(m_settings.getJarFiles());

        m_fieldsController.updateData(m_settings, spec,
                flowVariables);
        // update template info panel
        m_templateLocation.setText(createTemplateLocationText(template));

        setSelected(SNIPPET_TAB);
        // set caret position to the start of the custom expression
        m_snippetTextArea.setCaretPosition(
                m_snippet.getDocument().getGuardedSection(
                JavaSnippetDocument.GUARDED_BODY_START).getEnd().getOffset()
                + 1);
        m_snippetTextArea.requestFocus();

    }

    /**
     * Get the template's location for display.
     * @param template the template
     * @return the template's loacation for display
     */
    private String createTemplateLocationText(final JSnippetTemplate template) {
        TemplateProvider provider = TemplateProvider.getDefault();
        return provider.getDisplayLocation(template);
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
        ViewUtils.invokeAndWaitInEDT(new Runnable() {

            @Override
            public void run() {
                // Commit editing - This is a workaround for a bug in the Dialog
                // since the tables do not loose focus when OK or Apply is
                // pressed.
                if (null != m_inFieldsTable.getTable().getCellEditor()) {
                    m_inFieldsTable.getTable().getCellEditor().
                    stopCellEditing();
                }
                if (null != m_outFieldsTable.getTable().getCellEditor()) {
                    m_outFieldsTable.getTable().getCellEditor().
                    stopCellEditing();
                }
            }
        });
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
