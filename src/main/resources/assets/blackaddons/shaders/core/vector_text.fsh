#version 330

in vec2 texCoord;
in vec4 vColor;

uniform sampler2D Sampler0;

out vec4 fragColor;

float unpackFloat(int coordIndex, int curveIndex) {
    vec4 tex = texelFetch(Sampler0, ivec2(coordIndex, curveIndex), 0);
    uint bits = uint(round(tex.r * 255.0)) |
                (uint(round(tex.g * 255.0)) << 8u) |
                (uint(round(tex.b * 255.0)) << 16u) |
                (uint(round(tex.a * 255.0)) << 24u);
    return uintBitsToFloat(bits);
}

int windingAt(vec2 p, int curveCount) {
    int w = 0;
    for (int i = 1; i <= curveCount; i++) {
        vec2 p0 = vec2(unpackFloat(0, i), unpackFloat(1, i));
        vec2 p1 = vec2(unpackFloat(2, i), unpackFloat(3, i));
        vec2 p2 = vec2(unpackFloat(4, i), unpackFloat(5, i));

        if ((p0.y <= p.y && p.y < p2.y) || (p2.y <= p.y && p.y < p0.y)) {
            float a = p0.y - 2.0 * p1.y + p2.y;
            float b = 2.0 * (p1.y - p0.y);
            float c = p0.y - p.y;
            float t = -1.0;
            if (abs(a) < 0.00001) {
                if (abs(b) > 0.00001) t = -c / b;
            } else {
                float disc = b * b - 4.0 * a * c;
                if (disc >= 0.0) {
                    float sq = sqrt(disc);
                    float t1 = (-b + sq) / (2.0 * a);
                    float t2 = (-b - sq) / (2.0 * a);
                    if (t1 >= 0.0 && t1 <= 1.0) t = t1;
                    else if (t2 >= 0.0 && t2 <= 1.0) t = t2;
                }
            }
            if (t >= 0.0 && t <= 1.0) {
                float rx = (1.0-t)*(1.0-t)*p0.x + 2.0*t*(1.0-t)*p1.x + t*t*p2.x;
                if (rx > p.x) {
                    if (p0.y < p2.y) w++; else w--;
                }
            }
        }
    }
    return w;
}

void main() {
    float xMin = unpackFloat(0, 0);
    float yMin = unpackFloat(1, 0);
    float xMax = unpackFloat(2, 0);
    float yMax = unpackFloat(3, 0);
    int curveCount = int(unpackFloat(4, 0));

    vec2 dx = dFdx(texCoord) * 0.25;
    vec2 dy = dFdy(texCoord) * 0.25;

    vec2 tc0 = texCoord + dx + dy;
    vec2 tc1 = texCoord - dx + dy;
    vec2 tc2 = texCoord + dx - dy;
    vec2 tc3 = texCoord - dx - dy;

    float coverage = 0.0;
    coverage += float(windingAt(vec2(mix(xMin, xMax, tc0.x), mix(yMax, yMin, tc0.y)), curveCount) != 0);
    coverage += float(windingAt(vec2(mix(xMin, xMax, tc1.x), mix(yMax, yMin, tc1.y)), curveCount) != 0);
    coverage += float(windingAt(vec2(mix(xMin, xMax, tc2.x), mix(yMax, yMin, tc2.y)), curveCount) != 0);
    coverage += float(windingAt(vec2(mix(xMin, xMax, tc3.x), mix(yMax, yMin, tc3.y)), curveCount) != 0);
    coverage /= 4.0;

    if (coverage == 0.0) discard;
    fragColor = vec4(vColor.rgb, vColor.a * coverage);
}
