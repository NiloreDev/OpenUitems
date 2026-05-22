package wtf.uitems.client.feature.module.impl.visual.overlay.impl.targetinfo;

import com.ibm.icu.impl.Pair;
// import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.LocalDataWatch;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.IOverlayElement;
import wtf.uitems.client.feature.module.property.impl.ScreenPositionProperty;
import wtf.uitems.client.renderer.MinecraftRenderer;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.utility.misc.chat.ChatUsernameDecorator;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.OrderedTextVisitor;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;
import wtf.uitems.utility.socket.user.User;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.NVG_IMAGE_NODELETE;
import static org.lwjgl.nanovg.NanoVGGL3.nvglCreateImageFromHandle;
import static wtf.uitems.client.renderer.NVGRenderer.getContext;
import static wtf.uitems.client.Constants.mc;

public final class TargetInfoElement implements IOverlayElement {

    private static final NVGTextRenderer BOLD_FONT = FontRepository.getFont("Mifont");
    private static final NVGTextRenderer MEDIUM_FONT = FontRepository.getFont("Mifont");
    private static final NVGTextRenderer TENACITY_FONT = FontRepository.getFont("tenacity-bold");
    private static final NVGTextRenderer ICON_FONT = FontRepository.getFont("materialicons-regular");
    private static final DecimalFormat HEALTH_DF = new DecimalFormat("0.#");

    private final Animation targetAnimation, healthAnimation, cardHealthTextAnimation;
    private final TargetInfoSettings settings;
    private final HUDModule overlay;
    private Target currentTarget, lastTarget;
    private int cardHealthTargetId = -1;

    public TargetInfoElement(final HUDModule module) {
        this.overlay = module;
        this.settings = new TargetInfoSettings(module);

        this.targetAnimation = new Animation(Easing.EASE_OUT_EXPO, 200);
        this.targetAnimation.setValue(1);

        this.healthAnimation = new Animation(Easing.EASE_OUT_EXPO, 1000);
        this.cardHealthTextAnimation = new Animation(Easing.EASE_OUT_EXPO, 350);
    }

    public void initialize() {
    }

