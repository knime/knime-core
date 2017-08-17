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
 *   Apr 11, 2017 (hornm): created
 */
package org.knime.core.gateway.serverproxy.test.service;

import static org.knime.core.gateway.entities.EntityBuilderManager.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.gateway.v0.test.entity.TestEnt;
import org.knime.core.gateway.v0.test.entity.builder.TestEntBuilder;
import org.knime.core.gateway.v0.test.service.TestService;
import org.knime.core.gateway.v0.workflow.entity.builder.XYEntBuilder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultTestService implements TestService {

    /**
     * {@inheritDoc}
     */
    @Override
    public TestEnt getTest() {
        return builder(TestEntBuilder.class)
            .setOther("bliblablub")
            .setPrimitiveList(Arrays.asList("1", "2", "3"))
            .setPrimitiveMap(Collections.emptyMap())
            .setXY(builder(XYEntBuilder.class).setX(10).setY(15).build())
            .setXYList(Arrays.asList(builder(XYEntBuilder.class).setX(3).setY(2).build()))
            .setXYMap(Collections.emptyMap()).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestEnt updateTest(final TestEnt id) {
        return builder(TestEntBuilder.class)
            .setOther(id.getOther() + "_updated")
            .setPrimitiveList(id.getPrimitiveList())
            .setPrimitiveMap(id.getPrimitiveMap())
            .setXY(id.getXY())
            .setXYList(id.getXYList())
            .setXYMap(id.getXYMap()).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TestEnt> updateTestList(final List<TestEnt> list) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updatePrimitives(final String s, final List<String> stringlist) {
        return 0;
    }

}
