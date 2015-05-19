/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * History
 *   15.05.2015 (thor): created
 */
package org.knime.core.node.dialog;

import org.knime.core.node.InvalidSettingsException;

/**
 * Interface for nodes that can be controlled by input from external sources, e.g. web services.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.12
 */
public interface InputNode {
    /**
     * Returns a template for the expected input data. Clients may use the templates, replace values, and set the via
     * {@link #setInputData(ExternalNodeData)}. If the node reads larger amount from an external resource you must
     * specifiy a URL in the data object (e.g. the default location from the dialog).
     *
     * @return a template for the input data
     */
    ExternalNodeData getInputData();

    /**
     * Validates the input for the node. Implementation should only check if the data is applicable but not load it
     * into the node. If the data is not applicable, an {@link InvalidSettingsException} must be thrown and callers
     * should avoid calling {@link #setInputData(ExternalNodeData)}.
     *
     * @param inputData an external node data object
     * @throws InvalidSettingsException if the data is invalid
     */
    void validateInputData(ExternalNodeData inputData) throws InvalidSettingsException;


    /**
     * Sets the input for the node. Implementations must make sure that the nodes internal configuration is updated
     * accordingly so that the next execution uses the provided input data. If the data is not applicable (which should
     * not happen if {@link #validateInputData(ExternalNodeData)} has been called before) then an
     * {@link InvalidSettingsException} should be thrown.
     *
     * @param inputData an external node data object
     * @throws InvalidSettingsException if the data is invalid
     */
    void setInputData(ExternalNodeData inputData) throws InvalidSettingsException;
}
