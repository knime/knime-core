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
 *   21.03.2012 (mb): created
 */
package org.knime.workbench.nodemonitorview;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.util.MutableBoolean;
import org.knime.core.util.Pair;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/** An Eclipse View showing the variables of the currently
 * selected node.
 * 
 * @author M. Berthold, KNIME.com AG
 */
public class VariableMonitorView extends ViewPart
                              implements ISelectionListener, LocationListener,
                                         NodeStateChangeListener {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            VariableMonitorView.class);

    private Text m_title;
    private Text m_state;
    private Table m_table;
    
    private IStructuredSelection m_lastSelection;
    private NodeContainer m_lastNode;

    private MutableBoolean m_showVariables = new MutableBoolean(true);
    private MutableBoolean m_showSettings = new MutableBoolean(false);
    private MutableBoolean m_expertMode = new MutableBoolean(false);

    /**
     * The Constructor.
     */
    public VariableMonitorView() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public void createPartControl(final Composite parent) {
        getViewSite().getPage().addSelectionListener(this);
        // Toolbar
        IToolBarManager toolbarMGR
                    = getViewSite().getActionBars().getToolBarManager();
        final RetargetAction actionFilter
             = new RetargetAction("Vars", "Variables", IAction.AS_CHECK_BOX);
        actionFilter.setImageDescriptor(ImageDescriptor.createFromFile(
                this.getClass(), "viewVars.png"));
        actionFilter.setChecked(m_showVariables.booleanValue());
        actionFilter.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                m_showVariables.setValue(actionFilter.isChecked());
                updateNodeContainerInfo(m_lastNode);
            }
        });
        actionFilter.setEnabled(true);
        toolbarMGR.add(actionFilter);
        final RetargetAction actionFilter2
             = new RetargetAction("Conf", "Settings", IAction.AS_CHECK_BOX);
        actionFilter2.setImageDescriptor(ImageDescriptor.createFromFile(
                this.getClass(), "viewSettings.png"));
        actionFilter2.setChecked(m_showSettings.booleanValue());
        actionFilter2.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                m_showSettings.setValue(actionFilter2.isChecked());
                updateNodeContainerInfo(m_lastNode);
            }
        });
        actionFilter2.setEnabled(true);
        toolbarMGR.add(actionFilter2);
        final RetargetAction actionFilter3
              = new RetargetAction("Expert", "Show All", IAction.AS_CHECK_BOX);
        actionFilter3.setImageDescriptor(ImageDescriptor.createFromFile(
                this.getClass(), "viewAll.png"));
        actionFilter3.setChecked(m_expertMode.booleanValue());
        actionFilter3.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                m_expertMode.setValue(actionFilter3.isChecked());
                updateNodeContainerInfo(m_lastNode);
            }
        });
        actionFilter3.setEnabled(true);
        toolbarMGR.add(actionFilter3);
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
        if (structSel.equals(m_lastSelection)) {
            // selection hasn't changed - return.
            return;
        }
        m_lastSelection = structSel;
        // Nothing selected
        if (structSel.size() < 1) {
            m_title.setText("");
            m_state.setText("no node selected");            
            m_table.removeAll();
            return;
        }
        if (structSel.size() > 1) {
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
        assert Display.getCurrent().getThread() == Thread.currentThread();
        if ((m_lastNode != null) && (m_lastNode != nc)) {
            m_lastNode.removeNodeStateChangeListener(
                                   VariableMonitorView.this);
        }
        if ((m_lastNode == null) || (m_lastNode != nc)) {
            m_lastNode = nc;
            nc.addNodeStateChangeListener(VariableMonitorView.this);
        }
        m_title.setText(nc.getName() + "  (" + nc.getID() + ")");
        m_state.setText(nc.getState().toString());
        m_table.removeAll();
        if (m_showVariables.booleanValue()) {
            updateVariableTable(nc);
        }
        if (m_showSettings.booleanValue()) {
            updateSettingsTable(nc);
        }
        for (int i = 0; i < m_table.getColumnCount(); i++) {
            m_table.getColumn(i).pack();
        }
    }

    /*
     *  Put info about workflow variables into table.
     */
    private void updateVariableTable(final NodeContainer nc) {
        assert Display.getCurrent().getThread() == Thread.currentThread();
        // retrieve variables
        FlowObjectStack fos = nc.getOutPort(0).getFlowObjectStack();
        Map<String, FlowVariable> vars
                              = fos.getAvailableFlowVariables();
        // and update table
        for (FlowVariable fv : vars.values()) {
            TableItem item = new TableItem(m_table, SWT.NONE);
            item.setText(0, fv.getName());
            item.setText(1, fv.getValueAsString());
        }
    }

    /*
     *  Put info about node settings into table.
     */
    private void updateSettingsTable(final NodeContainer nc) {
        assert Display.getCurrent().getThread() == Thread.currentThread();
        // retrieve settings
        NodeSettings settings = new NodeSettings("");
        try {
            nc.getParent().saveNodeSettings(nc.getID(), settings);
        } catch (InvalidSettingsException ise) {
            // never happens.
        }
        // and put them into the table
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
                if ((!val.endsWith("_Internals"))
                                || m_expertMode.booleanValue()) {
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
            if ((!noexpertskip) || m_expertMode.booleanValue()) {
                String value = ace.toStringValue();
                TableItem item = new TableItem(m_table, SWT.NONE);
                char[] indent = new char[depth - 1];
                Arrays.fill(indent, '_');
                item.setText(0, new String(indent) + key);
                item.setText(1, value);
            }
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
