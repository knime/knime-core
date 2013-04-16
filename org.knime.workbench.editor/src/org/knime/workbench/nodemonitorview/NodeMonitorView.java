/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   21.03.2012 (mb): created
 */
package org.knime.workbench.nodemonitorview;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.Pair;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/** An Eclipse View showing the interna of the currently
 * selected (meta)node.
 *
 * @author M. Berthold, KNIME.com AG
 */
public class NodeMonitorView extends ViewPart
                              implements ISelectionListener, LocationListener,
                                         NodeStateChangeListener {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            VariableMonitorView.class);

    private Text m_title;
    private Text m_state;
    private Composite m_infoPanel;
    private Label m_info;
    private ComboViewer m_portIndex;
    private Table m_table;

    private IStructuredSelection m_lastSelection;
    private IStructuredSelection m_lastSelectionWhilePinned;
    private NodeContainer m_lastNode;
    private boolean m_pinned = false;

    private enum DISPLAYOPTIONS { VARS, SETTINGS, ALLSETTINGS, TABLE };
    private DISPLAYOPTIONS m_choice = DISPLAYOPTIONS.VARS;

    /**
     * The Constructor.
     */
    public NodeMonitorView() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public void createPartControl(final Composite parent) {
        // Toolbar
        IToolBarManager toolbarMGR
                    = getViewSite().getActionBars().getToolBarManager();
        // create button which allows to "pin" selection:
        final RetargetAction pinButton
             = new RetargetAction("PinView", "Pin view to selected node",
                                      IAction.AS_CHECK_BOX);
        pinButton.setImageDescriptor(ImageDescriptor.createFromFile(
                this.getClass(), "icons/pin.png"));
        pinButton.setChecked(m_pinned);
        pinButton.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (pinButton.isChecked()) {
                    m_pinned = true;
                    m_lastSelectionWhilePinned = m_lastSelection;
                } else {
                    m_pinned = false;
                    selectionChanged(null, m_lastSelectionWhilePinned);
                }
            }
        });
        pinButton.setEnabled(true);
        toolbarMGR.add(pinButton);
        toolbarMGR.add(new Separator());
        // configure drop down menu
