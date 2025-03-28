package net.sf.l2j.gameserver.model.entity;

import java.awt.Polygon;
import java.awt.geom.Rectangle2D;

public class EventTerritory {

    private int _id = 0;
    private int _minZ = 0;
    private int _maxZ = 0;
    private Polygon _p = null;
    private Rectangle2D _r = null;

    public EventTerritory(int id) {
        _id = id;
        _p = new Polygon();
    }

    public void addPoint(int x, int y) {
        _p.addPoint(x, y);
    }

    public void setZ(int min, int max) {
        _minZ = min;
        _maxZ = max;
        _r = _p.getBounds();
    }

    public boolean contains(int x, int y, int z) {
        if (z < _minZ || z > _maxZ) {
            return false;
        }

        return (_p.contains(x, y));
    }

    public boolean intersectsLine(int x, int y, int z, int tx, int ty, int tz) {
        /*if (z < _minZ || z > _maxZ)
        return false;*/

        if ((z < _minZ || z > _maxZ) && (tz < _minZ || tz > _maxZ)) {
            return false;
        }

        return (_r.intersectsLine(x, y, tx, ty));
    }
}