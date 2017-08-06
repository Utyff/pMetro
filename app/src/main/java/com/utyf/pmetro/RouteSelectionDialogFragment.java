package com.utyf.pmetro;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.utyf.pmetro.util.RouteListItem;
import com.utyf.pmetro.util.RouteListItemAdapter;

public class RouteSelectionDialogFragment extends DialogFragment {
    interface Listener {
        void onRouteSelected(int position);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof StationContextMenuFragment.Listener)) {
            throw new ClassCastException(context.toString() + " must implement RouteSelectionDialogFragment.Listener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Listener listener = (Listener) getActivity();
        Bundle arguments = getArguments();
        RouteListItem items[] = (RouteListItem[])arguments.getParcelableArray("items");
        DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position) {
                listener.onRouteSelected(position);
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_route);
        builder.setAdapter(new RouteListItemAdapter(getActivity(), R.layout.route_list_item, items), dialogListener);
        return builder.create();
    }
}
