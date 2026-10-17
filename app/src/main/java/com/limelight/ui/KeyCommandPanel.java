package com.limelight.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import com.limelight.R;
import com.limelight.nvstream.input.KeyboardPacket;

/**
 * Modal panel of special keys and key combinations that an Android soft keyboard
 * cannot produce (Esc, Home, Delete, F-keys, Alt+F4, ...).
 *
 * <p>The trigger button, scrim, and content container are declared in activity_game.xml;
 * this class populates the container with buttons and toggles visibility. Actually sending
 * the keys is the caller's job, via {@link KeyCommandListener}.
 *
 * <p>There is deliberately no gesture trigger: OEM skins (ColorOS among them) claim
 * multi-finger gestures for screenshots and split screen and pilfer the touch stream
 * before the app sees it, so a gesture cannot be made reliable across devices.
 */
public class KeyCommandPanel {
    public interface KeyCommandListener {
        void onKeyCommand(int vkCode, byte modifiers);
        void onShowKeyboard();
    }

    private static final byte CTRL = KeyboardPacket.MODIFIER_CTRL;
    private static final byte SHIFT = KeyboardPacket.MODIFIER_SHIFT;
    private static final byte ALT = KeyboardPacket.MODIFIER_ALT;

    /*
     * Windows virtual key codes.
     *
     * Deliberately NOT reusing KeyboardTranslator's VK_* constants: several of those hold
     * Android keycodes rather than Windows VKs (its VK_PRINTSCREEN is 154, whereas the
     * Windows VK for Print Screen is 0x2C), so reusing them would send the wrong key.
     */
    private static final int VK_BACKSPACE = 0x08;
    private static final int VK_TAB = 0x09;
    private static final int VK_ENTER = 0x0D;
    private static final int VK_ESCAPE = 0x1B;
    private static final int VK_PAGE_UP = 0x21;
    private static final int VK_PAGE_DOWN = 0x22;
    private static final int VK_END = 0x23;
    private static final int VK_HOME = 0x24;
    private static final int VK_LEFT = 0x25;
    private static final int VK_UP = 0x26;
    private static final int VK_RIGHT = 0x27;
    private static final int VK_DOWN = 0x28;
    private static final int VK_SNAPSHOT = 0x2C;
    private static final int VK_DELETE = 0x2E;
    private static final int VK_C = 0x43;
    private static final int VK_V = 0x56;
    private static final int VK_LWIN = 0x5B;
    private static final int VK_F1 = 0x70;
    private static final int VK_F4 = 0x73;

    private static final int FUNCTION_KEY_COUNT = 12;

    /*
     * Labels are key legends (identical to what is printed on a physical keyboard) rather
     * than prose, so they are intentionally not localized.
     */
    private static final Command[] NAV_COMMANDS = {
            new Command("Esc", VK_ESCAPE, (byte) 0),
            new Command("Tab", VK_TAB, (byte) 0),
            new Command("Enter", VK_ENTER, (byte) 0),
            new Command("Bksp", VK_BACKSPACE, (byte) 0),
            new Command("Del", VK_DELETE, (byte) 0),
            new Command("PrtSc", VK_SNAPSHOT, (byte) 0),

            new Command("Home", VK_HOME, (byte) 0),
            new Command("End", VK_END, (byte) 0),
            new Command("PgUp", VK_PAGE_UP, (byte) 0),
            new Command("PgDn", VK_PAGE_DOWN, (byte) 0),
            new Command("Win", VK_LWIN, (byte) 0),
            new Command("↑", VK_UP, (byte) 0),

            new Command("←", VK_LEFT, (byte) 0),
            new Command("↓", VK_DOWN, (byte) 0),
            new Command("→", VK_RIGHT, (byte) 0),
    };

    /*
     * All of these are delivered as ordinary keystrokes. Ctrl+Alt+Del is deliberately
     * absent: it is the Windows Secure Attention Sequence, which SendInput (what the host
     * uses to inject input) cannot generate. Ctrl+Shift+Esc opens Task Manager instead.
     */
    private static final Command[] COMBO_COMMANDS = {
            new Command("Ctrl+C", VK_C, CTRL),
            new Command("Ctrl+V", VK_V, CTRL),
            new Command("Ctrl+Shift+Esc", VK_ESCAPE, (byte) (CTRL | SHIFT)),
            new Command("Alt+F4", VK_F4, ALT),
            new Command("Alt+Tab", VK_TAB, ALT),
            new Command("Ctrl+Esc", VK_ESCAPE, CTRL),
            new Command("Alt+Enter", VK_ENTER, ALT),
    };

    // Phone screens are only ~360dp tall in landscape. Rows are 40dp, so the panel must
    // stay at seven rows or the last one gets clipped off the bottom: 2 nav rows,
    // 2 function rows, 2 combo rows, and 1 action row.
    private static final int NAV_COLUMNS = 8;
    private static final int FUNCTION_COLUMNS = 6;
    private static final int COMBO_COLUMNS = 4;

