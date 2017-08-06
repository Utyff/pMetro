package com.utyf.pmetro;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.utyf.pmetro.util.StationSelectionMenuAdapter;
import com.utyf.pmetro.util.StationSelectionMenuItem;
import com.utyf.pmetro.util.StationsNum;

public class StationSelectionDialogFragment extends DialogFragment {
    interface Listener {
        void onStationSelected(StationsNum stn, boolean isLongTap);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Listener listener = (Listener) getActivity();
        Bundle arguments = getArguments();
        StationSelectionMenuItem items[] = (StationSelectionMenuItem[])arguments.getParcelableArray("items");
        final StationsNum[] stations = (StationsNum[])arguments.getParcelableArray("stations");
        final boolean isLongTap = arguments.getBoolean("isLongTap");
        DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position) {
                listener.onStationSelected(stations[position], isLongTap);
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_station);
        builder.setAdapter(new StationSelectionMenuAdapter(getActivity(), items), dialogListener);
        return builder.create();
    }
}
