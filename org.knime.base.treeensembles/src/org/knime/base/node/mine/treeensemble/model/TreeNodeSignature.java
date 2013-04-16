/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 5, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeNodeSignature {

    public static final TreeNodeSignature ROOT_SIGNATURE =
        new TreeNodeSignature();

    private final short[] m_signature;

    /**
     *  */
    private TreeNodeSignature() {
        this(new short[] {0});
    }

    private TreeNodeSignature(final short[] signature) {
        m_signature = signature;
    }

    public short[] getSignaturePath() {
        return m_signature;
    }

    public TreeNodeSignature createChildSignature(final short childIndex) {
        short[] newArray = Arrays.copyOf(m_signature, m_signature.length + 1);
        newArray[m_signature.length] = childIndex;
        return new TreeNodeSignature(newArray);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < m_signature.length; i++) {
            b.append(i == 0 ? "" : "-").append(m_signature[i]);
        }
        return b.toString();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_signature);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TreeNodeSignature) {
            short[] oSignature = ((TreeNodeSignature)obj).m_signature;
            return Arrays.equals(oSignature, m_signature);
        }
        return false;
    }

    public void save(final DataOutputStream out) throws IOException {
        out.writeInt(m_signature.length);
        for (int i = 0; i < m_signature.length; i++) {
            out.writeShort(m_signature[i]);
        }
    }

    public static TreeNodeSignature load(final DataInputStream in)
        throws IOException {
        final int length = in.readInt();
        short[] signature = new short[length];
        for (int i = 0; i < length; i++) {
            signature[i] = in.readShort();
        }
        return new TreeNodeSignature(signature);
    }

}
