package net.ehicks.eoi;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class EOICache
{
    public static ConcurrentMap<String, SoftReference> cache = new ConcurrentHashMap<>();

    public static AtomicInteger hits = new AtomicInteger();
    public static AtomicInteger misses = new AtomicInteger();
    public static AtomicInteger keyHitObjectMiss = new AtomicInteger();

    public static Object get(String key)
    {
        if (cache.containsKey(key))
        {
            SoftReference softReference = cache.get(key);

            if (softReference.get() != null)
            {
                hits.incrementAndGet();
                return softReference.get();
            }
            else
            {
                keyHitObjectMiss.incrementAndGet();
                cache.remove(key);
                return null;
            }
        }
        else
        {
            misses.incrementAndGet();
            return null;
        }
    }

    public static void set(Object object)
    {
        cache.put(object.toString(), new SoftReference(object));
    }

    public static void unset(Object object)
    {
        cache.remove(object.toString());
    }

    public static int getKeysWithNoValue()
    {
        int count = 0;
        for (String key : cache.keySet())
        {
            SoftReference value = cache.get(key);
            if (value.get() == null)
                count++;
        }
        return count;
    }
}
