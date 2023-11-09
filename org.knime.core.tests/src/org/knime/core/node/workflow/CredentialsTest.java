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
 *   8 Nov 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class CredentialsTest {

    @Test
    void testGetSecondAuthenticationFactor() {
        assertThat(new Credentials("name", "login", "password").getSecondAuthenticationFactor()).isEmpty();
        assertThat(new Credentials("name", "login", "password", "second factor").getSecondAuthenticationFactor()).get()
            .isEqualTo("second factor");
    }

    @Test
    void testSetSecondAuthenticationFactor() {
        final var cred = new Credentials("name", "login", "password");
        cred.setSecondAuthenticationFactor("second factor");
        assertThat(cred.getSecondAuthenticationFactor()).get().isEqualTo("second factor");
        cred.setSecondAuthenticationFactor(null);
        assertThat(cred.getSecondAuthenticationFactor()).isEmpty();
    }

    @Test
    void testClone() {
        final var cred = new Credentials("name", "login", "password");
        final var clone = cred.clone();

        assertEquals(cred, clone);
        assertNotSame(cred, clone);

        assertSame(cred.getName(), clone.getName());
        assertSame(cred.getLogin(), clone.getLogin());
        assertSame(cred.getPassword(), clone.getPassword());
        assertEquals(cred.getSecondAuthenticationFactor(), clone.getSecondAuthenticationFactor());
    }

    @Test
    void testCloneWithSecondFactorPresent() {
        final var cred = new Credentials("name", "login", "password", "second factor");
        final var clone = cred.clone();

        assertEquals(cred, clone);
        assertNotSame(cred, clone);

        assertSame(cred.getName(), clone.getName());
        assertSame(cred.getLogin(), clone.getLogin());
        assertSame(cred.getPassword(), clone.getPassword());
        assertEquals(cred.getSecondAuthenticationFactor(), clone.getSecondAuthenticationFactor());
    }

}
