/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 16, 2015 (albrecht): created
 */
package org.knime.workbench.editor2.subnode;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.layout.DefaultLayoutCreatorImpl;
import org.knime.js.core.layout.bs.JSONLayoutColumn;
import org.knime.js.core.layout.bs.JSONLayoutContent;
import org.knime.js.core.layout.bs.JSONLayoutPage;
import org.knime.js.core.layout.bs.JSONLayoutRow;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public class SubnodeLayoutJSONEditorPage extends WizardPage {

    private static NodeLogger LOGGER = NodeLogger.getLogger(SubnodeLayoutJSONEditorPage.class);

    private SubNodeContainer m_subNodeContainer;
    private WorkflowManager m_wfManager;
    private DefaultLayoutCreatorImpl m_layoutCreator;
    @SuppressWarnings("rawtypes")
    private Map<NodeIDSuffix, WizardNode> m_viewNodes;
    private String m_jsonDocument;
    private Label m_statusLine;
    private RSyntaxTextArea m_textArea;
    private int m_caretPosition = 5;
    private Text m_text;
    private List<Integer> m_documentNodeIDs = new ArrayList<Integer>();
    private boolean m_basicPanelAvailable = true;
    private final Map<NodeIDSuffix, BasicLayoutInfo> m_basicMap;
    private Composite m_basicComposite;

    private NodeUsageComposite m_nodeUsageComposite;

    /**
     * Crates a new page instance with a given page name
     * @param pageName the page name
     */
    protected SubnodeLayoutJSONEditorPage(final String pageName) {
        super(pageName);
        setDescription("Specifiy in what way the contained View and Quickform nodes are allowed to be used and define a layout. \nThe layout is used in the KNIME WebPortal and the Wrapped Metanode View.");
        m_jsonDocument = "";
        m_basicMap = new LinkedHashMap<NodeIDSuffix, BasicLayoutInfo>();
        m_layoutCreator = new DefaultLayoutCreatorImpl();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        TabFolder tabs = new TabFolder(parent, SWT.BORDER);

        TabItem usageTab = new TabItem(tabs, SWT.NONE);
        usageTab.setText("Node Usage");
        m_nodeUsageComposite = new NodeUsageComposite(tabs, m_viewNodes, m_subNodeContainer);
        usageTab.setControl(m_nodeUsageComposite);
        usageTab.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(final DisposeEvent e) {
                m_nodeUsageComposite = null;
            }
        });

        TabItem basicTab = new TabItem(tabs, SWT.NONE);
        basicTab.setText("Basic");
        basicTab.setControl(createBasicComposite(tabs));

        TabItem jsonTab = new TabItem(tabs, SWT.NONE);
        jsonTab.setText("Advanced");
        jsonTab.setControl(createJSONEditorComposite(tabs));

        setControl(tabs);
    }

    boolean applyUsageChanges() {
        try (WorkflowLock lock = m_subNodeContainer.lock()) { // each node will cause lock acquisition, do it as bulk
            for (Entry<NodeID, Button> wUsage : m_nodeUsageComposite.getWizardUsageMap().entrySet()) {
                NodeID id = wUsage.getKey();
                boolean hide = !wUsage.getValue().getSelection();
                try {
                    m_subNodeContainer.setHideNodeFromWizard(id, hide);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unable to set hide in wizard flag on node: " + e.getMessage(), e);
                    return false;
                }
            }

            for (Entry<NodeID, Button> dUsage : m_nodeUsageComposite.getDialogUsageMap().entrySet()) {
                NodeID id = dUsage.getKey();
                boolean hide = !dUsage.getValue().getSelection();
                try {
                    m_subNodeContainer.setHideNodeFromDialog(id, hide);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unable to set hide in dialog flag on node: " + e.getMessage(), e);
                    return false;
                }
            }
        }

        return true;
    }

    private Composite createBasicComposite(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        ScrolledComposite scrollPane = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        scrollPane.setExpandHorizontal(true);
        scrollPane.setExpandVertical(true);
        m_basicComposite = new Composite(scrollPane, SWT.NONE);
        scrollPane.setContent(m_basicComposite);
        scrollPane.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        fillBasicComposite();
        scrollPane.setMinSize(m_basicComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return composite;
    }

    private void fillBasicComposite() {

        if (!m_basicPanelAvailable) {
            GridData gridData = new GridData();
            gridData.grabExcessHorizontalSpace = true;
            gridData.horizontalAlignment = SWT.CENTER;
            Label infoLabel = new Label(m_basicComposite, SWT.CENTER);
            infoLabel.setText("A basic configuration of the layout is not possible. \nPlease use the \"Advanced\" tab.");
            infoLabel.setLayoutData(gridData);
            return;
        }

        m_basicComposite.setLayout(new GridLayout(7, false));
        m_basicComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        Label titleLabel = new Label(m_basicComposite, SWT.LEFT);
        FontData fontData = titleLabel.getFont().getFontData()[0];
        Font boldFont = new Font(Display.getCurrent(),
            new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
        titleLabel.setText("Node");
        titleLabel.setFont(boldFont);
        new Composite(m_basicComposite, SWT.NONE); /* Warning placeholder */
        Label rowLabel = new Label(m_basicComposite, SWT.CENTER);
        rowLabel.setText("Row");
        rowLabel.setFont(boldFont);
        Label colLabel = new Label(m_basicComposite, SWT.CENTER);
        colLabel.setText("Column");
        colLabel.setFont(boldFont);
        Label widthLabel = new Label(m_basicComposite, SWT.CENTER);
        widthLabel.setText("Width");
        widthLabel.setFont(boldFont);
        new Composite(m_basicComposite, SWT.NONE); /* More placeholder */
        new Composite(m_basicComposite, SWT.NONE); /* Remove placeholder */

        for (final Entry<NodeIDSuffix, BasicLayoutInfo> entry : m_basicMap.entrySet()) {
            NodeIDSuffix suffix = entry.getKey();
            BasicLayoutInfo layoutInfo = entry.getValue();
            NodeID nodeID = suffix.prependParent(m_subNodeContainer.getWorkflowManager().getID());
            NodeContainer nodeContainer = m_viewNodes.containsKey(suffix) ? m_wfManager.getNodeContainer(nodeID) : null;

            createNodeLabelComposite(m_basicComposite, nodeID, nodeContainer);

            final Label warningLabel = new Label(m_basicComposite, SWT.CENTER);
            if (nodeContainer!= null && m_viewNodes.get(suffix).isHideInWizard()) {
                warningLabel.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/warning.png"));
                warningLabel.setToolTipText("Node is set to 'Hide in Wizard'. It might not be displayed in the layout.");
            }

            GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
            gridData.widthHint = 50;
            final Spinner rowSpinner = createBasicPanelSpinner(m_basicComposite, layoutInfo.getRow(), 1, 999);
            rowSpinner.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    layoutInfo.setRow(rowSpinner.getSelection());
                    tryUpdateJsonFromBasic();
                }
            });
            final Spinner colSpinner = createBasicPanelSpinner(m_basicComposite, layoutInfo.getCol(), 1, 99);
            colSpinner.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    layoutInfo.setCol(colSpinner.getSelection());
                    tryUpdateJsonFromBasic();
                }
            });
            final Spinner widthSpinner = createBasicPanelSpinner(m_basicComposite, layoutInfo.getColWidth(), 1, 12);
            widthSpinner.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    layoutInfo.setColWidth(widthSpinner.getSelection());
                    tryUpdateJsonFromBasic();
                }
            });

            final Button advancedButton = new Button(m_basicComposite, SWT.PUSH | SWT.CENTER);
            advancedButton.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/settings.png"));
            advancedButton.setToolTipText("Additional layout settings");
            if (nodeContainer == null) {
                advancedButton.setEnabled(false);
            } else {
                advancedButton.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        JSONLayoutViewContent defaultViewContent = m_layoutCreator.getDefaultViewContentForNode(suffix, m_viewNodes.get(suffix));
                        ViewContentSettingsDialog settingsDialog = new ViewContentSettingsDialog(m_basicComposite.getShell(), layoutInfo.getView(), defaultViewContent);
                        if (settingsDialog.open() == Window.OK) {
                            layoutInfo.setView(settingsDialog.getViewSettings());
                            tryUpdateJsonFromBasic();
                        }
                    }
                });
            }

            final Button removeButton = new Button(m_basicComposite, SWT.PUSH | SWT.CENTER);
            removeButton.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/remove.png"));
            removeButton.setToolTipText("Remove node from layout");
            removeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    if (nodeContainer != null) {
                        if (!MessageDialog.openConfirm(m_basicComposite.getShell(), "Confirm deletion",
                            "Are you sure you want to delete node " + suffix + " from the layout?")) {
                            return;
                        }
                    }
                    m_basicMap.remove(suffix);
                    tryUpdateJsonFromBasic();
                }
            });
        }

        for (@SuppressWarnings("rawtypes") final Entry<NodeIDSuffix, WizardNode> entry : m_viewNodes.entrySet()) {
            if (m_basicMap.containsKey(entry.getKey())) {
                continue;
            }
            // Node not in layout
            NodeID nodeID = entry.getKey().prependParent(m_subNodeContainer.getWorkflowManager().getID());
            NodeContainer nodeContainer = m_wfManager.getNodeContainer(nodeID);
            createNodeLabelComposite(m_basicComposite, nodeID, nodeContainer);

            final Label warningLabel = new Label(m_basicComposite, SWT.CENTER);
            if (nodeContainer!= null && m_viewNodes.get(entry.getKey()).isHideInWizard()) {
                warningLabel.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/warning.png"));
                warningLabel.setToolTipText("Node is set to 'Hide in Wizard'. It might not be displayed in the layout.");
            }

            final Button addButton = new Button(m_basicComposite, SWT.PUSH | SWT.CENTER);
            addButton.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/add.png"));
            addButton.setToolTipText("Add node to layout");
            addButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    int lastRow = 0;
                    for (BasicLayoutInfo basicLayoutInfo : m_basicMap.values()) {
                        lastRow = Math.max(lastRow, basicLayoutInfo.getRow());
                    }
                    BasicLayoutInfo newInfo = new BasicLayoutInfo();
                    newInfo.setRow(lastRow + 1);
                    newInfo.setCol(1);
                    newInfo.setColWidth(12);
                    newInfo.setView(m_layoutCreator.getDefaultViewContentForNode(entry.getKey(), entry.getValue()));
                    m_basicMap.put(entry.getKey(), newInfo);
                    tryUpdateJsonFromBasic();
                }
            });
            GridData gridData = new GridData();
            gridData.horizontalSpan = 4;
            Label space = new Label(m_basicComposite, SWT.NONE);
            space.setLayoutData(gridData);
        }

        GridData fillRow = new GridData();
        fillRow.grabExcessHorizontalSpace = true;
        fillRow.horizontalAlignment = SWT.CENTER;
        fillRow.horizontalSpan = 7;

        Button resetButton = new Button(m_basicComposite, SWT.PUSH | SWT.CENTER);
        resetButton.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/reset.png"));
        resetButton.setText("Reset");
        resetButton.setToolTipText("Reset layout to default according to currently available nodes.");
        resetButton.setLayoutData(fillRow);
        resetButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (!MessageDialog.openConfirm(m_basicComposite.getShell(), "Confirm reset",
                    "Are you sure you want to reset the layout according to the currently available nodes?")) {
                    return;
                }
                resetLayout();
                if (isWindows()) {
                    m_textArea.setText(m_jsonDocument);
                } else {
                    m_text.setText(m_jsonDocument);
                }
                updateModelFromJson();
            }
        });
    }

    private Composite createNodeLabelComposite(final Composite parent, final NodeID nodeID, final NodeContainer nodeContainer) {

        Composite labelComposite = new Composite(m_basicComposite, SWT.NONE);
        labelComposite.setLayout(new GridLayout(2, false));
        labelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        Label iconLabel = new Label(labelComposite, SWT.CENTER);
        iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        if (nodeContainer == null) {
            iconLabel.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png"));
            iconLabel.setToolTipText("This node does not exist. \nIt is suggested to delete it from the layout.");
        } else {
            try (InputStream iconURLStream = FileLocator.resolve(nodeContainer.getIcon()).openStream()) {
                iconLabel.setImage(new Image(Display.getCurrent(), iconURLStream));
            } catch (IOException e) { /* do nothing */ }
        }

        Label nodeLabel = new Label(labelComposite, SWT.LEFT);
        nodeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        String nodeName;
        String annotation = null;
        if (nodeContainer == null) {
            nodeName = "[MISSING]";
            FontData font = nodeLabel.getFont().getFontData()[0];
            nodeLabel.setFont(new Font(m_basicComposite.getDisplay(), new FontData(font.getName(), font.getHeight(), SWT.ITALIC)));
            nodeLabel.setToolTipText("This node does not exist. \nIt is suggested to delete it from the layout.");
        } else {
            nodeName = nodeContainer.getName();
            annotation = nodeContainer.getNodeAnnotation().getText();
            if (annotation.length() > 42) {
                nodeLabel.setToolTipText(annotation);
            }
            annotation = StringUtils.abbreviateMiddle(annotation, " [...] ", 50).replaceAll("[\n|\r]", "");
        }
        String nodeLabelText = nodeName + "\nID: " + nodeID.getIndex();
        if (StringUtils.isNoneBlank(annotation)) {
            nodeLabelText += "\n" + annotation;
        }
        nodeLabel.setText(nodeLabelText);
        return labelComposite;
    }

    private Spinner createBasicPanelSpinner(final Composite parent, final int initialValue, final int min, final int max) {
        Spinner spinner = new Spinner(parent, SWT.BORDER);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setIncrement(1);
        spinner.setDigits(0);
        spinner.setSelection(initialValue);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.widthHint = 50;
        if (Platform.OS_LINUX.equals(Platform.getOS())) {
            gridData.widthHint = 100;
        }
        spinner.setLayoutData(gridData);
        return spinner;
    }

    private Composite createJSONEditorComposite(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        if (isWindows()) {

            Composite embedComposite = new Composite(composite, SWT.EMBEDDED | SWT.NO_BACKGROUND);
            final GridLayout gridLayout = new GridLayout();
            gridLayout.verticalSpacing = 0;
            gridLayout.marginWidth = 0;
            gridLayout.marginHeight = 0;
            gridLayout.horizontalSpacing = 0;
            embedComposite.setLayout(gridLayout);
            embedComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            Frame frame = SWT_AWT.new_Frame(embedComposite);
            Panel heavyWeightPanel = new Panel();
            heavyWeightPanel.setLayout(new BoxLayout(heavyWeightPanel, BoxLayout.Y_AXIS));
            frame.add(heavyWeightPanel);
            frame.setFocusTraversalKeysEnabled(false);

            // Use JApplet with JRootPane as layer in between heavyweightPanel and RTextScrollPane
            // This reduces flicker on resize in RSyntaxTextArea
            JApplet applet = new JApplet();
            JRootPane root = applet.getRootPane();
            Container contentPane = root.getContentPane();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            heavyWeightPanel.add(applet);

            m_textArea = new RSyntaxTextArea(10, 60);
            m_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            m_textArea.setCodeFoldingEnabled(true);
            m_textArea.setAntiAliasingEnabled(true);
            RTextScrollPane sp = new RTextScrollPane(m_textArea);
            sp.setDoubleBuffered(true);
            m_textArea.setText(m_jsonDocument);
            m_textArea.setEditable(true);
            m_textArea.setEnabled(true);
            contentPane.add(sp);

            Dimension size = sp.getPreferredSize();
            embedComposite.setSize(size.width, size.height);

            // forward focus to RSyntaxTextArea
            embedComposite.addFocusListener(new FocusAdapter() {

                @Override
                public void focusGained(final FocusEvent e) {
                    ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
                        @Override
                        public void run() {
                            m_textArea.requestFocus();
                            m_textArea.setCaretPosition(m_caretPosition);
                        }
                    });
                }

                @Override
                public void focusLost(final FocusEvent e) {
                    // do nothing
                }
            });

            // delete content of status line, when something is inserted or deleted
            m_textArea.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void changedUpdate(final DocumentEvent arg0) {
                    if (!composite.isDisposed()) {
                        composite.getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (m_statusLine != null && !m_statusLine.isDisposed()) {
                                    m_statusLine.setText("");
                                    updateModelFromJson();
                                }
                            }
                        });
                    }
                }

                @Override
                public void insertUpdate(final DocumentEvent arg0) { /* do nothing */ }

                @Override
                public void removeUpdate(final DocumentEvent arg0) { /* do nothing */ }

            });

            // remember caret position
            m_textArea.addCaretListener(new CaretListener() {
                @Override
                public void caretUpdate(final CaretEvent arg0) {
                    m_caretPosition = arg0.getDot();
                }
            });

        } else {
            m_text = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
            GridData layoutData = new GridData(GridData.FILL_BOTH);
            layoutData.widthHint = 600;
            layoutData.heightHint = 400;
            m_text.setLayoutData(layoutData);
            m_text.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(final ModifyEvent e) {
                    m_jsonDocument = m_text.getText();
                    if (m_statusLine != null && !m_statusLine.isDisposed()) {
                        m_statusLine.setText("");
                        updateModelFromJson();
                    }
                }
            });
            m_text.setText(m_jsonDocument);
        }

        // add status line
        m_statusLine = new Label(composite, SWT.SHADOW_NONE | SWT.WRAP);
        GridData statusGridData = new GridData(SWT.LEFT | SWT.FILL, SWT.BOTTOM, true, false);
        int maxHeight = new PixelConverter(m_statusLine).convertHeightInCharsToPixels(3);
        statusGridData.heightHint = maxHeight + 5;
        // seems to have no impact on the layout. The height will still be 3 rows (at least on Windows 8)
        statusGridData.minimumHeight = new PixelConverter(m_statusLine).convertHeightInCharsToPixels(1);
        m_statusLine.setLayoutData(statusGridData);
        compareNodeIDs();

        return composite;
    }

    /**
     * Sets all currently available view nodes on this editor page.
     *
     * @param manager the workflow manager
     * @param subnodeContainer the wrapped metanode container
     * @param viewNodes a map of all available view nodes
     */
    public void setNodes(final WorkflowManager manager, final SubNodeContainer subnodeContainer, @SuppressWarnings("rawtypes") final Map<NodeIDSuffix, WizardNode> viewNodes) {
        m_wfManager = manager;
        m_subNodeContainer = subnodeContainer;
        m_viewNodes = viewNodes;
        JSONLayoutPage page = null;
        String layout = m_subNodeContainer.getLayoutJSONString();
        if (StringUtils.isNotEmpty(layout)) {
            try {
                ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
                page = mapper.readValue(layout, JSONLayoutPage.class);
                if (page.getRows() == null) {
                    page = null;
                } else {
                    m_jsonDocument = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
                }
            } catch (IOException e) {
                LOGGER.error("Error parsing layout. Pretty printing not possible: " + e.getMessage(), e);
                m_jsonDocument = layout;
            }
        }
        if (page == null) {
            page = resetLayout();
        }
        List<JSONLayoutRow> rows = page.getRows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            JSONLayoutRow row = rows.get(rowIndex);
            populateDocumentNodeIDs(row);
            processBasicLayoutRow(row, rowIndex);
        }
    }

    /**
     * @param page
     */
    private JSONLayoutPage resetLayout() {
        m_documentNodeIDs.clear();
        m_basicMap.clear();
        return generateInitialJson();
    }

    private JSONLayoutPage generateInitialJson() {
        JSONLayoutPage page = m_layoutCreator.createDefaultLayoutStructure(m_viewNodes);
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        try {
            String initialJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
            m_jsonDocument = initialJson;
            return page;
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not create initial layout: " + e.getMessage(), e);
            return null;
        }
    }

    private void populateDocumentNodeIDs(final JSONLayoutContent content) {
        if (content instanceof JSONLayoutRow) {
            JSONLayoutRow row = (JSONLayoutRow)content;
            if (row.getColumns() != null && row.getColumns().size() > 0) {
                for (JSONLayoutColumn col : row.getColumns()) {
                    if (col.getContent() != null && col.getContent().size() > 0) {
                        for (JSONLayoutContent c : col.getContent()) {
                            populateDocumentNodeIDs(c);
                        }
                    }
                }
            }
        } else if (content instanceof JSONLayoutViewContent) {
            String id = ((JSONLayoutViewContent)content).getNodeID();
            if (id != null && !id.isEmpty()) {
                m_documentNodeIDs.add(Integer.parseInt(id));
            }
        }
    }

    private void updateBasicLayout() {
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        JSONLayoutPage page = new JSONLayoutPage();
        ObjectReader reader = mapper.readerForUpdating(page);
        try {
            page = reader.readValue(m_jsonDocument);
        } catch (Exception e) { /* do nothing, input needs to be validated beforehand */ }
        m_basicMap.clear();
        m_basicPanelAvailable = true;
        List<JSONLayoutRow> rows = page.getRows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            JSONLayoutRow row = rows.get(rowIndex);
            populateDocumentNodeIDs(row);
            processBasicLayoutRow(row, rowIndex);
        }
    }

    /**
     * Processes one layout row, creates {@link BasicLayoutInfo} for each contained node and determines if basic layout
     * is available. Some advanced configurations (additional styles or classes, HTML content or nested layouts) can not
     * be represented in a basic layout.
     *
     * @param row the row to process
     * @param rowIndex the index of the row as it appears in the layout
     */
    private void processBasicLayoutRow(final JSONLayoutRow row, final int rowIndex) {
        if (listNotNullOrEmpty(row.getAdditionalStyles()) || listNotNullOrEmpty(row.getAdditionalClasses())) {
            // basic layout not possible, show only advanced tab
            m_basicPanelAvailable = false;
            return;
        }
        List<JSONLayoutColumn> columns = row.getColumns();
        for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
            JSONLayoutColumn column = columns.get(colIndex);
            if (listNotNullOrEmpty(column.getAdditionalStyles()) || listNotNullOrEmpty(column.getAdditionalClasses())) {
                // basic layout not possible, show only advanced tab
                m_basicPanelAvailable = false;
                return;
            }
            List<JSONLayoutContent> content = column.getContent();
            if (content != null) {
                for (JSONLayoutContent jsonLayoutContent : content) {
                    if (jsonLayoutContent != null) {
                        if (jsonLayoutContent instanceof JSONLayoutViewContent) {
                            JSONLayoutViewContent viewContent = (JSONLayoutViewContent)jsonLayoutContent;
                            if (listNotNullOrEmpty(viewContent.getAdditionalStyles()) || listNotNullOrEmpty(viewContent.getAdditionalClasses())) {
                             // basic layout not possible, show only advanced tab
                                m_basicPanelAvailable = false;
                                return;
                            }
                            BasicLayoutInfo basicInfo = new BasicLayoutInfo();
                            basicInfo.setRow(rowIndex + 1);
                            basicInfo.setCol(colIndex + 1);
                            basicInfo.setColWidth(column.getWidthMD());
                            basicInfo.setView(viewContent);
                            NodeIDSuffix id = NodeIDSuffix.fromString(viewContent.getNodeID());
                            m_basicMap.put(id, basicInfo);
                        } else {
                            // basic layout not possible, show only advanced tab
                            m_basicPanelAvailable = false;
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean listNotNullOrEmpty(final List<?> list) {
        return list != null && list.size() > 0;
    }

    private void tryUpdateJsonFromBasic() {
        try {
            basicLayoutToJson();
        } catch (Exception e) {
            //TODO show error in dialog?, this should not happen
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void basicLayoutToJson() throws Exception{
        JSONLayoutPage page = new JSONLayoutPage();
        List<JSONLayoutRow> rows = new ArrayList<JSONLayoutRow>();
        for (BasicLayoutInfo basicLayoutInfo : m_basicMap.values()) {
            while(rows.size() < basicLayoutInfo.getRow()) {
                rows.add(new JSONLayoutRow());
            }
            JSONLayoutRow row = rows.get(basicLayoutInfo.getRow() - 1);
            if (row.getColumns() == null) {
                row.setColumns(new ArrayList<JSONLayoutColumn>());
            }
            List<JSONLayoutColumn> columns = row.getColumns();
            while (columns.size() < basicLayoutInfo.getCol()) {
                columns.add(new JSONLayoutColumn());
            }
            JSONLayoutColumn column = columns.get(basicLayoutInfo.getCol() - 1);
            column.setWidthMD(basicLayoutInfo.getColWidth());
            List<JSONLayoutContent> contentList = column.getContent();
            if (contentList == null) {
                contentList = new ArrayList<JSONLayoutContent>(1);
            }
            contentList.add(basicLayoutInfo.getView());
            column.setContent(contentList);
        }
        page.setRows(rows);
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
        m_jsonDocument = json;
        if (isWindows()) {
            m_textArea.setText(m_jsonDocument);
        } else {
            m_text.setText(m_jsonDocument);
        }
    }

    private void compareNodeIDs() {
        Set<Integer> missingIDs = new HashSet<Integer>();
        Set<Integer> notExistingIDs = new HashSet<Integer>(m_documentNodeIDs);
        Set<Integer> duplicateIDCheck = new HashSet<Integer>(m_documentNodeIDs);
        Set<Integer> duplicateIDs = new HashSet<Integer>();
        for (NodeIDSuffix id : m_viewNodes.keySet()) {
            int i = NodeID.fromString(id.toString()).getIndex();
            if (m_documentNodeIDs.contains(i)) {
                notExistingIDs.remove(i);
            } else {
                missingIDs.add(i);
            }
        }
        for (int id : m_documentNodeIDs) {
            if (!duplicateIDCheck.remove(id)) {
                duplicateIDs.add(id);
            }
        }
        StringBuilder error = new StringBuilder();
        if (notExistingIDs.size() > 0) {
            error.append("Node IDs referenced in layout, but do not exist in node: ");
            for (int id : notExistingIDs) {
                error.append(id);
                error.append(", ");
            }
            error.setLength(error.length() - 2);
            if (missingIDs.size() > 0 || duplicateIDs.size() > 0) {
                error.append("\n");
            }
        }
        if (missingIDs.size() > 0) {
            error.append("Node IDs missing in layout: ");
            for (int id : missingIDs) {
                error.append(id);
                error.append(", ");
            }
            error.setLength(error.length() -2);
            if (duplicateIDs.size() > 0) {
                error.append("\n");
            }
        }
        if (duplicateIDs.size() > 0) {
            error.append("Multiple references to node IDs: ");
            for (int id : duplicateIDs) {
                error.append(id);
                error.append(", ");
            }
            error.setLength(error.length() - 2);
        }
        if (error.length() > 0 && m_statusLine != null && !m_statusLine.isDisposed()) {
            int textWidth = isWindows() ? m_textArea.getSize().width : m_text.getSize().x;
            Point newSize = m_statusLine.computeSize(textWidth, m_statusLine.getSize().y, true);
            m_statusLine.setSize(newSize);
            m_statusLine.setForeground(new Color(Display.getCurrent(), 255, 140, 0));
            m_statusLine.setText(error.toString());
        }
    }

    private void updateModelFromJson() {
        if (isJSONValid()) {
            updateBasicLayout();
        } else {
            m_basicPanelAvailable = false;
        }
        for (Control control : m_basicComposite.getChildren()) {
            control.dispose();
        }
        fillBasicComposite();
        m_basicComposite.layout(true);
    }

    /**
     * @return true, if current JSON layout structure is valid
     */
    protected boolean isJSONValid() {
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        try {
            String json = isWindows() ? m_textArea.getText() : m_jsonDocument;
            JSONLayoutPage page = reader.readValue(json);
            m_documentNodeIDs.clear();
            if (page.getRows() != null) {
                for (JSONLayoutRow row : page.getRows()) {
                    populateDocumentNodeIDs(row);
                }
                compareNodeIDs();
            }
            return true;
        } catch (IOException | NumberFormatException e) {
            String errorMessage;
            if (e instanceof JsonProcessingException) {
                JsonProcessingException jsonException = (JsonProcessingException)e;
                Throwable cause = null;
                Throwable newCause = jsonException.getCause();
                while (newCause instanceof JsonProcessingException) {
                    if (cause == newCause) {
                        break;
                    }
                    cause = newCause;
                    newCause = cause.getCause();
                }
                if (cause instanceof JsonProcessingException) {
                    jsonException = (JsonProcessingException)cause;
                }
                errorMessage = jsonException.getOriginalMessage().split("\n")[0];
                JsonLocation location = jsonException.getLocation();
                if (location != null) {
                    errorMessage += " at line: " + (location.getLineNr() + 1) + " column: " + location.getColumnNr();
                }
            } else {
                String message = e.getMessage();
                errorMessage = message;
            }
            if (m_statusLine != null && !m_statusLine.isDisposed()) {
                m_statusLine.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
                m_statusLine.setText(errorMessage);
                int textWidth = isWindows() ? m_textArea.getSize().width : m_text.getSize().x;
                Point newSize = m_statusLine.computeSize(textWidth, m_statusLine.getSize().y, true);
                m_statusLine.setSize(newSize);
            }
        }
        return false;
    }

    /**
     * @return the jsonDocument
     */
    public String getJsonDocument() {
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        try {
            String json = isWindows() ? m_textArea.getText() : m_jsonDocument;
            JSONLayoutPage page = reader.readValue(json);
            String layoutString = mapper.writeValueAsString(page);
            return layoutString;
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve JSON string from layout:" + e.getMessage(), e);
        }

        return "";
    }

    private boolean isWindows() {
        return Platform.OS_WIN32.equals(Platform.getOS());
    }

    private static class BasicLayoutInfo {

        private int m_row;
        private int m_col;
        private int m_colWidth;
        private JSONLayoutViewContent m_view;

        /**
         * @return the row
         */
        public int getRow() {
            return m_row;
        }

        /**
         * @param row the row to set
         */
        public void setRow(final int row) {
            m_row = row;
        }

        /**
         * @return the col
         */
        public int getCol() {
            return m_col;
        }

        /**
         * @param col the col to set
         */
        public void setCol(final int col) {
            m_col = col;
        }

        /**
         * @return the colWidth
         */
        public int getColWidth() {
            return m_colWidth;
        }

        /**
         * @param colWidth the colWidth to set
         */
        public void setColWidth(final int colWidth) {
            m_colWidth = colWidth;
        }

        /**
         * @return the view
         */
        JSONLayoutViewContent getView() {
            return m_view;
        }
        /**
         * @param view the view to set
         */
        void setView(final JSONLayoutViewContent view) {
            m_view = view;
        }

    }

}
