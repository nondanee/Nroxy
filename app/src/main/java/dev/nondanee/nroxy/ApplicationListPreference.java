package dev.nondanee.nroxy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApplicationListPreference extends MultiSelectListPreference {
//    private boolean[] checkedItems;
    private List<ApplicationItem> applicationItems = new ArrayList<>();

    public ApplicationListPreference(Context context) {
        super(context);
        onCreate();
    }

    public ApplicationListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        onCreate();
    }

    public ApplicationListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onCreate();
    }

    public ApplicationListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        onCreate();
    }

//    private void onCreate() {
//        List<CharSequence> entries = new ArrayList<CharSequence>();
//        for (CharSequence entry : getEntries()) {
//            entries.add(entry);
//        }
//
//        List<CharSequence> entryValues = new ArrayList<CharSequence>();
//        for (CharSequence entryValue : getEntryValues()) {
//            entryValues.add(entryValue);
//        }
//
//        Intent intent = new Intent(Intent.ACTION_MAIN, null);
//        intent.addCategory(Intent.CATEGORY_LAUNCHER);
//
//        PackageManager packageManager = getContext().getPackageManager();
//        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
//        Collections.sort(list, new ResolveInfo.DisplayNameComparator(packageManager));
//
//        for (ResolveInfo info : list) {
//            if ((info.activityInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) {
//                entryValues.add(info.activityInfo.packageName);
//                entries.add(info.loadLabel(packageManager).toString());
//            }
//        }
//
//        setEntries(entries.toArray(new CharSequence[entries.size()]));
//        setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
//    }

    private void onCreate() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager packageManager = getContext().getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA);
        Collections.sort(list, new ResolveInfo.DisplayNameComparator(packageManager));

        for (ResolveInfo info : list) {
            if ((info.activityInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) {
                applicationItems.add(new ApplicationItem(info.loadLabel(packageManager).toString(), info.activityInfo.packageName, info.loadIcon(packageManager)));
            }
        }
    }


    private class ApplicationItem {
        protected String applicationName;
        protected String packageName;
        protected Drawable applicationIcon;
        protected boolean checked;

        public ApplicationItem(String applicationName, String packageName, Drawable applicationIcon) {
            this.applicationName = applicationName;
            this.packageName = packageName;
            this.applicationIcon = applicationIcon;
        }
    }

    // FROM https://stackoverflow.com/questions/32226245/how-to-set-icon-in-listpreferences-item-in-android/41806449#41806449
    private class ApplicationArrayAdapter extends ArrayAdapter<ApplicationItem> {
        private Context context;
        private List<ApplicationItem> applicationItems;
        private int resource;

        private class ViewHolder {
            protected ImageView icon;
            protected TextView title;
            protected TextView summary;
            protected CheckBox checkBox;
        }

        public ApplicationArrayAdapter(Context context, int resource, List<ApplicationItem> objects) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            this.applicationItems = objects;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(resource, parent, false);

                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.icon);
                holder.title = convertView.findViewById(R.id.title);
                holder.summary = convertView.findViewById(R.id.summary);
                holder.checkBox = convertView.findViewById(R.id.checkbox);
                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.summary.setText(applicationItems.get(position).packageName);
            holder.title.setText(applicationItems.get(position).applicationName);
            holder.icon.setImageDrawable(applicationItems.get(position).applicationIcon);
            holder.checkBox.setChecked(applicationItems.get(position).checked);

            convertView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    ViewHolder holder = (ViewHolder) view.getTag();
                    boolean checked = applicationItems.get(position).checked;
                    applicationItems.get(position).checked = !checked;
                    holder.checkBox.setChecked(!checked);
                }
            });
            return convertView;
        }
    }

//    @Override
//    public void setEntries(CharSequence[] entries) {
//        super.setEntries(entries);
//        checkedItems = new boolean[entries.length];
//    }

//    @Override
//    protected void onPrepareDialogBuilder(Builder builder) {
//        CharSequence[] entries = getEntries();
//        CharSequence[] entryValues = getEntryValues();
//        if (entries == null || entryValues == null || entries.length != entryValues.length) {
//            throw new IllegalStateException("Irregular array length");
//        }
//
//        restoreCheckedItems();
//        builder.setMultiChoiceItems(entries, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
//            public void onClick(DialogInterface dialog, int which, boolean value) {
//                checkedItems[which] = value;
//            }
//        });
//    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        restoreCheckedItems();

        ApplicationArrayAdapter applicationArrayAdapter = new ApplicationArrayAdapter(getContext(), R.layout.preference_application, applicationItems);
        builder.setAdapter(applicationArrayAdapter, null);
    }

//    private void restoreCheckedItems() {
//        List<CharSequence> entryValues = Arrays.asList(getEntryValues());
//        Set<String> values = getValues();
//
//        Iterator<String> iterator = values.iterator();
//        while (iterator.hasNext()) {
//            String value = iterator.next();
//            int index = entryValues.indexOf(value);
//            if (index != -1) checkedItems[index] = true;
//        }
//    }

    private void restoreCheckedItems() {
        Set<String> values = getValues();
        for (ApplicationItem applicationItem : applicationItems) {
            applicationItem.checked = values.contains(applicationItem.packageName);
        }
    }

//    @Override
//    protected void onDialogClosed(boolean positiveResult) {
//        super.onDialogClosed(positiveResult);
//        if (!positiveResult) return;
//
//        CharSequence[] entryValues = getEntryValues();
//        Set<String> values = new HashSet<>();
//
//        if (entryValues != null) {
//            for (int i = 0; i < entryValues.length; i++) {
//                if (checkedItems[i] == true) {
//                    String value = (String) entryValues[i];
//                    values.add(value);
//                }
//            }
//
//            if (callChangeListener(values)) {
//                setValues(values);
//            }
//        }
//    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult) return;

        Set<String> values = new HashSet<>();

        for (ApplicationItem applicationItem : applicationItems) {
            if (applicationItem.checked) {
                values.add(applicationItem.packageName);
            }
        }

        if (callChangeListener(values)) {
            setValues(values);
        }
    }
}