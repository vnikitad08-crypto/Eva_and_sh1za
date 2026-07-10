package Grend.chatlogger.client.commands;

import com.mojang.brigadier.*;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.*;
import com.mojang.brigadier.tree.*;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import Grend.chatlogger.data.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import Grend.chatlogger.client.util.OnlineChecker;
import Grend.chatlogger.client.ModConfig;
import Grend.chatlogger.client.ClanHighlightConfig;
import Grend.chatlogger.client.gui.EvoChatGui;
import Grend.chatlogger.client.gui.RunesScreen;
import Grend.chatlogger.client.RunesManager;

/**
 * Команды мода EvoChat
 *
 * /chatlogger export [клан] - экспортировать данные в файл
 * /chatlogger list [клан] - показать список игроков (в чат)
 * /chatlogger clans - показать список всех кланов
 * /chatlogger clear - очистить все данные
 * /chatlogger count - показать количество игроков
 */
public class ModCommands {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Регистрирует все команды
     */
    public static void register(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> mainCommand = dispatcher.register(literal("chatlogger")
            .then(literal("export")
                .executes(ModCommands::exportAll)
                .then(argument("clan", greedyString())
                    .executes(ModCommands::exportClan)
                )
            )
            .then(literal("list")
                .executes(ModCommands::listAll)
                .then(argument("clan", greedyString())
                    .executes(ModCommands::listClan)
                )
            )
            .then(literal("clans")
                .executes(ModCommands::listClans)
            )
            .then(literal("clear")
                .executes(ModCommands::clear)
            )
            .then(literal("clean")
                .executes(ModCommands::cleanInvalid)
            )
            .then(literal("count")
                .executes(ModCommands::count)
            )
            .then(literal("check")
                .executes(ModCommands::checkAll)
                .then(argument("clan", greedyString())
                    .executes(ModCommands::checkClan)
                )
            )
            .then(literal("stop")
                .executes(ModCommands::stopCheck))
            .then(literal("config")
                .executes(ModCommands::config)
                .then(literal("tellCheck")
                    .executes(ModCommands::toggleTellCheck)
                )
                .then(literal("message")
                    .then(argument("msg", greedyString())
                        .executes(ModCommands::setTellMessage)
                    )
                )
                .then(literal("command")
                    .then(argument("cmd", greedyString())
                        .executes(ModCommands::setTellCommand)
                    )
                )
                .then(literal("reset")
                    .executes(ModCommands::resetConfig)
                )
            )
            .then(literal("add")
                .then(argument("player", StringArgumentType.word())
                    .then(argument("level", IntegerArgumentType.integer(1, 100))
                        .then(argument("clan", greedyString())
                            .executes(ModCommands::addPlayerToClanWithLevel)
                        )
                    )
                    .then(argument("clan", greedyString())
                        .executes(ModCommands::addPlayerToClan)
                    )
                )
            )
            .then(literal("remove")
                .then(argument("player", StringArgumentType.word())
                    .executes(ModCommands::removePlayerFromClan)
                )
            )
            .then(literal("highlight")
                .executes(ModCommands::highlightList)
                .then(literal("enable")
                    .executes(ModCommands::highlightEnable))
                .then(literal("disable")
                    .executes(ModCommands::highlightDisable))
                .then(literal("color")
                    .then(argument("clan", word())
                        .then(argument("color", greedyString())
                            .executes(ModCommands::highlightSetColor))))
                .then(literal("remove")
                    .then(argument("clan", word())
                        .executes(ModCommands::highlightRemoveColor)))
                .then(literal("list")
                    .executes(ModCommands::highlightList))
                .then(literal("debug")
                    .then(argument("player", word())
                        .executes(ModCommands::highlightDebug)))
                .then(literal("enemy")
                    .then(literal("enable").executes(ModCommands::enemyEnable))
                    .then(literal("disable").executes(ModCommands::enemyDisable))
                    .then(literal("color")
                        .then(argument("color", greedyString())
                            .executes(ModCommands::enemyColor))))
                .then(literal("friend")
                    .then(literal("add")
                        .then(argument("clan", word())
                            .executes(ModCommands::friendAdd)))
                    .then(literal("remove")
                        .then(argument("clan", word())
                            .executes(ModCommands::friendRemove)))
                    .then(literal("list").executes(ModCommands::friendList))))
            .then(literal("sendclan")
                .then(argument("clan", greedyString())
                    .executes(ModCommands::sendClan)))
            .then(literal("help")
                .executes(ModCommands::help)
            )
            .then(literal("gui")
                .executes(ModCommands::openGui)
            )
            .then(literal("runesbag")
                .executes(ModCommands::openRunesBag)
            )
            .then(literal("runeset")
                .then(argument("set", integer(1, 5))
                    .executes(ModCommands::applyRuneSet)
                )
            )
        );
    }

