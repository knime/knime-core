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
 *   15.12.2014 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dmg.pmml.ApplyDocument.Apply;
import org.dmg.pmml.ConstantDocument.Constant;
import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.ExtensionDocument.Extension;
import org.dmg.pmml.FieldRefDocument.FieldRef;
import org.knime.base.data.statistics.Statistic;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * The base class for all missing cell handlers.
 * It is created by a MissingCellHandlerFactory and used for a single column.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class MissingCellHandler {

    private static NodeLogger LOGGER = NodeLogger.getLogger(MissingCellHandler.class);

    /**
     * Name of the pmml extension that holds the name of a custom missing value handler.
     */
    public static final String CUSTOM_HANDLER_EXTENSION_NAME = "customMissingValueHandler";

    // Some constants for PMML
    private static final String IS_MISSING_FUNCTION_NAME = "isMissing";

    private static final String IF_FUNCTION_NAME = "if";

    private DataColumnSpec m_col;

    /**
     * Constructor for the missing cell handler.
     * @param col the column this handler is used for
     */
    public MissingCellHandler(final DataColumnSpec col) {
        m_col = col;
    }

    /**
     * @return the name of the column this handler is used for.
     */
    public DataColumnSpec getColumnSpec() {
        return m_col;
    }

    /**
     * @return Any warning messages that occured during execution.
     */
    public String getWarningMessage() {
        return null;
    }

    /**
     * Loads user settings.
     * @param settings the settings
     * @throws InvalidSettingsException when the settings cannot be loaded
     */
    public abstract void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves user settings.
     * @param settings the settings
     */
    public abstract void saveSettingsTo(NodeSettingsWO settings);

    /**
     * Returns a Knime statistic to be evaluated before missing values are replaced.
     * Can return null to indicate that no statistics are needed.
     * @return a Knime statistic object
     */
    public abstract Statistic getStatistic();

    /**
     * Calculates the replacement value for a cell.
     * If the returned value is null, the row this cell in is removed from the output table.
     * @param key the row key of the row where the cell should be replaced.
     * @param window the window over the previous and next cells as defined by
     * {@link #getPreviousCellsWindowSize() getPreviousCellsWindowSize}
     * and {@link #getNextCellsWindowSize() getNextCellsWindowSize}.
     * @return a data cell with the replacement value or null if the row should be removed completely
     */
    public abstract DataCell getCell(RowKey key, DataColumnWindow window);

    /**
     * Is called when the iterator over the table passes over a row that has a
     * non-missing cell in this handler's column.
     * @param key the key of the row
     * @param window the sliding window over the column
     */
    public void nonMissingValueSeen(final RowKey key, final DataColumnWindow window) {
    }

    /**
     * Is called when the current value is missing but the current row is skipped because a previous column had a
     * handler that requested to remove the row. When a handler requests to remove a row after
     * this handler's column has been treated (getCell has been called already) then this method is not called.
     * This method should be used to advance iterators for statistics etc.
     * @param key the key of the row that is removed
     */
    public void rowRemoved(final RowKey key) {
    }

    /**
     * Creates a derived field for the documentation of the operation in PMML.
     * @return the derived field
     */
    public abstract DerivedField getPMMLDerivedField();

    /**
     * @return the number of cells to be remembered as lookbehind.
     */
    public abstract int getPreviousCellsWindowSize();

    /**
     * @return the number of cells to be read ahead as lookahead.
     */
    public abstract int getNextCellsWindowSize();

    /**
     * @return The type of the column with the replaced values.
     * If this value is null, the handler returns the data type of the input column.
     */
    public DataType getOutputDataType() {
        return m_col.getType();
    }

    /**
     * Helper method for creating a derived field that replaces a field's value with a fixed value.
     * @param dataType the data type of the field.
     * @param value the replacement value for the field
     * @return the derived field
     */
    protected DerivedField createValueReplacingDerivedField(final DATATYPE.Enum dataType, final String value) {
        DerivedField field = DerivedField.Factory.newInstance();
        if (dataType == org.dmg.pmml.DATATYPE.STRING || dataType == org.dmg.pmml.DATATYPE.BOOLEAN) {
            field.setOptype(org.dmg.pmml.OPTYPE.CATEGORICAL);
        } else {
            field.setOptype(org.dmg.pmml.OPTYPE.CONTINUOUS);
        }

        /*
         * Create the PMML equivalent of: "if fieldVal is missing then x else fieldVal"
         * <Apply function="if">
         *    <Apply function="isMissing">
         *        <FieldRef field="fieldVal"/>
         *    </Apply>
         *    <Constant dataType="___" value="x"/>
         *    <FieldRef field="fieldVal"/>
         * </Apply>
         */
        Apply ifApply = field.addNewApply();
        ifApply.setFunction(IF_FUNCTION_NAME);
        Apply isMissingApply = Apply.Factory.newInstance();
        FieldRef fieldRef = FieldRef.Factory.newInstance();
        fieldRef.setField(m_col.getName());
        isMissingApply.setFieldRefArray(new FieldRef[]{fieldRef});
        isMissingApply.setFunction(IS_MISSING_FUNCTION_NAME);
        ifApply.setApplyArray(new Apply[]{isMissingApply});
        Constant replacement = Constant.Factory.newInstance();
        replacement.setDataType(dataType);
        replacement.setStringValue(value);
        ifApply.setConstantArray(new Constant[] {replacement});
        ifApply.setFieldRefArray(new FieldRef[] {fieldRef});
        field.setDataType(dataType);
        field.setName(m_col.getName());
        field.setDisplayName(m_col.getName());
        return field;
    }

    /**
     * Creates a derived field that contains an extension which
     * contains the name of the factory to use for the replacement.
     * The result may be adjusted to contain necessary information for the handler.
     * @param dataType the data type of the derived field
     * @param factoryID the id of the factory
     * @return the derived field
     */
    protected DerivedField createExtensionDerivedField(final DATATYPE.Enum dataType, final String factoryID) {
        DerivedField field = DerivedField.Factory.newInstance();
        if (dataType == org.dmg.pmml.DATATYPE.STRING || dataType == org.dmg.pmml.DATATYPE.BOOLEAN) {
            field.setOptype(org.dmg.pmml.OPTYPE.CATEGORICAL);
        } else {
            field.setOptype(org.dmg.pmml.OPTYPE.CONTINUOUS);
        }
        Extension e = field.addNewExtension();
        e.setName(CUSTOM_HANDLER_EXTENSION_NAME);
        e.setValue(factoryID);
        field.setDataType(dataType);
        field.setName(m_col.getName());
        field.setDisplayName(m_col.getName());

        // Insert settings
        NodeSettings nodeSettings = new NodeSettings("");
        saveSettingsTo(nodeSettings);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            nodeSettings.saveToXML(baos);
            Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            Node copy = e.getDomNode().getOwnerDocument().importNode(doc.getFirstChild(), true);
            e.getDomNode().appendChild(copy);
        } catch (Exception ex) {
            LOGGER.error("An error occurred while writing settings to PMML.\n" + ex.getMessage());
            return null;
        }
        return field;
    }

    /**
     * Maps the columns data type to a PMML data type.
     * @return The PMML data type for this handler's column
     */
    protected DATATYPE.Enum getPMMLDataTypeForColumn() {
        if (m_col.getType().equals(DoubleCell.TYPE)) {
            return org.dmg.pmml.DATATYPE.DOUBLE;
        } else if (m_col.getType().equals(BooleanCell.TYPE)) {
            return org.dmg.pmml.DATATYPE.BOOLEAN;
        } else if (m_col.getType().equals(IntCell.TYPE) || m_col.getType().equals(LongCell.TYPE)) {
            return org.dmg.pmml.DATATYPE.INTEGER;
        } else if (m_col.getType().equals(DateAndTimeCell.TYPE)) {
            return org.dmg.pmml.DATATYPE.DATE_TIME;
        } else {
            return org.dmg.pmml.DATATYPE.STRING;
        }
    }

    /**
     * Creates a missing cell handler from an extension that is inside of a PMML derived field.
     * @param column the column this handler is used for
     * @param manager missing value factory manager
     * @param ext the extension containing the necessary information
     * @return a missing cell handler that was initialized from the extension
     * @throws InvalidSettingsException if the the factory from the extension is not applicable for the column
     */
    protected static MissingCellHandler fromPMMLExtension(final DataColumnSpec column,
        final MissingCellHandlerFactoryManager manager, final Extension ext) throws InvalidSettingsException {

        String factoryID = ext.getValue();
        MissingCellHandlerFactory fac = manager.getFactoryByID(factoryID);
        if (fac == null) {
            LOGGER.error("Unknown missing cell handler " + factoryID + ".");
            final String parts[] = factoryID.split("\\.");
            throw new InvalidSettingsException("Unknown missing cell handler " + parts[parts.length - 1] + ".");
        }

        if (!fac.isApplicable(column.getType())) {
            throw new InvalidSettingsException("Missing cell handler " + fac.getDisplayName()
                + " is not applicable for columns of type " + column.getType().toString() + ".");
        }

        // Create document from empty node settings
        NodeSettings nodeSettings = new NodeSettings("");
        MissingCellHandler handler = fac.createHandler(column);

        // Without a panel, the handler has no settings and we can return it
        if (!fac.hasSettingsPanel()) {
            return handler;
        }
        // Load settings from the panel
        try {
            // Create an XML document from empty settings
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            nodeSettings.saveToXML(baos);
            Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            // Remove original settings
            doc.removeChild(doc.getFirstChild());
            // And plug in those from the PMML
            doc.appendChild(doc.importNode(ext.getDomNode().getFirstChild(), true));

            // Now write it to a stream and create new node settings
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(bos);
            transformer.transform(source, result);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            NodeSettingsRO settings = NodeSettings.loadFromXML(bis);
            handler.loadSettingsFrom(settings);
        } catch (Exception ex) {
            LOGGER.error("An error occurred while loading settings for a MissingCellHandler from PMML.\n"
                            + ex.getMessage());
        }
        return handler;
    }

    /**
     * Creates a missing cell handler from an extension that is inside of a PMML derived field.
     * @param column the column this handler is used for
     * @param ext the extension containing the necessary information
     * @return a missing cell handler that was initialized from the extension
     * @throws InvalidSettingsException if the the factory from the extension is not applicable for the column
     */
    public static MissingCellHandler fromPMMLExtension(final DataColumnSpec column, final Extension ext)
                                                                throws InvalidSettingsException {

        final MissingCellHandlerFactoryManager manager = MissingCellHandlerFactoryManager.getInstance();
        return fromPMMLExtension(column, manager, ext);
    }
}
