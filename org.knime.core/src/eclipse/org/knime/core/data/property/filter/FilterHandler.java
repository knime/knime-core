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
 *   Oct 24, 2016 (wiswedel): created
 */
package org.knime.core.data.property.filter;

import org.knime.core.data.DataCell;
import org.knime.core.data.property.PropertyHandler;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;

/**
 * A handler that is attached to a {@linkplain org.knime.core.data.DataColumnSpec column} and represents (read-only)
 * visual filtering. Changing a filtering permanently requires re-execution of the node that defines the filtering.
 * In the KNIME Webportal, however, the filtering is interactive and is derived from the filter handler defined on a
 * table/column and is derived from the contained model's {@link FilterModel#getFilterUUID() ID}.
 *
 * @author Bernd Wiswedel, KNIME.com
 * @since 3.3
 */
public final class FilterHandler implements PropertyHandler {

    private final FilterModel m_filterModel;

    /** Constructs new object based on a non-null model.
     * @param filterModel The model to wrap
     */
    private FilterHandler(final FilterModel filterModel) {
        m_filterModel = CheckUtils.checkArgumentNotNull(filterModel);
    }

    /** For a given element in the column return whether that row having that cell is filtering out or not.
     * @param cell cell in question, might be {@linkplain DataCell#isMissing() missing} but must not be null.
     * @return Whether that row containing that cell is filtered out (return value is <code>true</code>) or not.
     * @see FilterModel#isInFilter(DataCell)
     */
    public boolean isInFilter(final DataCell cell) {
        return m_filterModel.isInFilter(cell);
    }

    /** Saves this filter to a config object; used during workflow save or when the table is persisted to disc.
     * @param config to write to (must not be null) */
    public void save(final ConfigWO config) {
        m_filterModel.save(config);
    }

    /** @return the underlying model. */
    public FilterModel getModel() {
        return m_filterModel;
    }

    /** Counterpart method for {@link #save(ConfigWO)}.
     * @param config To load from (not null)
     * @return The restored handler (not null).
     * @throws InvalidSettingsException If that fails for whatever reason.
     */
    public static FilterHandler load(final ConfigRO config) throws InvalidSettingsException {
        return new FilterHandler(FilterModel.load(config));
    }

    @Override
    public String toString() {
        return m_filterModel.toString();
    }

    @Override
    public int hashCode() {
        return m_filterModel.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof FilterHandler) {
            return ((FilterHandler)obj).m_filterModel.equals(m_filterModel);
        }
        return false;
    }


    /** Create filter handler based on a given nominal filter model.
     * @param filterModelNominal Non null model.
     * @return a new handler
     */
    public static FilterHandler from(final FilterModelNominal filterModelNominal) {
        return new FilterHandler(filterModelNominal);
    }

    /** Create filter handler based on a given range filter model.
     * @param filterModelRange Non null model.
     * @return a new handler
     */
    public static FilterHandler from(final FilterModelRange filterModelRange) {
        return new FilterHandler(filterModelRange);
    }

}
