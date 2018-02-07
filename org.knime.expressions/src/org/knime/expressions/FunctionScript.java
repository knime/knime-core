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
 * -------------------------------------------------------------------
 *
 */
package org.knime.expressions;

import java.util.Optional;
import java.util.function.Function;

/**
 * Interface for a {@link Function} that holds a compiled script, which can be
 * executed.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public interface FunctionScript extends FunctionalScript<Object[], Object>, Script {

	/** Method name that is used to invoke the method. */
	final static String METHOD_NAME = "knime_GroovyClass_mainStart";

	/**
	 * @return The length of the input array needed by the script.
	 */
	int getNrArgs();

	/**
	 * Returns the Java type of the input array with the given index.
	 * 
	 * @param idx
	 *            index of the input field.
	 * @return Java type of the specific input field.
	 */
	Optional<Class<?>> type(int idx);

	/**
	 * Returns the index of the input for the specified column/flow variable.
	 * 
	 * @param originalName
	 *            original name of the column/flow variable.
	 * @return Index for the input array at which this the provided column/flow
	 *         variable is expected. Returns -1, if the column/flow variable is not
	 *         used.
	 */
	int argIdxOf(String originalName);

	/**
	 * Returns the original index for the specified column/flow variable.
	 * 
	 * @param originalName
	 *            original name of the column/flow variable.
	 * @return Original index of the variable. Returns -1, if the column/flow
	 *         variable is not used.
	 */
	int originalIdxOf(String originalName);

	/**
	 * @return The method name that is used to invoke the execution of the script.
	 */
	default public String getMethodName() {
		return METHOD_NAME;
	}
	
	/**
	 * @return The original names of the used columns.
	 */
	String[] getColumnNames();
}
