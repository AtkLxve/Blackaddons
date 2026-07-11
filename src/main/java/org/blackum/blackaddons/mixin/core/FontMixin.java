package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.feature.customname.CustomNameManager;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.render.font.CustomBakedGlyph;
import org.blackum.blackaddons.gui.render.font.CustomTexturedBakedGlyph;
import org.blackum.blackaddons.gui.render.font.EmojiManager;
import org.blackum.blackaddons.gui.render.font.CustomFontManager;
import org.blackum.blackaddons.gui.render.font.CustomFontRenderer;
import org.blackum.blackaddons.gui.render.font.VectorFontRenderer;
import org.blackum.blackaddons.gui.render.font.EmojiSequenceCharSequence;
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.blackum.blackaddons.common.util.mc.McCompat;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;

@Mixin(value = Font.class, priority = 10000)
public class FontMixin {
    @Shadow
    @Final
    private RandomSource random;

    private static int measureVisualWidth(CustomFontRenderer renderer, CustomFontManager mgr, String text, boolean boldOverride) {
        float scale = renderer.getCachedScale();
        float cursor = 0.0f;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp == 167) { // '§'
                i += Character.charCount(cp);
                if (i < text.length()) {
                    int nextCp = text.codePointAt(i);
                    i += Character.charCount(nextCp);
                }
                continue;
            }
            if (CustomFontRenderer.isVariationSelector(cp)) {
                i += Character.charCount(cp);
                continue;
            }
            int arrowDir = EmojiManager.getArrowDirection(cp);
            if (arrowDir != -1) {
                cursor += EmojiManager.getArrowAdvance();
                i += Character.charCount(cp);
                continue;
            }
            if (EmojiManager.isEmoji(cp)) {
                float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
                cursor += emojiSize + 1.0f;
                i += Character.charCount(cp);
                continue;
            }
            CustomFontManager.GlyphData data = mgr != null ? mgr.getGlyphData(cp) : null;
            cursor += data != null ? data.advance * scale : 5.0f;
            i += Character.charCount(cp);
        }

        return Mth.ceil(cursor);
    }

    private static boolean isCustomTextActive() {
        if (!ConfigManager.data.customTextEnabled) return false;
        if (!ConfigManager.data.customTextGuiOnly) return true;
        return McCompat.getScreen(Minecraft.getInstance()) instanceof BaseScreen;
    }

    private static boolean blackaddons$ensureCustomRendererReady(CustomFontRenderer renderer) {
        if (!renderer.isInitialized()) {
            renderer.init();
        }

        return renderer.isInitialized();
    }

    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true, index = 1)
    private String onDrawString(String text) {
        return EmojiManager.preprocessString(CustomNameManager.getInstance().replaceInString(text));
    }

    @ModifyVariable(
            method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
    private FormattedCharSequence onDrawSequence(FormattedCharSequence text) {
        return new EmojiSequenceCharSequence(CustomNameManager.getInstance().replaceInSequence(text));
    }

    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void onWidthStringCustom(String text, CallbackInfoReturnable<Integer> cir) {
        if (isCustomTextActive() && text != null) {
            CustomFontRenderer renderer = CustomFontRenderer.getInstance();
            if (blackaddons$ensureCustomRendererReady(renderer)) {
                CustomFontManager mgr = renderer.getManager();
                int result = measureVisualWidth(renderer, mgr, text, ConfigManager.data.customFontBold);
                cir.setReturnValue(result);
            }
        }
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true, index = 1)
    private String onWidthString(String text) {
        return EmojiManager.preprocessString(CustomNameManager.getInstance().replaceInString(text));
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true, index = 1)
    private FormattedText onWidthComponent(FormattedText text) {
        if (text instanceof Component c) return CustomNameManager.getInstance().replaceNames(c);
        return text;
    }

    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true)
    private void onWidthComponentCustom(FormattedText text, CallbackInfoReturnable<Integer> cir) {
        if (isCustomTextActive() && text != null) {
            CustomFontRenderer renderer = CustomFontRenderer.getInstance();
            if (blackaddons$ensureCustomRendererReady(renderer)) {
                CustomFontManager mgr = renderer.getManager();
                float scale = renderer.getCachedScale();
                float[] cursor = {0};
                text.visit((style, string) -> {
                    String preprocessed = EmojiManager.preprocessString(string);
                    for (int i = 0; i < preprocessed.length(); ) {
                        int cp = preprocessed.codePointAt(i);
                        if (cp == 167) { // '§'
                            i += Character.charCount(cp);
                            if (i < string.length()) {
                                int nextCp = string.codePointAt(i);
                                i += Character.charCount(nextCp);
                            }
                            continue;
                        }
                        if (CustomFontRenderer.isVariationSelector(cp)) {
                            i += Character.charCount(cp);
                            continue;
                        }
                        int arrowDir = EmojiManager.getArrowDirection(cp);
                        if (arrowDir != -1) {
                            cursor[0] += EmojiManager.getArrowAdvance();
                            i += Character.charCount(cp);
                            continue;
                        }
                        if (EmojiManager.isEmoji(cp)) {
                            float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
                            cursor[0] += emojiSize + 1.0f;
                            i += Character.charCount(cp);
                            continue;
                        }
                        CustomFontManager.GlyphData data = mgr != null ? mgr.getGlyphData(cp) : null;
                        cursor[0] += data != null ? data.advance * scale : 5.0f;
                        i += Character.charCount(cp);
                    }
                    return Optional.empty();
                }, Style.EMPTY);
                int result = Mth.ceil(cursor[0]);
                cir.setReturnValue(result);
            }
        }
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true, index = 1)
    private FormattedCharSequence onWidthSequence(FormattedCharSequence text) {
        return new EmojiSequenceCharSequence(CustomNameManager.getInstance().replaceInSequence(text));
    }

    @Inject(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), cancellable = true)
    private void onWidthSequenceCustom(FormattedCharSequence text, CallbackInfoReturnable<Integer> cir) {
        if (isCustomTextActive() && text != null) {
            CustomFontRenderer renderer = CustomFontRenderer.getInstance();
            if (blackaddons$ensureCustomRendererReady(renderer)) {
                CustomFontManager mgr = renderer.getManager();
                float scale = renderer.getCachedScale();
                float[] cursor = {0};
                text.accept((idx, style, cp) -> {
                    if (CustomFontRenderer.isVariationSelector(cp)) {
                        return true;
                    }
                    int arrowDir = EmojiManager.getArrowDirection(cp);
                    if (arrowDir != -1) {
                        cursor[0] += EmojiManager.getArrowAdvance();
                        return true;
                    }
                    if (EmojiManager.isEmoji(cp)) {
                        float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
                        cursor[0] += emojiSize + 1.0f;
                        return true;
                    }
                    CustomFontManager.GlyphData data = mgr != null ? mgr.getGlyphData(cp) : null;
                    cursor[0] += data != null ? data.advance * scale : 5.0f;
                    return true;
                });
                int result = Mth.ceil(cursor[0]);
                cir.setReturnValue(result);
            }
        }
    }



