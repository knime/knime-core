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
 *   Dec 13, 2024 (hornm): created
 */
package org.knime.node;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.FluentNodeAPI;
import org.knime.core.node.port.PortObject;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.Page.RequireFromFileOrString;

/**
 * Fluent API to create a node view - not to be created directly but via the {@link DefaultNode}
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultView implements FluentNodeAPI {

    final Class<? extends DefaultNodeSettings> m_settingsClass;

    final String m_description;

    final Function<ViewInput, Page> m_pageFct;

    static RequireViewSettings create() {
        return settingsClass -> description -> pageFct -> new DefaultView(settingsClass, description,
            viewInput -> pageFct.apply(viewInput, Page.create()));
    }

    private DefaultView(final Class<? extends DefaultNodeSettings> settingsClass, final String description,
        final Function<ViewInput, Page> pageFct) {
        m_settingsClass = settingsClass;
        m_description = description;
        m_pageFct = pageFct;

    }

    /* REQUIRED PROPERTIES */

    public interface RequireViewSettings {
        RequireDescription settingsClass(Class<? extends DefaultNodeSettings> settingsClass);
    }

    public interface RequireDescription {
        RequirePage description(String description);
    }

    public interface RequirePage {
        DefaultView page(BiFunction<ViewInput, RequireFromFileOrString, Page> page);
    }

    /* OPTIONAL PROPERTIES */

    // TODO refactor build to refactor to follow new principles
    public DefaultView initialDataService(final InitialDataService initialDataService) {
        return this;
    }

    public <D> DefaultView initialData(final Function<ViewInput, D> initialDataSupplier) {
        return this;

    }

    // TODO refactor build to refactor to follow new principles
    DefaultView rpcDataService(final RpcDataService dataService) {

        return this;
    }

    public <S> DefaultView dataService(final Function<ViewInput, S> dataService) {

        return this;
    }

    public static interface ViewInput {

        <S extends DefaultNodeSettings> S getSettings();

        <D> D getInternalData();

        BufferedDataTable[] getInternalTables();

        PortObject[] getInternalPortObjects();

    }

}
