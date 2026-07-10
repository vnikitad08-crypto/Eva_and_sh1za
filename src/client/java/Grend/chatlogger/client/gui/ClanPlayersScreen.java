package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import Grend.chatlogger.data.DataManager;
import Grend.chatlogger.data.PlayerData;
import Grend.chatlogger.client.util.OnlineChecker;

import java.util.List;

/**
 * Экран просмотра игроков конкретного клана
 */
public class ClanPlayersScreen extends Screen {
    private final Screen parent;
    private final String clanName;
    private List<PlayerData> players;
    private int scrollOffset = 0;
    private int maxVisiblePlayers = 8;
    private TextFieldWidget addPlayerField;
    private TextFieldWidget addLevelField;

    public ClanPlayersScreen(Screen parent, String clanName) {
        super(Text.literal("EvoChat - Клан: " + clanName));
        this.parent = parent;
        this.clanName = clanName;
    }

    @Override
    protected void init() {
        super.init();

        players = DataManager.getInstance().getPlayersByClan(clanName);
        players.sort((a, b) -> b.getLevel() - a.getLevel());
        maxVisiblePlayers = (height - 130) / 22;

        int centerX = width / 2;

        // Поля ввода - сверху с отступом
        addPlayerField = new TextFieldWidget(textRenderer, centerX - 110, 10, 100, 18, Text.literal(""));
        addPlayerField.setMaxLength(32);
        addPlayerField.setPlaceholder(Text.literal("Ник игрока"));
        addDrawableChild(addPlayerField);

        addLevelField = new TextFieldWidget(textRenderer, centerX + 15, 10, 40, 18, Text.literal(""));
        addLevelField.setMaxLength(3);
        addLevelField.setPlaceholder(Text.literal("Ур."));
        addDrawableChild(addLevelField);

        addDrawableChild(ButtonWidget.builder(Text.literal("§aДобавить"), button -> addPlayer())
                .dimensions(centerX + 60, 10, 80, 18)
                .build());

        // Кнопки снизу
        addDrawableChild(ButtonWidget.builder(Text.literal("§eПроверить онлайн"), button -> checkOnline())
                .dimensions(centerX - 100, height - 55, 120, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("§aВ клан-чат"), button -> sendToClanChat())
                .dimensions(centerX + 25, height - 55, 100, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("§7Назад"), button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(centerX - 75, height - 30, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 38, Formatting.GREEN.getColorValue());

        // Статистика
        int onlineCount = (int) players.stream().filter(PlayerData::isOnline).count();
        String stats = String.format("Игроков: %d | Онлайн: %d | Оффлайн: %d",
            players.size(), onlineCount, players.size() - onlineCount);
        context.drawCenteredTextWithShadow(textRenderer, stats, width / 2, 50, Formatting.GRAY.getColorValue());

        // Список игроков
        int listX = width / 2 - 170;
        int listY = 62;
        int listWidth = 340;
        int listHeight = maxVisiblePlayers * 24;

        context.fill(listX - 2, listY - 2, listX + listWidth + 2, listY + listHeight + 2, 0x80000000);

        if (players.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "§7Клан пуст", width / 2, listY + 10, Formatting.GRAY.getColorValue());
            return;
        }

        int visibleCount = Math.min(players.size() - scrollOffset, maxVisiblePlayers);

        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= players.size()) break;

            PlayerData player = players.get(index);
            int y = listY + i * 24;

            boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth &&
                               mouseY >= y && mouseY <= y + 22;
            int bgColor = isHovered ? 0x30FFFFFF : 0x10FFFFFF;
            context.fill(listX, y, listX + listWidth, y + 22, bgColor);

            // Статус онлайн
            String status = player.isOnline() ? "§a[+]" : "§7[-]";
            context.drawText(textRenderer, status, listX + 5, y + 8, 0xFFFFFF, true);

            // Ник и уровень
            String playerInfo = String.format("§f%s §7[%d]", player.getNickname(), player.getLevel());
            context.drawText(textRenderer, playerInfo, listX + 25, y + 8, 0xFFFFFF, true);

            // Последняя активность
            String lastSeen = formatLastSeen(player.getLastSeenTimestamp());
            context.drawText(textRenderer, "§7" + lastSeen, listX + 180, y + 8, 0x888888, true);

            // Кнопка удаления
            int removeBtnX = listX + listWidth - 45;
            boolean removeHovered = mouseX >= removeBtnX && mouseX <= removeBtnX + 40 &&
                                   mouseY >= y + 4 && mouseY <= y + 20;
            int removeBtnColor = removeHovered ? 0xFFAA0000 : 0xFF880000;
            context.fill(removeBtnX, y + 4, removeBtnX + 40, y + 20, removeBtnColor);
            context.drawCenteredTextWithShadow(textRenderer, "§cX", removeBtnX + 20, y + 8, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int listX = width / 2 - 170;
        int listY = 62;
        int listWidth = 340;

        int visibleCount = Math.min(players.size() - scrollOffset, maxVisiblePlayers);

        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= players.size()) break;

            PlayerData player = players.get(index);
            int y = listY + i * 24;

            int removeBtnX = listX + listWidth - 45;
            if (mouseX >= removeBtnX && mouseX <= removeBtnX + 40 &&
                mouseY >= y + 4 && mouseY <= y + 20) {
                removePlayer(player.getNickname());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (players.size() > maxVisiblePlayers) {
            scrollOffset -= (int) verticalAmount;
            scrollOffset = Math.max(0, Math.min(scrollOffset, players.size() - maxVisiblePlayers));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (addPlayerField.isFocused() || addLevelField.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void checkOnline() {
        OnlineChecker.checkClan(clanName, null);
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§7[EvoChat] Проверка клана: " + clanName), false);
        }
        MinecraftClient.getInstance().execute(() -> {
            players = DataManager.getInstance().getPlayersByClan(clanName);
            players.sort((a, b) -> b.getLevel() - a.getLevel());
        });
    }

    private void sendToClanChat() {
        Grend.chatlogger.client.commands.ModCommands.sendClanMessage(clanName, client);
    }

    private void addPlayer() {
        String nickname = addPlayerField.getText().trim();
        String levelText = addLevelField.getText().trim();

        if (nickname.isEmpty()) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[EvoChat] Введите ник игрока"), false);
            }
            return;
        }

        int level = 1;
        if (!levelText.isEmpty()) {
            try {
                level = Integer.parseInt(levelText);
                if (level < 1 || level > 100) {
                    level = 1;
                }
            } catch (NumberFormatException e) {
                level = 1;
            }
        }

        DataManager.getInstance().addPlayerToClan(nickname, clanName, level);
        addPlayerField.setText("");
        addLevelField.setText("");

        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§a[EvoChat] Игрок " + nickname + " добавлен в клан"), false);
        }

        players = DataManager.getInstance().getPlayersByClan(clanName);
        players.sort((a, b) -> b.getLevel() - a.getLevel());
    }

    private void removePlayer(String nickname) {
        DataManager.getInstance().removePlayerFromClan(nickname);
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§7[EvoChat] Игрок " + nickname + " удалён из клана"), false);
        }

        players = DataManager.getInstance().getPlayersByClan(clanName);
        players.sort((a, b) -> b.getLevel() - a.getLevel());
    }

    private String formatLastSeen(long timestamp) {
        if (timestamp <= 0) return "никогда";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) return "только что";
        if (diff < 3600000) return (diff / 60000) + " мин. назад";
        if (diff < 86400000) return (diff / 3600000) + " ч. назад";
        return (diff / 86400000) + " дн. назад";
    }
}
