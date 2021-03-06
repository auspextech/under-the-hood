/*
 *  Copyright 2016 Patrick Favre-Bulle
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.favre.lib.hood.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import at.favre.lib.hood.R;
import timber.log.Timber;

/**
 * Dialogs used as detail view in {@link at.favre.lib.hood.internal.entries.KeyValueEntry}. See
 * {@link at.favre.lib.hood.internal.entries.KeyValueEntry.DialogClickAction}
 */
public class KeyValueDetailDialogs {
    private static final String TAG = KeyValueDetailDialogs.class.getName();

    /**
     * DialogFragment Wrapper for the dialog
     */
    public static class DialogFragmentWrapper extends DialogFragment {
        private LogRunnable logFunction;

        public static DialogFragmentWrapper newInstance(CharSequence key, String value) {
            DialogFragmentWrapper frag = new DialogFragmentWrapper();
            Bundle args = new Bundle();
            args.putString("key", String.valueOf(key));
            args.putString("value", value);
            args.putString("tag", null);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onAttach(final Context context) {
            super.onAttach(context);

            if (context instanceof HoodController) {
                logFunction = new LogRunnable() {
                    @Override
                    public void logImpl(String msg) {
                        ((HoodController) context).getCurrentPagesFromThisView().log(msg);
                    }
                };

                if (getDialog() != null) {
                    ((CustomDialog) getDialog()).setLogImpl(logFunction);
                }
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            CustomDialog d = new CustomDialog(getActivity(), getArguments().getString("key"), getArguments().getString("value"), getArguments().getString("tag", TAG));
            d.setLogImpl(logFunction);
            return d;
        }
    }

    /**
     * Custom view dialog
     */
    public static class CustomDialog extends Dialog {
        private final CharSequence key;
        private final String value;
        private final String logTag;
        private LogRunnable logFunction;

        public CustomDialog(Context context, CharSequence key, String value, String logTag) {
            super(new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth));
            this.key = key;
            this.value = value;
            this.logTag = logTag;
            setup();
        }

        private void setup() {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.hoodlib_dialog_detail);
            setTitle(key);
            ((TextView) findViewById(R.id.key)).setText(key);
            ((TextView) findViewById(R.id.value)).setText(value);
            findViewById(R.id.btn_copy_clipboard).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyToClipboard(String.valueOf(key), value, getContext());
                    Toast.makeText(getContext(), R.string.hood_toast_copied, Toast.LENGTH_SHORT).show();
                }
            });
            findViewById(R.id.btn_log).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String logMsg = key + "\n" + value;
                    if (logFunction != null) {
                        logFunction.logImpl(logMsg);
                    } else {
                        Timber.tag(logTag).w(logMsg);
                    }
                    Toast.makeText(getContext(), R.string.hood_toast_logged, Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void setLogImpl(LogRunnable logFunction) {
            this.logFunction = logFunction;
        }
    }

    /**
     * Basic, native styled dialog
     */
    public static class NativeDialog extends AlertDialog {
        private final CharSequence key;
        private final String value;
        private final String logTag;

        public NativeDialog(Context context, CharSequence key, String value, String logTag) {
            super(context);
            this.key = key;
            this.value = value;
            this.logTag = logTag;
            setup();
        }

        private void setup() {
            setTitle(key);
            setMessage(value);
            setButton(BUTTON_POSITIVE, "Copy", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    copyToClipboard(String.valueOf(key), value, getContext());
                    Toast.makeText(getContext(), R.string.hood_toast_copied, Toast.LENGTH_SHORT).show();
                }
            });
            setButton(BUTTON_NEUTRAL, "Close", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            setButton(BUTTON_NEGATIVE, "Log", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Timber.tag(logTag).w(key + "\n" + value);
                    Toast.makeText(getContext(), R.string.hood_toast_logged, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static void copyToClipboard(String label, String value, Context ctx) {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, value);
        clipboard.setPrimaryClip(clip);
    }

    public interface LogRunnable {
        void logImpl(String msg);
    }
}
