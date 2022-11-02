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
 *   9 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.impl;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.webui.node.dialog.impl.JsonFormsDataUtil.getMapper;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.PortObjectSpec;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class JsonFormsDataUtilTest {

    @SuppressWarnings("unused")
    private static class TestSettings implements DefaultNodeSettings {
        String fromSettings = "def";

        String m_fromSpec = "def";

        TestSettings() {
        }

        TestSettings(final String settings) {
            fromSettings = settings;
        }

        @Override
        public boolean equals(final Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    private static class TestSettingsSpec extends TestSettings {
        @SuppressWarnings("unused")
        TestSettingsSpec() {
        }

        TestSettingsSpec(final PortObjectSpec[] specs) {
            m_fromSpec = ((DataTableSpec)specs[0]).getColumnSpec(0).getName();
        }
    }

    static PortObjectSpec[] createSpecs(final String name) {
        return new PortObjectSpec[]{new DataTableSpec(new DataColumnSpecCreator(name, StringCell.TYPE).createSpec())};
    }

    @Test
    void testToJsonData() {
        assertThatJson(JsonFormsDataUtil.toJsonData(new TestSettings("foo")))//
            .isObject()//
            .containsEntry("fromSettings", "foo")//
            .containsEntry("fromSpec", "def");
    }

    @Test
    void testToDefaultNodeSettings() {
        assertThat(JsonFormsDataUtil.toDefaultNodeSettings(getMapper().createObjectNode().put("fromSettings", "foo"),
            TestSettings.class)).isEqualTo(new TestSettings("foo"));
    }

    @Test
    void testCreateDefaultNodeSettingsWithSpecs() {
        assertThat(JsonFormsDataUtil.createDefaultNodeSettings(TestSettingsSpec.class, createSpecs("bar")))
            .isEqualTo(new TestSettingsSpec(createSpecs("bar")));
    }

    @Test
    void testCreateDefaultNodeSettingsWithSpecsDefault() {
        assertThat(JsonFormsDataUtil.createDefaultNodeSettings(TestSettings.class, createSpecs("bar")))
            .isEqualTo(new TestSettings());
    }

}
