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
import org.blackum.blackaddons.gui.render.font.CustomFontManager;
import org.blackum.blackaddons.gui.render.font.CustomFontRenderer;
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
        return CustomNameManager.getInstance().replaceInString(text);
    }

    @ModifyVariable(
            method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
    private FormattedCharSequence onDrawSequence(FormattedCharSequence text) {
        return CustomNameManager.getInstance().replaceInSequence(text);
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
        return CustomNameManager.getInstance().replaceInString(text);
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
                    for (int i = 0; i < string.length(); ) {
                        int cp = string.codePointAt(i);
                        if (cp == 167) { // '§'
                            i += Character.charCount(cp);
                            if (i < string.length()) {
                                int nextCp = string.codePointAt(i);
                                i += Character.charCount(nextCp);
                            }
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
        return CustomNameManager.getInstance().replaceInSequence(text);
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
