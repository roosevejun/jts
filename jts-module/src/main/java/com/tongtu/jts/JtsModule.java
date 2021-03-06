package com.tongtu.jts;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tongtu.jts.parsers.*;
import com.tongtu.jts.serialization.GeometryDeserializer;
import com.tongtu.jts.serialization.GeometrySerializer;
import org.locationtech.jts.geom.*;

public class JtsModule extends SimpleModule {

    public JtsModule() {
        this(new GeometryFactory());
    }
    
    public JtsModule(GeometryFactory geometryFactory) {
        super("JtsModule", new Version(1, 0, 0, null,"com.bedatadriven","jackson-datatype-jts"));

        addSerializer(Geometry.class, new GeometrySerializer());
        GenericGeometryParser genericGeometryParser = new GenericGeometryParser(geometryFactory);
        addDeserializer(Geometry.class, new GeometryDeserializer<Geometry>(genericGeometryParser));
        addDeserializer(Point.class, new GeometryDeserializer<Point>(new PointParser(geometryFactory)));
        addDeserializer(MultiPoint.class, new GeometryDeserializer<MultiPoint>(new MultiPointParser(geometryFactory)));
        addDeserializer(LineString.class, new GeometryDeserializer<LineString>(new LineStringParser(geometryFactory)));
        addDeserializer(MultiLineString.class, new GeometryDeserializer<MultiLineString>(new MultiLineStringParser(geometryFactory)));
        addDeserializer(Polygon.class, new GeometryDeserializer<Polygon>(new PolygonParser(geometryFactory)));
        addDeserializer(MultiPolygon.class, new GeometryDeserializer<MultiPolygon>(new MultiPolygonParser(geometryFactory)));
        addDeserializer(GeometryCollection.class, new GeometryDeserializer<GeometryCollection>(new GeometryCollectionParser(geometryFactory, genericGeometryParser)));
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
    }
}
