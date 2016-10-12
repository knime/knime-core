/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   21.03.2012 (mb): created
 */
package org.knime.workbench.nodemonitorview;

import static org.knime.core.node.util.UseImplUtil.getImplOf;
import static org.knime.core.node.util.UseImplUtil.getWFMImplOf;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.api.node.workflow.NodeStateChangeListener;
import org.knime.core.api.node.workflow.NodeStateEvent;
import org.knime.core.api.node.workflow.WorkflowEvent;
import org.knime.core.api.node.workflow.WorkflowEvent.Type;
import org.knime.core.api.node.workflow.WorkflowListener;
import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.tableview.TableView;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeGraphAnnotation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.Pair;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.ui.wrapper.Panel2CompositeWrapper;

/**
 * An Eclipse View showing the output of a node.
 *
 * @author M. Berthold, KNIME.com AG
 */
public class NodeOutputView extends ViewPart implements ISelectionListener, LocationListener, NodeStateChangeListener,
    WorkflowListener {
    /** The ID of the view as specified with the extension point. */
    public static final String ID = "org.knime.workbench.nodeoutputview";

    private static final String DEF_VIEW_NAME = "Node Output View";
    private Text m_title;

    private Label m_info;

    private ComboViewer m_portIndex;

    private int m_portIndexInit = 0; // might be set when view is inited

    private Composite m_stackPanel;

    private Table m_table;

    private TableView m_tableView;

    private Panel2CompositeWrapper m_tableViewPanel;

    private Label m_errorLabel; // displayed instead of data table in case of error

    private IStructuredSelection m_lastSelection;

    private IStructuredSelection m_lastSelectionWhilePinned;

    private IStructuredSelection m_selectionWhenLocked;

    private WorkflowManager m_parentWfm;

    private WorkflowManager m_workflow;

    private NodeID m_lastNode = null;

    private boolean m_pinned = false;

    private RetargetAction m_pinButton;

    private boolean m_branchLocked = true;

    private enum DISPLAYOPTIONS {
        VARS, SETTINGS, ALLSETTINGS, TABLE, GRAPHANNOTATIONS
    }

    private DISPLAYOPTIONS m_choice = DISPLAYOPTIONS.TABLE;

    /** {@inheritDoc} */
    @Override
    public void createPartControl(final Composite parent) {
        // Toolbar
        IToolBarManager toolbarMGR = getViewSite().getActionBars().getToolBarManager();
        // create button for stick with branch.
        final RetargetAction lockAction =
            new RetargetAction("StayOnBranch", "Pin view to new node added to branch", IAction.AS_CHECK_BOX);
        lockAction.setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(), "icons/lock.png"));
        lockAction.setChecked(m_branchLocked);
        lockAction.setEnabled(m_pinned);
        lockAction.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (lockAction.isChecked()) {
                    m_branchLocked = true;
                    m_selectionWhenLocked = m_lastSelection;
                } else {
                    m_branchLocked = false;
                    selectionChanged(null, m_lastSelectionWhilePinned);
                }
            }
        });
        toolbarMGR.add(lockAction);

        // create button which allows to "pin" selection:
        m_pinButton = new RetargetAction("PinView", "Pin view to selected node", IAction.AS_CHECK_BOX);
        m_pinButton.setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(), "icons/pin.png"));
        m_pinButton.setChecked(m_pinned);
        m_pinButton.setEnabled(true);
        m_pinButton.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (m_pinButton.isChecked()) {
                    m_pinned = true;
                    m_lastSelectionWhilePinned = m_lastSelection;
                    lockAction.setEnabled(true);
                } else {
                    m_pinned = false;
                    selectionChanged(null, m_lastSelectionWhilePinned);
                    lockAction.setEnabled(false);
                }
            }
        });
        toolbarMGR.add(m_pinButton);
        toolbarMGR.add(new Separator());
        // configure drop down menu
        IMenuManager dropDownMenu = getViewSite().getActionBars().getMenuManager();
        // drop down menu entry for outport table:
        final RetargetAction menuentrytable =
            new RetargetAction("OutputTable", "Show Output Table", IAction.AS_RADIO_BUTTON);
        menuentrytable.setChecked(DISPLAYOPTIONS.TABLE.equals(m_choice));
        menuentrytable.setEnabled(true);
        menuentrytable.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (menuentrytable.isChecked()) {
                    m_choice = DISPLAYOPTIONS.TABLE;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        dropDownMenu.add(menuentrytable);
        // drop down menu entry for node variables:
        final RetargetAction dropdownmenuvars =
            new RetargetAction("NodeVariables", "Show Variables", IAction.AS_RADIO_BUTTON);
        dropdownmenuvars.setChecked(DISPLAYOPTIONS.VARS.equals(m_choice));
        dropdownmenuvars.setEnabled(true);
        dropdownmenuvars.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (dropdownmenuvars.isChecked()) {
                    m_choice = DISPLAYOPTIONS.VARS;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        dropDownMenu.add(dropdownmenuvars);
        // drop down menu entry for configuration/settings:
        final RetargetAction menuentrysettings =
            new RetargetAction("NodeConf", "Show Configuration", IAction.AS_RADIO_BUTTON);
        menuentrysettings.setChecked(DISPLAYOPTIONS.SETTINGS.equals(m_choice));
        menuentrysettings.setEnabled(true);
        menuentrysettings.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (menuentrysettings.isChecked()) {
                    m_choice = DISPLAYOPTIONS.SETTINGS;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        dropDownMenu.add(menuentrysettings);
        // drop down menu entry for configuration/settings:
        final RetargetAction menuentryallsettings =
            new RetargetAction("NodeConfAll", "Show Entire Configuration", IAction.AS_RADIO_BUTTON);
        menuentryallsettings.setChecked(DISPLAYOPTIONS.ALLSETTINGS.equals(m_choice));
        menuentryallsettings.setEnabled(true);
        menuentryallsettings.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (menuentryallsettings.isChecked()) {
                    m_choice = DISPLAYOPTIONS.ALLSETTINGS;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        dropDownMenu.add(menuentryallsettings);
        // drop down menu entry for node graph annotations
        final RetargetAction menuentrygraphannotations =
            new RetargetAction("NodeGraphAnno", "Show Graph Annotations", IAction.AS_RADIO_BUTTON);
        menuentrygraphannotations.setChecked(DISPLAYOPTIONS.GRAPHANNOTATIONS.equals(m_choice));
        menuentrygraphannotations.setEnabled(true);
        menuentrygraphannotations.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (menuentrygraphannotations.isChecked()) {
                    m_choice = DISPLAYOPTIONS.GRAPHANNOTATIONS;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        dropDownMenu.add(menuentrygraphannotations);
        // Content
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(parent);
        // Node Title:
        Label titlelabel = new Label(parent, SWT.NONE);
        titlelabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        titlelabel.setText("Node: ");
        m_title = new Text(parent, SWT.BORDER);
        m_title.setEditable(false);
        m_title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        m_title.setText("n/a.");
        // Panel for currently displayed information (some information
        // providers will add more elements to this):
        Composite infoPanel = new Composite(parent, SWT.NONE);
        GridData infoGrid = new GridData(SWT.FILL, SWT.TOP, true, false);
        infoGrid.horizontalSpan = 2;
        infoPanel.setLayoutData(infoGrid);
        GridLayoutFactory.swtDefaults().numColumns(3).applyTo(infoPanel);
        m_info = new Label(infoPanel, SWT.NONE);
        m_info.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        m_info.setText("n/a.                        ");
        m_portIndex = new ComboViewer(infoPanel);
        m_portIndex.add(new String[]{"port 0", "port 1", "port 2"});
        m_portIndex.getCombo().setEnabled(false);
        m_portIndex.getCombo().setSelection(new Point(m_portIndexInit, m_portIndexInit));
        m_portIndex.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                ISelection sel = event.getSelection();
                try {
                    int newIndex = Integer.parseInt(sel.toString().substring(5).replace(']', ' ').trim());
                    NodeContainer node;
                    if (m_workflow.containsNodeContainer(m_lastNode)) {
                        node = m_workflow.getNodeContainer(m_lastNode);
                        updateDataTable(node, newIndex);
                    }
                } catch (NumberFormatException nfe) {
                    // ignore.
                }
            }
        });
        // Table:
        // stack panel switches between KNIME data table view and SWT table for other info
        m_stackPanel = new Composite(parent, SWT.NONE);
        m_stackPanel.setLayout(new StackLayout());
        GridData tableGrid = new GridData(SWT.FILL, SWT.FILL, true, true);
        m_stackPanel.setLayoutData(tableGrid);
        // DataTable view wrapped in AWT-SWT bridge
        m_tableView = new TableView();
        m_tableViewPanel = new Panel2CompositeWrapper(m_stackPanel, m_tableView, SWT.NONE);
        m_errorLabel = new Label(m_stackPanel, SWT.NONE);
        // SWT Table for the other info
        m_table = new Table(m_stackPanel, SWT.MULTI | SWT.BORDER);
        m_table.setLinesVisible(true);
        m_table.setHeaderVisible(true);
        tableGrid.horizontalSpan = 2;
        m_table.setLayoutData(tableGrid);
        String[] titles = {"Name", "Value"};
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(m_table, SWT.NONE);
            column.setText(titles[i]);
        }
        for (int i = 0; i < titles.length; i++) {
            m_table.getColumn(i).pack();
        }
        m_lastNode = null;
        getViewSite().getPage().addSelectionListener(this);

        if (m_choice.equals(DISPLAYOPTIONS.TABLE)) {
            ((StackLayout)m_stackPanel.getLayout()).topControl = m_tableViewPanel;
            m_stackPanel.layout();
        } else {
            ((StackLayout)m_stackPanel.getLayout()).topControl = m_table;
            m_stackPanel.layout();
        }
        m_stackPanel.layout();
        selectionChanged(null, m_lastSelection);
    }


    private void disconnectFromWFM() {
        if (m_workflow != null) {
            m_workflow.removeListener(this);
            m_parentWfm.removeListener(this);
            if (m_lastNode != null) {
                if (m_workflow.containsNodeContainer(m_lastNode)) {
                    m_workflow.getNodeContainer(m_lastNode).removeNodeStateChangeListener(this);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        getViewSite().getPage().removeSelectionListener(this);
        disconnectFromWFM();
        super.dispose();
    }

    /**
     * The method updating the content of the monitor.
     *
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            // unsupported selection
//            showErrorAndClear("");
            return;
        }
        IStructuredSelection structSel = (IStructuredSelection)selection;
        if (m_pinned) {
            m_lastSelectionWhilePinned = structSel;
            if (m_branchLocked && m_selectionWhenLocked == null) {
                m_selectionWhenLocked = structSel;
            }
            return;
        }
        if (structSel.equals(m_lastSelection)) {
            // selection hasn't changed - return.
            return;
        }
        m_lastSelection = structSel;
        if (structSel.size() < 1) {
            // Nothing selected
            showErrorAndClear("No node selected");
            return;
        }
        if (structSel.size() > 1) {
            // too many selected items
            showErrorAndClear("Multiple elements selected");
            return;
        }
        // retrieve first (and only!) selection:
        Iterator<?> selIt = structSel.iterator();
        Object sel = selIt.next();
        //
        if (sel instanceof NodeContainerEditPart) {
            // a NodeContainer was selected, display it's name and status
            NodeContainer nc = getImplOf(((NodeContainerEditPart)sel).getNodeContainer(), NodeContainer.class);
            WorkflowManager wfm = nc.getParent();
            checkWorkflowManagerListener(wfm);
            updateNodeContainerInfo(nc.getID());
        } else if (sel instanceof WorkflowInPortBarEditPart) {
            WorkflowManager wfm = getWFMImplOf(((WorkflowInPortBarEditPart)sel).getNodeContainer());
            checkWorkflowManagerListener(wfm);

        } else {
            // unsupported selection
            showErrorAndClear("No info available for this selection");
            return;
        }
    }

    private void checkWorkflowManagerListener(final WorkflowManager wfm) {
        if (m_workflow != null && wfm != m_workflow) {
            m_parentWfm.removeListener(this);
            m_workflow.removeListener(this);
        }
        if (m_workflow == null || m_workflow != wfm) {
            m_workflow = wfm;
            m_parentWfm = wfm.getParent();
            m_workflow.addListener(this);
            m_parentWfm.addListener(this); // need to know if flow gets deleted
        }
    }

    private void showErrorAndClear(final String errMsg) {
        setPartName(DEF_VIEW_NAME); // the API documentation lies :(
        m_title.setText("");
        m_errorLabel.setText(errMsg);
        m_lastSelection = null;
        m_portIndex.getCombo().setEnabled(false);
        ((StackLayout)m_stackPanel.getLayout()).topControl = m_errorLabel;
        m_stackPanel.layout();
    }

    /* Update all visuals with information regarding new NodeContainer.
     * Also de-register previous node and register with the new one if
     * the underlying NC changed.
     */
    private void updateNodeContainerInfo(final NodeID nc) {
        if (m_portIndex.getCombo().isDisposed()) {
            return;
        }
        if (nc == null) {
            showErrorAndClear("");
            return;
        }
        m_portIndex.getCombo().setEnabled(false);
        assert Display.getCurrent().getThread() == Thread.currentThread();
        if ((m_lastNode != null) && (m_lastNode != nc)) {
            if (m_workflow.containsNodeContainer(m_lastNode)) {
                NodeContainer last = m_workflow.getNodeContainer(m_lastNode);
                last.removeNodeStateChangeListener(NodeOutputView.this);
            }
        }
        NodeContainer node = m_workflow.getNodeContainer(nc);
        if ((m_lastNode == null) || (m_lastNode != nc)) {
            m_lastNode = nc;
            node.addNodeStateChangeListener(NodeOutputView.this);
            setPartName(node.getNameWithID());
        }
        m_title.setText(node.getName() + "  (" + node.getID() + ")");
        switch (m_choice) {
            case VARS:
                updateVariableTable(node);
                break;
            case SETTINGS:
            case ALLSETTINGS:
                updateSettingsTable(node, DISPLAYOPTIONS.ALLSETTINGS.equals(m_choice));
                break;
            case TABLE:
                m_portIndex.getCombo().setEnabled(true);
                int nrPorts = node.getNrOutPorts();
                if (node instanceof SingleNodeContainer) {
                    // correct for (default - mostly invisible) Variable Port
                    nrPorts--;
                }
                String[] vals = new String[nrPorts];
                for (int i = 0; i < nrPorts; i++) {
                    vals[i] = "Port " + i;
                }
                m_portIndex.getCombo().removeAll();
                m_portIndex.getCombo().setItems(vals);
                m_portIndex.getCombo().select(0);
                updateDataTable(node, 0);
                break;
            case GRAPHANNOTATIONS:
                updateGraphAnnotationTable(node);
                break;
            default:
                throw new AssertionError("Unhandled switch case: " + m_choice);
        }
    }

    /*
     *  Put info about node graph annotations into table.
     */
    private void updateGraphAnnotationTable(final NodeContainer nc) {
        assert Display.getCurrent().getThread() == Thread.currentThread();
        // show swt table
        ((StackLayout)m_stackPanel.getLayout()).topControl = m_table;
        m_stackPanel.layout();

        // Initialize table
        m_table.removeAll();
        for (TableColumn tc : m_table.getColumns()) {
            tc.dispose();
        }
        String[] titles = {"Property", "Value"};
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(m_table, SWT.NONE);
            column.setText(titles[i]);
        }
        // retrieve content
        Set<NodeGraphAnnotation> ngas = nc.getParent().getNodeGraphAnnotation(nc.getID());
        for (NodeGraphAnnotation nga : ngas) {
            TableItem item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "ID");
            item.setText(1, nga.getID().toString());
            item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "depth");
            item.setText(1, "" + nga.getDepth());
            item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "outport Index");
            item.setText(1, "" + nga.getOutportIndex());
            item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "connected inports");
            item.setText(1, "" + nga.getConnectedInportIndices());
            item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "connected outports");
            item.setText(1, "" + nga.getConnectedOutportIndices());
            item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "start node stack");
            item.setText(1, nga.getStartNodeStackAsString());
            item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "end node stack");
            item.setText(1, nga.getEndNodeStackAsString());
            item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "role");
            item.setText(1, nga.getRole());
            item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "status");
            String status = nga.getError();
            item.setText(1, status == null ? "ok" : status);
        }
        m_info.setText("Node Annotation");
        for (int i = 0; i < m_table.getColumnCount(); i++) {
            m_table.getColumn(i).pack();
        }
    }

    /*
     *  Put info about workflow variables into table.
     */
    private void updateVariableTable(final NodeContainer nc) {
        assert Display.getCurrent().getThread() == Thread.currentThread();
        // display swt table
        ((StackLayout)m_stackPanel.getLayout()).topControl = m_table;
        m_stackPanel.layout();
        // Initialize table
        m_table.removeAll();
        for (TableColumn tc : m_table.getColumns()) {
            tc.dispose();
        }
        String[] titles = {"Variable", "Value"};
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(m_table, SWT.NONE);
            column.setText(titles[i]);
        }
        // retrieve variables
        Collection<FlowVariable> fvs;
        if ((nc instanceof SingleNodeContainer) || nc.getNrOutPorts() > 0) {
            // for normal nodes port 0 is available (hidden variable OutPort!)
            FlowObjectStack fos = nc.getOutPort(0).getFlowObjectStack();
            if (fos != null) {
                fvs = fos.getAvailableFlowVariables(org.knime.core.node.workflow.FlowVariable.Type.values()).values();
            } else {
                fvs = null;
            }
            m_info.setText("Node Variables");
        } else {
            // no output port on metanode - display workflow variables
            fvs = ((WorkflowManager)nc).getWorkflowVariables();
            m_info.setText("Metanode Variables");
        }
        if (fvs != null) {
            // update content
            for (FlowVariable fv : fvs) {
                TableItem item = new TableItem(m_table, SWT.NONE);
                item.setText(0, fv.getName());
                item.setText(1, fv.getValueAsString());
            }
        }
        for (int i = 0; i < m_table.getColumnCount(); i++) {
            m_table.getColumn(i).pack();
        }
    }

    /*
     *  Put info about node settings into table.
     */
    private void updateSettingsTable(final NodeContainer nc, final boolean showAll) {
        assert Display.getCurrent().getThread() == Thread.currentThread();
        // display swt table
        ((StackLayout)m_stackPanel.getLayout()).topControl = m_table;
        m_stackPanel.layout();
        m_info.setText("Node Configuration");
        // retrieve settings
        NodeSettings settings = new NodeSettings("");
        try {
            nc.getParent().saveNodeSettings(nc.getID(), settings);
        } catch (InvalidSettingsException ise) {
            // never happens.
        }
        // and put them into the table
        m_table.removeAll();
        for (TableColumn tc : m_table.getColumns()) {
            tc.dispose();
        }
        String[] titles = {"Key", "Value"};
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(m_table, SWT.NONE);
            column.setText(titles[i]);
        }
        // add information about plugin and version to list (in show all/expert mode only)
        if ((nc instanceof NativeNodeContainer) && showAll) {
            NativeNodeContainer nnc = (NativeNodeContainer)nc;

            TableItem item4 = new TableItem(m_table, SWT.NONE);
            item4.setText(0, "Node's feature name");
            item4.setText(1, nnc.getNodeAndBundleInformation().getFeatureName().orElse("?"));

            TableItem item5 = new TableItem(m_table, SWT.NONE);
            item5.setText(0, "Node's feature symbolic name");
            item5.setText(1, nnc.getNodeAndBundleInformation().getFeatureSymbolicName().orElse("?"));

            TableItem item6 = new TableItem(m_table, SWT.NONE);
            item6.setText(0, "Node's feature version (last saved with)");
            item6.setText(1, nnc.getNodeAndBundleInformation().getFeatureVersion().map(v -> v.toString()).orElse("?"));

            TableItem item1 = new TableItem(m_table, SWT.NONE);
            item1.setText(0, "Node's plug-in name");
            item1.setText(1, nnc.getNodeAndBundleInformation().getBundleName().orElse("?"));

            TableItem item2 = new TableItem(m_table, SWT.NONE);
            item2.setText(0, "Node's plug-in symbolic name");
            item2.setText(1, nnc.getNodeAndBundleInformation().getBundleSymbolicName().orElse("?"));

            TableItem item3 = new TableItem(m_table, SWT.NONE);
            item3.setText(0, "Node's plug-in version (last saved with)");
            item3.setText(1, nnc.getNodeAndBundleInformation().getBundleVersion().map(v -> v.toString()).orElse("?"));
        }
        // add settings to table
        Stack<Pair<Iterator<String>, ConfigBase>> stack = new Stack<Pair<Iterator<String>, ConfigBase>>();
        Iterator<String> it = settings.keySet().iterator();
        if (it.hasNext()) {
            stack.push(new Pair<Iterator<String>, ConfigBase>(it, settings));
        }
        while (!stack.isEmpty()) {
            String key = stack.peek().getFirst().next();
            int depth = stack.size();
            boolean noexpertskip = (depth <= 1);
            AbstractConfigEntry ace = stack.peek().getSecond().getEntry(key);
            if (!stack.peek().getFirst().hasNext()) {
                stack.pop();
            }
            if (ace.getType().equals(ConfigEntries.config)) {
                // it's another Config entry, push on stack!
                String val = ace.toStringValue();
                if ((!val.endsWith("_Internals")) || showAll) {
                    Iterator<String> it2 = ((ConfigBase)ace).iterator();
                    if (it2.hasNext()) {
                        stack.push(new Pair<Iterator<String>, ConfigBase>(it2, (ConfigBase)ace));
                    }
                } else {
                    noexpertskip = true;
                }
            }
            // in both cases, we report its value
            if ((!noexpertskip) || showAll) {
                String value = ace.toStringValue();
                TableItem item = new TableItem(m_table, SWT.NONE);
                char[] indent = new char[depth - 1];
                Arrays.fill(indent, '_');
                item.setText(0, new String(indent) + key);
                item.setText(1, value != null ? value : "null");
            }
        }
        for (int i = 0; i < m_table.getColumnCount(); i++) {
            m_table.getColumn(i).pack();
        }
    }

    /*
     *  Put (static and simple) content of one output port table into table.
     */
    private void updateDataTable(final NodeContainer nc, final int port) {
        assert Display.getCurrent().getThread() == Thread.currentThread();
        // display data table
        ((StackLayout)m_stackPanel.getLayout()).topControl = m_tableViewPanel;
        m_stackPanel.layout();

        m_info.setText("Port Output");
        m_tableView.setDataTable(null);
        // check if we can display something at all:
        int index = port;
        if (nc instanceof SingleNodeContainer) {
            index++; // we don't care about (hidden) variable OutPort
        }
        if (nc.getNrOutPorts() <= index) {
            // no (real) port available
            m_errorLabel.setText("No output ports");
            ((StackLayout)m_stackPanel.getLayout()).topControl = m_errorLabel;
            m_stackPanel.layout();
            return;
        }
        NodeOutPort nop = nc.getOutPort(index);
        PortObject po = nop.getPortObject();
        if ((po == null) || !(po instanceof BufferedDataTable)) {
            // no table in port - ignore.
            m_errorLabel.setText("Unknown or no PortObject");
            ((StackLayout)m_stackPanel.getLayout()).topControl = m_errorLabel;
            m_stackPanel.layout();
            return;
        }
        // retrieve table
        m_tableView.setDataTable((DataTable)po);
        m_tableView.repaint();
    }

    /** {@inheritDoc} */
    @Override
    public void setFocus() {
        // not needed (so far)
    }

    /** {@inheritDoc} */
    @Override
    public void changed(final LocationEvent event) {
        // nothing to do here
    }

    /** {@inheritDoc} */
    @Override
    public void changing(final LocationEvent event) {
        // nothing to do here
    }

    private final AtomicBoolean m_updateInProgress = new AtomicBoolean(false);

    /** {@inheritDoc} */
    @Override
    public void stateChanged(final NodeStateEvent state) {

        // if another state is waiting to be processed, simply return
        // and leave the work to the previously started thread. This
        // works because we are retrieving the current state information!
        if (m_updateInProgress.compareAndSet(false, true)) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    // let others know we are in the middle of processing
                    // this update - they will now need to start their own job.
                    m_updateInProgress.set(false);
                    updateNodeContainerInfo(m_lastNode);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent event) {
        if (event.getType() == Type.NODE_REMOVED) {
            // unregister from the node removed
            Object oldValue = event.getOldValue();
            if (oldValue instanceof NodeContainer) {
                NodeContainer oldNode = (NodeContainer)oldValue;
                if (oldNode.getID().equals(m_workflow.getID())) {
                    // our flow got removed (i.e. the editor or the workbench is closed)
                    disconnectFromWFM();
                    m_lastNode = null;
                    m_workflow = null;
                    m_parentWfm = null;
                    updateNCinfoInSWT(null);
                    return;
                }
                if (oldNode.getID().equals(m_lastNode)) {
                    oldNode.removeNodeStateChangeListener(this);
                    m_pinButton.setChecked(false); // pinned node is gone: unpin
                }
            }
            return;
        }

        if (!m_pinned || (m_pinned && !m_branchLocked)) {
            return;
        }
        Type type = event.getType();
        if (type == Type.CONNECTION_ADDED) {
            ConnectionContainer cc = (ConnectionContainer)event.getNewValue();
            if (cc.getSource().equals(m_lastNode)) {
                if (m_workflow.getOutgoingConnectionsFor(m_lastNode).size() == 1) {
                    if (m_workflow.containsNodeContainer(cc.getDest())) { // could be an outgoing metanode connection
                        // first connection: follow this new branch extension
                        updateNCinfoInSWT(cc.getDest());
                    }
                }
            }
        } else if (type == Type.CONNECTION_REMOVED) {
            ConnectionContainer cc = (ConnectionContainer)event.getOldValue();
            if (cc.getDest().equals(m_lastNode)) {
                if (m_workflow.containsNodeContainer(m_lastNode)) {
                    if (m_workflow.getOutgoingConnectionsFor(m_lastNode).size() == 0) {
                        updateNCinfoInSWT(cc.getSource());
                    }
                } else {
                    updateNCinfoInSWT(cc.getSource());
                }
            }
        }  else {
            return;
        }
    }

    private void updateNCinfoInSWT(final NodeID id) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                updateNodeContainerInfo(id);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveState(final IMemento memento) {
        super.saveState(memento);
        memento.putInteger("org.knime.workbench.nodeoutportview.version", 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IViewSite site, final IMemento memento) throws PartInitException {
        super.init(site, memento);
        if (memento == null) {
            return;
        }
        Integer ver = memento.getInteger("org.knime.workbench.nodeoutportview.version");
        if (ver != null && ver == 1) {
            // fully restoring the state requires access to the editor - which may not be initiated yet.
        }
    }
}
