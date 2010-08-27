package com.riotopsys.MALforAndroid;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

class IntegerPicker extends Dialog implements OnClickListener {

	private int lower;
	private int upper;
	private int current;
	private Button cmdInc;
	private Button cmdDec;
	private Button cmdDone;
	private EditText txtValue;
	
	private boolean canceled;

	public IntegerPicker(Context context) {
		super(context);
		this.setContentView(R.layout.integer_picker);

		lower = Integer.MIN_VALUE;
		upper = Integer.MAX_VALUE;

		current = 0;

		cmdInc = (Button) findViewById(R.id.ButtonInc);
		cmdDec = (Button) findViewById(R.id.ButtonDec);
		cmdDone = (Button) findViewById(R.id.ButtonDone);

		cmdInc.setOnClickListener(this);
		cmdDec.setOnClickListener(this);
		cmdDone.setOnClickListener(this);

		txtValue = (EditText) findViewById(R.id.textValue);
		txtValue.setText(String.valueOf(current));

		txtValue.addTextChangedListener(new IntWatcher());
		canceled = false;
	}
	
	public boolean wasCanceled(){
		return !(canceled);
	}
	
	public int getCurrent(){
		return current;
	}
	
	@Override
	public void show(){
		canceled = false;
		super.show();
	}

	public void setLimits(int lower, int upper) {
		this.lower = lower;
		this.upper = upper;
	}

	public void setCurrent(int current) {
		if (lower <= current && current <= upper) {
			this.current = current;
			txtValue.setText(String.valueOf(current));
		} else {
			Log.i("IntegerPicker", "out of bounds");
		}
	}

	@Override
	public void onClick(View v) {
		if (v == cmdInc) {
			setCurrent(current + 1);
		} else if (v == cmdDec) {
			setCurrent(current - 1);
		} else {
			canceled = true;
			dismiss();
		}

	}

	private class IntWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable a) {

			try {
				int bob = Integer.valueOf(a.toString());
				if (!(lower <= bob && bob <= upper)) {
					txtValue.setText(String.valueOf(current));
				} else {
					current = bob;
				}
			} catch (NumberFormatException e) {
				txtValue.setText(String.valueOf(current));
			}

		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

		}

	}

}