    private static int openGui(CommandContext<FabricClientCommandSource> context) {
        EvoChatGui.openMainScreen();
        sendFeedback("§a[EvoChat] Открываю GUI...");
        return Command.SINGLE_SUCCESS;
    }

    private static int openRunesBag(CommandContext<FabricClientCommandSource> context) {
        RunesManager.openRunesBag();
        sendFeedback("§a[EvoChat] Открытие мешка с рунами...");
        return Command.SINGLE_SUCCESS;
    }

    private static int applyRuneSet(CommandContext<FabricClientCommandSource> context) {
        int setIndex = IntegerArgumentType.getInteger(context, "set") - 1;
        RunesManager.applyRuneSet(setIndex);
        sendFeedback("§a[EvoChat] Применение сета рун #" + (setIndex + 1) + "...");
        return Command.SINGLE_SUCCESS;
    }

    private static int exportAll(CommandContext<FabricClientCommandSource> context) {
        DataManager manager = DataManager.getInstance();
        Path exportPath = manager.getExportPath();

        try {
            manager.exportToFile(exportPath);
            sendFeedback("§a[EvoChat] Данные экспортированы в " + exportPath.toAbsolutePath());
            sendFeedback("§7Всего игроков: " + manager.getPlayerCount());
        } catch (IOException e) {
            sendFeedback("§c[EvoChat] Ошибка экспорта: " + e.getMessage());
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int exportClan(CommandContext<FabricClientCommandSource> context) {
        String clan = StringArgumentType.getString(context, "clan");
        
        // Сначала проверяем онлайн игроков клана, затем экспортируем
        OnlineChecker.checkClan(clan, () -> {
            DataManager manager = DataManager.getInstance();
            Path exportPath = Paths.get("clan_" + clan + ".txt");

            try {
                manager.exportClanToFile(clan, exportPath);
                sendFeedback("§a[EvoChat] Данные клана '" + clan + "' экспортированы в " + exportPath.toAbsolutePath());
            } catch (IOException e) {
                sendFeedback("§c[EvoChat] Ошибка экспорта: " + e.getMessage());
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int listAll(CommandContext<FabricClientCommandSource> context) {
        DataManager manager = DataManager.getInstance();

        if (manager.getPlayerCount() == 0) {
            sendFeedback("§7[EvoChat] Нет собранных игроков");
            return Command.SINGLE_SUCCESS;
        }

        sendFeedback("§a[EvoChat] Всего игроков: " + manager.getPlayerCount());

        // Показываем первые 10 игроков
        int count = 0;
        for (PlayerData player : manager.getAllPlayers()) {
            if (count >= 10) break;
            sendFeedback("§7  " + player.toString());
            count++;
        }

        if (manager.getPlayerCount() > 10) {
            sendFeedback("§7... и ещё " + (manager.getPlayerCount() - 10) + " игроков. Используйте /chatlogger export");
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int listClan(CommandContext<FabricClientCommandSource> context) {
        String clan = StringArgumentType.getString(context, "clan");

        DataManager manager = DataManager.getInstance();
        var updatedPlayers = manager.getPlayersByClan(clan);

        if (updatedPlayers.isEmpty()) {
            sendFeedback("§7[EvoChat] Клан '" + clan + "' не найден или пуст");
            return Command.SINGLE_SUCCESS;
        }

        sendFeedback("§a[EvoChat] Клан '" + clan + "' - " + updatedPlayers.size() + " игроков:");

        int onlineCount = 0;
        for (PlayerData player : updatedPlayers) {
            if (player.isOnline()) onlineCount++;
            String status = player.isOnline() ? "§aОНЛАЙН" : "§7оффлайн";
            sendFeedback("§7  " + player.getNickname() + " [" + player.getLevel() + "] - " + status);
        }

        sendFeedback("§7Онлайн: " + onlineCount + " / " + updatedPlayers.size());

        return Command.SINGLE_SUCCESS;
    }

    private static int listClans(CommandContext<FabricClientCommandSource> context) {
        DataManager manager = DataManager.getInstance();
        var clans = manager.getAllClans();

        if (clans.isEmpty()) {
            sendFeedback("§7[EvoChat] Нет собранных кланов");
            return Command.SINGLE_SUCCESS;
        }

        sendFeedback("§a[EvoChat] Всего кланов: " + clans.size());

        for (String clan : clans) {
            int playerCount = manager.getPlayersByClan(clan).size();
            sendFeedback("§7  [" + clan + "] - " + playerCount + " игроков");
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandContext<FabricClientCommandSource> context) {
        DataManager.getInstance().clear();
        sendFeedback("§a[EvoChat] Данные очищены");
        return Command.SINGLE_SUCCESS;
    }

    private static int cleanInvalid(CommandContext<FabricClientCommandSource> context) {
        DataManager manager = DataManager.getInstance();
        int removed = 0;
        
        // Собираем ники для удаления (числовые)
        List<String> toRemove = new ArrayList<>();
        for (var player : manager.getAllPlayers()) {
            String nick = player.getNickname();
            if (nick.matches("^\\d+$") || nick.length() > 16) {
                toRemove.add(nick);
            }
        }
        
        // Удаляем
        for (String nick : toRemove) {
            manager.removePlayer(nick);
            removed++;
        }
        
        sendFeedback("§a[EvoChat] Удалено §b" + removed + " §aневерных записей");
        return Command.SINGLE_SUCCESS;
    }

    private static int count(CommandContext<FabricClientCommandSource> context) {
        DataManager manager = DataManager.getInstance();
        sendFeedback("§a[EvoChat] Всего игроков: " + manager.getPlayerCount());
        sendFeedback("§a[EvoChat] Всего кланов: " + manager.getAllClans().size());
        return Command.SINGLE_SUCCESS;
    }

    private static int help(CommandContext<FabricClientCommandSource> context) {
        sendFeedback("§a=== EvoChat Commands ===");
        sendFeedback("§7/chatlogger export [клан] - экспортировать данные в файл");
        sendFeedback("§7/chatlogger list [клан] - показать игроков");
        sendFeedback("§7/chatlogger clans - список всех кланов");
        sendFeedback("§7/chatlogger count - количество игроков");
        sendFeedback("§7/chatlogger clear - очистить все данные");
        sendFeedback("§7/chatlogger clean - удалить неверные ники (числа)");
        sendFeedback("§7/chatlogger check [клан] - проверить онлайн клана");
        sendFeedback("§7/chatlogger check - проверить всех игроков");
        sendFeedback("§7/chatlogger stop - остановить проверку онлайна");
        sendFeedback("§7/chatlogger add <игрок> <клан> [уровень] - добавить в клан");
        sendFeedback("§7/chatlogger remove <игрок> - удалить из клана");
        sendFeedback("§7/chatlogger config - показать настройки");
        sendFeedback("§7/chatlogger config message <текст> - изменить сообщение");
        sendFeedback("§7/chatlogger config command <команда> - изменить команду");
        sendFeedback("§7/chatlogger config reset - сбросить настройки");
        sendFeedback("§7/chatlogger highlight - настройки подсветки кланов");
        sendFeedback("§7/chatlogger highlight debug <игрок> - отладка подсветки");
        sendFeedback("§7/chatlogger highlight enemy enable|disable - подсветка врагов");
        sendFeedback("§7/chatlogger highlight enemy color <цвет> - цвет врагов");
        sendFeedback("§7/chatlogger highlight friend add|remove|list [клан] - дружественные кланы");
        sendFeedback("§7/chatlogger sendclan <клан> - отправить состав в клан-чат");
        sendFeedback("§7/chatlogger gui - открыть GUI меню мода");
        sendFeedback("§7/chatlogger runesbag - открыть мешок с рунами");
        sendFeedback("§7/chatlogger runeset <1-5> - применить сет рун");
        return Command.SINGLE_SUCCESS;
    }

    private static int checkAll(CommandContext<FabricClientCommandSource> context) {
        OnlineChecker.checkAllPlayers();
        return Command.SINGLE_SUCCESS;
    }

    private static int checkClan(CommandContext<FabricClientCommandSource> context) {
        String clan = StringArgumentType.getString(context, "clan");
        OnlineChecker.checkClan(clan, null);
        sendFeedback("§7[EvoChat] Проверка клана: " + clan);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopCheck(CommandContext<FabricClientCommandSource> context) {
        OnlineChecker.stopChecking();
        return Command.SINGLE_SUCCESS;
    }

    private static int config(CommandContext<FabricClientCommandSource> context) {
        ModConfig config = ModConfig.getInstance();
        sendFeedback("§a=== EvoChat Настройки ===");
        sendFeedback("§7Команда для ЛС: /" + config.getTellCommand() + " <игрок> <сообщение>");
        sendFeedback("§7Сообщение для проверки: " + config.getTellMessage());
        sendFeedback("§7/chatlogger check - проверить всех игроков");
        sendFeedback("§7/chatlogger check <клан> - проверить онлайн клана");
        sendFeedback("§7/chatlogger list <клан> - показать клан (без проверки)");
        sendFeedback("§7/chatlogger export <клан> - проверить и экспортировать");
        sendFeedback("§7/chatlogger config reset - сбросить настройки");
        return Command.SINGLE_SUCCESS;
    }

    private static int toggleTellCheck(CommandContext<FabricClientCommandSource> context) {
        sendFeedback("§a[EvoChat] Проверка через ЛС активна.");
        return Command.SINGLE_SUCCESS;
    }

    private static int setTellMessage(CommandContext<FabricClientCommandSource> context) {
        String msg = StringArgumentType.getString(context, "msg");
        ModConfig.getInstance().setTellMessage(msg);
        sendFeedback("§a[EvoChat] Сообщение для проверки установлено: " + msg);
        return Command.SINGLE_SUCCESS;
    }

    private static int setTellCommand(CommandContext<FabricClientCommandSource> context) {
        String cmd = StringArgumentType.getString(context, "cmd");
        ModConfig.getInstance().setTellCommand(cmd);
        sendFeedback("§a[EvoChat] Команда для ЛС установлена: /" + cmd);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetConfig(CommandContext<FabricClientCommandSource> context) {
        ModConfig config = ModConfig.getInstance();
        config.setTellCommand("m");
        config.setTellMessage("1");
        sendFeedback("§a[EvoChat] Настройки сброшены к значениям по умолчанию");
        return Command.SINGLE_SUCCESS;
    }

    private static int addPlayerToClan(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");
        String clan = StringArgumentType.getString(context, "clan");
        
        DataManager.getInstance().addPlayerToClan(player, clan, -1);
        sendFeedback("§a[EvoChat] Игрок " + player + " добавлен в клан " + clan);
        return Command.SINGLE_SUCCESS;
    }

    private static int addPlayerToClanWithLevel(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");
        int level = IntegerArgumentType.getInteger(context, "level");
        String clan = StringArgumentType.getString(context, "clan");

        DataManager.getInstance().addPlayerToClan(player, clan, level);
        sendFeedback("§a[EvoChat] Игрок " + player + " добавлен в клан " + clan + " [уровень " + level + "]");
        return Command.SINGLE_SUCCESS;
    }

    private static int removePlayerFromClan(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");

        DataManager.getInstance().removePlayerFromClan(player);
        sendFeedback("§a[EvoChat] Игрок " + player + " удалён из клана");
        return Command.SINGLE_SUCCESS;
    }

    // === Highlight команды ===

    private static int highlightEnable(CommandContext<FabricClientCommandSource> context) {
        ClanHighlightConfig.getInstance().setHighlightEnabled(true);
        sendFeedback("§a[EvoChat] Подсветка кланов включена");
        return Command.SINGLE_SUCCESS;
    }

    private static int highlightDisable(CommandContext<FabricClientCommandSource> context) {
        ClanHighlightConfig.getInstance().setHighlightEnabled(false);
        sendFeedback("§7[EvoChat] Подсветка кланов выключена");
        return Command.SINGLE_SUCCESS;
    }

    private static int highlightSetColor(CommandContext<FabricClientCommandSource> context) {
        String clan = StringArgumentType.getString(context, "clan");
        String color = StringArgumentType.getString(context, "color");
        
        if (color == null || color.trim().isEmpty()) {
            sendFeedback("§c[EvoChat] Укажите цвет. Пример: /chatlogger highlight color WHITE red");
            return Command.SINGLE_SUCCESS;
        }
        
        String normalizedColor = ClanHighlightConfig.normalizeColor(color);
        ClanHighlightConfig config = ClanHighlightConfig.getInstance();
        config.setClanColor(clan, normalizedColor);
        
        // Проверяем, что сохранилось
        String savedColor = config.getClanColor(clan);
        sendFeedback("§a[EvoChat] Цвет клана §b" + clan + " §aустановлен: §b" + normalizedColor);
        if (!normalizedColor.equals(savedColor)) {
            sendFeedback("§c[WARN] Сохранённый цвет отличается: " + savedColor);
        }
        
        return Command.SINGLE_SUCCESS;
    }

    private static int highlightRemoveColor(CommandContext<FabricClientCommandSource> context) {
        String clan = StringArgumentType.getString(context, "clan");
        ClanHighlightConfig.getInstance().removeClanColor(clan);
        sendFeedback("§7[EvoChat] Цвет клана §b" + clan + " §7удалён");
        return Command.SINGLE_SUCCESS;
    }

    private static int highlightList(CommandContext<FabricClientCommandSource> context) {
        ClanHighlightConfig config = ClanHighlightConfig.getInstance();
        sendFeedback("§a=== Подсветка кланов ===");
        sendFeedback("§7Состояние: " + (config.isHighlightEnabled() ? "§aвключено" : "§7выключено"));
        
        var colors = config.getAllClanColors();
        if (colors.isEmpty()) {
            sendFeedback("§7Нет закреплённых цветов");
        } else {
            sendFeedback("§aЗакреплённые цвета:");
            for (var entry : colors.entrySet()) {
                sendFeedback("§7  [" + entry.getKey() + "] - §b" + entry.getValue());
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int enemyEnable(CommandContext<FabricClientCommandSource> context) {
        ClanHighlightConfig.getInstance().setEnemyHighlightEnabled(true);
        sendFeedback("§a[EvoChat] Подсветка врагов включена");
        return Command.SINGLE_SUCCESS;
    }

    private static int enemyDisable(CommandContext<FabricClientCommandSource> context) {
        ClanHighlightConfig.getInstance().setEnemyHighlightEnabled(false);
        sendFeedback("§7[EvoChat] Подсветка врагов выключена");
        return Command.SINGLE_SUCCESS;
    }

    private static int enemyColor(CommandContext<FabricClientCommandSource> context) {
        String color = StringArgumentType.getString(context, "color");
        String normalized = ClanHighlightConfig.normalizeColor(color);
        ClanHighlightConfig.getInstance().setEnemyColor(normalized);
        sendFeedback("§a[EvoChat] Цвет подсветки врагов: §b" + normalized);
        return Command.SINGLE_SUCCESS;
    }

    private static int friendAdd(CommandContext<FabricClientCommandSource> context) {
        String clan = StringArgumentType.getString(context, "clan");
        ClanHighlightConfig.getInstance().addFriendlyClan(clan);
        sendFeedback("§a[EvoChat] Клан §b" + clan + " §aотмечен дружественным (не подсвечивается как враг)");
        return Command.SINGLE_SUCCESS;
    }

    private static int friendRemove(CommandContext<FabricClientCommandSource> context) {
        String clan = StringArgumentType.getString(context, "clan");
        ClanHighlightConfig.getInstance().removeFriendlyClan(clan);
        sendFeedback("§7[EvoChat] Клан §b" + clan + " §7убран из дружественных");
        return Command.SINGLE_SUCCESS;
    }

    private static int friendList(CommandContext<FabricClientCommandSource> context) {
        var friendly = ClanHighlightConfig.getInstance().getFriendlyClans();
        if (friendly.isEmpty()) {
            sendFeedback("§7[EvoChat] Нет дружественных кланов");
        } else {
            sendFeedback("§aДружественные кланы:");
            for (String c : friendly) sendFeedback("§7  " + c);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int highlightDebug(CommandContext<FabricClientCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");
        
        sendFeedback("§a=== Отладка подсветки: §b" + playerName);
        
        // Проверяем игрока в DataManager
        PlayerData playerData = DataManager.getInstance().getPlayer(playerName);
        if (playerData == null) {
            sendFeedback("§cИгрок не найден в DataManager");
            return Command.SINGLE_SUCCESS;
        }
        
        sendFeedback("§7Клан: §b" + playerData.getClan());
        sendFeedback("§7Уровень: §b" + playerData.getLevel());
        sendFeedback("§7Онлайн: §b" + (playerData.isOnline() ? "да" : "нет"));
        
        // Проверяем цвет клана
        ClanHighlightConfig config = ClanHighlightConfig.getInstance();
        sendFeedback("§7Подсветка включена: §b" + config.isHighlightEnabled());
        
        String clan = playerData.getClan();
        if (config.hasClanColor(clan)) {
            String color = config.getClanColor(clan);
            sendFeedback("§aЦвет для клана установлен: §b" + color);
            float[] rgb = ClanHighlightConfig.hexToRgb(color);
            sendFeedback("§7RGB: §b" + rgb[0] + ", " + rgb[1] + ", " + rgb[2]);
        } else {
            sendFeedback("§cЦвет для клана §b" + clan + " §cне установлен");
            sendFeedback("§7Установите: /chatlogger highlight color " + clan + " red");
        }
        
        // Показываем последнее имя из рендера
        // (отключено в упрощённой версии)
        
        return Command.SINGLE_SUCCESS;
    }

    private static int sendClan(CommandContext<FabricClientCommandSource> context) {
        String clan = StringArgumentType.getString(context, "clan");
        DataManager manager = DataManager.getInstance();
        var clanPlayers = manager.getPlayersByClan(clan);

        if (clanPlayers.isEmpty()) {
            sendFeedback("§7[EvoChat] Клан '" + clan + "' не найден или пуст");
            return Command.SINGLE_SUCCESS;
        }

        ModConfig config = ModConfig.getInstance();
        String symbol = config.getClanChatSymbol();

        int onlineCount = 0;
        List<String> onlinePlayers = new ArrayList<>();
        List<String> offlinePlayers = new ArrayList<>();

        for (var player : clanPlayers) {
            String entry = player.getNickname() + "[" + player.getLevel() + "]";
            if (player.isOnline()) {
                onlineCount++;
                onlinePlayers.add(entry);
            } else {
                offlinePlayers.add(entry);
            }
        }

        // Формируем сообщение с ограничением в 256 символов (лимит Minecraft)
        String header = symbol + clan + ": ";
        StringBuilder message = new StringBuilder(header);

        if (!onlinePlayers.isEmpty()) {
            message.append("[Онлайн]: ");
            boolean first = true;
            for (String player : onlinePlayers) {
                if (!first) message.append(", ");
                if (message.length() + player.length() > 250) {
                    message.append("...");
                    break;
                }
                message.append(player);
                first = false;
            }
        }

        if (!offlinePlayers.isEmpty()) {
            if (message.length() > header.length()) {
                if (message.length() + 3 > 250) {
                    message = new StringBuilder(message.substring(0, Math.min(250, message.length()))).append("...");
                } else {
                    message.append(" | ");
                }
            }
            if (message.length() < 250) {
                message.append("[Оффлайн]: ");
                boolean first = true;
                for (String player : offlinePlayers) {
                    if (!first) message.append(", ");
                    if (message.length() + player.length() > 250) {
                        message.append("...");
                        break;
                    }
                    message.append(player);
                    first = false;
                }
            }
        }

        String finalMessage = message.length() > 256 ? message.substring(0, 253) + "..." : message.toString();

        if (mc.player != null) {
            // Отправляем сообщение в чат (без кодов форматирования)
            mc.player.networkHandler.sendChatMessage(finalMessage);
        }

        sendFeedback("§a[EvoChat] Состав клана §b" + clan + " §aотправлен в чат (" + onlineCount + "/" + clanPlayers.size() + " онлайн)");
        return Command.SINGLE_SUCCESS;
    }

    private static void sendFeedback(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }

    /**
     * Публичный метод для отправки состава клана в чат (используется из GUI)
     */
    public static void sendClanMessage(String clan, MinecraftClient client) {
        DataManager manager = DataManager.getInstance();
        var clanPlayers = manager.getPlayersByClan(clan);

        if (clanPlayers.isEmpty()) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§7[EvoChat] Клан '" + clan + "' не найден или пуст"), false);
            }
            return;
        }

        ModConfig config = ModConfig.getInstance();
        String symbol = config.getClanChatSymbol();

        int onlineCount = 0;
        List<String> onlinePlayers = new ArrayList<>();
        List<String> offlinePlayers = new ArrayList<>();

        for (var player : clanPlayers) {
            String entry = player.getNickname() + "[" + player.getLevel() + "]";
            if (player.isOnline()) {
                onlineCount++;
                onlinePlayers.add(entry);
            } else {
                offlinePlayers.add(entry);
            }
        }

        String header = symbol + clan + ": ";
        StringBuilder message = new StringBuilder(header);

        if (!onlinePlayers.isEmpty()) {
            message.append("[Онлайн]: ");
            boolean first = true;
            for (String player : onlinePlayers) {
                if (!first) message.append(", ");
                if (message.length() + player.length() > 250) {
                    message.append("...");
                    break;
                }
                message.append(player);
                first = false;
            }
        }

        if (!offlinePlayers.isEmpty()) {
            if (message.length() > header.length()) {
                if (message.length() + 3 > 250) {
                    message = new StringBuilder(message.substring(0, Math.min(250, message.length()))).append("...");
                } else {
                    message.append(" | ");
                }
            }
            if (message.length() < 250) {
                message.append("[Оффлайн]: ");
                boolean first = true;
                for (String player : offlinePlayers) {
                    if (!first) message.append(", ");
                    if (message.length() + player.length() > 250) {
                        message.append("...");
                        break;
                    }
                    message.append(player);
                    first = false;
                }
            }
        }

        String finalMessage = message.length() > 256 ? message.substring(0, 253) + "..." : message.toString();

        if (client != null && client.player != null) {
            // Отправляем сообщение в чат (без кодов форматирования для сервера)
            client.player.networkHandler.sendChatMessage(finalMessage);
            client.player.sendMessage(Text.literal("§a[EvoChat] Состав клана отправлен (" + onlineCount + "/" + clanPlayers.size() + " онлайн)"), false);
        }
    }
}
