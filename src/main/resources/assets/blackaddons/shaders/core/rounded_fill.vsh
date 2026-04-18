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

out vec2 localPos;
out vec2 trueHalfSize;
out vec4 edgeFlags;
out float cornerRadius;
out vec4 vColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    localPos = UV0;
    
    trueHalfSize.x = float(UV2.x & 0x7FF) * 0.25;
    trueHalfSize.y = float(UV2.y & 0x7FF) * 0.25;
    
    int flags = (UV2.x >> 11) & 0xF;
    edgeFlags = vec4(
        float(flags & 1),
        float((flags >> 1) & 1),
        float((flags >> 2) & 1),
        float((flags >> 3) & 1)
    );
    
    cornerRadius = float((UV2.y >> 11) & 0x1F) * 0.5;
}
