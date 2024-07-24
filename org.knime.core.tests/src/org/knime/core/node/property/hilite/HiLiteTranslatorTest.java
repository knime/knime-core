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
 *   Jul 24, 2024 (hornm): created
 */
package org.knime.core.node.property.hilite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.knime.core.data.RowKey;

/**
 * Tests aspects of the {@link HiLiteTranslator}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class HiLiteTranslatorTest {

    /**
     * Tests that any events that are fired via a {@link HiLiteHandler} associated with a {@link HiLiteTranslator} are
     * still fired synchronously after the translation.
     */
    @Test
    void testFireEventsSynchronously() {
        testFireEvents(false);
    }

    /**
     * Tests that any events that are fired via a {@link HiLiteHandler} associated with a {@link HiLiteTranslator} are
     * still fired asynchronously after the translation.
     */
    @Test
    void testFireEventsAsynchronously() {
        testFireEvents(true);
    }

    void testFireEvents(final boolean async) {
        var rowKey1 = RowKey.toRowKeys("foo")[0];
        var rowKey2 = RowKey.toRowKeys("bar")[0];
        var translator = new HiLiteTranslator(new HiLiteMapper() {

            @Override
            public Set<RowKey> keySet() {
                return Set.of(rowKey1);
            }

            @Override
            public Set<RowKey> getKeys(final RowKey key) {
                assertThat(key).isEqualTo(rowKey1);
                return Set.of(rowKey2);
            }
        });
        var toHiLiteHandler = new HiLiteHandler();
        translator.addToHiLiteHandler(toHiLiteHandler);

        var testHiLiteListener = new TestHiLiteListener();
        toHiLiteHandler.addHiLiteListener(testHiLiteListener);
        var fromHiLiteHandler = translator.getFromHiLiteHandler();

        /* downstream events */

        fromHiLiteHandler.fireHiLiteEvent(new KeyEvent(new Object(), rowKey1), async);
        assertListener(testHiLiteListener, rowKey2, async);

        fromHiLiteHandler.fireUnHiLiteEvent(new KeyEvent(new Object(), rowKey1), async);
        assertListener(testHiLiteListener, rowKey2, async);

        fromHiLiteHandler.fireClearHiLiteEvent(new KeyEvent(new Object(), rowKey1), async);
        assertListener(testHiLiteListener, rowKey2, async);

        /* upstream events */

        testHiLiteListener = new TestHiLiteListener();
        fromHiLiteHandler.addHiLiteListener(testHiLiteListener);

        toHiLiteHandler.fireHiLiteEvent(new KeyEvent(new Object(), rowKey2), async);
        assertListener(testHiLiteListener, rowKey1, async);

        toHiLiteHandler.fireUnHiLiteEvent(new KeyEvent(new Object(), rowKey2), async);
        assertListener(testHiLiteListener, rowKey1, async);

        toHiLiteHandler.fireClearHiLiteEvent(new KeyEvent(new Object(), rowKey2), async);
        assertListener(testHiLiteListener, rowKey1, async);
    }

    private static void assertListener(final TestHiLiteListener testHiLiteListener, final RowKey expectedRowKey,
        final boolean async) {
        if (async) {
            Awaitility.await().untilAsserted(() -> assertThat(testHiLiteListener.m_lastEvent).isNotNull());
        }
        assertThat(testHiLiteListener.m_lastEvent.keys()).isEqualTo(Set.of(expectedRowKey));
        if (async) {
            assertThat(testHiLiteListener.m_lastCallThread).isNotSameAs(Thread.currentThread());
        } else {
            assertThat(testHiLiteListener.m_lastCallThread).isSameAs(Thread.currentThread());
        }
    }

    private class TestHiLiteListener implements HiLiteListener {

        Thread m_lastCallThread;

        KeyEvent m_lastEvent;

        @Override
        public void hiLite(final KeyEvent event) {
            track(event);
        }

        @Override
        public void unHiLite(final KeyEvent event) {
            track(event);
        }

        @Override
        public void unHiLiteAll(final KeyEvent event) {
            track(event);
        }

        private void track(final KeyEvent event) {
            m_lastCallThread = Thread.currentThread();
            m_lastEvent = event;
        }
    }

}
