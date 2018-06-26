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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 10, 2012 (wiswedel): created
 */
package org.knime.core.node.util.filter;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class NameFilterConfigurationTest {

    private String[] m_defIncludes = new String[] {"I1", "I2", "I3"};
    private String[] m_defExcludes = new String[] {"E1", "E2"};

    private NameFilterConfiguration createConfiguration(final EnforceOption enforceOption) {
        NameFilterConfiguration configuration = new NameFilterConfiguration("ignored");
        configuration.setIncludeList(m_defIncludes);
        configuration.setExcludeList(m_defExcludes);
        configuration.setEnforceOption(enforceOption);
        return configuration;
    }

    @Test
    public void testGoodCaseAllAvailable() throws Exception {
        NameFilterConfiguration conf = createConfiguration(EnforceOption.EnforceExclusion);
        testGoodCaseAllAvailable(conf);
        NodeSettings s = new NodeSettings("foobar");
        conf.saveConfiguration(s);
        conf = new NameFilterConfiguration("ignored");
        conf.loadConfigurationInModel(s);
        testGoodCaseAllAvailable(conf);
    }

    private void testGoodCaseAllAvailable(final NameFilterConfiguration conf) {
        FilterResult applyTo = conf.applyTo(new String[] {"I1", "I2", "I3", "E1", "E2"});
        Assert.assertArrayEquals(applyTo.getIncludes(), m_defIncludes);
        Assert.assertArrayEquals(applyTo.getExcludes(), m_defExcludes);
        Assert.assertArrayEquals(applyTo.getRemovedFromIncludes(), new String[0]);
        Assert.assertArrayEquals(applyTo.getRemovedFromExcludes(), new String[0]);
    }

    @Test
    public void testSomeMissing() throws Exception {
        NameFilterConfiguration conf = createConfiguration(EnforceOption.EnforceExclusion);
        testSomeMissing(conf);
        NodeSettings s = new NodeSettings("foobar");
        conf.saveConfiguration(s);
        conf = new NameFilterConfiguration("ignored");
        conf.loadConfigurationInModel(s);
        testSomeMissing(conf);
    }

    private void testSomeMissing(final NameFilterConfiguration conf) {
        FilterResult applyTo = conf.applyTo(new String[] {"I1", "I3", "E1"});
        Assert.assertArrayEquals(applyTo.getIncludes(), new String[] {"I1", "I3"});
        Assert.assertArrayEquals(applyTo.getExcludes(), new String[] {"E1"});
        Assert.assertArrayEquals(applyTo.getRemovedFromIncludes(), new String[] {"I2"});
        Assert.assertArrayEquals(applyTo.getRemovedFromExcludes(), new String[] {"E2"});
    }

    @Test
    public void testSomeExtra() throws Exception {
        NameFilterConfiguration conf = createConfiguration(EnforceOption.EnforceExclusion);
        testSomeExtra(conf);
        NodeSettings s = new NodeSettings("foobar");
        conf.saveConfiguration(s);
        conf = new NameFilterConfiguration("ignored");
        conf.loadConfigurationInModel(s);
        testSomeExtra(conf);
    }

    private void testSomeExtra(final NameFilterConfiguration conf) {
        FilterResult applyTo = conf.applyTo(new String[] {"I1", "I2", "N1", "I3", "E1", "E2", "N2"});
        Assert.assertArrayEquals(applyTo.getIncludes(), new String[] {"I1", "I2", "N1", "I3", "N2"});
        Assert.assertArrayEquals(applyTo.getExcludes(), m_defExcludes);
        Assert.assertArrayEquals(applyTo.getRemovedFromIncludes(), new String[] {});
        Assert.assertArrayEquals(applyTo.getRemovedFromExcludes(), new String[] {});
    }

    @Test
    public void testShuffledWithSomeMissingAndSomeExtra_Enforce() throws Exception {
        NameFilterConfiguration conf = createConfiguration(EnforceOption.EnforceExclusion);
        FilterResult applyTo = conf.applyTo(new String[] {"N1", "E1", "I3", "I2", "N2", "N3"});
        Assert.assertArrayEquals(applyTo.getIncludes(), new String[] {"N1", "I3", "I2", "N2", "N3"});
        Assert.assertArrayEquals(applyTo.getExcludes(), new String[] {"E1"});
        Assert.assertArrayEquals(applyTo.getRemovedFromIncludes(), new String[] {"I1"});
        Assert.assertArrayEquals(applyTo.getRemovedFromExcludes(), new String[] {"E2"});
    }

    @Test
    public void testShuffledWithSomeMissingAndSomeExtra_Include() throws Exception {
        NameFilterConfiguration conf = createConfiguration(EnforceOption.EnforceInclusion);
        FilterResult applyTo = conf.applyTo(new String[] {"N1", "E1", "I3", "I2", "N2", "N3"});
        Assert.assertArrayEquals(applyTo.getIncludes(), new String[] {"I3", "I2"});
        Assert.assertArrayEquals(applyTo.getExcludes(), new String[] {"N1", "E1", "N2", "N3"});
        Assert.assertArrayEquals(applyTo.getRemovedFromIncludes(), new String[] {"I1"});
        Assert.assertArrayEquals(applyTo.getRemovedFromExcludes(), new String[] {"E2"});
    }

}
