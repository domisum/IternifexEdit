package de.domisum.iternifexedit;

import de.domisum.iternifexedit.navmesh.edit.NavMeshEditCoordinator;
import org.bukkit.plugin.java.JavaPlugin;

public class IternifexEdit extends JavaPlugin
{

	// EDITING
	private NavMeshEditCoordinator navMeshEditCoordinator = null;


	// INITIALIZATION
	@Override
	public void onEnable()
	{
		navMeshEditCoordinator = new NavMeshEditCoordinator();
		navMeshEditCoordinator.initialize();

		getLogger().info(getClass().getSimpleName()+" has been enabled");
	}

	@Override
	public void onDisable()
	{
		saveAll();

		if(navMeshEditCoordinator != null)
			navMeshEditCoordinator.terminate();

		getLogger().info(getClass().getSimpleName()+" has been disabled");
	}


	// LOADING/SAVING
	private void saveAll()
	{
		// TODO
	}

}
