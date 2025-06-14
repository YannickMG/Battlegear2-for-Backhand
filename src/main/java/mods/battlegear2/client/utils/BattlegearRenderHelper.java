package mods.battlegear2.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import mods.battlegear2.api.RenderPlayerEventChild.PlayerElementType;
import mods.battlegear2.api.RenderPlayerEventChild.PostRenderPlayerElement;
import mods.battlegear2.api.RenderPlayerEventChild.PreRenderPlayerElement;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.core.IBattlePlayer;
import mods.battlegear2.api.core.IOffhandRender;
import mods.battlegear2.api.shield.IArrowDisplay;
import mods.battlegear2.api.shield.IShield;
import xonin.backhand.api.core.BackhandUtils;

public final class BattlegearRenderHelper {

    private static final ItemStack dummyStack = new ItemStack(Blocks.flowing_lava);
    public static final float RENDER_UNIT = 1F / 16F; // 0.0625
    public static float PROGRESS_INCREMENT_LIMIT = 0.4F;

    private static final ResourceLocation ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    private static final ResourceLocation DEFAULT_ARROW = new ResourceLocation("textures/entity/arrow.png");

    public static final float[] arrowX = new float[64];
    public static final float[] arrowY = new float[arrowX.length];
    public static final float[] arrowDepth = new float[arrowX.length];
    public static final float[] arrowPitch = new float[arrowX.length];
    public static final float[] arrowYaw = new float[arrowX.length];

    static {
        for (int i = 0; i < arrowX.length; i++) {
            double r = Math.random() * 5;
            double theta = Math.random() * Math.PI * 2;

            arrowX[i] = (float) (r * Math.cos(theta));
            arrowY[i] = (float) (r * Math.sin(theta));
            arrowDepth[i] = (float) (Math.random() * 0.5 + 0.5F);

            arrowPitch[i] = (float) (Math.random() * 50 - 25);
            arrowYaw[i] = (float) (Math.random() * 50 - 25);
        }
    }

