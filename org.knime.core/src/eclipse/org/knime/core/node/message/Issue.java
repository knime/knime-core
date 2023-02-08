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
 *   Dec 14, 2022 (wiswedel): created
 */
package org.knime.core.node.message;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;

/**
 * An Issue is part of a {@link Message} and describes a single error or warning, along with its location. For nodes
 * processing data tables, this location is the row/column and the concrete implementation is {@link RowIssue}. More
 * generic "issues" are represented by {@link DefaultIssue}.
 *
 * <p>
 * Issues can be added to a {@link MessageBuilder}, e.g. via {@link MessageBuilder#newRowIssueCollector(int)}.
 *
 * @author Bernd Wiswedel, KNIME, Konstanz, Germany
 * @since 5.0
 */
@SuppressWarnings("javadoc")
interface Issue {

    /**
     * Type used internally to be able to restore issues in a saved workflow.
     */
    enum Type {
        TABLE_ROW(RowIssue::load),
        TEXT(DefaultIssue::load);

        private interface IssueLoader {
            Issue load(ConfigBaseRO config) throws InvalidSettingsException;
        }

        private final IssueLoader m_loader;

        Type(final IssueLoader loader) {
            m_loader = loader;
        }

        @SuppressWarnings("unchecked")
        <T extends Issue> T loadIssue(final ConfigBaseRO config) throws InvalidSettingsException {
            return (T)m_loader.load(config);
        }

        void saveType(final ConfigBaseWO config) {
            config.addString("type", name());
        }

        static Type loadType(final ConfigBaseRO config) throws InvalidSettingsException {
            var typeS = config.getString("type");
            return Arrays.stream(values()) //
                    .filter(t -> t.name().equals(typeS)) //
                    .findFirst() //
                    .orElseThrow(() -> new InvalidSettingsException("Invalid type: " + typeS));
        }

    }

    /**
     * The string that sufficiently describes this issue. In the UI it's shown in fixed font.
     *
     * @return That string, not <code>null</code>.
     */
    String toPreformatted();

    Type getType();

    void saveTo(ConfigBaseWO config);

}
