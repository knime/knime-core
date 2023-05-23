/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 */
package org.knime.core.data.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.renderer.DoubleBarRenderer;
import org.knime.core.data.renderer.DoubleGrayValueRenderer;
import org.knime.core.data.renderer.DoubleValueRenderer;
import org.knime.core.data.renderer.ImageValueRenderer;
import org.knime.core.node.CanceledExecutionException;

@SuppressWarnings("javadoc")
public class DataValueRendererUtilsTest {

    @Test
    void testGetNumberRenderer() throws CanceledExecutionException {
        DataType dataType = spy(DoubleCell.TYPE);
        when(dataType.getRendererFactories()).thenReturn(Collections.emptyList());
        assertThrows(IllegalStateException.class, () -> DataValueRendererUtils.getNumberRenderer(dataType),
            "Should fail if no valid renderer is found");
    }

    @Test
    void testGetNumberRendererFindfirstApplicable() throws CanceledExecutionException {
        DataType dataType = spy(DoubleCell.TYPE);
        when(dataType.getRendererFactories())
            .thenReturn(Lists.list(new DoubleBarRenderer.Factory(), new DoubleGrayValueRenderer.Factory(),
                new ImageValueRenderer.Factory(), new DoubleValueRenderer.PercentageRendererFactory()));
        assertThat(DataValueRendererUtils.getNumberRenderer(dataType).getClass()).as("First text based renderer")
            .isEqualTo(DoubleValueRenderer.class);
    }

    @Test
    void testFormatNumber() throws CanceledExecutionException {
        final var intRenderer = DataValueRendererUtils.getIntRenderer();
        assertThat(DataValueRendererUtils.formatNumber(intRenderer, null)).as("Int renderer").isNull();
        assertThat(DataValueRendererUtils.formatNumber(intRenderer, 5000)).as("Int renderer").isEqualTo("5000");

        final var doubleRenderer = DataValueRendererUtils.getDoubleRenderer();
        assertThat(DataValueRendererUtils.formatNumber(doubleRenderer, null)).as("Double renderer").isNull();
        assertThat(DataValueRendererUtils.formatNumber(doubleRenderer, 12.3456789)).as("Double renderer")
            .isEqualTo("12.346");
    }
}
