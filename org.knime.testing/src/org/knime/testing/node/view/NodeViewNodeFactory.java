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
 *   Sep 13, 2021 (hornm): created
 */
package org.knime.testing.node.view;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.ReExecuteDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.NodeViewFactory;
import org.knime.core.webui.page.Page;

/**
 * Dummy node factory for tests around the {@link NodeView}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeViewNodeFactory extends NodeFactory<NodeViewNodeModel> implements NodeViewFactory<NodeViewNodeModel> {

    private final Function<NodeViewNodeModel, NodeView> m_nodeViewCreator;

    private final int m_numInputs;

    private final int m_numOutputs;

    private String m_initialData = "the initial data";

    /**
     *
     */
    public NodeViewNodeFactory() {
        this(0, 0);
    }

    /**
     * @param nodeViewCreator
     */
    public NodeViewNodeFactory(final Function<NodeViewNodeModel, NodeView> nodeViewCreator) {
        m_nodeViewCreator = nodeViewCreator;
        m_numInputs = 0;
        m_numOutputs = 0;
    }

    /**
     * @param numInputs
     * @param numOutputs
     */
    public NodeViewNodeFactory(final int numInputs, final int numOutputs) {
        m_numInputs = numInputs;
        m_numOutputs = numOutputs;
        m_nodeViewCreator = m -> { // NOSONAR
            return NodeView.builder(Page.builderFromString(() -> "blub", "index.html").build())//
                .initialDataService(new TextInitialDataService() {
                    @Override
                    public String getInitialData() {
                        return m_initialData;
                    }
                })//
                .reExecuteDataService(createReExecuteDataService())//
                .build();
        };
    }

    private ReExecuteDataService createReExecuteDataService() {
        return new TextReExecuteDataService() {

            @Override
            public Optional<String> validateData(final String data) throws IOException {
                if (data.startsWith("ERROR")) {
                    return Optional.of(data);
                } else {
                    return Optional.empty();
                }
            }

            @Override
            public void applyData(final String data) throws IOException {
                m_initialData = data;
            }

            @Override
            public void reExecute(final String data) throws IOException {
                //
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView createNodeView(final NodeViewNodeModel nodeModel) {
        assertThat("A node context is expected to be given", NodeContext.getContext().getNodeContainer(),
            is(notNullValue()));
        return m_nodeViewCreator.apply(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeViewNodeModel createNodeModel() {
        return new NodeViewNodeModel(m_numInputs, m_numOutputs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.knime.core.node.NodeView<NodeViewNodeModel> createNodeView(final int viewIndex,
        final NodeViewNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

}
