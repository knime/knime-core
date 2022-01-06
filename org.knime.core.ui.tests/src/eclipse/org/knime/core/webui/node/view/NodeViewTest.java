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
 *   Oct 16, 2021 (hornm): created
 */
package org.knime.core.webui.node.view;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.json.impl.JsonReExecuteDataServiceImpl;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.view.NodeViewNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests for {@link NodeView}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeViewTest {

    private WorkflowManager m_wfm;

    @SuppressWarnings("javadoc")
    @Before
    public void createEmptyWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    @SuppressWarnings("javadoc")
    @After
    public void disposeWorkflow() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
    }

    /**
     * Tests {@link NodeView#callTextInitialDataService()}, {@link NodeView#callTextDataService(String)} and
     * {@link NodeView#callTextAppyDataService(String)}
     */
    @Test
    public void testCallDataServices() {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var nodeView = createNodeView(page, new TextInitialDataService() {

            @Override
            public String getInitialData() {
                return "init service";
            }
        }, new TextDataService() {

            @Override
            public String handleRequest(final String request) {
                return "general data service";
            }
        }, new TextReExecuteDataService() {

            @Override
            public Optional<String> validateData(final String data) throws IOException {
                throw new UnsupportedOperationException("should not be called in this test");
            }

            @Override
            public void applyData(final String data) throws IOException {
                throw new UnsupportedOperationException("should not be called in this test");
            }

            @Override
            public void reExecute(final String data) throws IOException {
                throw new IOException("re-execute data service");

            }
        });
        NativeNodeContainer nc = NodeViewManagerTest.createNodeWithNodeView(m_wfm, m -> nodeView);

        var newNodeView = NodeViewManager.getInstance().getNodeView(nc);
        assertThat(newNodeView.callTextInitialDataService(), is("init service"));
        assertThat(newNodeView.callTextDataService(""), is("general data service"));
        String message = assertThrows(IOException.class, () -> newNodeView.callTextAppyDataService("ERROR,test")).getMessage();
        assertThat(message, is("re-execute data service"));
    }

    static NodeView createNodeView(final Page page, final NodeViewNodeModel m) {
        return createNodeView(page, null, null, new JsonReExecuteDataServiceImpl<String, NodeViewNodeModel>(m, String.class));
    }

    @SuppressWarnings("javadoc")
    public static NodeView createNodeView(final Page page) {
        return createNodeView(page, null, null, null);
    }

    @SuppressWarnings("javadoc")
    public static NodeView createNodeView(final Page page, final InitialDataService initDataService, final DataService dataService,
        final ApplyDataService applyDataService) {
        return new NodeView() { // NOSONAR

            @Override
            public Optional<InitialDataService> getInitialDataService() {
                return Optional.ofNullable(initDataService);
            }

            @Override
            public Optional<DataService> getDataService() {
                return Optional.ofNullable(dataService);
            }

            @Override
            public Optional<ApplyDataService> getApplyDataService() {
                return Optional.ofNullable(applyDataService);
            }

            @Override
            public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                //
            }

            @Override
            public void modelChanged() {
                //
            }

            @Override
            public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
                //
            }

            @Override
            public Page getPage() {
                return page;
            }
        };
    }

}
