package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import Grend.chatlogger.client.AutoMessageConfig;
import Grend.chatlogger.client.AutoMessageSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран настройки автоотправки сообщений
 */
public class AutoMessageScreen extends Screen {
    private final Screen parent;
    private final AutoMessageConfig config;

    private ButtonWidget enableButton;
    private ButtonWidget intervalButton;
    private ButtonWidget testButton;
    private ButtonWidget timeInfoButton;
    private List<ButtonWidget> messageButtons = new ArrayList<>();
    private TextFieldWidget addMessageField;
    private ButtonWidget addMessageButton;
    private ButtonWidget removeMessageButton;

    private int selectedMessageIndex = -1;

    // Константы для позиционирования
    private static final int BUTTON_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 25;
    private static final int START_Y = 50;
    private static final int MESSAGE_BUTTON_HEIGHT = 18;
    private static final int MESSAGE_GAP = 2;
    private static final int MAX_MESSAGES = 6;

    public AutoMessageScreen(Screen parent) {
        super(Text.literal("EvoChat - Автоотправка"));
        this.parent = parent;
        this.config = AutoMessageConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        messageButtons.clear();

        int centerX = width / 2;

        // Кнопка включения/выключения (Y = 50)
        enableButton = addDrawableChild(ButtonWidget.builder(
                config.isEnabled() ? Text.literal("§aСтатус: §fВКЛ") : Text.literal("§cСтатус: §fВЫКЛ"),
                button -> {
                    config.setEnabled(!config.isEnabled());
                    updateButtons();
                })
                .dimensions(centerX - BUTTON_WIDTH / 2, START_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        // Кнопка интервала (Y = 75)
        intervalButton = addDrawableChild(ButtonWidget.builder(
                Text.literal("§eИнтервал: §f" + config.getIntervalSeconds() + " сек"),
                button -> {
                    int interval = config.getIntervalSeconds();
                    config.setIntervalSeconds(interval >= 300 ? 5 : interval + 5);
                    updateButtons();
                })
                .dimensions(centerX - BUTTON_WIDTH / 2, START_Y + GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        // Поле ввода (Y = 100)
        addMessageField = new TextFieldWidget(textRenderer, centerX - BUTTON_WIDTH / 2, START_Y + GAP * 2, BUTTON_WIDTH - 80, BUTTON_HEIGHT - 2, Text.literal(""));
        addMessageField.setMaxLength(256);
        addMessageField.setPlaceholder(Text.literal("Введите сообщение..."));
        addDrawableChild(addMessageField);

        // Кнопка добавления (Y = 100, справа)
        addMessageButton = addDrawableChild(ButtonWidget.builder(
                Text.literal("§aДобавить"),
                button -> {
                    String text = addMessageField.getText().trim();
                    if (!text.isEmpty()) {
                        config.addMessage(text);
                        addMessageField.setText("");
                        updateButtons();
                    }
                })
                .dimensions(centerX + BUTTON_WIDTH / 2 - 75, START_Y + GAP * 2, 75, BUTTON_HEIGHT)
                .build());

        // Кнопка теста (Y = 125)
        testButton = addDrawableChild(ButtonWidget.builder(
                Text.literal("§bТест отправки"),
                button -> AutoMessageSender.forceSend())
                .dimensions(centerX - BUTTON_WIDTH / 2, START_Y + GAP * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        // Информация о времени (Y = 147)
        updateTimeInfoButton();

        // Заголовок списка сообщений (Y = 170)
        int listHeaderY = START_Y + GAP * 4 + 22;

        // Список сообщений (Y = 180+)
        int listStartY = listHeaderY + 10;

        List<String> messages = config.getMessages();
        for (int i = 0; i < Math.min(messages.size(), MAX_MESSAGES); i++) {
            final int index = i;
            String msg = messages.get(i);
            String displayMsg = msg.length() > 42 ? msg.substring(0, 42) + "..." : msg;

            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal("§f" + (i + 1) + ". §7" + displayMsg),
                    button -> {
                        selectedMessageIndex = index;
                        updateButtons();
                    })
                    .dimensions(centerX - BUTTON_WIDTH / 2, listStartY + i * (MESSAGE_BUTTON_HEIGHT + MESSAGE_GAP), BUTTON_WIDTH - 50, MESSAGE_BUTTON_HEIGHT)
                    .build();

            if (i == selectedMessageIndex) {
                btn.setMessage(Text.literal("§a> " + (i + 1) + ". §7" + displayMsg));
            }

            messageButtons.add(addDrawableChild(btn));
        }

        // Кнопка удаления (справа от списка)
        removeMessageButton = addDrawableChild(ButtonWidget.builder(
                Text.literal("§cУдалить"),
                button -> {
                    if (selectedMessageIndex >= 0) {
                        config.removeMessage(selectedMessageIndex);
                        selectedMessageIndex = -1;
                        updateButtons();
                    }
                })
                .dimensions(centerX + BUTTON_WIDTH / 2 - 45, listStartY, 45, BUTTON_HEIGHT)
                .build());

        // Кнопка "Назад" (внизу)
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§7Назад"),
                button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(centerX - BUTTON_WIDTH / 2, height - 35, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void updateTimeInfoButton() {
        int seconds = AutoMessageSender.getTimeUntilNextMessage();
        String text = config.isEnabled() ?
            "§fДо следующего: §b" + seconds + " сек" :
            "§7Отключено";

        int centerX = width / 2;

        if (timeInfoButton != null) {
            timeInfoButton.setMessage(Text.literal(text));
        } else {
            timeInfoButton = addDrawableChild(ButtonWidget.builder(
                    Text.literal(text),
                    button -> {})
                    .dimensions(centerX - BUTTON_WIDTH / 2, START_Y + GAP * 3 + 22, BUTTON_WIDTH, 18)
                    .build());
        }
    }

    private void updateButtons() {
        if (enableButton != null) {
            enableButton.setMessage(config.isEnabled() ? Text.literal("§aСтатус: §fВКЛ") : Text.literal("§cСтатус: §fВЫКЛ"));
        }
        if (intervalButton != null) {
            intervalButton.setMessage(Text.literal("§eИнтервал: §f" + config.getIntervalSeconds() + " сек"));
        }
        if (removeMessageButton != null) {
            removeMessageButton.active = selectedMessageIndex >= 0;
        }
        init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;

        // Заголовок (Y = 15)
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), centerX, 15, Formatting.GREEN.getColorValue());

        // Описание (Y = 30)
        context.drawCenteredTextWithShadow(textRenderer, "§7Автоматическая отправка сообщений в чат",
                centerX, 30, Formatting.GRAY.getColorValue());

        // Подсказка (Y = 42)
        context.drawCenteredTextWithShadow(textRenderer, "§7Сообщения отправляются по кругу",
                centerX, 42, Formatting.GRAY.getColorValue());

        // Заголовок списка сообщений
        int listHeaderY = START_Y + GAP * 4 + 22;
        context.drawText(textRenderer, "§fСообщения (§b" + config.getMessageCount() + "§f):",
                centerX - BUTTON_WIDTH / 2 + 2, listHeaderY, 0xFFFFFF, true);
    }

    @Override
    public void tick() {
        if (client != null && client.player != null) {
            updateTimeInfoButton();
        }
    }
}
