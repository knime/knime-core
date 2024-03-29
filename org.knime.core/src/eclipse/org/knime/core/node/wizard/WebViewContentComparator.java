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
 *   Jun 15, 2021 (ben.laney): created
 */
package org.knime.core.node.wizard;

import org.knime.core.node.web.WebViewContent;

/**
 * Interface which should be implemented by value representations which pass between the application and a user client
 * that require a more specific comparison mechanism than the default "equals" method. This is useful when comparing
 * changes from values/settings in the client and defaults or saved values/settings. By implementing this interface,
 * representations may add additional comparison logic or edge-case handling without polluting the "equals" prototype.
 *
 * @author ben.laney
 * @param <V>
 * @since 4.4
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface WebViewContentComparator<V extends WebViewContent> {

    /**
     * A method which compares values provided by a wizard client with defaults or saved values. The functionality here
     * should be targeted at cases where a simple "equals" check would return a false determination and additional logic
     * is required to check if settings in the value have been changed.
     *
     * @param other the comparison object.
     * @return if the values are effectively equal in the execution context.
     */
    public boolean compareViewValues(final V other);
}
