package tab.bettertab.config;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static tab.bettertab.ConfigSystem.configFile;
import tab.bettertab.ConfigSystem;

public class ConfigScreen extends Screen {
    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
    private final JsonObject editedConfigFile = new JsonObject();

    public ConfigScreen(Screen screen) {
        super(Text.of("BetterStats"));
    }

    @Override
    public void init() {
        Tab[] tabs = new Tab[5];
        tabs[0] = new newTab(Text.translatable("tab.bettertab.config.tabs.general").getString(), new ArrayList<>(List.of("enable_mod", "render_heads", "scroll_with_mouse", "column_numbers")));
        tabs[1] = new newTab(Text.translatable("tab.bettertab.config.tabs.styling").getString(), new ArrayList<>(List.of("background_color", "cell_color", "name_color", "spectator_color")));
        tabs[2] = new newTab(Text.translatable("tab.bettertab.config.tabs.ping").getString(), new ArrayList<>(List.of("render_ping", "use_numeric", "ping_color_none", "ping_color_low", "ping_color_medium", "ping_color_high")));
        tabs[3] = new newTab(Text.translatable("tab.bettertab.config.tabs.keybinds").getString(), new ArrayList<>(List.of()));
        tabs[4] = new newTab(Text.translatable("tab.bettertab.config.tabs.advanced").getString(), new ArrayList<>(List.of()));
        // Bypass scroll-with-mouse: ctrl
        // Left / Right scroll button
        // Scroll type (column/page)
        // Open config keybind
        // Toggle mod keybind

        TabNavigationWidget tabNavigation = TabNavigationWidget.builder(this.tabManager, this.width).tabs(tabs).build();
        this.addDrawableChild(tabNavigation);

        ButtonWidget saveButton = ButtonWidget.builder(Text.translatable("tab.bettertab.config.button_text.done"), btn -> saveFile()).dimensions(width / 4, height - 25, width / 2, 20).build();
        this.addDrawableChild(saveButton);

        tabNavigation.selectTab(0, false);
        tabNavigation.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    private class newTab extends GridScreenTab {
        public SettingWidget settingWidget;
        public newTab(String tabName, ArrayList<String> settings) {
            super(Text.of(tabName));
            GridWidget.Adder adder = grid.createAdder(1);

            settingWidget = new SettingWidget(width, height, settings, editedConfigFile);
            adder.add(settingWidget);
        }
    }

    private void saveFile() {
        for (Element tab : ((TabNavigationWidget) this.children().get(0)).children()) {
            newTab tabElm = (newTab) ((TabButtonWidget) tab).getTab();
            for (SettingWidget.Entry a : tabElm.settingWidget.children()) {
                if (a.textField != null) {
                    editedConfigFile.addProperty(a.setting, a.textField.getText());
                }
            }
        }
        boolean saved = new ConfigSystem().saveElementFiles(editedConfigFile);
        if (saved) {
            configFile = editedConfigFile;
        }
        close();
    }
}