//? if >=26.2 {

    /*@Inject(method = "prepare8xTextOutline", at = @At("HEAD"))
    private void onOutlineStart(CallbackInfoReturnable<Font.PreparedText> cir) {
        CustomFontRenderer.inOutlinePass = true;
    }

    @Inject(method = "prepare8xTextOutline", at = @At("RETURN"))
    private void onOutlineEnd(CallbackInfoReturnable<Font.PreparedText> cir) {
        CustomFontRenderer.inOutlinePass = false;
    }

*///?} else {
    @Inject(method = "drawInBatch8xOutline", at = @At("HEAD"))
    private void onOutlineStart(CallbackInfo ci) {
        CustomFontRenderer.inOutlinePass = true;
    }

    @Inject(method = "drawInBatch8xOutline", at = @At("RETURN"))
    private void onOutlineEnd(CallbackInfo ci) {
        CustomFontRenderer.inOutlinePass = false;
    }
//?}

    @Inject(method = "getGlyph", at = @At("HEAD"), cancellable = true)
    private void onGetGlyph(int codepoint, Style style, CallbackInfoReturnable<BakedGlyph> cir) {
        if (isCustomTextActive() && !CustomFontRenderer.inOutlinePass) {
            CustomFontRenderer renderer = CustomFontRenderer.getInstance();
            if (blackaddons$ensureCustomRendererReady(renderer)) {
                int arrowDir = EmojiManager.getArrowDirection(codepoint);
                if (arrowDir != -1) {
                    final int dir = arrowDir;
                    cir.setReturnValue(new CustomTexturedBakedGlyph(
                            EmojiManager::getArrowAdvance,
                            (x, y) -> {
                                float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
                                float arrowSize = EmojiManager.getArrowSize();
                                float advance = EmojiManager.getArrowAdvance();
                                float baseline = ConfigManager.data.customTextEnabled ? renderer.getCachedBaseline() : 7.0f;
                                float yCenter = y + baseline - emojiSize / 2.0f;
                                float ey0 = yCenter - arrowSize / 2.0f;
                                float ey1 = yCenter + arrowSize / 2.0f;
                                float ex0 = x + (advance - arrowSize) / 2.0f;
                                float ex1 = ex0 + arrowSize;
                                return new float[] { ex0, ey0, ex1, ey1 };
                            },
                            () -> EmojiManager.getArrowUvs(dir),
                            () -> (RenderType) McCompat.createTextRenderType("arrow_3d",
                                    BlackaddonsRenderPipelines.PLAIN_TEXTURED, EmojiManager.ARROW_LOCATION),
                            EmojiManager::getArrowTextureView,
                            BlackaddonsRenderPipelines.PLAIN_TEXTURED
                    ));
                    return;
                }
                if (CustomFontRenderer.isVariationSelector(codepoint) || EmojiManager.isEmoji(codepoint)) {
                    final int cp = codepoint;
                    cir.setReturnValue(new CustomTexturedBakedGlyph(
                            () -> {
                                if (CustomFontRenderer.isVariationSelector(cp)) return 0.0f;
                                float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
                                return emojiSize + 1.0f;
                            },
                            (x, y) -> {
                                if (CustomFontRenderer.isVariationSelector(cp)) return new float[] { x, y, x, y };
                                EmojiManager.EmojiTexture tex = EmojiManager.getEmojiTexture(cp);
                                if (tex == null) return new float[] { x, y, x, y };
                                float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
                                float baseline = ConfigManager.data.customTextEnabled ? renderer.getCachedBaseline() : 7.0f;
                                float ey1 = y + baseline + emojiSize * 0.1f;
                                float ey0 = ey1 - emojiSize;
                                return new float[] { x, ey0, x + emojiSize, ey1 };
                            },
                            () -> new float[] { 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f },
                            () -> {
                                EmojiManager.EmojiTexture tex = EmojiManager.getEmojiTexture(cp);
                                if (tex == null) return null;
                                return (RenderType) McCompat.createTextRenderType("emoji_3d",
                                        BlackaddonsRenderPipelines.PLAIN_TEXTURED, tex.location);
                            },
                            () -> {
                                EmojiManager.EmojiTexture tex = EmojiManager.getEmojiTexture(cp);
                                return tex != null ? tex.textureView : null;
                            },
                            BlackaddonsRenderPipelines.PLAIN_TEXTURED
                    ));
                    return;
                }
                CustomBakedGlyph baked = style.isObfuscated()
                        ? renderer.getOrCreateObfuscatedBakedGlyph(codepoint, this.random)
                        : renderer.getOrCreateBakedGlyph(codepoint);
                if (baked != null) {
                    cir.setReturnValue(baked);
                }
            }
        }
    }
}
