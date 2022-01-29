/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import io.github.rosemoe.sora.interfaces.EditorLanguage;
import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextReference;

/**
 * Auto complete window for editing code quicker
 *
 * @author Rose
 */
public class EditorAutoCompleteWindow extends EditorPopupWindow {
    private final CodeEditor mEditor;
    private final ListView mListView;
    private final ProgressBar mPb;
    private final GradientDrawable mBg;
    protected boolean mCancelShowUp = false;
    private int mCurrent = 0;
    private long mRequestTime;
    private int mMaxHeight;
    private EditorCompletionAdapter mAdapter;
    private AutoCompletionThread mThread;

    /**
     * Create a panel instance for the given editor
     *
     * @param editor Target editor
     */
    public EditorAutoCompleteWindow(CodeEditor editor) {
        super(editor, FEATURE_HIDE_WHEN_FAST_SCROLL | FEATURE_SCROLL_AS_CONTENT);
        mEditor = editor;
        mAdapter = new DefaultCompletionItemAdapter();
        RelativeLayout layout = new RelativeLayout(mEditor.getContext());
        mListView = new ListView(mEditor.getContext());
        layout.addView(mListView, new LinearLayout.LayoutParams(-1, -1));
        mPb = new ProgressBar(editor.getContext());
        layout.addView(mPb);
        var params = ((RelativeLayout.LayoutParams) mPb.getLayoutParams());
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.width = params.height = (int) (mEditor.getDpUnit() * 30);
        setContentView(layout);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(editor.getDpUnit() * 8);
        layout.setBackground(gd);
        mBg = gd;
        applyColorScheme();
        mListView.setDividerHeight(0);
        setLoading(true);
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                select(position);
            } catch (Exception e) {
                Toast.makeText(mEditor.getContext(), e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void setAdapter(EditorCompletionAdapter adapter) {
        mAdapter = adapter;
        if (adapter == null) {
            mAdapter = new DefaultCompletionItemAdapter();
        }
    }

    private long requestShow = 0;
    private long requestHide = -1;

    @Override
    public void show() {
        if (mCancelShowUp) {
            return;
        }
        requestShow = System.currentTimeMillis();
        final var requireRequest = mRequestTime;
        mEditor.postDelayed(() -> {
            if (requestHide < requestShow && mRequestTime == requireRequest) {
                super.show();
            }
        }, 70);
    }

    public void hide() {
        super.dismiss();
        mRequestTime = 0;
        requestHide = System.currentTimeMillis();
    }

    public Context getContext() {
        return mEditor.getContext();
    }

    public int getCurrentPosition() {
        return mCurrent;
    }

    /**
     * Apply colors for self
     */
    public void applyColorScheme() {
        EditorColorScheme colors = mEditor.getColorScheme();
        mBg.setStroke(1, colors.getColor(EditorColorScheme.AUTO_COMP_PANEL_CORNER));
        mBg.setColor(colors.getColor(EditorColorScheme.AUTO_COMP_PANEL_BG));
    }

    /**
     * Change layout to loading/idle
     *
     * @param state Whether loading
     */
    public void setLoading(boolean state) {
        mPb.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Move selection down
     */
    public void moveDown() {
        if (mCurrent + 1 >= mListView.getAdapter().getCount()) {
            return;
        }
        mCurrent++;
        ((EditorCompletionAdapter) mListView.getAdapter()).notifyDataSetChanged();
        ensurePosition();
    }

    /**
     * Move selection up
     */
    public void moveUp() {
        if (mCurrent - 1 < 0) {
            return;
        }
        mCurrent--;
        ((EditorCompletionAdapter) mListView.getAdapter()).notifyDataSetChanged();
        ensurePosition();
    }

    /**
     * Perform motion events
     */
    private void performScrollList(int offset) {
        long down = SystemClock.uptimeMillis();
        var ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 0, 0, 0);
        mListView.onTouchEvent(ev);
        ev.recycle();

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_MOVE, 0, offset, 0);
        mListView.onTouchEvent(ev);
        ev.recycle();

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_CANCEL, 0, offset, 0);
        mListView.onTouchEvent(ev);
        ev.recycle();
    }

