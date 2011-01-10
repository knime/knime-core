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
package org.knime.workbench.editor2.actions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.AbstractWorkflowEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Abstract base class for actions that do something with a
 * <code>NodeContainer</code> inside the <code>WorkflowEditor</code>. Note
 * that this hooks as a workflow listener as soon as the
 * <code>WorkflowManager</code> is available. This is needed, because
 * enablement of an action may change not only on selection changes but also on
 * workflow changes.
 *
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractNodeAction extends SelectionAction {

    private final WorkflowEditor m_editor;

    /**
     *
     * @param editor The editor that is associated with this action
     */
    public AbstractNodeAction(final WorkflowEditor editor) {
        super(editor);
        setLazyEnablementCalculation(true);

        m_editor = editor;

    }

    /**
     * @return The manager that is edited by the current editor. Subclasses may
     *         want to have a reference to this.
     *
     * Note that this value may be <code>null</code> if the editor has not
     * already been created completely !
     *
     */
    protected final WorkflowManager getManager() {
        return m_editor.getWorkflowManager();

    }

    /**
     * Calls <code>runOnNodes</code> with the current selected
     * <code>NodeContainerEditPart</code>s.
     *
     * @see org.eclipse.jface.action.IAction#run()
     */
    @Override
    public final void run() {

        // call implementation of this action in the SWT UI thread
        Display.getCurrent().syncExec(new Runnable() {
            @Override
            public void run() {
                runInSWT();
            }

        });
    }

    /**
     * Calls {@link #runOnNodes(NodeContainerEditPart[])}
     * with the current selected <code>NodeContainerEditPart</code>s.
     */
    public void runInSWT() {
        // get selected parts...
        final NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        runOnNodes(parts);

    }

    /** Get selected edit parts.
     * @param editPartClass The class of interest
     * @param <T> The class to the argument
     * @return The selected <code>EditParts</code> of the given part. */
    protected <T extends EditPart> T[] getSelectedParts(
            final Class<T> editPartClass) {
        return filterObjects(editPartClass, getSelectedObjects());
    }

    /** Get all edit parts.
     * @param editPartClass The class of interest
     * @param <T> The class to the argument
     * @return The <code>EditParts</code> of the given part. */
    protected <T extends EditPart> T[] getAllParts(
            final Class<T> editPartClass) {
        return filterObjects(editPartClass, getAllObjects());
    }

    /** @param editPartClass The class of interest
     * @param list To filter from
     * @param <T> The class to the argument
     * @return The selected <code>EditParts</code> of the given part. */
    public static final <T extends EditPart> T[] filterObjects(
            final Class<T> editPartClass, final List<?> list) {
        ArrayList<T> objects = new ArrayList<T>();

        // clean list, that is, remove all objects that are not edit
        // parts for a NodeContainer
        for (Object e : list) {
            if (editPartClass.isInstance(e)) {
                objects.add(editPartClass.cast(e));
            }
        }
        @SuppressWarnings("unchecked")
        T[] array = (T[])Array.newInstance(editPartClass, objects.size());
        return objects.toArray(array);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List getSelectedObjects() {
        ISelectionProvider provider = m_editor.getEditorSite()
                .getSelectionProvider();
        if (provider == null) {
            return Collections.EMPTY_LIST;
        }
        ISelection sel = provider.getSelection();
        if (!(sel instanceof IStructuredSelection)) {
            return Collections.EMPTY_LIST;
        }

        return ((IStructuredSelection)sel).toList();
    }

    /**
     * @return all objects of the selected editor site.
     */
    protected List getAllObjects() {

        ScrollingGraphicalViewer provider = (ScrollingGraphicalViewer)m_editor
                .getEditorSite().getSelectionProvider();
        if (provider == null) {
            return Collections.EMPTY_LIST;
        }

        // get parent of the node parts
        EditPart editorPart = (EditPart)provider.getRootEditPart()
                .getChildren().get(0);

        return editorPart.getChildren();
    }

    /**
     * Returns all edit parts with the given ids.
     *
     * @param nodeIds the node container ids to retrieve the edit parts for
     * @param connectionIds the connection container ids to retrieve the edit
     *            parts for
     * @return the edit parts of the specified ids
     */
    protected List<AbstractWorkflowEditPart> getEditPartsById(
            final int[] nodeIds, final int[] connectionIds) {

        throw new UnsupportedOperationException("This method no longer exist!");
    }

    /** {@inheritDoc} */
    @Override
    public abstract String getId();

    /** Clients can implement action code here (or overwrite
     * {@link #runInSWT()}).
     *
     * @param nodeParts The parts that the action should be executed on.
     */
    public abstract void runOnNodes(final NodeContainerEditPart[] nodeParts);

    /**
     * @return the underlying editor for this action
     */
    WorkflowEditor getEditor() {
        return m_editor;
    }
}
