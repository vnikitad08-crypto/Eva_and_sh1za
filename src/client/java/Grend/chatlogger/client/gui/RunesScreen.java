package Grend.chatlogger.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import Grend.chatlogger.client.RunesConfig;
import Grend.chatlogger.client.RunesManager;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Экран настройки сетов рун
 */
public class RunesScreen extends Screen {
    private final Screen parent;
    private final RunesConfig config;

    private int selectedSet = 0;
    private TextFieldWidget[] runeFields = new TextFieldWidget[6];
    private ButtonWidget[] setButtons = new ButtonWidget[5];
    private ButtonWidget[] keyButtons = new ButtonWidget[5];
    private ButtonWidget[] enableButtons = new ButtonWidget[5];
    private ButtonWidget applyButton;
    private int waitingForKeyBind = -1;

    public RunesScreen(Screen parent) {
        super(Text.literal("EvoChat - Руны"));
        this.parent = parent;
        this.config = RunesConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int startY = height / 5 + 10;

        // Кнопки выбора сета (слева)
        for (int i = 0; i < 5; i++) {
            final int setIndex = i;
            int yPos = startY + i * 22;

            // Кнопка выбора сета
            setButtons[i] = addDrawableChild(ButtonWidget.builder(
                    Text.literal("§fСет §b" + (i + 1)),
                    button -> {
                        selectedSet = setIndex;
                        init();
                    })
                    .dimensions(centerX - 180, yPos, 60, 20)
                    .build());

            // Кнопка включения/выключения
            enableButtons[i] = addDrawableChild(ButtonWidget.builder(
                    config.isEnabled(i) ? Text.literal("§aВКЛ") : Text.literal("§cВЫКЛ"),
                    button -> {
                        config.toggleEnabled(setIndex);
                        updateButtons();
                    })
                    .dimensions(centerX - 115, yPos, 45, 20)
                    .build());

            // Кнопка клавиши
            keyButtons[i] = addDrawableChild(ButtonWidget.builder(
                    getKeyNameOrWaiting(i),
                    button -> {
                        if (waitingForKeyBind == setIndex) {
                            waitingForKeyBind = -1;
                        } else {
                            waitingForKeyBind = setIndex;
                        }
                        updateButtons();
                    })
                    .dimensions(centerX - 65, yPos, 70, 20)
                    .build());
        }

        // Поля ввода рун (6 слотов)
        for (int i = 0; i < 6; i++) {
            final int slotIndex = i;
            int fieldY = startY + i * 22;

            runeFields[i] = new TextFieldWidget(
                textRenderer,
                centerX - 100,
                fieldY,
                200,
                18,
                Text.literal("")
            );
            runeFields[i].setMaxLength(32);
            runeFields[i].setPlaceholder(Text.literal("Руна " + (i + 1)));
            runeFields[i].setText(config.getRune(selectedSet, slotIndex));
            addDrawableChild(runeFields[i]);
        }

        // Кнопка применения
        applyButton = addDrawableChild(ButtonWidget.builder(
                Text.literal("§aПрименить сет"),
                button -> applyCurrentSet())
                .dimensions(centerX + 110, startY, 100, 20)
                .build());

        // Кнопка очистки сета
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§cОчистить сет"),
                button -> clearCurrentSet())
                .dimensions(centerX + 110, startY + 25, 100, 20)
                .build());

        // Кнопка "Назад"
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§7Назад"),
                button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(centerX - 100, height - 35, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Заголовок
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 15, Formatting.GREEN.getColorValue());

        // Подсказка по слотам
        context.drawCenteredTextWithShadow(textRenderer, "§7Слот 1: §fсет 1 | §7Слот 3: §fсет 2 | §7Слот 4: §fсет 3 | §7Слот 5: §fсет 4 | §7Слот 6: §fсет 5",
                width / 2, height - 20, 0x888888);

        // Обновляем поля при смене сета
        for (int i = 0; i < 6; i++) {
            if (runeFields[i] != null) {
                String currentText = runeFields[i].getText();
                String configText = config.getRune(selectedSet, i);
                if (!currentText.equals(configText)) {
                    config.setRune(selectedSet, i, currentText);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Если ждём ввода клавиши
        if (waitingForKeyBind >= 0) {
            // Escape для отмены
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKeyBind = -1;
                updateButtons();
                return true;
            }

            // Сохраняем новую клавишу
            if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                config.setSetKeyCode(waitingForKeyBind, keyCode);
                waitingForKeyBind = -1;
                updateButtons();
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void updateButtons() {
        for (int i = 0; i < 5; i++) {
            if (enableButtons[i] != null) {
                enableButtons[i].setMessage(config.isEnabled(i) ? Text.literal("§aВКЛ") : Text.literal("§cВЫКЛ"));
            }
            if (keyButtons[i] != null) {
                keyButtons[i].setMessage(getKeyNameOrWaiting(i));
            }
            if (setButtons[i] != null) {
                if (i == selectedSet) {
                    setButtons[i].setMessage(Text.literal("§a> Сет " + (i + 1)));
                } else {
                    setButtons[i].setMessage(Text.literal("§fСет §b" + (i + 1)));
                }
            }
        }
    }

    private Text getKeyNameOrWaiting(int index) {
        if (waitingForKeyBind == index) {
            return Text.literal("§eНажми...");
        }
        int keyCode = config.getSetKeyCode(index);
        if (keyCode == 0) {
            return Text.literal("§7-");
        }
        String keyName = InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString();
        return Text.literal("§b" + keyName);
    }

    private void applyCurrentSet() {
        // Сохраняем текущие значения из полей
        for (int i = 0; i < 6; i++) {
            if (runeFields[i] != null) {
                config.setRune(selectedSet, i, runeFields[i].getText().trim());
            }
        }

        // Применяем сет через RunesManager
        RunesManager.applyRuneSet(selectedSet);

        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§a[EvoChat] Применение сета §b" + (selectedSet + 1)), false);
        }
    }

    private void clearCurrentSet() {
        for (int i = 0; i < 6; i++) {
            config.setRune(selectedSet, i, "");
            if (runeFields[i] != null) {
                runeFields[i].setText("");
            }
        }

        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§7[EvoChat] Сет §b" + (selectedSet + 1) + " §7очищен"), false);
        }
    }

    @Override
    public void close() {
        // Сохраняем при закрытии
        for (int i = 0; i < 6; i++) {
            if (runeFields[i] != null) {
                config.setRune(selectedSet, i, runeFields[i].getText().trim());
            }
        }
        config.save();
        super.close();
    }
}
