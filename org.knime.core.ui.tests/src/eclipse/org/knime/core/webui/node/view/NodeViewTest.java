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

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.json.impl.JsonReExecuteDataServiceImpl;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.view.NodeViewNodeModel;

/**
 * Helper methods and tests for {@link NodeView NodeViews}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class NodeViewTest {

    private NodeViewTest() {
        // at the moment it's just a utility class
    }

    static NodeView createNodeView(final Page page, final NodeViewNodeModel m) {
        return createNodeView(page, null, null,
            new JsonReExecuteDataServiceImpl<String, NodeViewNodeModel>(m, String.class));
    }

    @SuppressWarnings("javadoc")
    public static NodeView createNodeView(final Page page) {
        return createNodeView(page, null, null, null);
    }

    @SuppressWarnings("javadoc")
    public static NodeView createNodeView(final Page page, final InitialDataService initDataService,
        final DataService dataService, final ApplyDataService applyDataService) {
        return new NodeView() { // NOSONAR

            @Override
            public Optional<InitialDataService> createInitialDataService() {
                return Optional.ofNullable(initDataService);
            }

            @Override
            public Optional<DataService> createDataService() {
                return Optional.ofNullable(dataService);
            }

            @Override
            public Optional<ApplyDataService> createApplyDataService() {
                return Optional.ofNullable(applyDataService);
            }

            @Override
            public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
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
