package net.ehicks.eoi;

import java.util.ArrayList;
import java.util.List;

public class CachePreloader
{
    // example orderByClause for a films table: order by cinemang_rating desc, imdb_id nulls last
    public static void preload(DBMap dbMap, String orderByClause)
    {
        int i = 0;
        int limit = 1000;
        List result = EOI.executeQueryOneResult("select count(*) from " + dbMap.tableName, new ArrayList<>());
        long resultSize = (Long) result.get(0);

        long freeRamMb = getFreeRamMb();
        while (freeRamMb > 100 && i*limit < resultSize)
        {
            int offset = i * limit;
            EOI.executeQuery("select * from " + dbMap.tableName + " " + orderByClause + " limit 1000 offset " + offset);
            System.out.println("Loaded into cache " + dbMap.tableName + " " + offset + " to " + (offset + limit) + ". free ram:" + freeRamMb + "MB");
            System.out.println("--> cache now holds " + EOICache.cache.size() + " objects.");
            freeRamMb = getFreeRamMb();
            i++;
        }
    }

    private static long getFreeRamMb()
    {
        long maxMemory = Runtime.getRuntime().maxMemory() ;
        long allocatedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        return (maxMemory - allocatedMemory) / 1024 / 1024;
    }
}
