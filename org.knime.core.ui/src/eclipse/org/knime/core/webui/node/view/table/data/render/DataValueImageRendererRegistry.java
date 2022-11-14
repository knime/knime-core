/*
 * ------------------------------------------------------------------------
 *
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
 *   Jul 19, 2022 (hornm): created
 */
package org.knime.core.webui.node.view.table.data.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.view.table.data.TableViewDataService;
import org.knime.core.webui.page.PageUtil;
import org.knime.core.webui.page.PageUtil.PageType;

/**
 * Allows one to (short-term) register {@link DataValueImageRenderer DataValueImageRenderers} together with their
 * respective {@link DataValue DataValues} to render.
 *
 * Required because the {@link TableViewDataService} doesn't return the rendered images directly but just a relative
 * image path. The image is only rendered once the browser uses the provided path to render the image. And this class is
 * intended to serve as the interchange between the place where the image path is returned via
 * {@link #addRendererAndGetImgPath(String, DataCell, DataValueImageRenderer)} (while iterating the data table) and the place
 * where the image is finally rendered {@link #renderAndRemove(String)} (the data value is accessed again to finally
 * render it for real).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class DataValueImageRendererRegistry {

    /**
     * URL-path prefix to be able to identify image resources for data cells that are rendered into images.
     */
    public static final String RENDERED_CELL_IMAGES_PATH_PREFIX = "images";

    private static final int DEFAULT_CELL_IMAGE_WIDTH = 100;

    private static final int DEFAULT_CELL_IMAGE_HEIGHT = 100;

    private static final Pattern WIDTH_AND_HEIGHT_PATTERN =
        Pattern.compile("w=(\\d+)&h=(\\d+)");

    private final Supplier<String> m_pageIdSupplier;

    // map from table-id to (map from cell hash code to pair of data-cell and renderer)
    private final Map<String, Map<String, Pair<DataCell, DataValueImageRenderer>>> m_cellAndRendererMapsPerTableId =
        Collections.synchronizedMap(new HashMap<>());

    private int m_hashCollisionCount = 0;

    /**
     * @param pageIdSupplier the page id of the view (see, e.g.,
     *            {@link PageUtil#getPageId(NativeNodeContainer, boolean, PageType)}). It's used to define the relative
     *            path where image resources (output of the data value renderers) are available. Supplied lazily because
     *            the page id is not available yet on construction time of renderer factory. Can be {@code null} if no
     *            values are to be rendered into images.
     */
    public DataValueImageRendererRegistry(final Supplier<String> pageIdSupplier) {
        m_pageIdSupplier = pageIdSupplier;
    }

    /**
     * Adds a new image renderer and the data cell to render to the registry and returns the image path.
     *
     * @param tableId the table to add the renderer for; must be globally unique
     * @param cell the data cell to add and to get the image path for
     * @param renderer the renderer to add
     *
     * @return the relative path where the image can be accessed
     */
    public String addRendererAndGetImgPath(final String tableId, final DataCell cell,
        final DataValueImageRenderer renderer) {
        var key = Integer.toString(31 * cell.hashCode() + renderer.getId().hashCode());
        var cellAndRendererMap = m_cellAndRendererMapsPerTableId.computeIfAbsent(tableId, id -> new HashMap<>());
        if (cellAndRendererMap.containsKey(key)) {
            var existingCell = cellAndRendererMap.get(key).getFirst();
            if (!cell.equals(existingCell)) {
                // hash collision
                key += "_" + m_hashCollisionCount;
                m_hashCollisionCount++; // NOSONAR
            }
        }
        cellAndRendererMap.put(key, Pair.create(cell, renderer));
        return String.format("%s/%s/%s/%s.png", m_pageIdSupplier.get(), RENDERED_CELL_IMAGES_PATH_PREFIX, tableId, key);
    }

    /**
     * Renders the image for the given relative image path and removes the respective renderer (and data value) from the
     * registry.
     *
     * @param imgPath the relative image path
     * @return the image data
     * @throws NoSuchElementException if there is no image available for the given path (anymore)
     */
    public byte[] renderAndRemove(final String imgPath) {
        var split = imgPath.split("\\?", 2);
        int width = DEFAULT_CELL_IMAGE_WIDTH;
        int height = DEFAULT_CELL_IMAGE_HEIGHT;
        if (split.length == 2) {
            var matcher = WIDTH_AND_HEIGHT_PATTERN.matcher(split[1]);
            if (matcher.matches()) {
                width = Integer.valueOf(matcher.group(1));
                height = Integer.valueOf(matcher.group(2));
            }
        }

        var tableIdAndKey = split[0].replace(".png", "").split("/");
        var tableId = tableIdAndKey[0];
        var key = tableIdAndKey[1];
        var valueAndRenderer = m_cellAndRendererMapsPerTableId.get(tableId).get(key);
        if (valueAndRenderer == null) {
            throw new NoSuchElementException("There is no image '" + split[0] + "' available (anymore)");
        }

        m_cellAndRendererMapsPerTableId.get(tableId).remove(key);
        return valueAndRenderer.getSecond().renderImage(valueAndRenderer.getFirst(), width, height);
    }

    /**
     * Removes all the renderers for the given table.
     *
     * @param tableId the id of the table to clear all stored cells and renderers for
     */
    public void clear(final String tableId) {
        if (m_cellAndRendererMapsPerTableId.containsKey(tableId)) {
            m_cellAndRendererMapsPerTableId.get(tableId).clear();
        }
    }

    /**
     * @param tableId
     * @return the number of renderers registered
     */
    public int numRegisteredRenderers(final String tableId) {
        if (m_cellAndRendererMapsPerTableId.containsKey(tableId)) {
            return m_cellAndRendererMapsPerTableId.get(tableId).size();
        } else {
            return 0;
        }

    }

}
