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
 *   Oct 17, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability.nominal.renderer;

import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.probability.ProbabilityDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 * Renderer for {@link ProbabilityDistributionValue} which prints each class name followed by the corresponding
 * probability percentage.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class NominalDistributionClassNameRenderer extends DefaultDataValueRenderer {

    private final NominalDistributionValueMetaData m_metaData;

    private static final long serialVersionUID = 1L;

    private static final String DESCRIPTION_PROB_DISTR = "Class names and probabilities";

    private NominalDistributionClassNameRenderer(final DataColumnSpec spec) {
        m_metaData = NominalDistributionValueRendererUtil.extractMetaData(spec);
    }

    /**
     * Returns true if the {@link DataColumnSpec} selected contains element names, which are not null or empty, since
     * they are needed to define the class names and if the data type of the selected column is compatible with
     * {@link NominalDistributionValue}, which is the expected type.
     *
     * @return {@code true} is the {@link DataColumnSpec} is accepted, {@code false} otherwise.
     */
    @Override
    public boolean accepts(final DataColumnSpec spec) {
        return NominalDistributionValueRendererUtil.accepts(spec);
    }

    /**
     *
     * @return "Class names and probabilities"
     */
    @Override
    public String getDescription() {
        return DESCRIPTION_PROB_DISTR;
    }

    @Override
    protected void setValue(final Object value) {
        if (value instanceof NominalDistributionValue) {
            final NominalDistributionValue probDistrValue = (NominalDistributionValue)value;
            super.setValue(m_metaData.getValues().stream()
                .map(v -> String.format("%s: %.2f%%", v, probDistrValue.getProbability(v) * 100))
                .collect(Collectors.joining(", ")));
        } else {
            super.setValue(value);
        }
    }

    /**
     * Renderer factory registered through extension point.
     *
     * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
     */
    public static final class ClassNameRendererFactory extends AbstractDataValueRendererFactory {

        @Override
        public String getDescription() {
            return DESCRIPTION_PROB_DISTR;
        }

        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new NominalDistributionClassNameRenderer(colSpec);
        }
    }
}