    @Override
    public void render(DrawContext context, float delta, boolean isBloom) {
        final Target target = this.getTarget();
        if (target == null) {
            this.cardHealthTargetId = -1;
            return;
        }

        if (this.settings.getMode() == TargetInfoSettings.Mode.CARD) {
            this.renderCardMode(context, target, isBloom);
            if (currentTarget != null) {
                lastTarget = currentTarget;
            }
            return;
        }

        final float scale = this.settings.getScale();

        final float targetNameSize = 6;
        final float hpSize = 5;

        String targetName = Formatting.WHITE + target.getFormattedName();
        int targetNameColor = -1;

        String ign = target.entity.getName().getString();
        if (target.entity instanceof net.minecraft.entity.player.PlayerEntity p) {
            ign = p.getGameProfile().name();
        }

        String displayName = null;
        final wtf.uitems.client.verify.VerifyManager verifyManager = wtf.uitems.client.verify.VerifyManager.getInstance();
        if (verifyManager != null && verifyManager.getTransport() != null) {
            displayName = verifyManager.getTransport().getName(ign);
        }
        if (displayName == null) {
            final User user = ClientSocket.getInstance().getUserOrNull(target.entity.getUuid());
            if (user != null) {
                displayName = user.getName();
            }
        }
        if (target.entity.getUuid().equals(mc.getSession().getUuidOrNull())) {
            final String verifyName = wtf.uitems.client.verify.VerifyManager.getInstance().getUsername();
            if (verifyName != null && !verifyName.isEmpty()) {
                displayName = verifyName;
            }
        }

        if (displayName != null) {
            final String plain = targetName.replaceAll("§.", "");
            final String suffix = " (" + displayName + ")";
            if (!plain.contains(suffix)) {
                targetName += " " + Formatting.GRAY + "(" + Formatting.RESET + displayName + Formatting.GRAY + ")";
            }
            targetNameColor = ColorUtility.getClientTheme().first;
        }
        final String finalTargetName = targetName;
        final int finalTargetNameColor = targetNameColor;

        final int skinTextureGlId = isBloom ? -1 : this.getSkinTextureGlId(target.entity);

        final float padding = 3;
        final float headOffset = 22.5F;
        final float equipmentWidth = 55;

        final ScreenPositionProperty screenPosition = this.settings.getScreenPosition();

        final float width = (padding * 2) + Math.max(50, Math.max(equipmentWidth, BOLD_FONT.getStringWidth(targetName, targetNameSize))) + headOffset + 1;
        final float height = (padding * 2) + 25.5F;

        final float x = screenPosition.getScaledX();
        final float y = screenPosition.getScaledY();

        screenPosition.setWidth(width * scale);
        screenPosition.setHeight(height * scale);

        final float targetAnimationProgress = this.targetAnimation.getValue();
        final float healthAnimationProgress = this.healthAnimation.getValue();

        final Pair<Integer, Integer> theme = ColorUtility.getClientTheme();

        final float trueHealthPercent = MathHelper.clamp(
                (target.getHealth() + target.entity.getAbsorptionAmount()) / (target.getMaxHealth() + target.entity.getAbsorptionAmount()),
                0, 1
        );

        this.healthAnimation.run(trueHealthPercent);


        NVGRenderer.scale(scale, x, y, 0, 0, () -> {
            NVGRenderer.globalAlpha(targetAnimationProgress);

            // background
            NVGRenderer.roundedRect(x, y, width, height, 4, NVGRenderer.BLUR_PAINT, overlay.getBlurSampleOpacity());
            if (this.settings.isGlow()) {
                final long now = System.currentTimeMillis();
                final float t = (float) ((now % 4000L) / 4000.0);
                final float angle = t * 360F;
                final int a1 = (int) (90 + 50 * Math.sin(t * Math.PI * 2));
                final int a2 = (int) (70 + 60 * Math.sin((t + 0.25F) * Math.PI * 2));
                final int c1 = ColorUtility.applyOpacity(theme.first, Math.max(0, Math.min(255, a1)));
                final int c2 = ColorUtility.applyOpacity(theme.second, Math.max(0, Math.min(255, a2)));
                NVGRenderer.roundedRectGradient(x, y, width, height, 4, c1, c2, angle);
                NVGRenderer.roundedRectGradient(x, y, width, height, 4, c2, c1, angle + 110F);
            }
            NVGRenderer.roundedRect(x, y, width, height, 4, overlay.getBlurBackgroundColor());
            overlay.renderBlurTextureOverlay(x, y, width, height, 4F);

            // name
            BOLD_FONT.drawString(finalTargetName, x + padding + headOffset, y + 9, targetNameSize, finalTargetNameColor);

            // health
            final float absorption = target.entity.getAbsorptionAmount();
            final float heartWidth = ICON_FONT.getStringWidth("\uE87D", hpSize);
            final String hp = HEALTH_DF.format(target.getHealth() + absorption);

            ICON_FONT.drawString((absorption > 0 ? "" : Formatting.RED) + "\uE87D", x + width - (padding * 2.5F), y + 29, hpSize, 0xFFFFC247);
            MEDIUM_FONT.drawString(hp, x + width - padding - MEDIUM_FONT.getStringWidth(hp, hpSize) - heartWidth - 0.25F, y + 28.5F, hpSize, -1);

            // health bar
            {
                final float healthBarWidth = width - (padding * 2.75F) - MEDIUM_FONT.getStringWidth(hp.length() > 2 ? hp : "88.", hpSize) - heartWidth;

                // full width bg
                NVGRenderer.roundedRect(
                        x + padding - 0.125F, y + 24.75F,
                        healthBarWidth, 4, 5 / 3F,
                        ColorUtility.applyOpacity(ColorUtility.darker(theme.second, 0.8F), 0.6F)
                );

                // animated health bg
                if (healthAnimationProgress > 0.01) {
                    NVGRenderer.roundedRectGradient(
                            x + padding - 0.125F, y + 24.75F,
                            healthAnimationProgress * healthBarWidth, 4, 5 / 3F,
                            ColorUtility.darker(theme.first, 0.6F), ColorUtility.darker(theme.second, 0.6F), 0
                    );
                }

                // true health
                if (trueHealthPercent > 0.01) {
                    NVGRenderer.roundedRectGradient(
                            x + padding - 0.125F, y + 24.75F,
                            trueHealthPercent * healthBarWidth, 4, 5 / 3F,
                            theme.first, theme.second, 0
                    );

                    NVGRenderer.roundedRectGradient(
                            x + padding - 0.125F, y + 24.75F,
                            trueHealthPercent * healthBarWidth, 4, 5 / 3F,
                         Color.TRANSLUCENT, ColorUtility.applyOpacity(0xFF000000, 0.6F), 90
                    );
                }
            }

            // head
            renderHead:
            {
                if (skinTextureGlId == -1) {
                    break renderHead;
                }

                long vg = getContext();
                nvgBeginPath(vg);

                final float headX = x + padding + 0.25F;
                final float headY = y + padding;
                final float headScale = 8 / 3F;
                final float size = 19.5F;

                final int skinTextureHandle = target.getSkinTextureHandle(skinTextureGlId);
                nvgImagePattern(vg, headX - ((64 - 4.8F) / headScale), headY - ((64 - 3) / headScale),
                        64 * headScale, 64 * headScale, 0, skinTextureHandle, 1, NVGRenderer.NVG_PAINT);
//                    nvgShapeAntiAlias(vg, false);

                if (target.entity.hurtTime > 0) {
                    final float damageFactor = target.entity.hurtTime / (float) target.entity.maxHurtTime;
                    final float reductionFactor = 0.6F;
                    final float r = Math.min(1, 1 + ((1 - reductionFactor) * damageFactor));
                    final float g = 1 - (damageFactor * reductionFactor);
                    final float b = 1 - (damageFactor * reductionFactor);
                    NVGRenderer.applyColor(new Color(r, g, b).getRGB(), NVGRenderer.NVG_COLOR_1);
                    NVGRenderer.NVG_PAINT.innerColor(NVGRenderer.NVG_COLOR_1);
                }

                nvgFillPaint(vg, NVGRenderer.NVG_PAINT);
                nvgRoundedRect(vg, headX, headY, size, size, 2);
                nvgFill(vg);
                nvgClosePath(vg);
//                    nvgShapeAntiAlias(vg, true);
            }

            // render equipment
            {
                final List<ItemStack> equipment = new ArrayList<>();

                for (final EquipmentSlot equipmentSlot : AttributeModifierSlot.ARMOR) {
                    if (equipmentSlot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                        continue;
                    }
                    equipment.add(target.entity.getEquippedStack(equipmentSlot));
                }

                equipment.add(target.entity.getMainHandStack());
                Collections.reverse(equipment);

                final float stackScale = 0.625F * scale;
                final float stackTextScale = 0.6F;

                final int equipmentCount = equipment.size();

                // slot backgrounds
                for (int i = 0; i < equipmentCount; i++) {
                    final float boxX = x + (i * 11.5F) + padding + headOffset - 0.5F;
                    final float boxY = y + padding + 8.5F;
                    NVGRenderer.roundedRect(boxX, boxY, 10.5F, 10.5F, 1, ColorUtility.applyOpacity(Colors.BLACK, 0.2F));
                }

                // reset alpha
                NVGRenderer.globalAlpha(1);

                if (!isBloom) {
                    // Bloom only needs the card silhouette, so skip the item queue work there.
                    MinecraftRenderer.addToQueue(() -> {
                        // draw now so alpha doesn't affect previously queued items
                        context.createNewRootLayer();

                        // RenderSystem.enableBlend();
//                    RenderSystem.setShaderColor(1, 1, 1, targetAnimationProgress);
//                    DiffuseLighting.disableGuiDepthLighting();

                        for (int i = 0; i < equipmentCount; i++) {
                            final float offsetX = (i * 11.6F) + padding + headOffset - 0.5F / scale;
                            final float offsetY = padding + 8.5F;
                            final float stackX = x + offsetX * scale;
                            final float stackY = y + offsetY * scale;

                            context.getMatrices().pushMatrix();
                            context.getMatrices().translate(stackX, stackY);
                            context.getMatrices().scale(stackScale, stackScale);

                            context.getMatrices().scale(stackTextScale, stackTextScale);
//                        context.getMatrices().translate(6, 8);

                            final ItemStack stack = equipment.get(i);
                            context.getMatrices().pushMatrix();

                            context.getMatrices().transform(new Vector3f(-6, -12, -200));
                            context.getMatrices().scale(1 / stackTextScale, 1 / stackTextScale);

                            if (stack.getItem() instanceof BlockItem) {
                                if (targetAnimationProgress >= 0.5F) {
                                    context.drawItem(stack, 0, 0, -200);
                                }
                            } else {
                                context.drawItem(stack, 0, 0, -200);
                            }
                            context.getMatrices().popMatrix();

                            context.getMatrices().popMatrix();
                        }
                        // GlStateManager._disableBlend();
                    });
                }
            }
        });

        if (currentTarget != null) {
            lastTarget = currentTarget;
        }
    }

