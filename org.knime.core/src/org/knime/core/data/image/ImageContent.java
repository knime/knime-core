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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.data.image;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.knime.core.data.DataCell;


/** Generic content of an image. Such content objects are used in
 * (individual) data cell implementations and generic port objects.
 *
 * <p><b>Note:</b> Objects of this interface must be read-only!
 *
 * Implementations are required to provide a no-arg constructor that is used
 * by the framework to restore the image content. As objects of this class
 * are generally read-only, the documentation should state that access to
 * the no-arg constructor is discouraged for client code.
 *
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 */
public interface ImageContent {

    /** Render image into argument graphics object.
     * @param g To paint to.
     * @param width image width
     * @param height image height
     */
    public void paint(final Graphics2D g, final int width, final int height);

    /** Preferred dimension, width and height, for the given image to be
     * rendered.
     * @return preferred dimension
     */
    public Dimension getPreferredSize();

    /** Factory method to generate cell implementation.
     * @return A (likely new) cell representing this image.
     */
    public DataCell toImageCell();

    /** Save the image content to an output stream.
     * @param out To save to.
     * @throws IOException If that fails.
     */
    public void save(final OutputStream out) throws IOException;

    /** Framework method that is used along with the required no-arg
     * constructor to restore the image content. As objects of this class are
     * read-only, this method should not be used by client code.
     * @param in To read from.
     * @throws IOException If that fails for any reason.
     */
    public void load(final InputStream in) throws IOException;

}

