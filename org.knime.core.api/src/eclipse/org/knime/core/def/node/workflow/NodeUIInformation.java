/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.core.def.node.workflow;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Special <code>NodeUIInformation</code> object used by the workflow editor.
 * Basically this stores the visual bounds of the node in the workflow editor
 * pane. Note: To be independent of draw2d/GEF this doesn't use the "natural"
 * <code>Rectangle</code> object, but simply stores an <code>int[]</code>.
 *
 * @author Florian Georg, University of Konstanz
 */
public final class NodeUIInformation {

    private final int[] m_bounds;

    /** Set to true if the bounds are absolute (correct in the context of the
     * editor). It's false if the coordinates refer to relative coordinates and
     * need to be adjusted by the NodeContainerFigure#initFigure... method.
     * This field is transient and not stored as part of the
     * {@link #save(NodeSettingsWO)} method. A loaded object has always absolute
     * coordinates.
     */
    private final boolean m_hasAbsoluteCoordinates;

    /**
     * Since Ver.2.3.0 the x/y coordinates are the ones of the symbol of the
     * node (background image with node icon). Before the location was relative
     * to the top left corner of the node's figure. (Change was made to avoid
     * tilted connections with changing font size in the figure.) Only loading
     * ui info from an old workflow sets this flag to false - and causes an
     * offset to be applied (in the NodeContainerEditPart).
     */
    private final boolean m_symbolRelative;

    /**
     * Since v2.6.0 we support grid in the editor. The center of the node figure is snapped to the grid. The center
     * coordinates are only available after node figure creation. This flag adjusts the initially set coordinates to
     * snap the node figure to the grid. It should not be saved/loaded.
     */
    private final boolean m_roundToGrid;

    /**
     * If true, the location will be corrected at editpart activation time (when the figure is available), so that the
     * icon center (or what ever reference point is lined up with the grid) is placed at the coordinates (i.e. at the
     * drop location, which is the cursor position). (Available since 2.6, not saved/loaded.)
     */
    private final boolean m_isDropLocation;

    /**
     * Constructor to create a new {@link NodeUIInformation}-object from it's {@link Builder}. All builder fields are
     * copied.
     *
     * @param builder the builder
     */
    NodeUIInformation(final Builder builder) {
        m_bounds = builder.m_bounds.clone();
        m_hasAbsoluteCoordinates = builder.m_hasAbsoluteCoordinates;
        m_symbolRelative = builder.m_symbolRelative;
        m_roundToGrid = builder.m_roundToGrid;
        m_isDropLocation = builder.m_isDropLocation;
    }

    /**
     * @return the hasAbsoluteCoordinates (transient) field
     */
    public boolean hasAbsoluteCoordinates() {
        return m_hasAbsoluteCoordinates;
    }

    /**
     * If false, the coordinates are loaded with an old workflow and are
     * relative to the top left corner of the node's figure.
     * @return true if coordinates are relative to the figure's symbol (always the case since v2.3)
     */
    public boolean isSymbolRelative() {
        return m_symbolRelative;
    }

    /**
     * If set, the coordinates set should be changed right after edit part activation. They should be rounded to the
     * closest grid position.
     *
     * @return true if set coordinates should be snapped to grid.
     * @since 2.6
     */
    public boolean getSnapToGrid() {
        return m_roundToGrid;
    }

    /**
     * Return true, if the coordinates specify the location of the node drop and should be adjusted to place the node
     * figure center under the cursor.
     * @return true if coordinates specify the location of the node drop and should be adjusted
     * @since 2.6
     */
    public boolean isDropLocation() {
        return m_isDropLocation;
    }

