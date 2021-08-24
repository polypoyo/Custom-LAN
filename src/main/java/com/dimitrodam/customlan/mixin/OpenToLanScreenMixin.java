package com.dimitrodam.customlan.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Mixin(OpenToLanScreen.class)
public abstract class OpenToLanScreenMixin extends Screen {
    private static final String FIND_LOCAL_PORT = "net/minecraft/client/util/NetworkUtils.findLocalPort()I";

    private static final int DEFAULT_PORT = 25565;
    private static final int DEFAULT_MAX_PLAYERS = 8;
    private static final Text ONLINE_MODE_TEXT = new TranslatableText("lanServer.onlineMode");
    private static final Text PVP_ENABLED_TEXT = new TranslatableText("lanServer.pvpEnabled");
    private static final Text PORT_TEXT = new TranslatableText("lanServer.port");
    private static final Text MAX_PLAYERS_TEXT = new TranslatableText("lanServer.maxPlayers");
    private static final Text MOTD_TEXT = new TranslatableText("lanServer.motd");

    private ButtonWidget onlineModeButton;
    private ButtonWidget pvpEnabledButton;
    private TextFieldWidget portField;
    private TextFieldWidget maxPlayersField;
    private TextFieldWidget motdField;

    private boolean onlineMode = true;
    private boolean pvpEnabled = true;
    private int port = DEFAULT_PORT;
    private boolean portValid = true;
    private int maxPlayers = DEFAULT_MAX_PLAYERS;
    private boolean maxPlayersValid = true;

    protected OpenToLanScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    private void method_19851(ButtonWidget button) {
        throw new AssertionError();
    }

    @Shadow
    private void updateButtonText() {
        throw new AssertionError();
    }

    private boolean canOpenToLan() {
        return this.portValid && this.maxPlayersValid;
    }

    // Custom buttons must be created before the call to updateButtonText
    // at the end of the Vanilla method. Hence, @At("HEAD").
    @Inject(method = "init", at = @At("HEAD"))
    private void addCustomWidgets(CallbackInfo ci) {
        // Online Mode button
        this.onlineModeButton = this
                .addButton(new ButtonWidget(this.width / 2 - 155, 124, 150, 20, ONLINE_MODE_TEXT, (button) -> {
                    this.onlineMode = !this.onlineMode;
                    this.updateButtonText();
                }));
        // PvP Enabled button
        this.pvpEnabledButton = this
                .addButton(new ButtonWidget(this.width / 2 + 5, 124, 150, 20, PVP_ENABLED_TEXT, (button) -> {
                    this.pvpEnabled = !this.pvpEnabled;
                    this.updateButtonText();
                }));

        // Port field
        this.portField = new TextFieldWidget(this.textRenderer, this.width / 2 - 154, this.height - 92, 148,
                20, PORT_TEXT);
        portField.setText(Integer.toString(port));
        portField.setChangedListener((port) -> {
            ButtonWidget startButton = (ButtonWidget) this.children().get(5);
            Integer newPort = null;
            try {
                newPort = Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
            if (newPort != null && newPort > 0 && newPort <= 65535) {
                this.port = newPort;
                this.portValid = true;
                portField.setEditableColor(0xFFFFFF);
            } else {
                this.port = DEFAULT_PORT;
                this.portValid = false;
                portField.setEditableColor(0xFF0000);
            }
            startButton.active = canOpenToLan();
        });
        this.addChild(portField);

        // Max Players field
        this.maxPlayersField = new TextFieldWidget(this.textRenderer, this.width / 2 + 6, this.height - 92,
                148, 20, MAX_PLAYERS_TEXT);
        maxPlayersField.setText(Integer.toString(maxPlayers));
        maxPlayersField.setChangedListener((maxPlayers) -> {
            ButtonWidget startButton = (ButtonWidget) this.children().get(5);
            Integer newMaxPlayers = null;
            try {
                newMaxPlayers = Integer.parseInt(maxPlayers);
            } catch (NumberFormatException e) {
            }
            if (newMaxPlayers != null && newMaxPlayers > 0) {
                this.maxPlayers = newMaxPlayers;
                this.maxPlayersValid = true;
                maxPlayersField.setEditableColor(0xFFFFFF);
            } else {
                this.maxPlayers = DEFAULT_MAX_PLAYERS;
                this.maxPlayersValid = false;
                maxPlayersField.setEditableColor(0xFF0000);
            }
            startButton.active = canOpenToLan();
        });
        this.addChild(maxPlayersField);

        // MOTD field
        IntegratedServer server = this.client.getServer();
        this.motdField = new TextFieldWidget(this.textRenderer, this.width / 2 - 154, this.height - 54, 308, 20,
                MOTD_TEXT);
        motdField.setMaxLength(59); // https://minecraft.fandom.com/wiki/Server.properties#motd
        motdField.setText(server.getServerMotd());
        this.addChild(motdField);
    }

    @Inject(method = "updateButtonText", at = @At("TAIL"))
    private void updateCustomButtonText(CallbackInfo ci) {
        this.onlineModeButton
                .setMessage(ONLINE_MODE_TEXT.copy().append(" ").append(ScreenTexts.getToggleText(this.onlineMode)));
        this.pvpEnabledButton
                .setMessage(PVP_ENABLED_TEXT.copy().append(" ").append(ScreenTexts.getToggleText(this.pvpEnabled)));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustom(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Port field text
        drawTextWithShadow(matrices, this.textRenderer, PORT_TEXT, this.width / 2 - 154, this.height - 104, 10526880);
        // Max Players field text
        drawTextWithShadow(matrices, this.textRenderer, MAX_PLAYERS_TEXT, this.width / 2 + 6, this.height - 104,
                10526880);
        // MOTD field text
        drawTextWithShadow(matrices, this.textRenderer, MOTD_TEXT, this.width / 2 - 154, this.height - 66, 10526880);

        this.portField.render(matrices, mouseX, mouseY, delta);
        this.maxPlayersField.render(matrices, mouseX, mouseY, delta);
        this.motdField.render(matrices, mouseX, mouseY, delta);
    }

    @Inject(method = "method_19851", at = @At("HEAD"))
    private void beforeOpenedToLan(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();

        server.setOnlineMode(this.onlineMode);
        server.setPvpEnabled(this.pvpEnabled);

        ((PlayerManagerAccessor) server.getPlayerManager()).setMaxPlayers(this.maxPlayers);

        // MOTD needs to be set before openToLan
        // because the LAN pinger calls getServerMotd.
        server.setMotd(this.motdField.getText());
        // Metadata doesn't get updated automatically.
        server.getServerMetadata().setDescription(new LiteralText(server.getServerMotd()));
    }

    @Redirect(method = "method_19851", at = @At(value = "INVOKE", ordinal = 0, target = FIND_LOCAL_PORT))
    private int getPort() {
        return this.port;
    }

    /**
     * Allow starting with Enter as well as the Start LAN World button.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER) {
            return false;
        } else {
            if (canOpenToLan()) {
                // Open to LAN
                this.method_19851((ButtonWidget) this.children().get(5));
            }
            return true;
        }
    }
}
