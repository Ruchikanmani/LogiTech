package me.matl114.logitech.utils.UtilClass.StorageClass;

import com.google.common.base.Preconditions;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.matl114.logitech.core.Cargo.StorageMachines.AbstractIOPort;
import me.matl114.logitech.core.Cargo.Storages;
import me.matl114.logitech.manager.ScheduleSave;
import me.matl114.logitech.utils.CraftUtils;
import me.matl114.logitech.utils.DataCache;
import me.matl114.logitech.utils.Debug;
import me.matl114.logitech.utils.MenuUtils;
import me.matl114.logitech.utils.UtilClass.ItemClass.ItemCounter;
import me.matl114.logitech.utils.UtilClass.ItemClass.ItemSlotPusher;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * when you load a storage cache from menu slot......
 * automatically deal with menu update ,call updateMenu at persistent=false mod to toggle save
 */
public class ItemStorageCache extends ItemSlotPusher { // extends ItemPusher
    /**
     * should be the reference to the exact slotItem in menu
     */
    // 由何种存储解析器所解析
    protected StorageType storageType;
    // 存储物品的物品源,记录一个CraftItemStack
    protected ItemStack source;
    // 存储物品的物品meta 记录着含有存储数据的pdc的meta
    protected ItemMeta sourceMeta;
    @Nullable
    protected final BlockMenu menu;
    // 这个成员是记录该cache是否是长期cache
    // 长期cache会被记录在Map中,并且代表着一个AbstractIOPort
    // 短期cache比如存储解析器产物
    protected boolean persistent = false;
    // 这个成员是记录存储数目的
    protected long storageAmount;
    // 这个成员是用来记录这个cache是否还可以使用的
    // 当玩家尝试从menu里取出存储物品时 该物品将被标记为deprecated 之后的远程访问将不能访问该存储cache
    boolean deprecated = false;
    //
    private static byte[] lock = new byte[0];
    public static final HashMap<Location, ItemStorageCache> cacheMap = new HashMap<>();

    public static void setCache(BlockMenu inv, ItemStorageCache cache) {
        synchronized (lock) {
            cacheMap.put(inv.getLocation(), cache);
        }
    }

    public static ItemStorageCache getCache(Location loc) {
        ItemStorageCache cache ;
        synchronized (lock) {
            cache = cacheMap.get(loc);
        }
        if(cache != null && cache.isNotValid()){
            removeCache(loc);
        }
        return cache;
    }

    public static ItemStorageCache removeCache(Location loc) {
        // prevent dupe from last record,
        AbstractIOPort.setStorageAmount(loc, -1L, false);
        synchronized (lock) {
            return cacheMap.remove(loc);
        }
    }

    static {
        ScheduleSave.addFinalTask(() -> {
            synchronized (lock) {
                Iterator<Map.Entry<Location, ItemStorageCache>> it = cacheMap.entrySet().iterator();
                while (it.hasNext()) {
                    var a = it.next();
                    if(a.getValue().isNotValid()){
                        AbstractIOPort.setStorageAmount(a.getKey(), -1L, false);
                        it.remove();
                    }else{
                        BlockMenu menu = DataCache.getMenu(a.getKey());
                        if (menu == a.getValue().menu) {
                            a.getValue().setPersistent(false);
                            a.getValue().updateMenu(menu);
                            a.getValue().setPersistent(true);
                        }else{
                            it.remove();
                        }
                    }
                }
            }
        });
        ScheduleSave.addPeriodicTask(() -> {});

        Storages.setup();
    }

    /**
     * will try get ,if null, create a possible one or failed return null
     * if type not specific, auto search
     * @param source
     * @param sourceMeta
     * @param saveSlot
     * @return
     */

    // 首先要保证数据安全!
    public static ItemStorageCache getOrCreate(BlockMenu menu, ItemStack source, ItemMeta sourceMeta, int saveSlot) {
        return getOrCreate(menu, source, sourceMeta, saveSlot, i -> true);
    }

