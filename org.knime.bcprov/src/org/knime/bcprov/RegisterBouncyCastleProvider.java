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
 *   Feb 13, 2026 (bjoern): created
 */
package org.knime.bcprov;

import java.security.Provider;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.IEarlyStartup;

/**
 * The BouncyCastle bcprov library provides a Java Security provider, that extends
 * the list of cryptographic primitives available to code running in the JVM, e.g.  *
 * additional hash functions, encryption algorithms, etc. KNIME provides bcprov through
 * its target platform, but other plugins may contain bcprov duplicates. This class ensures
 * that the BouncyCastleProvider from the target platform is registered as a Java Security
 * provider, before any of its duplicates in other plugins can do so.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class RegisterBouncyCastleProvider implements IEarlyStartup {

    private static final NodeLogger LOG = NodeLogger.getLogger(RegisterBouncyCastleProvider.class);

    @Override
    public void run() {
        try {
            // make sure that the BouncyCastleProvider from the target platform (bcprov) is
            // found and registered, before any of its duplicates in other plugins can do so.
            var bcProviderIdx = indexOfBadProvider(Security.getProviders());
            while (bcProviderIdx != -1) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                bcProviderIdx = indexOfBadProvider(Security.getProviders());
            }
            // this is safe to be added even if it is already registered
            Security.addProvider(new BouncyCastleProvider());
            LOG.info("Added BouncyCastleProvider to Java Security providers");
        } catch (Exception e) {
            LOG.warn("Failed to install BouncyCastleProvider in list of Java Security providers: " + e.getMessage(), //
                    e);
        }
    }

    /**
     *
     * @param providers
     * @return first index of provider that has the BouncyCastle provider name, but is not an
     *         instance of the target platform class
     */
    private static int indexOfBadProvider(final Provider[] providers) {
        for (int i = 0; i < providers.length; i++) {
            // a provider is considered "bad" if it has the BouncyCastle provider name, but is not
            // an instance of the correct class
            if (providers[i].getName().equals(BouncyCastleProvider.PROVIDER_NAME)//
                    && !(providers[i] instanceof BouncyCastleProvider)) {
                return i;
            }
        }
        return -1;
    }
}
