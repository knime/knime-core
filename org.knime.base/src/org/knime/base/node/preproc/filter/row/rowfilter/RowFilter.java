/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   29.06.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Used by the {@link org.knime.base.node.preproc.filter.row.RowFilterIterator}
 * to determine whether a row should be filtered or not. <br>
 * New row filter implementations MUST also modify the
 * {@link org.knime.base.node.preproc.filter.row.rowfilter.RowFilterFactory} in
 * order to get load and save work.
 *
 * @author Peter Ohl, University of Konstanz
 */
public abstract class RowFilter implements Cloneable {

    /**
     * Return <code>true</code> if the specified row matches the criteria set
     * in the filter. Can throw a {@link EndOfTableException} if the filter can
     * tell that no more rows of the table will be able to fulfill the criteria.
     *
     * @param row the row to test
     * @param rowIndex the row index of the passed row in the original table
     * @return <code>true</code> if the row matches the criteria set in the
     *         filter, <code>false</code> if not
     * @throws EndOfTableException if there is no chance that any of the rows
     *             coming (including the current <code>rowIndex</code>) will
     *             fulfill the criteria, thus no further row in the original
     *             table will be a match to this filter. (In general this is
     *             hard to tell, but a row number filter can certainly use it.)
     *             If the exception is received the row filter table iterator
     *             will flag an end of table.
     * @throws IncludeFromNowOn if the current and all following rows from now
     *             on are to be included into the result table
     */
    public abstract boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn;

    /**
     * Load your internal settings from the configuration object. Throw an
     * exception if the config is invalid/incorrect/inconsistent.
     *
     * @param cfg the object holding the settings to load
     * @throws InvalidSettingsException if cfg contains
     *             invalid/incorrect/inconsistent settings
     */
    public abstract void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException;

    /**
     * Save your internal settings into the specified configuration object.
     * Passing the object then to the loadSettingsFrom method should flawlessly
     * work.
     *
     * @param cfg the object to add the current internal settings to
     */
    public final void saveSettingsTo(final NodeSettingsWO cfg) {
        RowFilterFactory.prepareConfigFor(cfg, this);
        saveSettings(cfg);
    }

    /**
     * Do not call this function - rather call
     * {@link #saveSettingsTo(NodeSettingsWO)}. This is just a helper function
     * for {@link #saveSettingsTo(NodeSettingsWO)}. Row filters implement this
     * and do the work usually done in {@link #saveSettingsTo(NodeSettingsWO)}.
     * The passed config is prepared in a way that the factory will be able to
     * recreate this object from it.
     *
     * @param cfg object to add the current internal settings to
     */
    protected abstract void saveSettings(final NodeSettingsWO cfg);

    /**
     * Called when a new {@link DataTableSpec} is available. The filters can
     * grab whatever they need from that new config (e.g. a comparator), should
     * do some error checking (e.g. col number against number of columns) -
     * throw an {@link InvalidSettingsException} if settings are invalid, and
     * can return a new table spec according to their settings - if they can. If
     * a filter cannot tell how it would modify the spec, it should return null.
     * (Returned table specs are not used right now anyway.)
     *
     * @param inSpec the new spec propagated into the row filter node. Could be
     *            null or empty!
     * @return a new table spec, if you can
     * @throws InvalidSettingsException if the settings in the row filter are
     *             not compatible with the table spec coming in
     */
    public abstract DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            // this shouldn't happen, since we are cloneable
            throw new InternalError();
        }
    }
}
