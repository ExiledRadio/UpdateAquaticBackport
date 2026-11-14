package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.presentation.client.ChatSuggestions;
import com.exiledradio.backportcore.recipe.book.BookNetwork;
import com.exiledradio.backportcore.recipe.book.SmeltingBook;
import com.exiledradio.backportcore.recipe.book.SmeltingBooks;
import com.exiledradio.backportcore.recipe.book.client.ClientBooks;
import com.exiledradio.backportcore.recipe.book.client.GuiFurnaceBook;
import com.exiledradio.backportcore.recipe.book.client.SmeltingBookGui;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.FileUtils;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.List;

/**
 * Command suggestions and the furnace book on a real client, for {@code gradlew runClient -Pselftest=clientcmd}: a
 * flat creative world with cheats, then the chat box (the server's real tab-complete answers through the connection:
 * "/g" lists the g commands, Down moves to give, Tab accepts it, "/give " lists the players, Esc closes the list and leaves /give's usage from the
 * server, plain chat lists names on Tab), then a furnace opened by the server (the book's screen, the unlock sent at
 * holding iron ore, shift-placing through the server with the slot coming back, the ghost for a recipe without its
 * ingredients, the book's open state on the server). Keys are given to the engine in code, not to the window.
 * Logs {@code CMDFURNACE ...} lines and {@code CLIENT SELFTEST DONE pass=N fail=M}, then closes.
 */
@SideOnly(Side.CLIENT)
public final class ClientCommandsFurnaceTest {