    public static ItemStorageCache getOrCreate(
            BlockMenu menu, ItemStack source, ItemMeta sourceMeta, int saveSlot, Predicate<StorageType> filter) {
        StorageType type = StorageType.getStorageType(sourceMeta, filter);
        if(type != null){
            ItemStorageCache getCache = getWithoutCheck(menu, source, sourceMeta, saveSlot, type);
            if(getCache != null){
                return getCache;
            }
        }

        String id = CraftUtils.parseSfId(sourceMeta);
        SlimefunItem it = id == null ? null : SlimefunItem.getById(id);
        type = StorageType.getPossibleStorageType(it, filter);
        if (type == null) {
            return null;
        } else {
            var newInstance = new ItemStorageCache(menu, source, saveSlot, type);
            return newInstance.isNotValid()? null : newInstance;
        }

    }

    /**
     * will search storage content already have
     * if type not specific, auto search
     * @param source
     * @param sourceMeta
     * @param saveSlot
     * @return
     */
    public static ItemStorageCache getTempCache(ItemStack source, ItemMeta sourceMeta, int saveSlot) {
        return getTempCache(source, sourceMeta, saveSlot, i -> true);
    }

    public static ItemStorageCache getTempCache(
          ItemStack source, ItemMeta sourceMeta, int saveSlot, Predicate<StorageType> filter) {
        StorageType type = StorageType.getStorageType(sourceMeta, filter);
        if (type == null) {
            return null;
        }
        return getWithoutCheck(null, source, sourceMeta, saveSlot, type);
    }


    public static ItemStorageCache getWithoutCheck(
     @Nullable   BlockMenu inv, ItemStack source, ItemMeta sourceMeta, int saveSlot, @Nonnull StorageType type) {
        if (type instanceof LocationProxy lp) {
            Location loc = lp.getLocation(sourceMeta);
            ItemStack stored = lp.getItemStack(loc);
            if (stored == null) {
                return null;
            }
            ItemStorageCache cache = new LocationStorageProxy(stored, source, sourceMeta, saveSlot, type, loc);
            cache.setAmount(lp.getAmount(loc));
            cache.storageAmount = cache.getAmountLong();
            cache.dirty = false;
            return cache;
        }
        ItemStack stored = type.getStorageContent(sourceMeta);
        if (stored == null) {
            return null;
        }
        ItemStorageCache tmp = new ItemStorageCache(inv, stored, source, sourceMeta, saveSlot, type);
        tmp.setAmount(type.getStorageAmount(sourceMeta));
        tmp.storageAmount = tmp.getAmountLong();
        tmp.dirty = false;
        if(tmp.isNotValid()){
            return null;
        }
        return tmp;
    }
    /**
     * construct common
     * @param item
     * @param source
     * @param sourceMeta
     * @param saveslot
     */
    protected ItemStorageCache(BlockMenu menu, ItemStack item, ItemStack source, ItemMeta sourceMeta, int saveslot, StorageType type) {
        super(item, saveslot);
        Preconditions.checkArgument(source != null, "Item Storage cache source should not be null!");
        this.source = source;
        this.sourceMeta = sourceMeta;
        this.storageType = type;
        this.maxStackCnt = type.getStorageMaxSize(this.sourceMeta);
        this.menu = menu;
    }

    protected ItemStorageCache(BlockMenu nv, ItemStack item, ItemStack source, int saveslot, StorageType type) {
        this(nv, item, source, source.getItemMeta(), saveslot, type);
    }
    /**
     * construct when storage = null potential
     * @param source
     * @param slot
     * @param type
     */
    protected ItemStorageCache(BlockMenu menu, ItemStack source, int slot, StorageType type) {
        super(slot);
        this.source = source;
        this.sourceMeta = source.getItemMeta();
        this.storageType = type;
        this.maxStackCnt = type.getStorageMaxSize(sourceMeta);
        this.menu = menu;
    }



