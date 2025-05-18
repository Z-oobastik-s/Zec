package com.zoobastiks.zec

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.ChatColor
import org.bukkit.Bukkit

/**
 * Команда /zec [страница]
 * Команда /zec admin view <игрок> [страница] - просмотр страницы инвентаря другого игрока
 */
class Cmd(private val gui: GUI) : CommandExecutor {
    
    override fun onCommand(s: CommandSender, c: Command, l: String, a: Array<out String>): Boolean {
        val config = gui.getConfig()
        
        if (s !is Player) {
            val message = ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.players-only") ?: "&cТолько для игроков!")
            s.sendMessage(message)
            return true
        }
        
        // Проверяем админские команды
        if (a.isNotEmpty() && a[0].equals("admin", ignoreCase = true)) {
            if (!s.hasPermission("zec.admin")) {
                val message = ChatColor.translateAlternateColorCodes('&', 
                    config.getString("messages.no-permission") ?: "&cУ вас нет доступа к этой команде!")
                s.sendMessage(message)
                return true
            }
            
            // Обработка подкоманд админа
            if (a.size > 1) {
                when (a[1].lowercase()) {
                    "view" -> {
                        // /zec admin view <игрок> [страница]
                        if (a.size < 3) {
                            s.sendMessage(ChatColor.RED.toString() + "Использование: /zec admin view <игрок> [страница]")
                            return true
                        }
                        
                        val targetName = a[2]
                        val targetPlayer = Bukkit.getPlayer(targetName)
                        
                        if (targetPlayer == null) {
                            // Проверяем оффлайн игрока
                            val offlinePlayer = Bukkit.getOfflinePlayer(targetName)
                            if (!offlinePlayer.hasPlayedBefore()) {
                                s.sendMessage(ChatColor.RED.toString() + "Игрок $targetName не найден!")
                                return true
                            }
                            
                            val pageId = if (a.size > 3) a[3].lowercase() else "main"
                            gui.openOfflinePlayerPage(s, offlinePlayer.uniqueId, pageId)
                        } else {
                            val pageId = if (a.size > 3) a[3].lowercase() else "main"
                            gui.openPlayerPage(s, targetPlayer, pageId)
                        }
                        
                        return true
                    }
                    "help" -> {
                        // Выводим справку по админ-командам
                        val adminCommands = listOf(
                            "&e/zec admin view <игрок> [страница] &7- просмотр инвентаря игрока",
                            "&e/zec admin help &7- эта справка"
                        )
                        
                        s.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6=== Команды администратора Zec ==="))
                        for (cmd in adminCommands) {
                            s.sendMessage(ChatColor.translateAlternateColorCodes('&', cmd))
                        }
                        
                        return true
                    }
                    else -> {
                        s.sendMessage(ChatColor.RED.toString() + "Неизвестная подкоманда. Используйте /zec admin help для справки.")
                        return true
                    }
                }
            } else {
                s.sendMessage(ChatColor.RED.toString() + "Использование: /zec admin <view|help>")
                return true
            }
        }
        
        // Обычные команды для игроков
        if (a.isEmpty()) {
            // Если не указана страница, открываем главную
            gui.open(s)
        } else {
            // Если указана страница, пытаемся открыть её напрямую
            val pageId = a[0].lowercase()
            
            if (s.hasPermission("zec.use.$pageId") || s.hasPermission("zec.admin")) {
                if (gui.openSpecificPage(s, pageId)) {
                    // Страница успешно открыта
                } else {
                    val message = ChatColor.translateAlternateColorCodes('&', 
                        config.getString("messages.page-not-exists") 
                            ?: "&cСтраница '%page%' не существует!")
                            .replace("%page%", pageId)
                    s.sendMessage(message)
                }
            } else {
                val message = ChatColor.translateAlternateColorCodes('&', 
                    config.getString("messages.no-permission") 
                        ?: "&cУ вас нет доступа к этой странице!")
                s.sendMessage(message)
            }
        }
        
        return true
    }
} 