    private void renderCardMode(final DrawContext context, final Target target, final boolean isBloom) {
        final float scale = this.settings.getScale();
        final float padding = 4F;
        final float radius = 6F;
        final float headSize = 28F;
        final float gap = 6F;

        final String targetName = target.getFormattedName();
        final String ircName = target.entity instanceof net.minecraft.entity.player.PlayerEntity p
                ? ChatUsernameDecorator.getSocketDisplayName(p.getUuid(), p.getGameProfile().name())
                : null;
        final String formattedIrcName = ircName != null && !ircName.isEmpty() ? ircName : null;
        final float nameSize = 10.2F;
        final float hpSize = 6F;
        final float nameWidth = BOLD_FONT.getStringWidth(targetName, nameSize)
                + (formattedIrcName != null ? BOLD_FONT.getStringWidth("(" + formattedIrcName + ")", nameSize - 0.2F) + 2F : 0F);

        final float width = Math.max(120F, padding * 2F + headSize + gap + Math.max(nameWidth, 62F));
        final float height = padding * 2F + headSize;

        final ScreenPositionProperty screenPosition = this.settings.getScreenPosition();
        final float x = screenPosition.getScaledX();
        final float y = screenPosition.getScaledY();
        screenPosition.setWidth(width * scale);
        screenPosition.setHeight(height * scale);

        final Pair<Integer, Integer> theme = ColorUtility.getClientTheme();
        final float realHealth = target.getHealth() + target.entity.getAbsorptionAmount();
        final float totalHealth = Math.max(1F, target.getMaxHealth() + target.entity.getAbsorptionAmount());
        final float trueHealthPercent = MathHelper.clamp(realHealth / totalHealth, 0F, 1F);

        if (this.cardHealthTargetId != target.entity.getId()) {
            this.cardHealthTargetId = target.entity.getId();
            this.cardHealthTextAnimation.setValue(realHealth);
        }
        this.cardHealthTextAnimation.run(realHealth);
        this.healthAnimation.run(trueHealthPercent);

        final float anim = this.targetAnimation.getValue();
        final long now = System.currentTimeMillis();
        final float t = (float) ((now % 3200L) / 3200.0);
        final float angle = t * 360F;

        final int skinTextureGlId = isBloom ? -1 : this.getSkinTextureGlId(target.entity);

        NVGRenderer.scale(scale, x, y, 0, 0, () -> {
            NVGRenderer.globalAlpha(anim);

            if (isBloom) {
                final int edgeA = ColorUtility.applyOpacity(theme.first, 170);
                final int edgeB = ColorUtility.applyOpacity(theme.second, 170);
                NVGRenderer.roundedRectOutline(x, y, width, height, radius, 1.2F, edgeA);
                NVGRenderer.roundedRectOutline(x + 0.35F, y + 0.35F, width - 0.7F, height - 0.7F, radius, 1.0F, edgeB);
                NVGRenderer.globalAlpha(1F);
                return;
            }

            NVGRenderer.roundedRect(x, y, width, height, radius, NVGRenderer.BLUR_PAINT, overlay.getBlurSampleOpacity());
            if (this.settings.isGlow()) {
                final int c1 = ColorUtility.applyOpacity(theme.first, 105);
                final int c2 = ColorUtility.applyOpacity(theme.second, 90);
                NVGRenderer.roundedRectGradient(x, y, width, height, radius, c1, c2, angle);
                NVGRenderer.roundedRectGradient(x, y, width, height, radius, c2, c1, angle + 120F);
            }
            NVGRenderer.roundedRect(x, y, width, height, radius, overlay.getBlurBackgroundColor());
            overlay.renderBlurTextureOverlay(x, y, width, height, radius);

            final float headX = x + padding;
            final float headY = y + padding;
            if (skinTextureGlId != -1) {
                final long vg = getContext();
                final int skinTextureHandle = target.getSkinTextureHandle(skinTextureGlId);

                final float patternSize = headSize * 8F;

                nvgBeginPath(vg);
                nvgImagePattern(vg, headX - headSize, headY - headSize,
                        patternSize, patternSize, 0, skinTextureHandle, 1, NVGRenderer.NVG_PAINT);
                if (target.entity.hurtTime > 0) {
                    final float damageFactor = target.entity.hurtTime / (float) target.entity.maxHurtTime;
                    final float reductionFactor = 0.6F;
                    final float r = Math.min(1, 1 + ((1 - reductionFactor) * damageFactor));
                    final float g = 1 - (damageFactor * reductionFactor);
                    final float b = 1 - (damageFactor * reductionFactor);
                    NVGRenderer.applyColor(new Color(r, g, b).getRGB(), NVGRenderer.NVG_COLOR_1);
                    NVGRenderer.NVG_PAINT.innerColor(NVGRenderer.NVG_COLOR_1);
                }
                nvgFillPaint(vg, NVGRenderer.NVG_PAINT);
                nvgRoundedRect(vg, headX, headY, headSize, headSize, 4F);
                nvgFill(vg);
                nvgClosePath(vg);

                nvgBeginPath(vg);
                nvgImagePattern(vg, headX - (headSize * 5F), headY - headSize,
                        patternSize, patternSize, 0, skinTextureHandle, 1, NVGRenderer.NVG_PAINT);
                nvgFillPaint(vg, NVGRenderer.NVG_PAINT);
                nvgRoundedRect(vg, headX, headY, headSize, headSize, 4F);
                nvgFill(vg);
                nvgClosePath(vg);
            }

            final float infoX = headX + headSize + gap;
            final float nameY = y + padding + 10.2F;
            final float hpY = y + padding + 22.8F;
            BOLD_FONT.drawString(targetName, infoX, nameY, nameSize, -1);

            if (formattedIrcName != null) {
                final int ircColor = ColorUtility.interpolateColorsBackAndForth(16, Math.abs(formattedIrcName.hashCode() % 180), theme.first, theme.second);
                final int grayColor = ColorUtility.applyOpacity(0xFFB0B0B0, 0.88F);
                final float ircSize = nameSize - 0.2F;
                final float ircX = infoX + BOLD_FONT.getStringWidth(targetName, nameSize) + 1.5F;
                BOLD_FONT.drawString("(", ircX, nameY, ircSize, grayColor);
                final float openWidth = BOLD_FONT.getStringWidth("(", ircSize);
                BOLD_FONT.drawString(formattedIrcName, ircX + openWidth, nameY, ircSize, ircColor);
                final float nameWidthIrc = BOLD_FONT.getStringWidth(formattedIrcName, ircSize);
                BOLD_FONT.drawString(")", ircX + openWidth + nameWidthIrc, nameY, ircSize, grayColor);
            }

            final float animatedHpValue = Math.max(0F, this.cardHealthTextAnimation.getValue());
            final String hpText = HEALTH_DF.format(animatedHpValue) + " HP";
            final float hpTextWidth = MEDIUM_FONT.getStringWidth(hpText, hpSize);
            MEDIUM_FONT.drawString(hpText, infoX, hpY, hpSize, ColorUtility.applyOpacity(-1, 0.9F));

            final float barGap = 4F;
            final float barX = infoX + hpTextWidth + barGap;
            final float barH = 4.5F;
            final float barY = hpY - 4.9F;
            final float barW = Math.max(22F, x + width - padding - barX);

            NVGRenderer.roundedRect(barX, barY, barW, barH, 2.5F, ColorUtility.applyOpacity(0xFF000000, 0.35F));
            final float easedHealth = MathHelper.clamp(this.healthAnimation.getValue(), 0F, 1F);
            if (easedHealth > 0.001F) {
                final int flowA = ColorUtility.interpolateColorsBackAndForth(16, 0, theme.first, theme.second);
                final int flowB = ColorUtility.interpolateColorsBackAndForth(16, 110, theme.first, theme.second);
                NVGRenderer.roundedRectGradient(barX, barY, barW * easedHealth, barH, 2.5F,
                        ColorUtility.applyOpacity(flowA, 230), ColorUtility.applyOpacity(flowB, 230), 0F);
            }

            NVGRenderer.globalAlpha(1F);
        });
    }

