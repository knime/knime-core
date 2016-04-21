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
 * -------------------------------------------------------------------
 *
 * History
 *   11.01.2006 (Florian Georg): created
 */
package org.knime.workbench.repository.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.core.nodeprovider.NodeProvider;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 * Contribution Item within the RepositoryView. It's essentially the text box to type the search query.
 *
 * @author Martin Horn, University of Konstanz
 */
class SearchQueryContributionItem extends ControlContribution implements KeyListener {
    private final TreeViewer m_viewer;

    private TextualViewFilter m_filter;

    private Text m_text;

    private Runnable m_callback = null;

    private final boolean m_liveUpdate;

    private static final int KEY_UP = 16777217;

    private static final int KEY_DOWN = 16777218;

    //stores the tree items currently shown in the viewer
    // -> doesn't need to be determined again if the query string didn't change
    private TreeItem[] m_treeItems;

    /**
     * Flag that indicates whether the thread that delays
     * the actual processing of the search query is running.
     */
    private final AtomicBoolean m_isDelayThreadRunning = new AtomicBoolean(false);

    /**
     * Flag that indicates whether the 'delay thread' should continue
     * delaying the processing of the search query a bit further.
     */
    private final AtomicBoolean m_continueDelay = new AtomicBoolean(true);

    /**
     * The last key that was entered by the user.
     */
    private char m_lastKey;

    /**
     * Delay for a triggered (by a key event) tree viewer update process before the actually update is performed.
     * This avoids unnecessary updates while typing the search query.
     */
    private static final long DELAY = 200;

