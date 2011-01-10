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
 * History
 *   Aug 8, 2005 (georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;

/**
 * Locator for port figures. Makes sure that the ports are "near" the centered
 * icon of the surrounding <code>NodeContainerFigure</code>
 *
 * @author Florian Georg, University of Konstanz
 */
public class NodePortLocator extends PortLocator {

    private final NodeContainerFigure m_parent;

    // private static final NodeLogger LOGGER = NodeLogger.getLogger(
    // NodePortLocator.class);

    /**
     * Creates a new locator.
     *
     * @param parent The parent figure (NodeContainerFigure)
     * @param isInport true if it is an in port, false if it's an out port
     * @param maxPorts max number of data ports to locate
     * @param portIndex The port index
     * @param portType type of the port
     * @param isMetaNodePort if port belongs to a meta node
     */
    public NodePortLocator(final NodeContainerFigure parent,
            final boolean isInport, final int maxPorts, final int portIndex,
            final PortType portType, final boolean isMetaNodePort) {
        super(portType, portIndex, isInport, maxPorts, isMetaNodePort);
        m_parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relocate(final IFigure fig) {
        /*
         * Coordinates are relative to the symbol of the node figure!
         */
        Rectangle symbolBounds =
                m_parent.getSymbolFigure().getBounds().getCopy();
        Rectangle parentBounds = m_parent.getBounds().getCopy();

        // try not to cover the node's symbol (that's used to move node)
        // we need like 7 pixels for the icon at a MetaNodeOutPortFigure
        int width = (parentBounds.width - symbolBounds.width) / 2 + 7;
        int height = AbstractPortFigure.NODE_PORT_SIZE;
        int x = 0;
        if (isInPort()) {
            x = parentBounds.x;
            if (isImplVariablePort()) {
                // move the mickey mouse ears to the center a bit
                x += 5;
            }
        } else {
            x = parentBounds.x + parentBounds.width - width;
            if (isImplVariablePort()) {
                // move the mickey mouse ears to the center a bit
                x -= 5;
            }
        }
        int y;
        // Y position:
        // Implicit flow variable ports are always at the very top.
        // Meta nodes don't have implicit flow variable ports.
        // With port count one or two we spread evenly
        // Multiple ports (more than 2) hang off the top of the icon
        if (isImplVariablePort()) {
            y = symbolBounds.y;
        } else {
            int portNr = getNrPorts();
            int portPos = getPortIndex();
            int iconHeight = 38; // the size of the icon use din 2.1.2
            int iconOffset = 5;  // from the top of the icon figure (in 2.1.2)
            if (!isMetaNodePort()) {
                // the implicit ports don't count in, when we spread ports
                portNr--;
                portPos--;
            }
            if (portNr > 2) {
                // hang them off the top
                y = symbolBounds.y + height + (portPos * (height + 1));
            } else {
                // spread them evenly
                double d = iconHeight / (double)portNr / 2;
                int middle = (int)((portPos * 2 + 1) * d);
                y = symbolBounds.y + iconOffset + middle - (height / 2) - 1;
            }
        }
        Rectangle bounds = new Rectangle(x, y, width, height);
        fig.setBounds(bounds);
    }
//
//    private void relocateMultiPortFigure(final IFigure fig) {
//        /*
//         * Coordinates are relative to the symbol of the node figure!
//         */
//        Rectangle symbolBounds =
//                m_parent.getSymbolFigure().getBounds().getCopy();
//        Rectangle parentBounds = m_parent.getBounds().getCopy();
//
//        int height = AbstractPortFigure.NODE_PORT_SIZE;
//        // try not to cover the node's symbol (that's used to move node)
//        // we need like 5 pixels for the icon at a MetaNodeOutPortFigure
//        int width = (parentBounds.width - symbolBounds.width) / 2 + 5;
//        int x = 0;
//        int position = getPortIndex();
//        if (isMetaNodePort()) {
//            position++; // meta nodes don't have implicit variable ports
//        }
//        int y = symbolBounds.y + (position * (height + 1));
//        if (isInPort()) {
//            x = parentBounds.x;
//            if (isImplVariablePort()) {
//                // move the mickey mouse ears to the center a bit
//                x += 5;
//            }
//        } else {
//            x = parentBounds.x + parentBounds.width - width;
//            if (isImplVariablePort()) {
//                // move the mickey mouse ears to the center a bit
//                x -= 5;
//            }
//        }
//        Rectangle bounds = new Rectangle(x, y, width, height);
//        fig.setBounds(bounds);
//
//    }

    private boolean isImplVariablePort() {
        return (!isMetaNodePort() && (getPortIndex() == 0) && getType().equals(
                FlowVariablePortObject.TYPE));
    }

}
