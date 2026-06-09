package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.R;

import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.RegisterViewModel;
import android.text.Editable;
import android.text.TextWatcher;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String BUNDLE = "bundle";

    ImageView img_signup_BackBtn;
    TextInputLayout txtl_signup_FullName, txtl_signup_UserName, txtl_signup_Email, txtl_signup_PhoneNumber, txtl_signup_Password;
    Button btn_signup_Next;

    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_layout);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        //region Lấy đối tượng view
        img_signup_BackBtn = findViewById(R.id.img_signup_BackBtn);
        txtl_signup_FullName = findViewById(R.id.txtl_signup_FullName);
        txtl_signup_UserName = findViewById(R.id.txtl_signup_UserName);
        txtl_signup_Email = findViewById(R.id.txtl_signup_Email);
        txtl_signup_PhoneNumber = findViewById(R.id.txtl_signup_PhoneNumber);
        txtl_signup_Password = findViewById(R.id.txtl_signup_Password);
        btn_signup_Next = findViewById(R.id.btn_signup_Next);
        //endregion

        // Khôi phục dữ liệu từ ViewModel nếu có
        restoreViewModelData();

        // Lắng nghe thay đổi để cập nhật vào ViewModel
        setupTextWatchers();

        img_signup_BackBtn.setOnClickListener(this);
        btn_signup_Next.setOnClickListener(this);
    }

    private void restoreViewModelData() {
        if (txtl_signup_FullName.getEditText() != null && !viewModel.getFullName().isEmpty()) {
            txtl_signup_FullName.getEditText().setText(viewModel.getFullName());
        }
        if (txtl_signup_UserName.getEditText() != null && !viewModel.getUserName().isEmpty()) {
            txtl_signup_UserName.getEditText().setText(viewModel.getUserName());
        }
        if (txtl_signup_Email.getEditText() != null && !viewModel.getEmail().isEmpty()) {
            txtl_signup_Email.getEditText().setText(viewModel.getEmail());
        }
        if (txtl_signup_PhoneNumber.getEditText() != null && !viewModel.getPhoneNumber().isEmpty()) {
            txtl_signup_PhoneNumber.getEditText().setText(viewModel.getPhoneNumber());
        }
        if (txtl_signup_Password.getEditText() != null && !viewModel.getPassword().isEmpty()) {
            txtl_signup_Password.getEditText().setText(viewModel.getPassword());
        }
    }

    private void setupTextWatchers() {
        if (txtl_signup_FullName.getEditText() != null) {
            txtl_signup_FullName.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.setFullName(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        if (txtl_signup_UserName.getEditText() != null) {
            txtl_signup_UserName.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.setUserName(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        if (txtl_signup_Email.getEditText() != null) {
            txtl_signup_Email.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.setEmail(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        if (txtl_signup_PhoneNumber.getEditText() != null) {
            txtl_signup_PhoneNumber.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.setPhoneNumber(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        if (txtl_signup_Password.getEditText() != null) {
            txtl_signup_Password.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.setPassword(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_signup_Next) {
            if (!validateFullName() | !validateUserName() | !validateEmail() | !validatePhone() | !validatePass()) {
                return;
            }

            String fullname = txtl_signup_FullName.getEditText().getText().toString();
            String username = txtl_signup_UserName.getEditText().getText().toString();
            String email = txtl_signup_Email.getEditText().getText().toString();
            String sdt = txtl_signup_PhoneNumber.getEditText().getText().toString();
            String password = txtl_signup_Password.getEditText().getText().toString();

            Intent intent = new Intent(RegisterActivity.this, Register2ndActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("hoten", fullname);
            bundle.putString("tendn", username);
            bundle.putString("email", email);
            bundle.putString("sdt", sdt);
            bundle.putString("matkhau", password);
            intent.putExtra(BUNDLE, bundle);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        } else if (id == R.id.img_signup_BackBtn) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    //region Validate fields
    private boolean validateFullName() {
        String val = txtl_signup_FullName.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_signup_FullName.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            txtl_signup_FullName.setError(null);
            txtl_signup_FullName.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateUserName() {
        String val = txtl_signup_UserName.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_signup_UserName.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            txtl_signup_UserName.setError(null);
            txtl_signup_UserName.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateEmail() {
        String val = txtl_signup_Email.getEditText().getText().toString().trim();
        String checkEmail = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (val.isEmpty()) {
            txtl_signup_Email.setError(getResources().getString(R.string.not_empty));
            return false;
        } else if (!val.matches(checkEmail)) {
            txtl_signup_Email.setError("Email không hợp lệ!");
            return false;
        } else {
            txtl_signup_Email.setError(null);
            txtl_signup_Email.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePhone() {
        String val = txtl_signup_PhoneNumber.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_signup_PhoneNumber.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            txtl_signup_PhoneNumber.setError(null);
            txtl_signup_PhoneNumber.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePass() {
        String val = txtl_signup_Password.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_signup_Password.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            txtl_signup_Password.setError(null);
            txtl_signup_Password.setErrorEnabled(false);
            return true;
        }
    }
    //endregion
}