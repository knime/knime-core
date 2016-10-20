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
 *   Nov 20, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.knime.base.data.aggregation.dialogutil.type.DataTypeNameSorter;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
public class DBUtil {

    /** Default SQL-Type for String */
    static final String SQL_TYPE_STRING = "varchar(255)";

    /** Default SQL-Type for Boolean */
    static final String SQL_TYPE_BOOLEAN = "boolean";

    /** Default SQL-Type for Integer */
    static final String SQL_TYPE_INTEGER = "integer";

    /** Default SQL-Type for Double */
    static final String SQL_TYPE_DOUBLE = "numeric(30,10)";

    /** Default SQL-Type for Timestamps */
    static final String SQL_TYPE_DATEANDTIME = "timestamp";

    /** Default SQL-Type for Binary */
    static final String SQL_TYPE_BLOB = "blob";


    /**
     * @param spec
     * @return the {@link List} of {@link DataType}s the user can choose from
     */
    static List<DataType> getKNIMETypeList(final DataTableSpec spec) {
        final Set<DataType> basicTypes = new HashSet<>();
        final DataType generalType = DataType.getType(DataCell.class);
        basicTypes.add(generalType);
        basicTypes.add(BooleanCell.TYPE);
        basicTypes.add(IntCell.TYPE);
        basicTypes.add(LongCell.TYPE);
        basicTypes.add(DoubleCell.TYPE);
        basicTypes.add(StringCell.TYPE);
        basicTypes.add(DateAndTimeCell.TYPE);

        final Set<DataType> types = new HashSet<>();
        types.addAll(basicTypes);
        for (DataType type : basicTypes) {
            types.add(ListCell.getCollectionType(type));
            types.add(SetCell.getCollectionType(type));
        }
        //also add the types from the input spec if available
        if (spec != null) {
            for (DataColumnSpec colSpec : spec) {
                final DataType type = colSpec.getType();
                types.add(type);
                if (!type.isCollectionType()) {
                    types.add(ListCell.getCollectionType(type));
                    types.add(SetCell.getCollectionType(type));
                }
            }
        }
        final List<DataType> typeList = new ArrayList<>(types);
        Collections.sort(typeList, DataTypeNameSorter.getInstance());
        return typeList;
    }

    static Map<DataType, Set<String>> getSqlTypesMap() {
        Map<DataType, Set<String>> map = new LinkedHashMap<>();
        // BooleanCell
        Set<String> compatibleTypes = new TreeSet<>();
        compatibleTypes.add(SQL_TYPE_BOOLEAN);
        map.put(BooleanCell.TYPE, compatibleTypes);

        // IntCell
        compatibleTypes = new TreeSet<>();
        compatibleTypes.add(SQL_TYPE_INTEGER);
        map.put(IntCell.TYPE, compatibleTypes);

        // DoubleCell
        compatibleTypes = new TreeSet<>();
        compatibleTypes.add(SQL_TYPE_DOUBLE);
        map.put(DoubleCell.TYPE, compatibleTypes);

        // DateAndTimeCell
        compatibleTypes = new TreeSet<>();
        compatibleTypes.add(SQL_TYPE_DATEANDTIME);
        map.put(DateAndTimeCell.TYPE, compatibleTypes);

        // BinaryObjectDataCell
        compatibleTypes = new TreeSet<>();
        compatibleTypes.add(SQL_TYPE_BLOB);
        map.put(BinaryObjectDataCell.TYPE, compatibleTypes);

        // StringCell
        compatibleTypes = new TreeSet<>();
        compatibleTypes.add(SQL_TYPE_STRING);
        map.put(StringCell.TYPE, compatibleTypes);

        return map;
    }

    static Map<DataType, Set<String>> getSqlTypesMap(final String dbIdentifier) {
        return getSqlTypesMap();
    }

    static List<String> getSqlTypes(final String dbIdentifier) {
        return getSqlTypes();
    }

    /**
     * @return all supported SQL types
     */
    static List<String> getSqlTypes() {
        return Arrays.asList(new String[] {
            SQL_TYPE_BOOLEAN,
            SQL_TYPE_INTEGER,
            SQL_TYPE_DOUBLE,
            SQL_TYPE_DATEANDTIME,
            SQL_TYPE_BLOB,
            SQL_TYPE_STRING
        });
    }

    /**
     * @param knimeType the KNIME data type whose default SQL type should be returned
     * @return the default SQL type of the given KNIME data type
     */
    static String getDefaultSQLType(final DataType knimeType) {
        if (knimeType.isCompatible(BooleanValue.class)) {
            return SQL_TYPE_BOOLEAN;
        } else if (knimeType.isCompatible(IntValue.class)) {
            return SQL_TYPE_INTEGER;
        } else if (knimeType.isCompatible(DoubleValue.class)) {
            return SQL_TYPE_DOUBLE;
        } else if (knimeType.isCompatible(DateAndTimeValue.class)) {
            return SQL_TYPE_DATEANDTIME;
        } else if (knimeType.isCompatible(BinaryObjectDataValue.class)) {
            return SQL_TYPE_BLOB;
        } else {
            return SQL_TYPE_STRING;
        }
    }

}
