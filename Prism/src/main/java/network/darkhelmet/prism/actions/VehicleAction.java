package network.darkhelmet.prism.actions;

import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.api.ChangeResult;
import network.darkhelmet.prism.api.ChangeResultType;
import network.darkhelmet.prism.api.PrismParameters;
import network.darkhelmet.prism.appliers.ChangeResultImpl;
import network.darkhelmet.prism.utils.EntityUtils;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.entity.minecart.StorageMinecart;

public class VehicleAction extends GenericAction {

    private static byte serverMajorVersion = Prism.getInstance().getServerMajorVersion();

    private VehicleActionData actionData;

    /**
     * Set the vehicle.
     * @param vehicle Entity
     */
    public void setVehicle(Entity vehicle) {

        actionData = new VehicleActionData();
        if (vehicle instanceof CommandMinecart) {
            actionData.vehicleName = "命令方块矿车";
        } else if (vehicle instanceof ExplosiveMinecart) {
            actionData.vehicleName = "TNT矿车";
        } else if (vehicle instanceof HopperMinecart) {
            actionData.vehicleName = "漏斗矿车";
        } else if (vehicle instanceof PoweredMinecart) {
            actionData.vehicleName = "动力矿车";
        } else if (vehicle instanceof RideableMinecart) {
            actionData.vehicleName = "矿车";
        } else if (vehicle instanceof SpawnerMinecart) {
            actionData.vehicleName = "刷怪笼矿车";
        } else if (vehicle instanceof StorageMinecart) {
            actionData.vehicleName = "运输矿车";
        } else if (vehicle instanceof Boat) {
            actionData.vehicleName = "船";
        } else {
            actionData.vehicleName = vehicle.getType().name().toLowerCase();
        }

        if (vehicle instanceof Boat) {
            actionData.woodType = EntityUtils.treeSpeciesToName(((Boat) vehicle).getWoodType());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNiceName() {
        return (actionData.woodType != null ? actionData.woodType + " " : "") + actionData.vehicleName;
    }

    @Override
    public boolean hasExtraData() {
        return true;
    }

    @Override
    public String serialize() {
        return gson().toJson(actionData);
    }

    @Override
    public void deserialize(String data) {
        if (data.startsWith("{")) {
            actionData = gson().fromJson(data, VehicleActionData.class);
        } else {
            // Old version support
            actionData = new VehicleActionData();
            actionData.vehicleName = data;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeResult applyRollback(Player player, PrismParameters parameters, boolean isPreview) {
        Entity vehicle = null;
        switch (actionData.vehicleName) {
            case "命令方块矿车":
                vehicle = getWorld().spawn(getLoc(), CommandMinecart.class);
                break;
            case "动力矿车":
                vehicle = getWorld().spawn(getLoc(), PoweredMinecart.class);
                break;
            case "运输矿车":
                vehicle = getWorld().spawn(getLoc(), StorageMinecart.class);
                break;
            case "TNT矿车":
                vehicle = getWorld().spawn(getLoc(), ExplosiveMinecart.class);
                break;
            case "刷怪笼矿车":
                vehicle = getWorld().spawn(getLoc(), SpawnerMinecart.class);
                break;
            case "漏斗矿车":
                vehicle = getWorld().spawn(getLoc(), HopperMinecart.class);
                break;
            case "矿车":
                vehicle = getWorld().spawn(getLoc(), Minecart.class);
                break;
            case "船":
                vehicle = getWorld().spawn(getLoc(), Boat.class);
                break;
            default:
                //null
        }
        if (vehicle == null) {
            return new ChangeResultImpl(ChangeResultType.SKIPPED, null);
        }

        if (vehicle instanceof Boat && actionData.woodType != null) {
            ((Boat) vehicle).setWoodType(EntityUtils.nameToTreeSpecies(actionData.woodType));
        }

        return new ChangeResultImpl(ChangeResultType.APPLIED, null);
    }

    public static class VehicleActionData {
        String vehicleName;
        String woodType;
    }
}
