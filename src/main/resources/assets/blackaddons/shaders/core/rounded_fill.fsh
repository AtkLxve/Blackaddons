#version 330

in vec2 localPos;
in vec2 trueHalfSize;
in float cornerRadius;
in vec4 vColor;

out vec4 fragColor;

float roundedRectSdf(vec2 p, vec2 extents, float r) {
    vec2 d = abs(p) - extents + vec2(r);
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    float dist = roundedRectSdf(localPos, trueHalfSize, cornerRadius);

    if (dist > 0.0) discard;
    fragColor = vColor;
}
