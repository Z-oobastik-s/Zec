package com.zoobastiks.zec

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import java.util.HashMap
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.inventory.InventoryType

/**
 * Расширенный GUI с поддержкой множества страниц и проверкой разрешений
 */
class GUI(private val plugin: JavaPlugin) : Listener {
    
    // Конфигурация плагина
    private val config = plugin.config
    
    // Хранилище инвентарей игроков (UUID игрока -> ID страницы -> массив предметов)
    private val playerInventories = HashMap<UUID, HashMap<String, Array<ItemStack?>>>()
    
    // Файл для хранения данных
    private val storageFile = File(plugin.dataFolder, "storage.yml")
    private val storage = YamlConfiguration()
    
    // Текущая открытая страница для игрока
    private val openPages = HashMap<UUID, String>()
    
    // Информация о просматриваемом инвентаре (UUID просматривающего -> UUID просматриваемого)
    private val viewingOtherInventory = HashMap<UUID, UUID>()
    
    // Режим редактирования (UUID просматривающего -> true/false)
    private val editMode = HashMap<UUID, Boolean>()
    
    init {
        // Регистрируем слушатель событий
        plugin.server.pluginManager.registerEvents(this, plugin)
        
        // Создаем директорию плагина, если её нет
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }
        
        // Загружаем данные из файла
        loadStorage()
    }
    
    /**
     * Возвращает конфигурацию плагина для использования в других классах
     */
    fun getConfig(): FileConfiguration {
        return config
    }
    
    /**
     * Открывает главную страницу GUI для игрока
     */
    fun open(p: Player) {
        openPage(p, "main")
    }
    
    /**
     * Открывает указанную страницу напрямую через команду
     * @return true если страница существует и открыта, false иначе
     */
    fun openSpecificPage(p: Player, pageId: String): Boolean {
        // Проверяем, существует ли страница в конфиге
        if (config.contains("pages.$pageId")) {
            openPage(p, pageId)
            return true
        }
        
        return false
    }
    
    /**
     * Открывает страницу другого игрока (админская функция)
     */
    fun openPlayerPage(admin: Player, target: Player, pageId: String): Boolean {
        // Проверяем, существует ли страница в конфиге
        if (!config.contains("pages.$pageId")) {
            val message = ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.page-not-exists") 
                    ?: "&cСтраница '%page%' не существует!")
                    .replace("%page%", pageId)
            admin.sendMessage(message)
            return false
        }
        
        // Получаем настройки страницы из конфига
        val title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("messages.admin-view-title") ?: "&4[АДМИН] &r%player% - %page%")
                .replace("%player%", target.name)
                .replace("%page%", pageId)
        val size = config.getInt("pages.$pageId.size", 54)
        
        // Создаем инвентарь
        val inv = Bukkit.createInventory(null, size, title)
        
        // Загружаем предметы игрока
        if (!playerInventories.containsKey(target.uniqueId)) {
            playerInventories[target.uniqueId] = HashMap()
        }
        
        val playerPages = playerInventories[target.uniqueId]!!
        
        if (playerPages.containsKey(pageId)) {
            val items = playerPages[pageId]!!
            for (i in items.indices) {
                if (items[i] != null) {
                    inv.setItem(i, items[i])
                }
            }
        }
        
        // Добавляем кнопки администрирования
        addAdminButtons(inv, admin.hasPermission("zec.admin.edit"))
        
        // Открываем инвентарь админу
        admin.openInventory(inv)
        
        // Запоминаем, что админ просматривает чужой инвентарь
        openPages[admin.uniqueId] = "admin_view"
        viewingOtherInventory[admin.uniqueId] = target.uniqueId
        editMode[admin.uniqueId] = false
        
        return true
    }
    
    /**
     * Открывает страницу оффлайн игрока (админская функция)
     */
    fun openOfflinePlayerPage(admin: Player, targetUUID: UUID, pageId: String): Boolean {
        // Проверяем, существует ли страница в конфиге
        if (!config.contains("pages.$pageId")) {
            val message = ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.page-not-exists") 
                    ?: "&cСтраница '%page%' не существует!")
                    .replace("%page%", pageId)
            admin.sendMessage(message)
            return false
        }
        
        // Проверяем, есть ли данные для этого игрока
        if (!playerInventories.containsKey(targetUUID) || !playerInventories[targetUUID]!!.containsKey(pageId)) {
            val targetName = Bukkit.getOfflinePlayer(targetUUID).name ?: targetUUID.toString()
            admin.sendMessage(ChatColor.RED.toString() + "У игрока " + targetName + " нет сохраненных данных для страницы " + pageId)
            return false
        }
        
        // Получаем имя игрока
        val targetName = Bukkit.getOfflinePlayer(targetUUID).name ?: targetUUID.toString()
        
        // Получаем настройки страницы из конфига
        val title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("messages.admin-view-title") ?: "&4[АДМИН] &r%player% - %page%")
                .replace("%player%", targetName)
                .replace("%page%", pageId)
        val size = config.getInt("pages.$pageId.size", 54)
        
        // Создаем инвентарь
        val inv = Bukkit.createInventory(null, size, title)
        
        // Загружаем предметы игрока
        val playerPages = playerInventories[targetUUID]!!
        
        val items = playerPages[pageId]!!
        for (i in items.indices) {
            if (items[i] != null) {
                inv.setItem(i, items[i])
            }
        }
        
        // Добавляем кнопки администрирования
        addAdminButtons(inv, admin.hasPermission("zec.admin.edit"))
        
        // Открываем инвентарь админу
        admin.openInventory(inv)
        
        // Запоминаем, что админ просматривает чужой инвентарь
        openPages[admin.uniqueId] = "admin_view"
        viewingOtherInventory[admin.uniqueId] = targetUUID
        editMode[admin.uniqueId] = false
        
        return true
    }
    
    /**
     * Добавляет кнопки администрирования в инвентарь
     */
    private fun addAdminButtons(inv: Inventory, canEdit: Boolean) {
        // Кнопка режима редактирования (если есть права)
        if (canEdit) {
            val editButton = ItemStack(Material.WRITABLE_BOOK)
            val meta = editButton.itemMeta
            meta?.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lРежим редактирования"))
            val lore = listOf(
                ChatColor.translateAlternateColorCodes('&', "&7Нажмите, чтобы"),
                ChatColor.translateAlternateColorCodes('&', "&7включить/выключить режим редактирования"),
                ChatColor.translateAlternateColorCodes('&', "&cВнимание: в режиме редактирования"),
                ChatColor.translateAlternateColorCodes('&', "&cизменения сохраняются!")
            )
            meta?.lore = lore
            editButton.itemMeta = meta
            
            inv.setItem(8, editButton)
        }
        
        // Кнопка закрытия
        val closeButton = ItemStack(Material.BARRIER)
        val meta = closeButton.itemMeta
        meta?.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lЗакрыть"))
        val lore = listOf(
            ChatColor.translateAlternateColorCodes('&', "&7Нажмите, чтобы"),
            ChatColor.translateAlternateColorCodes('&', "&7закрыть инвентарь")
        )
        meta?.lore = lore
        closeButton.itemMeta = meta
        
        inv.setItem(0, closeButton)
    }
    
    /**
     * Открывает указанную страницу GUI для игрока
     */
    private fun openPage(p: Player, pageId: String) {
        // Проверяем, есть ли у игрока доступ к странице
        val permission = config.getString("pages.$pageId.permission") ?: "zec.use.$pageId"
        
        // Проверка с PermissionEx (если доступен)
        if (!hasPermission(p, permission)) {
            val message = ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.no-permission") ?: "&cУ вас нет доступа к этой странице!")
            p.sendMessage(message)
            return
        }
        
        // Получаем настройки страницы из конфига
        val title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("pages.$pageId.title") ?: "Инвентарь")
        val size = config.getInt("pages.$pageId.size", 54)
        
        // Создаем инвентарь
        val inv = Bukkit.createInventory(null, size, title)
        
        // Загружаем предметы, если они есть
        if (!playerInventories.containsKey(p.uniqueId)) {
            playerInventories[p.uniqueId] = HashMap()
        }
        
        val playerPages = playerInventories[p.uniqueId]!!
        
        if (playerPages.containsKey(pageId)) {
            val items = playerPages[pageId]!!
            for (i in items.indices) {
                if (items[i] != null) {
                    inv.setItem(i, items[i])
                }
            }
        }
        
        // Добавляем кнопки навигации
        addNavigationButtons(inv, pageId)
        
        // Открываем инвентарь игроку
        p.openInventory(inv)
        
        // Запоминаем открытую страницу
        openPages[p.uniqueId] = pageId
        // Сбрасываем информацию о просмотре чужого инвентаря
        viewingOtherInventory.remove(p.uniqueId)
        editMode.remove(p.uniqueId)
    }
    
    /**
     * Добавляет кнопки навигации в инвентарь
     */
    private fun addNavigationButtons(inv: Inventory, currentPage: String) {
        val pages = config.getConfigurationSection("pages")?.getKeys(false)?.toList() ?: return
        val currentIndex = pages.indexOf(currentPage)
        
        // Кнопка предыдущей страницы
        if (currentIndex > 0) {
            val prevConfig = config.getConfigurationSection("buttons.previous_page")
            if (prevConfig != null) {
                val material = Material.valueOf(prevConfig.getString("material", "ARROW")!!)
                val position = prevConfig.getInt("position", 45)
                val name = ChatColor.translateAlternateColorCodes('&', prevConfig.getString("name", "&aПредыдущая страница")!!)
                val lore = prevConfig.getStringList("lore").map { ChatColor.translateAlternateColorCodes('&', it) }
                
                val prevButton = createButton(material, name, lore)
                inv.setItem(position, prevButton)
            }
        }
        
        // Кнопка следующей страницы
        if (currentIndex < pages.size - 1) {
            val nextConfig = config.getConfigurationSection("buttons.next_page")
            if (nextConfig != null) {
                val material = Material.valueOf(nextConfig.getString("material", "ARROW")!!)
                val position = nextConfig.getInt("position", 53)
                val name = ChatColor.translateAlternateColorCodes('&', nextConfig.getString("name", "&aСледующая страница")!!)
                val lore = nextConfig.getStringList("lore").map { ChatColor.translateAlternateColorCodes('&', it) }
                
                val nextButton = createButton(material, name, lore)
                inv.setItem(position, nextButton)
            }
        }
    }
    
    /**
     * Создает кнопку для навигации
     */
    private fun createButton(material: Material, name: String, lore: List<String>): ItemStack {
        val button = ItemStack(material)
        val meta = button.itemMeta
        
        meta?.setDisplayName(name)
        meta?.lore = lore
        button.itemMeta = meta
        
        return button
    }
    
    /**
     * Проверяет, есть ли у игрока разрешение (с поддержкой PermissionEx)
     */
    private fun hasPermission(p: Player, permission: String): Boolean {
        // Сначала проверяем с использованием Bukkit API
        if (p.hasPermission(permission)) {
            return true
        }
        
        // Пробуем проверить через PermissionEx
        try {
            val pexPlugin = Bukkit.getPluginManager().getPlugin("PermissionsEx")
            if (pexPlugin != null && pexPlugin.isEnabled) {
                val pexClass = Class.forName("ru.tehkode.permissions.bukkit.PermissionsEx")
                val getPermissionManager = pexClass.getMethod("getPermissionManager")
                val permManager = getPermissionManager.invoke(null)
                
                val userClass = Class.forName("ru.tehkode.permissions.PermissionManager")
                val getUser = userClass.getMethod("getUser", String::class.java)
                val user = getUser.invoke(permManager, p.name)
                
                val permUserClass = Class.forName("ru.tehkode.permissions.PermissionUser")
                val has = permUserClass.getMethod("has", String::class.java)
                
                return has.invoke(user, permission) as Boolean
            }
        } catch (e: Exception) {
            // Если возникла ошибка, используем результат Bukkit API
            plugin.logger.warning("Не удалось проверить разрешение через PermissionEx: ${e.message}")
        }
        
        return false
    }
    
    /**
     * Обрабатывает клики в инвентаре
     */
    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val p = e.whoClicked as? Player ?: return
        val currentPage = openPages[p.uniqueId] ?: return
        
        // Проверяем, что это наш GUI
        val title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("pages.$currentPage.title") ?: "Инвентарь")
            
        if (e.view.title != title) return
        
        // Получаем настройки навигационных кнопок
        val prevPos = config.getInt("buttons.previous_page.position", 45)
        val nextPos = config.getInt("buttons.next_page.position", 53)
        
        // Проверяем клик по кнопкам навигации
        if (e.rawSlot == prevPos || e.rawSlot == nextPos) {
            e.isCancelled = true
            
            val pages = config.getConfigurationSection("pages")?.getKeys(false)?.toList() ?: return
            val currentIndex = pages.indexOf(currentPage)
            
            if (e.rawSlot == prevPos && currentIndex > 0) {
                // Переход на предыдущую страницу
                saveCurrentPageItems(p, currentPage, e.inventory)
                openPage(p, pages[currentIndex - 1])
            } else if (e.rawSlot == nextPos && currentIndex < pages.size - 1) {
                // Переход на следующую страницу
                saveCurrentPageItems(p, currentPage, e.inventory)
                openPage(p, pages[currentIndex + 1])
            }
        }
        
        // Для остальных действий разрешаем стандартное поведение
    }
    
    /**
     * Сохраняет предметы текущей страницы перед переходом
     */
    private fun saveCurrentPageItems(p: Player, pageId: String, inv: Inventory) {
        val size = config.getInt("pages.$pageId.size", 54)
        val items = arrayOfNulls<ItemStack>(size)
        
        for (i in 0 until size) {
            // Пропускаем позиции навигационных кнопок
            val prevPos = config.getInt("buttons.previous_page.position", 45)
            val nextPos = config.getInt("buttons.next_page.position", 53)
            
            if (i != prevPos && i != nextPos) {
                items[i] = inv.getItem(i)
            }
        }
        
        // Сохраняем в памяти
        if (!playerInventories.containsKey(p.uniqueId)) {
            playerInventories[p.uniqueId] = HashMap()
        }
        
        playerInventories[p.uniqueId]!![pageId] = items
    }
    
    /**
     * Обрабатывает закрытие инвентаря
     */
    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        val p = e.player as? Player ?: return
        val currentPage = openPages[p.uniqueId] ?: return
        
        // Проверяем, что это наш GUI
        val title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("pages.$currentPage.title") ?: "Инвентарь")
            
        if (e.view.title != title) return
        
        // Сохраняем содержимое инвентаря
        saveCurrentPageItems(p, currentPage, e.inventory)
        
        // Сохраняем в файл
        savePlayerInventories(p.uniqueId)
        saveStorage()
        
        // Удаляем из активных страниц
        openPages.remove(p.uniqueId)
    }
    
    /**
     * Сохраняет все инвентари игрока в конфигурацию
     */
    private fun savePlayerInventories(uuid: UUID) {
        val playerPages = playerInventories[uuid] ?: return
        
        for ((pageId, items) in playerPages) {
            // Проверяем, пустой ли инвентарь
            var isEmpty = true
            for (item in items) {
                if (item != null) {
                    isEmpty = false
                    break
                }
            }
            
            if (isEmpty) {
                // Если инвентарь пуст, удаляем запись
                storage.set("inventories.$uuid.$pageId", null)
                continue
            }
            
            // Сериализуем предметы
            val encodedItems = ArrayList<String>()
            for (i in items.indices) {
                val item = items[i]
                if (item != null) {
                    try {
                        // Сохраняем позицию и предмет
                        val baos = ByteArrayOutputStream()
                        val bos = BukkitObjectOutputStream(baos)
                        bos.writeObject(item)
                        bos.flush()
                        
                        // Кодируем в Base64
                        val serialized = Base64.getEncoder().encodeToString(baos.toByteArray())
                        encodedItems.add("$i:$serialized")
                        
                        bos.close()
                        baos.close()
                    } catch (e: Exception) {
                        plugin.logger.warning("Не удалось сериализовать предмет: ${e.message}")
                    }
                }
            }
            
            // Сохраняем в конфигурацию
            storage.set("inventories.$uuid.$pageId", encodedItems)
        }
    }
    
    /**
     * Сохраняет все данные в файл
     */
    private fun saveStorage() {
        try {
            storage.save(storageFile)
        } catch (e: Exception) {
            plugin.logger.severe("Не удалось сохранить storage.yml: ${e.message}")
        }
    }
    
    /**
     * Загружает данные из файла
     */
    private fun loadStorage() {
        if (storageFile.exists()) {
            try {
                storage.load(storageFile)
                
                // Загружаем инвентари игроков
                val playersSection = storage.getConfigurationSection("inventories")
                if (playersSection != null) {
                    for (uuidString in playersSection.getKeys(false)) {
                        val uuid = UUID.fromString(uuidString)
                        val pagesSection = storage.getConfigurationSection("inventories.$uuidString")
                        
                        if (pagesSection != null) {
                            val playerPages = HashMap<String, Array<ItemStack?>>()
                            
                            for (pageId in pagesSection.getKeys(false)) {
                                val encodedItems = storage.getStringList("inventories.$uuidString.$pageId")
                                val size = config.getInt("pages.$pageId.size", 54)
                                val items = arrayOfNulls<ItemStack>(size)
                                
                                for (encoded in encodedItems) {
                                    val parts = encoded.split(":", limit = 2)
                                    if (parts.size == 2) {
                                        val slot = parts[0].toInt()
                                        val serialized = parts[1]
                                        
                                        try {
                                            // Декодируем из Base64
                                            val data = Base64.getDecoder().decode(serialized)
                                            val bais = ByteArrayInputStream(data)
                                            val bis = BukkitObjectInputStream(bais)
                                            
                                            // Получаем предмет
                                            val item = bis.readObject() as ItemStack
                                            items[slot] = item
                                            
                                            bis.close()
                                            bais.close()
                                        } catch (e: Exception) {
                                            plugin.logger.warning("Не удалось десериализовать предмет: ${e.message}")
                                        }
                                    }
                                }
                                
                                playerPages[pageId] = items
                            }
                            
                            playerInventories[uuid] = playerPages
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Не удалось загрузить storage.yml: ${e.message}")
            }
        }
    }
} 