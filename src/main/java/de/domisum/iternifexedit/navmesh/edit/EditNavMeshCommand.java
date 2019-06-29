package de.domisum.iternifexedit.navmesh.edit;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
class EditNavMeshCommand implements CommandExecutor
{

	// DEPENDENCIES
	private final NavMeshEditCoordinator navMeshEditCoordinator;


	// EXECUTION
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(!(sender instanceof Player))
		{
			sender.sendMessage("This command can only be used by players!");
			return true;
		}

		Player player = (Player) sender;
		navMeshEditCoordinator.executeCommand(player, args);

		return true;
	}

}