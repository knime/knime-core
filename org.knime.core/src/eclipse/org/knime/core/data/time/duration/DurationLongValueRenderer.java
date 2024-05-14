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
package org.knime.core.data.time.duration;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 * Renders a {@link DurationValue}.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 * @since 3.3
 */
@SuppressWarnings("serial")
public final class DurationLongValueRenderer extends DefaultDataValueRenderer {
    private static final DurationLongValueRenderer INSTANCE = new DurationLongValueRenderer();

    private static final String DESCRIPTION_DURATION = "Long Duration";

    private DurationLongValueRenderer() {
    }

    @Override
    public String getDescription() {
        return DESCRIPTION_DURATION;
    }

    @Override
    protected void setValue(final Object value) {
        if (value instanceof DurationValue dv) {
            final var duration = dv.getDuration();
            super.setValue(formatDurationLong(duration));
        } else {
            super.setValue(value);
        }
    }

    /** Renderer factory registered through extension point. */
    public static final class DurationLongRendererFactory extends AbstractDataValueRendererFactory {
        @Override
        public String getDescription() {
            return DESCRIPTION_DURATION;
        }

        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return INSTANCE;
        }
    }


    /**
     * Formats the given duration into a pluralization correct string.
     *
     * @param duration the duration to format
     * @return formatted string
     */
    // Copied from DurationPeriodFormatUtils
    private static String formatDurationLong(final Duration duration) {
        var s = duration.toString();
        // remove leading 'PT'
        s = StringUtils.replaceOnce(s, "PT", " ");

        // replace ISO letters with words
        s = StringUtils.replaceOnce(s, "H", " hours ");
        s = StringUtils.replaceOnce(s, "M", " minutes ");
        s = StringUtils.replaceOnce(s, "S", " seconds ");

        // handle plurals
        s = StringUtils.replaceOnce(s, " 1 seconds", " 1 second");
        s = StringUtils.replaceOnce(s, " 1 minutes", " 1 minute");
        s = StringUtils.replaceOnce(s, " 1 hours", " 1 hour");
        s = StringUtils.replaceOnce(s, " -1 seconds", " -1 second");
        s = StringUtils.replaceOnce(s, " -1 minutes", " -1 minute");
        s = StringUtils.replaceOnce(s, " -1 hours", " -1 hour");
        return s.trim();
    }
}
