/*
 * ------------------------------------------------------------------------
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
 * Created on 25.11.2013 by NanoTec
 */
package org.knime.base.node.preproc.constantvalue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.NodeLogger;
import org.xml.sax.SAXException;

/**
 * A TypeCellFactory creates for a string and an optional additional parameter a new cell.
 *
 * @author Marcel Hanser
 */
enum TypeCellFactory {
    /**
     * Creates a {@link LongCell} for the given String.
     */
    LONG(LongCell.TYPE) {
        @Override
        public DataCell innerCreateCell(final String toConvert, final String otherArgument) //
            throws TypeParsingException {
            return new LongCell(Long.valueOf(toConvert));
        }
    },
    /**
     * Creates a {@link IntCell} from the given String.
     */
    INT(IntCell.TYPE) {
        @Override
        public DataCell innerCreateCell(final String toConvert, final String otherArgument) {
            return new IntCell(Integer.valueOf(toConvert));
        }
    },
    /**
     * Creates a {@link DoubleCell} from the given String.
     */
    DOUBLE(DoubleCell.TYPE) {
        @Override
        public DataCell innerCreateCell(final String toConvert, final String otherArgument) {
            return new DoubleCell(Double.valueOf(toConvert));
        }
    },
    /**
     * Creates a {@link XMLCell} from the given String.
     */
    XML(XMLCell.TYPE) {
        @Override
        public DataCell innerCreateCell(final String toConvert, final String otherArgument) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException {
            return XMLCellFactory.create(toConvert);
        }
    },
    /**
     * Creates a {@link StringCell} from the given String.
     */
    STRING(StringCell.TYPE) {
        @Override
        public DataCell innerCreateCell(final String toConvert, final String otherArgument) {
            return new StringCell(toConvert.toString());
        }
    },
    /**
     * Creates a {@link DateAndTimeCell} from the given String and the additional SimpleDateFormat conform pattern.
     */
    DATE(DateAndTimeCell.TYPE) {
        @Override
        public DataCell innerCreateCell(final String toConvert, final String otherArgument) throws Exception {

            Date parsed = new SimpleDateFormat(otherArgument).parse(toConvert);
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsed);
            return new DateAndTimeCell(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }
    },
    /**
     * Creates a {@link BooleanCell} from the given String.
     */
    BOOLEAN(BooleanCell.TYPE) {
        @Override
        public DataCell innerCreateCell(final String toConvert, final String otherArgument) {
            String lowerCase = toConvert.toLowerCase();
            if (!("false".equals(lowerCase) || "true".equals(lowerCase))) {
                throw new IllegalArgumentException();
            }
            return BooleanCell.get(Boolean.valueOf(lowerCase));
        }
    };

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TypeCellFactory.class);

    private final DataType m_dataType;

    private TypeCellFactory(final DataType dataType) {
        this.m_dataType = dataType;
    }

    /**
     * @return the dataType
     */
    public DataType getDataType() {
        return m_dataType;
    }

    /**
     * Should not be called. Call {@link #createCell(String, String)} instead.
     *
     * @param toConvert the string to convert
     * @param pattern an additional argument, semantic definition is done by the concrete {@link TypeCellFactory}
     * @return the data cell
     * @throws Exception if the toConvert string is not parseable to the DataCell to create
     */
    abstract DataCell innerCreateCell(final String toConvert, String pattern) throws Exception;

    /**
     * Creates a cell from the given String and the additional argument, which is sometimes necessary (e.g.for creating
     * a DataAndTimeCell).
     *
     * @param toConvert the string to convert
     * @param otherArgument an additional argument, semantic definition is done by the concrete {@link TypeCellFactory}
     * @return a new data cell from the given value
     * @throws TypeParsingException if an error occured during convertion
     */
    final DataCell createCell(final String toConvert, final String otherArgument) throws TypeParsingException {
        try {
            return innerCreateCell(toConvert, otherArgument);
        } catch (Exception e) {
            LOGGER.info("error on parsing value: " + toConvert, e);
            throw new TypeParsingException(e);
        }
    }

    /**
     * @param dataType the data type top receive the {@link TypeCellFactory} for
     * @return the the {@link TypeCellFactory} responsible for creating {@link DataCell}s of the given {@link DataType}
     */
    static final TypeCellFactory forDataType(final DataType dataType) {
        for (TypeCellFactory factory : values()) {
            if (factory.getDataType().equals(dataType)) {
                return factory;
            }
        }
        throw new IllegalArgumentException("no factory found for type: " + dataType);
    }
}
