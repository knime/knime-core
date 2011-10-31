/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Aug 18, 2011 (wiswedel): created
 */
package org.knime.base.node.viz.property.color.extract;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorHandler.ColorModel;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;

/**
 * NodeModel to Color Extractor.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColorExtractNodeModel extends NodeModel {

    /** Color Port Object in, Data Table out. */
    ColorExtractNodeModel() {
        super(new PortType[]{ColorHandlerPortObject.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec colorSpec = (DataTableSpec)inSpecs[0];
        if (colorSpec == null) {
            return null;
        }
        return new DataTableSpec[] {
                extractColorTable(colorSpec).getDataTableSpec()};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        ColorHandlerPortObject colorPO = (ColorHandlerPortObject)inObjects[0];
        DataTableSpec colorSpec = colorPO.getSpec();
        return new BufferedDataTable[] {exec.createBufferedDataTable(
                extractColorTable(colorSpec), exec)
        };
    }

    private DataTable extractColorTable(final DataTableSpec colorSpec)
        throws InvalidSettingsException {
        // first column has column handler (convention in ColorHandlerPO)
        ColorHandler clrHdl = colorSpec.getColumnSpec(0).getColorHandler();
        final ColorModel model = clrHdl.getColorModel();
        if (model.getClass() == ColorModelNominal.class) {
            ColorModelNominal nom = (ColorModelNominal) model;
            return extractColorTable(nom);
        } else if (model.getClass() == ColorModelRange.class) {
            ColorModelRange range = (ColorModelRange) model;
            return extractColorTable(range);
        } else {
            throw new InvalidSettingsException("Unknown ColorModel class: "
                    + model.getClass());
        }
    }



    /**
     * @param range
     * @return */
    private DataTable extractColorTable(final ColorModelRange range) {
        DataTableSpec spec = createSpec(DoubleCell.TYPE);
        DataContainer cnt = new DataContainer(spec);
        RowKey[] keys = new RowKey[] {new RowKey("min"), new RowKey("max")};
        Color[] clrs = new Color[] {range.getMinColor(), range.getMaxColor()};
        double[] vals = new double[] {range.getMinValue(), range.getMaxValue()};
        for (int i = 0; i < 2; i++) {
            Color clr = clrs[i];
            DataRow row = new DefaultRow(keys[i], new DoubleCell(vals[i]),
                    new IntCell(clr.getRed()), new IntCell(clr.getGreen()),
                    new IntCell(clr.getBlue()), new IntCell(clr.getAlpha()),
                    new IntCell(clr.getRGB()));
            cnt.addRowToTable(row);
        }
        cnt.close();
        return cnt.getTable();
    }

    /**
     * @param nom
     * @return
     * @throws InvalidSettingsException */
    private DataTable extractColorTable(final ColorModelNominal nom)
    throws InvalidSettingsException {
        DataType superType = null;
        for (DataCell c : nom) {
            if (superType == null) {
                superType = c.getType();
            } else {
                superType = DataType.getCommonSuperType(superType, c.getType());
            }
        }
        if (superType == null) {
            throw new InvalidSettingsException("No nominal values in model");
        }
        DataTableSpec spec = createSpec(superType);
        DataContainer cnt = new DataContainer(spec);
        int counter = 0;
        for (DataCell c : nom) {
            Color clr = nom.getColorAttr(c).getColor();
            DataRow row = new DefaultRow(RowKey.createRowKey(counter++),
                    c, new IntCell(clr.getRed()), new IntCell(clr.getGreen()),
                    new IntCell(clr.getBlue()), new IntCell(clr.getAlpha()),
                    new IntCell(clr.getRGB()));
            cnt.addRowToTable(row);
        }
        cnt.close();
        return cnt.getTable();
    }

    /**
     * @param valueType
     * @return */
    private DataTableSpec createSpec(final DataType valueType) {
        DataTableSpec spec = new DataTableSpec(new DataColumnSpec[] {
                new DataColumnSpecCreator("value", valueType).createSpec(),
                new DataColumnSpecCreator("R", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("G", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("B", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("A", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("RGBA", IntCell.TYPE).createSpec(),
        });
        return spec;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

}
