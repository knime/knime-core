/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 21, 2012 (wiswedel): created
 */
package org.knime.core.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Subclass of {@link NodeDialogPane} that requires the full input data to
 * define a configuration.
 *
 * <p>
 * Pending API! Not to be implemented (as of yet).
 *
 * @since 2.6
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class DataAwareNodeDialogPane extends NodeDialogPane {

    /** {@inheritDoc} */
    @Override
    void callDerivedLoadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs, final PortObject[] data)
            throws NotConfigurableException {
        loadSettingsFrom(settings, data);
    }

    /**
     * Throws error as this method is not called by framework.
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        throw new IllegalStateException("Method not to be called on derived "
                + "classes of "
                + DataAwareNodeDialogPane.class.getSimpleName());
    }

    /**
     * Throws error as this method is not called by framework.
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        throw new IllegalStateException("Method not to be called on "
                + "derived classes of "
                + DataAwareNodeDialogPane.class.getSimpleName());
    }

    /**
     * Replacement function for
     * {@link NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])}
     * which provides the implementation with the input data instead of just the
     * meta data.
     *
     * <p>
     * Overwrite this method if the input data of the node is all table based.
     * Otherwise overwrite
     * {@link #loadSettingsFrom(NodeSettingsRO, PortObject[])}.
     *
     * @param settings The settings as saved in the model, may be invalid but
     *            not null.
     * @param input The input data. The array is not null but may contain null
     *            values if the input is not executed (the framework will
     *            attempt to execute the workflow but that may fail due to not
     *            connected nodes or invalid configurations of the upstream
     *            nodes)
     * @throws NotConfigurableException If node is not configurable (as
     *          described in {@link NodeDialogPane})
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final BufferedDataTable[] input) throws NotConfigurableException {
        final String error =
                "Implementation of " + getClass().getSimpleName()
                + " does not overwrite the corresponding load "
                + "functions -- not configurable";
        getLogger().coding(error);
        throw new NotConfigurableException(error);
    }

    /**
     * Replacement function for
     * {@link NodeDialogPane#loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])}
     * which provides the implementation with the input data instead of just the
     * meta data.
     *
     * @param settings The settings as saved in the model, may be invalid but
     *            not null.
     * @param input The input data. The array is not null but may contain null
     *            values if the input is not executed (the framework will
     *            attempt to execute the workflow but that may fail due to not
     *            connected nodes or invalid configurations of the upstream
     *            nodes)
     * @throws NotConfigurableException If node is not configurable (as
     *          described in {@link NodeDialogPane})
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObject[] input) throws NotConfigurableException {
        // type cast all elements from argument array into BDT
        BufferedDataTable[] inData = new BufferedDataTable[input.length];
        for (int i = 0; i < inData.length; i++) {
            try {
                inData[i] = (BufferedDataTable)input[i];
            } catch (ClassCastException cce) {
                throw new NotConfigurableException("Input Port " + i
                        + " does not hold BufferedDataTable.\n"
                        + "Wrong version of loadSettingsFrom() overwritten!");
            }
        }
        loadSettingsFrom(settings, inData);
    }

}
