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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.apache.commons.lang3.ObjectUtils;
import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;

/**
 * A filter model with subclasses for range and nominal filters. These are created via the factory methods defined
 * in this class.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 3.3
 */
public abstract class FilterModel {

    /** {@link WeakHashMap} to prevent duplicated filters after load. */
    private static final Map<UUID, FilterModel> INTERNER_MAP = Collections.synchronizedMap(new WeakHashMap<>());

    private final UUID m_filterUUID;

    /** Constructor for both 'new' instance and also instances loaded from disc/config.
     * @param  filterUUID The ID, <code>null</code> for new instances. */
    FilterModel(final UUID filterUUID) {
        m_filterUUID = filterUUID == null ? UUID.randomUUID() : filterUUID;
    }

    /**
     * @return a unique auto-generated ID per FilterModel instance
     */
    public final UUID getFilterUUID() {
        return m_filterUUID;
    }

    /** Determines if the given cell matches the filter (= is included). For a nominal filter it checks whether
     * the given cell is element of the set of nominal values. For a range filter it checks if the value is within
     * the specified range.
     * @param cell Non-null cell to check.
     * @return Whether cell matches the filter. */
    public abstract boolean isInFilter(final DataCell cell);

    @Override
    public String toString() {
        return "ID: " + m_filterUUID;
    }

    @Override
    public int hashCode() {
        return m_filterUUID.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FilterModel)) {
            return false;
        }
        return m_filterUUID.equals(((FilterModel)obj).m_filterUUID);
    }

    /** Saves this object to the argument. Used during load/save of the workflow.
     * @param config To save to, not null.
     */
    public final void save(final ConfigWO config) {
        final String cl;
        if (this instanceof FilterModelNominal) {
            cl = "nominal";
        } else if (this instanceof FilterModelRange) {
            cl = "range";
        } else {
            throw new InternalError("FilterModel class " + getClass().getName() + " not supported");
        }
        config.addString("id", m_filterUUID.toString());
        config.addString("type", cl);
        saveSubclass(config);
    }

    /** Called from {@link #save(ConfigWO)} to save content of sub-class.
     * @param config To save to, not null. */
    abstract void saveSubclass(final ConfigWO config);

    /** Create a new filter model for a numeric range.
     * @param minimum minimum value; negative infinity or NaN represent an unbounded minimum
     * @param maximum maximum value; positive infinity or NaN represent an unbounded maximum
     * @param minimumInclusive if minimum is inclusive or not (ignored for unbounded extreme)
     * @param maximumInclusive if minimum is inclusive or not (ignored for unbounded extreme)
     * @return A new {@link FilterModelRange} representing the values.
     * @throws IllegalArgumentException If minimum is larger than maximum or both min and max are unbounded
     */
    public static FilterModelRange newRangeModel(final double minimum, final double maximum,
        final boolean minimumInclusive, final boolean maximumInclusive) {
        FilterModelRange f = new FilterModelRange(minimum, maximum, minimumInclusive, maximumInclusive);
        INTERNER_MAP.put(f.getFilterUUID(), f);
        return f;
    }

    /** Create a new filter model for a set of nominal values.
     * @param values The values to be included (not null and null values not allowed).
     * @return A new model representing the values. */
    public static FilterModelNominal newNominalModel(final Collection<DataCell> values) {
        FilterModelNominal f = new FilterModelNominal(values);
        INTERNER_MAP.put(f.getFilterUUID(), f);
        return f;
    }

    /**
     * Counterpart to {@link #save(ConfigWO)}. Note that models are 'internalized', that is no two identical objects
     * exist.
     * @param config Non-null config to load from
     * @return A model restored from disc, internalized (using a weak hash map).
     * @throws InvalidSettingsException
     */
    public static FilterModel load(final ConfigRO config) throws InvalidSettingsException {
        final String idS = CheckUtils.checkSettingNotNull(config.getString("id"), "ID must not be null");
        final UUID filterUUID;
        try {
            filterUUID = UUID.fromString(idS);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException("Invalid ID string: " + idS + ": " + e.getMessage(), e);
        }
        final String cl = CheckUtils.checkSettingNotNull(config.getString("type"), "Type must not be null");
        final FilterModel f;
        switch (cl) {
            case "nominal":
                f = FilterModelNominal.loadSubclass(filterUUID, config);
                break;
            case "range":
                f = FilterModelRange.loadSubclass(filterUUID, config);
                break;
            default:
                throw new InvalidSettingsException("Unsupported type: " + cl);
        }
        FilterModel existingModel = INTERNER_MAP.putIfAbsent(filterUUID, f);
        CheckUtils.checkSetting(existingModel == null || existingModel.equals(f),
                "Two un-equal models with the same ID exist; previously used \"%s\" and now loaded \"%s\"",
                existingModel, f);
        return ObjectUtils.defaultIfNull(existingModel, f);
    }
}
