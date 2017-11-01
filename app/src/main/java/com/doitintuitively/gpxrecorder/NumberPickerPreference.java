package com.doitintuitively.gpxrecorder;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

public class NumberPickerPreference extends DialogPreference {
  private int mNumber;

  public NumberPickerPreference(Context context) {
    this(context, null);
  }

  public NumberPickerPreference(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.dialogPreferenceStyle);
  }

  public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, defStyleAttr);
  }

  public NumberPickerPreference(
      Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public int getNumber() {
    return mNumber;
  }

  public void setNumber(int number) {
    mNumber = number;
    persistInt(number);
  }

  @Override
  public int getDialogLayoutResource() {
    return R.layout.dialog_number_picker;
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getInt(index, 0);
  }

  void setUpSummary() {
    this.setSummary(
        String.format(getContext().getString(R.string.pref_update_frequency_summary), getNumber()));
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    // Read the value. Use the default value if it is not possible.
    setNumber(restorePersistedValue ? getPersistedInt(mNumber) : (int) defaultValue);
    setUpSummary();
  }
}
