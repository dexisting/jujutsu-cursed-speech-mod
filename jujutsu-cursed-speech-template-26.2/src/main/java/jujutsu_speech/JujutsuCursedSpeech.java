package jujutsu_speech;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import com.mojang.brigadier.Command;

import static net.minecraft.server.command.CommandManager.literal;

public class JujutsuCursedSpeech implements ModInitializer {
	public static int throatRemaining = 0;
	public static int throatMaxDuration = 0;
	public static boolean throatAnnouncedReady = true;

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (throatRemaining > 0 && throatMaxDuration > 0) {
				int totalBlocks = 10;
				double progress = 1.0 - ((double) throatRemaining / throatMaxDuration);
				int greenBlocks = (int) Math.floor(progress * totalBlocks);
				StringBuilder bar = new StringBuilder();

				for (int i = 0; i < totalBlocks; i++) {
					if (i < greenBlocks) {
						bar.append("§a■");
					} else {
						bar.append("§c■");
					}
				}

				int secsLeft = (int) Math.ceil(throatRemaining / 20.0);
				for (var player : server.getPlayerManager().getPlayerList()) {
					player.sendMessage(Text.literal("§dThroat: " + bar + " §f" + secsLeft + "s"), true);
				}
				throatRemaining -= 5;
			} else if (!throatAnnouncedReady) {
				throatRemaining = 0;
				throatAnnouncedReady = true;
				for (var player : server.getPlayerManager().getPlayerList()) {
					player.sendMessage(Text.literal("§a【 THROAT READY 】"), true);
					player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.6f);
				}
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("dontmove").executes(context -> { castDontMove(context.getSource()); return Command.SINGLE_SUCCESS; }));
			dispatcher.register(literal("dm").executes(context -> { castDontMove(context.getSource()); return Command.SINGLE_SUCCESS; }));
			dispatcher.register(literal("heal").executes(context -> { castHeal(context.getSource()); return Command.SINGLE_SUCCESS; }));
			dispatcher.register(literal("h").executes(context -> { castHeal(context.getSource()); return Command.SINGLE_SUCCESS; }));
			dispatcher.register(literal("gear").executes(context -> { castGear(context.getSource()); return Command.SINGLE_SUCCESS; }));
		});
	}

	private boolean isThroatCoolingDown(ServerCommandSource source) {
		if (throatRemaining > 0) {
			var player = source.getPlayer();
			if (player != null) {
				player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0f, 0.5f);
			}
			return true;
		}
		return false;
	}

	private void triggerThroatCooldown(String moveName, int seconds) {
		throatMaxDuration = seconds * 20;
		throatRemaining = seconds * 20;
		throatAnnouncedReady = false;
	}

	private void castDontMove(ServerCommandSource source) {
		if (isThroatCoolingDown(source)) return;
		var player = source.getPlayer();
		if (player == null) return;

		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.0f, 1.5f);

		player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(14)).forEach(entity -> {
			if (entity instanceof net.minecraft.entity.LivingEntity living) {
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 255, true, false));
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 255, true, false));
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 255, true, false));
			}
		});

		player.damage(source.getWorld(), source.getWorld().getDamageSources().magic(), 4.0f);
		triggerThroatCooldown("DON'T MOVE", 4);
	}

	private void castHeal(ServerCommandSource source) {
		throatRemaining = 0;
		throatAnnouncedReady = true;
		var player = source.getPlayer();
		if (player == null) return;

		player.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 20, 1, true, false));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1, true, false));
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.2f);
		player.sendMessage(Text.literal("§a【 THROAT SOOTHED & READY 】"), true);
	}

	private void castGear(ServerCommandSource source) {
		var player = source.getPlayer();
		if (player == null) return;

		source.getServer().getCommandManager().executeWithPrefix(source, "give @p netherite_sword 1");
		source.getServer().getCommandManager().executeWithPrefix(source, "give @p netherite_helmet 1");
		source.getServer().getCommandManager().executeWithPrefix(source, "give @p netherite_chestplate 1");
		source.getServer().getCommandManager().executeWithPrefix(source, "give @p netherite_leggings 1");
		source.getServer().getCommandManager().executeWithPrefix(source, "give @p netherite_boots 1");
		source.getServer().getCommandManager().executeWithPrefix(source, "give @p golden_apple 16");

		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}
}