package net.nikezono.sqlime.view.candidates;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import org.androidannotations.annotations.EView;

/**
 * Created by nikezono on 2015/03/01.
 */

@EView
public class CandidateButton extends TextView {

    public CandidateButton(Context context){
        super(context);
    }
    public CandidateButton(Context context,AttributeSet attrs){
        super(context,attrs);
    }
    public CandidateButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
