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
 */
package org.knime.core.util;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class DataTableSpecColumnNamer {

    private final HashSet<String> m_nameHash;
    /** Pattern with group matching, for instance for a string
     * "Column Name (#5)" group 1 denotes "Column Name" and group 2 "5". */
    private static final Pattern PATTERN =
        Pattern.compile("(^.*) \\(#(\\d+)\\)");

    /**  */
    public DataTableSpecColumnNamer(final DataTableSpec spec) {
        m_nameHash = new HashSet<String>();
        for (DataColumnSpec col : spec) {
            m_nameHash.add(col.getName());
        }
    }

    public String newName(final String suggested) {
        if (m_nameHash.add(suggested)) {
            return suggested;
        }
        int index = 1;
        String baseName = suggested;
        Matcher baseNameMatcher = PATTERN.matcher(baseName);
        if (baseNameMatcher.matches()) {
            baseName = baseNameMatcher.group(1);
            try {
                index = Integer.parseInt(baseNameMatcher.group(2)) + 1;
            } catch (NumberFormatException nfe) {
                // ignore, must be out of range
            }
        }
        String newName;
        do {
           newName = baseName + "(#" + (index++) + ")";
        } while (!m_nameHash.add(newName));
        return newName;
    }

    public DataColumnSpecCreator newCreator(
            final String suggested, final DataType type) {
        String name = newName(suggested);
        return new DataColumnSpecCreator(name, type);
    }

    public DataColumnSpec newColumn(final String suggested,
            final DataType type) {
        return newCreator(suggested, type).createSpec();
    }

}
