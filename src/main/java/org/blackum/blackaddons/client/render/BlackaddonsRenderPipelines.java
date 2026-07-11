package org.blackum.blackaddons.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.io.IOUtils;
import com.mojang.blaze3d.shaders.ShaderType;
import org.blackum.blackaddons.common.util.mc.McCompat;
import net.minecraft.resources.Identifier;
//? if >=26.2 {

/*import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.PrimitiveTopology;

*///?}

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BlackaddonsRenderPipelines {

    private static final List<RenderPipeline> PIPELINES = new ArrayList<>();

//? if >=26.2 {

    /*public static final RenderPipeline CUSTOM_TEXT = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "custom_text"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());

    public static final RenderPipeline CUSTOM_TEXT_DEPTH = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "custom_text_depth"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F))
            .build());

    public static final RenderPipeline VECTOR_TEXT = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "vector_text"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/vector_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/vector_text"))
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());

    public static final RenderPipeline ROUNDED_FILL = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "rounded_fill"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/rounded_fill"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/rounded_fill"))
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());

    public static final RenderPipeline PLAIN_TEXTURED = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "plain_textured"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/plain_textured"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/plain_textured"))
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());


*///?} else {
    public static final RenderPipeline CUSTOM_TEXT = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "custom_text"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());

    public static final RenderPipeline CUSTOM_TEXT_DEPTH = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "custom_text_depth"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -1.0F, -10.0F))
            .build());

    public static final RenderPipeline VECTOR_TEXT = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "vector_text"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/vector_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/vector_text"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());

    public static final RenderPipeline ROUNDED_FILL = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "rounded_fill"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/rounded_fill"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/rounded_fill"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());

    public static final RenderPipeline PLAIN_TEXTURED = add(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "plain_textured"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/plain_textured"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/plain_textured"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());
//?}

    private static RenderPipeline add(RenderPipeline pipeline) {
        PIPELINES.add(pipeline);
        return pipeline;
    }

    public static void precompile() {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();

        for (RenderPipeline pipeline : PIPELINES) {
            device.precompilePipeline(pipeline, (location, shaderType) -> {
                String extension = shaderType == ShaderType.VERTEX ? ".vsh" : ".fsh";
                String shaderPath = "shaders/" + location.getPath() + extension;
                Resource resource = McCompat.findResource(resources, location.getNamespace(), shaderPath)
                        .orElseThrow(() -> new RuntimeException("Could not find shader: " + location.getNamespace() + ":" + shaderPath));

                try (var inputStream = resource.open()) {
                    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load shader: " + location.getNamespace() + ":" + shaderPath, e);
                }
            });
        }
    }
}
