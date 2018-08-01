/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   13.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.workbench.editor2.EditorModeParticipant;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractPortFigure extends Shape implements EditorModeParticipant {
    private static final Color COLOR_FLOW_VAR_PORT;

    static {
        int c = FlowVariablePortObject.TYPE.getColor();
        COLOR_FLOW_VAR_PORT = new Color(Display.getCurrent(), (c & 0xff0000) >> 16, (c & 0x00ff00) >> 8, (c & 0x0000ff));
    }

    /**
     *
     * @param c the color to lighten up
     * @return a new color derived from the argument
     */
    protected static Color lightenColor(final Color c) {
        try {
            float[] hsb = new float[3];
            java.awt.Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
            // bring down the saturation
            hsb[1] = Math.max(0, hsb[1] * 0.5f);
            // push up the lightness
            hsb[2] = Math.min(1.0f, hsb[2] + ((1.0f - hsb[2]) * 0.5f));
            int lCol = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            return new Color(Display.getCurrent(), (lCol >> 16) & 0xFF,
                    (lCol >> 8) & 0xFF, lCol & 0xFF);
        } catch (Throwable t) {
            return c;
        }
    }

    /**
     * The only color (so far) used outside this class (for connections).
     *
     * @return the color used to color flow variable ports.
     */
    public static Color getFlowVarPortColor() {
        return COLOR_FLOW_VAR_PORT;
    }

    /** @return the pixel size of the port when used as part of workflow port bar **/
    public static int getPortSizeWorkflow() {
        return 20;
    }

    /** @return the pixel size of the port for a node **/
    public static int getPortSizeNode() {
        return 9;
    }


    private int m_nrOfPorts;

    private final PortType m_portType;

    private boolean m_isConnected;

    private int m_portIdx;

    private final boolean m_isMetaNodePort;

    private WorkflowEditorMode m_currentEditorMode = WorkflowEditor.INITIAL_EDITOR_MODE;

    /**
     * @param type port type
     * @param nrOfPorts total number of ports
     * @param portIdx the index of this port within its parent
     * @param metaNodePort whether this node is a port for a metanode
     */
    public AbstractPortFigure(final PortType type, final int nrOfPorts, final int portIdx, final boolean metaNodePort) {
        m_portType = type;
        m_nrOfPorts = nrOfPorts;
        m_portIdx = portIdx;
        m_isMetaNodePort = metaNodePort;
        m_isConnected = false;
        setFill(true);
        setOutline(true);
        setOpaque(true);
    }

    /**
     * @return the type of the port
     */
    protected PortType getType() {
        return m_portType;
    }

    /**
     * @return total number of ports
     */
    protected int getNrPorts() {
        return m_nrOfPorts;
    }

    /**
     * @param numOfPorts new number of ports in the parent node
     */
    public void setNumberOfPorts(final int numOfPorts) {
        m_nrOfPorts = numOfPorts;
    }

    /**
     * @param isConnected the new value to set.
     */
    public void setIsConnected(final boolean isConnected) {
        m_isConnected = isConnected;
    }

    /**
     * @return whether this port has a connection
     */
    public boolean isConnected() {
        return m_isConnected;
    }

    /**
     * @return the index of the port
     */
    public int getPortIndex() {
        return m_portIdx;
    }

    /**
     * @param portIdx the portIdx to set
     */
    public void setPortIdx(final int portIdx) {
        m_portIdx = portIdx;
    }

    /**
     * @return whether this port represents a port for a metanode
     */
    protected boolean isMetaNodePort() {
        return m_isMetaNodePort;
    }

    /**
     * @return true, if the underlying port is a flow variable port generated by the framework
     */
    protected boolean isImplFlowVarPort() {
        return !m_isMetaNodePort && (m_portIdx == 0) && getType().isOptional()
                && getType().equals(FlowVariablePortObject.TYPE);
    }

    /**
     * {@inheritDoc}
     *
     * We need to set the colors explicitly dependend on the {@link PortType}.
     *
     * @return the background color, dependend on the {@link PortType}
     */
    @Override
    public Color getBackgroundColor() {
        // the colors set here get lightened up, if the port is optional
        final int c = getType().getColor();
        final boolean erasePort = getType().equals(FlowVariablePortObject.TYPE) && !m_isMetaNodePort && (m_portIdx == 0)
            && !m_isConnected && !showFlowVarPorts();
        Color color = erasePort ? Display.getCurrent().getSystemColor(SWT.COLOR_WHITE)
            : new Color(Display.getCurrent(), (c & 0xff0000) >> 16, (c & 0x00ff00) >> 8, (c & 0x0000ff));

        if (!WorkflowEditorMode.NODE_EDIT.equals(m_currentEditorMode) && !erasePort) {
            color = ColorConstants.lightGray;
        }

        return color;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getForegroundColor() {
        return getBackgroundColor();
    }

    /**
     * The color is determined with {@link #getBackgroundColor()} and set.
     *
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());
        graphics.setForegroundColor(getForegroundColor());
        super.paintFigure(graphics);

    }

    /**
     * Fills the shape, the points of the actual shape are set in
     * {@link NodeInPortFigure#createShapePoints(Rectangle)} and
     * {@link NodeOutPortFigure#createShapePoints(Rectangle)}. Only data ports
     * (ports of type {@link BufferedDataTable#TYPE}) are outlined, all other
     * port types are filled.
     *
     * {@inheritDoc}
     *
     * @see NodeInPortFigure#createShapePoints(Rectangle)
     * @see NodeOutPortFigure#createShapePoints(Rectangle)
     */
    @Override
    protected void fillShape(final Graphics graphics) {

        if (isImplFlowVarPort() && !showFlowVarPorts() && !m_isConnected) {
            // do not draw implicit flow ports if we are not supposed to
            return;
        }

        // switch antialiasing on
        graphics.setAntialias(SWT.ON);

        // Data ports and unconnected implicit flowVar ports are not filled.
        if (isFilled()) {
            Rectangle r = computePortShapeBounds(getBounds().getCopy());
            // fill doesn't include the rectangle's borders (outline does!)
            r.width++;
            r.height++;

            PointList points = createShapePoints(r);

            // variable ports are round circles (in their rectangle)
            if (getType().equals(FlowVariablePortObject.TYPE)) {
                if (points.size() == 4) {
                    Rectangle p =
                            new Rectangle(points.getPoint(0),
                                    points.getPoint(2));
                    // to be able to draw the full circle one pixel has to be subtracted from the width and height
                    Rectangle smallerP = new Rectangle(p.x, p.y, p.width - 1, p.height - 1);
                    graphics.fillOval(smallerP);
                } else {
                    graphics.fillPolygon(points);
                }
            } else {
                // others are polygons
                graphics.fillPolygon(points);
            }
        }
    }

    /**
     * Optional ports are not filled. Mandatory ports and FlowVar ports are filled.
     *
     * @return true if port is filled (mandatory)
     */
    private boolean isFilled() {
        if (!getType().isOptional() || isImplFlowVarPort()) {
            return true;
        }
        return false;
    }

    /**
     * @return true if implicit flow variable ports are currently shown, or true if the parent of this port is not an
     *         instance of NodeContainerFigure
     */
    protected boolean showFlowVarPorts() {
        IFigure p = getParent();
        if (p instanceof NodeContainerFigure) {
            return ((NodeContainerFigure)getParent()).getShowFlowVarPorts();
        } else {
            return true;
        }
    }

    /**
     * @param boundsWithYLocationToUse the y location of this will be used in the returned bounds
     * @return the computed bounds of this port shape
     */
    public Rectangle computePortShapeBounds(final Rectangle boundsWithYLocationToUse) {
        // Ports are located at the top of their bounds, next to the node icon
        NodeContainerFigure parent = (NodeContainerFigure)getParent();
        Rectangle iconBounds = parent.getSymbolFigure().getBounds().getCopy();
        int size = getPreferredSize().width;
        int x;
        if (this instanceof NodeInPortFigure) {
            // input ports are left of the node's symbol
            x = iconBounds.x - size;
            if (isImplFlowVarPort()) {
                // move the mickey mouse ears towards the middle.
                x += 5;
            }
        } else {
            // output ports are right of the node's symbol
            x = iconBounds.x + iconBounds.width;
            if (isImplFlowVarPort()) {
                // move the mickey mouse ears towards the middle.
                x -= 5;
            }
        }
        return new Rectangle(x, boundsWithYLocationToUse.y, size, size);
    }

    /**
     * Outlines the shape, the points of the actual shape are set in
     * {@link NodeInPortFigure#createShapePoints(Rectangle)} and
     * {@link NodeOutPortFigure#createShapePoints(Rectangle)}. Only data ports
     * (ports of type {@link BufferedDataTable#TYPE})are outlined, all other
     * port types are filled.
     *
     * {@inheritDoc}
     *
     * @see NodeInPortFigure#createShapePoints(Rectangle)
     * @see NodeOutPortFigure#createShapePoints(Rectangle)
     */
    @Override
    protected void outlineShape(final Graphics graphics) {
        if (isImplFlowVarPort() && !showFlowVarPorts() && !m_isConnected) {
            // do not draw implicit flow ports if we are not supposed to
            return;
        }

        // switch antialiasing on
        graphics.setAntialias(SWT.ON);

        if (!isFilled()) {
            Rectangle r = computePortShapeBounds(getBounds().getCopy());
            PointList points = createShapePoints(r);
            // variable ports are round circles (in their rectangle)
            if (getType().equals(FlowVariablePortObject.TYPE)) {
                if (points.size() == 4) {
                    Rectangle p =
                            new Rectangle(points.getPoint(0),
                                    points.getPoint(2));
                    // to be able to draw the full circle one pixel has to be subtracted from the width and height
                    Rectangle smallerP = new Rectangle(p.x, p.y, p.width - 1, p.height - 1);
                    graphics.drawOval(smallerP);
                } else {
                    graphics.drawPolygon(points);
                }
            } else {
                // others are polygons
                graphics.drawPolygon(points);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowEditorModeWasSet(final WorkflowEditorMode newMode) {
        m_currentEditorMode = newMode;

        repaint();
    }

    // ////////////////////////////////////////////////////////////////
    //
    // ABSTRACT METHODS
    //
    // ////////////////////////////////////////////////////////////////

    /**
     * Create a point list for the triangular figure (a polygon).
     *
     * @param r The bounds
     * @return the pointlist
     */
    protected abstract PointList createShapePoints(final Rectangle r);

    /**
     * Children must return a <code>Locator</code> that calculate the position
     * inside the hosting figure.
     *
     * @return The locator
     */
    public abstract Locator getLocator();

    /**
     * @return size of the square inside the bounds, where the port is plotted
     */
    @Override
    public abstract Dimension getPreferredSize(int wHint, int hHint);
}