    /**
     * Creates the contribution item.
     *
     * @param viewer The viewer that will be updated when the search query string has been changed. Furthermore, the
     *            first item of the (tree-)viewer will be selected when the key down/up is pressed.
     * @param filter The filter that will be updated (i.e. calling {@link TextualViewFilter#setQueryString(String)}.
     * @param liveUpdate Set to true if the filter should be updated on every key pressed. If false, it is only updated
     *            on pressing enter.
     */
    public SearchQueryContributionItem(final TreeViewer viewer, final TextualViewFilter filter, final boolean liveUpdate) {
        super("org.knime.workbench.repository.view.SearchQueryContributionItem");
        m_viewer = viewer;
        m_filter = filter;
        m_liveUpdate = liveUpdate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createControl(final Composite parent) {
        m_text = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.BORDER_SOLID);
        m_text.addKeyListener(this);
        m_text.setToolTipText("Filter contents");
        return m_text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeWidth(final Control control) {
        return Math.max(super.computeWidth(control), 150);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void keyPressed(final KeyEvent e) {
        if (e.keyCode == KEY_UP || e.keyCode == KEY_DOWN) {
            //if down key is pressed, select an element in the node list

            if (m_treeItems == null) {
                m_treeItems = m_viewer.getTree().getItems();
                if (m_treeItems[0].getItemCount() != 0) {
                    //tree is an actual tree and we have to retrieve all leaves by traversing the tree
                    List<TreeItem> tmp = new ArrayList<TreeItem>();
                    getLeavesRecursively(m_treeItems, tmp);
                    m_treeItems = tmp.toArray(new TreeItem[tmp.size()]);

                }
            }
            ITreeSelection hu = m_viewer.getStructuredSelection();

            int i = -1;
            if (!hu.isEmpty()) {
                //checks whether there is an element already selected
                Object selected = hu.getFirstElement();
                for (i = 0; i < m_treeItems.length; i++) {
                    if (m_treeItems[i].getData() == selected) {
                        break;
                    }
                }
            }

            //if there wasn't any element selected already, select the first one,
            //otherwise the one above/below
            int index = (i == -1 ? 0 : (e.keyCode == KEY_UP ? i - 1 : i + 1));
            if (index >= 0 && index < m_treeItems.length) {
                m_viewer.setSelection(new StructuredSelection(m_treeItems[index].getData()), true);
            }
        } else if (e.character == SWT.CR) {
            //enter was pressed, insert the selected node, if there is one selected
            //and select the whole search string (such that the user can just further type text in
            createNode(((IStructuredSelection)m_viewer.getSelection()).getFirstElement());
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    m_text.setFocus();
                }
            });
            m_text.selectAll();
        } else if(e.character == SWT.ESC) {
            //give focus to the workflow editor
            PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage().getActiveEditor().setFocus();
        }
    }

    private void getLeavesRecursively(final TreeItem[] items, final List<TreeItem> leaves) {
        for(TreeItem item : items) {
            if(item.getItemCount() > 0) {
                getLeavesRecursively(item.getItems(), leaves);
            } else {
                leaves.add(item);
            }
        }
        return;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void keyReleased(final KeyEvent e) {
        //don't update the tree if the enter or down/up keys are used
        if (e.keyCode == KEY_DOWN || e.keyCode == KEY_UP || e.character == SWT.CR) {
            return;
        }

        m_lastKey = e.character;

        if (!m_isDelayThreadRunning.getAndSet(true)) {
            //if the thread to delay the actually
            //processing of the search query is not running, start a new one
            delayQueryProcessing();
        } else {
            //if delay thread is already running
            //and a new key event arrives,
            //just continue delaying the actual
            //processing of the search query a bit further
            m_continueDelay.set(true);
        }
    }

    private void delayQueryProcessing() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //delay the processing a bit
                try {
                    while (m_continueDelay.getAndSet(false)) {
                        Thread.sleep(DELAY);
                    }
                    //do the actual work in the UI thread
                    Display.getDefault().asyncExec(() -> updateRepositoryTree());
                } catch (InterruptedException ex) {
                    // do nothing, just return
                }
            }
        }).start();
    }

    /**
     * @return the text
     */
    protected Text getText() {
        return m_text;
    }


    /**
     * @param a callback that is called whenever the search query has changed
     */
    void setQueryChangedCallback(final Runnable c) {
        m_callback  = c;
    }

    /**
     * Replaces the currently used filter used to update (i.e. {@link TextualViewFilter#setQueryString(String)}).
     * @param filter the filter
     */
    void setFilter(final TextualViewFilter filter) {
        m_treeItems = null;
        m_filter = filter;
    }

    /**
     * Helper to insert a node into the workflow editor.
     *
     * @param event
     */
    private void createNode(final Object o) {
        if (o instanceof NodeTemplate) {
            NodeTemplate tmplt = (NodeTemplate)o;
            NodeFactory<? extends NodeModel> nodeFact;
            try {
                nodeFact = tmplt.createFactoryInstance();
            } catch (Exception e) {
                NodeLogger.getLogger(SearchQueryContributionItem.class)
                    .error("Unable to instantiate the selected node " + tmplt.getFactory().getName(), e);
                return;
            }
            boolean added = NodeProvider.INSTANCE.addNode(nodeFact);
            if (added) {
                NodeUsageRegistry.addNode(tmplt);
            }
        }
        if (o instanceof MetaNodeTemplate) {
            MetaNodeTemplate mnt = (MetaNodeTemplate)o;
            NodeID metaNode = mnt.getManager().getID();
            NodeProvider.INSTANCE.addMetaNode(WorkflowManager.META_NODE_ROOT, metaNode);
        }
    }

    private void updateRepositoryTree() {
        m_continueDelay.set(true);
        m_isDelayThreadRunning.set(false);

        //clear the tree items since the search query possibly has been changed
        m_treeItems = null;

        boolean shouldExpand = true;
        boolean update = m_liveUpdate;

        final String searchString;
        if (m_lastKey == SWT.ESC) {
            m_text.setText("");
            searchString = "";
            update = true;
        } else {
            searchString = m_text.getText();
        }
        m_filter.setQueryString(searchString);
        if (m_callback != null) {
            m_callback.run();
        }

        TreeViewerUpdater.collapseAndUpdate(m_viewer, update || searchString.isEmpty(), searchString.isEmpty(),
            shouldExpand || !searchString.isEmpty());
        if (searchString.isEmpty()) {
            m_viewer.collapseAll();
        }
    }
}