    private int ticks;
    private int pass;
    private int fail;
    private int step = -1;
    private int wait;
    private boolean done;
    private BlockPos furnacePos;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientCommandsFurnaceTest());
        UpdateAquaticBackport.LOGGER.info("CLIENT SELFTEST cmdfurnace registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            UpdateAquaticBackport.LOGGER.info("CMDFURNACE ok {}", what);
        } else {
            this.fail++;
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    /** {@code eclipse/screenshots/<name>.png}, of the last frame (the screen as drawn). */
    private static void shot(Minecraft mc, String name) {
        net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.gameDir, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        UpdateAquaticBackport.LOGGER.info("CMDFURNACE screenshot {}", name);
    }

    private static GuiTextField input(GuiChat chat) {
        return ObfuscationReflectionHelper.getPrivateValue(GuiChat.class, chat, "field_146415_a");
    }

    private static void type(Minecraft mc, String text) {
        GuiTextField f = input((GuiChat) mc.currentScreen);
        f.setText(text);
        f.setCursorPositionEnd();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || this.done) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (this.step < 0) {
                if (!(mc.currentScreen instanceof GuiMainMenu) || ++this.ticks < 100) {
                    return;
                }
                mc.gameSettings.pauseOnLostFocus = false;
                mc.gameSettings.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                FileUtils.deleteDirectory(new File(mc.gameDir, "saves/uacmdfurnace"));
                WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.enableCommands();
                mc.launchIntegratedServer("uacmdfurnace", "uacmdfurnace", settings);
                this.step = 0;
                return;
            }
            if (mc.player == null || mc.world == null || mc.getIntegratedServer() == null) {
                return;
            }
            if (this.wait > 0) {
                this.wait--;
                return;
            }
            run(mc);
        } catch (Throwable t) {
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private EntityPlayerMP serverPlayer(Minecraft mc) {
        IntegratedServer server = mc.getIntegratedServer();
        return server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
    }

    private void run(Minecraft mc) {
        ChatSuggestions s = ChatSuggestions.INSTANCE;
        switch (this.step++) {
            case 0:
                this.wait = 60;
                return;
            case 1:
                mc.displayGuiScreen(new GuiChat("/"));
                this.wait = 5;
                return;
            case 2:
                type(mc, "/g");
                this.wait = 20;
                return;
            case 3: {
                shot(mc, "ua_cmd_suggestions");
                List<String> shown = s.shownForTest();
                ok(shown.contains("give") && shown.contains("gamemode") && !shown.contains("help"),
                        "\"/g\": the server's answer lists the g commands: " + shown);
                // The mouse may rest on a row (hovering selects), so Down until give.
                for (int i = 0; i < shown.size() && !"ive".equals(s.previewForTest()); i++) {
                    s.keyForTest(Keyboard.KEY_DOWN, false);
                }
                ok("ive".equals(s.previewForTest()), "Down moves to give; the preview is its rest: " + s.previewForTest());
                ok(s.keyForTest(Keyboard.KEY_TAB, false), "Tab is the list's");
                GuiTextField f = input((GuiChat) mc.currentScreen);
                ok("/give".equals(f.getText()) && f.getCursorPosition() == 5, "Tab accepts give: " + f.getText());
                type(mc, "/give ");
                this.wait = 20;
                return;
            }
            case 4: {
                List<String> shown = s.shownForTest();
                ok(shown.contains(mc.player.getName()), "\"/give \": the players are listed: " + shown);
                ok(s.keyForTest(Keyboard.KEY_ESCAPE, false) && s.shownForTest().isEmpty() && mc.currentScreen instanceof GuiChat,
                        "Esc closes the list, not the chat");
                this.wait = 20;
                return;
            }
            case 5: {
                String usage = s.usageForTest();
                ok(usage != null && usage.contains("<player>") && !usage.startsWith("/give"),
                        "/give's usage from the server, after its name: " + usage);
                ok(!s.keyForTest(Keyboard.KEY_ESCAPE, false), "a second Esc is the chat's");
                String name = mc.player.getName();
                type(mc, "hello " + name.substring(0, 2));
                this.wait = 5;
                return;
            }
            case 6: {
                ok(s.shownForTest().isEmpty(), "plain chat shows no list until Tab");
                s.keyForTest(Keyboard.KEY_TAB, false);
                ok(s.shownForTest().contains(mc.player.getName()), "Tab lists the players' names: " + s.shownForTest());
                mc.displayGuiScreen(null);
                // The furnace, opened by the server.
                EntityPlayerMP player = serverPlayer(mc);
                this.furnacePos = player.getPosition().north(2);
                player.getServerWorld().setBlockState(this.furnacePos, Blocks.FURNACE.getDefaultState());
                player.inventory.clear();
                player.inventory.addItemStackToInventory(new ItemStack(Blocks.IRON_ORE, 12));
                player.inventoryContainer.detectAndSendChanges();
                this.wait = 40;
                return;
            }
            case 7: {
                ClientBooks.State state = ClientBooks.state(SmeltingBook.FURNACE);
                ok(state.unlocked.contains("minecraft:iron_ingot"), "holding iron ore unlocked iron ingots on the client: " + state.unlocked);
                EntityPlayerMP player = serverPlayer(mc);
                player.displayGUIChest((TileEntityFurnace) player.getServerWorld().getTileEntity(this.furnacePos));
                this.wait = 10;
                return;
            }
            case 8: {
                ok(mc.currentScreen instanceof GuiFurnaceBook, "the furnace opens with its book: " + mc.currentScreen);
                if (!(mc.currentScreen instanceof GuiFurnaceBook)) {
                    finish(mc);
                    return;
                }
                SmeltingBookGui book = ((GuiFurnaceBook) mc.currentScreen).smeltingBook();
                if (!book.isVisible()) {
                    book.toggleVisibility();
                }
                // Laid out again with the book open, as opening the screen with it open does.
                mc.currentScreen.setWorldAndResolution(mc, mc.currentScreen.width, mc.currentScreen.height);
                book = ((GuiFurnaceBook) mc.currentScreen).smeltingBook();
                ok(((GuiFurnaceBook) mc.currentScreen).getGuiLeft() > (mc.currentScreen.width - 176) / 2 || mc.currentScreen.width < 379,
                        "the furnace moves right of the book: guiLeft " + ((GuiFurnaceBook) mc.currentScreen).getGuiLeft());
                ok(book.shownForTest().contains("minecraft:iron_ingot") && book.craftableForTest("minecraft:iron_ingot"),
                        "the book lists iron ingots as smeltable: " + book.shownForTest());
                BookNetwork.sendToServer(new BookNetwork.Place(mc.player.openContainer.windowId, SmeltingBook.FURNACE,
                        "minecraft:iron_ingot", true));
                this.wait = 10;
                return;
            }
            case 9: {
                shot(mc, "ua_furnace_book");
                ItemStack in = mc.player.openContainer.getSlot(0).getStack();
                ok(in.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.IRON_ORE) && in.getCount() == 12,
                        "shift-placed through the server, the input slot came back with 12 iron ore: " + in);
                EntityPlayerMP player = serverPlayer(mc);
                ok(SmeltingBooks.data(player, SmeltingBooks.get(SmeltingBook.FURNACE)).isOpen(), "the server keeps the book open");
                SmeltingBooks.unlock(player, SmeltingBooks.get(SmeltingBook.FURNACE), java.util.Collections.singleton("minecraft:cooked_beef"));
                this.wait = 5;
                return;
            }
            case 10:
                BookNetwork.sendToServer(new BookNetwork.Place(mc.player.openContainer.windowId, SmeltingBook.FURNACE,
                        "minecraft:cooked_beef", false));
                this.wait = 10;
                return;
            case 11: {
                shot(mc, "ua_furnace_ghost");
                SmeltingBookGui book = ((GuiFurnaceBook) mc.currentScreen).smeltingBook();
                ok(book.ghostSizeForTest() == 3, "no beef: the ghost shows (output, input, fuel): " + book.ghostSizeForTest());
                ok(mc.player.openContainer.getSlot(0).getStack().isEmpty(), "and the ore went back to the inventory");
                book.toggleVisibility();
                mc.displayGuiScreen(null);
                finish(mc);
                return;
            }
            default:
                finish(mc);
        }
    }

    private void finish(Minecraft mc) {
        this.done = true;
        UpdateAquaticBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }
}
