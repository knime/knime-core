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
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.EditorHistory;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.EditorUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.Pointer;
import org.knime.workbench.core.nodeprovider.NodeProvider;
import org.knime.workbench.core.nodeprovider.NodeProvider.EventListener;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.actions.AddAnnotationAction;
import org.knime.workbench.editor2.actions.CancelAction;
import org.knime.workbench.editor2.actions.CancelAllAction;
import org.knime.workbench.editor2.actions.CheckUpdateMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.CollapseMetaNodeAction;
import org.knime.workbench.editor2.actions.CopyAction;
import org.knime.workbench.editor2.actions.CutAction;
import org.knime.workbench.editor2.actions.DefaultOpenViewAction;
import org.knime.workbench.editor2.actions.DisconnectMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.ExecuteAction;
import org.knime.workbench.editor2.actions.ExecuteAllAction;
import org.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import org.knime.workbench.editor2.actions.ExpandMetaNodeAction;
import org.knime.workbench.editor2.actions.HideNodeNamesAction;
import org.knime.workbench.editor2.actions.LockMetaNodeAction;
import org.knime.workbench.editor2.actions.MetaNodeReconfigureAction;
import org.knime.workbench.editor2.actions.NodeConnectionContainerDeleteAction;
import org.knime.workbench.editor2.actions.OpenDialogAction;
import org.knime.workbench.editor2.actions.PasteAction;
import org.knime.workbench.editor2.actions.PasteActionContextMenu;
import org.knime.workbench.editor2.actions.PauseLoopExecutionAction;
import org.knime.workbench.editor2.actions.ResetAction;
import org.knime.workbench.editor2.actions.ResumeLoopAction;
import org.knime.workbench.editor2.actions.RevealMetaNodeTemplateAction;
import org.knime.workbench.editor2.actions.SaveAsMetaNodeTemplateAction;
import org.knime.workbench.editor2.actions.SetNodeDescriptionAction;
import org.knime.workbench.editor2.actions.StepLoopAction;
import org.knime.workbench.editor2.actions.ToggleFlowVarPortsAction;
import org.knime.workbench.editor2.commands.CreateNewConnectedMetaNodeCommand;
import org.knime.workbench.editor2.commands.CreateNewConnectedNodeCommand;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeAnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.view.actions.validators.FileStoreNameValidator;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.SyncExecQueueDispatcher;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.navigator.WorkflowEditorAdapter;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * This is the implementation of the Eclipse Editor used for editing a
 * <code>WorkflowManager</code> object. This also handles the basic GEF stuff
 * (command stack) and hooks into the workbench to provide actions etc. ...
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowEditor extends GraphicalEditor implements
        CommandStackListener, ISelectionListener, WorkflowListener,
        IResourceChangeListener, NodeStateChangeListener,
        NodePropertyChangedListener, ISaveablePart2, NodeUIInformationListener,
        EventListener, IPropertyChangeListener {

    /** Id as defined in plugin.xml. */
    public static final String ID = "org.knime.workbench.editor.WorkflowEditor";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditor.class);

    /** Clipboard name. */
    public static final String CLIPBOARD_ROOT_NAME = "clipboard";

    /**
     * The static clipboard for copy/cut/paste.
     */
    private static ClipboardObject clipboard;

    private static final Color BG_COLOR_WRITE_LOCK =
        new Color(null, 235, 235, 235);

    private static final Color BG_COLOR_DEFAULT =
        Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

    /** root model object (=editor input) that is handled by the editor. * */
    private WorkflowManager m_manager;

    /** the editor's action registry. */
    private ActionRegistry m_actionRegistry;

    /** the <code>EditDomain</code>. */
    private final DefaultEditDomain m_editDomain;

    /** the dirty state. */
    private boolean m_isDirty;

    /** List with the action ids that are associated to this editor. * */
    private List<String> m_editorActions;

    private GraphicalViewer m_graphicalViewer;

    // path to the workflow directory (that contains the workflow.knime file)
    private URI m_fileResource;

    /** If subworkflow editor, store the parent for saving. */
    private WorkflowEditor m_parentEditor;

    private NewOverviewOutlinePage m_overviewOutlinePage;

    private PropertySheetPage m_undoablePropertySheetPage;

    private final WorkflowSelectionTool m_selectionTool;

    /** whether the afterOpen method has been run already (disallow queuing
     * another runnable). */
    private boolean m_hasAfterOpenRun = false;
    /** A list of runnable to be run after the editor is initialized.
     * See also {@link #addAfterOpenRunnable(Runnable)} for details. */
    private List<Runnable> m_afterOpenRunnables;

    /** Indicates if this editor has been closed. */
    private boolean m_closed;

    private String m_manuallySetToolTip;

    /**
     * No arg constructor, creates the edit domain for this editor.
     */
    public WorkflowEditor() {
        super();

        LOGGER.debug("Creating WorkflowEditor...");

        m_closed = false;

        // create an edit domain for this editor (handles the command stack)
        m_editDomain = new DefaultEditDomain(this);
        m_selectionTool = new WorkflowSelectionTool();
        m_editDomain.setActiveTool(m_selectionTool);
        m_editDomain.setDefaultTool(m_selectionTool);

        setEditDomain(m_editDomain);

        // initialize actions (can't be in init(), as setInput is called before)
        createActions();
    }

    /**
     * @return the graphical viewer of this editor.
     */
    public GraphicalViewer getViewer() {
        return getGraphicalViewer();
    }

    /**
     * Returns the clipboard content for this editor.
     *
     * @return the clipboard for this editor
     */
    public ClipboardObject getClipboardContent() {
        return clipboard;
    }

    /**
     * Sets the clipboard content for this editor.
     *
     * @param content the content to set into the clipboard
     *
     */
    public void setClipboardContent(final ClipboardObject content) {
        clipboard = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input)
            throws PartInitException {

        LOGGER.debug("Initializing editor UI...");

        // TODO FIXME: Colors need to be assigned to different debugging levels
        NodeLogger.getLogger(WorkflowEditor.class).debug(
                "Opening workflow Editor on " + input.getName());

        // store site and input
        setSite(site);
        setInput(input);

        // add this as a CommandStackListener
        getCommandStack().addCommandStackListener(this);

        // add this as a selection change listener
        getSite().getWorkbenchWindow().getSelectionService()
                .addSelectionListener(this);

        // add this editor as a listener to WorkflowEvents
        m_manager.addListener(this);
        m_manager.addNodePropertyChangedListener(this);
        IPreferenceStore prefStore =
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        prefStore.addPropertyChangeListener(this);

        queueAfterOpen();
    }

    /** Add an action that is run in the SWT main thread after the editor
     * is initialized. It's used to prompt for additional actions (update links)
     * or to display notifications.
     * @param action The action to queue.
     * @throws IllegalStateException If editor is already initialized and the
     * after-open method has already been called. */
    public void addAfterOpenRunnable(final Runnable action) {
        if (m_hasAfterOpenRun) {
            throw new IllegalStateException("Can't queue afterOpen-runner - "
                    + "method has already been run");
        }
        if (m_afterOpenRunnables == null) {
            m_afterOpenRunnables = new ArrayList<Runnable>();
        }
        m_afterOpenRunnables.add(action);
    }

    /** Queues all {@link Runnable} in the after-open-runnable list in the
     * SWT main thread. */
    private void queueAfterOpen() {
        m_hasAfterOpenRun = true;
        if (m_afterOpenRunnables != null) {
            final Display d = Display.getDefault();
            for (final Runnable r : m_afterOpenRunnables) {
                d.asyncExec(r);
            }
        }
        m_afterOpenRunnables = null;
    }

    private List<IEditorPart> getSubEditors() {
        List<IEditorPart> editors = new ArrayList<IEditorPart>();
        if (m_manager == null) {
            // no workflow, no sub editors
            return editors;
        }
        for (NodeContainer child : m_manager.getNodeContainers()) {
            if (child instanceof SingleNodeContainer) {
                continue;
            }
            WorkflowManagerInput in =
                    new WorkflowManagerInput((WorkflowManager)child, this);
            if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
                for (IWorkbenchPage p : PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getPages()) {
                    IEditorPart editor = p.findEditor(in);
                    if (editor != null) {
                        editors.add(editor);
                        if (editor instanceof WorkflowEditor) {
                            /*
                             * Recursively get all subeditors. This is necessary
                             * for meta nodes in meta nodes.
                             */
                            editors.addAll(((WorkflowEditor)editor)
                                    .getSubEditors());
                        }
                    }
                }
            }
        }
        return editors;
    }

    /**
     * Deregisters all listeners when the editor is disposed.
     *
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        NodeLogger.getLogger(WorkflowEditor.class).debug("Disposing editor...");
        if (m_fileResource != null && m_manager != null) {
            // disposed is also called when workflow load fails or is canceled
            ProjectWorkflowMap.unregisterClientFrom(m_fileResource, this);
            ProjectWorkflowMap.remove(m_fileResource);
            m_manager.removeListener(this);
            m_manager.removeNodeStateChangeListener(this);
            m_manager.removeUIInformationListener(this);
        }
        // remember that this editor has been closed
        m_closed = true;
        for (IEditorPart child : getSubEditors()) {
            child.getEditorSite().getPage().closeEditor(child, false);
        }
        NodeProvider.INSTANCE.removeListener(this);
        if (m_manager != null) {
            m_manager.removeNodePropertyChangedListener(this);
        }
        getSite().getWorkbenchWindow().getSelectionService()
                .removeSelectionListener(this);
        // remove resource listener..
        if (m_fileResource != null
                && KnimeResourceUtil.getResourceForURI(m_fileResource)
                        != null) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        }
        if (m_parentEditor != null && m_manager != null) {
            // Store the editor settings with the meta node
            saveEditorSettingsToWorkflowManager(); // doesn't persist settings to disk
            // bug fix 2051: Possible memory leak related to sub-flow editor.
            // meta node editors were still referenced by the EditorHistory
            IWorkbench workbench = PlatformUI.getWorkbench();
            if (workbench instanceof Workbench) {
                EditorHistory hist = ((Workbench)workbench).getEditorHistory();
                WorkflowManagerInput wfmInput =
                        new WorkflowManagerInput(m_manager, m_parentEditor);
                hist.remove(wfmInput);
            }
        }

        getCommandStack().removeCommandStackListener(this);
        IPreferenceStore prefStore =
            KNIMEUIPlugin.getDefault().getPreferenceStore();

        prefStore.removePropertyChangeListener(this);
        super.dispose();
    }

    /**
     * Creates the editor actions.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
     */
    @Override
    protected void createActions() {
        LOGGER.debug("creating editor actions...");

        // super already does something for us...
        super.createActions();

        // Stack actions
        StackAction undo = new UndoAction(this);
        StackAction redo = new RedoAction(this);

        // Editor Actions
        WorkbenchPartAction delete =
                new NodeConnectionContainerDeleteAction(this);
        WorkbenchPartAction save = new SaveAction(this);
        WorkbenchPartAction print = new PrintAction(this);
        WorkbenchPartAction hideNodeName = new HideNodeNamesAction(this);

        // node actions
        //
        AbstractNodeAction openDialog = new OpenDialogAction(this);

        AbstractNodeAction execute = new ExecuteAction(this);
        AbstractNodeAction executeAll = new ExecuteAllAction(this);
        AbstractNodeAction cancelAll = new CancelAllAction(this);
        AbstractNodeAction cancel = new CancelAction(this);
        AbstractNodeAction pause = new PauseLoopExecutionAction(this);
        AbstractNodeAction step = new StepLoopAction(this);
        AbstractNodeAction resume = new ResumeLoopAction(this);
        AbstractNodeAction executeAndView = new ExecuteAndOpenViewAction(this);
        AbstractNodeAction reset = new ResetAction(this);
        AbstractNodeAction setNameAndDescription =
                new SetNodeDescriptionAction(this);
        AbstractNodeAction toggleFlowVarPorts =
            new ToggleFlowVarPortsAction(this);
        AbstractNodeAction defaultOpenView = new DefaultOpenViewAction(this);

        AbstractNodeAction metaNodeReConfigure =
            new MetaNodeReconfigureAction(this);
        AbstractNodeAction defineMetaNodeTemplate =
            new SaveAsMetaNodeTemplateAction(this);
        AbstractNodeAction checkUpdateMetaNodeLink =
            new CheckUpdateMetaNodeLinkAction(this);
        AbstractNodeAction revealMetaNodeTemplate
            = new RevealMetaNodeTemplateAction(this);
        AbstractNodeAction disconnectMetaNodeLink =
            new DisconnectMetaNodeLinkAction(this);
        AbstractNodeAction lockMetaLink = new LockMetaNodeAction(this);

        // new annotation action
        AddAnnotationAction annotation = new AddAnnotationAction(this);

        // copy / cut / paste action
        CopyAction copy = new CopyAction(this);
        CutAction cut = new CutAction(this);
        PasteAction paste = new PasteAction(this);
        PasteActionContextMenu pasteContext = new PasteActionContextMenu(this);
        CollapseMetaNodeAction collapse = new CollapseMetaNodeAction(this);
        ExpandMetaNodeAction expand = new ExpandMetaNodeAction(this);

        // register the actions
        m_actionRegistry.registerAction(undo);
        m_actionRegistry.registerAction(redo);
        m_actionRegistry.registerAction(delete);
        m_actionRegistry.registerAction(save);
        m_actionRegistry.registerAction(print);

        m_actionRegistry.registerAction(openDialog);
        m_actionRegistry.registerAction(execute);
        m_actionRegistry.registerAction(executeAll);
        m_actionRegistry.registerAction(cancelAll);
        m_actionRegistry.registerAction(cancel);
        m_actionRegistry.registerAction(pause);
        m_actionRegistry.registerAction(step);
        m_actionRegistry.registerAction(resume);
        m_actionRegistry.registerAction(executeAndView);
        m_actionRegistry.registerAction(reset);
        m_actionRegistry.registerAction(toggleFlowVarPorts);
        m_actionRegistry.registerAction(setNameAndDescription);
        m_actionRegistry.registerAction(defaultOpenView);

        m_actionRegistry.registerAction(copy);
        m_actionRegistry.registerAction(cut);
        m_actionRegistry.registerAction(paste);
        m_actionRegistry.registerAction(pasteContext);
        m_actionRegistry.registerAction(hideNodeName);
        m_actionRegistry.registerAction(collapse);
        m_actionRegistry.registerAction(expand);

        m_actionRegistry.registerAction(metaNodeReConfigure);
        m_actionRegistry.registerAction(defineMetaNodeTemplate);
        m_actionRegistry.registerAction(checkUpdateMetaNodeLink);
        m_actionRegistry.registerAction(revealMetaNodeTemplate);
        m_actionRegistry.registerAction(disconnectMetaNodeLink);
        m_actionRegistry.registerAction(lockMetaLink);
        m_actionRegistry.registerAction(annotation);

        // remember ids for later updates via 'updateActions'
        m_editorActions = new ArrayList<String>();
        m_editorActions.add(undo.getId());
        m_editorActions.add(redo.getId());
        m_editorActions.add(delete.getId());
        m_editorActions.add(save.getId());

        m_editorActions.add(openDialog.getId());
        m_editorActions.add(execute.getId());
        m_editorActions.add(executeAll.getId());
        m_editorActions.add(cancelAll.getId());
        m_editorActions.add(executeAndView.getId());
        m_editorActions.add(reset.getId());
        m_editorActions.add(setNameAndDescription.getId());
        m_editorActions.add(toggleFlowVarPorts.getId());
        m_editorActions.add(defaultOpenView.getId());
        m_editorActions.add(hideNodeName.getId());
        m_editorActions.add(collapse.getId());
        m_editorActions.add(expand.getId());

        m_editorActions.add(copy.getId());
        m_editorActions.add(cut.getId());
        m_editorActions.add(paste.getId());
        m_editorActions.add(metaNodeReConfigure.getId());
        m_editorActions.add(defineMetaNodeTemplate.getId());
        m_editorActions.add(checkUpdateMetaNodeLink.getId());
        m_editorActions.add(annotation.getId());
    }

    /**
     * This hooks keys like F2 for editing inside the editor.
     *
     * @return The common (shared) key handler.
     */
    protected KeyHandler getCommonKeyHandler() {
        return new KeyHandler() {
            @Override
            public boolean keyPressed(final org.eclipse.swt.events.KeyEvent e) {
                if (e.keyCode == SWT.F2) {
                    onF2Pressed();
                }
                return super.keyPressed(e);
            }
        };
    }

    /** Opens editor for (node) annotation (given that a single node or
     * annotation is selected). */
    private void onF2Pressed() {
        ISelectionProvider provider = getEditorSite().getSelectionProvider();
        if (provider == null) {
            return;
        }
        ISelection sel = provider.getSelection();
        if (!(sel instanceof IStructuredSelection)) {
            return;
        }

        Set<AnnotationEditPart> selectedAnnoParts =
            new HashSet<AnnotationEditPart>();
        @SuppressWarnings("rawtypes")
        Iterator selIter = ((IStructuredSelection)sel).iterator();
        while (selIter.hasNext()) {
            Object next = selIter.next();
            if (next instanceof AnnotationEditPart) {
                selectedAnnoParts.add((AnnotationEditPart)next);
            } else if (next instanceof NodeContainerEditPart) {
                NodeAnnotationEditPart nodeAnnoPart =
                    ((NodeContainerEditPart)next).getNodeAnnotationEditPart();
                if (nodeAnnoPart != null) {
                    selectedAnnoParts.add(nodeAnnoPart);
                }
            } else {
                return; // unknown type selected
            }
        }
        if (selectedAnnoParts.size() == 1) {
            AnnotationEditPart next = selectedAnnoParts.iterator().next();
            next.performEdit();
        }
    }

    /**
     * Returns the action registry for this editor. It is "lazy" created on
     * first invocation.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#getActionRegistry()
     */
    @Override
    public ActionRegistry getActionRegistry() {
        if (m_actionRegistry == null) {
            m_actionRegistry = new ActionRegistry();
        }
        return m_actionRegistry;
    }

    /**
     * Creates the graphical viewer that is hosted in this editor and hooks
     * keyhandler and edit domain.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #createGraphicalViewer(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createGraphicalViewer(final Composite parent) {
        IEditorSite editorSite = getEditorSite();
        GraphicalViewer viewer = null;
        viewer =
                new WorkflowGraphicalViewerCreator(editorSite,
                        this.getActionRegistry()).createViewer(parent);

        // Add a listener to the static node provider
        NodeProvider.INSTANCE.addListener(this);

        // Configure the key handler
        GraphicalViewerKeyHandler keyHandler =
            new GraphicalViewerKeyHandler(viewer);

        KeyHandler parentKeyHandler =
                keyHandler.setParent(getCommonKeyHandler());
        viewer.setKeyHandler(parentKeyHandler);

        // hook the viewer into the EditDomain
        getEditDomain().addViewer(viewer);

        // activate the viewer as selection provider for Eclipse
        getSite().setSelectionProvider(viewer);

        // remember this viewer
        m_graphicalViewer = viewer;

        // We already have the model - set it into the viewer
        getGraphicalViewer().setContents(m_manager);

        // add Help context
        WorkbenchHelpSystem.getInstance().setHelp(
                m_graphicalViewer.getControl(),
                "org.knime.workbench.help.flow_editor_context");

        loadProperties();
        updateEditorBackgroundColor();
        ((WorkflowRootEditPart)getGraphicalViewer().getRootEditPart()
                .getChildren().get(0))
                .createToolTipHelper(getSite().getShell());

    }

    /**
     * This does nothing by now, as all is handled by
     * <code>createGraphicalViewer</code>.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #initializeGraphicalViewer()
     */
    @Override
    protected void initializeGraphicalViewer() {
        // nothing
    }

    /**
     * Configures the graphical viewer.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
     */
    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();

    }

    /**
     * @return The graphical viewer in this editor
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#getGraphicalViewer()
     */
    @Override
    protected GraphicalViewer getGraphicalViewer() {
        return m_graphicalViewer;
    }

    /**
     * Sets the editor input, that is, the file that contains the serialized
     * workflow manager.
     *
     * {@inheritDoc}
     */
    @Override
    protected void setInput(final IEditorInput input) {
        LOGGER.debug("Setting input into editor...");

        super.setInput(input);

        if (input instanceof WorkflowManagerInput) {
            setWorkflowManagerInput((WorkflowManagerInput)input);
        } else {
            // input is the (full, absolute) path to the workflow.knime file
            File wfFile = new File(((IURIEditorInput)input).getURI());

            // non-null if the workflow editor was open and a doSaveAs is
            // called -- need to re-register listeners
            URI oldFileResource = m_fileResource;

            // store the workflow directory
            m_fileResource = wfFile.getParentFile().toURI();

            LOGGER.debug("Resource File's project: " + m_fileResource);

            // register listener to check whether the underlying knime file
            // (input) has been deleted or renamed (in case it is a resource)
            IWorkspace workspaceRoot = ResourcesPlugin.getWorkspace();
            if (oldFileResource != null && KnimeResourceUtil.getResourceForURI(
                    oldFileResource) != null) {
                // relocated editor (new input after doSaveAs)
                workspaceRoot.removeResourceChangeListener(this);
            }
            if (KnimeResourceUtil.getResourceForURI(m_fileResource) != null) {
                workspaceRoot.addResourceChangeListener(this,
                        IResourceChangeEvent.POST_CHANGE);
            }

            try {
                // FIXME:
                // setInput is called before the entire repository is loaded,
                // need to figure out how to do it the other way around
                // the static block needs to be executed, access
                // RepositoryManager.INSTANCE
                if (RepositoryManager.INSTANCE == null) {
                    LOGGER.fatal(
                            "Repository Manager Instance must not be null!");
                }
                assert m_manager == null;

                m_manager =
                        (WorkflowManager)ProjectWorkflowMap
                                .getWorkflow(m_fileResource);
                if (m_manager != null) {
                    // in case the workflow manager was edited somewhere else
                    // ...
                    if (m_manager.isDirty()) {
                        // ... make sure to inform the user about it
                        markDirty();
                    }
                } else {
                    IWorkbench wb = PlatformUI.getWorkbench();
                    IProgressService ps = wb.getProgressService();
                    // this one sets the workflow manager in the editor
                    LoadWorkflowRunnable loadWorflowRunnable =
                            new LoadWorkflowRunnable(this, wfFile);
                    ps.busyCursorWhile(loadWorflowRunnable);
                    // check if the editor should be disposed
                    // non-null if set by workflow runnable above
                    if (m_manager == null) {
                    if (loadWorflowRunnable.hasLoadingBeenCanceled()) {
                        final String cancelError =
                            loadWorflowRunnable.getLoadingCanceledMessage();
                        SwingUtilities.invokeLater(new Runnable() {
                            /** {@inheritDoc} */
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(null,
                                        cancelError,
                                        "Editor could not be opened",
                                        JOptionPane.ERROR_MESSAGE);
//                                    ErrorDialog.openError(Display
//                                            .getDefault()
//                                            .getActiveShell(),
//                                            "Editor could not be opened",
//                                            cancelError, null);

                            }
                        });
                        Display.getDefault().asyncExec(new Runnable() {
                            /** {@inheritDoc} */
                            @Override
                            public void run() {
                                getEditorSite().getPage().closeEditor(
                                        WorkflowEditor.this, false);
                            }
                        });
                        throw new OperationCanceledException(cancelError);
                    } else if (loadWorflowRunnable.getThrowable() != null) {
                        throw new RuntimeException(
                                loadWorflowRunnable.getThrowable());
                    }
                    }
                    ProjectWorkflowMap.putWorkflow(m_fileResource, m_manager);
                }
                // in any case register as client (also if the workflow was
                // already
                // loaded by another client
                ProjectWorkflowMap.registerClientTo(m_fileResource, this);
                m_manager.addListener(this);
                m_manager.addNodeStateChangeListener(this);
            } catch (InterruptedException ie) {
                LOGGER.fatal("Workflow loading thread interrupted", ie);
            } catch (InvocationTargetException e) {
                LOGGER.fatal("Workflow could not be loaded.", e);
            }

            updatePartName();

            if (getGraphicalViewer() != null) {
                loadProperties();
            }

            // update Actions, as now there's everything available
            updateActions();
        }
    }

    private void updatePartName() {
        // Editor name (title)
        setPartName(getTitleToolTip());
        setTitleToolTip(getTitleToolTip());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setTitleToolTip(final String toolTip) {
        m_manuallySetToolTip = toolTip;
        super.setTitleToolTip(toolTip);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitleToolTip() {
        // only for projects -> they have file resources in title
        // when renamed, they cannot reflect the changes, thus, the manually
        // set title is returned...
        // meta nodes can do not have file resources in title
        if (m_manuallySetToolTip != null && m_parentEditor == null) {
            return m_manuallySetToolTip;
        }
        // if this is a project
        if (m_parentEditor == null) {
            return m_manager.getID().getIDWithoutRoot() + ": "
                    + new Path(m_fileResource.getPath()).lastSegment();
        } else {
            // we are a meta node editor
            // return id and node name (custom name)
            return m_manager.getDisplayLabel();
        }
    }

    private void setWorkflowManagerInput(final WorkflowManagerInput input) {
        m_parentEditor = input.getParentEditor();
        WorkflowManager wfm = (input).getWorkflowManager();
        setWorkflowManager(wfm);
        setPartName(input.getName());
        wfm.addListener(this);
        if (getGraphicalViewer() != null) {
            loadProperties();
        }

        // update Actions, as now there's everything available
        updateActions();
        updatePartName();
        m_manager.addUIInformationListener(this);
        return;
    }

    /**
     * Updates the actions of this workflow editor. Can be used from subclassing
     * objects to update the actions.
     */
    public void updateActions() {
        // TODO: update here the actions in the action bar
        // based on current selection
        // -> maybe solves the execute all enabled/disabled problem
        updateActions(m_editorActions);
    }

    /**
     * Returns the overview for the outline view.
     *
     * @return the overview
     */
    protected NewOverviewOutlinePage getOverviewOutlinePage() {
        if ((m_overviewOutlinePage == null) && (getGraphicalViewer() != null)) {
            RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();

            if (rootEditPart instanceof ScalableFreeformRootEditPart) {
                m_overviewOutlinePage =
                        new NewOverviewOutlinePage(
                                (ScalableFreeformRootEditPart)rootEditPart);
            }
        }

        return m_overviewOutlinePage;
    }

    /**
     * Returns the undoable <code>PropertySheetPage</code> for this editor.
     *
     * @return the undoable <code>PropertySheetPage</code>
     */
    protected PropertySheetPage getPropertySheetPage() {
        if (m_undoablePropertySheetPage == null) {
            m_undoablePropertySheetPage = new PropertySheetPage();

            m_undoablePropertySheetPage
                    .setRootEntry(new UndoablePropertySheetEntry(
                            getCommandStack()));
        }

        return m_undoablePropertySheetPage;
    }

    /**
     * @return The WFM that is edited by this editor ("root model object")
     */
    public WorkflowManager getWorkflowManager() {
        return m_manager;
    }

    /**
     * Adaptable implementation for Editor, returns the objects used in this
     * editor, if asked for.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(final Class adapter) {
        // we need to handle common GEF elements we created
        if (adapter == GraphicalViewer.class) {
            return this.getGraphicalViewer();
        } else if (adapter == EditPartViewer.class) {
            return this.getGraphicalViewer();
        } else if (adapter == CommandStack.class) {
            return this.getCommandStack();
        } else if (adapter == EditDomain.class) {
            return this.getEditDomain();
        } else if (adapter == ActionRegistry.class) {
            return this.getActionRegistry();
        } else if (adapter == IPropertySheetPage.class) {
            return this.getPropertySheetPage();
        } else if (adapter == IContentOutlinePage.class) {
            return this.getOverviewOutlinePage();
        } else if (adapter == ZoomManager.class) {
            return getGraphicalViewer().getProperty(
                    ZoomManager.class.toString());
        }

        if (adapter == WorkflowEditorAdapter.class) {
            // hackaround to deliver the wfm to the navigator
            return new WorkflowEditorAdapter(m_manager);
        }

        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    /**
     * Sets the snap functionality and zoomlevel
     */
    private void loadProperties() {
        // Snap to Geometry property
        GraphicalViewer graphicalViewer = getGraphicalViewer();
        graphicalViewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED,
                new Boolean(true));

        if (getWorkflowManager() != null) {
            applyEditorSettingsFromWorkflowManager();
        }
    }

    /** Sets background color according to edit mode (see
     * {@link WorkflowManager#isWriteProtected()}. */
    private void updateEditorBackgroundColor() {
        final Color color;
        if (m_manager.isWriteProtected()) {
            color = BG_COLOR_WRITE_LOCK;
        } else {
            color = BG_COLOR_DEFAULT;
        }
        Runnable r = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                GraphicalViewer gv = getGraphicalViewer();
                Control control = gv.getControl();
                control.setBackground(color);
            }
        };
        Display display = Display.getDefault();
        if (display.getThread() == Thread.currentThread()) {
            r.run();
        } else {
            display.asyncExec(r);
        }

    }

    /**
     * Whether the dialog has passed the {@link #promptToSaveOnClose()} method.
     * This helps us to distinguish whether we have to offer an exit dialog when
     * executing nodes are not saveable. This flag is only true if the user
     * closes the editor.
     */
    private boolean m_isClosing;

    /**
     * Brings up the Save-Dialog and sets the m_isClosing flag.
     * {@inheritDoc}
     */
    @Override
    public int promptToSaveOnClose() {
        /*
         * Ideally we would just set the m_isClosing flag and return
         * ISaveablePart2.DEFAULT which will bring up a separate dialog. This
         * does not work as we have to set the m_isClosing only if the user
         * presses YES (no means to figure out what button was pressed when
         * eclipse opens the dialog).
         */
        if (m_parentEditor != null) {
            // ignore closing meta node editors.
            return ISaveablePart2.NO;
        }
        String message =
                NLS.bind(WorkbenchMessages.EditorManager_saveChangesQuestion,
                        getTitle());
        // Show a dialog.
        Shell sh = Display.getDefault().getActiveShell();
        String[] buttons =
                new String[]{IDialogConstants.YES_LABEL,
                        IDialogConstants.NO_LABEL,
                        IDialogConstants.CANCEL_LABEL};
        MessageDialog d =
                new MessageDialog(sh, WorkbenchMessages.Save_Resource, null,
                        message, MessageDialog.QUESTION, buttons, 0);
        switch (d.open()) { // returns index in buttons[] array
        case 0: // YES
            m_isClosing = true;
            return ISaveablePart2.YES;
        case 1: // NO
            return ISaveablePart2.NO;
        default: // CANCEL button or window 'x'
            return ISaveablePart2.CANCEL;
        }
    }

    private void saveTo(final URI fileResource,
            final IProgressMonitor monitor) {
        LOGGER.debug("Saving workflow ...");

        // Exception messages from the inner thread
        final StringBuffer exceptionMessage = new StringBuffer();

        if (fileResource == null && m_parentEditor != null) {
            m_parentEditor.doSave(monitor);
            m_isDirty = false;
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
            return;
        }

        // attach editor properties with the workflow manager - for all sub editors too
        saveEditorSettingsToWorkflowManager();
        for (IEditorPart subEditor : getSubEditors()) {
            ((WorkflowEditor)subEditor).saveEditorSettingsToWorkflowManager();
        }

        // to be sure to mark dirty and inform the user about running nodes
        // we ask for the state BEFORE saving
        // this flag is evaluated at the end of this method
        boolean wasInProgress = false;
        try {
            final File file = new File(new File(fileResource),
                    WorkflowPersistor.WORKFLOW_FILE);

            // If something fails an empty workflow is created
            // except when cancellation occurred
            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();
            SaveWorkflowRunnable saveWorflowRunnable =
                    new SaveWorkflowRunnable(this, file, exceptionMessage,
                            monitor);

            State state = m_manager.getState();
            wasInProgress =
                    state.executionInProgress()
                            && !state.equals(State.EXECUTINGREMOTELY);

            ps.run(true, false, saveWorflowRunnable);
            // after saving the workflow, check for the import marker
            // and delete it

            // mark command stack (no undo beyond this point)
            getCommandStack().markSaveLocation();

        } catch (Exception e) {
            LOGGER.error("Could not save workflow: " + exceptionMessage, e);

            // inform the user
            if (exceptionMessage.toString().trim().length() > 0) {
                showInfoMessage("Workflow could not be saved ...",
                        exceptionMessage.toString());
            }

            throw new OperationCanceledException("Workflow was not saved: "
                    + exceptionMessage.toString());
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!Display.getDefault().isDisposed()) {
                    // mark all sub editors as saved
                    for (IEditorPart subEditor : getSubEditors()) {
                        final WorkflowEditor editor = (WorkflowEditor)subEditor;
                        ((WorkflowEditor)subEditor).setIsDirty(false);
                        editor.firePropertyChange(IEditorPart.PROP_DIRTY);
                    }
                }
            }
        });

        monitor.done();

        // bugfix 799 (partly)
        // check if the workflow manager is in execution
        // this happens if the user pressed "Yes" on save confirmation dialog
        // or simply saves (Ctrl+S)
        if (wasInProgress) {
            markDirty();
            final Pointer<Boolean> abortPointer = new Pointer<Boolean>();
            abortPointer.set(Boolean.FALSE);
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    boolean abort = false;
                    Shell sh = Display.getDefault().getActiveShell();
                    String title = "Workflow in execution";
                    String message = "Executing nodes are not saved!";
                    if (m_isClosing) {
                        abort =
                                !MessageDialog.openQuestion(sh, title, message
                                        + " Exit anyway?");
                        m_isClosing = !abort; // user canceled close
                    } else {
                        IPreferenceStore prefStore =
                                KNIMEUIPlugin.getDefault().getPreferenceStore();
                        String toogleMessage = "Don't warn me again";
                        if (prefStore.getBoolean(
                                PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED)) {
                            MessageDialogWithToggle
                                    .openInformation(
                                            sh,
                                            title,
                                            message,
                                            toogleMessage,
                                            false,
                                            prefStore,
                                            PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED);
                        }
                    }
                    abortPointer.set(Boolean.valueOf(abort));
                }
            });
            if (abortPointer.get()) {
                throw new OperationCanceledException(
                        "Closing workflow canceled on user request.");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        saveTo(m_fileResource, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doSaveAs() {
        /** This code is almost functional but is not actually called
         * (see isSaveAsAllowed). In order to enable this code we need to
         * - change input to this editor (review setInput)
         * - verify that the workflow is then looking to the new file resource
         * - change open editors (subeditors (dunno) and report editors)
         */
        URI fileResource = m_fileResource;
        WorkflowEditor parentEditor = m_parentEditor;
        while (fileResource == null && parentEditor != null) {
            fileResource = m_parentEditor.m_fileResource;
            m_parentEditor = parentEditor.m_parentEditor;
        }
        if (fileResource == null) {
            assert false : "Workflow doesn't have default save location";
            URI newDirURI =
                ResourcesPlugin.getWorkspace().getRoot().getRawLocationURI();
            if (newDirURI == null || !"file".equals(newDirURI.getScheme())) {
                throw new RuntimeException("Can't access workspace root");
            }
            fileResource = new File(new File(newDirURI),
                    "KNIME_project").toURI();
        }
        File workflowDir = new File(fileResource);
        File workflowDirParent = workflowDir.getParentFile();
        final Set<String> invalidFileNames = new HashSet<String>(
                Arrays.asList(workflowDirParent.list()));
        String workflowDirName = workflowDir.getName();
        String workflowDirNewName = guessNewWorkflowNameOnSaveAs(
                invalidFileNames, workflowDirName);
        Display display = Display.getDefault();
        Shell activeShell = display.getActiveShell();
        final InputDialog newNameDialog = new InputDialog(activeShell,
                "Save as new workflow", "New workflow name",
                workflowDirNewName, new FileStoreNameValidator() {
            @Override
            public String isValid(final String name) {
                if (invalidFileNames.contains(name)) {
                    return "Workflow/Directory already exists";
                }
                return super.isValid(name);
            }
        });
        newNameDialog.setBlockOnOpen(true);
        final AtomicBoolean proceed = new AtomicBoolean(false);
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                proceed.set(newNameDialog.open() == InputDialog.OK);
            }
        });
        if (!proceed.get()) {
            return;
        }
        workflowDirNewName = newNameDialog.getValue();
        final URI newFileResource = new File(
                workflowDirParent, workflowDirNewName).toURI();
        saveTo(newFileResource, new NullProgressMonitor());
        IWorkspace workspaceRoot = ResourcesPlugin.getWorkspace();
        if (m_fileResource != null && KnimeResourceUtil.getResourceForURI(
                m_fileResource) != null) {
            // relocated editor (new input after doSaveAs)
            workspaceRoot.removeResourceChangeListener(this);
        }
    }

    /** Derives new name, e.g. KNIME_project_2 -> KNIME_project_3.
     * Separator between name and number can be empty, space, dash, underscore
     * @param invalidFileNames Names of files/folders that already exist
     * @param workflowDirName The name of the old workflow
     * @return The new suggested name (shown in rename prompt). */
    private static String guessNewWorkflowNameOnSaveAs(
            final Set<String> invalidFileNames, final String workflowDirName) {
        Pattern pattern = Pattern.compile("(^.*[ _\\-]?)(\\d)");
        Matcher matcher = pattern.matcher(workflowDirName);
        String baseName = workflowDirName;
        int index;
        if (matcher.matches()) {
            try {
                index = Integer.parseInt(matcher.group(2));
            } catch (Exception e) {
                index = 0;
            }
            baseName = matcher.group(1);
        } else {
            index = 0;
            baseName = workflowDirName + " ";
        }
        String workflowDirNewName = workflowDirName;
        while (invalidFileNames.contains(workflowDirNewName)) {
            workflowDirNewName = baseName + (++index);
        }
        return workflowDirNewName;
    }

    /**
     * Stores the current editor settings with the workflow manager. In its NodeUIInfo object. Overrides any previously
     * stored editor settings.
     */
    private void saveEditorSettingsToWorkflowManager() {
        // overwriting any existing editor settings in the ui info
        getWorkflowManager().setEditorUIInformation(getCurrentEditorSettings());
    }

    /**
     * @return the current values of the settings (grid and zoomlevel) of this editor
     */
    public EditorUIInformation getCurrentEditorSettings() {
        EditorUIInformation editorInfo = new EditorUIInformation();
        editorInfo.setZoomLevel(getZoomfactor());
        editorInfo.setSnapToGrid(getEditorSnapToGrid());
        editorInfo.setShowGrid(getEditorIsGridVisible());
        editorInfo.setGridX(getEditorGridX());
        editorInfo.setGridY(getEditorGridY());
        return editorInfo;
    }

    /**
     * Applies the settings to the editor. Can't be null.
     * @see #getCurrentEditorSettings()
     * @see #getEditorSettingsDefault()
     * @param settings to apply
     */
    public void applyEditorSettings(final EditorUIInformation settings) {
        getViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.valueOf(settings.getSnapToGrid()));
        getViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.valueOf(settings.getShowGrid()));
        getViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING,
                new Dimension(settings.getGridX(), settings.getGridY()));
        setZoomfactor(settings.getZoomLevel());
    }

    private void applyEditorSettingsFromWorkflowManager() {
        final WorkflowManager wfm = getWorkflowManager();
        EditorUIInformation settings = wfm.getEditorUIInformation();
        if (settings == null) {
            // if this is a meta node - derive settings from parent
            if (m_fileResource == null && m_parentEditor != null) {
                m_parentEditor.saveEditorSettingsToWorkflowManager();
                settings = m_parentEditor.getCurrentEditorSettings();
                // don't derive zoom factor.
                if (settings != null) {
                    settings.setZoomLevel(1.0);
                }
                if (settings == null) {
                    settings = getEditorSettingsDefault();
                }
            } else {
                // this is an old workflow: don't show or enable grid
                settings = getEditorSettingsDefault();
                settings.setShowGrid(false);
                settings.setSnapToGrid(false);
            }
        }
        applyEditorSettings(settings);
    }

    /**
     * @return an object with the default value (mostly from the preference page) for the editor settings
     */
    public EditorUIInformation getEditorSettingsDefault() {
        EditorUIInformation result = new EditorUIInformation();
        result.setSnapToGrid(getPrefSnapToGrid());
        result.setShowGrid(getPrefIsGridVisible());
        result.setGridX(getPrefGridXSize());
        result.setGridY(getPrefGridYSize());
        result.setZoomLevel(1.0);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        // if we are a subworkflow editor we are never dirty
        if (m_parentEditor != null) {
            return m_parentEditor.isDirty();
        }
        return m_isDirty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Shows a simple information message.
     *
     * @param message the info message to display
     */
    private void showInfoMessage(final String header, final String message) {
        // inform the user

        MessageBox mb =
                new MessageBox(this.getSite().getShell(), SWT.ICON_INFORMATION
                        | SWT.OK);
        mb.setText(header);
        mb.setMessage(message);
        mb.open();
    }

    /**
     * Notifies property listeners on the editor about changes (e.g. dirty state
     * has changed). Updates the available actions afterwards
     *
     * @see org.eclipse.ui.part.WorkbenchPart#firePropertyChange(int)
     */
    @Override
    protected void firePropertyChange(final int property) {

        super.firePropertyChange(property);

        // updates the editor actions
        updateActions(m_editorActions);
    }

    /**
     * Called when the editors selection has changed. Updates the list of
     * available actions for the new selection in the editor.
     *
     * @see org.eclipse.ui.ISelectionListener#selectionChanged
     *      (org.eclipse.ui.IWorkbenchPart,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part,
            final ISelection selection) {
        // update available actions
        updateActions();
    }

    /**
     * Called when the command stack has changed, that is, a GEF command was
     * executed (Add,Remove,....). This keeps track of the dirty state of the
     * editor. {@inheritDoc}
     */
    @Override
    public void commandStackChanged(final EventObject event) {

        // update the actions (should enable undo/redo accordingly)
        updateActions(m_editorActions);

        // track the dirty state of the edit domain
        boolean b = m_editDomain.getCommandStack().isDirty();
        if (b || getWorkflowManager().isDirty()) {
            markDirty();
        } else {
            m_isDirty = false;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /*
     * --------- methods for adding a auto-placed and auto-connected node -----
     */

    /**
     * {@inheritDoc}
     * Listener interface method of the {@link NodeProvider}.
     * Called when other instances want to add a meta node to the workflow in
     * the editor. <br>
     * The implementation only adds it if the editor is active. If one other
     * node is selected in the editor, to which the new node will then be
     * connected to.
     *
     * @param sourceManager wfm to copy the meta node from
     * @param id the id of the meta node in the source manager
     * @return if the meta node was actually added
     */
    @Override
    public boolean addMetaNode(final WorkflowManager sourceManager,
            final NodeID id) {
        if (id == null || sourceManager == null) {
            return false;
        }
        if (!isEditorActive()) {
            return false;
        }

        NodeContainerEditPart preNode = getTheOneSelectedNode();
        NodeID preID = null;
        Point nodeLoc = null;
        if (preNode == null) {
            nodeLoc = getViewportCenterLocation();
            nodeLoc = toAbsolute(nodeLoc);
        } else {
            nodeLoc = getLocationRightOf(preNode);
            preID = preNode.getNodeContainer().getID();
        }
        if (getEditorSnapToGrid()) {
            nodeLoc = getClosestGridLocation(nodeLoc);
        }
        Command newNodeCmd =
                new CreateNewConnectedMetaNodeCommand(getViewer(), m_manager,
                        sourceManager, id, nodeLoc, preID);
        getCommandStack().execute(newNodeCmd);
        // after adding a node the editor should get the focus
        setFocus();
        return true;
    }

    /**
     * Listener interface method of the {@link NodeProvider}. Called when other
     * instances want to add a node to the workflow in the editor. <br>
     * The implementation only adds it if the editor is active and one node is
     * selected in the editor, to which the new node will then be connected to.
     * {@inheritDoc}
     */
    @Override
    public boolean addNode(final NodeFactory<? extends NodeModel> nodeFactory) {

        if (!isEditorActive()) {
            return false;
        }

        NodeContainerEditPart preNode = getTheOneSelectedNode();
        Point nodeLoc = null;
        Command newNodeCmd = null;
        if (preNode == null) {
            nodeLoc = getViewportCenterLocation();
            // this command accepts/requires relative coordinates
            newNodeCmd = new CreateNodeCommand(m_manager, nodeFactory, nodeLoc, getEditorSnapToGrid());
        } else {
            nodeLoc = getLocationRightOf(preNode);
            newNodeCmd =
                    new CreateNewConnectedNodeCommand(getViewer(), m_manager,
                            nodeFactory, nodeLoc, preNode.getNodeContainer()
                                    .getID());
        }

        getCommandStack().execute(newNodeCmd);
        // after adding a node the editor should get the focus
        setFocus();
        return true;
    }

    private boolean isEditorActive() {
        // find out if we are the active editor (any easier way than that???)
        IWorkbenchWindow window =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return false;
        }
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return false;
        }
        IEditorPart editor = page.getActiveEditor();
        return editor == this;
    }

    /**
     * @return a node if and only if exactly one node in the editor is selected
     */
    private NodeContainerEditPart getTheOneSelectedNode() {
        IStructuredSelection edSel =
                (IStructuredSelection)getSite().getSelectionProvider()
                        .getSelection();
        if (edSel.size() == 1) {
            Object o = edSel.getFirstElement();
            if (o instanceof NodeContainerEditPart) {
                // the one and only node that is selected
                return (NodeContainerEditPart)o;
            }
        }
        return null;
    }

    /**
     * @return a location in the middle of the visible part of the editor. These
     *         are relative coordinates (to the current scrolling position)
     */
    private Point getViewportCenterLocation() {
        FigureCanvas ctrl = ((FigureCanvas)getViewer().getControl());
        Viewport viewPort = ctrl.getViewport();
        Dimension viewSize = viewPort.getSize();
        int relX = viewSize.width / 2;
        int relY = viewSize.height / 2;
        Point nodeLoc = new Point(relX, relY);
        // make sure we have a free spot
        int stepX = getEditorSnapToGrid() ? getEditorGridXOffset(10) : 10;
        int stepY = getEditorSnapToGrid() ? getEditorGridYOffset(10) : 10;
        while (isNodeAtRel(nodeLoc)) {
            // move it a bit
            nodeLoc.x += stepX;
            nodeLoc.y += stepY;
        }
        return nodeLoc;


    }

    private Point getLocationRightOf(final NodeContainerEditPart refNode) {
        NodeUIInformation ui =
                refNode.getNodeContainer()
                .getUIInformation();
        int xOffset = 100;
        int yOffset = 120;
        // adjust offset to grid location
        if (getEditorSnapToGrid()) {
            // with grid enabled we use the grid size as offset (but at least a bit mire than the node width)
        	xOffset = getEditorGridXOffset((int)(refNode.getFigure().getBounds().width * 1.1));
        	yOffset = getEditorGridYOffset((int)(refNode.getFigure().getBounds().height * 1.1));
        }

        // first try: right of reference node
        Point loc = new Point(ui.getBounds()[0] + xOffset, ui.getBounds()[1]);
        // make sure we have a free spot
        while (isNodeAtAbs(loc)) {
            // move it down a bit
            loc.y += yOffset;
        }
        return loc;
    }

    private boolean isNodeAtAbs(final Point absoluteLoc) {
        return isNodeAtRel(toRelative(absoluteLoc));

    }

    private Point toRelative(final Point absLoc) {
        ScalableFreeformRootEditPart rootEditPart
            = (ScalableFreeformRootEditPart) getViewer().getRootEditPart();
        Viewport viewport = (Viewport) rootEditPart.getFigure();
        Rectangle area = viewport.getClientArea();
        Point loc = absLoc.getCopy();
        double z = getZoomfactor();
        loc.x = (int)Math.round((loc.x - area.x) * z);
        loc.y = (int)Math.round((loc.y - area.y) * z);
        return loc;
    }

    private Point toAbsolute(final Point relLoc) {
        ScalableFreeformRootEditPart rootEditPart
        = (ScalableFreeformRootEditPart) getViewer().getRootEditPart();
        Viewport viewport = (Viewport) rootEditPart.getFigure();
        Rectangle area = viewport.getClientArea();

        Point loc = relLoc.getCopy();
        loc.x += area.x;
        loc.y += area.y;
        return loc;
    }


    private double getZoomfactor() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return 1.0;
        }
        ZoomManager zoomManager = (ZoomManager)(viewer.getProperty(ZoomManager.class.toString()));
        if (zoomManager == null) {
            return 1.0;
        }
        return zoomManager.getZoom();
    }

    private void setZoomfactor(final double z) {
        ZoomManager zoomManager =
            (ZoomManager)(getViewer().getProperty(ZoomManager.class
                    .toString()));
        zoomManager.setZoom(z);
    }

    private boolean isNodeAtRel(final Point relativeLoc) {
        EditPart ep = getViewer().findObjectAt(relativeLoc);
        if (ep == null) {
            return false;
        }
        while (!(ep instanceof RootEditPart)) {
            if (ep instanceof NodeContainerEditPart) {
                return true;
            }
            EditPart parent = ep.getParent();
            // avoid endless loops
            if (parent == null || ep == parent) {
                return false;
            }
            ep = parent;
        }
        return false;
    }
    /*
     * ---------- end of auto-placing and auto-connecting --------------
     */

    /*
     * ------------- grid functions ----------------------------------------------------------
     */

    private void updateGrid() {
        /**
         * This is used to init the viewer and to update on a property change event. Maybe we need to split this in
         * an init and an update, in case we don't want to change all settings on an update (e.g. the view grid could
         * be an editor instance specific setting with a default value in the pref page).
         */
        boolean snapToGrid = getPrefSnapToGrid();
        boolean showGrid = getPrefIsGridVisible();
        GraphicalViewer graphicalViewer = getGraphicalViewer();
        graphicalViewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED,
                Boolean.valueOf(snapToGrid));
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED,
                Boolean.valueOf(snapToGrid));
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING,
                new Dimension(getPrefGridXSize(), getPrefGridYSize()));
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE,
                showGrid);
    }

    /**
     * Returns the grid horizontal spacing or the x value from the preference page, if the editor's property is not
     * set.
     *
     * @return the editors grid, or the value from the pref page (if not set in the editor)
     */
    public int getEditorGridX() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return getPrefGridXSize();
        }
        Dimension grid = (Dimension)viewer.getProperty(SnapToGrid.PROPERTY_GRID_SPACING);
        if (grid != null) {
            return grid.width;
        }
        return getPrefGridXSize();
    }

    /**
     * Returns the grid vertical spacing or the y value from the preference page, if the editor's property is not
     * set.
     *
     * @return the editors grid, or the value from the pref page (if not set in the editor)
     */
    public int getEditorGridY() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return getPrefGridYSize();
        }
        Dimension grid = (Dimension)viewer.getProperty(SnapToGrid.PROPERTY_GRID_SPACING);
        if (grid != null) {
            return grid.height;
        }
        return getPrefGridYSize();
    }

    /**
     * Returns true, if the grid is visible in this editor (or the preference page value if the editor's property is not
     * set).
     *
     * @return true, if the grid is visible in this editor
     */
    public boolean getEditorIsGridVisible() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return getPrefIsGridVisible();
        }
        Boolean visi = (Boolean)viewer.getProperty(SnapToGrid.PROPERTY_GRID_VISIBLE);
        if (visi != null) {
            return visi.booleanValue();
        }
        return getPrefIsGridVisible();
    }

    /**
     * Returns true, if the grid is enabled in this editor (or the preference page value if the editor's property is not
     * set).
     *
     * @return true, if snap to grid is enabled in this editor
     */
    public boolean getEditorSnapToGrid() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return getPrefSnapToGrid();
        }
        Boolean snap = (Boolean)viewer.getProperty(SnapToGrid.PROPERTY_GRID_ENABLED);
        if (snap == null) {
            return getPrefSnapToGrid();
        }
        return snap.booleanValue();
    }

    /**
     * @return the value from the preference page for 'show grid' (each editor has its own property value which could be
     *         different)
     */
    public static boolean getPrefIsGridVisible() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        return prefStore.getBoolean(PreferenceConstants.P_GRID_SHOW);
    }

    /**
     * @return the value from the preference page for 'snap to grid' (each editor has its own property value which could
     *         be different)
     */
    public static boolean getPrefSnapToGrid() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        return prefStore.getBoolean(PreferenceConstants.P_GRID_SNAP_TO);
    }

    /**
     * @return the preference page value for the horizontal grid size (or the default value if zero or negative)
     */
    public static int getPrefGridXSize() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        int gridSize = prefStore.getInt(PreferenceConstants.P_GRID_SIZE_X);
        if (gridSize <= 0) {
            gridSize = prefStore.getDefaultInt(PreferenceConstants.P_GRID_SIZE_X);
        }
        return gridSize;
    }

    /**
     * @return the preference page value for the vertical grid size (or the default value if zero or negative)
     */
    public static int getPrefGridYSize() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        int gridSize = prefStore.getInt(PreferenceConstants.P_GRID_SIZE_Y);
        if (gridSize <= 0) {
            gridSize = prefStore.getDefaultInt(PreferenceConstants.P_GRID_SIZE_Y);
        }
        return gridSize;
    }

    /**
     * Adjusts the passed offset to the grid size. The result is a multiple of the grid X size equal to or larger than
     * the passed value (disregards the snap to grid flag).
     *
     * @param xOffset to translate into a 'grid compatible' offset
     * @return a multiple of the grid X size equal to or larger than the passed value
     */
    public int getEditorGridXOffset(final int xOffset) {
        int gridSizeX = getEditorGridX();
        if (gridSizeX > 1 && (xOffset % gridSizeX != 0)) {
            return ((xOffset / gridSizeX) + 1) * gridSizeX;
        }
        return xOffset;
    }

    /**
     * The result is a multiple of the grid Y size equal to or larger than the passed value (disregards the snap to grid
     * flag).
     *
     * @param yOffset to translate into a 'grid compatible' offset
     * @return a multiple of the grid Y size equal to or larger than the passed value
     */
    public int getEditorGridYOffset(final int yOffset) {
        int gridSizeY = getEditorGridY();
        if (gridSizeY > 1 && (yOffset % gridSizeY != 0)) {
            return ((yOffset / gridSizeY) + 1) * gridSizeY;
        }
        return yOffset;
    }

    /**
     * Returns the closest location that is located on the grid.
     *
     * @param gridContainer the pane with the grid containing the location
     * @param loc reference point for the closest grid location, must be translated relative to the container
     * @return closest grid point
     */
    public Point getClosestGridLocation(final Point loc) {
        Point location = loc.getCopy();
        IFigure gridContainer = ((WorkflowRootEditPart)getViewer().getRootEditPart().getContents()).getFigure();
        // container coordinates could be negative
        gridContainer.translateToRelative(location);
        Point result = location.getCopy();
        int locX = loc.x;
        int gridX = getEditorGridX();
        if (gridX > 1) {
            // distance to the left grid line (or right, if locX is negative)
            int leftGrid = (locX / gridX) * gridX;
            if (Math.abs(locX - leftGrid) <= (gridX / 2)) {
                result.x = leftGrid;
            } else {
                // location is closer to the next grid (right of the location, or left if x is negative)
                result.x = leftGrid + (((int)Math.signum(locX)) * gridX);
            }
        }
        int locY = loc.y;
        int gridY = getEditorGridY();
        if (gridY > 1) {
            // distance to the next upper grid line (or lower line, if y is negative)
            int upperGrid = (locY / gridY) * gridY;
            if (Math.abs(locY - upperGrid) <= (gridY / 2)) {
                // location is closer to the upper grid line (or lower line, if y is negative)
                result.y = upperGrid;
            } else {
                // location is closer to the next lower grid
                result.y = upperGrid + (((int)Math.signum(locY)) * gridY);
            }
        }
        return result;
    }

    /**
     * Returns a point on the grid that has equal or larger coordinates than the passed location.
     *
     * @param gridContainer the pane with the grid containing the location
     * @param loc the reference point for the next grid location
     * @return next grid location (right of and lower than argument location or equal)
     */
    public final Point getNextGridLocation(final Point loc) {
        Point location = loc.getCopy();
        // container coordinates could be negative
        IFigure gridContainer = ((WorkflowRootEditPart)getViewer().getRootEditPart().getContents()).getFigure();
        gridContainer.translateToRelative(location);
        Point result = location.getCopy();
        int stepX = (loc.x >= 0) ? 1 : 0;
        int gridX = getEditorGridX();
        if (gridX > 1 && result.x % gridX != 0) {
            result.x = ((loc.x / gridX) + stepX) * gridX;
        }

        int stepY = (loc.y >= 0) ? 1 : 0;
        int gridY = getEditorGridY();
        if (gridY > 1 && result.y % gridY != 0) {
            result.y = ((loc.y / gridY) + stepY) * gridY;
        }
        return result;
    }


    /**
     * Returns the vertical grid distance for the active workflow editor (or -1 if no workflow editor is active at the
     * moment).
     *
     * @return the y grid size for the active workflow editor (or -1)
     * @see #getEditorGridY()
     */
    public static int getActiveEditorGridY() {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return -1;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return -1;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorGridY();
        }
        return -1;
    }

    /**
     * Returns the horizontal grid distance for the active workflow editor (or -1 if no workflow editor is active at the
     * moment).
     *
     * @return the x grid size for the active workflow editor (or -1)
     * @see #getEditorGridX()
     */
    public static int getActiveEditorGridX() {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return -1;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return -1;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorGridX();
        }
        return -1;
    }

    /**
     * Returns true, if the active editor is a workflow editor and snap to grid is enabled. False otherwise.
     * @return true, if the active editor is a workflow editor and snap to grid is enabled. False otherwise
     * @see #getEditorSnapToGrid()
     */
    public static boolean getActiveEditorSnapToGrid() {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return false;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return false;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorSnapToGrid();
        }
        return false;
    }

    /**
     * Returns the closest location on the grid of the active editor, or the argument if no workflow editor is active
     * @param loc the ref point
     * @return the closest location on the grid of the active editor, or the argument if no workflow editor is active
     * @see #getClosestGridLocation(Point)
     */
    public static Point getActiveEditorClosestGridLocation(final Point loc) {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return loc;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return loc;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getClosestGridLocation(loc);
        }
        return loc;
    }

    /**
     * Returns grid location on the next lower right grid location in the active editor, or the argument, if no workflow
     * editor is active.
     *
     * @param loc the ref point
     * @return the next grid point if the active editor is a worflow editor
     * @see #getNextGridLocation(Point)
     */
    public static Point getActiveEditorNextGridLocation(final Point loc) {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return loc;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return loc;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getClosestGridLocation(loc);
        }
        return loc;
    }

    /**
     * The result is a multiple of the grid Y size of the active editor equal to or larger than the passed value
     * (disregards the snap to grid flag).
     *
     * @param yOffset to translate into a 'grid compatible' offset
     * @return a multiple of the grid Y size equal to or larger than the passed value - or the argument, if no workflow
     * editor is active
     * @see #getEditorGridYOffset(int)
     */
    public static int getActiveEditorGridYOffset(final int yOffset) {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return yOffset;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return yOffset;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorGridYOffset(yOffset);
        }
        return yOffset;
    }

    /**
     * The result is a multiple of the grid X size of the active editor equal to or larger than the passed value
     * (disregards the snap to grid flag).
     *
     * @param xOffset to translate into a 'grid compatible' offset
     * @return a multiple of the grid X size equal to or larger than the passed value - or the argument, if no workflow
     * editor is active
     * @see #getEditorGridXOffset(int)
     */
    public static int getActiveEditorGridXOffset(final int xOffset) {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return xOffset;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return xOffset;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorGridXOffset(xOffset);
        }
        return xOffset;
    }


    /*----------------------------------------------------------------------------------------------------------*/

    /**
     * Listener callback, listens to workflow events and triggers UI updates.
     *
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent event) {
        LOGGER.debug("Workflow event triggered: " + event.toString());
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            @Override
            public void run() {
                switch (event.getType()) {
                case NODE_REMOVED:
                    Object oldValue = event.getOldValue();
                    // close sub-editors if a child meta node is deleted
                    if (oldValue instanceof WorkflowManager) {
                        WorkflowManager wm = (WorkflowManager)oldValue;
                        // since the equals method of the WorkflowManagerInput
                        // only looks for the WorkflowManager, we can pass
                        // null as the editor argument
                        WorkflowManagerInput in =
                            new WorkflowManagerInput(wm, null);
                        IEditorPart editor =
                            getEditorSite().getPage().findEditor(in);
                        if (editor != null) {
                            editor.getEditorSite().getPage().closeEditor(editor,
                                    false);
                        }
                    }
                    break;
                case CONNECTION_REMOVED:
                case CONNECTION_ADDED:
                    getViewer().getContents().refresh();
                    break;
                default: // no further actions, all handled in edit policies etc
                }
                markDirty();
                updateActions();
            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public void nodePropertyChanged(final NodePropertyChangedEvent e) {
        switch (e.getProperty()) {
        case Name:
            updatePartName();
            break;
        case TemplateConnection:
            updateEditorBackgroundColor();
            break;
        default:
            // ignore
        }
    }

    /**
     * Marks this editor as dirty and notifies the registered listeners.
     */
    public void markDirty() {
        if (!m_isDirty) {
            m_isDirty = true;
            m_manager.setDirty();

            SyncExecQueueDispatcher.asyncExec(new Runnable() {
                @Override
                public void run() {
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
            if (m_parentEditor != null) {
                m_parentEditor.markDirty();
            }
        }
    }

    /**
     * we need to listen for resource changes to get informed if the currently
     * opened file in the navigator is renamed or deleted.
     *
     * {@inheritDoc}
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        try {
            if (event == null || event.getDelta() == null) {
                return;
            }
            event.getDelta().accept(new MyResourceDeltaVisitor());
        } catch (CoreException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    /**
     * Simple visitor, checks whether the currently opened file has been renamed
     * and sets the new name in the editors' tab.
     */
    private class MyResourceDeltaVisitor implements IResourceDeltaVisitor {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean visit(final IResourceDelta delta) throws CoreException {
            if (!m_fileResource.equals(delta.getResource().getLocationURI())) {
                // doesn't concern us
                return true;
            }
            if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
                // remove workflow.knime from moved to path
                IPath newDirPath = delta.getMovedToPath();
                // directory name (without workflow groups)
                final String newName = newDirPath.lastSegment();
                WorkflowEditor.this.m_manager.renameWorkflowDirectory(newName);
                URI newDirURI =
                        ResourcesPlugin.getWorkspace().getRoot()
                                .findMember(newDirPath).getLocationURI();
                ProjectWorkflowMap.replace(newDirURI,
                        WorkflowEditor.this.m_manager, m_fileResource);
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        String newTitle =
                                m_manager.getID().getIDWithoutRoot() + ": "
                                        + newName;
                        setTitleToolTip(newTitle);
                        setPartName(newTitle);
                        m_fileResource =
                                ResourcesPlugin.getWorkspace().getRoot()
                                        .getFile(delta.getMovedToPath())
                                        .getLocationURI();
                        WorkflowEditor.super.setInput(new FileEditorInput(
                                ResourcesPlugin.getWorkspace().getRoot()
                                        .getFile(delta.getMovedToPath())));
                    }
                });
                /* false = don't visit children */
                return false;
            }
            if ((delta.getKind() == IResourceDelta.REMOVED)) {
                LOGGER.info(new Path(m_fileResource.getPath()).lastSegment()
                        + " resource has been removed.");

                // close the editor
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        getEditorSite().getPage().closeEditor(
                                WorkflowEditor.this, false);
                    }
                });
                /* false = don't visit children */
                return false;
            }

            return true;
        }
    }

    /**
     * Sets the underlying workflow manager for this editor.
     *
     * @param manager the workflow manager to set
     */
    void setWorkflowManager(final WorkflowManager manager) {
        m_manager = manager;
    }

    /**
     * @return returns true if this editor has been closed on the workbench
     */
    public boolean isClosed() {
        return m_closed;
    }

    /**
     * Transposes a point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     */
    public static void transposeZoom(final ZoomManager zoomManager,
            final Point pointToAdapt, final boolean adaptViewPortLocation) {

        double zoomLevel = zoomManager.getZoom();

        // adapt the location accordint to the zoom level
        pointToAdapt.x = (int)(pointToAdapt.x * zoomLevel);
        pointToAdapt.y = (int)(pointToAdapt.y * zoomLevel);

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x -= viewPortLocation.x;
            pointToAdapt.y -= viewPortLocation.y;
        }
    }

    /**
     * Adapts a point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     */
    public static void adaptZoom(final ZoomManager zoomManager,
            final Point pointToAdapt, final boolean adaptViewPortLocation) {

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x += viewPortLocation.x;
            pointToAdapt.y += viewPortLocation.y;
        }
        double zoomLevel = zoomManager.getZoom();

        // adapt the location according to the zoom level
        pointToAdapt.x = (int)(pointToAdapt.x * (1.0 / zoomLevel));
        pointToAdapt.y = (int)(pointToAdapt.y * (1.0 / zoomLevel));
    }

    /**
     * Adapts a precission point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     */
    public static void adaptZoom(final ZoomManager zoomManager,
            final PrecisionPoint pointToAdapt,
            final boolean adaptViewPortLocation) {

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x += viewPortLocation.x;
            pointToAdapt.y += viewPortLocation.y;
        }
        double zoomLevel = zoomManager.getZoom();

        // adapt the location accordint to the zoom level
        pointToAdapt.preciseX = (pointToAdapt.x * (1.0 / zoomLevel));
        pointToAdapt.preciseY = (pointToAdapt.y * (1.0 / zoomLevel));
    }

    /**
     * Set if the workflow loading process. Should only be invoked during
     * workflow loading.
     *
     * @param dirty whether the editor should be marked as dirty or not
     * @see LoadWorkflowRunnable
     */
    void setIsDirty(final boolean dirty) {
        m_isDirty = dirty;
    }

    public WorkflowSelectionTool getSelectionTool() {
        return m_selectionTool;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        markDirty();
    }

    /**
     * UI information listener only relevant if this editor is for a meta node
     * (update part name). {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        updatePartName();
    }

    /** {@inheritDoc} */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        // nothing in the properties would affect an open editor
    }


}
