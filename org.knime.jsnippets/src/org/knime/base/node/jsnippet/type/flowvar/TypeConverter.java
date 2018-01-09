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
 * ------------------------------------------------------------------------
 *
 * History
 *   24.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.type.flowvar;

import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Type converter for FlowVariables.
 * @author Heiko Hofer
 */
public interface TypeConverter {
    /**
     * Get the java types this flow variable type can be converted to.
     * @return the possible java types
     */
    @SuppressWarnings("rawtypes")
    Class[] canProvideJavaTypes();


    /**
     * Test if this flow variable type can be converted to the given java type.
     * @param javaType the java type to test for
     * @return true when the flow variable type can provide the java type
     */
    @SuppressWarnings("rawtypes")
    boolean canProvideJavaType(Class javaType);

    /**
     * The preferred java type (One of canProvideJavaTypes()).
     * @return the preferred java type
     */
    @SuppressWarnings("rawtypes")
    Class getPreferredJavaType();

    /**
     * Get the list of java types where a flow variable of this type can be
     * created from.
     * @return the list of possible java types for flow variable creation
     */
    @SuppressWarnings("rawtypes")
    Class[] canCreatedFromJavaTypes();


    /**
     * Get the flow variable type.
     * @return the flow variable type
     */
    Type getType();


    /**
     * Get a java object of the given class that holds the value of the
     * given flow variable. The returned object can safely be casted to the
     * given class
     * @param flowVar the flow variable
     * @param className the class
     * @return the value of the flow variable
     */
    @SuppressWarnings("rawtypes")
    Object getValue(FlowVariable flowVar, Class className);
}
