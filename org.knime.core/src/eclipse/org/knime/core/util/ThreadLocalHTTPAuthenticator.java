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
 *   20.05.2016 (thor): created
 */
package org.knime.core.util;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.knime.core.node.NodeLogger;

/**
 * This {@link Authenticator} allows threads to turn off any authentication callbacks in case authentication data is
 * wrong or missing. Usage is as follow:
 * <pre>
 * try (Closeable c = ThreadLocalHTTPAuthenticator.suppressAuthenticationPopups()) {
 *   // dome HTTP stuff
 * }
 * </pre>
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 3.2
 */
public class ThreadLocalHTTPAuthenticator extends Authenticator {
    private final Authenticator m_delegate;

    private static final ThreadLocal<Boolean> SUPPRESS_POPUP = new ThreadLocal<>();

    /**
     * Install a thread local authenticator as the default authenticator. Calls will be delegated to any existing
     * authenticator.
     */
    public static void installAuthenticator() {
        for (final Field f : Authenticator.class.getDeclaredFields()) {
            if (f.getType().equals(Authenticator.class)) {
                f.setAccessible(true);
                try {
                    Authenticator delegate = (Authenticator)f.get(null);
                    if ((delegate != null) && !(delegate instanceof ThreadLocalHTTPAuthenticator)) {
                        Authenticator.setDefault(new ThreadLocalHTTPAuthenticator(delegate));
                    }
                } catch (Exception ex) {
                    NodeLogger.getLogger(ThreadLocalHTTPAuthenticator.class)
                        .warn("Could not install HTTP authenticator: " + ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Turns suppression of authentication popups off. Make sure to close the returned closeable after you have
     * performed all HTTP operations (hint: use try-with-resources).
     *
     * @return a closeable that enables popups again (if there were any before)
     */
    public static Closeable suppressAuthenticationPopups() {
        SUPPRESS_POPUP.set(Boolean.TRUE);
        return new Closeable() {
            @Override
            public void close() {
                SUPPRESS_POPUP.set(Boolean.FALSE);
            }
        };
    }

    private ThreadLocalHTTPAuthenticator(final Authenticator delegate) {
        m_delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (SUPPRESS_POPUP.get() == Boolean.TRUE) {
            return null;
        }

        try {
            // write request values from this object into the delegate
            for (final Field f : Authenticator.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    Object o = f.get(this);
                    f.set(m_delegate, o);
                }
            }

            final Method m = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
            m.setAccessible(true);
            return (PasswordAuthentication)m.invoke(m_delegate);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ex) {
            NodeLogger.getLogger(getClass()).warn("Could not delegate HTTP authentication request: " + ex.getMessage(),
                ex);
            return null;
        }
    }
}
