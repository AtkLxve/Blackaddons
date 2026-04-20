#version 330

in vec2 texCoord;
in vec4 vColor;
in float effectParam;

uniform sampler2D Sampler0;

out vec4 fragColor;

const float SDF_PX_RANGE = 255.0 / 12.0;

float screenPxRange() {
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 unitRange = vec2(SDF_PX_RANGE) / texSize;
    vec2 screenTexSize = vec2(1.0) / max(fwidth(texCoord), vec2(1e-6));
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    int raw = floatBitsToInt(effectParam);

    float effect = float(raw & 0xFFFF) - 4.0;
    float aa = (float(raw >> 16) - 1000.0) / 100.0;

    float pxRange = screenPxRange();
    float screenDistance = (texture(Sampler0, texCoord).r - 0.5) * pxRange;

    float maxDist = 0.5 * pxRange;
    float required = aa + abs(effect);
    float fitScale = required > maxDist ? maxDist / required : 1.0;
    float scaledAa = max(aa * fitScale, 0.001);
    float scaledEffect = effect * fitScale;

    float alpha;
    if (scaledEffect < 0.0) {
        float outer = smoothstep(-scaledAa, scaledAa, screenDistance + abs(scaledEffect));
        float inner = smoothstep(-scaledAa, scaledAa, screenDistance);
        alpha = outer - inner;
    } else {
        alpha = smoothstep(-scaledAa, scaledAa, screenDistance + scaledEffect);
    }

    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vColor.rgb, vColor.a * alpha);
}
