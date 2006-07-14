/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   12.07.2006 (sieb): created
 */
package de.unikn.knime.workbench.editor2.editparts.snap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.handles.HandleBounds;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.GroupRequest;

import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.editparts.AbstractPortEditPart;
import de.unikn.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;
import de.unikn.knime.workbench.editor2.editparts.NodeInPortEditPart;

/**
 * A temporary helper used to perform snapping to existing elements. This helper
 * can be used in conjunction with the
 * {@link org.eclipse.gef.tools.DragEditPartsTracker DragEditPartsTracker} when
 * dragging editparts within a graphical viewer. Snapping is based on the
 * existing children of a <I>container</I>. When snapping a rectangle, the
 * edges of the rectangle will snap to edges of other rectangles generated from
 * the children of the given container. Similarly, the centers and middles of
 * rectangles will snap to each other.
 * <P>
 * If the snap request is being made during a Move, Reparent or Resize, then the
 * figures of the participants of that request will not be used for snapping. If
 * the request is a Clone, then the figures for the parts being cloned will be
 * used as possible snap locations.
 * <P>
 * This helper does not keep up with changes made to the container editpart.
 * Clients should instantiate a new helper each time one is requested and not
 * hold on to instances of the helper.
 * 
 * @since 3.0
 * @author Randy Hudson
 * @author Pratik Shah
 */
public class SnapToPortGeometry extends SnapToHelper {

    /**
     * A property indicating whether this helper should be used. The value
     * should be an instance of Boolean. Currently, this class does not check to
     * see if the viewer property is set to <code>true</code>.
     * 
     * @see EditPartViewer#setProperty(String, Object)
     */
    public static final String PROPERTY_SNAP_ENABLED = "SnapToGeometry.isEnabled"; //$NON-NLS-1$

    /**
     * The key used to identify the North anchor point in the extended data of a
     * request. The north anchor may be set to an {@link Integer} value
     * indicating where the snapping is occurring. This is used for feedback
     * purposes.
     */
    public static final String KEY_NORTH_ANCHOR = "SnapToGeometry.NorthAnchor"; //$NON-NLS-1$

    /**
     * The key used to identify the South anchor point in the extended data of a
     * request. The south anchor may be set to an {@link Integer} value
     * indicating where the snapping is occurring. This is used for feedback
     * purposes.
     */
    public static final String KEY_SOUTH_ANCHOR = "SnapToGeometry.SouthAnchor"; //$NON-NLS-1$

    /**
     * The key used to identify the West anchor point in the extended data of a
     * request. The west anchor may be set to an {@link Integer} value
     * indicating where the snapping is occurring. This is used for feedback
     * purposes.
     */
    public static final String KEY_WEST_ANCHOR = "SnapToGeometry.WestAnchor"; //$NON-NLS-1$

    /**
     * The key used to identify the East anchor point in the extended data of a
     * request. The east anchor may be set to an {@link Integer} value
     * indicating where the snapping is occurring. This is used for feedback
     * purposes.
     */
    public static final String KEY_EAST_ANCHOR = "SnapToGeometry.EastAnchor"; //$NON-NLS-1$

    /**
     * A vertical or horizontal snapping point. since 3.0
     */
    protected static class Entry {
        /**
         * The side from which this entry was created. -1 is used to indicate
         * left or top, 0 indicates the middle or center, and 1 indicates right
         * or bottom.
         */
        int side;

        /**
         * The location of the entry, in the container's coordinates.
         */
        int offset;

        /**
         * Wheather this is an inport value
         */
        boolean m_inport;

        /**
         * Wheather this is a model port
         */
        boolean m_modelPort;

        /**
         * Constructs a new entry with the given side and offset.
         * 
         * @param side an integer indicating T/L, B/R, or C/M
         * @param offset the location
         */
        Entry(int side, int offset) {
            this.side = side;
            this.offset = offset;
        }

        /**
         * Constructs a new entry with the given side and offset.
         * 
         * @param side an integer indicating T/L, B/R, or C/M
         * @param offset the location
         * @param inport wheather this entry belongs to an inport
         */
        Entry(int side, int offset, boolean inport, boolean modelPort) {
            this.side = side;
            this.offset = offset;
            m_inport = inport;
            m_modelPort = modelPort;
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "Offset: " + offset + "Inport: " + m_inport;
        }
    }

    /**
     * The sensitivity of the snapping. Corrections greater than this value will
     * not occur.
     */
    protected static final double THRESHOLD = 5.0001;

    boolean cachedCloneBool;

    /**
     * The horizontal rows being snapped to.
     */
    protected Entry rows[];

