package org.gestern.gringotts.dependency.towny.town;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.GringottsAccount;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.accountholder.AccountHolderProvider;
import org.gestern.gringotts.event.VaultCreationEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The type Town holder provider.
 */
public class TownHolderProvider implements AccountHolderProvider, Listener {
    private final Gringotts gringotts;

    /**
     * Instantiates a new Town holder provider.
     *
     * @param gringotts the gringotts
     */
    public TownHolderProvider(Gringotts gringotts) {
        this.gringotts = gringotts;
    }

    /**
     * Get the AccountHolder object mapped to the given id for this provider.
     *
     * @param id id of account holder
     * @return account holder for id
     */
    @Override
    public AccountHolder getAccountHolder(@NotNull String id) {
        try {
            return getAccountHolder(UUID.fromString(id));
        } catch (IllegalArgumentException ignored) {
            if (id.startsWith(VaultCreationEvent.Type.TOWN.getId() + "-")) {
                try {
                    return getAccountHolder(
                            TownyAPI.getInstance()
                                    .getDataSource()
                                    .getTown(id.substring(5))
                    );
                } catch (NotRegisteredException ignored1) {
                }
            } else {
                try {
                    return getAccountHolder(TownyAPI.getInstance().getDataSource().getTown(id));
                } catch (NotRegisteredException ignored1) {
                }
            }
        }

        return null;
    }

    /**
     * Get the AccountHolder object mapped to the given id for this provider.
     *
     * @param uuid id of account holder
     * @return account holder for id
     */
    @Override
    public AccountHolder getAccountHolder(@NotNull UUID uuid) {
        try {
            return getAccountHolder(TownyAPI.getInstance().getDataSource().getTown(uuid));
        } catch (NotRegisteredException ignored) {
        }

        return null;
    }

    /**
     * Get a TownyAccountHolder for the town of which player is a resident, if any.
     *
     * @param player player to get town for
     * @return TownyAccountHolder for the town of which player is a resident, if any. null otherwise.
     */
    @Override
    public AccountHolder getAccountHolder(@NotNull OfflinePlayer player) {
        try {
            Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

            if (resident == null) {
                return null;
            }

            Town town = resident.getTown();

            return getAccountHolder(town);
        } catch (NotRegisteredException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    @Override
    public VaultCreationEvent.Type getType() {
        return VaultCreationEvent.Type.TOWN;
    }

    /**
     * Gets account names.
     *
     * @return the account names
     */
    @Override
    public Set<String> getAccountNames() {
        return TownyAPI.getInstance()
                .getDataSource()
                .getTowns()
                .stream()
                .map(TownyObject::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Gets account holder.
     *
     * @param town the town
     * @return the account holder
     */
    public AccountHolder getAccountHolder(Town town) {
        return new TownAccountHolder(town);
    }

    /**
     * Rename town.
     *
     * @param event the event
     */
    @EventHandler
    public void renameTown(RenameTownEvent event) {
        Town town = event.getTown();

        AccountHolder holder = this.getAccountHolder(town);

        GringottsAccount account = this.gringotts.getAccounting().getAccount(holder);

        this.gringotts.getDao().retrieveChests(account).forEach(AccountChest::updateSign);
    }
}
