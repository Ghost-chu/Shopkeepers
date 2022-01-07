package com.nisovin.shopkeepers.commands.shopkeepers.snapshot;

import java.util.function.Supplier;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperSnapshot;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

class CommandSnapshotCreate extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_SNAPSHOT_NAME = "snapshot-name";

	CommandSnapshotCreate() {
		super("create");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SNAPSHOT_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSnapshotCreate);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER),
				TargetShopkeeperFilter.ANY
		));
		// Snapshot names can consist of multiple words (i.e. contain spaces), but only when created programmatically
		// via the API. Via command, the snapshot name cannot contain spaces.
		this.addArgument(new StringArgument(ARGUMENT_SNAPSHOT_NAME));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null;
		String snapshotName = context.get(ARGUMENT_SNAPSHOT_NAME);
		assert snapshotName != null && !snapshotName.isEmpty();

		if (snapshotName.length() > ShopkeeperSnapshot.getMaxNameLength()) {
			TextUtils.sendMessage(sender, Messages.snapshotNameTooLong,
					"maxLength", ShopkeeperSnapshot.getMaxNameLength(),
					"name", snapshotName
			);
			return;
		}
		if (!ShopkeeperSnapshot.isNameValid(snapshotName)) {
			TextUtils.sendMessage(sender, Messages.snapshotNameInvalid,
					"name", snapshotName
			);
			return;
		}

		if (shopkeeper.getSnapshot(snapshotName) != null) {
			TextUtils.sendMessage(sender, Messages.snapshotNameAlreadyExists, "name", snapshotName);
			return;
		}

		ShopkeeperSnapshot snapshot = shopkeeper.createSnapshot(snapshotName);
		shopkeeper.addSnapshot(snapshot);
		shopkeeper.save();

		TextUtils.sendMessage(sender, Messages.snapshotCreated,
				"name", snapshotName,
				"id", shopkeeper.getSnapshots().size(),
				"timestamp", (Supplier<?>) () -> DerivedSettings.dateTimeFormatter.format(snapshot.getTimestamp())
		);
	}
}
