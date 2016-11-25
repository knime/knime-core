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
 *   Oct 25, 2016 (wiswedel): created
 */
package org.knime.core.data.property.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * A FilterModel representing a selection of nominal values of a column. Instances of this object
 * are constructed via {@link FilterModel#newNominalModel(Collection)}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 3.3
 */
public final class FilterModelNominal extends FilterModel {

    private final Collection<DataCell> m_values;

    /** Defines filter based on a collection of included elements.
     * @param values The values to be included (not null and null values not allowed). */
    FilterModelNominal(final Collection<DataCell> values) {
        this(null, values);
    }

    /** Load constructor. */
    private FilterModelNominal(final UUID filterUUID, final Collection<DataCell> values) {
        super(filterUUID);
        CheckUtils.checkArgumentNotNull(values);
        m_values = Collections.unmodifiableList(new ArrayList<DataCell>(values));
        CheckUtils.checkArgument(!m_values.contains(null), "Null elements not allowed");
    }

    /**
     * @return unmodifiable collection of the values.
     */
    public Collection<DataCell> getValues() {
        return m_values;
    }

    @Override
    public boolean isInFilter(final DataCell cell) {
        return m_values.contains(cell);
    }

    @Override
    public String toString() {
        return "[" + ConvenienceMethods.getShortStringFrom(m_values, 10) + "] (" + super.toString() + ")";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).append(m_values).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FilterModelNominal)) {
            return false;
        }
        FilterModelNominal fobj = (FilterModelNominal)obj;
        return super.equals(fobj) && m_values.equals(fobj.m_values);
    }

    @Override
    void saveSubclass(final ConfigWO config) {
        config.addDataCellArray("values", m_values.toArray(new DataCell[m_values.size()]));
    }

    /** Load factory method.
     * @param filterUUID Non-null ID as loaded by super class.
     * @param config Non-null config to read values from.
     * @return A new Filter.
     * @throws InvalidSettingsException ...
     */
    static FilterModelNominal loadSubclass(final UUID filterUUID, final ConfigRO config)
            throws InvalidSettingsException {
        DataCell[] values = CheckUtils.checkSettingNotNull(
            config.getDataCellArray("values"), "Values array must not be null");
        return new FilterModelNominal(filterUUID, Arrays.asList(values));
    }

}
