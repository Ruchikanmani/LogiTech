package me.matl114.logitech.utils.UtilClass.StorageClass;

import me.matl114.logitech.utils.MathUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;

public class LocationStorageProxy extends ItemStorageCache {
    Location location;
    boolean lock = false;
    long lastStorageAmount;

    protected LocationStorageProxy(
          ItemStack item, ItemStack source, ItemMeta sourceMeta, int saveslot, StorageType type, Location location) {
        super(null, item, source, sourceMeta, saveslot, type);
        this.location = location;
        this.lastStorageAmount = ((LocationProxy) storageType).getAmount(location);
    }

    public boolean isDirty() {
        return lock || dirty;
    }

    public void updateStorage() {
        // 这里是代理存储 并不是唯一修改源
        // 需要刷新一下并加上用当前记录-历史记录 的东西
        long locationNowAmount = ((LocationProxy) storageType).getAmount(location);
        long delta = locationNowAmount - this.lastStorageAmount;
        long locationSetAmount = Math.min(getMaxStackCnt(), getStorageAmountLong() + delta);
        if (locationSetAmount < 0) {
            // async catcher
            //            Debug.logger("Catch Async Operation in cache Location :",location);
            //            Debug.logger("Using Amount now:",locationNowAmount,"Amount Record:",lastStorageAmount,"now
            // Amount:",getStorageAmount(),"Set value:",locationSetAmount);
        }
        ((LocationProxy) storageType).setAmount(location, locationSetAmount);
        ((LocationProxy) storageType).updateLocation(location);
    }

    public boolean isNotValid(){
        return this.deprecated;
    }

    public void updateMenu(@Nonnull BlockMenu menu) {
        if (getItem() != null && !getItem().getType().isAir()) {
            updateItemStack();
            updateStorage();
        }
        dirty = false;
    }

    public Location getProxyLocation() {
        return location;
    }
}
