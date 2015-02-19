package net.nikezono.sqlime;

import android.content.Context;
import net.nikezono.sqlime.softkeyboard.R;


/**
 * Created by nikezono on 2015/02/20.
 */
public class SpecialKeyCode {

    public static int KEYCODE_OPEN_MENU;
    public static int KEYCODE_TOGGLE_LETTERCASE;

    public static void initialize(Context context) {
        KEYCODE_OPEN_MENU = context.getResources().getInteger(R.integer.keycode_open_menu);
        KEYCODE_TOGGLE_LETTERCASE = context.getResources().getInteger(R.integer.keycode_toggle_lettercase);
    }
}
