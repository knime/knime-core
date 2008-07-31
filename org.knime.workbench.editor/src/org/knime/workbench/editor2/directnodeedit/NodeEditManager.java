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
 * History
 *   10.05.2005 (sieb): created
 */
package org.knime.workbench.editor2.directnodeedit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.CellEditorActionHandler;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;

import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;

import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * This Manager is responsible to create and control the direct edit cell for
 * direct editing of the user node name.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeEditManager extends DirectEditManager {
    private IActionBars m_actionBars;

    private CellEditorActionHandler m_actionHandler;

    private IAction m_copy, m_cut, m_paste, m_undo, m_redo, m_find,
            m_selectAll, m_delete;

    private Font m_scaledFont;

    /**
     * Creates a node edit manager.
     * 
     * @param source the underlying edit part
     * @param locator the locator to determine the position of the direct edit
     *            cell
     */
    public NodeEditManager(final GraphicalEditPart source,
            final CellEditorLocator locator) {
        super(source, null, locator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void bringDown() {

        if (m_actionHandler != null) {
            m_actionHandler.dispose();
            m_actionHandler = null;
        }

        if (m_actionBars != null) {
            restoreSavedActions(m_actionBars);
            m_actionBars.updateActionBars();
            m_actionBars = null;
        }

        Font disposeFont = m_scaledFont;
        m_scaledFont = null;
        super.bringDown();
        if (disposeFont != null) {
            disposeFont.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CellEditor createCellEditorOn(final Composite composite) {
        return new TextCellEditor(composite, SWT.SINGLE);
    }

    /**
     * Initializes the cell editor.
     * 
     * @see org.eclipse.gef.tools.DirectEditManager#initCellEditor()
     */
    @Override
    protected void initCellEditor() {

        Text text = (Text)getCellEditor().getControl();
        NodeContainerFigure nodeFigure = (NodeContainerFigure)getEditPart()
                .getFigure();
        String initialLabelText = nodeFigure.getCustomName();
        getCellEditor().setValue(initialLabelText);
        IFigure figure = getEditPart().getFigure();
        m_scaledFont = figure.getFont();
        FontData data = m_scaledFont.getFontData()[0];
        Dimension fontSize = new Dimension(0, data.getHeight());
        nodeFigure.translateToAbsolute(fontSize);
        data.setHeight(fontSize.height);
        m_scaledFont = new Font(null, data);
        text.setFont(m_scaledFont);
        text.selectAll();

        // Hook the cell editor's copy/paste actions to the actionBars so that
        // they can
        // be invoked via keyboard shortcuts.
        m_actionBars = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().getActiveEditor().getEditorSite()
                .getActionBars();
        saveCurrentActions(m_actionBars);
        m_actionHandler = new CellEditorActionHandler(m_actionBars);
        m_actionHandler.addCellEditor(getCellEditor());
        m_actionBars.updateActionBars();
    }

    private void restoreSavedActions(final IActionBars actionBars) {
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), m_copy);
        actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), m_paste);
        actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
                m_delete);
        actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
                m_selectAll);
        actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), m_cut);
        actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), m_find);
        actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), m_undo);
        actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), m_redo);
    }

    private void saveCurrentActions(final IActionBars actionBars) {
        m_copy = actionBars.getGlobalActionHandler(ActionFactory.COPY.getId());
        m_paste = actionBars
                .getGlobalActionHandler(ActionFactory.PASTE.getId());
        m_delete = actionBars.getGlobalActionHandler(ActionFactory.DELETE
                .getId());
        m_selectAll = actionBars
                .getGlobalActionHandler(ActionFactory.SELECT_ALL.getId());
        m_cut = actionBars.getGlobalActionHandler(ActionFactory.CUT.getId());
        m_find = actionBars.getGlobalActionHandler(ActionFactory.FIND.getId());
        m_undo = actionBars.getGlobalActionHandler(ActionFactory.UNDO.getId());
        m_redo = actionBars.getGlobalActionHandler(ActionFactory.REDO.getId());
    }
}
