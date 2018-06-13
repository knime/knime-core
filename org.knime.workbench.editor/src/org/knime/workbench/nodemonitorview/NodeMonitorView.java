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
 *   21.03.2012 (mb): created
 */
package org.knime.workbench.nodemonitorview;

import static org.knime.core.ui.wrapper.Wrapper.unwrapNC;
import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SingleNodeContainerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.nodemonitorview.NodeMonitorTable.LoadingFailedException;

/**
 * An Eclipse View showing the interna of the currently selected (meta)node.
 *
 * @author M. Berthold, KNIME.com AG
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeMonitorView extends ViewPart implements ISelectionListener, LocationListener, NodeStateChangeListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeMonitorView.class);

    private Text m_title;

    private Text m_state;

    private Label m_info;

    private ComboViewer m_portIndex;

    private Table m_table;

    private Button m_loadButton;

    private AtomicInteger m_loadButtonPressedCount = new AtomicInteger(0);

    private IStructuredSelection m_lastSelection;

    private IStructuredSelection m_lastSelectionWhilePinned;

    private NodeContainerUI m_lastNode;

    private boolean m_pinned = false;

    private NodeMonitorTable m_currentMonitorTable;

    private enum DISPLAYOPTIONS {
            VARS, SETTINGS, ALLSETTINGS, TABLE, TIMER, GRAPHANNOTATIONS
    }

    private DISPLAYOPTIONS m_choice = DISPLAYOPTIONS.VARS;

    /** {@inheritDoc} */
    @Override
    public void createPartControl(final Composite parent) {
        // Toolbar
        IToolBarManager toolbarMGR = getViewSite().getActionBars().getToolBarManager();
        // create button which allows to "pin" selection:
        final RetargetAction pinButton =
            new RetargetAction("PinView", "Pin view to selected node", IAction.AS_CHECK_BOX);
        pinButton.setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(), "icons/pin.png"));
        pinButton.setChecked(m_pinned);
        pinButton.addPropertyChangeListener(event -> {
            if (pinButton.isChecked()) {
                m_pinned = true;
                m_lastSelectionWhilePinned = m_lastSelection;
            } else {
                m_pinned = false;
                selectionChanged(null, m_lastSelectionWhilePinned);
            }
        });
        pinButton.setEnabled(true);
        toolbarMGR.add(pinButton);
        toolbarMGR.add(new Separator());
        // configure drop down menu
        IMenuManager dropDownMenu = getViewSite().getActionBars().getMenuManager();
        // drop down menu entry for outport table:
        final RetargetAction menuentrytable =
            new RetargetAction("OutputTable", "Show Output Table", IAction.AS_RADIO_BUTTON);
        menuentrytable.setChecked(DISPLAYOPTIONS.TABLE.equals(m_choice));
        menuentrytable.addPropertyChangeListener(event -> {
            if (menuentrytable.isChecked()) {
                m_choice = DISPLAYOPTIONS.TABLE;
                updateNodeContainerInfo(m_lastNode);
            }
        });
        menuentrytable.setEnabled(true);
        dropDownMenu.add(menuentrytable);
        // drop down menu entry for node variables:
        final RetargetAction dropdownmenuvars =
            new RetargetAction("NodeVariables", "Show Variables", IAction.AS_RADIO_BUTTON);
        dropdownmenuvars.setChecked(DISPLAYOPTIONS.VARS.equals(m_choice));
        dropdownmenuvars.addPropertyChangeListener(event -> {
            if (dropdownmenuvars.isChecked()) {
                m_choice = DISPLAYOPTIONS.VARS;
                updateNodeContainerInfo(m_lastNode);
            }
        });
        dropdownmenuvars.setEnabled(true);
        dropDownMenu.add(dropdownmenuvars);
        // drop down menu entry for configuration/settings:
        final RetargetAction menuentrysettings =
            new RetargetAction("NodeConf", "Show Configuration", IAction.AS_RADIO_BUTTON);
        menuentrysettings.setChecked(DISPLAYOPTIONS.SETTINGS.equals(m_choice));
        menuentrysettings.addPropertyChangeListener(event -> {
            if (menuentrysettings.isChecked()) {
                m_choice = DISPLAYOPTIONS.SETTINGS;
                updateNodeContainerInfo(m_lastNode);
            }
        });
        menuentrysettings.setEnabled(true);
        dropDownMenu.add(menuentrysettings);
        // drop down menu entry for configuration/settings:
        final RetargetAction menuentryallsettings =
            new RetargetAction("NodeConfAll", "Show Entire Configuration", IAction.AS_RADIO_BUTTON);
        menuentryallsettings.setChecked(DISPLAYOPTIONS.ALLSETTINGS.equals(m_choice));
        menuentryallsettings.addPropertyChangeListener(event -> {
            if (menuentryallsettings.isChecked()) {
                m_choice = DISPLAYOPTIONS.ALLSETTINGS;
                updateNodeContainerInfo(m_lastNode);
            }
        });
        menuentryallsettings.setEnabled(true);
        dropDownMenu.add(menuentryallsettings);
        // drop down menu entry for node timer
        final RetargetAction menuentrynodetimer =
            new RetargetAction("NodeTimer", "Show Node Timing Information", IAction.AS_RADIO_BUTTON);
        menuentrynodetimer.setChecked(DISPLAYOPTIONS.TIMER.equals(m_choice));
        menuentrynodetimer.addPropertyChangeListener(event -> {
            if (menuentrynodetimer.isChecked()) {
                m_choice = DISPLAYOPTIONS.TIMER;
                updateNodeContainerInfo(m_lastNode);
            }
        });
        menuentrynodetimer.setEnabled(true);
        dropDownMenu.add(menuentrynodetimer);
        // drop down menu entry for node graph annotations
        final RetargetAction menuentrygraphannotations =
            new RetargetAction("NodeGraphAnno", "Show Graph Annotations", IAction.AS_RADIO_BUTTON);
        menuentrygraphannotations.setChecked(DISPLAYOPTIONS.GRAPHANNOTATIONS.equals(m_choice));
        menuentrygraphannotations.addPropertyChangeListener(event -> {
            if (menuentrygraphannotations.isChecked()) {
                m_choice = DISPLAYOPTIONS.GRAPHANNOTATIONS;
                updateNodeContainerInfo(m_lastNode);
            }
        });
        menuentrygraphannotations.setEnabled(true);
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
        // Node State:
        Label statelabel = new Label(parent, SWT.NONE);
        statelabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        statelabel.setText("State: ");
        m_state = new Text(parent, SWT.BORDER);
        m_state.setEditable(false);
        m_state.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        m_state.setText("n/a.");
        // Panel for currently displayed information (some information
        // providers will add more elements to this):
        Composite infoPanel = new Composite(parent, SWT.NONE);
        GridData infoGrid = new GridData(SWT.FILL, SWT.TOP, true, false);
        infoGrid.horizontalSpan = 2;
        infoPanel.setLayoutData(infoGrid);
        GridLayoutFactory.swtDefaults().numColumns(4).applyTo(infoPanel);
        m_info = new Label(infoPanel, SWT.NONE);
        m_info.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        m_info.setText("n/a.                        ");
        m_portIndex = new ComboViewer(infoPanel);
        m_portIndex.add(new String[]{"port 0", "port 1", "port 2"});
        m_portIndex.getCombo().setEnabled(false);
        m_portIndex.addSelectionChangedListener(event -> {
            ISelection sel = event.getSelection();
            try {
                int newIndex = Integer.parseInt(sel.toString().substring(5).replace(']', ' ').trim());
                resetMonitorTable();
                switch (m_choice) {
                   case TABLE:
                        m_currentMonitorTable = new MonitorDataTable(newIndex);
                        break;
                }
                if (wraps(m_lastNode, NodeContainer.class)) {
                    //already load the data in case of an ordinary node container
                    loadAndSetupMonitorTableForOrdinaryNC();
                } else {
                    m_currentMonitorTable.updateControls(m_loadButton, m_portIndex.getCombo(), 0);
                }
            } catch (NumberFormatException nfe) {
                // ignore.
            }
        });
        m_loadButton = new Button(infoPanel, SWT.PUSH);
        m_loadButton.setText("   Manually load data   ");
        m_loadButton.setToolTipText("Manual trigger to load the data.");
        m_loadButton.setEnabled(false);
        m_loadButton.addSelectionListener(new LoadButtonListener());

        // Table:
        m_table = new Table(parent, SWT.MULTI | SWT.BORDER | SWT.VIRTUAL);
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
        NodeContainerUI cont = m_lastNode;
        if (cont != null) {
            cont.removeNodeStateChangeListener(this);
            m_lastNode = null;
        }
        getViewSite().getPage().removeSelectionListener(this);
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
            m_state.setText("more than one element selected");
            m_table.removeAll();
            return;
        }
        // retrieve first (and only!) selection:
        Iterator<?> selIt = structSel.iterator();
        Object sel = selIt.next();
        //
        if (sel instanceof NodeContainerEditPart) {
            // a NodeContainer was selected, display it's name and status
            NodeContainerUI nc = ((NodeContainerEditPart)sel).getNodeContainer();
            updateNodeContainerInfo(nc);
        } else {
            // unsupported selection
            if (m_lastNode != null) {
                m_lastNode.removeNodeStateChangeListener(NodeMonitorView.this);
                m_lastNode = null;
            }
            warningMessage("no node selected");
        }
    }

    private void warningMessage(final String message) {
        resetMonitorTable();
        if (m_lastNode != null) {
            if (message != null) {
                TableItem item = new TableItem(m_table, SWT.NONE);
                item.setText(0, message);
            }
        } else {
            m_title.setText("");
            m_state.setText(message);
        }
    }

    /* Update all visuals with information regarding new NodeContainer.
     * Also de-register previous node and register with the new one if
     * the underlying NC changed.
     */
    private void updateNodeContainerInfo(final NodeContainerUI nc) {
        if (nc == null) {
            return;
        }
        assert Display.getCurrent().getThread() == Thread.currentThread();
        resetMonitorTable();

        if ((m_lastNode != null) && (m_lastNode != nc)) {
            m_lastNode.removeNodeStateChangeListener(NodeMonitorView.this);
        }
        if ((m_lastNode == null) || (m_lastNode != nc)) {
            m_lastNode = nc;
            nc.addNodeStateChangeListener(NodeMonitorView.this);
        }
        m_title.setText(nc.getName() + "  (" + nc.getID() + ")");
        m_state.setText(nc.getNodeContainerState().toString());

        m_portIndex.getCombo().setEnabled(true);
        int nrPorts = nc.getNrOutPorts();
        if (nc instanceof SingleNodeContainerUI) {
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

        switch (m_choice) {
            case VARS:
                m_currentMonitorTable = new MonitorVariableTable();
                break;
            case SETTINGS:
            case ALLSETTINGS:
                m_currentMonitorTable = new MonitorSettingsTable(DISPLAYOPTIONS.ALLSETTINGS.equals(m_choice));
                break;
            case TABLE:
                m_currentMonitorTable = new MonitorDataTable(0);
                break;
            case TIMER:
                m_currentMonitorTable = new MonitorTimerTable();
                break;
            case GRAPHANNOTATIONS:
                m_currentMonitorTable = new MonitorGraphAnnotationTable();
                break;
            default:
                throw new AssertionError("Unhandled switch case: " + m_choice);
        }

        m_currentMonitorTable.updateInfoLabel(m_info);
        if (wraps(nc, NodeContainer.class)) {
            //already load the data in case of an ordinary node monitor
            loadAndSetupMonitorTableForOrdinaryNC();
        } else {
            m_currentMonitorTable.updateControls(m_loadButton, m_portIndex.getCombo(), 0);
        }
    }

    /*
     * Loads and setups the table if ordinary node container is given.
     */
    private void loadAndSetupMonitorTableForOrdinaryNC() {
        assert unwrapNC(m_lastNode) != null;
        try {
            //already load the data in case of an ordinary node monitor
            m_currentMonitorTable.loadTableData(m_lastNode, unwrapNC(m_lastNode), 0);
            m_currentMonitorTable.setupTable(m_table);
        } catch (LoadingFailedException e) {
            warningMessage(e.getMessage());
        }
    }

    private void resetMonitorTable() {
        m_loadButton.setEnabled(false);
        m_loadButton.setText("   Manually load data   ");

        m_loadButtonPressedCount.set(0);

        if (m_currentMonitorTable != null) {
            m_currentMonitorTable.dispose(m_table);
        }

        m_table.removeAll();
        for (TableColumn tc : m_table.getColumns()) {
            tc.dispose();
        }

        m_portIndex.getCombo().setEnabled(false);
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

    private class LoadButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(final SelectionEvent e) {
            try {
                final AtomicReference<LoadingFailedException> lfe =
                    new AtomicReference<NodeMonitorTable.LoadingFailedException>();
                PlatformUI.getWorkbench().getProgressService().busyCursorWhile((monitor) -> {
                    monitor.beginTask("Loading data ...", 100);
                    try {
                        m_currentMonitorTable.loadTableData(m_lastNode, Wrapper.unwrapNCOptional(m_lastNode).orElse(null),
                            m_loadButtonPressedCount.get());
                    } catch (LoadingFailedException e1) {
                        lfe.set(e1);
                    }
                });
                if (lfe.get() != null) {
                    throw lfe.get();
                }
                if (m_loadButtonPressedCount.get() == 0) {
                    m_currentMonitorTable.setupTable(m_table);
                }
                m_currentMonitorTable.updateControls(m_loadButton, m_portIndex.getCombo(),
                    m_loadButtonPressedCount.get() + 1);
                m_loadButtonPressedCount.incrementAndGet();
            } catch (LoadingFailedException ex) {
                warningMessage(ex.getMessage());
            } catch (InvocationTargetException e1) {
                warningMessage(e1.getCause().getMessage());
                LOGGER.warn(e1.getCause());
            } catch (InterruptedException e1) {
                warningMessage(e1.getMessage());
                LOGGER.warn(e1);
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void widgetDefaultSelected(final SelectionEvent e) {
        }

    }

}
