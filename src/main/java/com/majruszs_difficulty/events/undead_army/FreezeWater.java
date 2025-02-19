package com.majruszs_difficulty.events.undead_army;

import com.majruszs_difficulty.RegistryHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Freezing water for undead entities. It is required because when Army spawn in water then entities just begin to drown. */
@Mod.EventBusSubscriber
public class FreezeWater {
	@SubscribeEvent
	public static void freezeNearby( LivingEvent.LivingUpdateEvent event ) {
		if( !isEntityValid( event.getEntityLiving() ) )
			return;

		MonsterEntity monster = ( MonsterEntity )event.getEntityLiving();
		ServerWorld world = ( ServerWorld )monster.world;
		BlockPos position = new BlockPos( monster.getPositionVec() );

		BlockState blockState = Blocks.FROSTED_ICE.getDefaultState();
		double factor = 3.0;
		BlockPos.Mutable mutablePosition = new BlockPos.Mutable();
		Iterable< BlockPos > blockPositions = BlockPos.getAllInBoxMutable( position.add( -factor, -1.0, -factor ),
			position.add( factor, -1.0, factor )
		);

		for( BlockPos blockPosition : blockPositions ) {
			if( !blockPosition.withinDistance( monster.getPositionVec(), factor ) )
				continue;

			mutablePosition.setPos( blockPosition.getX(), blockPosition.getY() + 1.0, blockPosition.getZ() );
			BlockState blockAboveState = world.getBlockState( mutablePosition );

			if( !blockAboveState.isAir( world, mutablePosition ) )
				continue;

			BlockState currentBlockState = world.getBlockState( blockPosition );

			boolean isFull = currentBlockState.getBlock() == Blocks.WATER && currentBlockState.get( FlowingFluidBlock.LEVEL ) == 0;
			if( !isFull )
				continue;

			boolean isWater = currentBlockState.getMaterial() == Material.WATER;
			if( !isWater )
				continue;

			boolean isValid = blockState.isValidPosition( world, blockPosition ) && world.placedBlockCollides( blockState, blockPosition,
				ISelectionContext.dummy()
			);
			if( !isValid )
				continue;

			boolean hasPlacingSucceeded = !net.minecraftforge.event.ForgeEventFactory.onBlockPlace( monster,
				net.minecraftforge.common.util.BlockSnapshot.create( world.getDimensionKey(), world, blockPosition ), net.minecraft.util.Direction.UP
			);
			if( !hasPlacingSucceeded )
				continue;

			world.setBlockState( blockPosition, blockState );
			world.getPendingBlockTicks()
				.scheduleTick( blockPosition, Blocks.FROSTED_ICE, MathHelper.nextInt( monster.getRNG(), 10, 20 ) );
		}
	}

	/** Checks whether entity belongs to Undead Army. */
	protected static boolean isEntityValid( LivingEntity entity ) {
		if( !( entity instanceof MonsterEntity ) )
			return false;

		return UndeadArmy.doesEntityBelongToUndeadArmy( entity ) && entity.isServerWorld();
	}
}
