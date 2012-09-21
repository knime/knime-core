/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on Sep 13, 2012 by wiswedel
 */
package org.knime.core.data.blob;

import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.renderer.MultiLineStringValueRenderer;

/** Displays a hex dump for binary objects. For instance:
 * <pre>
 * 00000000 1F 8B 08 00 00 00 00 00 00 03 EC BD 6B 73 DB B6 ............ks..
 * 00000010 D6 C7 FB BE 9F 42 2F CE 0B 69 B6 A9 10 77 70 E6 .....B/..i...wp.
 * 00000020 EC 3D 0F 45 CB 96 1C 5B 8A ED DC 9C 4C A7 93 3A .=.E...[....L..:
 * 00000030 D9 B6 1B 27 4E 93 F4 24 EE A7 3F 04 78 27 17 24 ...'N..$..?.x'.$
 * 00000040 12 40 D2 EC 56 7D DA 67 33 8C B4 08 80 17 11 BF .@..V}.g3.......
 * 00000050 F5 C7 7F 21 2A 58 18 A2 9F 46 A3 60 3D 4F AE DF ...!*X...F.`=O..
 * 00000060 BC 0B 10 46 12 85 A1 C0 21 DE FF E9 A7 11 41 23 ...F....!.....A#
 * 00000070 82 47 A3 70 A4 FE 09 5B FF 46 51 34 7A 8A C3 30 .G.p...[.FQ4z..0
 * ...
 * </pre>
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
final class BinaryObjectDataValueRenderer extends MultiLineStringValueRenderer {

    private final int m_length;

    /** Create new instance.
     * @param lengthToShow Number bytes to show.
     * @param description ...
     */
    BinaryObjectDataValueRenderer(final int lengthToShow, final String description) {
        super(description, true);
        if (lengthToShow <= 0) {
            throw new IllegalArgumentException("Size <= 0: " + lengthToShow);
        }
        m_length = lengthToShow;
    }

    /** {@inheritDoc} */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof BinaryObjectDataValue) {
            BinaryObjectDataValue bValue = (BinaryObjectDataValue)value;
            InputStream in = null;
            try {
                in = bValue.openInputStream();
                String s = BinaryObjectCellFactory.getHexDump(in, m_length);
                super.setValue(s);
            } catch (Exception e) {
                super.setText(String.format("<Unable to read bytes: \"%s\">", e.getMessage()));
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // ignore;
                    }
                }
            }
        } else {
            super.setValue("?");
        }
    }

}