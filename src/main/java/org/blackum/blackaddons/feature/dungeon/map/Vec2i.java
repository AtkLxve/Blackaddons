package org.blackum.blackaddons.feature.dungeon.map;

import java.util.Objects;

public class Vec2i {
    public final int x;
    public final int z;

    public Vec2i(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public Vec2i add(Vec2i other) {
        return new Vec2i(this.x + other.x, this.z + other.z);
    }

    public Vec2i add(int x, int z) {
        return new Vec2i(this.x + x, this.z + z);
    }

    public Vec2i multiply(int n) {
        return new Vec2i(this.x * n, this.z * n);
    }

    public Vec2i multiply(double n) {
        return new Vec2i((int)(this.x * n), (int)(this.z * n));
    }

    public Vec2i divide(int n) {
        return new Vec2i(this.x / n, this.z / n);
    }

    public Vec2i divide(double n) {
        return new Vec2i((int)(this.x / n), (int)(this.z / n));
    }

    public int mapIndex() {
        return this.z * 128 + this.x;
    }

    public int roomListIndex() {
        return this.x * 6 + this.z;
    }

    public int index() {
        return (this.x + 201) / 32 * 6 + (this.z + 201) / 32;
    }

    public Vec2i roomTilePos() {
        return this.add(201, 201).divide(32);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec2i)) return false;
        Vec2i v = (Vec2i) o;
        return this.x == v.x && this.z == v.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "Vec2i(" + x + ", " + z + ")";
    }
}
