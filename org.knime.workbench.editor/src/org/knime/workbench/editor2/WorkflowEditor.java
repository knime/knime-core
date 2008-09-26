/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowException;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.actions.CancelAction;
import org.knime.workbench.editor2.actions.CancelAllAction;
import org.knime.workbench.editor2.actions.CopyAction;
import org.knime.workbench.editor2.actions.CutAction;
import org.knime.workbench.editor2.actions.DefaultOpenViewAction;
import org.knime.workbench.editor2.actions.ExecuteAction;
import org.knime.workbench.editor2.actions.ExecuteAllAction;
import org.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import org.knime.workbench.editor2.actions.NodeConnectionContainerDeleteAction;
import org.knime.workbench.editor2.actions.OpenDialogAction;
import org.knime.workbench.editor2.actions.PasteAction;
import org.knime.workbench.editor2.actions.PasteActionContextMenu;
import org.knime.workbench.editor2.actions.ResetAction;
import org.knime.workbench.editor2.actions.SetNameAndDescriptionAction;
import org.knime.workbench.editor2.actions.job.ProgressMonitorJob;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.ui.wizards.imports.WizardProjectsImportPage;

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
        IResourceChangeListener, NodeStateChangeListener {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(WorkflowEditor.class);

    /** Clipboard name. */
    public static final String CLIPBOARD_ROOT_NAME = "clipboard";

    // private static final int LINE_WIDTH_FOR_SELECTED_NODES = 2;
    //
    // private static final int LINE_WIDTH_FOR_UNSELECTED_NODES = 1;

    /**
     * The static clipboard for copy/cut/paste.
     */
    private static ClipboardObject clipboard;

    /** root model object (=editor input) that is handled by the editor. * */
    private WorkflowManager m_manager;

    /** The main graphical viewer embeded in this editor * */
    // private GraphicalViewer m_graphicalViewer;
    /** the editor's action registry. */
    private ActionRegistry m_actionRegistry;

    /** the <code>EditDomain</code>. */
    private final DefaultEditDomain m_editDomain;

    /** the dirty state. */
    private boolean m_isDirty;

    /** List with the action ids that are associated to this editor. * */
    private List<String> m_editorActions;

    private GraphicalViewer m_graphicalViewer;

    // private File m_file;
    private IFile m_fileResource;

    // if we are a subworkflow editor, we have to store the parent for saving
    private WorkflowEditor m_parentEditor;

    private NewOverviewOutlinePage m_overviewOutlinePage;

    private PropertySheetPage m_undoablePropertySheetPage;

    private final WorkflowSelectionTool m_selectionTool;

    /**
     * Stores possible exceptions from the workflow manager that can occur
     * during loading the workflow. The WorkflowException is a collection
     * exception (possible exceptions for each node).
     */
    private WorkflowException m_workflowException;

    private boolean m_loadingCanceled;

    private String m_loadingCanceledMessage;

    /**
     * Indicates if this editor has been closed.
     */
    private boolean m_closed;

 
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

        m_loadingCanceled = false;
        m_loadingCanceledMessage = "";
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
        // if (m_clipboard == null) {
        // m_clipboard = new Clipboard(getSite().getShell().getDisplay());
        // }
        //
        // return m_clipboard;
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

        // reset the workflow exception
        m_workflowException = null;

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

        // in case there occurred exceptions during loading the workflow
        // display them
        if (m_workflowException != null) {

            StringBuilder sb = new StringBuilder();
            sb.append("Exceptions occurred during workflow loading!\n\n");

            WorkflowException we = m_workflowException;
            int counter = 1;
            do {
                sb.append(counter + ". Exception:\n");
                String weMsg = we.getMessage();

                if (weMsg == null) {
                    sb.append("no details available");
                } else {
                    sb.append(weMsg);
                }

                sb.append("\n");
                we = we.getNextException();
                counter++;
            } while (we != null);

            // show message
            showInfoMessage("Workflow could not be loaded ...", sb.toString());
        }
    }

    private List<IEditorPart> getSubEditors() {
        List<IEditorPart> editors = new ArrayList<IEditorPart>();
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

        // remember that this editor has been closed
        m_closed = true;

        // remove appender listener from "our" NodeLogger
        NodeLogger.getLogger(WorkflowEditor.class).debug("Disposing editor...");
        

        for (IEditorPart child : getSubEditors()) {
            child.getEditorSite().getPage().closeEditor(child, false);
        }

        m_manager.removeListener(this);
        getSite().getWorkbenchWindow().getSelectionService()
                .removeSelectionListener(this);
        
        m_manager.removeNodeStateChangeListener(this);

        // remove resource listener..
        if (m_fileResource != null) {
            m_fileResource.getWorkspace().removeResourceChangeListener(this);
        }

        // TODO: we only have to do it on the parent
        if (m_parentEditor == null) {
            WorkflowManager.ROOT.removeProject(m_manager.getID());
        }

        getCommandStack().removeCommandStackListener(this);

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

        // super already does someting for us...
        super.createActions();

        // Stack actions

        StackAction undo = new UndoAction(this);
        StackAction redo = new RedoAction(this);

        // Editor Actions
        WorkbenchPartAction delete =
                new NodeConnectionContainerDeleteAction(this);
        WorkbenchPartAction save = new SaveAction(this);
        WorkbenchPartAction print = new PrintAction(this);

        // node actions
        //
        AbstractNodeAction openDialog = new OpenDialogAction(this);

        AbstractNodeAction execute = new ExecuteAction(this);
        AbstractNodeAction executeAll = new ExecuteAllAction(this);
        AbstractNodeAction cancelAll = new CancelAllAction(this);
        AbstractNodeAction cancel = new CancelAction(this);
        AbstractNodeAction executeAndView = new ExecuteAndOpenViewAction(this);
        AbstractNodeAction reset = new ResetAction(this);
        AbstractNodeAction setNameAndDescription =
                new SetNameAndDescriptionAction(this);

        AbstractNodeAction defaultOpenView = new DefaultOpenViewAction(this);

        // copy / cut / paste action
        CopyAction copy = new CopyAction(this);
        CutAction cut = new CutAction(this);
        PasteAction paste = new PasteAction(this);
        PasteActionContextMenu pasteContext = new PasteActionContextMenu(this);

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
        m_actionRegistry.registerAction(executeAndView);
        m_actionRegistry.registerAction(reset);
        m_actionRegistry.registerAction(setNameAndDescription);
        m_actionRegistry.registerAction(defaultOpenView);

        m_actionRegistry.registerAction(copy);
        m_actionRegistry.registerAction(cut);
        m_actionRegistry.registerAction(paste);
        m_actionRegistry.registerAction(pasteContext);

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
        m_editorActions.add(defaultOpenView.getId());

        m_editorActions.add(copy.getId());
        m_editorActions.add(cut.getId());
        m_editorActions.add(paste.getId());

    }

    /**
     * This hooks keys like F2 for editing, delete etc. inside the editor...
     *
     * @return The common (shared) key handler.
     */
    protected KeyHandler getCommonKeyHandler() {

        KeyHandler sharedKeyHandler = new KeyHandler();

        // Delete
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 0),
                getActionRegistry().getAction(ActionFactory.DELETE.getId()));

        // Edit
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0),
                getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));

        // Open Dialog on CR
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.CR, 0),
                getActionRegistry().getAction(OpenDialogAction.ID));

        return sharedKeyHandler;
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
                new WorkflowGraphicalViewerCreator(editorSite, this
                        .getActionRegistry()).createViewer(parent);

        viewer.addDropTargetListener(new MetaNodeTemplateDropTargetListener(
                this, viewer));
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
     * Configurs the graphical viewer.
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

        setDefaultInput(input);

        if (input instanceof WorkflowManagerInput) {
            setWorkflowManagerInput((WorkflowManagerInput)input);
        } else {

        // register listener to check wether the underlying knime file (input)
        // has been deleted or renamed
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
                IResourceChangeEvent.POST_CHANGE);

        setDefaultInput(input);
        // we only support file inputs

        // TODO: input should also be possible from WFM

        m_fileResource = ((IFileEditorInput)input).getFile();

        // TODO try to load a WFM-config from the file, create an empty one if
        // this fails
        // LOGGER.debug("Created new WFM object as input");

        LOGGER.debug("Resource File's project: " + m_fileResource.getProject());
        final File file = m_fileResource.getLocation().toFile();
        try {
            // FIXME:
            // setInput is called before the entire repository is loaded,
            // need to figure out how to do it the other way around
            // the static block needs to be executed, access
            // RepositoryManager.INSTANCE
            if (RepositoryManager.INSTANCE == null) {
                LOGGER.fatal("Repository Manager Instance must not be null!");
            }
            assert m_manager == null;

            // before loading the workflow, change the log level in case
            // this project was previously imported, this is done
            // to avoid error messages due to missing input data
            // -------------- Import Checking ----------------------
            // KNIME code (dirty fix to avoid error logs during
            // editor opening)

            IFile markerFile =
                    m_fileResource.getProject().getFile(
                            WizardProjectsImportPage.IMPORT_MARKER_FILE_NAME);
            // the file will be removed in the doSave method
            if (markerFile.exists()) {
                NodeLogger.setIgnoreLoadDataError(true);
            }
            // -------------- Import Checking End -----------------

            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();
            // this one sets the workflow manager in the editor
            LoadWorkflowRunnable loadWorflowRunnable =
                    new LoadWorkflowRunnable(this, file);
            ps.busyCursorWhile(loadWorflowRunnable);

            m_manager.setName(m_fileResource.getProject().getName());

            // check if the editor should be disposed
            if (m_manager == null) {
                if (m_loadingCanceled) {
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            getEditorSite().getPage().closeEditor(
                                    WorkflowEditor.this, false);
                            MessageBox mb =
                                    new MessageBox(Display.getDefault()
                                            .getActiveShell(),
                                            SWT.ICON_INFORMATION | SWT.OK);
                            mb.setText("Editor could not be opened");
                            mb.setMessage(m_loadingCanceledMessage);
                            mb.open();
                        }
                    });
                    throw new OperationCanceledException(
                            m_loadingCanceledMessage);
                } else {
                    throw new RuntimeException("Workflow could not be loaded");
                }
            }

            m_manager.addListener(this);
            m_manager.addNodeStateChangeListener(this);
        } catch (InterruptedException ie) {
            LOGGER.fatal("Workflow loading thread interrupted", ie);
        } catch (InvocationTargetException e) {
            LOGGER.fatal("Workflow could not be loaded.", e);
        } finally {
            NodeLogger.setIgnoreLoadDataError(false);
        }

        // Editor name (title)
        setPartName(m_manager.getID().getIDWithoutRoot() 
                + ": " + file.getParentFile().getName());

        if (getGraphicalViewer() != null) {
            loadProperties();
        }

        // update Actions, as now there's everything available
        updateActions();
        }
    }

    private void setWorkflowManagerInput(final WorkflowManagerInput input) {
        m_parentEditor = input.getParentEditor();
        WorkflowManager wfm =
                ((WorkflowManagerInput)input).getWorkflowManager();
        setWorkflowManager(wfm);
        setPartName(input.getName());
        wfm.addListener(this);
        if (getGraphicalViewer() != null) {
            loadProperties();
        }

        // update Actions, as now there's everything available
        updateActions();
        return;
    }

    /**
     * Sets the input in the super class for defaults.
     *
     * @param input the editor input object
     */
    void setDefaultInput(final IEditorInput input) {

        super.setInput(input);
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

        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    /**
     * Sets the snap functionality.
     */
    protected void loadProperties() {

        // Ruler properties
        // LogicRuler ruler =
        // getLogicDiagram().getRuler(PositionConstants.WEST);
        // RulerProvider provider = null;
        // if (ruler != null) {
        // provider = new LogicRulerProvider(ruler);
        // }
        // getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER,
        // provider);
        // ruler = getLogicDiagram().getRuler(PositionConstants.NORTH);
        // provider = null;
        // if (ruler != null) {
        // provider = new LogicRulerProvider(ruler);
        // }
        // getGraphicalViewer().setProperty(
        // RulerProvider.PROPERTY_HORIZONTAL_RULER, provider);
        // getGraphicalViewer().setProperty(
        // RulerProvider.PROPERTY_RULER_VISIBILITY,
        // new Boolean(getLogicDiagram().getRulerVisibility()));

        // Snap to Geometry property
        getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED,
                new Boolean(true));

        // Grid properties
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED,
                new Boolean(false));
        // Grid properties
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING,
                new Dimension(6, 6));
        // We keep grid visibility and enablement in sync
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE,
                new Boolean(false));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        LOGGER.debug("Saving workflow ...");

        // // create progress monitor
        // EventLoopProgressMonitor monitor2 =
        // (EventLoopProgressMonitor)monitor;
        //
        // ProgressHandler progressHandler = new ProgressHandler(monitor,
        // m_manager.getNodes().size());
        // DefaultNodeProgressMonitor progressMonitor = new
        // DefaultNodeProgressMonitor();
        // progressMonitor.addProgressListener(progressHandler);
        // ExecutionMonitor exec = new ExecutionMonitor(progressMonitor);

        // Exception messages from the inner thread
        final StringBuffer exceptionMessage = new StringBuffer();

        if (m_fileResource == null && m_parentEditor != null) {
            m_parentEditor.doSave(monitor);
            m_isDirty = false;
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
            return;
        }

        try {
            // make sure the resource is "fresh" before saving...
            // m_fileResource.refreshLocal(IResource.DEPTH_ONE, null);
            final File file = m_fileResource.getLocation().toFile();

            // If something fails an empty workflow is created
            // except when cancalation occured
            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();
            SaveWorkflowRunnable saveWorflowRunnable =
                    new SaveWorkflowRunnable(this, file, exceptionMessage,
                            monitor);
            ps.run(true, false, saveWorflowRunnable);
            // after saving the workflow, check for the import marker
            // and delete it
            // -------------- Import Checking ----------------------
            // KNIME code (dirty fix to avoid error logs during
            // editor opening)

            IFile markerFile =
                    m_fileResource.getProject().getFile(
                            WizardProjectsImportPage.IMPORT_MARKER_FILE_NAME);

            if (markerFile.exists()) {
                try {
                    markerFile.delete(true, null);
                } catch (Exception e) {
                    LOGGER.warn("Import maker file could not be deleted", e);
                }
            }
            // -------------- Import Checking End -----------------

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

            public void run() {
                try {
                m_fileResource.getProject().refreshLocal(
                        IResource.DEPTH_INFINITE,
                        monitor);
                } catch (CoreException ce) {
                    OperationCanceledException oce 
                        = new OperationCanceledException(
                                "Workflow was not saved: " + ce.toString());
                    oce.initCause(ce);
                    throw oce;
                }
            }                
        });


        // mark all sub editors as saved
        for (IEditorPart subEditor : getSubEditors()) {
            final WorkflowEditor editor = (WorkflowEditor)subEditor;
            ((WorkflowEditor)subEditor).setIsDirty(false);
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    editor.firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
        }
        // try {
        // // try to refresh project
        // // m_fileResource.getProject().refreshLocal(IResource.DEPTH_INFINITE,
        // // monitor);
        //
        // //archive attribute aendern
        // } catch (CoreException e) {
        // // TODO Auto-generated catch block
        // LOGGER.debug("", e);
        // }

        // mark sub editors as saved

        monitor.done();
    }

    /**
     * Shwos a simple information message.
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
     * {@inheritDoc}
     */
    @Override
    public void doSaveAs() {
        throw new UnsupportedOperationException("saveAs not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        // if we are a subworkflow editor we are never dirty
        if (m_parentEditor != null) {
            return false;
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

        // paint the incoming and outgoing connections of all
        // selected nodes "bold" (helps to differentiate the connections)
        // and paint the connections or the not any more selected nodes
        // normal
        // StructuredSelection structuredSelection = null;
        // if (selection instanceof StructuredSelection) {
        //
        // structuredSelection = (StructuredSelection)selection;
        //
        // // revert the bold connections for all old selections
        // // if available
        // if (m_boldNodeParts != null) {
        // for (Object element : m_boldNodeParts.toList()) {
        //
        // if (element instanceof NodeContainerEditPart) {
        //
        // // make the connections normal
        // makeConnectionsNormal((NodeContainerEditPart)element);
        // } else if (element instanceof ConnectionContainerEditPart) {
        //
        // makeConnectionNormal((ConnectionContainerEditPart)element);
        // }
        //
        // }
        // }
        //
        // // paint the connections of the new selection bold
        // for (Object element : structuredSelection.toList()) {
        //
        // if (element instanceof NodeContainerEditPart) {
        //
        // // make the connections bold
        // makeConnectionsBold((NodeContainerEditPart)element);
        // } else if (element instanceof ConnectionContainerEditPart) {
        //
        // makeConnectionBold((ConnectionContainerEditPart)element);
        // }
        // }
        // }

        // remember the new selection as the old one
        // m_boldNodeParts = structuredSelection;

    }

    // private void makeConnectionNormal(
    // final ConnectionContainerEditPart connectionPart) {
    // ((PolylineConnection)connectionPart.getFigure())
    // .setLineWidth(LINE_WIDTH_FOR_UNSELECTED_NODES);
    // }
    //
    // private void makeConnectionBold(
    // final ConnectionContainerEditPart connectionPart) {
    // ((PolylineConnection)connectionPart.getFigure())
    // .setLineWidth(LINE_WIDTH_FOR_SELECTED_NODES);
    // }

    // private void makeConnectionsBold(final NodeContainerEditPart nodePart) {
    //
    // for (ConnectionContainerEditPart connectionPart : nodePart
    // .getAllConnections()) {
    //
    // makeConnectionBold(connectionPart);
    // }
    // }
    //
    // private void makeConnectionsNormal(final NodeContainerEditPart nodePart)
    // {
    //
    // for (ConnectionContainerEditPart connectionPart : nodePart
    // .getAllConnections()) {
    //
    // makeConnectionNormal(connectionPart);
    // }
    // }

    /**
     * Called when the command stack has changed, that is, a GEF command was
     * executed (Add,Remove,....). This keeps track of the dirty state of the
     * editor.
     *
     * @see org.eclipse.gef.commands.CommandStackListener
     *      #commandStackChanged(java.util.EventObject)
     */
    @Override
    public void commandStackChanged(final EventObject event) {

        // update the actions (should enable undo/redo accordingly)
        updateActions(m_editorActions);

        // track the dirty state of the edit domain
        boolean b = m_editDomain.getCommandStack().isDirty();
        if (b != m_isDirty) {
            // If state has changed, notify listeners
            if (b) {
                markDirty();
            } else {
                m_isDirty = b;
            }
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }

    }

    private final Map<NodeID, ProgressMonitorJob> m_dummyNodeJobs =
            new HashMap<NodeID, ProgressMonitorJob>();

    /**
     * Listener callback, listens to workflow events and triggers UI updates.
     *
     * {@inheritDoc}
     */
    public void workflowChanged(final WorkflowEvent event) {

        LOGGER.debug("Workflow event triggered: " + event.toString());
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub

                markDirty();
                updateActions();

                if (event.getType().equals(WorkflowEvent.Type.NODE_WAITING)) {
                    NodeContainer nc = (NodeContainer)event.getOldValue();

                    NodeProgressMonitor pm =
                            (NodeProgressMonitor)event.getNewValue();

                    ProgressMonitorJob job =
                            new ProgressMonitorJob(nc.getCustomName() + " ("
                                    + nc.getName() + ")", pm, m_manager, nc,
                                    "Queued for execution...");
                    // Reverted as not properly ordered yet. Improve in next
                    // version
                    // job.schedule();

                    Object o = m_dummyNodeJobs.put(event.getID(), job);
                    assert (o == null);

                } else if (event.getType().equals(
                        WorkflowEvent.Type.NODE_FINISHED)) {
                    // TODO: Cleanup, Review, Beautify.
                    ProgressMonitorJob j =
                            m_dummyNodeJobs.remove(event.getID());
                    if (j != null) {
                        j.finish();
                    }
                }
            }
        });

    }

    /**
     * Marks this editor as diry and notifies the registered listeners.
     */
    public void markDirty() {
        if (!m_isDirty) {
            m_isDirty = true;
            m_manager.setDirty();

            Display.getDefault().asyncExec(new Runnable() {
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
     * @see org.eclipse.core.resources.IResourceChangeListener
     *      #resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
     */
    public void resourceChanged(final IResourceChangeEvent event) {
        try {
            if (event == null || event.getDelta() == null) {
                return;
            }
            event.getDelta().accept(new MyResourceDeltaVisitor());
        } catch (CoreException e) {
            // should never happen, I think...
            e.printStackTrace();
        }

    }

    /**
     * Simple visitor, checks wheter the currently opened file has been renamed
     * and sets the new name in the editors' tab.
     *
     * @author Florian Georg, University of Konstanz
     */
    private class MyResourceDeltaVisitor implements IResourceDeltaVisitor {
        /*
        private String getTypeString(final IResourceDelta delta) {
            StringBuffer buffer = new StringBuffer();

            if ((delta.getKind() & IResourceDelta.ADDED) != 0) {
                buffer.append("ADDED|");
            }
            if ((delta.getKind() & IResourceDelta.ADDED_PHANTOM) != 0) {
                buffer.append("ADDED_PHANTOM|");
            }
            if ((delta.getKind() & IResourceDelta.ALL_WITH_PHANTOMS) != 0) {
                buffer.append("ALL_WITH_PHANTOMS|");
            }
            if ((delta.getKind() & IResourceDelta.CHANGED) != 0) {
                buffer.append("CHANGED|");
            }
            if ((delta.getKind() & IResourceDelta.CONTENT) != 0) {
                buffer.append("CONTENT|");
            }
            if ((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
                buffer.append("DESCRIPTION|");
            }
            if ((delta.getKind() & IResourceDelta.ENCODING) != 0) {
                buffer.append("ENCODING|");
            }
            if ((delta.getKind() & IResourceDelta.MARKERS) != 0) {
                buffer.append("MARKERS|");
            }
            if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
                buffer.append("MOVED_FROM|");
            }
            if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
                buffer.append("MOVED_TO|");
            }
            if ((delta.getKind() & IResourceDelta.NO_CHANGE) != 0) {
                buffer.append("NO_CHANGE|");
            }
            if ((delta.getKind() & IResourceDelta.OPEN) != 0) {
                buffer.append("OPEN|");
            }
            if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
                buffer.append("REMOVED|");
            }
            if ((delta.getKind() & IResourceDelta.REMOVED_PHANTOM) != 0) {
                buffer.append("REMOVED_PHANTOM|");
            }
            if ((delta.getKind() & IResourceDelta.REPLACED) != 0) {
                buffer.append("REPLACED|");
            }
            if ((delta.getKind() & IResourceDelta.SYNC) != 0) {
                buffer.append("SYNC|");
            }
            if ((delta.getKind() & IResourceDelta.TYPE) != 0) {
                buffer.append("TYPE|");
            }
            return buffer.toString();
        }
        */

        /**
         * {@inheritDoc}
         */
        public boolean visit(final IResourceDelta delta) throws CoreException {

//            LOGGER.debug("Path: " + delta.getResource().getName() + "Parent: "
//                    + m_fileResource.getProject() + " Deltat type: "
//                    + getTypeString(delta));
            // Parent project removed? close this editor....
            if (m_fileResource.getProject().equals(delta.getResource())) {

                if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {

                    Display.getDefault().syncExec(new Runnable() {
                        public void run() {

                            setPartName(delta.getMovedToPath().segment(0));

                        }
                    });

                } else if ((delta.getKind() & IResourceDelta.REMOVED) != 0
                        && (delta.getFlags() & IResourceDelta.MOVED_TO) == 0) {
                    // We can't save the workflow here, so unsaved changes are
                    // definitly lost. Well, people deleting projects really
                    // should know what they're doing ;-)
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            getEditorSite().getPage().closeEditor(
                                    WorkflowEditor.this, false);
                        }
                    });
                }
            }

            // we're only interested in deltas that are about "our" resource
            if (!m_fileResource.equals(delta.getResource())) {
                return true;
            }

            // listen for "MOVED_TO" deltas...
            if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {

                // This updates UI, so we need the code to be executed in the
                // SWT UI thread.
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {

                        // // get the new name
                        // String newName =
                        // delta.getMovedToPath().lastSegment();
                        // // set the part name
                        // setPartName(newName);

                        // we need to update the file resource that we have
                        // currently opened in our editor, so that we now have
                        // the "new" file
                        m_fileResource =
                                ResourcesPlugin.getWorkspace().getRoot()
                                        .getFile(delta.getMovedToPath());
                        setDefaultInput(new FileEditorInput(m_fileResource));

                    }
                });

                return false;
            } else if ((delta.getKind() == IResourceDelta.REMOVED)) {
                LOGGER.info(m_fileResource.getName()
                        + " resource has been removed.");

                // close the editor
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        getEditorSite().getPage().closeEditor(
                                WorkflowEditor.this, false);
                    }
                });
            }

            return true;
        }
    }

    /**
     * Creates the underlying <code>WorkflowManager</code> for this editor.
     * Therefore the settings are loaded and the editor registeres itself as
     * listener to get workflow events.
     *
     * @param settings the settings representing this workflow
     */
    // void createWorkflowManager(final NodeSettings settings) {
    // m_manager = RepositoryManager.INSTANCE.loadWorkflowFromConfig(settings);
    // m_manager.addListener(this);
    // }
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

        // adapt the location accordint to the zoom level
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
     * Set if the workflow loading process was canceled. Should only be invoked
     * during workflow loading.
     *
     * @param canceled canceled or not
     * @see LoadWorkflowRunnable
     */
    void setLoadingCanceled(final boolean canceled) {
        m_loadingCanceled = canceled;
    }

    /**
     * Set if the workflow loading process was canceled and a message. Should
     * only be invoked during workflow loading.
     *
     * @param message the reason for the cancelation
     * @see LoadWorkflowRunnable
     */
    void setLoadingCanceledMessage(final String message) {
        m_loadingCanceled = true;
        m_loadingCanceledMessage = message;
    }

    /**
     * Set if the workflow loading process encountered an exception. Should only
     * be invoked during workflow loading.
     *
     * @param exception the exception to set
     * @see LoadWorkflowRunnable
     */
    void setWorkflowException(final WorkflowException exception) {
        m_workflowException = exception;
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
    public void stateChanged(NodeStateEvent state) {
        markDirty();
    }
}
