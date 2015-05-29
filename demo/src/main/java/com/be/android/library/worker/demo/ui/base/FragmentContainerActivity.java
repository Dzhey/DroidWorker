package com.be.android.library.worker.demo.ui.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.be.android.library.worker.demo.BaseActivity;
import com.be.android.library.worker.demo.R;


public class FragmentContainerActivity extends BaseActivity implements FragmentContainerCallbacks {

    public static final String EXTRA_FRAGMENT_NAME = "fragment_name";
    public static final String EXTRA_FRAGMENT_ARGS = "fragment_args";

    private static final String TAG_CONTENT_FRAGMENT = "FragmentContainerActivity_content_fragment_tag_";

    private String mFragmentName;
    private Bundle mFragmentArgs;
    private Toolbar mToolbar;

    public static Intent prepareLaunchIntent(Context packageContext, String fragmentName, Bundle fragmentArgs) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(FragmentContainerActivity.EXTRA_FRAGMENT_NAME, fragmentName);

        if (fragmentArgs != null) {
            intent.putExtra(FragmentContainerActivity.EXTRA_FRAGMENT_ARGS, fragmentArgs);
        }

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fragment_container);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Intent intent = getIntent();
        mFragmentName = intent.getStringExtra(EXTRA_FRAGMENT_NAME);
        mFragmentArgs = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS);

        Fragment fragment = getContentFragment();
        if (fragment == null) {
            initContentView(mFragmentName, mFragmentArgs);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        BaseFragment fragment = getContentFragment();

        switch (item.getItemId()) {
            case android.R.id.home:
                if (fragment != null && fragment.onOptionsItemSelected(item)) {
                    return true;
                }
                finish();
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        BaseFragment fragment = getContentFragment();

        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void initContentView(String fragmentName, Bundle fragmentArgs) {
        Fragment fragment = Fragment.instantiate(this, fragmentName, fragmentArgs);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, fragment, TAG_CONTENT_FRAGMENT)
                .commit();
    }

    protected BaseFragment getContentFragment() {
        return (BaseFragment) getSupportFragmentManager().findFragmentByTag(TAG_CONTENT_FRAGMENT);
    }

    @Override
    public void onBackPressed() {
        BaseFragment fragment = getContentFragment();

        if (fragment != null && fragment.handleBackPress()) {
            return;
        }

        super.onBackPressed();
    }
}