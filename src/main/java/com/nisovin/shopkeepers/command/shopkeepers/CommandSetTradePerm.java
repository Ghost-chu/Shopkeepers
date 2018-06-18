package com.nisovin.shopkeepers.command.shopkeepers;

import java.util.Arrays;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandException;
import com.nisovin.shopkeepers.command.lib.CommandInput;
import com.nisovin.shopkeepers.command.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.command.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.command.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.command.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.command.shopkeepers.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.util.Utils;

class CommandSetTradePerm extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_NEW_PERMISSION = "perm";
	private static final String ARGUMENT_REMOVE_PERMISSION = "-";
	private static final String ARGUMENT_QUERY_PERMISSION = "?";

	CommandSetTradePerm() {
		super(Arrays.asList("setTradePerm"));

		// set permission:
		this.setPermission(ShopkeepersPlugin.SETTRADEPERM_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionSettradeperm);

		// arguments:
		this.addArgument(new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, (shopkeeper) -> shopkeeper instanceof AdminShopkeeper));
		this.addArgument(new OptionalArgument(new FirstOfArgument("permArg", Arrays.asList(
				new LiteralArgument(ARGUMENT_QUERY_PERMISSION),
				new LiteralArgument(ARGUMENT_REMOVE_PERMISSION),
				new StringArgument(ARGUMENT_NEW_PERMISSION)), true, true)));
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null && shopkeeper instanceof AdminShopkeeper;
		String newTradePerm = context.get(ARGUMENT_NEW_PERMISSION);
		boolean removePerm = context.has(ARGUMENT_REMOVE_PERMISSION);

		if (removePerm) {
			// remove trade permission:
			assert newTradePerm == null;
			Utils.sendMessage(player, Settings.msgTradePermRemoved);
		} else if (newTradePerm != null) {
			// set trade permission:
			Utils.sendMessage(player, Settings.msgTradePermSet);
		} else {
			// display current trade permission:
			String currentTradePerm = ((AdminShopkeeper) shopkeeper).getTradePremission();
			if (currentTradePerm == null) currentTradePerm = "-";
			Utils.sendMessage(player, Settings.msgTradePermView, "{perm}", currentTradePerm);
			return;
		}

		// set trade permission:
		((AdminShopkeeper) shopkeeper).setTradePermission(newTradePerm);

		// save:
		shopkeeper.save();
	}
}
