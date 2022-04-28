package com.palmergames.bukkit.towny.hooks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class LuckPermsContexts implements ContextCalculator<Player> {
	
	private static final String RESIDENT_CONTEXT = "towny:resident";
	private static final String MAYOR_CONTEXT = "towny:mayor";
	private static final String KING_CONTEXT = "towny:king";
	private static final String INSIDETOWN_CONTEXT = "towny:insidetown";
	private static final String INSIDEOWNTOWN_CONTEXT = "towny:insideowntown";
	private static final String INSIDEOWNPLOT_CONTEXT = "towny:insideownplot";
	private static final String TOWN_RANK_CONTEXT = "towny:townrank";
	private static final String NATION_RANK_CONTEXT = "towny:nationrank";
	private static final String TOWN_CONTEXT = "towny:town";
	private static final String NATION_CONTEXT = "towny:nation";
	
	private static final List<String> booleanContexts = Arrays.asList(RESIDENT_CONTEXT, MAYOR_CONTEXT, KING_CONTEXT, INSIDETOWN_CONTEXT, INSIDEOWNTOWN_CONTEXT, INSIDEOWNPLOT_CONTEXT);
	
	private static LuckPerms luckPerms;

	public LuckPermsContexts() {
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (provider != null) {
			luckPerms = provider.getProvider();
			luckPerms.getContextManager().registerCalculator(this);
		}
	}
	
	@Override
	public void calculate(@NotNull Player player, @NotNull ContextConsumer contextConsumer) {
		Resident resident = TownyAPI.getInstance().getResident(player);
		if (resident == null)
			return;
			
		for (String townrank : resident.getTownRanks()) contextConsumer.accept(TOWN_RANK_CONTEXT, townrank);
		for (String nationrank : resident.getNationRanks()) contextConsumer.accept(NATION_RANK_CONTEXT, nationrank);
		Town town = resident.getTownOrNull();
		if(town != null) {
			contextConsumer.accept(TOWN_CONTEXT, town.getName());
			// There is no point to check for nation if town is not registered
			Nation nation = resident.getNationOrNull();
			if(nation != null) {
				contextConsumer.accept(NATION_CONTEXT, nation.getName());
			}
		}
		
		contextConsumer.accept(RESIDENT_CONTEXT, Boolean.toString(resident.hasTown()));
		contextConsumer.accept(MAYOR_CONTEXT, Boolean.toString(resident.isMayor()));
		contextConsumer.accept(KING_CONTEXT, Boolean.toString(resident.isKing()));

		WorldCoord wc = PlayerCacheUtil.getCache(player).getLastTownBlock();
		if (wc == null || TownyAPI.getInstance().isWilderness(wc)) {
			contextConsumer.accept(INSIDETOWN_CONTEXT, "false");
			contextConsumer.accept(INSIDEOWNPLOT_CONTEXT, "false");
			contextConsumer.accept(INSIDEOWNTOWN_CONTEXT, "false");
		} else {
			contextConsumer.accept(INSIDETOWN_CONTEXT, "true");

			contextConsumer.accept(INSIDEOWNTOWN_CONTEXT, Boolean.toString(wc.getTownBlockOrNull().getTownOrNull().hasResident(resident)));
			contextConsumer.accept(INSIDEOWNPLOT_CONTEXT, Boolean.toString(wc.getTownBlockOrNull().hasResident() && wc.getTownBlockOrNull().getResidentOrNull().equals(resident)));
		}
	}

	@Override
	public ContextSet estimatePotentialContexts() {
		ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
		for (String context : booleanContexts) {
			builder.add(context, "true");
			builder.add(context, "false");
		}
		for (String nationrank : TownyPerms.getNationRanks()) builder.add(NATION_RANK_CONTEXT, nationrank);
		for (String townrank : TownyPerms.getTownRanks()) builder.add(TOWN_RANK_CONTEXT, townrank);
		for (Town town : TownyUniverse.getInstance().getTowns()) builder.add(TOWN_CONTEXT, town.getName());
		for (Nation nation : TownyUniverse.getInstance().getNations()) builder.add(NATION_CONTEXT, nation.getName());
		
		return builder.build();
	}
}
