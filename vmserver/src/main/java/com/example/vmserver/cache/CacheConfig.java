package com.example.vmserver.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Configuration
@Transactional(readOnly = true)
@EnableCaching
public class CacheConfig {
    @Bean
    CacheManager cacheManager() {
        SimpleCacheManager scm = new SimpleCacheManager();
        scm.setCaches(Arrays.asList(
            new ConcurrentMapCache("VMStation"),
            new ConcurrentMapCache("VMStations"),
            new ConcurrentMapCache("VMUser"),
            new ConcurrentMapCache("VMUsers"),
            new ConcurrentMapCache("VMQueue") // Добавлен кэш для очереди
        ));
        return scm;
    }
}