package de.domisum.iternifexedit.navmesh.edit;

import de.domisum.iternifexedit.IternifexEdit;
import de.domisum.lib.compitum.CompitumLib;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;

class EditNavMeshCommand extends BukkitCommand
{

	// -------
	// CONSTRUCTOR
	// -------
	EditNavMeshCommand()
	{
		super("editNavMesh");

		description = "Used to edit the NavMeshes";
		usageMessage = "/editNavMesh";

		setAliases(new ArrayList<>());
	}


	// -------
	// EXECUTION
	// -------
	@Override
	public boolean execute(CommandSender sender, String alias, String[] args)
	{
		if(!(sender instanceof Player))
		{
			sender.sendMessage("This command can only be used by players!");
			return true;
		}

		Player player = (Player) sender;

		NavMeshEditCoordinator editManager = IternifexEdit.getNavMeshEditCoordinator();
		editManager.executeCommand(player, args);

		return true;
	}

}