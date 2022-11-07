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
 *   31 Oct 2022 (marcbux): created
 */
package org.knime.core.webui.node.view.table;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Supplier;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.webui.node.util.NodeCleanUpCallback;
import org.knime.core.webui.node.view.table.data.TableViewDataService;
import org.knime.core.webui.node.view.table.data.TableViewDataServiceImpl;
import org.knime.core.webui.node.view.table.data.TableViewInitialData;
import org.knime.core.webui.node.view.table.data.TableViewInitialDataImpl;
import org.knime.core.webui.node.view.table.data.render.DataValueImageRendererRegistry;
import org.knime.core.webui.node.view.table.data.render.SwingBasedRendererFactory;
import org.knime.core.webui.page.Page;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class TableViewUtil {

    // This is workaround/hack for the lack of proper random-access functionality for a (BufferedData)Table.
    // For more details see the class' javadoc.
    // It's static because it's registered and kept with the page which in turn is assumed to be static
    // (i.e. doesn't change between node instances and, hence, won't be re-created for each node instance).
    static final DataValueImageRendererRegistry RENDERER_REGISTRY =
        new DataValueImageRendererRegistry(createPageIdSupplier());

    private TableViewUtil() {
        // utility class
    }

    private static Supplier<String> createPageIdSupplier() {
        return TableViewUtil::getPageId;
    }

    /**
     * @return the table view's page id
     */
    public static String getPageId() {
        // Note on the 'static' page id: the entire TableView-page can be considered 'completely static'
        // because the page, represented by a vue component, is just a file (won't change at runtime)
        // And the image resources associated with a page of an individual table view instance are
        // served with a globally unique 'table id' in the path.
        // TODO should not be named after node factory; see followup ticket
        // TODO UIEXT-588
        return "view_org.knime.base.views.node.tableview.TableViewNodeFactory";
    }

    /**
     * @return the page representing the table view
     */
    public static Page createPage() {
        return Page.builder(TableViewUtil.class, "js-src/vue/dist", "TableView.umd.min.js")
            .addResources(createTableCellImageResourceSupplier(),
                DataValueImageRendererRegistry.RENDERED_CELL_IMAGES_PATH_PREFIX, true)
            .build();
    }

    private static Function<String, InputStream> createTableCellImageResourceSupplier() {
        return relativePath -> {
            var bytes = RENDERER_REGISTRY.renderAndRemove(relativePath);
            return new ByteArrayInputStream(bytes);
        };
    }

    /**
     * @param table the table to create the data service for
     * @param tableId a globally unique id to be able to uniquely identify the images belong to the table used here
     * @return a new table view data service instance
     */
    public static TableViewDataService createDataService(final BufferedDataTable table, final String tableId) {
        return createDataService(() -> table, tableId);
    }

    /**
     * @param tableSupplier the supplier for the table to create the data service for
     * @param tableId a globally unique id to be able to uniquely identify the images belong to the table used here
     * @return a new table view data service instance
     */
    public static TableViewDataService createDataService(final Supplier<BufferedDataTable> tableSupplier,
        final String tableId) {
        return new TableViewDataServiceImpl(tableSupplier, tableId, new SwingBasedRendererFactory(), RENDERER_REGISTRY);
    }

    /**
     * @param settings table view view settings
     * @param table the table to create the initial data for
     * @param tableId a globally unique id to be able to uniquely identify the images belonging to the table used here
     * @return the table view's initial data object
     */
    public static TableViewInitialData createInitialData(final TableViewViewSettings settings,
        final BufferedDataTable table, final String tableId) {
        return new TableViewInitialDataImpl(settings, () -> table, tableId, new SwingBasedRendererFactory(),
            RENDERER_REGISTRY);
    }

    /**
     * @param settings table view view settings
     * @param tableSupplier the supplier for the table to create the data service for
     * @param tableId a globally unique id to be able to uniquely identify the images belonging to the table used here
     * @return the table view's initial data object
     */
    public static TableViewInitialData createInitialData(final TableViewViewSettings settings,
        final Supplier<BufferedDataTable> tableSupplier, final String tableId) {
        return new TableViewInitialDataImpl(settings, tableSupplier, tableId, new SwingBasedRendererFactory(),
            RENDERER_REGISTRY);
    }

    /**
     * Method that is to be called when creating a {@link TableNodeView} and that registers the to-be-visualized table's
     * unique id with the global {@link DataValueImageRendererRegistry}.
     *
     * @param tableId a globally unique id to be able to uniquely identify the images belonging to the table used here
     */
    public static void registerRendererRegistryCleanup(final String tableId) {
        var nc = NodeContext.getContext().getNodeContainer();
        NodeCleanUpCallback.builder(nc, () -> RENDERER_REGISTRY.clear(tableId)) //
            .cleanUpOnNodeStateChange(true) //
            .deactivateOnNodeStateChange(false).build();
    }

    /**
     * Helper to return a proper table id from a node id.
     *
     * @param nodeID
     * @return a table id (which is globally unique, because the node id is)
     */
    public static String toTableId(final NodeID nodeID) {
        return nodeID.toString().replace(":", "_");
    }

}
