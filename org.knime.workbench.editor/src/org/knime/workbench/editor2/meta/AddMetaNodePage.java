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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   14.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.figures.AbstractPortFigure;

/**
 * The single page of the {@link AddMetaNodeWizard}. Used to either create a new meta node (then specify the template to
 * begin with) or to reconfigure an exiting meta node (then set the MetaNode) to initialize components accordingly.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class AddMetaNodePage extends WizardPage {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AddMetaNodePage.class);

    private static final String DESCRIPTION = "Specifiy the name of the node and define the number and type of "
        + "the desired in and out ports.";

    private Composite m_previewPanel;
    private Text m_name;
    private List m_inPorts;
    private List m_outPorts;
    private final ArrayList<MetaPortInfo>m_inPortList = new ArrayList<MetaPortInfo>();
    private final ArrayList<MetaPortInfo>m_outPortList = new ArrayList<MetaPortInfo>();


    private boolean m_wasVisible = false;

    private Button m_addInPort;
    private Button m_addOutPort;
    private Button m_remInPort;
    private Button m_remOutPort;
    private Button m_upInPort;
    private Button m_upOutPort;
    private Button m_downInPort;
    private Button m_downOutPort;

    private WorkflowManager m_metaNode;
    private String m_template;

    private Label m_portMsg;

    /**
     * Creates the page and sets title and description.
     * @param title of the wizard page
     */
    public AddMetaNodePage(final String title) {
        super(title);
        setDescription(DESCRIPTION);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        createTopPart(composite);
        createCenterPart(composite);
        createPreviewPanel(composite);
        setControl(composite);
    }

    /**
     * This page initializes either from the template or the meta node set.
     *
     * @param template the selected meta node template from previous page (that is to be created)
     */
    void setTemplate(final String template) {
        m_template = template;
    }

    /**
     * This page initializes either from the meta node or the template set.
     *
     * @param metaNode the meta node to initialize the port lists from (and that is to be reconfigured)
     */
    void setMetaNode(final WorkflowManager metaNode) {
        m_metaNode = metaNode;
    }


    /** {@inheritDoc} */
    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);
        if (visible) {
            m_wasVisible = true;
            if (m_template != null) {
                populateInfoListsFromTemplate();
            } else if (m_metaNode != null) {
                populateInfoListsFromMetaNode(m_metaNode);
            }
            populateSelectionListsFromInfoList();
            if (m_metaNode != null) {
                m_name.setText(m_metaNode.getName());
            } else {
                m_name.setText("MetaNode");
            }
            m_previewPanel.redraw();
            m_name.setFocus();
            m_name.selectAll();
            updateStatus();
        }
        m_previewPanel.redraw();
    }

    private void populateInfoListsFromMetaNode(final WorkflowManager metaNode) {
        m_inPortList.clear();
        m_outPortList.clear();
        MetaPortInfo[] inPortInfo = metaNode.getParent().getMetanodeInputPortInfo(metaNode.getID());
        MetaPortInfo[] outPortInfo = metaNode.getParent().getMetanodeOutputPortInfo(metaNode.getID());
        m_inPortList.addAll(Arrays.asList(inPortInfo));
        m_outPortList.addAll(Arrays.asList(outPortInfo));
    }

    private void populateInfoListsFromTemplate() {
        LOGGER.debug("Populating fields from template " + m_template);
        // clear all fields
        m_inPortList.clear();
        m_outPortList.clear();
        if (m_template == null) {
            return;
        }

        int nrInPorts = 0;
        int nrOutPorts = 0;
        if (m_template.equals(SelectMetaNodePage.ZERO_ONE)) {
            nrInPorts = 0;
            nrOutPorts = 1;
        } else if (m_template.equals(SelectMetaNodePage.ONE_ONE)) {
            nrInPorts = 1;
            nrOutPorts = 1;
        } else if (m_template.equals(SelectMetaNodePage.ONE_TWO)) {
            nrInPorts = 1;
            nrOutPorts = 2;
        } else if (m_template.equals(SelectMetaNodePage.TWO_ONE)) {
            nrInPorts = 2;
            nrOutPorts = 1;
        } else if (m_template.equals(SelectMetaNodePage.TWO_TWO)) {
            nrInPorts = 2;
            nrOutPorts = 2;
        }
        // add the ports to the lists
        for (int i = 0; i < nrInPorts; i++) {
            // the "new index" is not maintained in this wizard page
            MetaPortInfo info = new MetaPortInfo(BufferedDataTable.TYPE, -1);
            m_inPortList.add(info);
        }
        for (int i = 0; i < nrOutPorts; i++) {
            // the "new index" is not maintained in this wizard page
            MetaPortInfo info = new MetaPortInfo(BufferedDataTable.TYPE, -1);
            m_outPortList.add(info);
        }
    }

    private void populateSelectionListsFromInfoList() {
        populateSelectionlistFromInfolist(m_inPorts, m_inPortList, "in_");
        populateSelectionlistFromInfolist(m_outPorts, m_outPortList, "out_");
    }

    private void populateSelectionlistFromInfolist(final List selList, final ArrayList<MetaPortInfo> infoList,
            final String namePrefix) {
        selList.removeAll();
        for (int i = 0; i < infoList.size(); i++) {
            MetaPortInfo info = infoList.get(i);

            String typeName = info.getType().getPortObjectClass().getSimpleName();
            // see if we find a nicer name
            for (MetaNodePortType t :AddMetaNodePortTypeCollector.getInstance().getTypes()) {
                if (t.getType().equals(info.getType())) {
                    typeName = t.getName();
                    break;
                }
            }
            String listEntry = namePrefix + i + " (" + typeName + ")";
            selList.add(listEntry);
        }
    }

    /**
     * @return list of entered out ports
     */
    public ArrayList<MetaPortInfo> getOutPorts() {
        return m_outPortList;
    }

    /**
     * @return list of entered in ports
     */
    public ArrayList<MetaPortInfo>getInports() {
        return m_inPortList;
    }


    /**
     *
     * @return the entered name for the meta node
     */
    public String getMetaNodeName() {
        return m_name.getText();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isPageComplete() {
        if (!m_wasVisible) {
            return false;
        }
        if (m_template == null && m_metaNode == null) {
            return false;
        }
        return true;
    }

    private void updateStatus() {
        updateButtonState();
        m_portMsg.setText("");
        boolean inPort = true;
        int idx = m_inPorts.getSelectionIndex();
        if (idx < 0) {
            inPort = false;
            idx = m_outPorts.getSelectionIndex();
        }
        if (idx >= 0) {
            MetaPortInfo info = inPort ? m_inPortList.get(idx) : m_outPortList.get(idx);
            StringBuilder str = new StringBuilder();
            str.append(inPort ? "Input " : "Output ");
            str.append("port ");
            str.append(idx);
            str.append(": ");
            // Old ports show connectivity info
            if (info.getOldIndex() >= 0) {
                if (info.isConnected()) {
                    if (info.getMessage() != null && !info.getMessage().isEmpty()) {
                        str.append(info.getMessage()); // info contains connectivity info
                        str.append(". ");
                    } else {
                        str.append("Connected. ");
                    }
                } else {
                    str.append("Not connected. ");
                }
                if (info.getOldIndex() != idx) {
                    str.append("Moved from old index ");
                    str.append(info.getOldIndex());
                    str.append(". ");
                }
            } else {
                str.append("New port. ");
            }
            if (m_metaNode != null) {
                // only when we change things we show the infos
                m_portMsg.setText(str.toString());
            }
        }
        m_portMsg.setToolTipText(m_portMsg.getText());
        m_previewPanel.redraw();
        setPageComplete(isPageComplete());
    }

    private void updateButtonState() {
        int inSel = m_inPorts.getSelectionIndex();
        if (inSel >=0) {
            MetaPortInfo info = m_inPortList.get(inSel);
            m_addInPort.setEnabled(true);
            m_remInPort.setEnabled(!info.isConnected());
            m_upInPort.setEnabled(inSel > 0);
            m_downInPort.setEnabled(inSel < m_inPortList.size() - 1);
        } else {
            m_addInPort.setEnabled(true);
            m_remInPort.setEnabled(false);
            m_upInPort.setEnabled(false);
            m_downInPort.setEnabled(false);
        }

        int outSel = m_outPorts.getSelectionIndex();
        if (outSel >= 0) {
            MetaPortInfo info = m_outPortList.get(outSel);
            m_addOutPort.setEnabled(true);
            m_remOutPort.setEnabled(!info.isConnected());
            m_upOutPort.setEnabled(outSel > 0);
            m_downOutPort.setEnabled(outSel < m_outPortList.size() - 1);
        } else {
            m_addOutPort.setEnabled(true);
            m_remOutPort.setEnabled(false);
            m_upOutPort.setEnabled(false);
            m_downOutPort.setEnabled(false);
        }
    }

    private void createCenterPart(final Composite parent) {
        Composite composite = new Composite(parent, SWT.TOP | SWT.BORDER);
        composite.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        createPortPart(composite, true);
        createPortPart(composite, false);
        // the port message
        m_portMsg = new Label(composite, SWT.NO_FOCUS);
        m_portMsg.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        m_portMsg.setText("");
    }

    private void createPortPart(final Composite parent, final boolean in) {
        // two columns: port list and buttons
        Composite portHandling = new Composite(parent, SWT.BORDER);
        portHandling.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        portHandling.setLayoutData(gridData);
        // left (label and list)
        Composite left = new Composite(portHandling, SWT.LEFT);
        left.setLayout(new GridLayout(1, false));
        gridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        left.setLayoutData(gridData);
        // right (buttons)
        Composite right = new Composite(portHandling, SWT.RIGHT);
        right.setLayout(new GridLayout(1, false));
        GridData btnGridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER,
                GridData.VERTICAL_ALIGN_CENTER, false, true);
        btnGridData.widthHint = 80;

        if (in) {
            Label label = new Label(left, SWT.NONE);
            label.setText("In Ports:");
            label.setLayoutData(new GridData(SWT.FILL, GridData.VERTICAL_ALIGN_CENTER, false, false));
            m_inPorts = new List(left,
                    SWT.SINGLE | SWT.SCROLL_PAGE | SWT.BORDER);
            m_inPorts.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    portSelectionChanged(true);
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    // dbl-click
                    widgetSelected(e);
                }
            });
            m_addInPort = new Button(right, SWT.PUSH);
            m_addInPort.setText("Add...");
            m_addInPort.setLayoutData(btnGridData);
            m_addInPort.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    widgetDefaultSelected(e);
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    addPort(true);
                }
            });
            m_remInPort = new Button(right, SWT.PUSH);
            m_remInPort.setText("Remove");
            m_remInPort.setLayoutData(btnGridData);
            m_remInPort.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    widgetDefaultSelected(e);
                }
                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    removeSelPort(true);
                }
            });
            m_upInPort = new Button(right, SWT.PUSH);
            m_upInPort.setText("Up");
            m_upInPort.setLayoutData(btnGridData);
            m_upInPort.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    widgetDefaultSelected(e);
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    moveSelPort(true, true);
                }
            });
            m_downInPort = new Button(right, SWT.PUSH);
            m_downInPort.setText("Down");
            m_downInPort.setLayoutData(btnGridData);
            m_downInPort.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    widgetDefaultSelected(e);
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    moveSelPort(true, false);
                }
            });
        } else {
            Label label = new Label(left, SWT.NONE);
            label.setText("Out Ports:");
            label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                    GridData.VERTICAL_ALIGN_CENTER, false, false));
            m_outPorts = new List(left,
                    SWT.SINGLE | SWT.SCROLL_PAGE | SWT.BORDER);
            m_outPorts.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    portSelectionChanged(false);
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    // dbl-click
                    widgetSelected(e);
                }
            });
            m_addOutPort = new Button(right, SWT.PUSH);
            m_addOutPort.setText("Add...");
            m_addOutPort.setLayoutData(btnGridData);
            m_addOutPort.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    widgetDefaultSelected(e);
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    addPort(false);
                }
            });
            m_remOutPort = new Button(right, SWT.PUSH);
            m_remOutPort.setText("Remove");
            m_remOutPort.setLayoutData(btnGridData);
            m_remOutPort.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    widgetDefaultSelected(e);
                }
                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    removeSelPort(false);
                }
            });
            m_upOutPort = new Button(right, SWT.PUSH);
            m_upOutPort.setText("Up");
            m_upOutPort.setLayoutData(btnGridData);
            m_upOutPort.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    widgetDefaultSelected(e);
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    moveSelPort(false, true);
                }
            });
            m_downOutPort = new Button(right, SWT.PUSH);
            m_downOutPort.setText("Down");
            m_downOutPort.setLayoutData(btnGridData);
            m_downOutPort.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    widgetDefaultSelected(e);
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    moveSelPort(false, false);
                }
            });

        }
       // layout list
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = 130;
        if (in) {
            m_inPorts.setLayoutData(gridData);
        } else {
            m_outPorts.setLayoutData(gridData);
        }
    }

    private void portSelectionChanged(final boolean inPort) {
        if (inPort) {
            m_outPorts.deselectAll();
        } else {
            m_inPorts.deselectAll();
        }
        updateStatus();
    }


    private void addPort(final boolean inPort) {
        ArrayList<MetaPortInfo> infoList = inPort ? m_inPortList : m_outPortList;
        List portList = inPort ? m_inPorts : m_outPorts;
        MetaPortDialog dialog = new MetaPortDialog(Display.getDefault().getActiveShell());
        Port port = dialog.open();
        if (port != null) {
            MetaPortInfo info = new MetaPortInfo(port.getType(), infoList.size());
            infoList.add(info);
            populateSelectionlistFromInfolist(portList, infoList, inPort ? "in_" : "out_");
            portList.select(infoList.size() - 1);
        }
        m_previewPanel.redraw();
        updateStatus();
    }

    private void removeSelPort(final boolean inPort) {
        ArrayList<MetaPortInfo> infoList = inPort ? m_inPortList : m_outPortList;
        List portList = inPort ? m_inPorts : m_outPorts;

        int idx = portList.getSelectionIndex();
        if (idx < 0) {
            return;
        }
        if (idx >= infoList.size()) {
            LOGGER.coding("Selected port index is too big for internal port info list");
            return;
        }
        infoList.remove(idx);
        populateSelectionlistFromInfolist(portList, infoList, inPort ? "in_" : "out_");
        updateStatus();
    }

    private void moveSelPort(final boolean inPort, final boolean moveUp) {
        ArrayList<MetaPortInfo> infoList = inPort ? m_inPortList : m_outPortList;
        List portList = inPort ? m_inPorts : m_outPorts;
        int idx = portList.getSelectionIndex();
        int minIdx = moveUp ? 1 : 0;
        if (idx < minIdx) {
            return;
        }
        int maxIdx = moveUp ? infoList.size() - 1 : infoList.size() - 2;
        if (idx > maxIdx) {
            LOGGER.coding("Selected port index is too big for internal port info list to move "
                    + (moveUp ? "up" : "down"));
            return;
        }
        MetaPortInfo info = infoList.get(idx);
        infoList.remove(idx);

        idx = moveUp ? (idx - 1) : (idx + 1);

        infoList.add(idx, info);

        populateSelectionlistFromInfolist(portList, infoList, inPort ? "in_" : "out_");
        portList.select(idx);
        updateStatus();
    }

    private void createTopPart(final Composite parent) {
        // top part
        Composite composite = new Composite(parent, SWT.TOP);
        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 2;
        composite.setLayout(topLayout);
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL);
        composite.setLayoutData(gridData);
        // label
        Label label = new Label(composite, SWT.NONE);
        label.setText("Meta Node Name:");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                GridData.VERTICAL_ALIGN_CENTER, false, false);
        label.setLayoutData(gridData);
        // text field
        m_name = new Text(composite, SWT.BORDER);
        gridData = new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
                | GridData.VERTICAL_ALIGN_CENTER);
        m_name.setLayoutData(gridData);
        m_name.addFocusListener(new FocusAdapter() {
           @Override
            public void focusLost(final FocusEvent e) {
                   updateStatus();
            }
            @Override
            public void focusGained(final FocusEvent e) {
                m_name.selectAll();
            }
        });
    }


    private void createPreviewPanel(final Composite parent) {
        m_previewPanel = new Composite(parent, SWT.FILL | SWT.BORDER);
        m_previewPanel.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_CENTER
                | GridData.VERTICAL_ALIGN_CENTER));
        m_previewPanel.setLayout(new FillLayout());
        m_previewPanel.addPaintListener(new PaintListener() {
            private static final int IMAGE_HEIGHT = 30;
            private static final int IMAGE_WIDTH = 30;
            private static final int PORT_BAR_HEIGHT = 40;
            private static final int PORT_SIZE
                = AbstractPortFigure.NODE_PORT_SIZE;

            private int m_top;

            @Override
            public void paintControl(final PaintEvent e) {
                GC gc = e.gc;
                Rectangle bounds = m_previewPanel.getBounds();
                m_top = (bounds.height / 2) - (IMAGE_HEIGHT / 2)
                    - (PORT_SIZE / 2);
                drawInPorts(gc);
                drawOutPorts(gc);
                gc.drawImage(ImageRepository.getImage(
                    "/icons/meta/meta_custom_preview.png"),
                    (bounds.width / 2) - (IMAGE_WIDTH / 2),
                    (bounds.height / 2) - (IMAGE_HEIGHT / 2));
            }

            private void drawInPorts(final GC gc) {
                Rectangle b = m_previewPanel.getBounds();
                int offset = (PORT_BAR_HEIGHT + PORT_SIZE)
                    / (m_inPortList.size() + 1);
                int left = (b.width / 2) - (IMAGE_WIDTH / 2);
                int i = 0;
                for (MetaPortInfo inPort : m_inPortList) {
                    int y = m_top + (((i + 1) * offset) - (PORT_SIZE));
                    if (inPort.getType().equals(BufferedDataTable.TYPE)) {
                        gc.drawPolygon(new int[] {
                                left - PORT_SIZE, y,
                                left, y + (PORT_SIZE / 2),
                                left - PORT_SIZE, y + PORT_SIZE});
                    } else if (PMMLPortObject.TYPE.isSuperTypeOf(
                            inPort.getType())) {
                        gc.setBackground(ColorConstants.blue);
                        gc.fillRectangle(
                                left - PORT_SIZE,
                                y,
                                PORT_SIZE, PORT_SIZE);
                    } else if (inPort.getType().equals(
                            DatabasePortObject.TYPE)) {
                        gc.setBackground(getShell().getDisplay().getSystemColor(
                                SWT.COLOR_DARK_RED));
                        gc.fillRectangle(
                                left - PORT_SIZE,
                                y,
                                PORT_SIZE, PORT_SIZE);
                    } else if (inPort.getType().equals(
                            FlowVariablePortObject.TYPE)) {
                        gc.setBackground(getShell().getDisplay().getSystemColor(
                                SWT.COLOR_RED));
                        gc.fillOval(
                                left - PORT_SIZE - 1,
                                y -1,
                                PORT_SIZE + 1, PORT_SIZE + 1);
                    } else {
                        gc.setBackground(getShell().getDisplay().getSystemColor(
                                SWT.COLOR_GRAY));
                        gc.fillRectangle(
                                left - PORT_SIZE,
                                y,
                                PORT_SIZE, PORT_SIZE);
                    }
                    i++;
                }
            }

            private void drawOutPorts(final GC gc) {
                Rectangle b = m_previewPanel.getBounds();
                int right = (b.width / 2) + (IMAGE_WIDTH / 2) + 2;
                int offset = (PORT_BAR_HEIGHT + PORT_SIZE)
                    / (m_outPortList.size() + 1);
                int i = 0;
                for (MetaPortInfo inPort : m_outPortList) {
                    int y = m_top + (((i + 1) * offset) - (PORT_SIZE));
                    if (inPort.getType().equals(BufferedDataTable.TYPE)) {
                        gc.drawPolygon(new int[] {
                                right, y,
                                right + PORT_SIZE, y + (PORT_SIZE / 2),
                                right, y + PORT_SIZE});
                    } else if (PMMLPortObject.TYPE.isSuperTypeOf(
                            inPort.getType())) {
                        gc.setBackground(ColorConstants.blue);
                        gc.fillRectangle(right, y, PORT_SIZE, PORT_SIZE);
                    } else if (inPort.getType().equals(
                            DatabasePortObject.TYPE)) {
                        gc.setBackground(getShell().getDisplay().getSystemColor(
                                SWT.COLOR_DARK_RED));
                        gc.fillRectangle(right, y, PORT_SIZE, PORT_SIZE);
                    } else if (inPort.getType().equals(
                            FlowVariablePortObject.TYPE)) {
                        gc.setBackground(getShell().getDisplay().getSystemColor(
                                SWT.COLOR_RED));
                        gc.fillRectangle(right, y, PORT_SIZE, PORT_SIZE);
                    } else {
                        gc.setBackground(getShell().getDisplay().getSystemColor(
                                SWT.COLOR_GRAY));
                        gc.fillRectangle(right, y, PORT_SIZE, PORT_SIZE);
                    }
                    i++;
                }
            }
        });
    }

}
