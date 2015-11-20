/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 16, 2015 (albrecht): created
 */
package org.knime.workbench.editor2.subnode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.layout.bs.JSONLayoutColumn;
import org.knime.js.core.layout.bs.JSONLayoutPage;
import org.knime.js.core.layout.bs.JSONLayoutRow;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public class SubnodeLayoutJSONEditorPage extends WizardPage {

    private WorkflowManager m_wfManager;
    private SubNodeContainer m_subNodeContainer;
    private List<NodeID> m_viewNodes;

    private String m_jsonDocument;
    private Label m_statusLine;
    //private RSyntaxTextArea m_textArea;
    private Text m_textArea;

    /**
     * @param pageName
     */
    protected SubnodeLayoutJSONEditorPage(final String pageName) {
        super(pageName);
        setDescription("Please define the layout of the view nodes.");
        m_jsonDocument = "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        m_textArea = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.widthHint = 600;
        layoutData.heightHint = 400;
        m_textArea.setLayoutData(layoutData);
        m_textArea.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                m_jsonDocument = m_textArea.getText();
            }
        });
        m_textArea.setText(m_jsonDocument);

        /*Composite embedComposite = new Composite(composite, SWT.EMBEDDED);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        embedComposite.setLayout(gridLayout);
        embedComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        Frame frame = SWT_AWT.new_Frame(embedComposite);
        Panel heavyWeightPanel = new Panel();
        heavyWeightPanel.setLayout(new BoxLayout(heavyWeightPanel, BoxLayout.Y_AXIS));
        frame.add(heavyWeightPanel);
        frame.setFocusTraversalKeysEnabled(false);

        m_textArea = new RSyntaxTextArea(10, 60);
        m_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        m_textArea.setCodeFoldingEnabled(true);
        m_textArea.setAntiAliasingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(m_textArea);
        m_textArea.setText(m_jsonDocument);
        m_textArea.setEditable(true);
        m_textArea.setEnabled(true);
        heavyWeightPanel.add(sp);

        Dimension size = sp.getPreferredSize();
        embedComposite.setSize(size.width, size.height);

        embedComposite.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(final FocusEvent e) {
                m_textArea.requestFocus();
                ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Focusing");
                        m_textArea.requestFocus();
                        m_textArea.setCaretPosition(5);
                    }
                });
            }

            @Override
            public void focusLost(final FocusEvent e) {
                ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Focus out");
                    }
                });
            }
        });

        m_textArea.requestFocus();

        m_textArea.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final java.awt.event.FocusEvent e) {
                System.out.println("Focus lost");

            }

            @Override
            public void focusGained(final java.awt.event.FocusEvent e) {
                System.out.println("Focus gained");

            }
        });*/

        /*final FocusListener sizeFocusListener = new FocusListener() {

            @Override
            public void focusLost(final java.awt.event.FocusEvent e) {  do nothing  }

            @Override
            public void focusGained(final java.awt.event.FocusEvent e) {
                final Dimension panelSize = textArea.getSize();
                if (!embedComposite.isDisposed()) {
                    embedComposite.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!embedComposite.isDisposed()) {
                                final Point wrapperSize = embedComposite.getSize();
                                // Check if size of this component is the same as the panel
                                System.out.println(panelSize + " " + wrapperSize);
                                if (panelSize.width != wrapperSize.x || panelSize.height != wrapperSize.y) {
                                    ViewUtils.invokeLaterInEDT(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Set sizes on AWT frame and dialog panel if different from
                                            // wrapper component
                                            frame.setSize(wrapperSize.x, wrapperSize.y);
                                            sp.setSize(wrapperSize.x, wrapperSize.y);
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }
        };
        sp.addFocusListener(sizeFocusListener);*/

        m_statusLine = new Label(composite, SWT.BORDER);
        m_statusLine.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
        m_statusLine.setText("OK");

        setControl(composite);
    }

    /**
     * @param manager
     * @param subnodeContainer
     * @param viewNodes
     */
    public void setNodes(final WorkflowManager manager, final SubNodeContainer subnodeContainer,
            final List<NodeID> viewNodes) {
        m_wfManager = manager;
        m_subNodeContainer = subnodeContainer;
        m_viewNodes = viewNodes;

        String layout = m_subNodeContainer.getLayoutJSONString();
        if (layout != null && !layout.trim().isEmpty()) {
            try {
                ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
                Object oLayout = mapper.readValue(layout, Object.class);
                m_jsonDocument = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(oLayout);
            } catch (IOException e) {
                // TODO log error
            }
        }

        if (m_jsonDocument == null || m_jsonDocument.trim().isEmpty()) {
            generateInitialJson();
        }
    }

    private void generateInitialJson() {
        JSONLayoutPage page = new JSONLayoutPage();
        List<JSONLayoutRow> rows = new ArrayList<JSONLayoutRow>();
        page.setRows(rows);
        for (NodeID nodeID : m_viewNodes) {
            JSONLayoutRow row = new JSONLayoutRow();
            JSONLayoutColumn col = new JSONLayoutColumn();
            JSONLayoutViewContent view = new JSONLayoutViewContent();
            view.setNodeID(Integer.toString(nodeID.getIndex()));
            //view.setUseAspectRatioResize(true);
            col.setContent(Arrays.asList(new JSONLayoutViewContent[]{view}));
            col.setWidthMD(12);
            row.addColumn(col);
            rows.add(row);
        }
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        try {
            String initialJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
            m_jsonDocument = initialJson;
        } catch (JsonProcessingException e) {
            // TODO log error
        }
    }

    /**
     * @return true, if current JSON layout structure is valid
     */
    protected boolean isJSONValid() {
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        try {
            m_statusLine.setText("");
            JSONLayoutPage page = reader.readValue(m_jsonDocument);
            // TODO add more checks in objects (e.g. column widths)
            return true;
        } catch (IOException e) {
            m_statusLine.setText("Error reading structure: " + e.getMessage());
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
            JSONLayoutPage page = reader.readValue(m_jsonDocument);
            String layoutString = mapper.writeValueAsString(page);
            return layoutString;
        } catch (IOException e) {
            // TODO log error
        }

        return "";
    }

}
