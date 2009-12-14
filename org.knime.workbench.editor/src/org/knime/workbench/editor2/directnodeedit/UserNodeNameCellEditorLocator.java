/* @(#)$RCSfile$ 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Text;

import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * The locator to locate the cell editor for the
 * <code>NodeContainerFigure</code>. The cell editor edits the user node
 * name.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class UserNodeNameCellEditorLocator implements CellEditorLocator {
    private NodeContainerFigure m_nodeContainerFigure;

    /**
     * Creates a new <code>CellEditorLocator</code> for a user node name cell
     * editor.
     * 
     * @param containerFigure the node container figure which contains the user
     *            node name label
     */
    public UserNodeNameCellEditorLocator(
            final NodeContainerFigure containerFigure) {

        setLabel(containerFigure);
    }

    /**
     * {@inheritDoc}
     */
    public void relocate(final CellEditor celleditor) {

        Text text = (Text)celleditor.getControl();
        Rectangle rect = m_nodeContainerFigure.getNameLabelRectangle();
        m_nodeContainerFigure.translateToAbsolute(rect);
        org.eclipse.swt.graphics.Rectangle trim = text.computeTrim(0, 0, 0, 0);
        rect.translate(trim.x, trim.y);
        rect.width += trim.width;
        rect.height += trim.height;
        text.setBounds(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @return the node container figure.
     */
    protected NodeContainerFigure getLabel() {

        return m_nodeContainerFigure;
    }

    /**
     * Sets the node container figure.
     * 
     * @param nodeContainerFigure The stickyNote to set
     */
    protected void setLabel(final NodeContainerFigure nodeContainerFigure) {

        m_nodeContainerFigure = nodeContainerFigure;
    }
}
