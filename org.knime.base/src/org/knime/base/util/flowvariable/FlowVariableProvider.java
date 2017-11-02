/*
 * ------------------------------------------------------------------------
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created on 12.03.2014 by Marcel Hanser
 */
package org.knime.base.util.flowvariable;

import java.util.NoSuchElementException;

/**
 * Provides access to flow variables. {@link org.knime.core.node.NodeModel} already implements the defined methods and
 * can therefore easily marked as {@link FlowVariableProvider}.
 *
 * <p>This interface may have additional methods in the future, which will then also be available in
 * {@link org.knime.core.node.NodeModel}. Do not implement this interface unless it's an extension of
 * <code>NodeModel</code>.
 *
 * @author Marcel Hanser, University of Konstanz
 * @since 2.10
 */
public interface FlowVariableProvider {

    /**
     * Get the value of the String variable with the given name leaving the flow variable stack unmodified.
     *
     * @param name Name of the variable
     * @return The value of the string variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct type is available.
     * @since 2.10
     */
    String peekFlowVariableString(final String name);

    /**
     * Get the value of the double variable with the given name leaving the variable stack unmodified.
     *
     * @param name Name of the variable
     * @return The assignment value of the variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct type is available.
     * @since 2.10
     */
    double peekFlowVariableDouble(final String name);

    /**
     * Get the value of the integer variable with the given name leaving the variable stack unmodified.
     *
     * @param name Name of the variable
     * @return The value of the integer variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct type is available.
     * @since 2.10
     */
    int peekFlowVariableInt(final String name);
}
