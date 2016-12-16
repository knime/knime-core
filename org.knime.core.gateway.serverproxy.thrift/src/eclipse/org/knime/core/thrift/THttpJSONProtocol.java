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
 *   Dec 16, 2016 (hornm): created
 */
package org.knime.core.thrift;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class THttpJSONProtocol extends TJSONProtocol {

    private StringBuffer lineBuffer_ = new StringBuffer();

    /**
     * @param trans
     */
    public THttpJSONProtocol(final TTransport trans) {
        super(trans);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TMessage readMessageBegin() throws TException {
        //skip http header
        byte[] buf = new byte[1];
        String line = " ";
        while((line = readLine()).length() > 2) {
            System.out.print(line);
        }
        //read few more bytes
        trans_.read(buf, 0, 1);
        return super.readMessageBegin();
    }

    private String readLine() throws TTransportException {
        byte[] buf = new byte[1];
        lineBuffer_.setLength(0);
        while(buf[0] != 13) {
            trans_.read(buf, 0, 1);
            lineBuffer_.append((char) buf[0]);
        }
        return lineBuffer_.toString();
    }

    public static class Factory extends TJSONProtocol.Factory {

        /**
         * {@inheritDoc}
         */
        @Override
        public TProtocol getProtocol(final TTransport trans) {
            return new THttpJSONProtocol(trans);
        }

    }



}
