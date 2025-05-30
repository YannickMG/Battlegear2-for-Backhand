package mods.battlegear2.client;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.common.registry.GameData;
import mods.battlegear2.Battlegear;
import mods.battlegear2.CommonProxy;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.heraldry.IHeraldryItem;
import mods.battlegear2.api.shield.IShield;
import mods.battlegear2.client.renderer.BowRenderer;
import mods.battlegear2.client.renderer.BowRendererDiamond;
import mods.battlegear2.client.renderer.BowRendererIron;
import mods.battlegear2.client.renderer.FlagPoleItemRenderer;
import mods.battlegear2.client.renderer.FlagPoleTileRenderer;
import mods.battlegear2.client.renderer.HeraldryCrestItemRenderer;
import mods.battlegear2.client.renderer.HeraldryItemRenderer;
import mods.battlegear2.client.renderer.QuiverItremRenderer;
import mods.battlegear2.client.renderer.ShieldRenderer;
import mods.battlegear2.client.renderer.SpearRenderer;
import mods.battlegear2.client.utils.BattlegearClientUtils;
import mods.battlegear2.heraldry.TileEntityFlagPole;
import mods.battlegear2.packet.BattlegearAnimationPacket;
import mods.battlegear2.packet.SpecialActionPacket;
import mods.battlegear2.utils.BattlegearConfig;
import mods.battlegear2.utils.EnumBGAnimations;
import xonin.backhand.api.core.BackhandUtils;

public final class ClientProxy extends CommonProxy {

    public static boolean tconstructEnabled = false;
    public static IIcon[] backgroundIcon, bowIcons, bowIronIcons, bowDiamondIcons, bowGoldIcons; // bowGregIcons,;

    @Override
    public void registerTickHandelers() {
        super.registerTickHandelers();
        MinecraftForge.EVENT_BUS.register(BattlegearClientEvents.INSTANCE);
        FMLCommonHandler.instance().bus().register(BattlegearClientTickHandeler.INSTANCE);
        BattlegearUtils.RENDER_BUS.register(new BattlegearClientUtils());
    }

    @Override
    public void sendAnimationPacket(EnumBGAnimations animation, EntityPlayer entityPlayer) {
        if (entityPlayer instanceof EntityClientPlayerMP) {
            ((EntityClientPlayerMP) entityPlayer).sendQueue
                    .addToSendQueue(new BattlegearAnimationPacket(animation, entityPlayer).generatePacket());
        }
    }

    @Override
    public void startFlash(EntityPlayer player, float damage) {
        if (player.getCommandSenderName().equals(Minecraft.getMinecraft().thePlayer.getCommandSenderName())) {
            BattlegearClientTickHandeler.resetFlash();
            ItemStack offhand = BackhandUtils.getOffhandItem(player);

            if (offhand != null && offhand.getItem() instanceof IShield) BattlegearClientTickHandeler
                    .reduceBlockTime(((IShield) offhand.getItem()).getDamageDecayRate(offhand, damage));
        }
    }

