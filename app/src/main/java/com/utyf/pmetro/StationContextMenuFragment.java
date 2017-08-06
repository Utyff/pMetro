package com.utyf.pmetro;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.utyf.pmetro.util.StationsNum;

public class StationContextMenuFragment extends DialogFragment {
    interface Listener {
        void onInfoSelected(StationsNum stn);

        void onStartSelected(StationsNum stn);

        void onFinishSelected(StationsNum stn);

        void onViaSelected(StationsNum stn);

        void onAvoidSelected(StationsNum stn);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof Listener)) {
            throw new ClassCastException(context.toString() + " must implement StationContextMenuFragment.Listener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.map_station_info),
                getString(R.string.map_station_start),
                getString(R.string.map_station_finish),
                getString(R.string.map_station_via),
                getString(R.string.map_station_avoid)
        };
        final Listener listener = (Listener) getActivity();
        Bundle arguments = getArguments();
        String stationName = arguments.getString("station_name");
        final StationsNum stn = arguments.getParcelable("station_num");
        DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        listener.onInfoSelected(stn);
                        break;
                    case 1:
                        listener.onStartSelected(stn);
                        break;
                    case 2:
                        listener.onFinishSelected(stn);
                        break;
                    case 3:
                        listener.onViaSelected(stn);
                        break;
                    case 4:
                        listener.onAvoidSelected(stn);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(stationName);
        builder.setItems(items, dialogListener);
        return builder.create();
    }
}