    /**
     * @return Returns a clone of the bounds.
     */
    public int[] getBounds() {
        if (m_bounds != null) {
            return m_bounds.clone();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInformation clone() {
        //we should not provide a clone method
        //e.g. conflicts with the final-fields
        //see also https://stackoverflow.com/questions/2427883/clone-vs-copy-constructor-which-is-recommended-in-java
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_bounds == null) {
            return "not set";
        }
        return "x=" + m_bounds[0] + " y=" + m_bounds[1]
                + (m_symbolRelative ? "" : " (TOPLEFT!)") + " width="
                + m_bounds[2] + " height=" + m_bounds[3]
                + (m_hasAbsoluteCoordinates ? "(absolute)" : "(relative)");
    }

    /** @return new Builder with defaults. */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * @param nodeUIInfo object to copy the values from
     * @return new Builder with the values copied from the passed argument
     */
    public static final Builder builder(final NodeUIInformation nodeUIInfo) {
        return new Builder().copyFrom(nodeUIInfo);
    }

    /** Builder pattern for {@link NodeUIInformation}. */
    public static final class Builder {

        private int[] m_bounds = new int[]{0, 0, -1, -1};

        private boolean m_hasAbsoluteCoordinates = true;
        private boolean m_symbolRelative = true;
        private boolean m_roundToGrid = false;
        private boolean m_isDropLocation = false;

        /** Creates new object, the bounds to be set are assumed to be absolute. */
        private Builder() {
        }

        /** Copy all fields from argument and return this.
         * @param nodeUIInfo to copy from, not null.
         * @return this
         */
        public Builder copyFrom(final NodeUIInformation nodeUIInfo) {
            m_bounds = nodeUIInfo.m_bounds.clone();
            m_hasAbsoluteCoordinates = nodeUIInfo.m_hasAbsoluteCoordinates;
            m_roundToGrid = nodeUIInfo.m_roundToGrid;
            m_isDropLocation = nodeUIInfo.m_isDropLocation;
            m_symbolRelative = nodeUIInfo.m_symbolRelative;
            return this;
        }

        /**
         * If true is passed, the node figure icon is centered (right after NodeContainerEditPart activation) onto the grid.
         * It is placed on the nearest grid point (rounded).
         * @param snapIt true causes node to be positioned on the closest grid point.
         * @since 2.6
         * @return this
         */
        public Builder setSnapToGrid(final boolean snapIt) {
            m_roundToGrid = snapIt;
            return this;
        }

        /**
         * Set true, if the coordinates specify the location of the node drop and should be adjusted to place the node
         * figure center under the cursor.

         * @param isDropLoc true, if coordinates set are the location of the node drop
         * @since 2.6
         * @return this
         */
        public Builder setIsDropLocation(final boolean isDropLoc) {
            m_isDropLocation = isDropLoc;
            return this;
        }

        /**
         * Set to true if the bounds are absolute (correct in the context of the
         * editor). It's false if the coordinates refer to relative coordinates and
         * need to be adjusted by the NodeContainerFigure#initFigure... method.
         * This field is transient and not stored as part of a workflow save.
         * A loaded object has always absolute coordinates.
         * @param hasAbsoluteCoordinates
         * @return this
         *
         */
        public Builder setHasAbsoluteCoordinates(final boolean hasAbsoluteCoordinates) {
            m_hasAbsoluteCoordinates = hasAbsoluteCoordinates;
            return this;
        }

        /**
         * Sets the location. The x/y location must be relative to the node figure's
         * symbol (the background image that contains the node's icon). The
         * NodeContainerEditPart translates it into a figure location - taking the
         * actual font size (of the node name) into account.
         *
         * @param x x-coordinate of the figures symbol (i.e. the background image)
         * @param y y-coordinate of the figures symbol (i.e. the background image)
         * @param w width
         * @param h height
         * @return this
         *
         */
        public Builder setNodeLocation(final int x, final int y, final int w,
                final int h) {
            m_bounds[0] = x;
            m_bounds[1] = y;
            m_bounds[2] = w;
            m_bounds[3] = h;
            return this;
        }

        /**
         * Changes the position by setting the bounds left top corner according to
         * the given moving distance.
         *
         * @param moveDist the distance to change the left top corner
         * @return A clone of this ui information, whereby its x,y coordinates
         *         are shifted by the argument values.
         */
        public Builder translate(final int[] moveDist) {
            m_bounds[0] = m_bounds[0] + moveDist[0];
            m_bounds[1] = m_bounds[1] + moveDist[1];
            return this;
        }

        /**
         * If false, the coordinates are loaded with an old workflow and are
         * relative to the top left corner of the node's figure.
         * @param isSymbolRelative
         * @return this
         */
        public Builder setIsSymbolRelative(final boolean isSymbolRelative) {
            m_symbolRelative = isSymbolRelative;
            return this;
        }

        /** @return {@link NodeUIInformation} with current values. */
        public NodeUIInformation build() {
            return new NodeUIInformation(this);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

    }
}