    @Override
    public boolean isActive() {
        return this.settings.isEnabled();
    }

    private Target getTarget() {
        LivingEntity target = LocalDataWatch.get().lastEntityAttack.getRight();
        if (target != null && !LocalDataWatch.getTargetList().hasTarget(target.getId())) {
            target = null;
        }

        if (target == null) {
            final KillAuraModule killAuraModule = OpalClient.getInstance().getModuleRepository().getModule(KillAuraModule.class);
            if (killAuraModule.isEnabled()) {
                final CurrentTarget killAuraTarget = killAuraModule.getTargeting().getTarget();
                if (killAuraTarget != null) {
                    target = killAuraTarget.getEntity();
                }
            }
        }

        final Target preCurrentTarget = this.currentTarget;
        final Target preLastTarget = this.lastTarget;

        if (target != null) {
            if (this.currentTarget == null || this.currentTarget.entity.getId() != target.getId()) {
                this.currentTarget = new Target(target);
            }
        } else {
            if (mc.currentScreen instanceof ChatScreen) {
                if (this.currentTarget == null || this.currentTarget.entity.getId() != mc.player.getId()) {
                    this.currentTarget = new Target(mc.player);
                }
            } else {
                this.currentTarget = null;
            }
        }

        Target activeTarget = this.currentTarget;
        if (activeTarget == null) {
            if (this.targetAnimation.isFinished()) {
                this.lastTarget = null;
            } else {
                activeTarget = this.lastTarget;
                this.targetAnimation.run(0);
            }
        } else {
            this.targetAnimation.setValue(1);
            this.targetAnimation.reset();
        }

        if (activeTarget != null) {
            activeTarget.updateFormattedName();
        }

        if (preCurrentTarget != null && preCurrentTarget.skinTextureHandle != -1 && this.lastTarget == preCurrentTarget && preCurrentTarget != this.currentTarget && preCurrentTarget != activeTarget) {
            // target switched (no animation)
            nvgDeleteImage(getContext(), preCurrentTarget.skinTextureHandle);
        } else if (this.currentTarget == null && this.lastTarget != null && this.lastTarget.skinTextureHandle != -1 && this.targetAnimation.getValue() == 0) {
            // target animated out
            nvgDeleteImage(getContext(), preLastTarget.skinTextureHandle);
            this.lastTarget = null;
        }

        return activeTarget;
    }