    @Override
    public void registerItemRenderers() {
        if (Arrays.binarySearch(BattlegearConfig.disabledRenderers, "spear") < 0) {
            SpearRenderer spearRenderer = new SpearRenderer();
            for (Item spear : BattlegearConfig.spear) {
                if (spear != null) MinecraftForgeClient.registerItemRenderer(spear, spearRenderer);
            }
        }

        if (Arrays.binarySearch(BattlegearConfig.disabledRenderers, "shield") < 0) {
            ShieldRenderer shieldRenderer = new ShieldRenderer();
            for (Item shield : BattlegearConfig.shield) {
                if (shield != null) MinecraftForgeClient.registerItemRenderer(shield, shieldRenderer);
            }
        }

        if (Arrays.binarySearch(BattlegearConfig.disabledRenderers, "bow") < 0)
            MinecraftForgeClient.registerItemRenderer(Items.bow, new BowRenderer());
        MinecraftForgeClient.registerItemRenderer(BattlegearConfig.mobBowIron, new BowRendererIron());
        // MinecraftForgeClient.registerItemRenderer(BattlegearConfig.modBowGold, new BowRendererGold());
        MinecraftForgeClient.registerItemRenderer(BattlegearConfig.modBowDiamond, new BowRendererDiamond());
        // MinecraftForgeClient.registerItemRenderer(BattlegearConfig.modBowGreg, new BowRendererGold());
        if (BattlegearConfig.quiver != null && Arrays.binarySearch(BattlegearConfig.disabledRenderers, "quiver") < 0)
            MinecraftForgeClient.registerItemRenderer(BattlegearConfig.quiver, new QuiverItremRenderer());
        if (BattlegearConfig.banner != null
                && Arrays.binarySearch(BattlegearConfig.disabledRenderers, "flagpole") < 0) {
            MinecraftForgeClient
                    .registerItemRenderer(Item.getItemFromBlock(BattlegearConfig.banner), new FlagPoleItemRenderer());
            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFlagPole.class, new FlagPoleTileRenderer());
        }
        if (Battlegear.debug) {
            Item it = null;
            for (Iterator<Item> itr = GameData.getItemRegistry().iterator(); itr.hasNext(); it = itr.next()) {
                if (it instanceof IHeraldryItem && ((IHeraldryItem) it).useDefaultRenderer()) {
                    MinecraftForgeClient.registerItemRenderer(it, new HeraldryItemRenderer());
                }
            }
            MinecraftForgeClient.registerItemRenderer(BattlegearConfig.heradricItem, new HeraldryCrestItemRenderer());
        }
    }

    @Override
    public IIcon getSlotIcon(int index) {
        if (backgroundIcon != null) {
            return backgroundIcon[index];
        } else {
            return null;
        }
    }

    @Override
    public void doSpecialAction(EntityPlayer entityPlayer, ItemStack itemStack) {
        MovingObjectPosition mop = null;
        if (itemStack != null && itemStack.getItem() instanceof IShield) {
            mop = getMouseOver(1, 4);
        }

        FMLProxyPacket p;
        if (mop != null && mop.entityHit instanceof EntityLivingBase) {
            p = new SpecialActionPacket(entityPlayer, mop.entityHit).generatePacket();
            if (mop.entityHit instanceof EntityPlayerMP) {
                Battlegear.packetHandler.sendPacketToPlayer(p, (EntityPlayerMP) mop.entityHit);
            }
        } else {
            p = new SpecialActionPacket(entityPlayer, null).generatePacket();
        }
        Battlegear.packetHandler.sendPacketToServer(p);
    }

    /**
     * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
     */
    @Override
    public MovingObjectPosition getMouseOver(float tickPart, float maxDist) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc.renderViewEntity != null) {
            if (mc.theWorld != null) {
                mc.pointedEntity = null;
                double d0 = (double) maxDist;
                MovingObjectPosition objectMouseOver = mc.renderViewEntity.rayTrace(d0, tickPart);
                double d1 = d0;
                Vec3 vec3 = mc.renderViewEntity.getPosition(tickPart);

                if (objectMouseOver != null) {
                    d1 = objectMouseOver.hitVec.distanceTo(vec3);
                }

                Vec3 vec31 = mc.renderViewEntity.getLook(tickPart);
                Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
                Entity pointedEntity = null;
                float f1 = 1.0F;
                List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                        mc.renderViewEntity,
                        mc.renderViewEntity.boundingBox
                                .addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0)
                                .expand((double) f1, (double) f1, (double) f1));
                double d2 = d1;

                for (int i = 0; i < list.size(); ++i) {
                    Entity entity = (Entity) list.get(i);

                    if (entity.canBeCollidedWith()) {
                        float f2 = entity.getCollisionBorderSize();
                        AxisAlignedBB axisalignedbb = entity.boundingBox.expand((double) f2, (double) f2, (double) f2);
                        MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                        if (axisalignedbb.isVecInside(vec3)) {
                            if (0.0D < d2 || d2 == 0.0D) {
                                pointedEntity = entity;
                                d2 = 0.0D;
                            }
                        } else if (movingobjectposition != null) {
                            double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                            if (d3 < d2 || d2 == 0.0D) {
                                pointedEntity = entity;
                                d2 = d3;
                            }
                        }
                    }
                }

                if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                    objectMouseOver = new MovingObjectPosition(pointedEntity);
                }

                return objectMouseOver;
            }
        }
        return null;
    }

    @Override
    public EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }
}
