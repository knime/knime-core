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
 *   16.09.2015 (thor): created
 */
package org.knime.core;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Checks that the KNIME.com CA certificate is present in the bundled JRE's default keystore.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class JreTests {
    /**
     * Checks that the JRE's default keystore contains the KNIME.com CA certificate.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @Ignore("Does not work with tycho")
    public void checkForCACertificate() throws Exception {
        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore)null);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                X509TrustManager x509TrustManager = (X509TrustManager)trustManager;
                for (X509Certificate cert : x509TrustManager.getAcceptedIssuers()) {
                    if (cert.getSubjectDN().getName().equals("CN=KNIME.com CA, O=KNIME.com, L=Zurich, C=CH")) {
                        return;
                    }
                }
            }
        }

        fail("No CA certificate for KNIME.com found in default keystore");
    }

    /**
     * Checks that the JRE's default keystore contains the default server certificate.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @Ignore("Does not work with tycho")
    public void checkForDefaultServerCertificate() throws Exception {
        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore)null);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                X509TrustManager x509TrustManager = (X509TrustManager)trustManager;
                for (X509Certificate cert : x509TrustManager.getAcceptedIssuers()) {
                    if (cert.getSubjectDN().getName().equals(
                        "CN=default-server-installation.knime.local, O=KNIME.com AG, L=Atlantis, ST=Utopia, C=AA")) {
                        return;
                    }
                }
            }
        }

        fail("No default server certificate found in default keystore");
    }

    /**
     * Checks the the Java Cryptography Extension is installed in the JRE.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void checkJCEExtension() throws Exception {
        assertThat("JCE does not seem to be installed, allowed cipher length for AES is too small",
            Cipher.getMaxAllowedKeyLength("AES"), is(greaterThan(128)));
    }
}
