package net.nikezono.sqlime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.nikezono.sqlime.translate.dictionary.SQLite3DictionaryAccesor;

/**
 * Created by nikezono on 2015/09/14.
 */
public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SQLite3DictionaryAccesor.getInstance(context);
        SpecialKeyCode.initialize(context);
    }
}