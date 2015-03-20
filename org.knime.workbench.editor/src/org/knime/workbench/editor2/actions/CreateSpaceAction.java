/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   19.03.2015 (tibuch): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.draw2d.geometry.Point;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.CreateSpaceCommand.CreateSpaceDirection;

/**
 *
 * @author tibuch
 */
public class CreateSpaceAction extends MoveNodeAbstractAction {

    private Point m_point;

    public static final String ID = "knime.action.node.createspace";

    /**
     * @param editor
     */
    public CreateSpaceAction(final WorkflowEditor editor, final CreateSpaceDirection direction, final int distance) {
        super(editor);

        int multiX = 0;
        int multiY = 0;

        if (direction.equals(CreateSpaceDirection.TOP)) {
            multiX = 0;
            multiY = -1;
        } else if (direction.equals(CreateSpaceDirection.RIGHT)) {
            multiX = 1;
            multiY = 0;
        } else if (direction.equals(CreateSpaceDirection.BOTTOM)) {
            multiX = 0;
            multiY = 1;
        } else if (direction.equals(CreateSpaceDirection.LEFT)) {
            multiX = -1;
            multiY = 0;
        }

        m_point = new Point(multiX * distance, multiY * distance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Move node";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point getMoveDirection() {
        return m_point;
    }

}
