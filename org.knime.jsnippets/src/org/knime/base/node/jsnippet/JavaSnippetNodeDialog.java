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
 *   24.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;

import org.eclipse.core.runtime.Platform;
import org.fife.rsta.ac.LanguageSupport;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.rsta.ac.java.JarManager;
import org.fife.rsta.ac.java.JavaLanguageSupport;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.base.node.jsnippet.guarded.JavaSnippetDocument;
import org.knime.base.node.jsnippet.template.AddTemplateDialog;
import org.knime.base.node.jsnippet.template.DefaultTemplateController;
import org.knime.base.node.jsnippet.template.JavaSnippetTemplate;
import org.knime.base.node.jsnippet.template.JavaSnippetTemplateProvider;
import org.knime.base.node.jsnippet.template.TemplateNodeDialog;
import org.knime.base.node.jsnippet.template.TemplateProvider;
import org.knime.base.node.jsnippet.template.TemplatesPanel;
import org.knime.base.node.jsnippet.ui.BundleListPanel;
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
import org.knime.base.node.jsnippet.util.JavaSnippetSettings;
import org.knime.base.node.jsnippet.util.field.JavaColumnField;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.util.rsyntaxtextarea.guarded.GuardedDocument;
import org.knime.core.node.workflow.FlowVariable;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * The dialog of the java snippet node.
 *
 * @author Heiko Hofer
 */
