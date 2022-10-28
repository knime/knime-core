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
 *   Oct 26, 2022 (hornm): created
 */
package org.knime.core.ui.workflowcoach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.NodeInfo;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.ui.node.workflow.NativeNodeContainerUI;
import org.knime.core.ui.workflowcoach.NodeRecommendationManager.IUpdateListener;
import org.knime.core.ui.workflowcoach.NodeRecommendationManager.NodeRecommendation;
import org.knime.core.ui.workflowcoach.data.NodeTripleProvider;
import org.knime.core.ui.workflowcoach.data.NodeTripleProviderFactory;
import org.knime.core.ui.wrapper.NativeNodeContainerWrapper;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests {@link NodeRecommendationManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Kai Franze, KNIME GmbH
 */
public class NodeRecommendationManagerTest {

    private IUpdateListener m_updateListener;

    private final Predicate<NodeInfo> m_isSourceNode =
        ni -> ni.getFactory().equals("org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory");

    private final Predicate<NodeInfo> m_existsInRepository =
        ni -> ni.getFactory().startsWith("test_") || m_isSourceNode.test(ni);

    /**
     * Setup recommendation manager
     */
    @Before
    public void setup() {
        NodeRecommendationManager.getInstance().initialize(m_isSourceNode, m_existsInRepository);
        m_updateListener = mock(IUpdateListener.class);
        NodeRecommendationManager.getInstance().addUpdateListener(m_updateListener);
    }

    /**
     * Tear down recommandation manager
     */
    @After
    public void finish() {
        NodeRecommendationManager.getInstance().removeUpdateListener(m_updateListener);
        m_updateListener = null;
    }

    /**
     * Test {@link NodeRecommendationManager#getNodeRecommendationFor(NativeNodeContainerUI...)} using a node present in
     * the workflow
     *
     * @throws IOException
     */
    @Test
    public void testGetNodeRecommendationForNativeNodeContainer() throws IOException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm, new PortObjectInNodeFactory());
        var recommendations = getAndAssertNodeRecommendations(nnc);

        assertThat("Expected exactly 2 recommendations", recommendations.size(), equalTo(2));
        assertThat("Response is not a list", recommendations, instanceOf(List.class));
        recommendations.forEach(nr -> {
            assertThat("Item is not a node recommendation", nr, instanceOf(NodeRecommendation.class));
            assertThat("Node recommendation is not a <Test Row Filter> or <Test Row Splitter>", nr.getNodeName(),
                anyOf(equalTo("Test Row Filter"), equalTo("Test Row Splitter")));
        });
    }

    /**
     * Test {@link NodeRecommendationManager#getNodeRecommendationFor(NativeNodeContainerUI...)} without a node
     * selected. It also tests the update listener.
     *
     * @throws IOException
     */
    @Test
    public void testGetNodeRecommendationForNoneAndUpdateListener() throws IOException {
        var recommendations = getAndAssertNodeRecommendations(null);

        assertThat("Expected exactly 1 recommendation", recommendations.size(), equalTo(1));
        assertThat("Response is not a list", recommendations, instanceOf(List.class));
        recommendations.forEach(nr -> {
            assertThat("Item is not a node recommendation", nr, instanceOf(NodeRecommendation.class));
            assertThat("Node recommendation is not a <PortObject Reference Reader>", nr.getNodeName(),
                equalTo("PortObject Reference Reader"));
        });

        NodeRecommendationManager.getInstance().loadRecommendations();
        verify(m_updateListener, times(1)).updated();
    }

    /**
     * @param nnc The native node container to get recommendations for
     * @return The list of node recommendations
     */
    private List<NodeRecommendation> getAndAssertNodeRecommendations(final NativeNodeContainer nnc) {
        var recommendations = nnc == null ? NodeRecommendationManager.getInstance().getNodeRecommendationFor()
            : NodeRecommendationManager.getInstance().getNodeRecommendationFor(NativeNodeContainerWrapper.wrap(nnc));

        // Checks `getNodeRecommendationFor()` result (maybe add type check)
        assertThat("Expected a non-empty array", recommendations, not(emptyArray()));

        var recommendationsWithoutDups =
            NodeRecommendationManager.joinRecommendationsWithoutDuplications(recommendations);

        // Checks `joinRecommendationsWithoutDuplications()` result (maybe add type check)
        assertThat("Expected a list", recommendationsWithoutDups, instanceOf(List.class));

        // Checks update listener, no invocation since listener was registered
        verify(m_updateListener, times(0)).updated();

        return recommendationsWithoutDups.stream().map(ObjectUtils::firstNonNull).collect(Collectors.toList());
    }

    /**
     * This tests the following methods:
     * - {@link NodeRecommendationManager#initialize(Predicate, Predicate)}
     * - {@link NodeRecommendationManager#loadRecommendations()}
     * - {@link NodeRecommendationManager#getNumLoadedProviders()}
     * - {@link NodeRecommendationManager#getNodeTripleProviders()}
     * - {@link NodeRecommendationManager#getNodeTripleProviderFactories()}
     *
     * @throws IOException
     */
    @Test
    public void testRemainingMethods() throws IOException {
        // Cannot initialize again
        assertTrue("This should be true since recommendations were loaded before",
            NodeRecommendationManager.getInstance().initialize(ni -> true, ni -> false));
        verify(m_updateListener, times(0)).updated();

        // Will not load recommendations with incomplete predicates
        assertFalse("This initialization should return false",
            NodeRecommendationManager.getInstance().initialize(null, ni -> false));
        assertFalse("This initialization should return false",
            NodeRecommendationManager.getInstance().initialize(ni -> true, null));
        assertFalse("This initialization should return false",
            NodeRecommendationManager.getInstance().initialize(null, null));
        verify(m_updateListener, times(0)).updated();

        // Reload the node recommendations two times
        NodeRecommendationManager.getInstance().loadRecommendations();
        NodeRecommendationManager.getInstance().loadRecommendations();
        verify(m_updateListener, times(2)).updated();

        // Check number of loaded triple providers
        var numLoadedProviders = NodeRecommendationManager.getNumLoadedProviders();
        assertThat("Expected at least one node tripe provider loaded", numLoadedProviders, greaterThanOrEqualTo(1));

        // Check triple providers
        var tripleProviders = NodeRecommendationManager.getNodeTripleProviders();
        tripleProviders
            .forEach(tp -> assertThat("This is not a triple provider", tp, instanceOf(NodeTripleProvider.class)));

        // Check triple provider factories
        var tripleProviderFactory = NodeRecommendationManager.getNodeTripleProviderFactories();
        tripleProviderFactory.forEach(
            tpf -> assertThat("This is not a triple provider", tpf, instanceOf(NodeTripleProviderFactory.class)));
    }

}
