/*
 * ------------------------------------------------------------------------
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
 *   14.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.time.period;

import java.time.Period;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 * Renders a {@link PeriodValue}.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 * @since 3.3
 */
@SuppressWarnings("serial")
public final class PeriodLongValueRenderer extends DefaultDataValueRenderer {
    private static final PeriodLongValueRenderer INSTANCE = new PeriodLongValueRenderer();

    private static final String DESCRIPTION_PERIOD = "Long Period";

    private PeriodLongValueRenderer() {
    }

    @Override
    public String getDescription() {
        return DESCRIPTION_PERIOD;
    }

    @Override
    protected void setValue(final Object value) {
        if (value instanceof PeriodValue) {
            Period period = ((PeriodValue)value).getPeriod();
            super.setValue(formatPeriodLong(period));
        } else {
            super.setValue(value);
        }
    }

    /** Renderer factory registered through extension point. */
    public static final class PeriodLongRendererFactory extends AbstractDataValueRendererFactory {
        @Override
        public String getDescription() {
            return DESCRIPTION_PERIOD;
        }

        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return INSTANCE;
        }
    }

    /**
     * Formats the given period into a pluralization correct string.
     *
     * @param period the period to format
     * @return formatted string
     */
    // Copied from DurationPeriodFormatUtils
    private static String formatPeriodLong(final Period period) {
        String s = period.toString();
        // remove leading 'P'
        s = StringUtils.replaceOnce(s, "P", " ");

        // replace ISO letters with words
        s = StringUtils.replaceOnce(s, "Y", " years ");
        s = StringUtils.replaceOnce(s, "M", " months ");
        s = StringUtils.replaceOnce(s, "W", " weeks ");
        s = StringUtils.replaceOnce(s, "D", " days ");

        // handle plurals
        s = StringUtils.replaceOnce(s, " 1 years", " 1 year");
        s = StringUtils.replaceOnce(s, " -1 years", " -1 year");
        s = StringUtils.replaceOnce(s, " 1 months", " 1 month");
        s = StringUtils.replaceOnce(s, " 1 weeks", " 1 week");
        s = StringUtils.replaceOnce(s, " -1 months", " -1 month");
        s = StringUtils.replaceOnce(s, " 1 days", " 1 day");
        s = StringUtils.replaceOnce(s, " -1 days", " -1 day");

        return s.trim();
    }
}
