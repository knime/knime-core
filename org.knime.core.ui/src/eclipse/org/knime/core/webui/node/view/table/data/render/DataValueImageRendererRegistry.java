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

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NativeNodeContainer;
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
 * {@link #addRendererAndGetImgPath(String, DataCell, DataValueImageRenderer, long)} (while iterating the data table) and the
 * place where the image is finally rendered {@link #renderImage(String)} (the data value is accessed again to finally
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

    private static final Pattern WIDTH_AND_HEIGHT_PATTERN = Pattern.compile("w=(\\d+)&h=(\\d+)");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataValueImageRendererRegistry.class);

    private static final int MAX_NUM_ROW_BATCHES_IN_CACHE = 2;

    private final Supplier<String> m_pageIdSupplier;

    private final Map<String, Images> m_imagesPerTable = Collections.synchronizedMap(new HashMap<>());


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
     * @param rowIndex the index of the row the image to render is part of
     *
     * @return the relative path where the image can be accessed
     */
    public String addRendererAndGetImgPath(final String tableId, final DataCell cell,
        final DataValueImageRenderer renderer, final long rowIndex) {
        var images = m_imagesPerTable.get(tableId);
        if (images == null) {
            throw new IllegalStateException("'startNewBatchOfTableRows' needs to be called at least once before");
        }
        var key = images.addImage(cell, renderer, rowIndex);
        return String.format("%s/%s/%s/%s.png", m_pageIdSupplier.get(), RENDERED_CELL_IMAGES_PATH_PREFIX, tableId, key);
    }

    /**
     * Renders the image for the given relative image path and removes the respective renderer (and data value) from the
     * registry.
     *
     * @param imgPath the relative image path
     * @return the image data or an empty array if the image data can't be accessed (anymore)
     */
    public byte[] renderImage(final String imgPath) {
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
        var images = m_imagesPerTable.get(tableId);
        if (images == null) {
            LOGGER.debugWithFormat("There is no image data available anymore for table '%s'.", tableId);
            return new byte[0];
        }
        var image = images.getImage(key);
        if (image == null) {
            LOGGER.debugWithFormat("There is no image '%s' available (anymore)", split[0]);
            return new byte[0];
        }
        return image.getData(width, height);
    }

    /**
     * Signals that a new batch of table row is being requested. By that, this registry knows to what batch of rows
     * certain images belong which latter helps to partially clear the cache (e.g. only removing images from the oldest
     * batch).
     *
     * @param tableId the table to strat the new batch for
     */
    public void startNewBatchOfTableRows(final String tableId) {
        m_imagesPerTable.computeIfAbsent(tableId, id -> new Images()).startNewBatch();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugWithFormat("New batch of to-be-rendered images started for table with id '%s'.", tableId);
            logStatisticsMessages(tableId);
        }
    }

    private void logStatisticsMessages(final String tableId) {
        var images = m_imagesPerTable.get(tableId);
        if (images != null) {
            var stats = images.getStats();
            var batchSizes = stats.batchSizes();
            var numImages = stats.numImages();
            var numRenderedImages = stats.numRenderedImages();
            if (numImages == 0) {
                return;
            }
            LOGGER.debugWithFormat("  The registry for table '%s' currently contains:", tableId);
            LOGGER.debugWithFormat("  %d batches of size: %s, (sum: %d)", batchSizes.length,
                Arrays.toString(batchSizes), Arrays.stream(batchSizes).sum());
            LOGGER.debugWithFormat("  %d images in total; %d rendered, %d un-rendered", numImages, numRenderedImages,
                (numImages - numRenderedImages));
        }
    }

    /**
     * Removes all cached resources for the given table.
     *
     * @param tableId the id of the table to clear all stored cells and renderers for
     */
    public void clearImageDataCache(final String tableId) {
        if (m_imagesPerTable.remove(tableId) != null && LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(
                "Cached image data cleared for table with id '%s'. There is still image data cached for %d tables",
                tableId, m_imagesPerTable.size()));
        }
    }

    /**
     * @param tableId
     * @return the number of renderers registered
     */
    public int numRegisteredRenderers(final String tableId) {
        if (m_imagesPerTable.containsKey(tableId)) {
            return m_imagesPerTable.get(tableId).getStats().numImages();
        } else {
            return 0;
        }

    }

    StatsPerTable getStatsPerTable(final String tableId) {
        return m_imagesPerTable.get(tableId).getStats();
    }

    interface StatsPerTable {

        int numImages();

        int numRenderedImages();

        int[] batchSizes();

    }

    private static class Images {

        private Map<String, Image> m_images = new HashMap<>();

        private Deque<Set<String>> m_batches = new LinkedList<>();

        private int m_hashCollisionCount = 0;

        private StatsPerTable m_stats;

        String addImage(final DataCell cell, final DataValueImageRenderer renderer, final long rowIndex) {
            var key = Integer.toString(31 * cell.hashCode() + renderer.getId().hashCode());
            if (m_images.containsKey(key)) {
                var existingCell = m_images.get(key).getDataCell();
                if (!cell.equals(existingCell)) {
                    // hash collision
                    key += "_" + m_hashCollisionCount;
                    m_hashCollisionCount++; // NOSONAR
                    m_images.put(key, new Image(cell, renderer));
                }
            } else {
                m_images.put(key, new Image(cell, renderer));
            }
            m_batches.getFirst().add(key);
            return key;
        }

        Image getImage(final String imageId) {
            return m_images.get(imageId);
        }

        void startNewBatch() {
            if (!m_batches.isEmpty() && m_batches.getFirst().isEmpty()) {
                return;
            }
            while (m_batches.size() >= MAX_NUM_ROW_BATCHES_IN_CACHE) {
                var removedBatch = m_batches.removeLast();
                // only remove the images which are NOT part of the existing batches
                var imagesToKeep = m_batches.stream().flatMap(Set::stream).collect(Collectors.toSet());
                removedBatch.forEach(id -> {
                    if (!imagesToKeep.contains(id)) {
                        m_images.remove(id);
                    }
                });
            }
            m_batches.addFirst(new HashSet<>());
        }

        StatsPerTable getStats() {
            if (m_stats == null) {
                m_stats = new StatsPerTable() { // NOSONAR

                    @Override
                    public int numImages() {
                        return m_images.size();
                    }

                    @Override
                    public int numRenderedImages() {
                        return (int)m_images.values().stream().filter(Image::isRendered).count();
                    }

                    @Override
                    public int[] batchSizes() {
                        return m_batches.stream().mapToInt(Set::size).toArray();
                    }

                };
            }
            return m_stats;
        }

    }

    private static class Image {

        private DataCell m_cell;

        private DataValueImageRenderer m_renderer;

        private byte[] m_data;

        Image(final DataCell cell, final DataValueImageRenderer renderer) {
            m_cell = cell;
            m_renderer = renderer;
        }

        byte[] getData(final int width, final int height) {
            if (m_data == null) {
                m_data = m_renderer.renderImage(m_cell, width, height);
                m_renderer = null;
                m_cell = null;
            }
            return m_data;
        }

        DataCell getDataCell() {
            return m_cell;
        }

        boolean isRendered() {
            return m_data != null;
        }

    }

}