//        ImageDescriptor ddicon = ImageDescriptor.createFromFile(
//                this.getClass(), "icons/table.png");
        IMenuManager dropDownMenu
                          = getViewSite().getActionBars().getMenuManager();
        // drop down menu entry for outport table:
        final RetargetAction menuentrytable
                = new RetargetAction("OutputTable", "Show Output Table",
                                 IAction.AS_RADIO_BUTTON);
        menuentrytable.setChecked(DISPLAYOPTIONS.TABLE.equals(m_choice));
        menuentrytable.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (menuentrytable.isChecked()) {
                    m_choice = DISPLAYOPTIONS.TABLE;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        menuentrytable.setEnabled(true);
        dropDownMenu.add(menuentrytable);
        // drop down menu entry for node variables:
        final RetargetAction dropdownmenuvars
                = new RetargetAction("NodeVariables", "Show Variables",
                               IAction.AS_RADIO_BUTTON);
        dropdownmenuvars.setChecked(DISPLAYOPTIONS.VARS.equals(m_choice));
        dropdownmenuvars.addPropertyChangeListener(
                new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (dropdownmenuvars.isChecked()) {
                    m_choice = DISPLAYOPTIONS.VARS;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        dropdownmenuvars.setEnabled(true);
        dropDownMenu.add(dropdownmenuvars);
        // drop down menu entry for configuration/settings:
        final RetargetAction menuentrysettings
                = new RetargetAction("NodeConf", "Show Configuration",
                            IAction.AS_RADIO_BUTTON);
        menuentrysettings.setChecked(DISPLAYOPTIONS.SETTINGS.equals(m_choice));
        menuentrysettings.addPropertyChangeListener(
                new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (menuentrysettings.isChecked()) {
                    m_choice = DISPLAYOPTIONS.SETTINGS;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        menuentrysettings.setEnabled(true);
        dropDownMenu.add(menuentrysettings);
        // drop down menu entry for configuration/settings:
        final RetargetAction menuentryallsettings
                = new RetargetAction("NodeConfAll", "Show Entire Configuration",
                            IAction.AS_RADIO_BUTTON);
        menuentryallsettings.setChecked(DISPLAYOPTIONS.ALLSETTINGS.equals(m_choice));
        menuentryallsettings.addPropertyChangeListener(
                new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (menuentryallsettings.isChecked()) {
                    m_choice = DISPLAYOPTIONS.ALLSETTINGS;
                    updateNodeContainerInfo(m_lastNode);
                }
            }
        });
        menuentryallsettings.setEnabled(true);
        dropDownMenu.add(menuentryallsettings);
        // Content
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(parent);
        // Node Title:
        Label titlelabel = new Label(parent, SWT.NONE);
        titlelabel.setLayoutData(
                           new GridData(SWT.LEFT, SWT.CENTER, false, false));
        titlelabel.setText("Node: ");
        m_title = new Text(parent, SWT.BORDER);
        m_title.setEditable(false);
        m_title.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, false, false));
        m_title.setText("n/a.");
        // Node State:
        Label statelabel = new Label(parent, SWT.NONE);
        statelabel.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false));
        statelabel.setText("State: ");
        m_state = new Text(parent, SWT.BORDER);
        m_state.setEditable(false);
        m_state.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, false, false));
        m_state.setText("n/a.");
        // Panel for currently displayed information (some information
        // providers will add more elements to this):
        m_infoPanel = new Composite(parent, SWT.NONE);
        GridData infoGrid = new GridData(SWT.FILL, SWT.TOP, true, false);
        infoGrid.horizontalSpan = 2;
        m_infoPanel.setLayoutData(infoGrid);
        GridLayoutFactory.swtDefaults().numColumns(3).applyTo(m_infoPanel);
        m_info = new Label(m_infoPanel, SWT.NONE);
        m_info.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false));
        m_info.setText("n/a.                        ");
        m_portIndex = new ComboViewer(m_infoPanel);
        m_portIndex.add(new String[] {"port 0", "port 1", "port 2"});
        m_portIndex.getCombo().setEnabled(false);
        m_portIndex.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                ISelection sel = event.getSelection();
                try {
                    int newIndex = Integer.parseInt(sel.toString().substring(5).replace(']', ' ').trim());
                    updateDataTable(m_lastNode, newIndex);
                } catch (NumberFormatException nfe) {
                    // ignore.
                }
            }
        });
        // Table:
        m_table = new Table(parent, SWT.MULTI | SWT.BORDER);
        m_table.setLinesVisible(true);
        m_table.setHeaderVisible(true);
        GridData tableGrid = new GridData(SWT.FILL, SWT.FILL, true, true);
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        getViewSite().getPage().removeSelectionListener(this);
        super.dispose();
    }

    /**
     * The method updating the content of the monitor.
     *
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part,
            final ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }
        IStructuredSelection structSel = (IStructuredSelection)selection;
        if (m_pinned) {
            m_lastSelectionWhilePinned = structSel;
            return;
        }
        if (structSel.equals(m_lastSelection)) {
            // selection hasn't changed - return.
            return;
        }
        m_lastSelection = structSel;
        if (structSel.size() < 1) {
            // Nothing selected
            m_title.setText("");
            m_state.setText("no node selected");
            m_table.removeAll();
            return;
        }
        if (structSel.size() > 1) {
            // too many selected items
            m_title.setText("");
            m_state.setText("more than one element selected.");
            m_table.removeAll();
            return;
        }
        // retrieve first (and only!) selection:
        Iterator<?> selIt = structSel.iterator();
        Object sel = selIt.next();
        //
        if (sel instanceof NodeContainerEditPart) {
            // a NodeContainer was selected, display it's name and status
            NodeContainer nc = ((NodeContainerEditPart)sel).getNodeContainer();
            updateNodeContainerInfo(nc);
        } else {
            // unsupported selection
            m_title.setText("");
            m_state.setText("no info for '"
                            + sel.getClass().getSimpleName() + "'.");
            m_table.removeAll();
        }
    }

    /* Update all visuals with information regarding new NodeContainer.
     * Also de-register previous node and register with the new one if
     * the underlying NC changed.
     */
    private void updateNodeContainerInfo(final NodeContainer nc) {
        if (nc == null) {
            return;
        }
        m_portIndex.getCombo().setEnabled(false);
        assert Display.getCurrent().getThread() == Thread.currentThread();
        if ((m_lastNode != null) && (m_lastNode != nc)) {
            m_lastNode.removeNodeStateChangeListener(
                                   NodeMonitorView.this);
        }
        if ((m_lastNode == null) || (m_lastNode != nc)) {
            m_lastNode = nc;
            nc.addNodeStateChangeListener(NodeMonitorView.this);
        }
        m_title.setText(nc.getName() + "  (" + nc.getID() + ")");
        m_state.setText(nc.getState().toString());
        switch (m_choice) {
        case VARS:
            updateVariableTable(nc);
            break;
        case SETTINGS:
        case ALLSETTINGS:
            updateSettingsTable(nc,
                            DISPLAYOPTIONS.ALLSETTINGS.equals(m_choice));
            break;
        case TABLE:
            m_portIndex.getCombo().setEnabled(true);
            int nrPorts = nc.getNrOutPorts();
            if (nc instanceof SingleNodeContainer) {
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
            updateDataTable(nc, 0);
        }
    }

    /*
     *  Put info about workflow variables into table.
     */
    private void updateVariableTable(final NodeContainer nc) {
        assert Display.getCurrent().getThread() == Thread.currentThread();
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
        if ((nc instanceof SingleNodeContainer)
                || nc.getNrOutPorts() > 0) {
            // for normal nodes port 0 is available (hidden variable OutPort!)
            FlowObjectStack fos = nc.getOutPort(0).getFlowObjectStack();
            if (fos != null) {
                fvs = fos.getAvailableFlowVariables().values();
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
    private void updateSettingsTable(final NodeContainer nc,
                                     final boolean showAll) {
        assert Display.getCurrent().getThread() == Thread.currentThread();
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
        Stack<Pair<Iterator<String>, ConfigBase>> stack
                         = new Stack<Pair<Iterator<String>, ConfigBase>>();
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
                        stack.push(new Pair<Iterator<String>, ConfigBase>(
                                                        it2, (ConfigBase)ace));
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
        m_info.setText("Port Output");
        m_table.removeAll();
        for (TableColumn tc : m_table.getColumns()) {
            tc.dispose();
        }
        // check if we can display something at all:
        int index = port;
        if (nc instanceof SingleNodeContainer) {
            index++;  // we don't care about (hidden) variable OutPort
        }
        if (nc.getNrOutPorts() <= index) {
            // no (real) port available
            TableItem item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "No output ports");
            return;
        }
        NodeOutPort nop = nc.getOutPort(index);
        PortObject po = nop.getPortObject();
        if ((po == null) || !(po instanceof BufferedDataTable)) {
            // no table in port - ignore.
            TableItem item = new TableItem(m_table, SWT.NONE);
            item.setText(0, "Unknown or no PortObject");
            return;
        }
        // retrieve table
        BufferedDataTable bdt = (BufferedDataTable)po;
        TableColumn column = new TableColumn(m_table, SWT.NONE);
        column.setText("ID");
        for (int i = 0; i < bdt.getDataTableSpec().getNumColumns(); i++) {
            column = new TableColumn(m_table, SWT.NONE);
            column.setText(bdt.getDataTableSpec().getColumnSpec(i).getName());
        }
        int rowIndex = 0;
        Iterator<DataRow> rowIt = bdt.iteratorFailProve();
        while (rowIndex < 42 && rowIt.hasNext()) {
            DataRow thisRow = rowIt.next();
            TableItem item = new TableItem(m_table, SWT.NONE);
            item.setText(0, thisRow.getKey().getString());
            for (int i = 0; i < thisRow.getNumCells(); i++) {
                item.setText(i + 1, thisRow.getCell(i).toString());
            }
            rowIndex++;
        }
        for (int i = 0; i < m_table.getColumnCount(); i++) {
            m_table.getColumn(i).pack();
        }
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

}