    private int getSkinTextureGlId(final LivingEntity entity) {
        final Identifier identifier = switch (entity) {
            case AbstractClientPlayerEntity player -> player.getSkin().body().texturePath();
            case SkeletonEntity ignored -> Identifier.ofVanilla("textures/entity/skeleton/skeleton.png");
            case ZombieEntity ignored -> Identifier.ofVanilla("textures/entity/zombie/zombie.png");
            case CreeperEntity ignored -> Identifier.ofVanilla("textures/entity/creeper/creeper.png");
            case PiglinEntity ignored -> Identifier.ofVanilla("textures/entity/piglin/piglin.png");
            default -> null;
        };
        if (identifier == null) {
            return -1;
        }
        return Integer.parseInt(mc.getTextureManager().getTexture(identifier).getGlTexture().getLabel());
    }

    private static final class Target {

        private final LivingEntity entity;
        private String formattedName;

        private int skinTextureHandle = -1;

        private Target(final LivingEntity entity) {
            this.entity = entity;
        }

        private float getHealth() {
            final String name = this.entity.getName().getString();
            if (LocalDataWatch.SCOREBOARD_HEALTHS.containsKey(name)) {
                return LocalDataWatch.SCOREBOARD_HEALTHS.get(name).floatValue();
            }

            final String displayName = this.entity.getDisplayName().getString();
            if (displayName.contains("HP:")) {
                try {
                    final String healthStr = displayName.split("HP:")[1].split("]")[0].trim();
                    return Float.parseFloat(healthStr);
                } catch (Exception ignored) {
                }
            }
            return this.entity.getHealth();
        }

        private float getMaxHealth() {
            final String name = this.entity.getName().getString();
            if (LocalDataWatch.SCOREBOARD_HEALTHS.containsKey(name)) {
                return 20.0f;
            }

            final String displayName = this.entity.getDisplayName().getString();
            if (displayName.contains("HP:")) {
                return 20.0f; // Commonly used max HP on servers that show health in display name
            }
            return this.entity.getMaxHealth();
        }

        private String getFormattedName() {
            if (this.formattedName != null) {
                return this.formattedName;
            }
            return this.entity.getName().getString();
        }

        private void updateFormattedName() {
            if (this.entity.getDisplayName() == null) {
                return;
            }

            

            final OrderedTextVisitor visitor = new OrderedTextVisitor();
            this.entity.getDisplayName().asOrderedText().accept(visitor);
            this.formattedName = visitor.getFormattedString();
        }

        private int getSkinTextureHandle(final int skinTextureGlId) {
            if (this.skinTextureHandle != -1) {
                return this.skinTextureHandle;
            }
            return this.skinTextureHandle = nvglCreateImageFromHandle(getContext(), skinTextureGlId, 64, 64, NVG_IMAGE_NODELETE);
        }

    }

    @Override
    public boolean isBloom() {
        return true;
    }
}