    /**
     * Make current selection visible
     */
    private void ensurePosition() {
        mListView.post(() -> {
            while (mListView.getFirstVisiblePosition() + 1 > mCurrent && mListView.canScrollList(-1)) {
                performScrollList(mAdapter.getItemHeight() / 2);
            }
            while (mListView.getLastVisiblePosition() - 1 < mCurrent && mListView.canScrollList(1)) {
                performScrollList(-mAdapter.getItemHeight() / 2);
            }
        });
    }

    /**
     * Select current position
     */
    public void select() {
        select(mCurrent);
    }

    /**
     * Select the given position
     *
     * @param pos Index of auto complete item
     */
    public void select(int pos) {
        if (pos == -1) {
            mEditor.getCursor().onCommitText("\n");
            return;
        }
        CompletionItem item = ((EditorCompletionAdapter) mListView.getAdapter()).getItem(pos);
        Cursor cursor = mEditor.getCursor();
        if (!cursor.isSelected()) {
            mCancelShowUp = true;
            mEditor.restartInput();
            mEditor.getText().beginBatchEdit();
            item.performCompletion(mEditor, mEditor.getText(), mThread.mPosition.line, mThread.mPosition.column);
            mEditor.getText().endBatchEdit();
            mCancelShowUp = false;
        }
        mEditor.hideCompletionWindow();
    }

    /**
     * Stop previous completion thread
     */
    public void interruptCompletion() {
        var previous = mThread;
        if (previous != null && previous.isAlive()) {
            previous.interrupt();
            mRequestTime = 0;
        }
        mThread = null;
    }

    /**
     * Start completion at current selection position
     */
    public void requireCompletion() {
        if (!mEditor.isAutoCompletionEnabled() || mCancelShowUp) {
            return;
        }
        var text = mEditor.getText();
        if (text.getCursor().isSelected()) {
            return;
        }
        interruptCompletion();
        mRequestTime = System.nanoTime();
        mCurrent = -1;
        var publisher = new CompletionPublisher(mEditor.getHandler(), () -> {
            mAdapter.notifyDataSetChanged();
            float newHeight = mAdapter.getItemHeight() * mAdapter.getCount();
            setSize(getWidth(), (int) Math.min(newHeight, mMaxHeight));
            if (!isShowing()) {
                show();
            }
        });
        mAdapter.attachValues(this, publisher.getItems());
        mListView.setAdapter(mAdapter);
        mThread = new AutoCompletionThread(mRequestTime, publisher);
        setLoading(true);
        mThread.start();
    }

    public void setMaxHeight(int height) {
        mMaxHeight = height;
    }

    /**
     * Auto-completion Analyzing thread
     *
     * @author Rosemoe
     */
    private class AutoCompletionThread extends Thread implements TextReference.Validator {

        private final long mTime;
        private final TextAnalyzeResult mData;
        private final Bundle mExtra;
        private final CharPosition mPosition;
        private final EditorLanguage mLanguage;
        private final ContentReference mRef;
        private final CompletionPublisher mPublisher;

        public AutoCompletionThread(long requestTime, CompletionPublisher publisher) {
            mTime = requestTime;
            mData = mEditor.getTextAnalyzeResult();
            mPosition = mEditor.getCursor().left();
            mLanguage = mEditor.getEditorLanguage();
            mRef = new ContentReference(mEditor.getText());
            mRef.setValidator(this);
            mPublisher = publisher;
            mExtra = mEditor.getExtraArguments();
        }

        @Override
        public void validate() {
            if (mRequestTime != mTime) {
                throw new TextReference.ValidateFailedException("invalid access: this thread is abandoned by editor framework");
            }
        }

        @Override
        public void run() {
            try {
                mLanguage.requireAutoComplete(mRef, mPosition, mPublisher, mData, mExtra);
                if (mPublisher.hasData()) {
                    mPublisher.updateList(true);
                } else {
                    mEditor.post(EditorAutoCompleteWindow.this::hide);
                }
                mEditor.post(() -> setLoading(false));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

}

