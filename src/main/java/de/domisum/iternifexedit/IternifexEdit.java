package de.domisum.iternifexedit;

import de.domisum.iternifexedit.navmesh.edit.NavMeshEditCoordinator;
import de.domisum.lib.iternifex.navmesh.NavMeshRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class IternifexEdit extends JavaPlugin
{

	// EDITING
	private NavMeshRegistry navMeshRegistry;
	private NavMeshEditCoordinator navMeshEditCoordinator = null;


	// INITIALIZATION
	@Override
	public void onEnable()
	{
		navMeshRegistry = new NavMeshRegistry();

		navMeshEditCoordinator = new NavMeshEditCoordinator(this, navMeshRegistry);
		navMeshEditCoordinator.initialize();

		getLogger().info(getClass().getSimpleName()+" has been enabled");
	}

	@Override
	public void onDisable()
	{
		navMeshRegistry.save();

		if(navMeshEditCoordinator != null)
			navMeshEditCoordinator.terminate();

		getLogger().info(getClass().getSimpleName()+" has been disabled");
	}

}
