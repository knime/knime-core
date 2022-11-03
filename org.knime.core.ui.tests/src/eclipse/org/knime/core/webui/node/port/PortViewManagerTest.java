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
 *   Jul 21, 2022 (hornm): created
 */
package org.knime.core.webui.node.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Optional;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.node.NodePortWrapper;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.util.WorkflowManagerUtil;
import org.mockito.Mockito;

/**
 * Tests {@link PortViewManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class PortViewManagerTest {

    /**
     * Tests {@link PortViewManager#getPortView(NodePortWrapper)} and the proper caching and clean-up of the
     * {@link PortView}-instances.
     *
     * @throws IOException
     *
     */
    @Test
    public void testGetPortView() throws IOException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(0, 1));
        wfm.executeAllAndWaitUntilDone();

        var portViewFactory0 = mock(PortViewFactory.class);
        Mockito.when(portViewFactory0.createPortView(nnc.getOutPort(0).getPortObject())).thenReturn(createPortView());

        var portViewFactory1 = mock(PortViewFactory.class);
        Mockito.when(portViewFactory1.createPortView(nnc.getOutPort(1).getPortObject())).thenReturn(createPortView());

        var portViewManager = PortViewManager.getInstance();
        PortViewManager.registerPortViewFactory(FlowVariablePortObject.TYPE, portViewFactory0);
        PortViewManager.registerPortViewFactory(BufferedDataTable.TYPE, portViewFactory1);
        assertThat(portViewManager.getBaseUrl().orElse(null)).isEqualTo("http://org.knime.core.ui.port/");
        assertThat(portViewManager.getPagePath(NodePortWrapper.of(nnc, 0))).isEqualTo("blub/page.html");

        var portView = portViewManager.getPortView(NodePortWrapper.of(nnc, 0));
        assertThat(portView).isNotNull();
        var portView2 = portViewManager.getPortView(NodePortWrapper.of(nnc, 0));
        assertThat(portView == portView2).isTrue();

        portView = portViewManager.getPortView(NodePortWrapper.of(nnc, 1));
        assertThat(portView).isNotNull();

        // check that the port view cache is cleared on node reset
        assertThat(portViewManager.getPortViewMapSize()).isEqualTo(2);
        wfm.resetAndConfigureAll();
        assertThat(portViewManager.getPortViewMapSize()).isEqualTo(0);

        // check that the port view cache is cleared when the node is being removed
        portViewManager.getPortView(NodePortWrapper.of(nnc, 1));
        assertThat(portViewManager.getPortViewMapSize()).isEqualTo(1);
        wfm.removeNode(nnc.getID());
        Awaitility.await().untilAsserted(() -> assertThat(portViewManager.getPortViewMapSize()).isEqualTo(0));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    private static PortView createPortView() {
        return new PortView() {

            @Override
            public Optional<InitialDataService> createInitialDataService() {
                return Optional.empty();
            }

            @Override
            public Optional<DataService> createDataService() {
                return Optional.empty();
            }

            @Override
            public Page getPage() {
                return Page.builder(() -> "", "page.html").build();
            }

            @Override
            public String getPageId() {
                return "blub";
            }

        };
    }
}
