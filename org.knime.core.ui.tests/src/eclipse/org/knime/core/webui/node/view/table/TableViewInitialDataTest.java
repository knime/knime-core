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
 *   Sep 04, 2022 (Paul Bärnreuther): created
 */

package org.knime.core.webui.node.view.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.testing.node.view.TableTestUtil.createDefaultTestTable;
import static org.knime.testing.node.view.TableTestUtil.getDefaultTestSpec;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.webui.node.view.table.data.Renderer;
import org.knime.core.webui.node.view.table.data.TableViewDataServiceImpl;
import org.knime.core.webui.node.view.table.data.TableViewInitialDataImpl;
import org.knime.testing.node.view.TableTestUtil;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class TableViewInitialDataTest {

    private final int numRows = 10;

    private final String nodeId = "NodeID";

    private final BufferedDataTable table = TableTestUtil.createDefaultTestTable(numRows).get();

    private final String[] displayedColumns = new String[]{"double", "string", "date"};

    private MockedConstruction<TableViewDataServiceImpl> dataServiceMock;

    @BeforeEach
    public void beginTest() {
        dataServiceMock = Mockito.mockConstruction(TableViewDataServiceImpl.class);
    }

    @AfterEach
    public void endTest() {
        dataServiceMock.close();
    }

    @Test
    void testGetTableWithPagination() {
        final var settings = new TableViewViewSettings(table.getSpec());
        settings.m_displayedColumns = displayedColumns;
        settings.m_enablePagination = true;
        settings.m_pageSize = 8;
        final var initialData = TableViewUtil.createInitialData(settings, table, nodeId);
        initialData.getTable();
        verify(dataServiceMock.constructed().get(0)).getTable(aryEq(displayedColumns), eq(0L), eq(settings.m_pageSize),
            any(String[].class), eq(true));
    }

    @Test
    void testGetTableWithoutPagination() {
        final var settings = new TableViewViewSettings(table.getSpec());
        settings.m_displayedColumns = displayedColumns;
        settings.m_enablePagination = false;
        final var initialData = TableViewUtil.createInitialData(settings, table, nodeId);
        initialData.getTable();
        verify(dataServiceMock.constructed().get(0)).getTable(aryEq(displayedColumns), eq(0L), eq(0),
            any(String[].class), eq(true));
    }

    @Test
    void testGetSettings() {
        final var settings = new TableViewViewSettings();
        final var initialData = TableViewUtil.createInitialData(settings, table, nodeId);
        assertThat(initialData.getSettings()).isEqualTo(initialData.getSettings());
    }

    @Test
    void testInitialDataGetColumnCount() {
        final var res = new TableViewInitialDataImpl(new TableViewViewSettings(getDefaultTestSpec()),
            createDefaultTestTable(11), "tableId", null, null).getColumnCount();
        assertThat(res).isEqualTo(getDefaultTestSpec().getNumColumns());
    }

    @Test
    void testInitialDataGetDataTypes() {
        final var initData = TableViewUtil.createInitialData(new TableViewViewSettings(table.getSpec()), table, nodeId);
        var dataTypes = initData.getDataTypes();

        var stringType = dataTypes.get(String.valueOf(StringCell.TYPE.hashCode()));
        assertThat(stringType.getName()).isEqualTo("String");
        assertRendererNames(stringType.getRenderers(), "Multi-line String", "String");

        var doubleType = dataTypes.get(String.valueOf(DoubleCell.TYPE.hashCode()));
        assertThat(doubleType.getName()).isEqualTo("Number (double)");
        assertRendererNames(doubleType.getRenderers(), "Standard Double", "Percentage", "Full Precision", "Gray Scale",
            "Bars", "Standard Complex Number", "Default");

        var booleanType = dataTypes.get(String.valueOf(BooleanCell.TYPE.hashCode()));
        assertThat(booleanType.getName()).isEqualTo("Boolean value");
        assertRendererNames(booleanType.getRenderers(), "Default", "Default", "Standard Double", "Percentage",
            "Full Precision", "Gray Scale", "Bars", "Standard Complex Number", "Default");

        var imageType = dataTypes.get(String.valueOf(new PNGImageCellFactory().getDataType().hashCode()));
        assertThat(imageType.getName()).isEqualTo("PNG Image");
        assertRendererNames(imageType.getRenderers(), "PNG Image", "Default");
    }

    private static void assertRendererNames(final Renderer[] renderers, final String... expectedRendererNames) {
        assertThat(Arrays.stream(renderers).map(Renderer::getName).toArray(String[]::new))
            .isEqualTo(expectedRendererNames);
        assertThat(renderers[0].getId()).isNotNull();
    }

}
