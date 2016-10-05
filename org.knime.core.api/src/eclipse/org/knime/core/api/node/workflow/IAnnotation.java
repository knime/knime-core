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
 *   Sep 15, 2016 (hornm): created
 */
package org.knime.core.api.node.workflow;

import org.knime.core.api.node.workflow.AnnotationData.StyleRange;
import org.knime.core.api.node.workflow.AnnotationData.TextAlignment;

/**
 * An annotation on the workflow. It keeps all relevant information, such as
 * text, bg color and individual formatting.
 *
 * @author Martin Horn, KNIME.com
 */
public interface IAnnotation<D extends AnnotationData> extends Cloneable {

    /** @return the data */
    D getData();

    /**
     * Sets the entire annotation data.
     *
     * @param data the annotation data to be set, don't need to copied since {@link AnnotationData} is immutable
     */
    void setData(D data);

    /** @return the text */
    String getText();

    /** @return the styleRanges */
    StyleRange[] getStyleRanges();

    /** @return the bgColor */
    int getBgColor();

    /** @return the x */
    int getX();

    /** @return the y */
    int getY();

    /** @return the width */
    int getWidth();

    /** @return the height */
    int getHeight();

    /** @return the alignment */
    TextAlignment getAlignment();

    /** @return the border size, 0 or neg. for none.
     * @since 3.0*/
    int getBorderSize();

    /** @return the border color.
     * @since 3.0*/
    int getBorderColor();

    /**
     * @return the default font size for this annotation, or -1 if size from pref page should be used (for old
     * annotations only)
     * @since 3.1
     */
    int getDefaultFontSize();

    /**
     * @return The version to guarantee backward compatible look.
     * @see org.knime.core.node.workflow.AnnotationData#getVersion()
     * @since 3.0
     */
    int getVersion();

    /** Shift annotation after copy&amp;paste.
     * @param xOff x offset
     * @param yOff y offset
     */
    void shiftPosition(int xOff, int yOff);

    /**
     * Set dimensionality.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param width width of component
     * @param height height of component
     */
    void setDimension(int x, int y, int width, int height);

    /** {@inheritDoc} */
    @Override
    String toString();


    /**
     * {@inheritDoc}
     */
    IAnnotation<D> clone();

    /**
     * Copy content, styles, position from the argument and notify listeners.
     *
     * @param annotationData To copy from.
     * @param includeBounds Whether to also update x, y, width, height. If
     * false, it will only a copy the text with its styles
     */
    void copyFrom(D annotationData, boolean includeBounds);

    void addUIInformationListener(NodeUIInformationListener l);

    void removeUIInformationListener(NodeUIInformationListener l);

    /**
     * Informs all registered listeners that something has been changed.
     */
    public void fireChangeEvent();

}