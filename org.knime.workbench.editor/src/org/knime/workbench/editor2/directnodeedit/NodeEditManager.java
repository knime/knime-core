/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
