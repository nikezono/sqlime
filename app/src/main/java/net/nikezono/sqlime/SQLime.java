/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nikezono.sqlime;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.ArrayAdapter;

import net.nikezono.sqlime.softkeyboard.R;
import net.nikezono.sqlime.translate.RomaToKanaTranslater;
import net.nikezono.sqlime.translate.dictionary.SQLite3DictionaryAccesor;
import net.nikezono.sqlime.view.candidates.CandidatesView;
import net.nikezono.sqlime.view.input.LatinKeyboard;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.api.BackgroundExecutor;

import java.util.ArrayList;

import hugo.weaving.DebugLog;

@EService
public class SQLime extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static SQLime mService;
    private InputMethodManager mInputMethodManager;

    private KeyboardView mInputView;
    private CandidatesView mCandidatesView;

    private SQLite3DictionaryAccesor mDictionary;
    
    private ComposingText mComposing = new ComposingText();
    private static final ArrayList<CandidateWord> EMPTYLIST = new ArrayList<CandidateWord>();
    private ArrayAdapter<CandidateWord> mCandidatesAdapter;

    private boolean mJapaneseInputMode;
    private int mLastDisplayWidth;
    
    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    
    private LatinKeyboard mCurKeyboard;

    private String mLastComposed = "";
    private CandidateWord mLastCommited;

    public static SQLime getService(){
        return mService;
    }

    @Override public void onCreate() {
        super.onCreate();
        mService = this;

        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        SpecialKeyCode.initialize(this);
        mCandidatesAdapter = new ArrayAdapter<>(this,R.layout.candidate_text); // @todo UIスレッドに色々させすぎ
        mDictionary = new SQLite3DictionaryAccesor(this);

    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }

        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
    }
    
    /**
     * Called by the framework when your view for creating input_view needs to be generated.
     * This will be called the first time your input_view method is displayed,
     * and every time it needs to be re-created such as due to a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (KeyboardView) getLayoutInflater().inflate(R.layout.input_view,null);
        mInputView.setOnKeyboardActionListener(this); // @todo 分割
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidatesView = (CandidatesView) getLayoutInflater().inflate(R.layout.candidates_view,null);
        mCandidatesView.setAdapter(mCandidatesAdapter);
        return mCandidatesView;
    }

    /**
     * This is the main point where we do our initialization of the input_view method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.clear();
        mCandidatesAdapter.clear();
        updateCandidatesView(EMPTYLIST);

        mJapaneseInputMode = false;
        
        // どのキーボードを初期表示にするか、エディタのattributeから判断する
        // @todo すべてのattributeを網羅していない
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_PHONE:
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_TEXT:
                mCurKeyboard = mQwertyKeyboard;
                mJapaneseInputMode = true;

                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    mJapaneseInputMode = false;
                }
                
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    mJapaneseInputMode = false;
                }
                
                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    mJapaneseInputMode = true;
                }

                break;
                
            default:
                // For all unknown input_view types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
        }

        setCandidatesViewShown(mJapaneseInputMode);

        if(mJapaneseInputMode){
            prediction(new CandidateWord("",0)); // @note 0=文頭
        }
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        mComposing.clear();
        mCandidatesAdapter.clear();
        updateCandidatesView(EMPTYLIST);
        setCandidatesViewShown(false);

        mCurKeyboard = mQwertyKeyboard;

    }
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        mInputView.setKeyboard(mCurKeyboard);
    }

    @Override public void onFinishInputView(boolean finishingInput){
        super.onFinishInputView(finishingInput);
    }

    /**
     * For I18n, override this method and change interfaces.
     **/
    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        boolean isMovedSelection = (newSelStart != candidatesEnd || newSelEnd != candidatesEnd);

        // 確定する @todo 設定画面で挙動変える?
        if (mComposing.hasComposingText() && isMovedSelection) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
            mComposing.clear();
            mCandidatesAdapter.clear();
            updateCandidatesView(EMPTYLIST);
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        // @note フルスクリーンモードでのエディタからの補完候補を受け取る.フルスクリーンフラグは自分でevaluateさせる
    }
    
    /**
     * ハードキーイベントのインターセプト
     * IMEがフォーカスを与えられていない際にもアプリケーション向けのキーイベントをInterceptできる
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    /**
     * ハードキーイベントのインターセプト
     * IMEがフォーカスを与えられていない際にもアプリケーション向けのキーイベントをInterceptできる
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    /**
     * commit
     * 現在アタッチしているエディタに入力を確定させるヘルパ
     */
    private void commit() {
        // モードによって区別して入力
        if(mJapaneseInputMode){
            commit(mComposing.getConvertedString());
        }else if(!"".equals(mLastComposed)){
            commit(mLastComposed);
            mLastComposed = "";
        }else{
            commit(mComposing.getInputtedString());
        }

    }
    /**
     * @param candidate 入力する文字列
     */
    private void commit(String candidate) {
        getCurrentInputConnection().commitText(candidate, 1);
    }

    /**
     * @param candidate 入力するCandidateWord
     */
    private void commit(CandidateWord candidate) {
        getCurrentInputConnection().commitText(candidate.getWord(), 1);
    }

    /**
     * compose
     * 現在アタッチしているエディタに未確定文字列(underlined)を送るヘルパ
     */
    private void compose(){
        // 入力中の文字列が存在しない場合はreturn
        if(!mComposing.hasComposingText()) return;

        compose(mComposing.getConvertedString());
        updateCandidates();
    }

    /**
     * @param text
     */
    private void compose(String text){
        getCurrentInputConnection().setComposingText(text, 1);
        mLastComposed = text;
    }

    /**
     * prediction
     * 前回の入力結果から予測を行う
     */
    @Background
    public void prediction(){
        if(mLastCommited == null || mLastCommited.getWord().length() == 0) return;
        prediction(mLastCommited);
    }

    /**
     * @param seed 予測を行うCandidateWord
     */
    @Background
    public void prediction(CandidateWord seed){
        updateCandidatesView(mDictionary.getWordsByLastInput(seed));
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    @DebugLog
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    // Implementation of KeyboardViewListener
    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode == '\n'){
            handleEnter();
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == SpecialKeyCode.KEYCODE_CHANGE_INPUTMODE){
            handleChangeInputMode();
        } else if (primaryCode == SpecialKeyCode.KEYCODE_TOGGLE_LETTERCASE){
            handleToggleLetterCase();
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            handleKeyCodeModeChange();
        } else if (primaryCode == SpecialKeyCode.KEYCODE_MOVE_CARET_LEFT) {
            handleMoveLeft();
        } else if (primaryCode == SpecialKeyCode.KEYCODE_MOVE_CARET_RIGHT) {
            handleMoveRight();
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    /**
     * エンターキー. 入力がある ? 確定 : エディタに送信
     */
    private void handleEnter(){
        if(mComposing.hasComposingText()){
            commit();
        }else {
            keyDownUp(KeyEvent.KEYCODE_ENTER);
        }
    }
    /**
     * Deleteキー. 入力がある ? 削除 : エディタに送信
     */
    private void handleBackspace() {
        if(mComposing.hasComposingText()){
            mComposing.backspace();
            compose();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
    }

    /**
     * すべての文字キー. 直接入力モード ? Compose : Commit
     */

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        String inputted = String.valueOf((char) primaryCode);
        if (mJapaneseInputMode) {
            mComposing.input(inputted);
            compose();

        } else {
            commit(inputted); // 直接入力
        }
    }

    /**
     * 日本語入力モード切り替え
     */
    private void handleChangeInputMode(){
        mJapaneseInputMode = !mJapaneseInputMode;
    }

    /**
     * 小文字/大文字(a/A) あ->ぁ
     */
    private void handleToggleLetterCase(){
        if(mJapaneseInputMode){
            String last = mComposing.getLastConverted();
            mComposing.backspace();
            mComposing.input(RomaToKanaTranslater.toKomojiDakuonHandakuon(last));
            compose();
        }else{
            // @see http://developer.android.com/reference/android/view/inputmethod/InputConnection.html#getTextBeforeCursor(int, int)
            CharSequence text = getCurrentInputConnection().getTextBeforeCursor(1,0);
            if(text.length() < 1) return;
            InputConnection ic = getCurrentInputConnection();
            ic.beginBatchEdit();
            ic.deleteSurroundingText(1,0);
            commit(RomaToKanaTranslater.toggleAlphabet(text.charAt(0)).toString());
            ic.endBatchEdit();
        }
    }

    /**
     * キーボード切り替え
     */
    private void handleKeyCodeModeChange(){
        if (mCurKeyboard == mSymbolsKeyboard) {
            mCurKeyboard = mSymbolsShiftedKeyboard;
        } else if(mCurKeyboard == mSymbolsShiftedKeyboard){
            mCurKeyboard = mQwertyKeyboard;
        } else if(mCurKeyboard == mQwertyKeyboard){
            mCurKeyboard = mSymbolsKeyboard;
        }
        mInputView.setKeyboard(mCurKeyboard);
    }

    /**
     * キャレットを左に移動
     */
    @DebugLog
    public void handleMoveLeft(){
        //テキストが無い時はreturn
        if(getCurrentInputConnection().getTextBeforeCursor(1, 0).length() == 0
                && getCurrentInputConnection().getTextAfterCursor(1, 0).length() == 0){
            return;
        }
        //未確定の文字がある場合はreturn
        if(mComposing.hasComposingText()){
            return;
        }

        moveCaretLeft();
    }

    /**
     * キャレットを右に移動
     */
    @DebugLog
    public void handleMoveRight(){
        //テキストが無い時はreturn
        if(getCurrentInputConnection().getTextBeforeCursor(1, 0).length() == 0
                && getCurrentInputConnection().getTextAfterCursor(1, 0).length() == 0){
            return;
        }

        //文末のとき補完
        if(getCurrentInputConnection().getTextAfterCursor(1, 0).length() == 0){
            endOfSentencePrediction();
        }else{
            moveCaretRight();
        }
    }

    private void moveCaretLeft(){
        getCurrentInputConnection().commitText("", -1);
    }

    private void moveCaretRight(){
        getCurrentInputConnection().commitText("", 2);
    }

    /**
     * 文末補完
     */
    @DebugLog
    private void endOfSentencePrediction(){
        prediction(new CandidateWord("",0)); // @note 0=文末
    }

    /**
     * @note これ、何かよくわからないしドキュメントにも説明が不十分
     * @param text
     */
    @Override public void onText(CharSequence text) {
        commit();
        commit(text.toString());
    }

    @Background(id="update_candidates")
    public void updateCandidates() {
        BackgroundExecutor.cancelAll("update_candidates",true);
        if(mJapaneseInputMode && mComposing.hasComposingText()) {
            updateCandidatesView(mDictionary.getWordsByYomigana(mComposing.getConvertedString()));
        }
    }

    @UiThread
    public void updateCandidatesView(ArrayList<CandidateWord> cands){
        mCandidatesAdapter.clear();
        mCandidatesAdapter.addAll(cands);
    }

    @DebugLog
    @UiThread
    public void onClickCandidate(Integer index){
        CandidateWord candidate = mCandidatesAdapter.getItem(index);
        commit(candidate);
        mLastCommited = candidate;
        prediction();
    }

    /**
     * Keyboard ActionListeners
     */
    @Override public void swipeRight() {
    }

    @Override public void swipeLeft() {
    }

    @Override public void swipeDown() {
    }

    @Override public void swipeUp() {
    }

    @Override public void onPress(int primaryCode) {
    }

    @Override public void onRelease(int primaryCode) {
    }
}
