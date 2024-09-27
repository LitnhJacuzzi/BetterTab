package tab.bettertab.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tab.bettertab.BetterTab;

import java.text.NumberFormat;
import java.util.*;

import static tab.bettertab.BetterTab.*;
import static tab.bettertab.ConfigSystem.configFile;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

	@Shadow @Final private static Comparator<PlayerListEntry> ENTRY_ORDERING;
	@Shadow private MinecraftClient client;
	@Shadow protected abstract List<PlayerListEntry> collectPlayerEntries();
	@Shadow @Nullable private Text header;
	@Shadow @Nullable private Text footer;
	@Shadow private boolean visible;
	@Shadow public abstract Text getPlayerName(PlayerListEntry entry);
	@Shadow protected abstract void renderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry);

	@Unique private boolean showArrows = false;
	@Unique private long lastCheck = 0;

	@Unique private boolean ENABLE_MOD;
	@Unique private int SCROLL_TYPE;
	@Unique private boolean RENDER_HEADS;
	@Unique private boolean RENDER_PING;
	@Unique private boolean USE_NUMERIC;
	@Unique private int BACKGROUND_COLOR;
	@Unique private int CELL_COLOR;
	@Unique private int NAME_COLOR;
	@Unique private int SPECTATOR_COLOR;
	@Unique private int PING_COLOR_NONE;
	@Unique private int PING_COLOR_LOW;
	@Unique private int PING_COLOR_MEDIUM;
	@Unique private int PING_COLOR_HIGH;
	@Unique private int COLUMN_NUMBERS;
	@Unique private int EMPTY_CELL_LINE_COLOR;
	@Unique private int COLUMN_NUMBER_COLOR;

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void onRender(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
		if (!ENABLE_MOD) {
			return;
		}
		List<PlayerListEntry> list = this.collectPlayerEntries();
		if (list.isEmpty()) { // Should not really be possible, but still
			return;
		}

		if (Calendar.getInstance().getTimeInMillis() - lastCheck > 530) {
			lastCheck = Calendar.getInstance().getTimeInMillis();
			showArrows = !showArrows;
		}

		int entryHeight = 10;
		int windowHeight = client.getWindow().getScaledHeight();

		int columnHeight = 0;
		int pageWidth = 0;
		ArrayList<PlayerListEntry> column = new ArrayList<>();
		ArrayList<ArrayList<PlayerListEntry>> columns = new ArrayList<>();
		ArrayList<ArrayList<PlayerListEntry>> allColumns = new ArrayList<>();
		ArrayList<Integer> widths = new ArrayList<>();
		ArrayList<Integer> columnsWidths = new ArrayList<>();

		int pages = 0;
		boolean correctPage = false;

		tabScroll = tabScroll < 0 ? 0 : tabScroll;
		for (PlayerListEntry player : list) {
			Text playerName = this.getPlayerName(player);
			if (columnHeight + entryHeight + 1 >= windowHeight / 2) {
				columnHeight = 0;
				if (Collections.max(widths) + pageWidth + 20 > scaledWindowWidth) {
					if (SCROLL_TYPE == 1) {
						if (tabScroll == pages) {
							correctPage = true;
							break;
						} else {
							columns = new ArrayList<>();
							columnsWidths = new ArrayList<>();
							pages ++;
							pageWidth = Collections.max(widths) + 2; // 0 or thihs
						}
					}
				} else {
					pageWidth += Collections.max(widths) + 2;
				}
				columns.add(column);
				allColumns.add(column);
				columnsWidths.add(Collections.max(widths));
				widths = new ArrayList<>();
				column = new ArrayList<>();
			}
			columnHeight += entryHeight + 1;
			widths.add(client.textRenderer.getWidth(playerName) + 10 + 2 + (RENDER_HEADS ? 8 : 0) + (RENDER_PING ? (USE_NUMERIC ? client.textRenderer.getWidth(String.valueOf(player.getLatency())) : 10) : 0));
			column.add(player);
		}
		if (!column.isEmpty() && !correctPage) {
			if (!columns.isEmpty()) {
				int emptyLinesNeeded = columns.getFirst().size() - column.size();
				for (int i = 0; i < emptyLinesNeeded; i++) {
					String emptyText = "";
					UUID emptyUUID = UUID.nameUUIDFromBytes(emptyText.getBytes());

					GameProfile emptyLine = new GameProfile(emptyUUID, emptyText);
					PlayerListEntry emptyEntry = new PlayerListEntry(emptyLine, false);

					column.add(emptyEntry);
				}
			}
			columns.add(column);
			allColumns.add(column);
			columnsWidths.add(Collections.max(widths));
			pageWidth += Collections.max(widths) + 2;
		}
		boolean canScrollLeft;
		boolean canScrollRight;
		if (SCROLL_TYPE == 0) {
			int maxVisibleColumns = 0;
			int currentWidth = 0;

			for (int colWidth : columnsWidths) {
				if (currentWidth + colWidth + 2 > scaledWindowWidth) {
					break;
				}
				currentWidth += colWidth + 2;
				maxVisibleColumns++;
			}

			tabScroll = Math.min(tabScroll, columns.size() - maxVisibleColumns);
			int columnSize = columns.size();

			int startIndex = (int) tabScroll;
			int endIndex = (int) Math.min(tabScroll + maxVisibleColumns, columns.size());

			columns = new ArrayList<>(columns.subList(startIndex, endIndex));
			columnsWidths = new ArrayList<>(columnsWidths.subList(startIndex, endIndex));

			pageWidth = 0;
            for (Integer columnsWidth : columnsWidths) {
                pageWidth += columnsWidth + 2;
            }
			canScrollLeft = tabScroll > 0;
			canScrollRight = columnSize > endIndex;
		} else {
			tabScroll = tabScroll > pages ? pages : tabScroll;
			canScrollLeft = pages > 0;
			canScrollRight = correctPage;
		}

		int x = (scaledWindowWidth - pageWidth) / 2;
		int startY = 10;
		int totalRowHeight = columns.getFirst().size() * (entryHeight + 1);

		boolean renderColumnNumbers = ((canScrollRight || canScrollLeft) && COLUMN_NUMBERS == 1) || COLUMN_NUMBERS == 2;

		List<OrderedText> headerList = List.of();
		if (this.header != null) {
			headerList = client.textRenderer.wrapLines(this.header, pageWidth);
		}

		List<OrderedText> footerList = List.of();
		if (this.footer != null) {
			footerList = client.textRenderer.wrapLines(this.footer, pageWidth);
		}

		int charWidth = client.textRenderer.getWidth("<");
		context.fill(x - 5 - (canScrollLeft ? 5 + charWidth : 0), startY - 5, x + pageWidth + 5 + (canScrollRight ? 5 + charWidth : 0), startY + headerList.size() * 9 + footerList.size() * 9 + totalRowHeight + 5 + (renderColumnNumbers ? 3 + 5 : 0), BACKGROUND_COLOR);
		if (showArrows) {
			if (canScrollLeft) {
				context.drawTextWrapped(client.textRenderer, StringVisitable.plain("<<<"), x - 5 - charWidth, startY + headerList.size() * 9 + totalRowHeight / 2 - 4 - 9, charWidth, 0xFFFFFFFF);
			}
			if (canScrollRight) {
				context.drawTextWrapped(client.textRenderer, StringVisitable.plain(">>>"), x + pageWidth + 5, startY + headerList.size() * 9 + totalRowHeight / 2 - 4 - 9, charWidth, 0xFFFFFFFF);
			}
		}

		for (OrderedText line : headerList) {
			context.drawTextWithShadow(this.client.textRenderer, line, scaledWindowWidth / 2 - client.textRenderer.getWidth(line) / 2, startY, -1);
			startY += 9;
		}
		startY -= 9;

		for (int i = 0; i < columns.size(); i++) {
			ArrayList<PlayerListEntry> col = columns.get(i);

			int y = startY;
            for (PlayerListEntry playerListEntry : col) {
                y += entryHeight + 1;

                context.fill(x - 1, y, x + columnsWidths.get(i), y + entryHeight, CELL_COLOR);
                RenderSystem.enableBlend();

                Text playerName = this.getPlayerName(playerListEntry);

                if (!playerName.getString().isEmpty()) {
                    if (RENDER_HEADS) {
                        PlayerSkinDrawer.draw(context, playerListEntry.getSkinTextures().texture(), x, y + 1, 8, true, false);
                    }
                    context.drawTextWithShadow(this.client.textRenderer, playerName, x + 2 + (RENDER_HEADS ? 8 : 0), y + 2, playerListEntry.getGameMode() == GameMode.SPECTATOR ? SPECTATOR_COLOR : NAME_COLOR);
                    if (RENDER_PING) {
                        if (USE_NUMERIC) {
                            String ping = String.valueOf(playerListEntry.getLatency());
                            context.drawTextWithShadow(this.client.textRenderer, ping, x + columnsWidths.get(i) - this.client.textRenderer.getWidth(ping) - 2, y + 2, numericalColoriser(Integer.parseInt(ping)));
                        } else {
                            this.renderLatencyIcon(context, columnsWidths.get(i), x, y + 1, playerListEntry);
                        }
                    }
                } else {
                    context.fill(x + 2, y + 2 + 3, x + columnsWidths.get(i) - 3, y + 2 + 4, EMPTY_CELL_LINE_COLOR);
                }
            }
			if (renderColumnNumbers) {
				context.drawCenteredTextWithShadow(this.client.textRenderer, String.valueOf(allColumns.indexOf(columns.get(i)) + 1), x + columnsWidths.get(i) / 2 + 1, y + entryHeight + 3, COLUMN_NUMBER_COLOR);
			}
			x += columnsWidths.get(i) + 2;
		}

		int y = startY + 5 + totalRowHeight + (renderColumnNumbers ? 3 + 5 : 0);
		for (OrderedText line : footerList) {
			y += 9;
			context.drawTextWithShadow(this.client.textRenderer, line, scaledWindowWidth / 2 - client.textRenderer.getWidth(line) / 2, y, -1);
		}
		ci.cancel();
	}

	@Inject(method = "collectPlayerEntries", at = @At("HEAD"), cancellable = true)
	private void onCollectPlayerEntries(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
		List<PlayerListEntry> playerList = new ArrayList<>(client.player.networkHandler.getListedPlayerListEntries().stream().sorted(ENTRY_ORDERING).toList());

		if (useExamples) {
			for (int i = 1; i <= 200; i++) {
				playerList.add(fakePlayer("ExamplePlayer" + i));
			}
		}

		if (ENABLE_MOD) {
			cir.setReturnValue(playerList);
		} else {
			cir.setReturnValue(playerList.stream().sorted(ENTRY_ORDERING).limit(80L).toList());
		}
	}

	@Inject(method = "setVisible", at = @At("HEAD"))
	private void onEnable(boolean visible, CallbackInfo ci) {
		if (this.visible != visible) {
			tabScroll = 0;
			ENABLE_MOD = configFile.getAsJsonObject().get("enable_mod").getAsBoolean();
			SCROLL_TYPE = configFile.getAsJsonObject().get("scroll_type").getAsInt();
			RENDER_HEADS = configFile.getAsJsonObject().get("render_heads").getAsBoolean();
			RENDER_PING = configFile.getAsJsonObject().get("render_ping").getAsBoolean();
			USE_NUMERIC = configFile.getAsJsonObject().get("use_numeric").getAsBoolean();
			COLUMN_NUMBERS = configFile.getAsJsonObject().get("column_numbers").getAsInt();
			BACKGROUND_COLOR = new BetterTab().parseColor(configFile.getAsJsonObject().get("background_color").getAsString());
			EMPTY_CELL_LINE_COLOR = new BetterTab().parseColor(configFile.getAsJsonObject().get("empty_cell_line_color").getAsString());
			COLUMN_NUMBER_COLOR = new BetterTab().parseColor(configFile.getAsJsonObject().get("column_number_color").getAsString());

			CELL_COLOR = new BetterTab().parseColor(configFile.getAsJsonObject().get("cell_color").getAsString());
			NAME_COLOR = new BetterTab().parseColor(configFile.getAsJsonObject().get("name_color").getAsString());
			SPECTATOR_COLOR = new BetterTab().parseColor(configFile.getAsJsonObject().get("spectator_color").getAsString());
			PING_COLOR_NONE = new BetterTab().parseColor(configFile.getAsJsonObject().get("ping_color_none").getAsString());
			PING_COLOR_LOW = new BetterTab().parseColor(configFile.getAsJsonObject().get("ping_color_low").getAsString());
			PING_COLOR_MEDIUM = new BetterTab().parseColor(configFile.getAsJsonObject().get("ping_color_medium").getAsString());
			PING_COLOR_HIGH = new BetterTab().parseColor(configFile.getAsJsonObject().get("ping_color_high").getAsString());
		}
	}

	@Unique
	private int numericalColoriser(int ping) {
		if (ping <= 0) {
			return PING_COLOR_NONE;
		} else if (ping > 300) {
			return PING_COLOR_HIGH;
		} else if (ping > 150) {
			return PING_COLOR_MEDIUM;
		}
		return PING_COLOR_LOW;
	}

	private PlayerListEntry fakePlayer(String fakePlayerName) {
		UUID fakeUUID = UUID.nameUUIDFromBytes(fakePlayerName.getBytes());
		GameProfile fakeProfile = new GameProfile(fakeUUID, fakePlayerName);
        return new PlayerListEntry(fakeProfile, false);
	}
}