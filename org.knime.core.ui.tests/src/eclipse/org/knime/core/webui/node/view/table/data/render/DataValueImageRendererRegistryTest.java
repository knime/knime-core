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
 *   Nov 11, 2022 (hornm): created
 */
package org.knime.core.webui.node.view.table.data.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.testing.node.view.TableTestUtil.createDefaultTestTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.view.table.data.TableViewDataServiceImpl;

/**
 * Tests for the {@link DataValueImageRendererRegistry}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class DataValueImageRendererRegistryTest {

    /**
     * Tests the interplay of the {@link TableViewDataServiceImpl} and the {@link DataValueImageRendererRegistry}.
     */
    @Test
    void testRenderImage() {
        var tableSupplier = createDefaultTestTable(15);

        var imgReg = new DataValueImageRendererRegistry(() -> "test_page_id");
        var dataService =
            new TableViewDataServiceImpl(tableSupplier, "test_table_id", new SwingBasedRendererFactory(), imgReg);
        var pathPrefix = "test_page_id/images/";

        // test pre-condition: make sure that all images in the table have unique ids
        var table = dataService.getTable(new String[]{"image"}, 0, 15, null, false, false);
        assertThat(Arrays.stream(table.getRows()).map(r -> r[1]).collect(Collectors.toSet())).hasSize(15);
        imgReg.clearImageDataCache("test_table_id");

        // access the same image multiple times (within the same chunk/page of rows)
        table = dataService.getTable(new String[]{"image"}, 0, 5, null, false, false);
        var imgPath = table.getRows()[3][1].replace(pathPrefix, "");
        var img = imgReg.renderImage(imgPath);
        assertThat(img).hasSizeGreaterThan(0);
        img = imgReg.renderImage(imgPath);
        assertThat(img).hasSizeGreaterThan(0);

        // request a new chunk of rows, but still access image resources from the previous chunk
        table = dataService.getTable(new String[]{"image"}, 5, 5, null, false, false);
        img = imgReg.renderImage(imgPath);
        assertThat(img).hasSizeGreaterThan(0);
        // request image from the new chunk
        imgPath = table.getRows()[3][1].replace(pathPrefix, "");
        img = imgReg.renderImage(imgPath);
        assertThat(img).hasSizeGreaterThan(0);

        // do the same again but with the image data cache cleared
        table = dataService.getTable(new String[]{"image"}, 10, 5, null, false, true);
        // request image from the previous chunk
        img = imgReg.renderImage(imgPath);
        assertThat(img).hasSize(0);
        imgPath = table.getRows()[3][1].replace(pathPrefix, "");
        // request image from the new chunk
        img = imgReg.renderImage(imgPath);
        assertThat(img).hasSizeGreaterThan(0);

    }

    /**
     * Makes sure that the image data cache keeps/clears the expected amount of image data.
     */
    @Test
    void testImageDataStats() {
        var tableSupplier = createDefaultTestTable(300);
        var imgReg = new DataValueImageRendererRegistry(() -> "test_page_id");
        var tableId = "test_table_id";
        var dataService = new TableViewDataServiceImpl(tableSupplier, tableId, new SwingBasedRendererFactory(), imgReg);
        var pathPrefix = "test_page_id/images/";

        var imgPaths = new HashSet<String>();

        var rendererIds = new String[]{"org.knime.core.data.renderer.DoubleBarRenderer$Factory", null};
        var columns = new String[]{"double", "image"};
        for (var i = 0; i <= 1; i++) {
            var table = dataService.getTable(columns, i * 100l, 100, rendererIds, false, false);
            Arrays.stream(table.getRows()).forEach(r -> imgPaths.add(r[1]));
            var stats = imgReg.getStatsPerTable(tableId);
            assertThat(stats.numImages()).isEqualTo((i + 1) * 100 * 2); // there are two images per row
            var batchSizes = new int[i + 1];
            Arrays.fill(batchSizes, 100 * 2);
            assertThat(stats.batchSizes()).isEqualTo(batchSizes);
            imgReg.renderImage(table.getRows()[0][1].replace(pathPrefix, ""));
            imgReg.renderImage(table.getRows()[50][1].replace(pathPrefix, ""));
            assertThat(stats.numRenderedImages()).isEqualTo(2 * (i + 1));
        }
        assertThat(imgReg.getStatsPerTable(tableId).batchSizes()).isEqualTo(new int[]{200, 200});

        // the image data cache has it's limit at 2 row batches -> if exceed, the oldest batch is removed
        var table = dataService.getTable(columns, 200, 80, rendererIds, false, false);
        Arrays.stream(table.getRows()).forEach(r -> imgPaths.add(r[1]));
        var stats = imgReg.getStatsPerTable(tableId);
        assertThat(stats.numImages()).isEqualTo(360); // there are two images per row
        var batchSizes = new int[]{160, 200};
        assertThat(stats.batchSizes()).isEqualTo(batchSizes);

        // assure that all img-paths in the image-column are unique (test pre-condition)
        assertThat(imgPaths).hasSize(280);

        // makes sure that the image data cache is cleared, when the respective parameter is passed to the data service
        dataService.getTable(new String[]{"image"}, 200, 10, null, false, true);
        stats = imgReg.getStatsPerTable(tableId);
        assertThat(stats.numImages()).isEqualTo(10);
        assertThat(stats.batchSizes()).isEqualTo(new int[]{10});

        // clear again directly
        imgReg.clearImageDataCache(tableId);
        imgReg.startNewBatchOfTableRows(tableId);
        stats = imgReg.getStatsPerTable(tableId);
        assertThat(stats.numImages()).isZero();
        assertThat(stats.batchSizes()).isEqualTo(new int[1]);
    }

    /**
     * Tests that images aren't removed from the cache in case they are part of two row-batches, a newer one and a an
     * older, to-be-removed, batch.
     */
    @Test
    public void testThatImagesRemainInTheCacheIfPartOfMultipleRowBatches() {
        var tableSupplier = createDefaultTestTable(600, idx -> idx % 100);
        var imgReg = new DataValueImageRendererRegistry(() -> "test_page_id");
        var tableId = "test_table_id";
        var dataService = new TableViewDataServiceImpl(tableSupplier, tableId, new SwingBasedRendererFactory(), imgReg);

        dataService.getTable(new String[]{"image"}, 0, 475, null, false, false);
        var stats = imgReg.getStatsPerTable(tableId);
        assertThat(stats.numImages()).isEqualTo(100);

        dataService.getTable(new String[]{"image"}, 475, 50, null, false, false);
        dataService.getTable(new String[]{"image"}, 525, 25, null, false, false);
        stats = imgReg.getStatsPerTable(tableId);
        assertThat(stats.numImages()).isEqualTo(75);

    }

}
