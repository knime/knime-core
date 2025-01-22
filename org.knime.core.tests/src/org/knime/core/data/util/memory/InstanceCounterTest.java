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
 *   Oct 3, 2024 (wiswedel): created
 */
package org.knime.core.data.util.memory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link InstanceCounter}.
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
final class InstanceCounterTest {

    private static final class SomeClass {
        private static final InstanceCounter<SomeClass> COUNTER = InstanceCounter.register(SomeClass.class);

        SomeClass() {
            COUNTER.track(this); // NOSONAR
        }
    }

    @SuppressWarnings("static-method")
    @Test
    final void testIncrementAndDecrement() throws InterruptedException {
        // assert counter is 0
        assertEquals(SomeClass.COUNTER.get(), 0, "Counter before test should be 0");
        // create some instances
        SomeClass[] instances = new SomeClass[1000];
        for (int i = 0; i < instances.length; i++) {
            instances[i] = new SomeClass();
        }
        assertEquals(SomeClass.COUNTER.get(), instances.length, "Counter after creation");
        Long valueInStream = InstanceCounter.stream().filter(e -> e.getKey().contains(SomeClass.class.getName()))
            .findFirst().orElseThrow().getValue();
        assertEquals(valueInStream, instances.length, "Counter after creation (in stream)");
        InstanceCounter.log();
        instances[0].toString(); // need to do "something with the array, otherwise it might be early-gc'ed
        instances = null;
        System.gc(); // trigger garbage collection
        long start = System.currentTimeMillis();
        while (SomeClass.COUNTER.get() >= 1000 && System.currentTimeMillis() - start < 1000) {
            Thread.sleep(50);
        }
        assertTrue(SomeClass.COUNTER.get() < 1000, "Counter after GC");

        assertThat("Unexpected counter name", SomeClass.COUNTER.getName(), is(SomeClass.class.getName()));
    }

}
