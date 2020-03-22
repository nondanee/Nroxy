package dev.nondanee.nroxy;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;


public class EditUrlPreference extends EditTextPreference {
    public EditUrlPreference(Context context) {
        super(context);
        onCreate();
    }

    public EditUrlPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        onCreate();
    }

    public EditUrlPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        onCreate();
    }

    public EditUrlPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        onCreate();
    }

    private boolean validate(String address) {
        if (!URLUtil.isHttpUrl(address)) return false;
        try {
            URL url = new URL(address);
            return !url.getHost().equals("") && url.getPort() < 65536 && (url.getPath().equals("") || url.getPath().equals("/"));
        }
        catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // FROM https://stackoverflow.com/questions/9966931/do-not-close-dialogpreference-onclick-if-condition-not-met/21849519#21849519
        final AlertDialog alertDialog = (AlertDialog) getDialog();
        alertDialog.setCanceledOnTouchOutside(false);
        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = getEditText().getText().toString();
                if (validate(address)) {
                    alertDialog.dismiss();
                    onDialogClosed(true);
                }
                else {
                    Toast.makeText(getContext(), getContext().getResources().getString(R.string.invalid_address), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void onCreate() {
        final EditText editText = getEditText();

        // FROM https://stackoverflow.com/questions/14195207/put-constant-text-inside-edittext-which-should-be-non-editable-android/14195571#14195571
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable string) {
                if(!string.toString().startsWith("http://")){
                    editText.setText("http://");
                    Selection.setSelection(editText.getText(), editText.getText().length());
                }
            }
        });
    }
}