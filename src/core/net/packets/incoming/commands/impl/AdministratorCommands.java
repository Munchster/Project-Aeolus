package core.net.packets.incoming.commands.impl;

import core.Server;
import core.game.GameConstants;
import core.game.content.Spellbook;
import core.game.model.entity.mob.MobDefinition;
import core.game.model.entity.mob.WalkType;
import core.game.model.entity.player.Player;
import core.game.model.entity.player.PlayerHandler;
import core.game.model.entity.player.Punishments;
import core.game.model.entity.player.Rights;
import core.game.model.item.ItemDefinition;
import core.net.packets.incoming.commands.Command;

/**
 * A class which will be the container of all the commands accessed by
 * Administrators and Developers but restricted to Players.
 * 
 * @author 7Winds
 */
public class AdministratorCommands implements Command {

	@Override
	public void execute(Player player, String[] command) {
		switch (command[0]) {
		case "test":
			player.sendMessage("I have access to Admin commands.");
			break;

		case "home":
			player.getMovement().movePlayer(GameConstants.RESPAWN_X,
					GameConstants.RESPAWN_Y, 0);
			break;

		case "spellbook":
			try {
				int id = Integer.parseInt(command[1]);
				switch (id) {

				case 0:
					player.getPA().setSpellbook(Spellbook.MODERN);
					break;

				case 1:
					player.getPA().setSpellbook(Spellbook.ANCIENT);
					break;

				case 2:
					player.getPA().setSpellbook(Spellbook.LUNAR);
					break;
				}

			} catch (NumberFormatException ex) {
				ex.printStackTrace();
			}
			break;

		case "teletome":
			try {
				String playerToTele = command[1];
				for (int i = 0; i < GameConstants.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToTele)) {
							Player player2 = PlayerHandler.players[i];
							if (player2.inWild()) {
								player.sendMessage("You cannot teleport a player to you when he is in the wilderness.");
								return;
							}
							if (player2.duelStatus == 5) {
								player.sendMessage("You cannot teleport a player to you when he is during a duel.");
								return;
							}
							if (player.inWild()) {
								player.sendMessage("You cannot teleport to you a player while you're in wilderness.");
								return;
							}
							player2.teleportToX = player.absX;
							player2.teleportToY = player.absY;
							player2.heightLevel = player.heightLevel;
							player.sendMessage("You have teleported "
									+ player2.playerName + " to you.");
							player2.sendMessage("You have been teleported to "
									+ player.playerName + "");
						}
					}
				}
			} catch (Exception e) {
				player.sendMessage("Player Must Be Offline.");
			}
			break;

		case "xteleto":
			String name = command[0].substring(8);
			for (int i = 0; i < GameConstants.MAX_PLAYERS; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(name)) {
						Player player2 = PlayerHandler.players[i];
						if (player2.inWild()) {
							player.sendMessage("The player you tried teleporting to is in the wilderness.");
							return;
						}
						if (player.inWild()) {
							player.sendMessage("You cannot teleport to a player while you're in the wilderness");
							return;
						}
						if (player.duelStatus == 5) {
							player.sendMessage("You cannot teleport to a player during a duel.");
							return;
						}
						player.getMovement().movePlayer(
								PlayerHandler.players[i].getX(),
								PlayerHandler.players[i].getY(),
								player.heightLevel);
					}
				}
			}
			break;

		case "setlvl":
			int skill = Integer.parseInt(command[1]);
			int level = Integer.parseInt(command[2]);
			if (level > 99) {
				level = 99;
				player.sendMessage("You cannot exceed a level higher than 99.");
			}
			if (level < 1) {
				level = 1;
				player.sendMessage("You cannot set your level below 1.");
			}
			if ((skill == player.playerHitpoints) && level < 10) {
				level = 10;
				player.sendMessage("You cannot set your health below 10.");
			}
			player.playerXP[skill] = player.getActionSender().getXPForLevel(
					level) + 5;
			player.playerLevel[skill] = player.getActionSender().getLevelForXP(
					player.playerXP[skill]);
			player.getActionSender().refreshSkill(skill);
			player.updateRequired = true;
			break;

		case "sendhome":
			try {
				String playerToBan = command[0].substring(9);
				for (int i = 0; i < GameConstants.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							Player player2 = PlayerHandler.players[i];
							player2.teleportToX = 3365;
							player2.teleportToY = 3265;
							player2.heightLevel = player.heightLevel;
							player.sendMessage("You have teleported "
									+ player2.playerName + " to home");
							player2.sendMessage("You have been teleported to home");
						}
					}
				}
			} catch (Exception e) {
				player.sendMessage("Player Must Be Offline.");
			}
			break;

		case "tele":
			String[] arg = command;
			if (arg.length > 3)
				player.getMovement().movePlayer(Integer.parseInt(arg[1]),
						Integer.parseInt(arg[2]), Integer.parseInt(arg[3]));
			else if (arg.length == 3)
				player.getMovement().movePlayer(Integer.parseInt(arg[1]),
						Integer.parseInt(arg[2]), player.heightLevel);
			break;

		case "item":
			try {
				if (command.length == 3) {
					int newItemID = Integer.parseInt(command[1]);
					int newItemAmount = Integer.parseInt(command[2]);
					if ((newItemID <= GameConstants.ITEM_LIMIT)
							&& (newItemID >= 0)) {
						player.getInventory().addItem(newItemID, newItemAmount);
					} else {
						player.sendMessage("No such item.");
					}
				} else {
					player.sendMessage("Use as ::item 995 200");
				}
			} catch (Exception e) {
				e.getMessage();
			}
			break;

		case "getitem":
			String args[] = command;
			String nameOfItem = "";
			int results = 0;
			for (int i = 1; i < args.length; i++) {
				nameOfItem = nameOfItem + args[i] + " ";
			}
			nameOfItem = nameOfItem.substring(0, nameOfItem.length() - 1);
			player.sendMessage("Searching: " + nameOfItem);
			for (int j = 0; j < GameConstants.ITEM_LIMIT; j++) {
				if (ItemDefinition.getDefinitions() != null) {
					if (player.getEquipment().getItemName(j).replace("_", " ")
							.toLowerCase().contains(nameOfItem.toLowerCase())) {
						player.sendMessage("<col=255>"
								+ player.getEquipment().getItemName(j)
										.replace("_", " ") + " - "
								+ player.getItems().getItemId(j));
						results++;
					}
				}
			}
			player.sendMessage(results + " results found...");
			break;

		case "getnpc":
			String nameOfNpc = "";
			int result = 0;
			for (int i = 1; i < command.length; i++) {
				nameOfNpc = nameOfNpc + command[i] + " ";
			}
			nameOfNpc = nameOfNpc.substring(0, nameOfNpc.length() - 1);
			player.sendMessage("Searching: " + nameOfNpc);
			for (int i = 0; i < GameConstants.MAX_LISTED_NPCS; i++) {
				if (MobDefinition.getDefinitions() != null) {
					String npcName = MobDefinition.getDefinitions()[i]
							.getName();
					if (npcName.replace("_", " ").toLowerCase()
							.contains(nameOfNpc.toLowerCase())) {
						player.sendMessage("<col=255>"
								+ npcName.replace("_", " ") + " - " + i);
						result++;
					}
				}
			}
			player.sendMessage(result + " results found...");
			break;

		case "bank":
			player.getActionSender().openUpBank();
			break;

		case "mypos":
			player.sendMessage("X: " + player.absX);
			player.sendMessage("Y: " + player.absY);
			player.sendMessage("H: " + player.heightLevel);
			break;

		case "npc":
			try {
				int newNPC = Integer.parseInt(command[1]);
				if (newNPC >= 0) {
					Server.npcHandler.spawnNpc(player, newNPC, player.absX,
							player.absY, 0, WalkType.SOUTH, 120, 7, 70, 70,
							false, false);
					player.sendMessage("You spawned a temporary npc.");
				} else {
					player.sendMessage("No such npc.");
				}
			} catch (Exception e) {

			}
			break;

		case "ipban":
			try {
				String playerToBan = command[0].substring(6);
				for (int i = 0; i < GameConstants.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							if (player.playerName == PlayerHandler.players[i].playerName) {
								player.sendMessage("You cannot IP Ban yourself.");
							}
							if (player.playerName.equalsIgnoreCase("mod wind")) {
								player.sendMessage("You can't use this command on this player!");
								return;
							} else {
								if (!Punishments
										.isIpBanned(PlayerHandler.players[i].connectedFrom)) {
									Punishments
											.addIpToBanList(PlayerHandler.players[i].connectedFrom);
									Punishments
											.addIpToFile(PlayerHandler.players[i].connectedFrom);
									player.sendMessage("You have IP banned the user: "
											+ PlayerHandler.players[i].playerName
											+ " with the host: "
											+ PlayerHandler.players[i].connectedFrom);
									PlayerHandler.players[i].disconnected = true;
								} else {
									player.sendMessage("This user is already IP Banned.");
								}
							}
						}
					}
				}
			} catch (Exception e) {
				player.sendMessage("Player Must Be Offline.");
			}
			break;

		case "info":
			String playerVar = command[0].substring(5);
			for (int i = 0; i < GameConstants.MAX_PLAYERS; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(playerVar)) {
						player.sendMessage("ip: "
								+ PlayerHandler.players[i].connectedFrom);
					}
				}
			}
			break;

		case "ban":
			try {
				String playerToBan = command[0].substring(4);
				Punishments.addNameToBanList(playerToBan);
				Punishments.addNameToFile(playerToBan);
				for (int i = 0; i < GameConstants.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							PlayerHandler.players[i].disconnected = true;
						}
					}

				}
			} catch (Exception e) {
				player.sendMessage("Player Must Be Offline.");
			}
			break;

		case "unban":
			try {
				String playerToBan = command[0].substring(6);
				Punishments.removeNameFromBanList(playerToBan);
				player.sendMessage(playerToBan + " has been unbanned.");
			} catch (Exception e) {
				player.sendMessage("Player Must Be Offline.");
			}
			break;

		case "kick":
			try {
				String playerToBan = command[0].substring(5);
				for (int i = 0; i < GameConstants.MAX_PLAYERS; i++) {
					Player player2 = PlayerHandler.players[i];
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							PlayerHandler.players[i].disconnected = true;
						}
					}
					if (player2.playerName.equalsIgnoreCase("mod sunny")) {
						player.sendMessage("You can't use this command on this player!");
						return;
					}
				}
			} catch (Exception e) {
				player.sendMessage("Player Must Be Offline.");
			}
			break;

		case "maxhitr":
			player.sendMessage("Your current maxhit is: "
					+ player.getCombat().calculateRangeAttack());
			break;

		case "maxhitmelee":
			player.sendMessage("Your current maxhit is: "
					+ player.getCombat().calculateMeleeMaxHit());
			break;

		case "maxhitmagic":
			// player.sendMessage("Your current maxhit is: " +
			// player.getCombat().ca);
			break;
		}
	}

	@Override
	public Rights getRights() {
		return Rights.ADMINISTRATOR;
	}
}
