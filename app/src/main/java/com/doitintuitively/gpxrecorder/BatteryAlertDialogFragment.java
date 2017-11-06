package com.doitintuitively.gpxrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

public class BatteryAlertDialogFragment extends DialogFragment {
  /* The activity that creates an instance of this dialog fragment must
   * implement this interface in order to receive event callbacks.
   * Each method passes the DialogFragment in case the host needs to query it. */
  public interface BatteryAlertDialogListener {
    public void onDialogPositiveClick(DialogFragment dialog);

    public void onDialogPositiveClickWithChecked(DialogFragment dialog);

    public void onDialogNegativeClick(DialogFragment dialog);
  }

  // Use this instance of the interface to deliver action events
  BatteryAlertDialogListener mListener;
  CheckBox checkBoxDoNotShowAgain;

  // Override the Fragment.onAttach() method to instantiate the BatteryAlertDialogListener
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    // Verify that the host activity implements the callback interface
    try {
      // Instantiate the BatteryAlertDialogListener so we can send events to the host
      mListener = (BatteryAlertDialogListener) activity;
    } catch (ClassCastException e) {
      // The activity doesn't implement the interface, throw exception
      throw new ClassCastException(
          activity.toString() + " must implement BatteryAlertDialogListener");
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    // Get the layout inflater
    LayoutInflater inflater = getActivity().getLayoutInflater();

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    final View view = inflater.inflate(R.layout.dialog_battery_alert, null);
    builder
        .setView(view)
        // Add action buttons
        .setPositiveButton(
            "Agree",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                checkBoxDoNotShowAgain = (CheckBox) view.findViewById(R.id.checkBoxDoNotShowAgain);
                if (checkBoxDoNotShowAgain.isChecked()) {
                  // Send the positive button event with do not show again back to the host activity
                  mListener.onDialogPositiveClickWithChecked(BatteryAlertDialogFragment.this);
                } else {
                  // Send the positive button event back to the host activity
                  mListener.onDialogPositiveClick(BatteryAlertDialogFragment.this);
                }
              }
            })
        .setNegativeButton(
            "Cancel",
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                // Send the negative button event back to the host activity
                mListener.onDialogNegativeClick(BatteryAlertDialogFragment.this);
              }
            })
        .setMessage(getString(R.string.battery_alert_dialog_text))
        .setTitle(getString(R.string.battery_alert_dialog_title));
    return builder.create();
  }
}
