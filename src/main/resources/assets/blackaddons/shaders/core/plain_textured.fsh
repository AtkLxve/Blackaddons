#version 330

in vec2 texCoord;
in vec4 vColor;

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    vec4 col = texture(Sampler0, texCoord);
    if (col.a < 0.1) {
        discard;
    }
    fragColor = col * vColor;
}
