package com.doitintuitively.gpxrecorder;

import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.NumberPicker;

public class NumberPickerPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

  private NumberPicker mNumberPicker;
  private static final int mMinNumber = 0;
  private static final int mMaxNumber = 120;

  @Override
  public void onDialogClosed(boolean positiveResult) {
    if (positiveResult) {
      DialogPreference preference = getPreference();
      if (preference instanceof NumberPickerPreference) {
        NumberPickerPreference numberPickerPreference = (NumberPickerPreference) preference;
        if (numberPickerPreference.callChangeListener(mNumberPicker.getValue())) {
          // Save the value
          numberPickerPreference.setNumber(mNumberPicker.getValue());
          numberPickerPreference.setUpSummary();
        }
      }
    }
  }

  public static NumberPickerPreferenceDialogFragmentCompat newInstance(String key) {
    final NumberPickerPreferenceDialogFragmentCompat fragment =
        new NumberPickerPreferenceDialogFragmentCompat();
    final Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);

    return fragment;
  }

  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);

    mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
    String[] nums = new String[mMaxNumber - mMinNumber + 1];
    for (int i = 0; i < nums.length; i++) {
      nums[i] = Integer.toString(i);
    }

    mNumberPicker.setMinValue(mMinNumber);
    mNumberPicker.setMaxValue(mMaxNumber);
    mNumberPicker.setWrapSelectorWheel(true);
    mNumberPicker.setDisplayedValues(nums);
    mNumberPicker.setValue(0);

    Integer number = null;
    DialogPreference preference = getPreference();
    if (preference instanceof NumberPickerPreference) {
      number = ((NumberPickerPreference) preference).getNumber();
    }

    if (number != null) {
      mNumberPicker.setValue(number);
    }
  }
}
