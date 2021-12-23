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
 *   Nov 29, 2021 (hornm): created
 */
package org.knime.gateway.impl.service.util;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.knime.core.data.RowKey;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.NodeViewTest;
import org.knime.core.webui.page.Page;
import org.knime.gateway.api.entity.NodeViewEnt;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.node.view.NodeViewNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;
import org.mockito.Mockito;

/**
 * Tests {@link HiLiteListenerRegistry} in conjuction with {@link NodeViewEnt}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class HiLiteListenerRegistryTest {

    /**
     * @throws IOException
     */
    @Test
    public void testHiLiteListenerRegistry() throws IOException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();

        Function<NodeViewNodeModel, NodeView> nodeViewCreator =
            m -> NodeViewTest.createNodeView(Page.builderFromString(() -> "blub", "index.html").build());
        NativeNodeContainer nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
        var hiLiteHandler = nnc.getNodeModel().getInHiLiteHandler(0);
        hiLiteHandler.fireHiLiteEvent(new RowKey("k1"), new RowKey("k2"));

        var hiLiteListenerRegistry = new HiLiteListenerRegistry();

        var message =
            assertThrows(IllegalStateException.class, () -> new NodeViewEnt(nnc, hiLiteListenerRegistry)).getMessage();
        assertThat(message, Matchers.containsString("Can't activate the hilite-listener. None registered for"));

        var hiLiteListenerMock = Mockito.mock(HiLiteListener.class);
        hiLiteListenerRegistry.registerHiLiteListener(nnc.getID(), hiLiteListenerMock);

        hiLiteHandler.fireHiLiteEvent(new RowKey("k3"));
        verify(hiLiteListenerMock, never()).hiLite(any());

        var nodeViewEnt = new NodeViewEnt(nnc, hiLiteListenerRegistry);
        assertThat(nodeViewEnt.getInitialSelection(), is(List.of("k1", "k2", "k3")));

        hiLiteHandler.fireHiLiteEvent(new RowKey("k4"));
        await().pollDelay(ONE_HUNDRED_MILLISECONDS).timeout(FIVE_SECONDS)
            .untilAsserted(() -> verify(hiLiteListenerMock).hiLite(any()));

        hiLiteListenerRegistry.unregisterHiLiteListener(nnc.getID());
        hiLiteHandler.fireHiLiteEvent(new RowKey("k5"));
        verify(hiLiteListenerMock).hiLite(any());

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

}