    /**
     * The vertical columnd being snapped to.
     */
    protected Entry cols[];

    /**
     * The y port values of the dragged node.
     */
    protected Entry[] m_yValues;

    /**
     * The container editpart providing the coordinates and the children to
     * which snapping occurs.
     */
    protected GraphicalEditPart container;

    private ZoomManager m_zoomManager;

    /**
     * Constructs a helper that will use the given part as its basis for
     * snapping. The part's contents pane will provide the coordinate system and
     * its children determine the existing elements.
     * 
     * @since 3.0
     * @param container the container editpart
     */
    public SnapToPortGeometry(GraphicalEditPart container) {
        this.container = container;

        m_zoomManager = (ZoomManager)container.getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());
    }

    /**
     * Generates a list of parts which should be snapped to. The list is the
     * original children, minus the given exclusions, minus and children whose
     * figures are not visible.
     * 
     * @since 3.0
     * @param exclusions the children to exclude
     * @return a list of parts which should be snapped to
     */
    protected List generateSnapPartsList(List exclusions) {
        // Don't snap to any figure that is being dragged
        List children = new ArrayList(container.getChildren());
        children.removeAll(exclusions);

        // Don't snap to hidden figures
        List hiddenChildren = new ArrayList();
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            GraphicalEditPart child = (GraphicalEditPart)iter.next();
            if (!child.getFigure().isVisible())
                hiddenChildren.add(child);
        }
        children.removeAll(hiddenChildren);

        return children;
    }

    /**
     * Returns the correction value for the given entries and sides. During a
     * move, the left, right, or center is free to snap to a location.
     * 
     * @param entries the entries
     * @param extendedData the requests extended data
     * @param vert <code>true</code> if the correction is vertical
     * @param near the left/top side of the rectangle
     * @param far the right/bottom side of the rectangle
     * @return the correction amount or THRESHOLD if no correction was made
     */
    protected double getCorrectionFor(Entry entries[], Map extendedData,
            boolean vert, double near, double far) {
        far -= 1.0;
        double total = near + far;
        // If the width is even (i.e., odd right now because we have reduced one
        // pixel from
        // far) there is no middle pixel so favor the left-most/top-most pixel
        // (which is what
        // populateRowsAndCols() does by using int precision).
        if ((int)(near - far) % 2 != 0)
            total -= 1.0;
        double result = getCorrectionFor(entries, extendedData, vert,
                total / 2, 0);
        if (result == THRESHOLD)
            result = getCorrectionFor(entries, extendedData, vert, near, -1);
        if (result == THRESHOLD)
            result = getCorrectionFor(entries, extendedData, vert, far, 1);
        return result;
    }

    /**
     * Returns the correction value for the given entries and sides. During a
     * move, the left, right, or center is free to snap to a location.
     * 
     * @param entries the entries
     * @param extendedData the requests extended data
     * @return the correction amount or THRESHOLD if no correction was made
     */
    protected double getCorrectionForY(final Entry[] entries,
            final Map extendedData, final Entry[] ys, final int moveDelta) {

        // get the smallest distance to the next y value
        double result = Double.MAX_VALUE;
        for (Entry entry : entries) {
            // System.out.println("Entry: " + entry.offset);
            for (Entry y : ys) {

                // only compare inports to outports as only oposite parts
                // can connect and must be alligned
                if (!(entry.m_inport ^ y.m_inport)) {
                    continue;
                }

                // and only ports of same type (data - data, model-model)
                // are snaped
                if (entry.m_modelPort ^ y.m_modelPort) {
                    continue;
                }

                // System.out.println("y val: " + (y.offset + moveDelta));
                double diff = entry.offset - (y.offset + moveDelta);
                if (Math.abs(diff) < Math.abs(result)) {
                    result = diff;
                }
            }
        }

        return Math.round(result);
    }

    /**
     * Returns the correction value between ± {@link #THRESHOLD}, or the
     * THRESHOLD if no corrections were found.
     * 
     * @param entries the entries
     * @param extendedData the map for setting values
     * @param vert <code>true</code> if vertical
     * @param value the value being corrected
     * @param side which sides should be considered
     * @return the correction or THRESHOLD if no correction was made
     */
    protected double getCorrectionFor(Entry entries[], Map extendedData,
            boolean vert, double value, int side) {
        double resultMag = THRESHOLD;
        double result = THRESHOLD;

        String property;
        if (side == -1)
            property = vert ? KEY_WEST_ANCHOR : KEY_NORTH_ANCHOR;
        else
            property = vert ? KEY_EAST_ANCHOR : KEY_SOUTH_ANCHOR;

        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            double magnitude;

            if (entry.side == -1 && side != 0) {
                magnitude = Math.abs(value - entry.offset);
                if (magnitude < resultMag) {
                    resultMag = magnitude;
                    result = entry.offset - value;
                    extendedData.put(property, new Integer(entry.offset));
                }
            } else if (entry.side == 0 && side == 0) {
                magnitude = Math.abs(value - entry.offset);
                if (magnitude < resultMag) {
                    resultMag = magnitude;
                    result = entry.offset - value;
                    extendedData.put(property, new Integer(entry.offset));
                }
            } else if (entry.side == 1 && side != 0) {
                magnitude = Math.abs(value - entry.offset);
                if (magnitude < resultMag) {
                    resultMag = magnitude;
                    result = entry.offset - value;
                    extendedData.put(property, new Integer(entry.offset));
                }
            }
        }
        return result;
    }

    /**
     * Returns the rectangular contribution for the given editpart. This is the
     * rectangle with which snapping is performed.
     * 
     * @since 3.0
     * @param part the child
     * @return the rectangular guide for that part
     */
    protected Rectangle getFigureBounds(GraphicalEditPart part) {
        IFigure fig = part.getFigure();
        if (fig instanceof HandleBounds)
            return ((HandleBounds)fig).getHandleBounds();
        return fig.getBounds();
    }

    private List<AbstractPortEditPart> getPorts(List parts) {
        // add the port edit parts to a list
        List<AbstractPortEditPart> portList = new ArrayList<AbstractPortEditPart>();

        if (parts != null) {
            for (Object part : parts) {

                if (part instanceof NodeContainerEditPart) {
                    NodeContainerEditPart containerEditPart = (NodeContainerEditPart)part;

                    // get the port parts
                    for (Object childPart : containerEditPart.getChildren()) {
                        if (childPart instanceof AbstractPortEditPart) {
                            // add to list
                            portList.add((AbstractPortEditPart)childPart);
                        }
                    }
                }
            }
        }

        return portList;
    }

    /**
     * Updates the cached row and column Entries using the provided parts.
     * Columns are only the center of a node figure while rows are all ports of
     * a node.
     * 
     * @param parts a List of EditParts
     */
    protected void populateRowsAndCols(final List parts, final List dragedParts) {

        // add the port edit parts to a list
        List<AbstractPortEditPart> portList = getPorts(parts);

        // create all row relevant points fromt the port list
        Vector<Entry> rowVector = new Vector<Entry>();
        for (int i = 0; i < portList.size(); i++) {
            GraphicalEditPart child = (GraphicalEditPart)portList.get(i);
            Rectangle bounds = getFigureBounds(child);

            // get information is this is an inport
            boolean inport = false;
            if (portList.get(i) instanceof NodeInPortEditPart) {
                inport = true;
            }

            // get information is this is a model port
            rowVector.add(new Entry(0, bounds.y + (bounds.height - 1) / 2,
                    inport, portList.get(i).isModelPort()));
        }

        // add the port edit parts to a list
        List<AbstractPortEditPart> dargedPortList = getPorts(dragedParts);
        for (int i = 0; i < dargedPortList.size(); i++) {

            // for each port get a possible connection (if connected)
            AbstractPortEditPart portPart = dargedPortList.get(i);

            List sourceConnections = portPart.getSourceConnections();
            for (int j = 0; j < sourceConnections.size(); j++) {
                ConnectionContainerEditPart conPart = (ConnectionContainerEditPart)sourceConnections
                        .get(i);

                Point p = ((Connection)conPart.getFigure()).getPoints()
                        .getPoint(2);

                rowVector.add(new Entry(0, p.y - 1, true, portPart.isModelPort()));
            }

            List targetConnections = portPart.getTargetConnections();
            for (int j = 0; j < targetConnections.size(); j++) {
                ConnectionContainerEditPart conPart = (ConnectionContainerEditPart)targetConnections
                        .get(i);

                PointList pList = ((Connection)conPart.getFigure()).getPoints();
                Point p = pList.getPoint(pList.size() - 3);

                rowVector.add(new Entry(0, p.y - 1, false, portPart.isModelPort()));
            }
        }

        Vector<Entry> colVector = new Vector<Entry>();

        for (int i = 0; i < parts.size(); i++) {
            GraphicalEditPart child = (GraphicalEditPart)parts.get(i);
            Rectangle bounds = getFigureBounds(child);
            colVector.add(new Entry(0, bounds.x + (bounds.width - 1) / 2));
        }

        rows = rowVector.toArray(new Entry[rowVector.size()]);
        cols = colVector.toArray(new Entry[colVector.size()]);
    }

    /**
     * @see SnapToHelper#snapRectangle(Request, int, PrecisionRectangle,
     *      PrecisionRectangle)
     */
    public int snapRectangle(Request request, int snapOrientation,
            PrecisionRectangle baseRect, PrecisionRectangle result) {

        baseRect = baseRect.getPreciseCopy();
        makeRelative(container.getContentPane(), baseRect);
        PrecisionRectangle correction = new PrecisionRectangle();
        makeRelative(container.getContentPane(), correction);

        // Recalculate snapping locations if needed
        boolean isClone = request.getType().equals(RequestConstants.REQ_CLONE);
        List exclusionSet = null;
        if (rows == null || cols == null || isClone != cachedCloneBool) {
            cachedCloneBool = isClone;
            exclusionSet = Collections.EMPTY_LIST;
            if (!isClone && request instanceof GroupRequest) {
                exclusionSet = ((GroupRequest)request).getEditParts();
            }
            populateRowsAndCols(generateSnapPartsList(exclusionSet),
                    exclusionSet);
        }

        if ((snapOrientation & HORIZONTAL) != 0) {
            double xcorrect = THRESHOLD;
            xcorrect = getCorrectionFor(cols, request.getExtendedData(), true,
                    baseRect.preciseX, baseRect.preciseRight());
            if (xcorrect != THRESHOLD) {
                snapOrientation &= ~HORIZONTAL;
                correction.preciseX += xcorrect;
            }

            // System.out.println("Xcorrect:" + correction.preciseX
            // + " intermediat: " + xcorrect);
        }

        // get y values of the draged node part ports
        if (exclusionSet != null) {
            List<AbstractPortEditPart> ports = getPorts(exclusionSet);
            Entry[] yValues = new Entry[ports.size()];
            int i = 0;
            for (AbstractPortEditPart port : ports) {

                boolean inport = false;
                if (port instanceof NodeInPortEditPart) {
                    inport = true;
                }

                yValues[i] = new Entry(0, getFigureBounds(port).getLeft().y,
                        inport, port.isModelPort());
                i++;
            }
            m_yValues = yValues;
        }

        // get the move delta of the orignial location
        Point moveDeltaPoint = ((ChangeBoundsRequest)request).getMoveDelta();
        WorkflowEditor.adaptZoom(m_zoomManager, moveDeltaPoint, true);
        int moveDelta = moveDeltaPoint.y;
        if ((snapOrientation & VERTICAL) != 0) {
            double ycorrect = THRESHOLD;
            ycorrect = getCorrectionForY(rows, request.getExtendedData(),
                    m_yValues, moveDelta);
            if (Math.abs(ycorrect) < THRESHOLD) {
                snapOrientation &= ~VERTICAL;
                correction.preciseY += (ycorrect + 1);
            }

            // System.out.println("Ycorrect:" + correction.preciseY
            // + " intermediat: " + ycorrect + "delta: " + moveDelta);
        }

        if ((snapOrientation & EAST) != 0) {
            double rightCorrection = getCorrectionFor(cols, request
                    .getExtendedData(), true, baseRect.preciseRight() - 1, 1);
            if (rightCorrection != THRESHOLD) {
                snapOrientation &= ~EAST;
                correction.preciseWidth += rightCorrection;
            }
        }

        if ((snapOrientation & WEST) != 0) {
            double leftCorrection = getCorrectionFor(cols, request
                    .getExtendedData(), true, baseRect.preciseX, -1);
            if (leftCorrection != THRESHOLD) {
                snapOrientation &= ~WEST;
                correction.preciseWidth -= leftCorrection;
                correction.preciseX += leftCorrection;
            }
        }

        if ((snapOrientation & SOUTH) != 0) {
            double bottom = getCorrectionFor(rows, request.getExtendedData(),
                    false, baseRect.preciseBottom() - 1, 1);
            if (bottom != THRESHOLD) {
                snapOrientation &= ~SOUTH;
                correction.preciseHeight += bottom;
            }
        }

        if ((snapOrientation & NORTH) != 0) {
            double topCorrection = getCorrectionFor(rows, request
                    .getExtendedData(), false, baseRect.preciseY, -1);
            if (topCorrection != THRESHOLD) {
                snapOrientation &= ~NORTH;
                correction.preciseHeight -= topCorrection;
                correction.preciseY += topCorrection;
            }
        }

        correction.updateInts();
        makeAbsolute(container.getContentPane(), correction);
        result.preciseX += correction.preciseX;
        result.preciseY += correction.preciseY;
        result.preciseWidth += correction.preciseWidth;
        result.preciseHeight += correction.preciseHeight;
        result.updateInts();

        return snapOrientation;
    }
}
