# Zec
## Многостраничное хранилище для Minecraft 1.21.4

### О плагине
Zec - плагин, который предоставляет игрокам удобное многостраничное хранилище с графическим интерфейсом в Minecraft. Плагин поддерживает систему разрешений для доступа к различным страницам хранилища.

### Особенности
- Многостраничное хранилище предметов
- Различные страницы для разных групп игроков
- Система прав доступа к страницам
- Администраторские функции для просмотра и редактирования инвентарей других игроков
- Простая и понятная конфигурация

### Команды
| Команда | Описание | Права |
|---------|----------|-------|
| `/zec` | Открыть основное хранилище | zec.use |
| `/zec [страница]` | Открыть указанную страницу хранилища | zec.use + zec.use.[страница] |
| `/zec admin view <игрок> [страница]` | Просмотр инвентаря другого игрока | zec.admin |
| `/zec admin help` | Справка по админ-командам | zec.admin |

### Права доступа
| Право | Описание |
|-------|----------|
| `zec.use` | Базовое право для использования команды /zec |
| `zec.use.main` | Доступ к основной странице хранилища |
| `zec.use.page1` | Доступ к дополнительной странице 1 |
| `zec.use.page2` | Доступ к дополнительной странице 2 |
| `zec.use.vip` | Доступ к VIP странице |
| `zec.admin` | Доступ ко всем страницам и админ-командам |
| `zec.admin.edit` | Право на редактирование чужих инвентарей |

### Установка
1. Скачайте последнюю версию плагина
2. Поместите JAR-файл в папку plugins вашего сервера
3. Перезапустите сервер или используйте плагин для загрузки плагинов
4. Настройте конфигурацию по желанию

### Конфигурация
В файле config.yml вы можете настроить:
- Размер и заголовок инвентарей
- Названия и размеры различных страниц хранилища
- Права доступа для каждой страницы
- Кнопки навигации между страницами
- Сообщения для игроков

### Совместимость
Плагин разработан для Minecraft 1.21.4 и может работать на следующих серверных платформах:
- Paper
- Spigot
- Bukkit

### Автор
**Zoobastiks** - [GitHub](https://github.com/Z-oobastik-s)

### Лицензия
Все права защищены © 2025 Zoobastiks 