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
 *   24.03.2015 (Tim-Oliver Buchholz): created
 */
package org.knime.workbench.editor2;

import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.gef.requests.CreateRequest;
import org.knime.workbench.editor2.actions.CreateSpaceAction.CreateSpaceDirection;

/**
 * The drop request with all information about the drop action. This request is used for the node replace/insert/create
 * drop action.
 *
 * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich, Switzerland
 */
public class CreateDropRequest extends CreateRequest {

    /**
     * The type of the drop action.
     *
     * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich, Switzerland
     */
    public enum RequestType {
        /**
         * Insert a new node in a connection.
         */
        INSERT,
        /**
         * Replace an old node with a new one.
         */
        REPLACE,
        /**
         * Just create a new node. Normal drop on workbench.
         */
        CREATE
    }

    private RequestType m_type;

    private AbstractEditPart m_editPart;

    private boolean m_createSpace;

    private int m_distance;

    private CreateSpaceDirection m_direction;

    /**
     * A new CreateDropRequest. Default RequestType is CREATE and nothing will be moved.
     */
    public CreateDropRequest() {
        super();
        m_type = RequestType.CREATE;
        m_editPart = null;
        m_createSpace = false;
        m_distance = 0;
        m_direction = null;
    }

    /**
     * @return the m_type
     */
    public RequestType getRequestType() {
        return m_type;
    }

    /**
     * @param type the {@link RequestType} of the drop request
     */
    public void setRequestType(final RequestType type) {
        m_type = type;
    }

    /**
     * @return the m_editpart
     */
    public AbstractEditPart getEditPart() {
        return m_editPart;
    }

    /**
     * @param editpart which will be replaced or inserted into NOTE: not used if {@link RequestType#CREATE} is set
     */
    public void setEditPart(final AbstractEditPart editpart) {
        m_editPart = editpart;
    }

    /**
     * @return createSpace
     */
    public boolean createSpace() {
        return m_createSpace;
    }

    /**
     * @param createSpace create space between the nodes of the insertion edge
     */
    public void setCreateSpace(final boolean createSpace) {
        m_createSpace = createSpace;
    }

    /**
     * @return the distance
     */
    public int getDistance() {
        return m_distance;
    }

    /**
     * @param distance the distance added to the insertion edge length
     */
    public void setDistance(final int distance) {
        this.m_distance = distance;
    }

    /**
     * @return the direction
     */
    public CreateSpaceDirection getDirection() {
        return m_direction;
    }

    /**
     * @param direction the {@link CreateSpaceDirection} in which the space should be generated
     */
    public void setDirection(final CreateSpaceDirection direction) {
        m_direction = direction;
    }
}
