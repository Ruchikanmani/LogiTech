package me.matl114.logitech.utils.UtilClass.ItemClass;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemCounter implements Cloneable {
    protected long cnt;
    protected boolean dirty;
    protected ItemStack item;
    protected ItemMeta meta = null;
    protected long maxStackCnt;
    // when -1,means item is up to date
    private int cachedItemAmount = -1;
    private static ItemCounter INSTANCE = new ItemCounter(new ItemStack(Material.STONE));

    protected ItemCounter(ItemStack item) {
        dirty = false;
        this.cnt = item.getAmount();
        this.cachedItemAmount = (int) this.cnt;
        this.item = item;
        this.maxStackCnt = item.getMaxStackSize();
        this.maxStackCnt = maxStackCnt <= 0 ? Long.MAX_VALUE : maxStackCnt;
    }

    protected void toNull() {
        item = null;
        meta = null;
        cnt = 0;
        dirty = false;
        itemChange();
        //
    }

    protected void fromSource(ItemCounter source, boolean overrideMaxSize) {
        item = source.getItem();
        if (overrideMaxSize) {
            maxStackCnt = item != null ? item.getMaxStackSize() : 0L;
        }
        cnt = 0L;
        meta = null;
        cachedItemAmount = -1;
        //
    }

    protected void itemChange() {
        cachedItemAmount = item != null ? item.getAmount() : 0;
    }

    public ItemCounter() {
        dirty = false;
    }

    public static ItemCounter get(ItemStack item) {
        ItemCounter consumer = INSTANCE.clone();
        consumer.init(item);
        return consumer;
    }

    protected void init(ItemStack item) {
        this.dirty = false;
        this.item = item;
        this.cnt = item.getAmount();
        this.maxStackCnt = item.getMaxStackSize();
        this.maxStackCnt = maxStackCnt <= 0 ? Long.MAX_VALUE : maxStackCnt;
        this.cachedItemAmount = (int) cnt;
    }

    protected void init() {
        this.dirty = false;
        this.cnt = 0L;
        this.item = null;
        this.maxStackCnt = 0L;
        this.cachedItemAmount = 0;
    }

    public long getMaxStackCnt() {
        return maxStackCnt;
    }

    public boolean isNull() {
        return item == null;
    }

    public final boolean isFull() {
        return cnt >= this.maxStackCnt;
    }

    public final boolean isEmpty() {
        return cnt <= 0;
    }
    /**
     * get meta info ,if havn't get ,getItemMeta() clone one
     * @return
     */
    public ItemMeta getMeta() {
        if (item.hasItemMeta()) {
            if (meta == null) {
                meta = item.getItemMeta();
            }
            return meta;
        }
        return null;
    }

    /**
     * make sure you know what you are doing!
     * @param meta
     */
    public void setMeta(ItemMeta meta) {
        this.meta = meta;
    }

    /**
     * get dirty bits
     * @return
     */
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean t) {
        this.dirty = t;
    }
    /**
     * void constructor
     */

    /**
     * get item,should be read-only! and will not represent real amount
     * @return
     */
    public ItemStack getItem() {
        return item;
    }
    /**
     * modify recorded amount
     * @param amount
     */
    public void setAmount(long amount) {
        dirty = dirty || amount != cnt;
        cnt = amount;
    }

    public void setAmount(int amount) {
        setAmount((long) amount);
    }
    /**
     * get recorded amount
     */
    public int getAmount() {
        return cnt > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cnt;
    }

    public long getAmountLong() {
        return cnt;
    }

    /**
     * modify recorded amount
     * @param amount
     */
    public void addAmount(long amount) {
        cnt += amount;
        dirty = dirty || (amount != 0);
    }

    public void addAmount(int amount) {
        addAmount((long) amount);
    }

    /**
     * will sync amount and other data ,override by subclasses
     */
    public void syncData() {
        if (dirty) {
            cnt = item.getAmount();
            dirty = false;
        }
    }

    /**
     * will only sync amount,keep the rest of data unchanged
     */
    public void syncAmount() {
        if (dirty) {
            cnt = item.getAmount();
            dirty = false;
        }
    }

    /**
     * update amount of real itemstack ,or amount of real storage.etc
     */
    public void updateItemStack() {
        if (dirty) {
            // check if cachedItemAmount is not refreshed
            if (cachedItemAmount < 0) {
                item.setAmount((int) Math.min(cnt, Integer.MAX_VALUE));
            } else {
                int newCachedItemAmount = item.getAmount();
                cnt += newCachedItemAmount - cachedItemAmount;
                item.setAmount((int) Math.min(cnt, Integer.MAX_VALUE));
            }
            cachedItemAmount = (int) Math.min(cnt, Integer.MAX_VALUE);

            dirty = false;
        }
    }

    /**
     * consume other counter ,till one of them got zero
     * @param other
     */
    public void consume(ItemCounter other) {
        if (cnt < 0) { // stop when cnt < 0 (storage) no meaning
            return;
        }
        long diff = (other.getAmountLong() > cnt) ? cnt : other.getAmountLong();
        cnt -= diff;
        dirty = true;
        other.addAmount(-diff);
    }

    /**
     * grab other counter till maxSize or sth
     * @param other
     */
    public void grab(ItemCounter other) {
        cnt += other.getAmountLong();
        dirty = true;
        other.setAmount(0L);
    }

    /**
     * push to other counter till maxsize or sth
     * @param other
     */
    public void push(ItemCounter other) {
        other.grab(this);
    }

    protected ItemCounter clone() {
        ItemCounter clone = null;
        try {
            clone = (ItemCounter) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return clone;
    }
}
