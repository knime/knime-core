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
 *   16.12.2014 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import org.knime.base.node.preproc.pmml.missingval.utils.MissingCellHandlerDescription;
import org.knime.base.node.preproc.pmml.missingval.utils.MissingCellHandlerDescriptionFactory;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;

/**
 * Factory class for a missing cell handler. Provides all the meta information and instances of the handler.
 * @author Alexander Fillbrunn
 */
public abstract class MissingCellHandlerFactory {

    /** The postfix for the factory name if the factory cannot produce valid PMML 4.2. **/
    public static final String NO_PMML_INDICATOR = "*";

    /**
     * @return the id of this factory.
     */
    public String getID() {
        return this.getClass().getCanonicalName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String s = getDisplayName();
        if (!producesPMML4_2()) {
            s += NO_PMML_INDICATOR;
        }
        return s;
    }

    /**
     * If true, the factory provides a custom settings panel.
     * @return should return true if the {@link #getSettingsPanel() getSettingsPanel} method returns not null.
     */
    public abstract boolean hasSettingsPanel();

    /**
     * Creates and returns the panel where the user can make changes to settings of the missing value handler.
     * @return the panel
     */
    public abstract MissingValueHandlerPanel getSettingsPanel();

    /**
     * @return the name of the factory.
     * May be appended automatically by an asterisk (*) if the factory does not support PMML 4.2.
     */
    public abstract String getDisplayName();

    /**
     * Creates a new instance of a missing cell handler managed by this factory.
     * @param column the column this handler is for.
     * @return an instance of the handler.
     */
    public abstract MissingCellHandler createHandler(final DataColumnSpec column);

    /**
     * Determines whether the missing cell handler created by this factory can be applied to cells of a data type.
     * @param type the data type to check
     * @return true, if the handler created by the factory can handle the column.
     */
    public abstract boolean isApplicable(final DataType type);

    /**
     * Determines whether the missing value handler managed by
     * this factory does operations that can be represented in PMML 4.2.
     * @return true, if the handler only does operations that can be described by a PMML 4.2 derived field.
     */
    public abstract boolean producesPMML4_2();

    /**
     * @return the description of the missing cell handler that is created by this factory.
     */
    public MissingCellHandlerDescription getDescription() {
        return MissingCellHandlerDescriptionFactory.getDescription(this);
    }
}
