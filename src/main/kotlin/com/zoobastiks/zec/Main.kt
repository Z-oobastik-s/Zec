package com.zoobastiks.zec

import org.bukkit.plugin.java.JavaPlugin

/**
 * Основной класс плагина
 */
class Main : JavaPlugin() {
    
    /**
     * Вызывается при включении плагина
     */
    override fun onEnable() {
        // Сохраняем конфигурацию по умолчанию, если файл не существует
        saveDefaultConfig()
        
        // Инициализируем GUI
        val gui = GUI(this)
        
        // Регистрируем команду
        getCommand("zec")?.setExecutor(Cmd(gui))
        
        logger.info("Zec загружен!")
    }
    
    /**
     * Вызывается при выключении плагина
     */
    override fun onDisable() {
        logger.info("Zec выключен!")
    }
} 