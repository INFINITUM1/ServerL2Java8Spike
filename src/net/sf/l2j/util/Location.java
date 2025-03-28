package net.sf.l2j.util;

import java.io.Serializable;

import net.sf.l2j.gameserver.model.L2Object;

@SuppressWarnings("serial")
public class Location implements Serializable {

    public int x;
    public int y;
    public int z;
    /**
     * Heading
     */
    public int h = 0;

    /**
     * Позиция (x, y, z)
     */
    public Location() {
    }

    public Location(int locX, int locY, int locZ) {
        x = locX;
        y = locY;
        z = locZ;
    }

    /**
     * Позиция (x, y, z, heading)
     */
    public Location(int locX, int locY, int locZ, int heading) {
        x = locX;
        y = locY;
        z = locZ;
        h = heading;
    }

    public Location(L2Object obj) {
        x = obj.getX();
        y = obj.getY();
        z = obj.getZ();
        h = obj.getHeading();
    }

    public boolean equals(Location loc) {
        return loc.x == x && loc.y == y && loc.z == z;
    }

    public Location changeZ(int zDiff) {
        z += zDiff;
        return this;
    }

    public Location setX(int x) {
        this.x = x;
        return this;
    }

    public Location setY(int y) {
        this.y = y;
        return this;
    }

    public Location setZ(int z) {
        this.z = z;
        return this;
    }

    public Location setH(int h) {
        this.h = h;
        return this;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getHeading() {
        return h;
    }

    @Override
    public final String toString() {
        return "Coords(" + x + "," + y + "," + z + "," + h + ")";
    }
}
