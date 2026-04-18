#version 330

in vec2 localPos;
in vec2 trueHalfSize;
in vec4 edgeFlags;
in float cornerRadius;
in vec4 vColor;

out vec4 fragColor;

float roundedRectSdf(vec2 p, vec2 extents, float r) {
    vec2 d = abs(p) - extents + vec2(r);
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    vec2 p = localPos;
    vec2 ext = trueHalfSize;
    float extendAmt = 10.0;
    
    if (edgeFlags.x < 0.5) {
        ext.y += extendAmt * 0.5;
        p.y += extendAmt * 0.5;
    }
    if (edgeFlags.y < 0.5) {
        ext.x += extendAmt * 0.5;
        p.x -= extendAmt * 0.5;
    }
    if (edgeFlags.z < 0.5) {
        ext.y += extendAmt * 0.5;
        p.y -= extendAmt * 0.5;
    }
    if (edgeFlags.w < 0.5) {
        ext.x += extendAmt * 0.5;
        p.x += extendAmt * 0.5;
    }
    
    float dist = roundedRectSdf(p, ext, cornerRadius);
    float alpha = clamp(0.5 - dist, 0.0, 1.0);
    
    if (alpha <= 0.0) discard;
    fragColor = vec4(vColor.rgb, vColor.a * alpha);
}
