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
 *   Dec 5, 2017 (clemens): created
 */
package org.knime.core.node.streamable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.swing.JComponent;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;

/**
 *
 * @author clemens
 * @since 3.5
 */
public class SharedContainerPortObjectSpec implements PortObjectSpec {

    private Class<?> m_containedClass;

    /**
     * Constructor.
     * @param containedClass the base class for the contained object
     */
    public SharedContainerPortObjectSpec(final Class<?> containedClass) {
        m_containedClass = containedClass;
    }

    public Class<?> getContainedClass() {
        return m_containedClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
     // no views
        return new JComponent[0];
    }

    public class SharedContainerPortObjectSpecSerialzer extends PortObjectSpecSerializer<SharedContainerPortObjectSpec> {

        /**
         * {@inheritDoc}
         */
        @Override
        public SharedContainerPortObjectSpec loadPortObjectSpec(final PortObjectSpecZipInputStream in) throws IOException {

                int bufferlength = 128;
                ByteBuffer buffer = null;
                int ctr = 0;
                while(in.available() > 0) {
                    byte[] b = new byte[bufferlength];
                    in.read(b);
                    ctr++;
                    ByteBuffer tmp = ByteBuffer.allocate(ctr * 128);
                    if(buffer != null) {
                        tmp.put(buffer);
                    }
                    tmp.put(b);
                    buffer = tmp;
                }
                String className = StandardCharsets.UTF_8.decode(buffer).toString();
            try {
                return new SharedContainerPortObjectSpec(Class.forName(className));
            } catch (ClassNotFoundException ex) {
                throw new IOException("Wrapped class " + className + " is not available on system.");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void savePortObjectSpec(final SharedContainerPortObjectSpec portObjectSpec, final PortObjectSpecZipOutputStream out)
            throws IOException {
            out.write(portObjectSpec.getContainedClass().toString().getBytes(StandardCharsets.UTF_8));

        }

    }

}