    public int getStorageAmount() {
        return this.storageAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) this.storageAmount;
    }

    public long getStorageAmountLong() {
        return this.storageAmount;
    }

    /**
     * only const cache can set this true
     * @param persistent
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
        this.dirty = true;
    }

    /**
     * set related slot num
     * @param slot
     */
    public void setSaveSlot(int slot) {
        this.slot = slot;
    }

    public void setDeprecate(boolean deprecated) {
        this.deprecated = deprecated;
        this.dirty = true;
    }

    public boolean isNotValid() {
        if(this.menu == null)return this.deprecated;
        if(!this.deprecated && this.source.getAmount() == 1 && CraftUtils.sameCraftItem(menu.getItemInSlot(slot), this.source)){
            ItemStack stack = menu.getItemInSlot(slot);
            if(stack.getAmount() != 1){
                return true;
            }
            if(CraftUtils.sameCraftItem(stack, this.source)){
                return false;
            }else {
                ItemMeta meta = stack.getItemMeta();
                if(sourceMeta.getPersistentDataContainer().equals(stack.getItemMeta().getPersistentDataContainer())){
                    source = stack;
                    sourceMeta = meta;
                    this.dirty = true;
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * check if cache can continue bind on this item,or just a item change,
     * if continue bind, reset source to this item and return True,else return False
     * check if someOne tend to replace it with a similar one or add its amount or do sth else
     * @param item
     * @return
     */
    public boolean keepRelated(ItemStack item) {
        if (item == null) return false;
        if (item.getAmount() != 1) return false;
        else if (CraftUtils.sameCraftItem(item, source)) {
            return true;
        }
        // only check pdc, else ignored
        else if (sourceMeta
                .getPersistentDataContainer()
                .equals(item.getItemMeta().getPersistentDataContainer())) {
            source = item;
            this.dirty = true;
            return true;
        } else return false;
    }

    public void updateItemStack() {
        if (dirty) {
            if (wasNull) {
                if (getItem() != null) {
                    item = item.clone();
                    item.setAmount(1);
                    storageAmount = getAmountLong();
                    storageType.setStorage(sourceMeta, this.getItem());
                    wasNull = false;
                    itemChange();
                }
            }
            storageAmount = getAmountLong();
        }
    }

    /**
     * save data to sfdata
     * @param loc
     */
    public void syncLocation(Location loc) {
        try {
            AbstractIOPort.setStorageAmount(loc, storageAmount, true);
        } catch (Throwable e) {
            Debug.logger("存储cache与粘液机器不对应!位置[%s]疑似出现刷物行为,移除相应存储cache并抛出错误:"
                    .formatted(DataCache.locationToDisplayString(loc)));

            ItemStorageCache cache = removeCache(loc);
            cache.updateStorage();
            throw e;
        }
    }

    public void updateStorage() {
        storageType.onStorageAmountWrite(sourceMeta, storageAmount);
        storageType.onStorageDisplayWrite(sourceMeta, storageAmount);
        source.setItemMeta(sourceMeta);
    }

    public void updateMenu(@Nonnull BlockMenu menu) {
        if(this.menu != null && menu != this.menu){
            throw new RuntimeException("Illegal menu passed");
        }
        if (getItem() != null && !getItem().getType().isAir()) {
            updateItemStack();
            if (persistent) {
                syncLocation(menu.getLocation());
            }
            // make sync to source when needed, do not do when not needed
            if (menu.hasViewer() || !persistent) {
                updateStorage();
            }
            // 不是persistent 将物品的clone进行替换// 和保存有关
        }
        // for saving item data ,not for updating Menu
        if ((!persistent) && slot >= 0) {
            // not work?
            // in case player take it away while working
            if (keepRelated(menu.getItemInSlot(slot))) source = MenuUtils.syncSlot(menu, slot, source);
        }
        dirty = false;
    }

    public void syncData() {
        if (!wasNull) {
            if (dirty) {
                cnt = storageAmount;
                dirty = false;
            }
        } else {
            toNull();
        }
    }
    // 修复了setFrom存储时覆写maxSize的问题
    public void setFrom(ItemCounter source) {
        if (wasNull || (source != null && source.getItem() != null)) {
            fromSource(source, false);
        }
    }
}
