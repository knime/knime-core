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
 * -------------------------------------------------------------------
 *
 * History
 *   02.06.2006 (gabriel): created
 */
package org.knime.core.data.property;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;


/**
 * Color model which maps a set of <code>DataCell</code> objects to
 * <code>Color</code>.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author Thomas Gabriel, University of Konstanz, Germany
 */
public final class ColorModelNominal implements ColorModel {

    /** Maps DataCell values to ColorAttr. */
    private final Map<DataCell, ColorAttr> m_map;

    private final ColorAttr[] m_paletteColors;

    /**
     * Creates new ColorHandler based on a mapping.
     * @param map Mapping form DataCell values to ColorAttr objects.
     * @param palette The palette passed on to a applier node to assign color to new unseen values (or null)
     * @throws IllegalArgumentException If the map is null.
     * @since 5.1
     */
    public ColorModelNominal(final Map<DataCell, ColorAttr> map, final ColorAttr[] palette) {
        if (map == null)  {
            throw new IllegalArgumentException("Mapping must not be null.");
        }
        m_map = Collections.unmodifiableMap(map);
        m_paletteColors = Objects.requireNonNullElseGet(palette, () -> new ColorAttr[] {ColorAttr.DEFAULT});
    }

    /**
     * Returns a ColorAttr for the given DataCell value, or
     * <code>ColorAttr.DEFAULT</code> if not set.
     * @param dc A DataCell value to get color for.
     * @return A ColorAttr for a DataCell value.
     */
    @Override
    public ColorAttr getColorAttr(final DataCell dc) {
        ColorAttr color = m_map.get(dc);
        if (color == null) {
            return ColorAttr.DEFAULT;
        }
        return color;
    }

    private static final String CFG_KEYS = "keys";
    private static final String CFG_PALETTE = "palette";

    /**
     * Saves the <code>DataCell</code> to <code>Color</code> mapping to the
     * given <code>Config</code>. The color is split into red, green, blue, and
     * alpha component which are stored as int array.
     * @param config Save settings to.
     * @see org.knime.core.data.property.ColorModel
     *      #save(ConfigWO)
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    @Override
    public void save(final ConfigWO config) {
        ConfigWO keyConfig = config.addConfig(CFG_KEYS);
        for (Map.Entry<DataCell, ColorAttr> e : m_map.entrySet()) {
            DataCell key = e.getKey();
            keyConfig.addDataCell(key.toString(), key);
            Color color = e.getValue().getColor();
            config.addInt(key.toString(), color.getRGB());
        }
        config.addIntArray(CFG_PALETTE,
            Stream.of(m_paletteColors).map(ColorAttr::getColor).mapToInt(Color::getRGB).toArray());
    }

    /**
     * Read color settings from given <code>Config</code> and returns a new
     * <code>ColorModelNominal</code> object.
     * @param config Reads color model from.
     * @return A new <code>ColorModelNominal</code> object.
     * @throws InvalidSettingsException If the color model settings could not
     *         be read.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public static ColorModelNominal load(final ConfigRO config)
            throws InvalidSettingsException {
        Map<DataCell, ColorAttr> map = new LinkedHashMap<>();
        ConfigRO keyConfig = config.getConfig(CFG_KEYS);
        for (String key : keyConfig.keySet()) {
            Color color;
            try {
                // load color components before 2.0
                int[] v = config.getIntArray(key.toString());
                color = new Color(v[0], v[1], v[2], v[3]);
            } catch (InvalidSettingsException ise) {
                color = new Color(config.getInt(key.toString()), true);
            }
            DataCell cell = keyConfig.getDataCell(key);
            map.put(cell, ColorAttr.getInstance(color));
        }
        int[] paletteInts = config.getIntArray(CFG_PALETTE, ColorAttr.DEFAULT.getColor().getRGB());
        ColorAttr[] palette = IntStream.of(paletteInts).mapToObj(i -> new Color(i, true)).map(ColorAttr::getInstance)
            .toArray(ColorAttr[]::new);
        return new ColorModelNominal(map, palette);
    }

    /**
     * @return <i>Nominal ColorModel</i>
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Nominal ColorModel";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ColorModelNominal)) {
            return false;
        }
        ColorModelNominal cmodel = (ColorModelNominal) obj;
        return m_map.equals(cmodel.m_map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_map.hashCode();
    }

    /**
     * The values known by this model.
     *
     * @return That read-only collection
     * @since 5.1
     */
    public Iterable<DataCell> getValues() {
        return Collections.unmodifiableCollection(m_map.keySet());
    }

    /**
     * This model represented by a map of color hex code to data values, e.g.
     * <pre>
     * #194fab -> "Foo", "Bar"
     * #88ff00 -> "Baz"
     * </pre>
     *
     * @return That (new) map.
     * @since 5.1
     */
    public Map<String, List<String>> getColorToValueMap() {
        final var result = new LinkedHashMap<String, List<String>>();
        for (var entry : m_map.entrySet()) {
            final var color = entry.getValue().getColor();
            final var colorS = ColorModel.colorToHexString(color);
            result.computeIfAbsent(colorS, c -> new ArrayList<>()).add(entry.getKey().toString());
        }
        return result;
    }

    /**
     * Used by the Color Appender node to apply the palette to new values seen in the "test" data.
     *
     * @param newValues Values in the input of the appender, possibly contains values already defined.
     * @return A new model for the old and new values.
     * @since 5.1
     */
    public ColorModelNominal applyToNewValues(final Iterable<DataCell> newValues) {
        // the intention of the code below....
        // assume orignal model contains this mapping:
        //   A - Color1
        //   B - Color2
        //   C - Color3
        // and this palette:
        //   Color1, Color2, Color3, Color4, Color5
        //
        // the values in the argument list (colors to apply to) - note 'B' is missing:
        //   A, C, D, E, F, G
        // then the final assignment is:
        //   A - Color1
        //   C - Color3
        //   D - Color4
        //   E - Color5
        //   F - Color1 (start over - palette apply from scratch)
        //   G - Color2
        // (Color2 is reserved as it's used for 'B' in the original data, though not present in the data now)

        Map<DataCell, ColorAttr> map = new LinkedHashMap<>(m_map);
        final var paletteList = new ArrayList<ColorAttr>(Arrays.asList(m_paletteColors));
        paletteList.removeAll(map.values());
        for (var cell : newValues) {
            if (paletteList.isEmpty()) {
                paletteList.addAll(Arrays.asList(m_paletteColors));
            }
            final var assignedColor = map.get(cell);
            if (assignedColor != null) {
                paletteList.remove(assignedColor);
            } else {
                map.put(cell, paletteList.remove(0));
            }
        }
        return new ColorModelNominal(map, m_paletteColors);
    }
}
