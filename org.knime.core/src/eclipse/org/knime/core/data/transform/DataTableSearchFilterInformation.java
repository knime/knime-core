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
 *   11 Mar 2019 (albrecht): created
 */
package org.knime.core.data.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 *
 * @noextend This class is not intended to be subclassed by clients. Pending API
 * @noinstantiate This class is not intended to be instantiated by clients. Pending API
 * @noreference This class is not intended to be referenced by clients. Pending API
 */
public class DataTableSearchFilterInformation implements DataTableFilterInformation {

    private final int[] m_colIndices;
    private final String m_searchTerm;
    private Pattern m_pattern = null;

    /**
     * @param searchTerm
     * @param isRegex
     * @param isSmart
     * @param colIndices
     */
    public DataTableSearchFilterInformation(final String searchTerm, final boolean isRegex, final boolean isSmart,
        final int... colIndices) {
        m_colIndices = colIndices;
        m_searchTerm = searchTerm;
        if (isRegex) {
            m_pattern = Pattern.compile(searchTerm);
        }
        if (isSmart) {
            // Construct 'smart' regex. The following pattern compilation is adapted from DataTables.net 'smart' search
            // @see https://datatables.net/reference/api/search()
            Pattern isQuoted = Pattern.compile("^\"(.*)\"$");
            Pattern split = Pattern.compile("\"[^\"]+\"|[^\\s]+");
            Matcher splitMatcher = split.matcher(searchTerm);
            List<String> individualTerms = new ArrayList<String>();
            while(splitMatcher.find()) {
                individualTerms.add(splitMatcher.group());
            }
            for (int i = 0; i < individualTerms.size(); i++) {
                if (individualTerms.get(i).startsWith("\"")) {
                    String term = individualTerms.get(i);
                    Matcher matcher = isQuoted.matcher(term);
                    if (matcher.find()) {
                        term = matcher.group(0);
                    }
                    individualTerms.set(i, term.replaceFirst("\"", ""));
                }
            }
            m_pattern = Pattern.compile("^(?=.*?\\Q" + String.join("\\E)(?=.*?\\Q", individualTerms) + "\\E).*$");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRowIncluded(final DataRow row) {
        // empty search matches by default, no need to iterate over cells
        if (StringUtils.isEmpty(m_searchTerm)) {
            return true;
        }
        // this matches the search term found in ANY of the specified columns
        for (int col : m_colIndices) {
            DataCell cell = row.getCell(col);
            if (cell.isMissing()) {
                continue;
            }
            String cellContent = null;
            //TODO: this is very naive as it does not account for special formatting applied to cells, etc.
            if (cell.getType().isCompatible(StringValue.class)) {
                cellContent = ((StringValue)cell).getStringValue();
            } else if (cell.getType().isCompatible(DoubleValue.class)) {
                cellContent = Double.toString(((DoubleValue)cell).getDoubleValue());
            }
            if (cellContent == null) {
                continue;
            }
            if (m_pattern != null) {
                Matcher match = m_pattern.matcher(cellContent);
                if (match.find()) {
                    return true;
                }
            } else if (cellContent.contains(m_searchTerm)) {
                return true;
            }
        }
        return false;
    }

}
