/*
 * JtsGeometry.java
 * 
 * Wrapper for PostgreSQL JDBC driver to allow transparent reading and writing
 * of JTS geometries
 * 
 * (C) 2005 Markus Schaber, markus.schaber@logix-tt.com
 * 
 * (C) 2015 Phillip Ross, phillip.w.g.ross@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 */

package com.tongtu.jts.jdbc;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

/**
 * JTS Geometry SQL wrapper. Supports PostGIS 1.x (lwgeom hexwkb) for writing
 * and both PostGIS 0.x (EWKT) and 1.x (lwgeom hexwkb) for reading.
 * 
 * @author Markus Schaber
 */

public class JtsGeometry extends PGobject {
    /* JDK 1.5 Serialization */
    private static final long serialVersionUID = 0x100;

    Geometry geom;

    final static JtsBinaryParser bp = new JtsBinaryParser();

    final static JtsBinaryWriter bw = new JtsBinaryWriter();

    final static PrecisionModel prec = new PrecisionModel();

    final static CoordinateSequenceFactory csfac = PackedCoordinateSequenceFactory.DOUBLE_FACTORY;

    final static GeometryFactory geofac = new GeometryFactory(prec, 0, csfac);

    static final WKTReader reader = new WKTReader(geofac);

    /** Constructor called by JDBC drivers */
    public JtsGeometry() {
        setType("geometry");
    }

    public JtsGeometry(Geometry geom) {
        this();
        this.geom = geom;
    }

    public JtsGeometry(String value) throws SQLException {
        this();
        setValue(value);
    }

    public void setValue(String value) throws SQLException {
        geom = geomFromString(value);
    }

    public static Geometry geomFromString(String value) throws SQLException {
        try {
            value = value.trim();
            if (value.startsWith("00") || value.startsWith("01")) {
                return bp.parse(value);
            } else {
                Geometry result;
                // no srid := 0 in JTS world
                int srid = 0;
                // break up geometry into srid and wkt
                if (value.startsWith("SRID=")) {
                    String[] temp = value.split(";");
                    value = temp[1].trim();
                    srid = Integer.parseInt(temp[0].substring(5));
                }

                result = reader.read(value);
                setSridRecurse(result, srid);
                return result;
            }
        } catch (Exception E) {
            E.printStackTrace();
            throw new SQLException("Error parsing SQL data:" + E);
        }
    }


    /**
     * Recursively set a srid for the geometry and all subgeometries
     * @param geom Geometry to work on
     * @param srid SRID to be set to
     */
    public static void setSridRecurse(final Geometry geom, final int srid) {
        geom.setSRID(srid);
        if (geom instanceof GeometryCollection) {
            final int subcnt = geom.getNumGeometries();
            for (int i = 0; i < subcnt; i++) {
                setSridRecurse(geom.getGeometryN(i), srid);
            }
        } else if (geom instanceof Polygon) {
            Polygon poly = (Polygon) geom;
            poly.getExteriorRing().setSRID(srid);
            final int subcnt = poly.getNumInteriorRing();
            for (int i = 0; i < subcnt; i++) {
                poly.getInteriorRingN(i).setSRID(srid);
            }
        }
    }

    public Geometry getGeometry() {
        return geom;
    }

    public String toString() {
        return geom.toString();
    }

    public String getValue() {
        return bw.writeHexed(getGeometry());
    }

    public Object clone() {
        JtsGeometry obj = new JtsGeometry(geom);
        obj.setType(type);
        return obj;
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof JtsGeometry)) {
            Geometry other = ((JtsGeometry) obj).geom;
            if (this.geom == other) { // handles identity as well as both
                                        // ==null
                return true;
            } else if (this.geom != null && other != null) {
                return other.equals(this.geom);
            }
        }
        return false;
    }
}
