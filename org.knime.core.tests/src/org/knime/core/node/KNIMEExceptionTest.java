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
 *   Jan 13, 2023 (wiswedel): created
 */
package org.knime.core.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.knime.core.node.KNIMEException.KNIMERuntimeException;
import org.knime.core.node.message.Message;

/**
 * Tests basic functionality of {@link KNIMEException}.
 *
 * @author Bernd Wiswedel, KNIME GmbH
 */
final class KNIMEExceptionTest {

    /**
     * Test method for {@link org.knime.core.node.KNIMEException#KNIMEException(java.lang.String, java.lang.Throwable)}.
     */
    @SuppressWarnings("static-method")
    @Test
    final void testKNIMEExceptionStringThrowable() {
        var cause = new RuntimeException("secret cause");
        var ex = new KNIMEException("foo", cause);
        assertThat(ex.getKNIMEMessage()).isNotNull().extracting(Message::getSummary).isEqualTo("foo");

        var ex2 = new KNIMEException(null, null);
        assertThat(ex2.getKNIMEMessage()).isNotNull().extracting(Message::getSummary).isEqualTo("unknown reason");

        var ex3 = new KNIMEException(null, new RuntimeException((String)null));
        assertThat(ex3.getKNIMEMessage()).isNotNull().extracting(Message::getSummary).isEqualTo("unknown reason");

        var ex4 = new KNIMEException(null, new RuntimeException("inner cause"));
        assertThat(ex4.getKNIMEMessage()).isNotNull().extracting(Message::getSummary).isEqualTo("inner cause");
    }

    /**
     * Test method for {@link org.knime.core.node.KNIMEException#KNIMEException(java.lang.String)}.
     */
    @SuppressWarnings("static-method")
    @Test
    final void testKNIMEExceptionString() {
        var ex = new KNIMEException("foo");
        assertThat(ex.getKNIMEMessage()).isNotNull().extracting(Message::getSummary).isEqualTo("foo");

        var ex2 = new KNIMEException(null);
        assertThat(ex2.getKNIMEMessage()).isNotNull().extracting(Message::getSummary).isEqualTo("unknown reason");
    }

    /**
     * Test method for {@link org.knime.core.node.KNIMEException#of(org.knime.core.node.message.Message)}.
     */
    @SuppressWarnings("static-method")
    @Test
    final void testOfMessage() {
        var m = Message.builder().withSummary("some summary").addResolutions("secret resolution").build().orElseThrow();
        var ex = KNIMEException.of(m);
        assertThat(ex).extracting(Exception::getMessage).isEqualTo("some summary");
        assertThat(ex).extracting(KNIMEException::getKNIMEMessage).isSameAs(m);

        assertThat(ex.toUnchecked()).extracting(KNIMERuntimeException::getCause).isSameAs(ex);
        assertThat(ex.toUnchecked()).extracting(KNIMERuntimeException::getKNIMEMessage).isSameAs(m);

        var ex1 = KNIMEException.of(m, new RuntimeException());
        assertThat(ex1).extracting(Exception::getMessage).isEqualTo("some summary");
        assertThat(ex1).extracting(KNIMEException::getKNIMEMessage).isSameAs(m);

        assertDoesNotThrow(() -> KNIMEException.of(m, null));
        assertThrows(IllegalArgumentException.class, () -> KNIMEException.of(null, new RuntimeException()));
    }

}