    public static void renderItemInFirstPerson(float frame, Minecraft mc, ItemRenderer itemRenderer) {
        GL11.glPopMatrix();
        GL11.glCullFace(GL11.GL_BACK);

        IOffhandRender offhandRender = (IOffhandRender) itemRenderer;

        if (offhandRender.battlegear2$getOffHandItemToRender() != dummyStack) {
            float progress = offhandRender.battlegear2$getPrevEquippedOffHandProgress()
                    + (offhandRender.battlegear2$getEquippedOffHandProgress()
                            - offhandRender.battlegear2$getPrevEquippedOffHandProgress()) * frame;

            EntityClientPlayerMP player = mc.thePlayer;

            float rotation = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * frame;
            GL11.glPushMatrix();
            GL11.glRotatef(rotation, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(
                    player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * frame,
                    0.0F,
                    1.0F,
                    0.0F);
            RenderHelper.enableStandardItemLighting();
            GL11.glPopMatrix();
            float var6;
            float var7;

            var6 = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * frame;
            var7 = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * frame;
            GL11.glRotatef((player.rotationPitch - var6) * 0.1F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef((player.rotationYaw - var7) * 0.1F, 0.0F, 1.0F, 0.0F);

            int var18 = mc.theWorld.getLightBrightnessForSkyBlocks(
                    MathHelper.floor_double(player.posX),
                    MathHelper.floor_double(player.posY),
                    MathHelper.floor_double(player.posZ),
                    0);
            int var8 = var18 % 65536;
            int var9 = var18 / 65536;
            OpenGlHelper
                    .setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) var8 / 1.0F, (float) var9 / 1.0F);
            float var10;
            float var21;
            float var20;

            if (offhandRender.battlegear2$getOffHandItemToRender() != null) {
                applyColorFromItemStack(offhandRender.battlegear2$getOffHandItemToRender(), 0);
            } else {
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }

            RenderPlayer var26 = (RenderPlayer) RenderManager.instance.getEntityRenderObject(mc.thePlayer);
            RenderPlayerEvent preRender = new RenderPlayerEvent.Pre(player, var26, frame);
            RenderPlayerEvent postRender = new RenderPlayerEvent.Post(player, var26, frame);
            var7 = 0.8F;
            if (offhandRender.battlegear2$getOffHandItemToRender() != null) {

                if (offhandRender.battlegear2$getOffHandItemToRender().getItem() instanceof IShield) {
                    GL11.glPushMatrix();

                    float swingProgress = (float) ((IBattlePlayer) player).battlegear2$getSpecialActionTimer()
                            / (float) ((IShield) offhandRender.battlegear2$getOffHandItemToRender().getItem())
                                    .getBashTimer(offhandRender.battlegear2$getOffHandItemToRender());

                    GL11.glTranslatef(
                            -0.7F * var7 + 0.25F * MathHelper.sin(swingProgress * (float) Math.PI),
                            -0.65F * var7 - (1.0F - progress) * 0.6F - 0.4F,
                            -0.9F * var7 + 0.1F - 0.25F * MathHelper.sin(swingProgress * (float) Math.PI));

                    if (((IBattlePlayer) player).battlegear2$isBlockingWithShield()) {
                        GL11.glTranslatef(0.25F, 0.15F, 0);
                    }

                    GL11.glRotatef(25, 0, 0, 1);
                    GL11.glRotatef(325 - 35 * MathHelper.sin(swingProgress * (float) Math.PI), 0, 1, 0);

                    if (!BattlegearUtils.RENDER_BUS.post(
                            new PreRenderPlayerElement(
                                    preRender,
                                    true,
                                    PlayerElementType.ItemOffhand,
                                    offhandRender.battlegear2$getOffHandItemToRender())))
                        itemRenderer.renderItem(player, offhandRender.battlegear2$getOffHandItemToRender(), 0);
                    BattlegearUtils.RENDER_BUS.post(
                            new PostRenderPlayerElement(
                                    postRender,
                                    true,
                                    PlayerElementType.ItemOffhand,
                                    offhandRender.battlegear2$getOffHandItemToRender()));
                    GL11.glPopMatrix();

                }
            } else if (!player.isInvisible()) {
                GL11.glPushMatrix();

                GL11.glScalef(-1.0F, 1.0F, 1.0F);

                var20 = ((IBattlePlayer) player).battlegear2$getOffSwingProgress(frame);
                var21 = MathHelper.sin(var20 * (float) Math.PI);
                var10 = MathHelper.sin(MathHelper.sqrt_float(var20) * (float) Math.PI);
                GL11.glTranslatef(
                        -var10 * 0.3F,
                        MathHelper.sin(MathHelper.sqrt_float(var20) * (float) Math.PI * 2.0F) * 0.4F,
                        -var21 * 0.4F);
                GL11.glTranslatef(var7 * var7, -0.75F * var7 - (1.0F - progress) * 0.6F, -0.9F * var7);

                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);

                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                var21 = MathHelper.sin(var20 * var20 * (float) Math.PI);
                GL11.glRotatef(var10 * 70.0F, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(var21 * 20.0F, 0.0F, 0.0F, 1.0F);

                mc.getTextureManager().bindTexture(player.getLocationSkin());
                GL11.glTranslatef(-1.0F, 3.6F, 3.5F);
                GL11.glRotatef(120.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(200.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);

                GL11.glScalef(1.0F, 1.0F, -1.0F);
                GL11.glTranslatef(5.6F, 0.0F, 0.0F);
                GL11.glScalef(1.0F, 1.0F, 1.0F);
                if (!BattlegearUtils.RENDER_BUS
                        .post(new PreRenderPlayerElement(preRender, true, PlayerElementType.Offhand, null))) {
                    var26.renderFirstPersonArm(mc.thePlayer);
                }
                BattlegearUtils.RENDER_BUS
                        .post(new PostRenderPlayerElement(postRender, true, PlayerElementType.Offhand, null));

                GL11.glPopMatrix();
            }

            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
        }
        GL11.glPushMatrix();
    }

    public static void updateEquippedItem(ItemRenderer itemRenderer, Minecraft mc) {
        IOffhandRender offhandRender = (IOffhandRender) itemRenderer;
        offhandRender
                .battlegear2$setPrevEquippedOffHandProgress(offhandRender.battlegear2$getEquippedOffHandProgress());
        int slot = mc.thePlayer.inventory.currentItem;
        EntityPlayer player = mc.thePlayer;
        ItemStack offhandStack;
        offhandStack = slot == BackhandUtils.getOffhandSlot(player) ? (BackhandUtils.getOffhandItem(player))
                : (dummyStack);

        boolean sameItem = offhandRender.battlegear2$getEquippedItemOffhandSlot() == slot
                && offhandStack == offhandRender.battlegear2$getOffHandItemToRender();

        if (offhandRender.battlegear2$getOffHandItemToRender() == null && offhandStack == null) {
            sameItem = true;
        }

        if (offhandStack != null && offhandRender.battlegear2$getOffHandItemToRender() != null
                && offhandStack != offhandRender.battlegear2$getOffHandItemToRender()
                && offhandStack.getItem() == offhandRender.battlegear2$getOffHandItemToRender().getItem()
                && offhandStack.getItemDamage() == offhandRender.battlegear2$getOffHandItemToRender().getItemDamage()) {
            offhandRender.battlegear2$setOffHandItemToRender(offhandStack);
            sameItem = true;
        }

        float increment = (sameItem ? 1.0F : 0.0F) - offhandRender.battlegear2$getEquippedOffHandProgress();

        if (increment < -PROGRESS_INCREMENT_LIMIT) {
            increment = -PROGRESS_INCREMENT_LIMIT;
        }

        if (increment > PROGRESS_INCREMENT_LIMIT) {
            increment = PROGRESS_INCREMENT_LIMIT;
        }

        offhandRender.battlegear2$setEquippedOffHandProgress(
                offhandRender.battlegear2$getEquippedOffHandProgress() + increment);

        if (offhandRender.battlegear2$getEquippedOffHandProgress() < 0.1F) {
            offhandRender.battlegear2$setOffHandItemToRender(offhandStack);
            offhandRender.battlegear2$serEquippedItemOffhandSlot(slot);
        }
    }

    public static void moveOffHandArm(Entity entity, ModelBiped biped, float frame) {
        if (entity instanceof IBattlePlayer) {
            IBattlePlayer battlePlayer = (IBattlePlayer) entity;
            EntityPlayer player = (EntityPlayer) entity;
            float offhandSwing = 0.0F;

            ItemStack offhand = BackhandUtils.getOffhandItem(player);
            if (offhand != null && offhand.getItem() instanceof IShield) {
                offhandSwing = (float) battlePlayer.battlegear2$getSpecialActionTimer()
                        / (float) ((IShield) offhand.getItem()).getBashTimer(offhand);
            } else {
                offhandSwing = battlePlayer.battlegear2$getOffSwingProgress(frame);
            }

            if (offhandSwing > 0.0F) {
                if (biped.bipedBody.rotateAngleY != 0.0F) {
                    biped.bipedLeftArm.rotateAngleY -= biped.bipedBody.rotateAngleY;
                    biped.bipedLeftArm.rotateAngleX -= biped.bipedBody.rotateAngleY;
                }
                biped.bipedBody.rotateAngleY = -MathHelper
                        .sin(MathHelper.sqrt_float(offhandSwing) * (float) Math.PI * 2.0F) * 0.2F;

                // biped.bipedRightArm.rotationPointZ = MathHelper.sin(biped.bipedBody.rotateAngleY) * 5.0F;
                // biped.bipedRightArm.rotationPointX = -MathHelper.cos(biped.bipedBody.rotateAngleY) * 5.0F;

                biped.bipedLeftArm.rotationPointZ = -MathHelper.sin(biped.bipedBody.rotateAngleY) * 5.0F;
                biped.bipedLeftArm.rotationPointX = MathHelper.cos(biped.bipedBody.rotateAngleY) * 5.0F;

                // biped.bipedRightArm.rotateAngleY += biped.bipedBody.rotateAngleY;
                // biped.bipedRightArm.rotateAngleX += biped.bipedBody.rotateAngleY;
                float f6 = 1.0F - offhandSwing;
                f6 = 1.0F - f6 * f6 * f6;
                double f8 = MathHelper.sin(f6 * (float) Math.PI) * 1.2D;
                double f10 = MathHelper.sin(offhandSwing * (float) Math.PI) * -(biped.bipedHead.rotateAngleX - 0.7F)
                        * 0.75F;
                biped.bipedLeftArm.rotateAngleX -= f8 + f10;
                biped.bipedLeftArm.rotateAngleY += biped.bipedBody.rotateAngleY * 3.0F;
                biped.bipedLeftArm.rotateAngleZ = MathHelper.sin(offhandSwing * (float) Math.PI) * -0.4F;
            }
        }
    }

    public static void renderItemIn3rdPerson(EntityPlayer par1EntityPlayer, ModelBiped modelBipedMain, float frame) {

        ItemStack offhandItem = BackhandUtils.getOffhandItem(par1EntityPlayer);

        if (offhandItem != null && offhandItem.getItem() instanceof IShield) {

            float var7;
            RenderPlayer render = (RenderPlayer) RenderManager.instance.getEntityRenderObject(par1EntityPlayer);
            RenderPlayerEvent preRender = new RenderPlayerEvent.Pre(par1EntityPlayer, render, frame);
            RenderPlayerEvent postRender = new RenderPlayerEvent.Post(par1EntityPlayer, render, frame);

            GL11.glPushMatrix();
            modelBipedMain.bipedLeftArm.postRender(RENDER_UNIT);
            BattlegearUtils.RENDER_BUS
                    .post(new PostRenderPlayerElement(postRender, false, PlayerElementType.Offhand, null));

            GL11.glTranslatef(RENDER_UNIT, 7 * RENDER_UNIT, RENDER_UNIT);

            var7 = 10 * RENDER_UNIT;
            GL11.glScalef(var7, -var7, var7);

            GL11.glTranslated(8 * RENDER_UNIT, -11 * RENDER_UNIT, -RENDER_UNIT);

            GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(25.0F, 0.0F, 0.0F, 1.0F);
            if (!BattlegearUtils.RENDER_BUS
                    .post(new PreRenderPlayerElement(preRender, false, PlayerElementType.ItemOffhand, offhandItem))) {
                renderItemAllPasses(par1EntityPlayer, offhandItem);
            }
            BattlegearUtils.RENDER_BUS
                    .post(new PostRenderPlayerElement(postRender, false, PlayerElementType.ItemOffhand, offhandItem));

            GL11.glPopMatrix();
        }
    }

    public static void renderItemAllPasses(EntityLivingBase livingBase, ItemStack itemStack) {
        if (itemStack.getItem().requiresMultipleRenderPasses()) {
            for (int var27 = 0; var27 < itemStack.getItem().getRenderPasses(itemStack.getItemDamage()); ++var27) {
                applyColorFromItemStack(itemStack, var27);
                RenderManager.instance.itemRenderer.renderItem(livingBase, itemStack, var27);
            }
        } else {
            applyColorFromItemStack(itemStack, 0);
            RenderManager.instance.itemRenderer.renderItem(livingBase, itemStack, 0);
        }
    }

    public static void applyColorFromItemStack(ItemStack itemStack, int pass) {
        int col = itemStack.getItem().getColorFromItemStack(itemStack, pass);
        float r = (float) (col >> 16 & 255) / 255.0F;
        float g = (float) (col >> 8 & 255) / 255.0F;
        float b = (float) (col & 255) / 255.0F;
        GL11.glColor4f(r, g, b, 1.0F);
    }

    public static void renderEnchantmentEffects(Tessellator tessellator) {
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDisable(GL11.GL_LIGHTING);
        Minecraft.getMinecraft().renderEngine.bindTexture(ITEM_GLINT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
        float f7 = 0.76F;
        GL11.glColor4f(0.5F * f7, 0.25F * f7, 0.8F * f7, 1.0F);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        float f8 = 0.125F;
        GL11.glScalef(f8, f8, f8);
        float f9 = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F * 8.0F;
        GL11.glTranslatef(f9, 0.0F, 0.0F);
        GL11.glRotatef(-50.0F, 0.0F, 0.0F, 1.0F);
        ItemRenderer.renderItemIn2D(tessellator, 0.0F, 0.0F, 1.0F, 1.0F, 256, 256, RENDER_UNIT);
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glScalef(f8, f8, f8);
        f9 = (float) (Minecraft.getSystemTime() % 4873L) / 4873.0F * 8.0F;
        GL11.glTranslatef(-f9, 0.0F, 0.0F);
        GL11.glRotatef(10.0F, 0.0F, 0.0F, 1.0F);
        ItemRenderer.renderItemIn2D(tessellator, 0.0F, 0.0F, 1.0F, 1.0F, 256, 256, RENDER_UNIT);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    public static void renderArrows(ItemStack stack, boolean isEntity) {
        if (stack.getItem() instanceof IArrowDisplay) {
            int arrowCount = ((IArrowDisplay) stack.getItem()).getArrowCount(stack);
            // Bounds checking (rendering this many is quite silly, any more would look VERY silly)
            if (arrowCount > 64) arrowCount = 64;
            for (int i = 0; i < arrowCount; i++) {
                BattlegearRenderHelper.renderArrow(isEntity, i);
            }
        }
    }

    public static void renderArrow(boolean isEntity, int id) {
        if (id < arrowX.length) {
            float pitch = arrowPitch[id] + 90F;
            float yaw = arrowYaw[id] + 45F;
            renderArrow(isEntity, arrowX[id], arrowY[id], arrowDepth[id], pitch, yaw);
        }
    }

    public static void renderArrow(boolean isEntity, float x, float y, float depth, float pitch, float yaw) {
        GL11.glPushMatrix();
        // depth = 1;
        Minecraft.getMinecraft().renderEngine.bindTexture(DEFAULT_ARROW);

        float f10 = 0.05F;
        GL11.glScalef(f10, f10, f10);
        if (isEntity) {
            GL11.glScalef(1, 1, -1);
        }

        GL11.glTranslatef(x + 10.5F, y + 9.5F, 0);

        GL11.glRotatef(pitch, 0, 1, 0);
        GL11.glRotatef(yaw, 1, 0, 0);
        GL11.glNormal3f(f10, 0, 0);

        double f2 = 12F / 32F * depth;
        double f5 = 5 / 32.0F;
        Tessellator tessellator = Tessellator.instance;
        for (int i = 0; i < 2; ++i) {
            GL11.glRotatef(90, 1, 0, 0);
            GL11.glNormal3f(0, 0, f10);
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(0, -2, 0, f2, 0);
            tessellator.addVertexWithUV(16 * depth, -2, 0, 0, 0);
            tessellator.addVertexWithUV(16 * depth, 2, 0, 0, f5);
            tessellator.addVertexWithUV(0, 2, 0, f2, f5);
            tessellator.draw();

            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(0, 2, 0, f2, f5);
            tessellator.addVertexWithUV(16 * depth, 2, 0, 0, f5);
            tessellator.addVertexWithUV(16 * depth, -2, 0, 0, 0);
            tessellator.addVertexWithUV(0 * depth, -2, 0, f2, 0);
            tessellator.draw();
        }
        GL11.glPopMatrix();
    }

    public static void renderTexturedQuad(int x, int y, float z, int width, int height) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV((double) (x + 0), (double) (y + height), (double) z, 0D, 1D);
        tessellator.addVertexWithUV((double) (x + width), (double) (y + height), (double) z, 1D, 1D);
        tessellator.addVertexWithUV((double) (x + width), (double) (y + 0), (double) z, 1D, 0D);
        tessellator.addVertexWithUV((double) (x + 0), (double) (y + 0), (double) z, 0D, 0D);
        tessellator.draw();
    }
}
