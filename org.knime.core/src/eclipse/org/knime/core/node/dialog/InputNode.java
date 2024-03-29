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
 *   15.05.2015 (thor): created
 */
package org.knime.core.node.dialog;

import org.knime.core.node.InvalidSettingsException;

/**
 * Interface for nodes that can be controlled by input from external sources, e.g. web services.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.12
 */
public interface InputNode {
    /**
     * Returns a template for the expected input data. Clients may use the templates, replace values, and set the via
     * {@link #setInputData(ExternalNodeData)}. If the node reads larger amount from an external resource you must
     * specify a URL in the data object (e.g. the default location from the dialog).
     *
     * @return a template for the input data
     */
    ExternalNodeData getInputData();

    /**
     * Validates the input for the node. Implementation should only check if the data is applicable but not load it into
     * the node. If the data is not applicable, an {@link InvalidSettingsException} must be thrown and callers should
     * avoid calling {@link #setInputData(ExternalNodeData)}. <br />
     * This method may only be called with a {@code null} value if {@link #isInputDataRequired()} returns {@code false}.
     * In that case, input data that is {@code null} should be treated as valid, see
     * {@link #setInputData(ExternalNodeData)}.
     *
     * @param inputData an external node data object
     * @throws InvalidSettingsException if the data is invalid
     */
    void validateInputData(ExternalNodeData inputData) throws InvalidSettingsException;

    /**
     * Sets the input for the node. Implementations must make sure that the nodes internal configuration is updated
     * accordingly so that the next execution uses the provided input data. If the data is not applicable (which should
     * not happen if {@link #validateInputData(ExternalNodeData)} has been called before) then an
     * {@link InvalidSettingsException} should be thrown. <br />
     * This method may only be called with a {@code null} value if {@link #isInputDataRequired()} returns {@code false}.
     * In the case of input data being {@code null}, the node should clear any previous set data so that the next
     * execution does not use external input data.
     *
     * @param inputData an external node data object
     * @throws InvalidSettingsException if the data is invalid
     */
    void setInputData(ExternalNodeData inputData) throws InvalidSettingsException;

    /**
     * Allows nodes to specify if they require external input data. If returned {@code false}, the node does not require
     * input data and {@link #validateInputData(ExternalNodeData)} and {@link #setInputData(ExternalNodeData)} can be
     * called with {@code null} values. If returned {@code true}, the node requires input data and
     * {@link #validateInputData(ExternalNodeData)} and {@link #setInputData(ExternalNodeData)} must not be called with
     * {@code null} values.<br />
     * This came into existence as part of AP-17400. This default implementation returns {@code true} in order to
     * guarantee backward compatibility.
     *
     * @return {@code true} here but potentially overwritten by nodes
     * @since 4.5
     */
    default boolean isInputDataRequired() {
        return true;
    }

    /**
     * Allows nodes to veto the use of fully qualified parameter names. That is, if returned <code>false</code> then the
     * node suggests to use the short name ("input-table") over its long name ("input-table-14") in an API description
     * (e.g. Swagger). However, even if <code>false</code> is returned the framework will still use the long variant
     * when conflicting parameter names are used. <br />
     * This came into existence as part of AP-14686. This default implementation returns <code>true</code> in order to
     * guarantee backward compatibility.
     *
     * @return <code>true</code> here but potentially overwritten by nodes (especially Container nodes)
     * @since 4.3
     */
    default boolean isUseAlwaysFullyQualifiedParameterName() {
        return true;
    }

}