package org.estgroup.phphub.ui.view.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;

import com.umeng.fb.FeedbackAgent;
import com.umeng.fb.model.UserInfo;

import org.estgroup.phphub.R;
import org.estgroup.phphub.common.App;
import org.estgroup.phphub.common.util.Utils;
import org.estgroup.phphub.ui.view.WebViewPageActivity;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import cn.jpush.android.api.JPushInterface;
import eu.unicate.retroauth.AuthAccountManager;

import static org.estgroup.phphub.common.Constant.USERNAME_KEY;
import static org.estgroup.phphub.common.Constant.USER_ID_KEY;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String LOGOUT_KEY = "logout";

    @Inject
    AccountManager accountManager;

    @Inject
    AuthAccountManager authAccountManager;

    Account account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        ((App) getActivity().getApplication()).getAppComponent().inject(this);
        account = Utils.getActiveAccount(getActivity(), authAccountManager);

        ((Preference) findPreference("feedback")).setOnPreferenceClickListener(this);
        ((Preference) findPreference("source_code")).setOnPreferenceClickListener(this);
        ((Preference) findPreference("about_phphub")).setOnPreferenceClickListener(this);
        ((Preference) findPreference("about_our_group")).setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.hasLoggedIn(getActivity(), accountManager) && (findPreference(LOGOUT_KEY) == null)) {
            Preference logoutPreference = new Preference(getActivity());
            logoutPreference.setKey(LOGOUT_KEY);
            logoutPreference.setLayoutResource(R.layout.common_logout);
            logoutPreference.setOnPreferenceClickListener(this);
            getPreferenceScreen().addPreference(logoutPreference);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "feedback":
                final FeedbackAgent agent = new FeedbackAgent(getActivity());
                agent.startFeedbackActivity();

                com.umeng.fb.model.UserInfo info = agent.getUserInfo();
                if (info == null) {
                    info = new UserInfo();
                }

                Map<String, String> contact = info.getContact();
                if (contact == null) {
                    contact = new HashMap<>();
                }

                if (Utils.hasLoggedIn(getActivity(), accountManager)) {
                    contact.put("plain", "uid: "+ accountManager.getUserData(account, USER_ID_KEY) + " uname: "+accountManager.getUserData(account, USERNAME_KEY));

                    info.setContact(contact);

                    agent.setUserInfo(info);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean result = agent.updateUserInfo();
                        }
                    }).start();
                }
                return true;
            case "source_code":
                Intent intentCode = WebViewPageActivity.getCallingIntent(getActivity(), "https://github.com/phphub/phphub-android");
                getActivity().startActivity(intentCode);

                return true;
            case "about_phphub":
                Intent intentPhphub = WebViewPageActivity.getCallingIntent(getActivity(), "https://phphub.org/about");
                getActivity().startActivity(intentPhphub);

                return true;
            case "about_our_group":
                Intent intentGroup = WebViewPageActivity.getCallingIntent(getActivity(), "http://est-group.org");
                getActivity().startActivity(intentGroup);

                return true;
            case LOGOUT_KEY:
                new AlertDialog.Builder(getActivity())
                        .setMessage("确认退出吗？")
                        .setCancelable(false
                        )
                        .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                JPushInterface.setAlias(getActivity().getApplicationContext(), "", null);
                                accountManager.removeAccount(Utils.getAccounts(getActivity(), accountManager)[0], null, null);
                                getPreferenceScreen().removePreference(findPreference(LOGOUT_KEY));
                            }
                        })
                        .setNegativeButton("容我想想", null)
                        .show();
                return true;
        }
        return false;
    }
}
