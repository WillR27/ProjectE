package moze_intel.projecte.utils;

import baubles.api.BaublesApi;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.SetFlyPKT;
import moze_intel.projecte.network.packets.StepHeightPKT;
import moze_intel.projecte.network.packets.SwingItemPKT;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Helper class for player-related methods.
 * Notice: Please try to keep methods tidy and alphabetically ordered. Thanks!
 */
public final class PlayerHelper
{
	/**
	 * Tries placing a block and fires an event for it.
	 * @return Whether the block was successfully placed
	 */
	public static boolean checkedPlaceBlock(EntityPlayerMP player, BlockPos pos, IBlockState state)
	{
		if (!hasEditPermission(player, pos))
		{
			return false;
		}
		World world = player.worldObj;
		BlockSnapshot before = BlockSnapshot.getBlockSnapshot(world, pos);
		world.setBlockState(pos, state);
		BlockEvent.PlaceEvent evt = new BlockEvent.PlaceEvent(before, Blocks.air.getDefaultState(), player); // Todo verify can use air here
		MinecraftForge.EVENT_BUS.post(evt);
		if (evt.isCanceled())
		{
			world.restoringBlockSnapshots = true;
			before.restore(true, false);
			world.restoringBlockSnapshots = false;
			PELogger.logInfo("Checked place block got canceled, restoring snapshot.");
			return false;
		}
		PELogger.logInfo("Checked place block passed!");
		return true;
	}

	public static boolean checkedReplaceBlock(EntityPlayerMP player, BlockPos pos, IBlockState state)
	{
		return hasBreakPermission(player, pos) && checkedPlaceBlock(player, pos, state);
	}

	public static ItemStack findFirstItem(EntityPlayer player, ItemPE consumeFrom)
	{
		for (ItemStack s : player.inventory.mainInventory)
		{
			if (s != null && s.getItem() == consumeFrom)
			{
				return s;
			}
		}
		return null;
	}

	public static IInventory getBaubles(EntityPlayer player)
	{
		if (!Loader.isModLoaded("Baubles"))
		{
			return null;
		} else
		{
			return BaublesApi.getBaubles(player);
		}
	}

	public static BlockPos getBlockLookingAt(EntityPlayer player, double maxDistance)
	{
		Pair<Vec3, Vec3> vecs = getLookVec(player, maxDistance);
		MovingObjectPosition mop = player.worldObj.rayTraceBlocks(vecs.getLeft(), vecs.getRight());
		if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
		{
			return mop.getBlockPos();

//	todo 1.8 merging 1.7 Sept 10 2015 - why is this different? ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
//			return Vec3.createVectorHelper(mop.blockX + dir.offsetX * 1.1, mop.blockY + dir.offsetY * 1.1, mop.blockZ + dir.offsetZ * 1.1);
//>>>>>>> 48eb5df9ac694f94c09c8be019f926a7412b3361
		}
		return null;
	}

	/**
	 * Returns a vec representing where the player is looking, capped at maxDistance away.
	 */
	public static Pair<Vec3, Vec3> getLookVec(EntityPlayer player, double maxDistance)
	{
		// Thank you ForgeEssentials
		Vec3 look = player.getLook(1.0F);
		Vec3 playerPos = new Vec3(player.posX, player.posY + (player.getEyeHeight() - player.getDefaultEyeHeight()), player.posZ);
		Vec3 src = playerPos.addVector(0, player.getEyeHeight(), 0);
		Vec3 dest = src.addVector(look.xCoord * maxDistance, look.yCoord * maxDistance, look.zCoord * maxDistance);
		return ImmutablePair.of(src, dest);
	}

	public static boolean hasBreakPermission(EntityPlayerMP player, BlockPos pos)
	{
		return hasEditPermission(player, pos)
				&& !(ForgeHooks.onBlockBreakEvent(player.worldObj, player.theItemInWorldManager.getGameType(), player, pos) == -1);
	}

	public static boolean hasEditPermission(EntityPlayerMP player, BlockPos pos)
	{
		return player.canPlayerEdit(pos, EnumFacing.NORTH, null) // todo 1.8 shim value, does this still work?
				&& !MinecraftServer.getServer().isBlockProtected(player.worldObj, pos, player);
	}


	public static void setPlayerFireImmunity(EntityPlayer player, boolean value)
	{
		ReflectionHelper.setEntityFireImmunity(player, value);
	}

	public static void setPlayerWalkSpeed(EntityPlayer player, float value)
	{
		ReflectionHelper.setPlayerCapabilityWalkspeed(player.capabilities, value);
	}

	public static void swingItem(EntityPlayer player)
	{
		if (player instanceof EntityPlayerMP)
		{
			PacketHandler.sendTo(new SwingItemPKT(), ((EntityPlayerMP) player));
		}
	}

	public static void updateClientServerFlight(EntityPlayerMP player, boolean state)
	{
		PacketHandler.sendTo(new SetFlyPKT(state), player);
		player.capabilities.allowFlying = state;

		if (!state)
		{
			player.capabilities.isFlying = false;
		}
	}

	public static void updateClientServerStepHeight(EntityPlayerMP player, float value)
	{
		player.stepHeight = value;
		PacketHandler.sendTo(new StepHeightPKT(value), player);
	}
}
