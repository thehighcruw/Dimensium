/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.imgui;

import org.lwjgl.input.Keyboard;

import imgui.flag.ImGuiKey;

/** Maps LWJGL 2 key codes to ImGuiKey constants. Returns -1 for unmapped keys. */
public final class LwjglKeyMap {

    private LwjglKeyMap() {}

    public static int toImGui(int key) {
        return switch (key) {
            // Control / editing
            case Keyboard.KEY_ESCAPE -> ImGuiKey.Escape;
            case Keyboard.KEY_RETURN -> ImGuiKey.Enter;
            case Keyboard.KEY_NUMPADENTER -> ImGuiKey.KeypadEnter;
            case Keyboard.KEY_BACK -> ImGuiKey.Backspace;
            case Keyboard.KEY_DELETE -> ImGuiKey.Delete;
            case Keyboard.KEY_INSERT -> ImGuiKey.Insert;
            case Keyboard.KEY_SPACE -> ImGuiKey.Space;
            case Keyboard.KEY_TAB -> ImGuiKey.Tab;
            // Navigation
            case Keyboard.KEY_LEFT -> ImGuiKey.LeftArrow;
            case Keyboard.KEY_RIGHT -> ImGuiKey.RightArrow;
            case Keyboard.KEY_UP -> ImGuiKey.UpArrow;
            case Keyboard.KEY_DOWN -> ImGuiKey.DownArrow;
            case Keyboard.KEY_HOME -> ImGuiKey.Home;
            case Keyboard.KEY_END -> ImGuiKey.End;
            case Keyboard.KEY_PRIOR -> ImGuiKey.PageUp;
            case Keyboard.KEY_NEXT -> ImGuiKey.PageDown;
            // Modifiers
            case Keyboard.KEY_LCONTROL -> ImGuiKey.LeftCtrl;
            case Keyboard.KEY_RCONTROL -> ImGuiKey.RightCtrl;
            case Keyboard.KEY_LSHIFT -> ImGuiKey.LeftShift;
            case Keyboard.KEY_RSHIFT -> ImGuiKey.RightShift;
            case Keyboard.KEY_LMENU -> ImGuiKey.LeftAlt;
            case Keyboard.KEY_RMENU -> ImGuiKey.RightAlt;
            case Keyboard.KEY_LMETA -> ImGuiKey.LeftSuper;
            case Keyboard.KEY_RMETA -> ImGuiKey.RightSuper;
            // Letters
            case Keyboard.KEY_A -> ImGuiKey.A;
            case Keyboard.KEY_B -> ImGuiKey.B;
            case Keyboard.KEY_C -> ImGuiKey.C;
            case Keyboard.KEY_D -> ImGuiKey.D;
            case Keyboard.KEY_E -> ImGuiKey.E;
            case Keyboard.KEY_F -> ImGuiKey.F;
            case Keyboard.KEY_G -> ImGuiKey.G;
            case Keyboard.KEY_H -> ImGuiKey.H;
            case Keyboard.KEY_I -> ImGuiKey.I;
            case Keyboard.KEY_J -> ImGuiKey.J;
            case Keyboard.KEY_K -> ImGuiKey.K;
            case Keyboard.KEY_L -> ImGuiKey.L;
            case Keyboard.KEY_M -> ImGuiKey.M;
            case Keyboard.KEY_N -> ImGuiKey.N;
            case Keyboard.KEY_O -> ImGuiKey.O;
            case Keyboard.KEY_P -> ImGuiKey.P;
            case Keyboard.KEY_Q -> ImGuiKey.Q;
            case Keyboard.KEY_R -> ImGuiKey.R;
            case Keyboard.KEY_S -> ImGuiKey.S;
            case Keyboard.KEY_T -> ImGuiKey.T;
            case Keyboard.KEY_U -> ImGuiKey.U;
            case Keyboard.KEY_V -> ImGuiKey.V;
            case Keyboard.KEY_W -> ImGuiKey.W;
            case Keyboard.KEY_X -> ImGuiKey.X;
            case Keyboard.KEY_Y -> ImGuiKey.Y;
            case Keyboard.KEY_Z -> ImGuiKey.Z;
            // Digits (row)
            case Keyboard.KEY_0 -> ImGuiKey._0;
            case Keyboard.KEY_1 -> ImGuiKey._1;
            case Keyboard.KEY_2 -> ImGuiKey._2;
            case Keyboard.KEY_3 -> ImGuiKey._3;
            case Keyboard.KEY_4 -> ImGuiKey._4;
            case Keyboard.KEY_5 -> ImGuiKey._5;
            case Keyboard.KEY_6 -> ImGuiKey._6;
            case Keyboard.KEY_7 -> ImGuiKey._7;
            case Keyboard.KEY_8 -> ImGuiKey._8;
            case Keyboard.KEY_9 -> ImGuiKey._9;
            // Function keys
            case Keyboard.KEY_F1 -> ImGuiKey.F1;
            case Keyboard.KEY_F2 -> ImGuiKey.F2;
            case Keyboard.KEY_F3 -> ImGuiKey.F3;
            case Keyboard.KEY_F4 -> ImGuiKey.F4;
            case Keyboard.KEY_F5 -> ImGuiKey.F5;
            case Keyboard.KEY_F6 -> ImGuiKey.F6;
            case Keyboard.KEY_F7 -> ImGuiKey.F7;
            case Keyboard.KEY_F8 -> ImGuiKey.F8;
            case Keyboard.KEY_F9 -> ImGuiKey.F9;
            case Keyboard.KEY_F10 -> ImGuiKey.F10;
            case Keyboard.KEY_F11 -> ImGuiKey.F11;
            case Keyboard.KEY_F12 -> ImGuiKey.F12;
            case Keyboard.KEY_F13 -> ImGuiKey.F13;
            case Keyboard.KEY_F14 -> ImGuiKey.F14;
            case Keyboard.KEY_F15 -> ImGuiKey.F15;
            case Keyboard.KEY_F16 -> ImGuiKey.F16;
            case Keyboard.KEY_F17 -> ImGuiKey.F17;
            case Keyboard.KEY_F18 -> ImGuiKey.F18;
            case Keyboard.KEY_F19 -> ImGuiKey.F19;
            // Numpad
            case Keyboard.KEY_NUMPAD0 -> ImGuiKey.Keypad0;
            case Keyboard.KEY_NUMPAD1 -> ImGuiKey.Keypad1;
            case Keyboard.KEY_NUMPAD2 -> ImGuiKey.Keypad2;
            case Keyboard.KEY_NUMPAD3 -> ImGuiKey.Keypad3;
            case Keyboard.KEY_NUMPAD4 -> ImGuiKey.Keypad4;
            case Keyboard.KEY_NUMPAD5 -> ImGuiKey.Keypad5;
            case Keyboard.KEY_NUMPAD6 -> ImGuiKey.Keypad6;
            case Keyboard.KEY_NUMPAD7 -> ImGuiKey.Keypad7;
            case Keyboard.KEY_NUMPAD8 -> ImGuiKey.Keypad8;
            case Keyboard.KEY_NUMPAD9 -> ImGuiKey.Keypad9;
            case Keyboard.KEY_DECIMAL -> ImGuiKey.KeypadDecimal;
            case Keyboard.KEY_DIVIDE -> ImGuiKey.KeypadDivide;
            case Keyboard.KEY_MULTIPLY -> ImGuiKey.KeypadMultiply;
            case Keyboard.KEY_SUBTRACT -> ImGuiKey.KeypadSubtract;
            case Keyboard.KEY_ADD -> ImGuiKey.KeypadAdd;
            case Keyboard.KEY_NUMPADEQUALS -> ImGuiKey.KeypadEqual;
            case Keyboard.KEY_NUMLOCK -> ImGuiKey.NumLock;
            // Punctuation / symbols
            case Keyboard.KEY_APOSTROPHE -> ImGuiKey.Apostrophe;
            case Keyboard.KEY_COMMA -> ImGuiKey.Comma;
            case Keyboard.KEY_MINUS -> ImGuiKey.Minus;
            case Keyboard.KEY_PERIOD -> ImGuiKey.Period;
            case Keyboard.KEY_SLASH -> ImGuiKey.Slash;
            case Keyboard.KEY_SEMICOLON -> ImGuiKey.Semicolon;
            case Keyboard.KEY_EQUALS -> ImGuiKey.Equal;
            case Keyboard.KEY_LBRACKET -> ImGuiKey.LeftBracket;
            case Keyboard.KEY_BACKSLASH -> ImGuiKey.Backslash;
            case Keyboard.KEY_RBRACKET -> ImGuiKey.RightBracket;
            case Keyboard.KEY_GRAVE -> ImGuiKey.GraveAccent;
            // Lock / system
            case Keyboard.KEY_CAPITAL -> ImGuiKey.CapsLock;
            case Keyboard.KEY_SCROLL -> ImGuiKey.ScrollLock;
            case Keyboard.KEY_SYSRQ -> ImGuiKey.PrintScreen;
            case Keyboard.KEY_PAUSE -> ImGuiKey.Pause;
            case Keyboard.KEY_APPS -> ImGuiKey.Menu;
            default -> -1;
        };
    }
}
