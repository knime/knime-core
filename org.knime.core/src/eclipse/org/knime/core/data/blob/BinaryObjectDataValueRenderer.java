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
import java.util.Random;

import org.apache.commons.io.HexDump;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 *
 * @author wiswedel
 */
final class BinaryObjectDataValueRenderer extends DefaultDataValueRenderer {

    private final int m_length;

    /**
     *
     */
    BinaryObjectDataValueRenderer(final int lengthToShow) {
        m_length = lengthToShow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Binary Content (" + (m_length < 0 ? "long)" : "short)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof BinaryObjectDataValue) {
            BinaryObjectDataValue bValue = (BinaryObjectDataValue)value;
            int length = m_length;
            if (length < 0) { // show all?
                length = (int)Math.min(bValue.length(), Integer.MAX_VALUE);
            }
            byte[] bs = new byte[length];
            int l = bs.length;
            int i = 0;
            InputStream in = null;
            try {
                in = bValue.getInputStream();
                while (i < l) {
                    i += in.read(bs, i, l - i);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream(bs.length);
                HexDump.dump(bs, 0, out, 0);
                String s = new String(out.toByteArray());
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

    public static void main(final String[] args) {
        BinaryObjectCellFactory fac = new BinaryObjectCellFactory(null);
        Random r = new Random();
        byte[] bs = new byte[3*1024];
        r.nextBytes(bs);
        DataCell create;
        try {
            create = fac.create(bs);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        BinaryObjectDataValueRenderer rend = new BinaryObjectDataValueRenderer(-1);
        rend.setValue(create);
        System.out.println(rend.getText());

    }

}