public class JavaSnippetNodeDialog extends NodeDialogPane implements TemplateNodeDialog<JavaSnippetTemplate> {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JavaSnippetNodeDialog.class);

    private static final String SNIPPET_TAB_NAME = "Java Snippet";

    private static final String ADDITIONAL_BUNDLES_TAB_NAME = "Additional Bundles";

    private JSnippetTextArea m_snippetTextArea;

    /** Component with a list of all input columns. */
    protected ColumnList m_colList;

    /** Component with a list of all input flow variables. */
    protected FlowVariableList m_flowVarsList;

    /** The settings. */
    protected JavaSnippetSettings m_settings;

    private JavaSnippet m_snippet;

    private InFieldsTable m_inFieldsTable;

    private OutFieldsTable m_outFieldsTable;

    private JSnippetFieldsController m_fieldsController;

    private JarListPanel m_jarPanel;

    private BundleListPanel m_bundleListPanel;

    private DefaultTemplateController<JavaSnippetTemplate> m_templatesController;

    private boolean m_isEnabled;

    private File[] m_autoCompletionJars;

    /** The templates category for templates viewed or edited by this dialog. */
    protected Class<?> m_templateMetaCategory;

    private JLabel m_templateLocation;

    private ErrorStrip m_errorStrip = null;


    /**
     * Create a new Dialog.
     *
     * @param templateMetaCategory the meta category used in the templates tab or to create templates
     */
    public JavaSnippetNodeDialog(final Class<?> templateMetaCategory) {
        this(templateMetaCategory, false);
    }

    /**
     * Create a new Dialog.
     *
     * @param templateMetaCategory the meta category used in the templates tab or to create templates
     * @param isPreview if this is a preview used for showing templates.
     */
    protected JavaSnippetNodeDialog(final Class<?> templateMetaCategory, final boolean isPreview) {
        m_templateMetaCategory = templateMetaCategory;
        m_settings = new JavaSnippetSettings();
        m_snippet = new JavaSnippet();

        final JPanel panel = createPanel(isPreview);
        m_fieldsController = new JSnippetFieldsController(m_snippet, m_inFieldsTable, m_outFieldsTable);
        m_colList.install(m_snippetTextArea);
        m_colList.install(m_fieldsController);
        m_flowVarsList.install(m_snippetTextArea);
        m_flowVarsList.install(m_fieldsController);

        addTab(SNIPPET_TAB_NAME, panel);
        if (!isPreview) {
            panel.setPreferredSize(new Dimension(800, 600));
        }

        addTab("Additional Libraries", createAdditionalLibsPanel());

        m_bundleListPanel = new BundleListPanel();
        m_bundleListPanel.getListModel().addListDataListener(forceReparseListener);
        addTab(ADDITIONAL_BUNDLES_TAB_NAME, m_bundleListPanel);

        if (!isPreview) {
            // The preview does not have the templates tab
            addTab("Templates", createTemplatesPanel());
        }
        m_isEnabled = true;
        setEnabled(!isPreview);
        m_outFieldsTable.addPropertyChangeListener(OutFieldsTable.PROP_FIELD_ADDED, evt -> {
            // add write statement to the snippet
            final OutFieldsTableModel model = (OutFieldsTableModel)m_outFieldsTable.getTable().getModel();
            final String javaName = (String)model.getValueAt(model.getRowCount() - 1, Column.JAVA_FIELD);
            final String enter = "\n" + javaName + " = ";
            if (null != m_snippetTextArea) {
                final GuardedDocument doc = m_snippet.getDocument();
                final int min = doc.getGuardedSection(JavaSnippetDocument.GUARDED_BODY_START).getEnd().getOffset() + 1;
                int pos = doc.getGuardedSection(JavaSnippetDocument.GUARDED_BODY_END).getStart().getOffset() - 1;
                try {
                    while (doc.charAt(pos) == '\n' && doc.charAt(pos - 1) == '\n' && pos > min) {
                        pos--;
                    }
                } catch (BadLocationException e) {
                    // do nothing, not critical
                }
                m_snippetTextArea.setCaretPosition(pos);
                m_snippetTextArea.replaceSelection(enter);
                m_snippetTextArea.requestFocus();
            }
        });
    }

    private JPanel createPanel(final boolean isPreview) {
        final JPanel p = new JPanel(new BorderLayout());

        final JComponent snippet = createSnippetPanel();
        final JComponent colsAndVars = createColsAndVarsPanel();

        final JPanel centerPanel = new JPanel(new GridLayout(0, 1));
        final JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplitPane.setLeftComponent(colsAndVars);
        centerSplitPane.setRightComponent(snippet);
        centerSplitPane.setResizeWeight(0.3); // colsAndVars expands to 0.3, the snippet to 0.7

        m_inFieldsTable = createInFieldsTable();
        m_outFieldsTable = createOutFieldsTable();
        final TableModelListener tableModelListener = e -> {
            if (e.getType() != TableModelEvent.DELETE) {
                // Updating completion can result in a small delay,
                // so we don't update when fields are removed.
                updateAutocompletion();
            }

            updateCustomTypesBundles();
        };
        m_inFieldsTable.getTable().getModel().addTableModelListener(tableModelListener);
        m_outFieldsTable.getTable().getModel().addTableModelListener(tableModelListener);

        // use split pane for fields
        m_inFieldsTable.setBorder(BorderFactory.createTitledBorder("Input"));
        m_outFieldsTable.setBorder(BorderFactory.createTitledBorder("Output"));

        final JSplitPane fieldsPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        fieldsPane.setTopComponent(m_inFieldsTable);
        fieldsPane.setBottomComponent(m_outFieldsTable);
        fieldsPane.setOneTouchExpandable(true);

        final JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(centerSplitPane);
        // minimize size of tables at the bottom
        fieldsPane.setPreferredSize(fieldsPane.getMinimumSize());
        mainSplitPane.setBottomComponent(fieldsPane);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setResizeWeight(0.7); // snippet gets more space, table with in/out gets less extra space

        centerPanel.add(mainSplitPane);

        p.add(centerPanel, BorderLayout.CENTER);
        final JPanel templateInfoPanel = createTemplateInfoPanel(isPreview);
        p.add(templateInfoPanel, BorderLayout.NORTH);
        final JPanel optionsPanel = createOptionsPanel();
        if (optionsPanel != null) {
            p.add(optionsPanel, BorderLayout.SOUTH);
        }
        return p;
    }

    /**
     * The panel at the to with the "Create Template..." Button.
     */
    private JPanel createTemplateInfoPanel(final boolean isPreview) {
        final JButton addTemplateButton = new JButton("Create Template...");
        addTemplateButton.addActionListener(e -> {
            final Frame parent = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, addTemplateButton);
            final JavaSnippetTemplate newTemplate = AddTemplateDialog.openUserDialog(parent, m_snippet,
                m_templateMetaCategory, JavaSnippetTemplateProvider.getDefault());

            if (null != newTemplate) {
                JavaSnippetTemplateProvider.getDefault().addTemplate(newTemplate);
                // update the template UUID of the current snippet
                m_settings.setTemplateUUID(newTemplate.getUUID());
                final String loc = JavaSnippetTemplateProvider.getDefault().getDisplayLocation(newTemplate);
                m_templateLocation.setText(loc);
                JavaSnippetNodeDialog.this.getPanel().validate();
            }
        });
        final JPanel templateInfoPanel = new JPanel(new BorderLayout());

        m_templateLocation = new JLabel(getTemplateLocation());
        if (isPreview) {
            templateInfoPanel.add(m_templateLocation, BorderLayout.CENTER);
        } else {
            templateInfoPanel.add(addTemplateButton, BorderLayout.LINE_END);
        }
        templateInfoPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return templateInfoPanel;
    }

    /* Get the location of the template set in the settings. (empty string if none set) */
    private String getTemplateLocation() {
        final TemplateProvider<JavaSnippetTemplate> provider = JavaSnippetTemplateProvider.getDefault();
        final String uuid = m_settings.getTemplateUUID();
        final JavaSnippetTemplate template = null != uuid ? provider.getTemplate(UUID.fromString(uuid)) : null;
        final String loc = null != template ? createTemplateLocationText(template) : "";

        return loc;
    }

    final ListDataListener forceReparseListener = new ListDataListener() {
        private void updateSnippet() {
            m_snippet.setJarFiles(m_jarPanel.getJarFiles());
            m_snippet.setAdditionalBundles(m_bundleListPanel.getBundles());

            // force reparsing of the snippet
            for (int i = 0; i < m_snippetTextArea.getParserCount(); i++) {
                m_snippetTextArea.forceReparsing(i);
            }

            // update autocompletion
            updateAutocompletion();

            // update error strip:
            // HACK: This forces refreshMarkers(), which is not triggered by reparsing of the document for some reason.
            // We are just re-setting the default again here.
            m_errorStrip.setLevelThreshold(Level.WARNING);
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
    };

    private JPanel createAdditionalLibsPanel() {
        m_jarPanel = new JarListPanel();
        m_jarPanel.addListDataListener(forceReparseListener);
        return m_jarPanel;
    }

    /** Create the templates tab. */
    private JPanel createTemplatesPanel() {
        final JavaSnippetNodeDialog preview = createPreview();

        m_templatesController = new DefaultTemplateController<>(this, preview);
        final TemplatesPanel<JavaSnippetTemplate> templatesPanel =
            new TemplatesPanel<>(Collections.singletonList(m_templateMetaCategory), m_templatesController,
                JavaSnippetTemplateProvider.getDefault());
        return templatesPanel;
    }

    /**
     * Create a non editable preview to be used to display a template. This method is typically overridden by
     * subclasses.
     *
     * @return a new instance prepared to display a preview.
     */
    protected JavaSnippetNodeDialog createPreview() {
        return new JavaSnippetNodeDialog(m_templateMetaCategory, true);
    }

    /**
     * Create table do display the input fields.
     *
     * @return the table
     */
    protected InFieldsTable createInFieldsTable() {
        return new InFieldsTable();
    }

    /**
     * Create table do display the output fields.
     *
     * @return the table
     */
    protected OutFieldsTable createOutFieldsTable() {
        final OutFieldsTable table = new OutFieldsTable(false);
        final FieldsTableModel model = (FieldsTableModel)table.getTable().getModel();

        final TableColumnModel columnModel = table.getTable().getColumnModel();
        columnModel.getColumn(model.getIndex(Column.FIELD_TYPE)).setPreferredWidth(30);
        columnModel.getColumn(model.getIndex(Column.REPLACE_EXISTING)).setPreferredWidth(15);
        columnModel.getColumn(model.getIndex(Column.IS_COLLECTION)).setPreferredWidth(15);

        return table;
    }

    /**
     * Create the panel with the snippet.
     */
    private JComponent createSnippetPanel() {
        m_snippetTextArea = new JSnippetTextArea(m_snippet);
        m_errorStrip = new ErrorStrip(m_snippetTextArea);

        // reset style which causes a recreation of the folds
        // this code is also executed in "onOpen" but that is not called for the template viewer tab
        m_snippetTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        m_snippetTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);

        collapseAllFolds();

        final JScrollPane snippetScroller = new RTextScrollPane(m_snippetTextArea);
        final JPanel snippet = new JPanel(new BorderLayout());
        snippet.add(snippetScroller, BorderLayout.CENTER);

        snippet.add(m_errorStrip, BorderLayout.LINE_END);

        return snippet;
    }

    /**
     * The panel at the left with the column and variables at the input. Override this method when the columns are
     * variables should not be displayed.
     *
     * @return the panel at the left with the column and variables at the input.
     */
    protected JComponent createColsAndVarsPanel() {
        final JSplitPane varSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        m_colList = new ColumnList();

        final JScrollPane colListScroller = new JScrollPane(m_colList);
        colListScroller.setBorder(createEmptyTitledBorder("Column List"));
        varSplitPane.setTopComponent(colListScroller);

        // set variable panel
        m_flowVarsList = new FlowVariableList();
        final JScrollPane flowVarScroller = new JScrollPane(m_flowVarsList);
        flowVarScroller.setBorder(createEmptyTitledBorder("Flow Variable List"));
        varSplitPane.setBottomComponent(flowVarScroller);
        varSplitPane.setOneTouchExpandable(true);
        varSplitPane.setResizeWeight(0.9);

        return varSplitPane;
    }

    /**
     * Create Panel with additional options to be displayed in the south.
     *
     * @return options panel or null if there are no additional options.
     */
    protected JPanel createOptionsPanel() {
        return null;
    }

    private void updateAutocompletion() {
        try {
            if (m_autoCompletionJars == null || !Arrays.stream(m_autoCompletionJars).allMatch(file -> file.exists())
                || !Arrays.equals(m_autoCompletionJars, m_snippet.getCompiletimeClassPath())) {

                final LanguageSupportFactory lsf = LanguageSupportFactory.get();
                final LanguageSupport support =
                    lsf.getSupportFor(org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JAVA);
                final JavaLanguageSupport jls = (JavaLanguageSupport)support;
                final JarManager jarManager = jls.getJarManager();

                m_autoCompletionJars = m_snippet.getCompiletimeClassPath();
                jarManager.clearClassFileSources();
                jarManager.addCurrentJreClassFileSource();
                for (final File jarFile : m_autoCompletionJars) {
                    if (!jarFile.getName().endsWith(".jar")) {
                        continue;
                    }
                    jarManager.addClassFileSource(jarFile);
                }
            }
        } catch (IOException ioe) {
            LOGGER.error(ioe.getMessage(), ioe);
        }

    }

    /**
     * Create an empty, titled border.
     *
     * @param string Title of the border.
     * @return Such a new border.
     */
    protected Border createEmptyTitledBorder(final String string) {
        return BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), string,
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP);
    }

    /**
     * Determines whether this component is enabled. An enabled component can respond to user input and generate events.
     *
     * @return <code>true</code> if the component is enabled, <code>false</code> otherwise
     */
    public boolean isEnabled() {
        return m_isEnabled;
    }

    /**
     * Sets whether or not this component is enabled. A component that is enabled may respond to user input, while a
     * component that is not enabled cannot respond to user input.
     *
     * @param enabled true if this component should be enabled, false otherwise
     */
    protected void setEnabled(final boolean enabled) {
        if (m_isEnabled != enabled) {
            m_colList.setEnabled(enabled);
            m_flowVarsList.setEnabled(enabled);
            m_inFieldsTable.setEnabled(enabled);
            m_outFieldsTable.setEnabled(enabled);
            m_jarPanel.setEnabled(enabled);
            m_bundleListPanel.setEnabled(enabled);
            m_snippetTextArea.setEnabled(enabled);

            m_isEnabled = enabled;
        }
    }

    @Override
    public boolean closeOnESC() {
        // do not close on ESC, since ESC is used to close autocomplete popups
        // in the snippets textarea.
        return false;
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        ViewUtils.invokeAndWaitInEDT(() -> loadSettingsFromInternal(settings, specs));
    }

    /**
     * Load settings invoked from the EDT-Thread.
     *
     * @param settings the settings to load
     * @param specs the specs of the input table
     */
    protected void loadSettingsFromInternal(final NodeSettingsRO settings, final DataTableSpec[] specs) {
        m_settings.loadSettingsForDialog(settings);

        m_colList.setSpec(specs[0]);
        m_flowVarsList.setFlowVariables(getAvailableFlowVariables().values());
        m_snippet.setSettings(m_settings);
        m_jarPanel.setJarFiles(m_settings.getJarFiles());
        m_bundleListPanel.setBundles(m_settings.getBundles());
        updateCustomTypesBundles();

        m_fieldsController.updateData(m_settings, specs[0], getAvailableFlowVariables());

        // set caret position to the start of the custom expression
        m_snippetTextArea.setCaretPosition(
            m_snippet.getDocument().getGuardedSection(JavaSnippetDocument.GUARDED_BODY_START).getEnd().getOffset() + 1);
        m_snippetTextArea.requestFocusInWindow();

        m_templatesController.setDataTableSpec(specs[0]);
        m_templatesController.setFlowVariables(getAvailableFlowVariables());

        // update template info panel
        m_templateLocation.setText(getTemplateLocation());

        updateAutocompletion();

        try {
            /* Update "Additional Bundles" tab title */
            validateBundlesSetting();
        } catch (InvalidSettingsException e) {
            /* This is not a problem, we only want the "Additional Bundles" tab title to be updated appropriately */
        }
    }

    /**
     * Reinitialize with the given blueprint.
     *
     * @param template the template
     * @param flowVariables the flow variables at the input
     * @param spec the input spec
     */
    @Override
    public void applyTemplate(final JavaSnippetTemplate template, final DataTableSpec spec,
        final Map<String, FlowVariable> flowVariables) {
        // save and read settings to decouple objects.
        final NodeSettings settings = new NodeSettings(template.getUUID());
        template.getSnippetSettings().saveSettings(settings);

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            settings.saveToXML(os);
            final NodeSettingsRO settingsro =
                NodeSettings.loadFromXML(new ByteArrayInputStream(os.toString("UTF-8").getBytes("UTF-8")));
            m_settings.loadSettings(settingsro);
        } catch (Exception e) {
            LOGGER.error("Cannot apply template.", e);
        }

        m_colList.setSpec(spec);
        m_flowVarsList.setFlowVariables(flowVariables.values());
        m_snippet.setSettings(m_settings);
        m_jarPanel.setJarFiles(m_settings.getJarFiles());
        m_bundleListPanel.setBundles(m_settings.getBundles());

        updateCustomTypesBundles();

        m_fieldsController.updateData(m_settings, spec, flowVariables);
        // update template info panel
        m_templateLocation.setText(createTemplateLocationText(template));

        setSelected(SNIPPET_TAB_NAME);
        // set caret position to the start of the custom expression
        m_snippetTextArea.setCaretPosition(
            m_snippet.getDocument().getGuardedSection(JavaSnippetDocument.GUARDED_BODY_START).getEnd().getOffset() + 1);
        m_snippetTextArea.requestFocus();

    }

    /**
     * Update bundles included with custom types
     */
    private void updateCustomTypesBundles() {
        final ArrayList<Bundle> bundles = new ArrayList<>();
        for (final JavaColumnField f : m_inFieldsTable.getInColFields()) {
            final Bundle b = JavaSnippet.resolveBundleForJavaType(f.getJavaType());
            if(b != null) {
                bundles.add(b);
            }
        }
        for (final JavaColumnField f : m_outFieldsTable.getOutColFields()) {
            final Bundle b = JavaSnippet.resolveBundleForJavaType(f.getJavaType());
            if(b != null) {
                bundles.add(b);
            }
        }
        m_bundleListPanel.setCustomTypeBundles(bundles);
    }

    /**
     * Get the template's location for display.
     *
     * @param template the template
     * @return the template's location for display
     */
    private String createTemplateLocationText(final JavaSnippetTemplate template) {
        final TemplateProvider<JavaSnippetTemplate> provider = JavaSnippetTemplateProvider.getDefault();
        return provider.getDisplayLocation(template);
    }

    @Override
    public void onOpen() {
        m_snippetTextArea.requestFocus();
        m_snippetTextArea.requestFocusInWindow();
        // reset style which causes a recreation of the popup window with
        // the side effect, that all folds are recreated, so that we must collapse
        // them next (bug 4061)
        m_snippetTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        m_snippetTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);

        collapseAllFolds();
    }

    /* Collapse all folds */
    private void collapseAllFolds() {
        final FoldManager foldManager = m_snippetTextArea.getFoldManager();
        final int foldCount = foldManager.getFoldCount();
        for (int i = 0; i < foldCount; i++) {
            final Fold fold = foldManager.getFold(i);
            fold.setCollapsed(true);
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ViewUtils.invokeAndWaitInEDT(() -> {
            // Commit editing - This is a workaround for a bug in the Dialog
            // since the tables do not loose focus when OK or Apply is
            // pressed.
            if (null != m_inFieldsTable.getTable().getCellEditor()) {
                m_inFieldsTable.getTable().getCellEditor().stopCellEditing();
            }
            if (null != m_outFieldsTable.getTable().getCellEditor()) {
                m_outFieldsTable.getTable().getCellEditor().stopCellEditing();
            }
        });
        final JavaSnippetSettings s = m_snippet.getSettings();

        // if settings have less fields than defined in the table it means
        // that the tables contain errors
        final FieldsTableModel inFieldsModel = (FieldsTableModel)m_inFieldsTable.getTable().getModel();
        if (!inFieldsModel.validateValues()) {
            throw new IllegalArgumentException("The input fields table has errors.");
        }
        final FieldsTableModel outFieldsModel = (FieldsTableModel)m_outFieldsTable.getTable().getModel();
        if (!outFieldsModel.validateValues()) {
            throw new IllegalArgumentException("The output fields table has errors.");
        }

        s.setBundles(m_bundleListPanel.getBundles());
        validateBundlesSetting();

        // give subclasses the chance to modify settings
        preSaveSettings(s);
        s.saveSettings(settings);
    }

    private void validateBundlesSetting() throws InvalidSettingsException {
        // Check additional bundles
        for (final String bundleString : m_snippet.getSettings().getBundles()) {
            final String[] split = bundleString.split(" ");
            final String bundleName = split[0];
            if (split.length <= 1) {
                setAdditionalBundlesTabTitle(false);
                throw new InvalidSettingsException(String.format("Missing version for bundle \"%s\" in settings", bundleName));
            }

            final Bundle[] bundles = Platform.getBundles(bundleName, null);
            if (bundles == null) {
                setAdditionalBundlesTabTitle(false);
                throw new InvalidSettingsException("Bundle \"" + bundleName + "\" required by this snippet was not found.");
            }

            boolean bundleFound = false;
            final Version savedVersion = Version.parseVersion(split[1]);
            for (final Bundle bundle : bundles) {
                final Version installedVersion = bundle.getVersion();

                if (JavaSnippet.versionMatches(installedVersion, savedVersion)) {
                    bundleFound = true;
                    break;
                }
            }

            if (!bundleFound) {
                setAdditionalBundlesTabTitle(false);
                throw new InvalidSettingsException(
                    String.format("No installed version of \"%s\" matched version range [%s, %d.0.0).", bundleName,
                        savedVersion, savedVersion.getMajor() + 1));
            }
        }
        setAdditionalBundlesTabTitle(true);
    }

    /**
     * Called right before storing the settings object. Gives subclasses the chance to modify the settings object.
     *
     * @param s the settings
     */
    protected void preSaveSettings(final JavaSnippetSettings s) {
        // just a place holder.
    }

    /*
     * Set the title of "Additional Bundles" panel so that it contains " <!>" if settings are invalid.
     */
    private void setAdditionalBundlesTabTitle(final boolean bundleSettingsValid) {
        /* Get the tab assuming it displays that bundle Settings are valid. If not, tab will be null. */
        final Component tab = getTab(ADDITIONAL_BUNDLES_TAB_NAME);

        /* tab != null is equivalent to it displaying that settings are valid */
        if(bundleSettingsValid != (tab != null)) {
            /* State changed, set the title accordingly */
            final String oldName = ADDITIONAL_BUNDLES_TAB_NAME + (!bundleSettingsValid ? "" : " <!>");
            final String newName = ADDITIONAL_BUNDLES_TAB_NAME + (bundleSettingsValid ? "" : " <!>");
            renameTab(oldName, newName);
        }
    }
}
