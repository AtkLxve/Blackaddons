#version 330

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out vec2 texCoord;
out vec4 vColor;
out float effectParam;

void main() {
    effectParam = intBitsToFloat((UV2.y << 16) | (UV2.x & 0xFFFF));
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, Position.z, 1.0);
    texCoord = UV0;
    vColor = Color;
}