    // Muted category colors, with dark text for contrast on the light pastel fills.
    // 80% alpha lets the stream show through the panel a little (see styleButton()).
    private static final int COLOR_NAV = 0xCC8FAFC9;       // soft blue
    private static final int COLOR_FUNCTION = 0xCC9DBEA5;  // soft green
    private static final int COLOR_COMBO = 0xCCD6BA91;     // soft amber
    private static final int COLOR_KEYBOARD = 0xCC8CBDB8;  // soft teal
    private static final int COLOR_CLOSE = 0xCCC98F8F;     // soft red
    private static final int COLOR_BUTTON_TEXT = 0xFF202020;

    private final View scrim;
    private final View triggerButton;
    private final KeyCommandListener listener;

    private static class Command {
        final String label;
        final int vkCode;
        final byte modifiers;

        Command(String label, int vkCode, byte modifiers) {
            this.label = label;
            this.vkCode = vkCode;
            this.modifiers = modifiers;
        }
    }

    public KeyCommandPanel(View scrim, View triggerButton, KeyCommandListener listener) {
        this.scrim = scrim;
        this.triggerButton = triggerButton;
        this.listener = listener;

        triggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show();
            }
        });

        Context context = scrim.getContext();
        LinearLayout content = scrim.findViewById(R.id.keyCommandPanelContent);

        content.addView(buildGrid(context, NAV_COLUMNS, NAV_COMMANDS, COLOR_NAV));
        content.addView(buildGrid(context, FUNCTION_COLUMNS, buildFunctionKeys(), COLOR_FUNCTION));
        content.addView(buildGrid(context, COMBO_COLUMNS, COMBO_COMMANDS, COLOR_COMBO));
        content.addView(buildActionRow(context), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Tapping the panel itself must not dismiss it.
        content.setClickable(true);

        // Tapping outside the panel dismisses it. The scrim also swallows every touch that
        // reaches it, which is what keeps the host mouse still while the panel is open.
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
    }

    private static Command[] buildFunctionKeys() {
        Command[] commands = new Command[FUNCTION_KEY_COUNT];
        for (int i = 0; i < FUNCTION_KEY_COUNT; i++) {
            commands[i] = new Command("F" + (i + 1), VK_F1 + i, (byte) 0);
        }
        return commands;
    }

    private GridLayout buildGrid(Context context, int columnCount, Command[] commands, int color) {
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(columnCount);

        for (final Command command : commands) {
            Button button = new Button(context);
            button.setText(command.label);
            button.setTextSize(12);
            button.setMinWidth(dp(context, 48));
            button.setMinHeight(dp(context, 40));
            button.setPadding(dp(context, 6), 0, dp(context, 6), 0);
            styleButton(context, button, color);

            // streamView must keep focus for physical keyboard forwarding, so tapping the
            // panel must never move focus to a button.
            button.setFocusable(false);
            button.setFocusableInTouchMode(false);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onKeyCommand(command.vkCode, command.modifiers);
                }
            });

            grid.addView(button);
        }

        return grid;
    }

    private LinearLayout buildActionRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.END);

        Button keyboard = new Button(context);
        keyboard.setText("Keyboard");
        keyboard.setTextSize(12);
        keyboard.setMinWidth(dp(context, 48));
        keyboard.setMinHeight(dp(context, 40));
        keyboard.setPadding(dp(context, 6), 0, dp(context, 6), 0);
        keyboard.setFocusable(false);
        keyboard.setFocusableInTouchMode(false);
        styleButton(context, keyboard, COLOR_KEYBOARD);
        keyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss the panel first, then let the owner raise the IME.
                hide();
                listener.onShowKeyboard();
            }
        });
        row.addView(keyboard);

        Button close = new Button(context);
        close.setText("✕");
        close.setMinWidth(dp(context, 48));
        close.setMinHeight(dp(context, 40));
        close.setFocusable(false);
        close.setFocusableInTouchMode(false);
        styleButton(context, close, COLOR_CLOSE);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        row.addView(close);
        return row;
    }

    private static void styleButton(Context context, Button button, int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(context, 6));
        // A light edge reads as a glass highlight against the dark scrim.
        background.setStroke(dp(context, 1), 0x40FFFFFF);
        button.setBackground(background);
        button.setTextColor(COLOR_BUTTON_TEXT);
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    public void show() {
        // The on-screen controller injects its buttons into the same parent FrameLayout at
        // runtime, which puts them above XML children. Without this the OSC would render on
        // top of the panel and stay tappable.
        scrim.bringToFront();
        scrim.setVisibility(View.VISIBLE);
    }

    public void hide() {
        scrim.setVisibility(View.GONE);
    }

    public boolean isShowing() {
        return scrim.getVisibility() == View.VISIBLE;
    }

    /** Shows or hides the button that opens the panel, independent of the panel itself. */
    public void setTriggerVisible(boolean visible) {
        triggerButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
