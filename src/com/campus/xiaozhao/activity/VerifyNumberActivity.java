package com.campus.xiaozhao.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import cn.bmob.v3.BmobSMS;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.LogInListener;
import cn.bmob.v3.listener.RequestSMSCodeListener;

import com.campus.xiaozhao.Configuration;
import com.campus.xiaozhao.R;
import com.campus.xiaozhao.basic.utils.CampusSharePreference;
import com.campus.xiaozhao.basic.widget.CountDownTimerView;
import com.campus.xiaozhao.basic.widget.CountDownTimerView.OnCountDownListener;
import com.component.logger.Logger;

public class VerifyNumberActivity extends Activity implements OnCountDownListener {
	
	public static final String TAG = "VerifyNumberActivity";
	private static final String KEY_PHONE_NUMBER = "phone_number";
	private static final String KEY_PASSWORD = "password";
	
	private TextView mNumberTextView;
	private EditText mVerifyCodeEditText;
	private CountDownTimerView mCountDownTimerView;
	
	private String mPhoneNumber;
	private String mPassword;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_verify_number);
		
		mPhoneNumber = getIntent().getStringExtra(KEY_PHONE_NUMBER);
		if (TextUtils.isEmpty(mPhoneNumber)) {
			Logger.e(TAG, "onCreate: invalid phone number: " + mPhoneNumber);
			return;
		}
		mPassword = getIntent().getStringExtra(KEY_PASSWORD);
		Logger.d(TAG, "onCreate: phone number: " + mPhoneNumber);
		
		ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        
        setTitle(R.string.label_verification_title);
        
        mNumberTextView = (TextView) findViewById(R.id.phone_number_tv);
        mNumberTextView.setText(mPhoneNumber);
        
        mCountDownTimerView = (CountDownTimerView) findViewById(R.id.request_verification_code);
        mCountDownTimerView.setOnCountDownListener(this);
        mCountDownTimerView.startCountDown(
                Configuration.VERIFICATION_WAIT_TIME,
                Configuration.COUNT_INTERVAL);
        
        mVerifyCodeEditText = (EditText) findViewById(R.id.verification_code_et);
        sendVerifyRequest();
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

	public static void startFrom(Context context, String phoneNumber, String password) {
		Intent intent = new Intent(context, VerifyNumberActivity.class);
		intent.putExtra(KEY_PHONE_NUMBER, phoneNumber);
		intent.putExtra(KEY_PASSWORD, password);
		context.startActivity(intent);
	}

	@Override
	public boolean onCountDownFinishState() {
		return true;
	}
	
	@Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
	        .setMessage(R.string.verification_quit_dialog_message)
	        .setNegativeButton(R.string.verification_quit_dialog_cancel, null)
	        .setPositiveButton(R.string.verification_quit_dialog_confirm, new OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	                finish();
	            }
	        }).create().show();
    }
	
	public void clickOnGetVerificationCode(View view) {
        mCountDownTimerView.startCountDown(
                Configuration.VERIFICATION_WAIT_TIME,
                Configuration.COUNT_INTERVAL);
        
        sendVerifyRequest();
    }
	
	public void clickOnCommit(View view) {
		String smsCode = mVerifyCodeEditText.getText().toString();
		if (TextUtils.isEmpty(smsCode)) {
			Toast.makeText(this, R.string.toast_input_verification_code, Toast.LENGTH_SHORT).show();
			return;
		}
		
		BmobUser user = new BmobUser();
		user.setUsername(mPhoneNumber);
		user.setPassword(mPassword);
		BmobUser.signOrLoginByMobilePhone(this, mPhoneNumber, smsCode, new LogInListener<BmobUser>() {
			@Override
			public void done(BmobUser user, BmobException ex) {
				if (ex != null) {
					Logger.e(TAG, "signOrLoginByMobilePhone failed: code=" + ex.getErrorCode()
							+ ", msg=" + ex.getLocalizedMessage());
					toast(getString(R.string.toast_verification_failed) + ": " + ex.getLocalizedMessage());
					return;
				}
				CampusSharePreference.setLogin(VerifyNumberActivity.this, true);
				MainActivity.startFrom(VerifyNumberActivity.this);
			}
		});
	}
	
	/**
	 * 向后台发送手机号验证请求
	 */
	private void sendVerifyRequest() {
		final String phoneNumber = mPhoneNumber;
        final String template = Configuration.SMS_VERIFY_TEMPLATE;
        BmobSMS.requestSMSCode(this, phoneNumber, template, new RequestSMSCodeListener() {
            @Override
            public void done(Integer smsId, BmobException ex) {
                if (ex != null) {
                	Logger.e(TAG, "requestSMSCode failed: code=" + ex.getErrorCode() + ", msg=" + ex.getLocalizedMessage());
                	toast(getString(R.string.toast_send_verification_error));
                	return;
                }
            }
        });
	}
	
	private void toast(String text) {
		Toast.makeText(VerifyNumberActivity.this, text, Toast.LENGTH_LONG).show();
	}
}
